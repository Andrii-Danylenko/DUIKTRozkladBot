package org.rozkladbot.handlers;

import org.rozkladbot.entities.User;
import org.rozkladbot.utils.GroupDB;
import org.rozkladbot.utils.JSONWriterImpl;
import org.rozkladbot.utils.Requester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminCommands {
    private static JSONWriterImpl jsonWriter = new JSONWriterImpl();
    public static void getAllCommands() {
        System.out.println("""
                           Головне меню адмін-панелі.
                           Команди:
                           /viewUsers - подивитися підключених юзерів
                           /synchronize [ключі] - оновити офлайн-файли
                           /terminateSession - закриває сесію бота, попередньо оновлюючи всі офлайн-дані
                           /commands - показати усі команди
                           /forceFetch - примусово оновити дані груп
                           """);
    }
    public static String viewUsers() {
        List<User> users = new ArrayList<>(ResponseHandler.getUsers().values());
        StringBuilder builder = new StringBuilder("Користувачі, які на даний момент підключені до бота:\n")
                .append("=======================").append('\n');
        users.forEach(user -> builder.append(user).append("=======================").append('\n'));
        return users.isEmpty() ? builder.append("Немає жодного користувача.").toString() : builder.toString();
    }

    public static void synchronize(String...params) {
        if (params.length == 2 && params[1].equalsIgnoreCase("-all")) {
            jsonWriter.dumpSchedule();
            jsonWriter.serializeUsers();
            return;
        }
        for (int i = 1; i < params.length; i++) {
            switch (params[i]) {
                case "-s" -> jsonWriter.dumpSchedule();
                // TODO: Добавить возможность сохранять пользователей и т.д.
                case "-u" -> jsonWriter.serializeUsers();
            }
        }
    }
    public static void forceFetch() {
        GroupDB.fetchGroups();
    }
}
