package org.rozkladbot.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.User;
import org.rozkladbot.web.Requester;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Component("FileUtils")
public class FileUtils {
    // Буферизация данных раз в день, которая позволяет избежать проблем с доступом к API

    @Async
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Kiev")
    public void dumpSchedule() {
       dumpSchedule(false);
    }

    @Async
    public void dumpSchedule(boolean isForced) {
        Set<Long> alreadyDumpedGroups = new HashSet<>();
        System.out.println("Починаю локальне зберігання розкладів");
        UserDB.getAllUsers().values().forEach(x -> {
            Long groupNumber = x.getGroupNumber();
            if (groupNumber == null || alreadyDumpedGroups.contains(groupNumber)) return;
            System.out.println(x.getGroupNumber());
            try {
                System.out.printf("Починаю локальне зберігання розкладів для користувача %d%n", x.getChatID());
                Path directoryPath = Paths.get("groupsSchedules");
                HashMap<String, String> params = x.getUserParams();
                String response = "";
                String fileName = x.getGroupNumber() + ".json";
                if (checkIfAlreadyWritten(directoryPath, fileName, isForced)) {
                    params.put("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString())));
                    params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                    response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                    writeFile(directoryPath, fileName, response);
                } else {
                    System.out.printf("Файл %s вже нещодавно був записаний. Пропускаю...%n", fileName);
                }
                fileName = x.getGroupNumber() + "NextWeek.json";
                if (checkIfAlreadyWritten(directoryPath, fileName, isForced)) {
                    response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                    writeFile(directoryPath,x.getGroupNumber() + ".json", response);
                    params.put("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                    params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(14)));
                    response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                    writeFile(directoryPath, fileName, response);
                } else {
                    System.out.printf("Файл %s вже нещодавно був записаний. Пропускаю...%n", fileName);
                }
                alreadyDumpedGroups.add(groupNumber);
            } catch (IOException e) {
                System.out.println("Виникла помилка: ");
                e.printStackTrace();
            }
        });
    }

    private boolean checkIfAlreadyWritten(Path directoryPath, String fileName, boolean isForced) {
        Path filePath = directoryPath.resolve(fileName);
        try {
            System.out.printf("""
                    last modified: %s
                    today date: %s
                    today date minus 1 day: %s
                    """, DateUtils.instantToLocalDate(Files.getLastModifiedTime(filePath).toInstant()), DateUtils.getTodayDate(), DateUtils.getTodayDate().minusDays(1));
            return isForced || DateUtils.instantToLocalDate(Files.getLastModifiedTime(filePath).toInstant()).isBefore(DateUtils.getTodayDate().minusDays(1)) || "Понеділок".equalsIgnoreCase(DateUtils.getDayOfWeek(DateUtils.getTodayDateString()));
        } catch (IOException exception) {
            System.out.printf("Файла %s не існує. Починаємо запис...%n", filePath);
            return true;
        }
    }
    private void writeFile(Path directoryPath, String fileName, String data) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
        Path filePath = directoryPath.resolve(fileName);
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile(), false)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }
    public static Path resolvePath(String path, String fileName) {
        Path dirPath = Path.of(path);
        Path finalPath = dirPath.resolve(fileName);
        System.out.println(finalPath);
        return finalPath;
    }
    @Async
    @Scheduled(cron = "0 0 5 * * *", zone = "Europe/Kiev")
    public synchronized void serializeUsers() throws IOException {
        // Значение пути будет стандартным (пока что?)
        UserDB.getAllUsers().values().forEach(UserDB::serialize);
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("userList.json");
        if (!Files.exists(filePath)) {
            Files.createDirectories(directoryPath);
            Files.createFile(filePath);
        }

        List<User> userList = UserDB.getAllUsers().values().stream().toList();
        StringBuilder builder = new StringBuilder("""
                {
                 "users": [
                """);
        for (int i = 0; i < userList.size(); i++) {
            builder.append(UserDB.serialize(userList.get(i)));
            if (i < userList.size() - 1) {
                builder.append(',').append('\n');
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile(), false)) {
            outputStream.write(builder.append("\n]\n}").toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("Завершив буферизацію усіх користувачів");
        } catch (IOException exception) {
            System.out.println("Помилка");
        }
    }
    public static Map<Long, User> deserializeUsers() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("userList.json");
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(String.valueOf(filePath)));
        JSONArray jsonArray = (JSONArray) jsonObject.get("users");
        Iterator jsonArrayIterator = jsonArray.iterator();
        Map<Long, User> userMap = new ConcurrentHashMap<>();
        while (jsonArrayIterator.hasNext()) {
            jsonObject = (JSONObject) jsonArrayIterator.next();
            long chatId = Long.parseLong((String) jsonObject.get("chatId"));
            String faculty = (String) jsonObject.get("faculty");
            String group = (String) jsonObject.get("group");
            String course = (String) jsonObject.get("course");
            String lastPinnedMessageStr = (String) jsonObject.get("lastPinnedMessage");
            UserRole role = UserRole.getUserRoleFromString((String) jsonObject.get("role"));
            UserState state = UserState.getUserStateFromString((String) jsonObject.get("state"));
            Integer lastPinnedMessage = lastPinnedMessageStr.equalsIgnoreCase("null") ? null : Integer.parseInt((String) jsonObject.get("lastPinnedMessage"));
            boolean areInBroadcastGroup = (Boolean) jsonObject.get("areInBroadcastGroup");
            User user = new User(chatId, group, faculty, course, state, role, lastPinnedMessage, areInBroadcastGroup);
            userMap.put(chatId, user);
        }
        return userMap;
    }
}
