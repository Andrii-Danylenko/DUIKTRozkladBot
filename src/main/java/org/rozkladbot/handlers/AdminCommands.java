package org.rozkladbot.handlers;

import org.rozkladbot.entities.User;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.MessageSender;
import org.rozkladbot.utils.data.UserUtils;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.utils.schedule.ScheduleDumper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component("AdminCommands")
public class AdminCommands {
    private static final ConsoleLineLogger<AdminCommands> log = new ConsoleLineLogger<>(AdminCommands.class);
    private static final UserUtils userUtils = new UserUtils();
    private static final ScheduleDumper scheduleDumper = new ScheduleDumper();
    private static MessageSender messageSender;
    @Autowired
    public AdminCommands(MessageSender messageSender) {
        AdminCommands.messageSender = messageSender;
    }
    public static String getAllCommands() {
        return  """
                           /viewUsers - подивитися підключених юзерів
                           /synchronize [ключі: -u, -s, -all] - оновити офлайн-файли
                           /terminateSession - закриває сесію бота, попередньо оновлюючи всі офлайн-дані
                           /forceFetch - примусово оновити дані груп
                           /sendMessage [user_id(s) або -all]- відправити повідомлення вибраним користувачам
                           /echo [user_ids або -all] - почати пересилання повідомлень
                           /echoStop [user_ids або -all] - закінчити пересилання повідомлень (Також пересилає групу повідомлень)
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
        for (User user : users) {
            try {
                log.info("Отримую дані користувача з id: %d%n".formatted(user.getChatID()));
                sendMessage.setText(user.toString());
                sender.execute(sendMessage);
                log.success("Успішно отримав дані користувача %s".formatted(user.getUserName()));
            } catch (NullPointerException exception) {
                log.error("Не вдалося отримати дані користувача!");
            }
        }
    }

    public static void synchronize(String...params) throws IOException {
        for (int i = 1; i < params.length; i++) {
            try {
                switch (params[i]) {
                    case "-all" -> {
                        log.info("Починаю синхронізацію розкладів...");
                        scheduleDumper.dumpSchedule(true);
                        log.success("Закінчив синхронізацію розкладів...");
                        log.info("Починаю синхронізацію користувачів...");
                        userUtils.serialize();
                        log.success("Закінчив синхронізацію користувачів...");
                        log.success("Усі дані оновлено успішно!");
                        return;
                    }
                    case "-s" -> {
                        log.info("Починаю синхронізацію розкладів...");
                        scheduleDumper.dumpSchedule(true);
                        log.success("Закінчив синхронізацію розкладів...");
                    }
                    case "-u" -> {
                        log.info("Починаю синхронізацію користувачів...");
                        userUtils.serialize();
                        log.success("Закінчив синхронізацію користувачів...");
                    }
                }
            } catch (Exception exception) {
                log.error("Виникла помилка в методі synchronize. Привід: " + exception.getCause());
            }
        }
    }
    public static void sendMessage(String params) {
        messageSender.sendMessage(params, parseMessage(params));
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
    public static void terminateSession() {
        try {
            synchronize("-all");
        } catch (IOException exception) {
            log.error("Не вдалося синхронізувати офлайн файли.");
            exception.printStackTrace();
        }
        finally {
            System.exit(0);
        }
    }
    public static void deleteUserById(String message) {
        log.info("Спроба видалили користувача...");
        long id = Long.parseLong(message.split("\\s")[1]);
        UserDB.removeUserById(id);
        log.success("Спроба видалили користувача з ID %d виконана успішно!".formatted(id));
    }
}
