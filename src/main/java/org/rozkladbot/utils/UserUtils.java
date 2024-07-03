package org.rozkladbot.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.Group;
import org.rozkladbot.entities.User;
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

@Component("UserUtils")
public class UserUtils {
    private static final ConsoleLineLogger<UserUtils> log = new ConsoleLineLogger<>(UserUtils.class);
    @Async
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Kiev")
    public synchronized void serializeUsers() throws IOException {
        UserDB.getAllUsers().values().forEach(UserDB::serialize);
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("usersList.json");
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
            log.success("Завершив буферизацію усіх користувачів");
        } catch (IOException exception) {
            log.error("Помилка під час буферизації користувачів. Привід: %s".formatted(exception.getCause()));
        }
    }
    public static Map<Long, User> deserializeUsers() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        Path directoryPath = Paths.get("users");
        Path filePath = directoryPath.resolve("usersList.json");
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(String.valueOf(filePath)));
        JSONArray jsonArray = (JSONArray) jsonObject.get("users");
        Iterator jsonArrayIterator = jsonArray.iterator();
        Map<Long, User> userMap = new ConcurrentHashMap<>();
        while (jsonArrayIterator.hasNext()) {
            jsonObject = (JSONObject) jsonArrayIterator.next();
            long chatId = Long.parseLong((String) jsonObject.get("chatId"));
            Group group = GroupDB.getGroups().get((String) jsonObject.get("group"));
            String lastPinnedMessageStr = (String) jsonObject.get("lastPinnedMessage");
            UserRole role = UserRole.getUserRoleFromString((String) jsonObject.get("role"));
            UserState state = UserState.getUserStateFromString((String) jsonObject.get("state"));
            Integer lastPinnedMessage = lastPinnedMessageStr.equalsIgnoreCase("null") ? null : Integer.parseInt((String) jsonObject.get("lastPinnedMessage"));
            boolean areInBroadcastGroup = (Boolean) jsonObject.get("areInBroadcastGroup");
            long lastSentMessage = (long) jsonObject.get("lastSentMessage");
            User user = new User(chatId, group, state, role, lastPinnedMessage, areInBroadcastGroup, lastSentMessage);
            userMap.put(chatId, user);
        }
        return userMap;
    }

    public static Map<String, Group> deserializeGroups() throws IOException, ParseException {
        JSONParser jsonParser = new JSONParser();
        Path directoryPath = Paths.get("groups");
        Path filePath = directoryPath.resolve("groupsList.json");
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader(String.valueOf(filePath)));
        JSONArray jsonArray = (JSONArray) jsonObject.get("groups");
        Iterator jsonArrayIterator = jsonArray.iterator();
        Map<String, Group> groupMap = new ConcurrentHashMap<>();
        while (jsonArrayIterator.hasNext()) {
            jsonObject = (JSONObject) jsonArrayIterator.next();
            String institute = (String) jsonObject.get("institute");
            String group = (String) jsonObject.get("group");
            String faculty = (String) jsonObject.get("faculty");
            long groupNumber = (long) jsonObject.get("groupNumber");
            String course = (String) jsonObject.get("course");
            groupMap.put(group, new Group(institute, group, faculty, groupNumber, course));
        }
        return groupMap;
    }
}
