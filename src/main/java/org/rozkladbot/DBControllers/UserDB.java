package org.rozkladbot.DBControllers;

import org.json.simple.parser.ParseException;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.User;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.data.AbstractJsonDeserializer;
import org.rozkladbot.utils.data.UserUtils;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository("UserDB")
public class UserDB {
    private static volatile Map<Long, User> users = new ConcurrentHashMap<>();
    private static final AbstractJsonDeserializer<Long, User> userDeserializer = new UserUtils();
    private static final ConsoleLineLogger<UserDB> log = new ConsoleLineLogger<>(UserDB.class);
    public UserDB() {

    }

    public static String serialize(User user) {
        return """
                   {
                       "chatId": "%s",
                       "group": "%s",
                       "state": "%s",
                       "role": "%s",
                       "lastPinnedMessage": "%s",
                       "areInBroadcastGroup": %b,
                       "lastSentMessage": %d,
                       "userName": "%s"
                   }"""
                .formatted(
                           user.getChatID(),
                           user.getGroup() == null ? GroupDB.getGroups().get("ІСД-32") : user.getGroup().getGroupName(),
                           user.getState() == null ? UserState.IDLE : user.getState(),
                           user.getRole() == null ? UserRole.USER : user.getRole(),
                           user.getLastPinnedMessageId() == null ? "null" : user.getLastPinnedMessageId(),
                           user.isAreInBroadcastGroup(),
                           user.getLastSentMessage(),
                           user.getUserName() == null ? "" : user.getUserName());

    }

    public static Map<Long, User> getAllUsers() {
        return users;
    }
    public static void updateUsersFromFile() {
        try {
            users = userDeserializer.deserialize("users",
                    "usersList.json","users",
                    "chatId",
                    "group",
                    "lastPinnedMessage",
                    "role",
                    "state",
                    "areInBroadcastGroup",
                    "lastSentMessage",
                    "userName");
        } catch (IOException exception) {
            System.out.println("Помилка під час виконання.");
            exception.printStackTrace();
        } catch (ParseException exception) {
            System.out.println("Помилка під час парсингу json файлу.");
        }
    }
    public static void removeUserById(long chatId) {
        log.info("Спроба видалили користувача...");
        users.remove(chatId);
        log.success("Спроба видалили користувача виконана успішно!");
    }
}
