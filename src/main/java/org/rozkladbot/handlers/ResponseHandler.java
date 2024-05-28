package org.rozkladbot.handlers;

import org.rozkladbot.constants.CommandFlags;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.DayOfWeek;
import org.rozkladbot.entities.DelayedCommand;
import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;
import org.rozkladbot.factories.KeyBoardFactory;
import org.rozkladbot.utils.DateUtils;
import org.rozkladbot.utils.GroupDB;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.rozkladbot.constants.UserState.*;
@Component("ResponseHandler")
// TODO: Вот бы log4j вставить, но пока впадлу
public class ResponseHandler {
    private final SilentSender sender;
    private static volatile Map<Long, User> users = new ConcurrentHashMap<>();
    private final Lock lock = new ReentrantLock();

    public ResponseHandler(SilentSender sender) {
        this.sender = sender;
    }
    public synchronized void replyToStart(long chatId) {
        try {
            User user = new User(chatId, AWAITING_INPUT);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("""
                    Привіт! Я бот, який дозволяє отримувати розклад прямо в телеграм!
                    Що хочеш зробити?
                    %s
                    """.formatted(Commands.getMenu()));
            sender.execute(message);
            users.put(chatId, user);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    public boolean userIsActive(long chatId) {
        return users.containsKey(chatId);
    }

    public void replyToButtons(Long chatId, Update update) {
        users.get(chatId).getLastMessages().addLast(update.getMessage());
        if (users.get(chatId).getState() == AWAITING_INPUT) {
            if (update.getMessage().getText().equalsIgnoreCase("Так")) {
                finishRegistration(chatId);
            }
        }
        if (update.getMessage().getText().equalsIgnoreCase("Змінити групу") && users.get(chatId).getState() != null) {
            users.get(chatId).setGroup(null);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/menu") ||
                update.getMessage().getText().equalsIgnoreCase("/menu@rozkad_bot")) {
            users.get(chatId).setState(MAIN_MENU);
        } if (update.getMessage().getText().equalsIgnoreCase("/settings") ||
                update.getMessage().getText().equalsIgnoreCase("/settings@rozkad_bot")  || users.get(chatId).getGroup() == null) {
            if (GroupDB.getGroups().containsKey(update.getMessage().getText().toUpperCase())) {
                users.get(chatId).setState(AWAITING_INPUT);
            }
            else users.get(chatId).setState(SETTINGS);
        }
        if (update.getMessage().getText().equalsIgnoreCase("/week") ||
                update.getMessage().getText().equalsIgnoreCase("/week@rozkad_bot")) {
            users.get(chatId).setState(AWAITING_THIS_WEEK_SCHEDULE);
        } if (update.getMessage().getText().equalsIgnoreCase("/nextWeek") ||
                update.getMessage().getText().equalsIgnoreCase("/nextWeek@rozkad_bot")) {
            users.get(chatId).setState(AWAITING_NEXT_WEEK_SCHEDULE);
        } if (update.getMessage().getText().equalsIgnoreCase("/day") ||
                update.getMessage().getText().equalsIgnoreCase("/day@rozkad_bot")) {
            users.get(chatId).setState(AWAITING_THIS_DAY_SCHEDULE);
        } if (update.getMessage().getText().equalsIgnoreCase("/nextDay") ||
                update.getMessage().getText().equalsIgnoreCase("/nextDay@rozkad_bot")) {
            users.get(chatId).setState(AWAITING_NEXT_DAY_SCHEDULE);
        }
        if (update.getMessage().getText().equalsIgnoreCase("/custom") ||
                update.getMessage().getText().equalsIgnoreCase("/custom@rozkad_bot")) {
            users.get(chatId).setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
        }
        switch (users.get(chatId).getState()) {
            case MAIN_MENU -> getMenu(chatId);
            case AWAITING_THIS_WEEK_SCHEDULE -> getThisWeekSchedule(chatId);
            case AWAITING_NEXT_WEEK_SCHEDULE -> getNextWeekSchedule(chatId);
            case AWAITING_THIS_DAY_SCHEDULE -> getTodaySchedule(chatId);
            case AWAITING_NEXT_DAY_SCHEDULE -> getTomorrowSchedule(chatId);
            case SETTINGS -> getSettings(chatId);
            case AWAITING_INPUT -> registerUser(chatId, update);
            case AWAITING_CUSTOM_SCHEDULE_INPUT -> getCustomSchedulePrepare(chatId);
            case AWAITING_CUSTOM_SCHEDULE -> splitAndSend(chatId, update);
        }
    }
    public void getMenu(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(Commands.getMenu());
        sender.execute(sendMessage);
    }
    public void getThisWeekSchedule(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            sendMessage.enableHtml(true);
            sendMessage.setParseMode("html");
            sendMessage.setText(Commands.getThisWeekSchedule(users.get(chatId)));
        } catch (IOException exception) {
            DelayedCommandsHandler.addDelayedCommand(new DelayedCommand(users.get(chatId), CommandFlags.THIS_WEEK_SCHEDULE));
            sendMessage.setText("""
                    Не вдалося отримати розклад :(
                    Задача додалася до списку невиконаних завдань та буде зроблена.
                    Очікуйте!
                    """);
        }
        sender.execute(sendMessage);
        users.get(chatId).setState(IDLE);
    }
    public void getNextWeekSchedule(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            sendMessage.enableHtml(true);
            sendMessage.setParseMode("html");
            sendMessage.setText(Commands.getNextWeekSchedule(users.get(chatId)));
        } catch (IOException exception) {
            DelayedCommandsHandler.addDelayedCommand(new DelayedCommand(users.get(chatId), CommandFlags.NEXT_WEEK_SCHEDULE));
            sendMessage.setText("""
                    Не вдалося отримати розклад :(
                    Задача додалася до списку невиконаних завдань та буде зроблена.
                    Очікуйте!
                    """);
        }
        sender.execute(sendMessage);
        users.get(chatId).setState(IDLE);
    }
    public void getTodaySchedule(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            sendMessage.enableHtml(true);
            sendMessage.setParseMode("html");
            sendMessage.setText(Commands.getThisDaySchedule(users.get(chatId)));
        } catch (IOException exception) {
            DelayedCommandsHandler.addDelayedCommand(new DelayedCommand(users.get(chatId), CommandFlags.TODAY_SCHEDULE));
            sendMessage.setText("""
                    Не вдалося отримати розклад :(
                    Задача додалася до списку невиконаних завдань та буде зроблена.
                    Очікуйте!
                    """);
        }
        sender.execute(sendMessage);
        users.get(chatId).setState(IDLE);
    }
    public void getTomorrowSchedule(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            sendMessage.enableHtml(true);
            sendMessage.setParseMode("html");
            sendMessage.setText(Commands.getTomorrowSchedule(users.get(chatId)));
        } catch (IOException exception) {
            DelayedCommandsHandler.addDelayedCommand(new DelayedCommand(users.get(chatId), CommandFlags.NEXT_DAY_SCHEDULE));
            sendMessage.setText("""
                    Не вдалося отримати розклад :(
                    Задача додалася до списку невиконаних завдань та буде зроблена.
                    Очікуйте!
                    """);
        }
        sender.execute(sendMessage);
        users.get(chatId).setState(IDLE);
    }
    public void getCustomSchedulePrepare(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.setText("""
                            Для того, щоб отримати власні дані,
                            Використовуйте синтаксис:
                            <b>[група]</b> з <b>[дата початку]</b> по <b>[дата кінця]</b>
                            Наприклад: <b>ІСД-22</b> з <b>19.04.2024</b> по <b>29.04.2024</b>
                            """);
        sender.execute(sendMessage);
        users.get(chatId).setState(AWAITING_CUSTOM_SCHEDULE);

    }
    public void splitAndSend(long chatId, Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            String[] data = update.getMessage().getText().split("\\sз\\s|\\sпо\\s");
            if (data.length != 3) throw new Exception();
            sendMessage.enableHtml(true);
            Table response = DAOImpl.getInstance().getCustomTable(String.valueOf(GroupDB.getGroups().get(data[0])), data[1], DateUtils.toString(DateUtils.parseDate(data[2]).plusDays(1)), String.valueOf(data[0].charAt(data[0].indexOf('-') + 1)));
            int weekDelimiter = 0;
            int finish = response.getTable().size();
            int globalCounter = 0;
            StringBuffer buffer = new StringBuffer();
            for (DayOfWeek day : response.getTable()) {
                weekDelimiter++;
                globalCounter++;
                if (globalCounter == finish) {
                    sendMessage.setText(buffer.toString());
                    sender.execute(sendMessage);
                }
                if (weekDelimiter == 7) {
                    sendMessage.setText(buffer.toString());
                    sender.execute(sendMessage);
                    weekDelimiter = 0;
                    buffer.delete(0, buffer.length() - 1);
                }
                buffer.append(day.toStringIfMany()).append('\n');
            }
            users.get(chatId).setState(IDLE);
        } catch (Exception exception) {
            sendMessage.setText("Дані не є коректними! Спробуйте ще раз.");
            users.get(chatId).setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
            sender.execute(sendMessage);
        }
    }
    public void getSettings(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (users.get(chatId).getGroup() == null) {
            StringBuffer stringBuffer = new StringBuffer(users.get(chatId).getLastPinnedMessageId() == null ? "Схоже, що ви не зареєстровані.\n" :
                    "Для того, щоб змінити дані,\n");
            stringBuffer.append("""
                    Будь-ласка, напишіть Вашу группу.
                    Групи, які наразі підтримуються:
                    """);
            GroupDB.getGroups().keySet().forEach(group -> stringBuffer.append("<b>").append(group).append("</b>").append('\n'));
            sendMessage.enableHtml(true);
            sendMessage.setText(stringBuffer.toString());
            users.get(chatId).setState(AWAITING_INPUT);
        } else {
            sendMessage.setText(Commands.getUserSettings(users.get(chatId)));
            sendMessage.setReplyMarkup(KeyBoardFactory.changeGroup());
        }
        sender.execute(sendMessage);
    }
    public void registerUser(long chatId, Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        String group = update.getMessage().getText().toUpperCase();
        if (GroupDB.getGroups().containsKey(group)) {
            sendMessage.setText("Ваша група: %s?".formatted(group));
            sendMessage.setReplyMarkup(KeyBoardFactory.getYesOrNo());
        }
        else {
            sendMessage.setText("Такої групи не існує!");
            sendMessage.setReplyMarkup(KeyBoardFactory.deleteKeyBoard());
        }
        sender.execute(sendMessage);
    }
    public void finishRegistration(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        Commands.finishRegistration(users.get(chatId));
        sendMessage.setText(users.get(chatId).getState() == null ? "Ви були успішно зареєстровані!" : "Ви успішно змінили налаштування групи!");
        sendMessage.setReplyMarkup(KeyBoardFactory.deleteKeyBoard());
        sender.execute(sendMessage);
        sendMessage.setText(Commands.getMenu());
        sender.execute(sendMessage);
        users.get(chatId).setState(IDLE);
    }
    // TODO: изменить значения
    @Scheduled(cron = "0 0 1 * * *")
    public void broadcastAndPinTomorrowSchedule() {
        lock.lock();
        try {
            for (Map.Entry<Long, User> buff : users.entrySet()) {
                User user = buff.getValue();
                Long chatId = buff.getKey();
                if (user.getLastPinnedMessageId() != null) {
                    UnpinChatMessage unpinMessage = new UnpinChatMessage(chatId.toString(), user.getLastPinnedMessageId());
                    sender.execute(unpinMessage);
                }
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                try {
                    sendMessage.enableHtml(true);
                    sendMessage.setParseMode("html");
                    sendMessage.setText(Commands.getTomorrowSchedule(user));
                    Optional<Message> message = sender.execute(sendMessage);
                    if (message.isPresent()) {
                        PinChatMessage pinMessage = new PinChatMessage(chatId.toString(), message.get().getMessageId());
                        sender.execute(pinMessage);
                        user.setLastPinnedMessageId(message.get().getMessageId());
                    } else throw new IOException();
                } catch (IOException exception) {
                    DelayedCommandsHandler.addDelayedCommand(new DelayedCommand(user, CommandFlags.NEXT_DAY_SCHEDULE));
                    sendMessage.setText("""
                    Не вдалося отримати розклад :(
                    Задача додалася до списку невиконаних завдань та буде зроблена.
                    Очікуйте!
                    """);
                    sender.execute(sendMessage);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    public static Map<Long, User> getUsers() {
        return ResponseHandler.users;
    }
}
