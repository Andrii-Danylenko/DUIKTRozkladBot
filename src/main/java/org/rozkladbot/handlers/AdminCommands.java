package org.rozkladbot.handlers;

import org.rozkladbot.entities.User;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.utils.FileUtils;
import org.rozkladbot.DBControllers.UserDB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommands {
    private static FileUtils jsonWriter = new FileUtils();
    public static void getAllCommands() {
        System.out.println("""
                           Головне меню адмін-панелі.
                           Команди:
                           /viewUsers - подивитися підключених юзерів
                           /synchronize [ключі: -u, -s, -all] - оновити офлайн-файли
                           /terminateSession - закриває сесію бота, попередньо оновлюючи всі офлайн-дані
                           /commands - показати усі команди
                           /forceFetch - примусово оновити дані груп
                           /sendMessage [user_id(s) або -all]- відправити повідомлення вибраним користувачам
                           """);
    }
    public static String viewUsers() {
        List<User> users = new ArrayList<>(UserDB.getAllUsers().values());
        StringBuilder builder = new StringBuilder("Користувачі, які на даний момент підключені до бота:\n")
                .append("=======================").append('\n');
        users.forEach(user -> builder.append(user).append("=======================").append('\n'));
        return users.isEmpty() ? builder.append("Немає жодного користувача.").toString() : builder.toString();
    }

    public static void synchronize(String...params) throws IOException {
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
    public static void sendMessage(String params) {
        ResponseHandler.sendMessage(ResponseHandler.getSilentSender(), params, parseMessage(params));
    }
    public static void forceFetch() {
        GroupDB.fetchGroups();
    }
    private static String parseMessage(String command) {
        Pattern pattern = Pattern.compile("\"([^\"]*)\"|\\S+");
        Matcher matcher = pattern.matcher(command);
        ArrayList<String> parts = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                parts.add(matcher.group(1));
            } else {
                parts.add(matcher.group());
            }
        }
        return parts.get(parts.size() - 1);
    }

    public static void getCurrentDate() {
        System.out.println();
    }
}
