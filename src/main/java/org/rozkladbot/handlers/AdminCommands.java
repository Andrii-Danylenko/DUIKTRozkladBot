package org.rozkladbot.handlers;

import org.rozkladbot.entities.User;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.utils.FileUtils;
import org.rozkladbot.DBControllers.UserDB;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminCommands {
    private static FileUtils fileUtils = new FileUtils();
    public static String getAllCommands() {
        return  """
                           /viewUsers - подивитися підключених юзерів
                           /synchronize [ключі: -u, -s, -all] - оновити офлайн-файли
                           /terminateSession - закриває сесію бота, попередньо оновлюючи всі офлайн-дані
                           /forceFetch - примусово оновити дані груп
                           /sendMessage [user_id(s) або -all]- відправити повідомлення вибраним користувачам
                           """;
    }
    public static void viewUsers(SilentSender sender, long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        List<User> users = new ArrayList<>(UserDB.getAllUsers().values());
        sendMessage.setText("""
                            Користувачі, які на даний момент підключені до бота:
                            """);
        sender.execute(sendMessage);
        if (users.isEmpty()) {
            sendMessage.setText("Немає жодного користувача.");
            sender.execute(sendMessage);
            return;
        }
        users.forEach(user -> {
            sendMessage.setText(user.toString());
            sender.execute(sendMessage);
        });
    }

    public static void synchronize(String...params) throws IOException {
        for (int i = 1; i < params.length; i++) {
            switch (params[i]) {
                case "-all" -> {
                    System.out.println("Починаю синхронізацію розкладів...");
                    fileUtils.dumpSchedule(true);
                    System.out.println("Закінчив синхронізацію розкладів...");
                    System.out.println("Починаю синхронізацію користувачів...");
                    fileUtils.serializeUsers();
                    System.out.println("Закінчив синхронізацію користувачів...");
                    System.out.println("Усі дані оновлено успішно!");
                    return;
                }
                case "-s" -> {
                    System.out.println("Починаю синхронізацію розкладів...");
                    fileUtils.dumpSchedule(true);
                    System.out.println("Закінчив синхронізацію розкладів...");
                    break;
                }
                case "-u" -> {
                    System.out.println("Починаю синхронізацію користувачів...");
                    fileUtils.serializeUsers();
                    System.out.println("Закінчив синхронізацію користувачів...");
                    break;
                }
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
        System.out.println(parts);
        return parts.get(parts.size() - 1);
    }
    public static void terminateSession() {
        try {
            synchronize("-all");
        } catch (IOException exception) {
            System.out.println("Не вдалося синхронізувати офлайн файли.");
            exception.printStackTrace();
        }
        finally {
            System.exit(0);
        }
    }
}
