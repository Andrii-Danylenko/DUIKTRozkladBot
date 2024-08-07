package org.rozkladbot.utils.data;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
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
            case "chatId" -> entity.setChatID(Long.parseLong((String) jsonObject.get("chatId")));
            case "group" -> entity.setGroup(GroupDB.getGroups().get((String) jsonObject.get("group")));
            case "lastPinnedMessage" -> {
                String lastPinnedMessageStr = (String) jsonObject.get("lastPinnedMessage");
                Integer lastPinnedMessage = lastPinnedMessageStr.equalsIgnoreCase("null") ? null : Integer.parseInt((String) jsonObject.get("lastPinnedMessage"));
                entity.setLastPinnedMessageId(lastPinnedMessage);
            }
            case "role" -> entity.setRole(UserRole.getUserRoleFromString((String) jsonObject.get("role")));
            case "state" -> entity.setState(UserState.getUserStateFromString((String) jsonObject.get("state")));
            case "areInBroadcastGroup" -> entity.setAreInBroadcastGroup((Boolean) jsonObject.get("areInBroadcastGroup"));
            case "lastSentMessage" -> entity.setLastSentMessage((long) jsonObject.get("lastSentMessage"));
        }
    }
}
