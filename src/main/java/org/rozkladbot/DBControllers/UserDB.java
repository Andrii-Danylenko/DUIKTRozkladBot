package org.rozkladbot.DBControllers;

import org.json.simple.parser.ParseException;
import org.rozkladbot.entities.User;
import org.rozkladbot.utils.FileUtils;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository("UserDB")
public class UserDB {
    private static volatile Map<Long, User> users = new ConcurrentHashMap<>();

    public UserDB() {

    }

    public static String serialize(User user) {
        return """
                   {
                       "chatId": "%s",
                       "group": "%s",
                       "faculty": "%s",
                       "course": "%s",
                       "state": "%s",
                       "role": "%s",
                       "lastPinnedMessage": "%s"
                   }"""
                .formatted(
                           user.getChatID(),
                           user.getGroup(),
                           user.getFaculty(),
                           user.getCourse(),
                           user.getState(),
                           user.getRole(),
                           user.getLastPinnedMessageId() == null ? "null" : user.getLastPinnedMessageId());

    }

    public static Map<Long, User> getAllUsers() {
        return users;
    }
    public static void updateUsersFromFile() {
        try {
            users = FileUtils.deserializeUsers();
        } catch (IOException exception) {
            System.out.println("Помилка під час виконання.");
        } catch (ParseException exception) {
            System.out.println("Помилка під час парсингу json файлу.");
        }
    }
}
