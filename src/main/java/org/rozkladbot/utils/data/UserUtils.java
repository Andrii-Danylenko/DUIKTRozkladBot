package org.rozkladbot.utils.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.entities.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.Group;
import org.rozkladbot.entities.User;
import org.rozkladbot.interfaces.Serializer;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Component("UserUtils")
public class UserUtils extends AbstractJsonDeserializer<Long, User> implements Serializer {
    private static final ConsoleLineLogger<UserUtils> log = new ConsoleLineLogger<>(UserUtils.class);
    @Async
    @Scheduled(cron = "0 0 4 * * *", zone = "Europe/Kiev")
    public synchronized void serialize() throws IOException {
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
                try {
                    builder.append(UserDB.serialize(userList.get(i)));
                    if (i < userList.size() - 1) {
                        builder.append(',').append('\n');
                    }
                } catch (Exception exception) {
                    log.error(exception.getMessage());
                }
            }
            try (FileOutputStream outputStream = new FileOutputStream(filePath.toFile(), false)) {
                outputStream.write(builder.append("\n]\n}").toString().getBytes(StandardCharsets.UTF_8));
                log.success("Завершив буферизацію усіх користувачів");
            } catch (IOException exception) {
                log.error("Помилка під час буферизації користувачів. Привід: %s".formatted(exception.getCause().getMessage()));
            }
    }

    @Override
    public Map<Long, User> deserialize(String dirName, String fileName, String jsonArrayName, String...keys) throws IOException, ParseException {
        Iterator jsonArrayIterator = getJsonIterator(dirName, fileName, jsonArrayName);
        JSONObject jsonObject;
        Map<Long, User> userMap = new ConcurrentHashMap<>();
        User user;
        while (jsonArrayIterator.hasNext()) {
            user = new User();
            jsonObject = (JSONObject) jsonArrayIterator.next();
            for (String key : keys) {
                getObjectAndApply(jsonObject, key, user);
            }
            userMap.put(user.getChatID(), user);
        }
        return userMap;
       }

    @Override
    protected void getObjectAndApply(JSONObject jsonObject, String key, User entity) {
        switch (key) {
            case "chatId" -> entity.setChatID(Long.parseLong((String) jsonObject.get(key)));
            case "group" -> {
                Group group = GroupDB.getGroups().get((String) jsonObject.get(key));
                if (group == null) {
                    entity.setGroup(GroupDB.getGroups().get("ІСД-11"));
                }
                else entity.setGroup(group);
            }
            case "lastPinnedMessage" -> {
                String lastPinnedMessageStr = (String) jsonObject.get(key);
                Integer lastPinnedMessage = lastPinnedMessageStr.equalsIgnoreCase("null") ? null : Integer.parseInt((String) jsonObject.get(key));
                entity.setLastPinnedMessageId(lastPinnedMessage);
            }
            case "role" -> entity.setRole(new UserRole(((String) jsonObject.get(key))));
            case "state" -> entity.setState(UserState.getUserStateFromString((String) jsonObject.get(key)));
            case "areInBroadcastGroup" -> entity.setAreInBroadcastGroup((Boolean) jsonObject.get(key));
            case "lastSentMessage" -> entity.setLastSentMessage((long) jsonObject.get(key));
            case "userName" -> {
                String userName = (String) jsonObject.get(key);
                entity.setUserName(userName == null ? "" : userName);
            }
        }
    }
}
