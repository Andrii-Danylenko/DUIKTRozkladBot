package org.rozkladbot.handlers;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.CommandFlags;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.DayOfWeek;
import org.rozkladbot.entities.DelayedCommand;
import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;
import org.rozkladbot.factories.KeyBoardFactory;
import org.rozkladbot.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.rozkladbot.constants.UserState.*;

@Component("ResponseHandler")
public class ResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);
    private final SilentSender sender;
    private final Lock lock = new ReentrantLock();
    private static SilentSender SILENT_SENDER_IMPORT;

    public ResponseHandler(SilentSender sender) {
        this.sender = sender;
        ResponseHandler.SILENT_SENDER_IMPORT = this.sender;
    }

    public synchronized void replyToStart(long chatId) {
        try {
            User user = new User(chatId, NULL_GROUP);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("""
                    Привіт! Я бот, який дозволяє отримувати розклад прямо в телеграм!
                    Що хочеш зробити?
                    %s
                    """.formatted(UserCommands.getMenu()));
            sender.execute(message);
            UserDB.getAllUsers().put(chatId, user);
            log.info("Розпочато діалог з користувачем з id {}", chatId);
        } catch (Exception exception) {
            log.error("""
                    Виникла помилка під час створення діалогу із користувачем з id {}.
                    Повідомлення помилки: {}
                    """, chatId, exception.getMessage());
        }
    }


    public boolean userIsActive(long chatId) {
        return UserDB.getAllUsers().containsKey(chatId);
    }

    public void replyToButtons(Long chatId, Update update) {
        UserDB.getAllUsers().get(chatId).getLastMessages().addLast(update.getMessage());
        if (UserDB.getAllUsers().get(chatId).getState() == UserState.NULL_GROUP || update.getMessage().getText().equalsIgnoreCase("Змінити групу") && UserDB.getAllUsers().get(chatId).getState() != null) {
            UserDB.getAllUsers().get(chatId).setGroup(null);
            UserDB.getAllUsers().get(chatId).setState(SETTINGS);
        }
        if (UserDB.getAllUsers().get(chatId).getState() == AWAITING_INPUT) {
            if (update.getMessage().getText().equalsIgnoreCase("Так")) {
                finishRegistration(chatId);
            } else if (update.getMessage().getText().equalsIgnoreCase("Ні")) {
                UserDB.getAllUsers().get(chatId).setState(SETTINGS);
            }
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/menu") ||
                update.getMessage().getText().equalsIgnoreCase("/menu@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(MAIN_MENU);
        } else if (update.getMessage().getText().equalsIgnoreCase("/settings") ||
                update.getMessage().getText().equalsIgnoreCase("/settings@rozkad_bot") || UserDB.getAllUsers().get(chatId).getGroup() == null) {
            if (GroupDB.getGroups().containsKey(update.getMessage().getText().toUpperCase())) {
                UserDB.getAllUsers().get(chatId).setState(AWAITING_INPUT);
            } else UserDB.getAllUsers().get(chatId).setState(SETTINGS);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/week") ||
                update.getMessage().getText().equalsIgnoreCase("/week@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(AWAITING_THIS_WEEK_SCHEDULE);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/nextWeek") ||
                update.getMessage().getText().equalsIgnoreCase("/nextWeek@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(AWAITING_NEXT_WEEK_SCHEDULE);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/day") ||
                update.getMessage().getText().equalsIgnoreCase("/day@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(AWAITING_THIS_DAY_SCHEDULE);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/nextDay") ||
                update.getMessage().getText().equalsIgnoreCase("/nextDay@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(AWAITING_NEXT_DAY_SCHEDULE);
        }
        else if (update.getMessage().getText().equalsIgnoreCase("/custom") ||
                update.getMessage().getText().equalsIgnoreCase("/custom@rozkad_bot")) {
            UserDB.getAllUsers().get(chatId).setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
        }
        switch (UserDB.getAllUsers().get(chatId).getState()) {
            case MAIN_MENU -> getMenu(chatId);
            case AWAITING_THIS_WEEK_SCHEDULE,
                    AWAITING_NEXT_DAY_SCHEDULE,
                    AWAITING_THIS_DAY_SCHEDULE,
                    AWAITING_NEXT_WEEK_SCHEDULE -> getSchedule(chatId);
            case AWAITING_CUSTOM_SCHEDULE_INPUT -> getCustomSchedulePrepare(chatId);
            case AWAITING_CUSTOM_SCHEDULE -> splitAndSend(chatId, update);
            case SETTINGS -> getSettings(chatId);
            case AWAITING_INPUT -> registerUser(chatId, update);
        }
    }

    public void getMenu(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(UserCommands.getMenu());
        sender.execute(sendMessage);
    }

    public synchronized void getSchedule(long chatId) {
        log.info("Користувач з id {} увійшов у getSchedule()", chatId);
        User user = UserDB.getAllUsers().get(chatId);
        if (user.getGroup() == null) {
            user.setState(NULL_GROUP);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.setReplyMarkup(KeyBoardFactory.deleteKeyBoard());
        sendMessage.setParseMode("html");
        DelayedCommand delayedCommand = new DelayedCommand(user, CommandFlags.IDLE);
        String scheduleType = "за власними параметрами";
        try {
            switch (user.getState()) {
                case AWAITING_THIS_WEEK_SCHEDULE -> {
                    log.info("""
                            Розпочато спробу отримання розкладу на цей тиждень
                            для користувача з id {}
                            """, chatId);
                    scheduleType = "на цей тиждень";
                    sendMessage.setText(UserCommands.getThisWeekSchedule(user));
                    delayedCommand.setDelayedCommand(CommandFlags.THIS_WEEK_SCHEDULE);
                    sender.execute(sendMessage);
                    log.info("""
                            Успішно завершено спробу отримання розкладу на цей тиждень
                            для користувача з id {}
                            """, chatId);
                }
                case AWAITING_NEXT_WEEK_SCHEDULE -> {
                    log.info("""
                            Розпочато спробу отримання розкладу на наступний тиждень
                            для користувача з id {}
                            """, chatId);
                    scheduleType = "на наступний тиждень";
                    sendMessage.setText(UserCommands.getNextWeekSchedule(user));
                    delayedCommand.setDelayedCommand(CommandFlags.NEXT_WEEK_SCHEDULE);
                    sender.execute(sendMessage);
                    log.info("""
                            Успішно завершено спробу отримання розкладу на цей наступний
                            для користувача з id {}
                            """, chatId);
                }
                case AWAITING_THIS_DAY_SCHEDULE -> {
                    log.info("""
                            Розпочато спробу отримання розкладу на цей день
                            для користувача з id {}
                            """, chatId);
                    scheduleType = "на сьогодні";
                    sendMessage.setText(UserCommands.getThisDaySchedule(user));
                    delayedCommand.setDelayedCommand(CommandFlags.TODAY_SCHEDULE);
                    sender.execute(sendMessage);
                    log.info("""
                            Успішно завершено спробу отримання розкладу на цей день
                            для користувача з id {}
                            """, chatId);
                }
                case AWAITING_NEXT_DAY_SCHEDULE -> {
                    log.info("""
                            Розпочато спробу отримання розкладу на наступний день
                            для користувача з id {}
                            """, chatId);
                    scheduleType = "на завтра";
                    sendMessage.setText(UserCommands.getTomorrowSchedule(user));
                    delayedCommand.setDelayedCommand(CommandFlags.NEXT_DAY_SCHEDULE);
                    sender.execute(sendMessage);
                    log.info("""
                            Успішно завершено спробу отримання розкладу на наступний день
                            для користувача з id {}
                            """, chatId);
                }
            }
        } catch (IOException exception) {
            log.error("""
                    Виникла помилка під час отримання розкладу для користувача з id {}.
                    Повідомлення помилки: {}
                       """, chatId, exception.getMessage());
            if (!DelayedCommandsHandler.getDelayedCommands().contains(delayedCommand)) {
                DelayedCommandsHandler.addDelayedCommand(delayedCommand);
                sendMessage.setText("""
                        Не вдалося отримати розклад %s :(
                        Задача додалася до списку невиконаних завдань та буде зроблена.
                        Очікуйте!
                        """.formatted(scheduleType));
            }
        } finally {
            UserDB.getAllUsers().get(chatId).setState(IDLE);
        }
    }

    public void getCustomSchedulePrepare(long chatId) {
        log.info("""
                Розпочато підготовку до отримання розкладу за власними параметрами
                для користувача з id {} в методі getCustomSchedulePrepare()
                """, chatId);
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
        UserDB.getAllUsers().get(chatId).setState(AWAITING_CUSTOM_SCHEDULE);

    }

    public void splitAndSend(long chatId, Update update) {
        log.info("""
                Розпочато спробу отримати дані для отримання розкладу за власними параметрами
                для користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            log.info("""
                    Розпочато спробу розпарсити ввід користувача з id {}.
                    Повідомлення для парсингу: {}
                    """, chatId, update.getMessage());
            String[] data = update.getMessage().getText().split("\\sз\\s|\\sпо\\s");
            if (data.length != 3) throw new Exception("Довжина масива менша за 3(%d)".formatted(data.length));
            sendMessage.enableHtml(true);
            log.info("""
                    Успішно завершено спробу розпарсити ввід користувача з id {}
                    """, chatId);
            log.info("""
                    Розпочато спробу отримати розклад для користувача з id {}.
                    Дата для отримання: з {} по {}
                    """, chatId, data[1], data[2]);
            Table response = DAOImpl.getInstance().getCustomTable(String.valueOf(GroupDB.getGroups().get(data[0])), data[1], DateUtils.toString(DateUtils.parseDate(data[2]).plusDays(1)), String.valueOf(data[0].charAt(data[0].indexOf('-') + 1)));
            log.info("""
                    Успішно завершено спробу отримати розклад для користувача з id {}.
                    Дата для отримання: з {} по {}
                    """, chatId, data[1], data[2]);
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
            UserDB.getAllUsers().get(chatId).setState(IDLE);
            log.info("""
                    Успішно завершено спробу отримати дані для отримання розкладу за власними параметрами
                    для користувача з id {}
                    """, chatId);
        } catch (Exception exception) {
            log.info("""
                    Помилка під час спроби розпарсити ввід користувача з id {}.
                    Повідомлення для парсингу: {}.
                    Повідомлення помилки: {}
                    """, chatId, update.getMessage(), exception.getMessage());
            sendMessage.setText("Дані не є коректними! Спробуйте ще раз.");
            UserDB.getAllUsers().get(chatId).setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
            sender.execute(sendMessage);
        }
    }

    public void getSettings(long chatId) {
        log.info("""
                Розпочато отримання даних користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (UserDB.getAllUsers().get(chatId).getGroup() == null) {
            StringBuffer stringBuffer = new StringBuffer(UserDB.getAllUsers().get(chatId).getLastPinnedMessageId() == null ? "Схоже, що ви не зареєстровані.\n" :
                    "Для того, щоб змінити дані,\n");
            stringBuffer.append("""
                    Будь-ласка, напишіть Вашу группу.
                    Групи, які наразі підтримуються:
                    """);
            GroupDB.getGroups().keySet().stream().sorted().forEach(group -> stringBuffer.append("<b>").append(group).append("</b>").append('\n'));
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(KeyBoardFactory.getGroupsKeyboard());
            sendMessage.setText(stringBuffer.toString());
            UserDB.getAllUsers().get(chatId).setState(AWAITING_INPUT);
        } else {
            sendMessage.setText(UserCommands.getUserSettings(UserDB.getAllUsers().get(chatId)));
            sendMessage.setReplyMarkup(KeyBoardFactory.changeGroup());
        }
        sender.execute(sendMessage);
        log.info("""
                Закінчено отримання даних користувача {}
                """, chatId);
    }

    public void registerUser(long chatId, Update update) {
        log.info("""
                Розпочато реєстрацію користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        String group = update.getMessage().getText().toUpperCase();
        if (GroupDB.getGroups().containsKey(group)) {
            sendMessage.setText("Ваша група: %s?".formatted(group));
            sendMessage.setReplyMarkup(KeyBoardFactory.getYesOrNo());
        } else {
            sendMessage.setText("Такої групи не існує!");
            sendMessage.setReplyMarkup(KeyBoardFactory.deleteKeyBoard());
        }
        sender.execute(sendMessage);
    }

    public void finishRegistration(long chatId) {
        log.info("""
                Розпочато спробу завершити рєстрацію користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        UserCommands.finishRegistration(UserDB.getAllUsers().get(chatId));
        sendMessage.setText(UserDB.getAllUsers().get(chatId).getState() == null ? "Ви були успішно зареєстровані!" : "Ви успішно змінили налаштування групи!");
        sendMessage.setReplyMarkup(KeyBoardFactory.deleteKeyBoard());
        sender.execute(sendMessage);
        sendMessage.setText(UserCommands.getMenu());
        sender.execute(sendMessage);
        UserDB.getAllUsers().get(chatId).setState(IDLE);
        log.info("""
                Завершено спробу зареєструвати користувача з id {}
                """, chatId);
    }

    // TODO: изменить значения
    @Scheduled(cron = "0 0 15 * * *")
    public void broadcastAndPinTomorrowSchedule() {
        log.warn("Час на сервері: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy")));
        log.info("Розпочато широкомовну розсилку розписів на завтра");
        lock.lock();
        try {
            for (Map.Entry<Long, User> buff : UserDB.getAllUsers().entrySet()) {
                User user = buff.getValue();
                Long chatId = buff.getKey();
                if (user.getLastPinnedMessageId() != null) {
                    log.info("Розпочато видалення минулого закріпленого повідомлення користувача з id {}", chatId);
                    UnpinChatMessage unpinMessage = new UnpinChatMessage(chatId.toString(), user.getLastPinnedMessageId());
                    sender.execute(unpinMessage);
                    log.info("Завершено видалення минулого закріпленого повідомлення користувача з id {}", chatId);
                }
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                try {
                    sendMessage.enableHtml(true);
                    sendMessage.setParseMode("html");
                    sendMessage.setText(UserCommands.getTomorrowSchedule(user));
                    Optional<Message> message = sender.execute(sendMessage);
                    if (message.isPresent()) {
                        log.info("Розпочато закріплення повідомлення користувача з id {}", chatId);
                        PinChatMessage pinMessage = new PinChatMessage(chatId.toString(), message.get().getMessageId());
                        sender.execute(pinMessage);
                        user.setLastPinnedMessageId(message.get().getMessageId());
                        log.info("Успішно завершено закріплення повідомлення користувача з id {}", chatId);
                    } else throw new IOException("Не вдалося отримати розклад. API впало?");
                } catch (IOException exception) {
                    log.error("""
                            Виникла помилка під час широкомовної розсилки розписів на завтра.
                            Повідомлення помилки: {}
                            """, exception.getMessage());
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
            log.info("Успішно закінчено широкомовну розсилку розписів на завтра");
        }
    }

    public static void sendMessage(SilentSender sender, String params, String message) {
        Set<Object> values = parseParams(params);
        SendMessage sendMessage = new SendMessage();
        Set<Long> presentIds = UserDB.getAllUsers().keySet();
        if (values.size() != 1) {
            values.stream().filter(value -> value instanceof Long).filter(value -> presentIds.contains((Long) value)).forEach(send -> {
                sendMessage.setText(message);
                sendMessage.setChatId(Long.parseLong(send.toString()));
                sender.execute(sendMessage);
            });
        } else {
            presentIds.forEach(send -> {
                sendMessage.setText(message);
                sendMessage.setChatId(send);
                sender.execute(sendMessage);
            });
        }
    }

    private static Set<Object> parseParams(String params) {
        String[] splitted = params.split(" ");
        if (splitted[1].equalsIgnoreCase("-all")) {
            return new HashSet<>() {{
                add(splitted[1]);
            }};
        }
        Set<Object> userIds = new HashSet<>();
        Arrays.stream(params.split(" ")).forEach(number -> {
            try {
                userIds.add(Long.parseLong(number));
            } catch (NumberFormatException exception) {
                return;
            }
        });
        return userIds;
    }
    public static SilentSender getSilentSender() {
        return SILENT_SENDER_IMPORT;
    }

}
