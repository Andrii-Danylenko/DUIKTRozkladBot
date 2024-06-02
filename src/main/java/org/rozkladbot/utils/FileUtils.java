package org.rozkladbot.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rozkladbot.DBControllers.UserDB;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component("FileUtils")
public class FileUtils {
    // Буферизация данных раз в день, которая позволяет избежать проблем с доступом к API
    @Async
    //TODO: изменить значения
    @Scheduled(cron = "0 0 5 * * *")
    public void dumpSchedule() {
        UserDB.getAllUsers().values().forEach(x -> {
            if (x.getGroupNumber() == null) return;
            try {
                Path directoryPath = Paths.get("groupsSchedules");
                Path filePath = directoryPath.resolve(x.getGroupNumber() + ".json");
                // Защита от повторной буферизации, если есть несколько человек из одинаковых групп
                HashMap<String, String> params = x.getUserParams();
                params.put("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString())));
                params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                String response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                writeFile(filePath, response);
                params.replace("dateFrom", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(7)));
                params.put("dateTo", DateUtils.toString(DateUtils.getStartOfWeek(DateUtils.getTodayDateString()).plusDays(14)));
                response = Requester.makeRequest(DAOImpl.getBaseUrl(), params);
                filePath = directoryPath.resolve(x.getGroupNumber() + "NextWeek.json");
                writeFile(filePath, response);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        });
    }
    private void writeFile(Path filePath, String data) throws IOException {
        if (!DateUtils.longToDate(filePath.toFile().lastModified()).isBefore(DateUtils.getTodayDate().minusDays(1))) {
            return;
        }
        if (!Files.exists(filePath)) {
            Files.createDirectories(filePath);
        }
        try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile(), false)) {
            outputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Async
    @Scheduled(cron = "0 0 6 * * *")
    public synchronized void serializeUsers() throws IOException {
        // Значение пути будет стандартным (пока что?)
        UserDB.getAllUsers().values().forEach(UserDB::serialize);
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("UserList.txt");
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
            System.out.println(UserDB.serialize(userList.get(i)));
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
        System.out.println(builder);
    }
    public static Map<Long, User> deserializeUsers() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("UserList.txt");
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
            UserState state = UserState.getUserStateFromString((String) jsonObject.get("state"));
            Integer lastPinnedMessage = lastPinnedMessageStr.equalsIgnoreCase("null") ? null : Integer.parseInt( (String) jsonObject.get("lastPinnedMessage"));
            User user = new User(chatId, group, faculty, course, state, lastPinnedMessage);
            userMap.put(chatId, user);
        }
        return userMap;
    }
}
