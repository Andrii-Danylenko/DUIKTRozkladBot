package org.rozkladbot.handlers;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.EmojiList;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.DayOfWeek;
import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;
import org.rozkladbot.factories.KeyBoardFactory;
import org.rozkladbot.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.rozkladbot.constants.UserState.*;

@Component("ResponseHandler")
public class ResponseHandler {
    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);
    private static SilentSender sender;
    private final Lock lock = new ReentrantLock();
    private final AbilityBot abilityBot;

    public ResponseHandler(SilentSender sender, AbilityBot abilityBot) {
        ResponseHandler.sender = sender;
        this.abilityBot = abilityBot;
    }

    public synchronized void replyToStart(long chatId) {
        try {
            String welcomeMessage = "";
            if (!UserDB.getAllUsers().containsKey(chatId)) {
                User user = new User(chatId, NULL_GROUP);
                user.setRole(UserRole.USER);
                UserDB.getAllUsers().put(chatId, user);
                welcomeMessage = UserCommands.getMenu();
            }
            log.info("Розпочато діалог з користувачем з id {}", chatId);
            SendPhoto sendPhoto = getSendPhoto(chatId, welcomeMessage);
            abilityBot.execute(sendPhoto);
        } catch (Exception exception) {
            log.error("""
                    Виникла помилка під час створення діалогу із користувачем з id {}.
                    Повідомлення помилки: {}
                    """, chatId, exception.getMessage());
        }
    }

    private static SendPhoto getSendPhoto(long chatId, String caption) throws IOException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setParseMode("html");
        sendPhoto.setChatId(chatId);
        sendPhoto.setCaption(caption);
        sendPhoto.setReplyMarkup(KeyBoardFactory.getCommandsList());
        sendPhoto.setPhoto(new InputFile(new FileInputStream(Paths.get("funnyImages/image.jpg").toFile()), "meme"));
        return sendPhoto;
    }

    public boolean userIsActive(long chatId) {
        return UserDB.getAllUsers().containsKey(chatId);
    }

    public void replyToButtons(Long chatId, Update update) {
        lock.lock();
        User currentUser = UserDB.getAllUsers().get(chatId);
        String messageText = "";
        System.out.println(currentUser.getState());
        if (update.hasMessage()) {
            messageText = update.getMessage().getText();
            currentUser.getLastMessages().addLast(update.getMessage().getText());
        }
        lock.unlock();
        if (update.hasCallbackQuery()) {
            String callbackQueryText = update.getCallbackQuery().getData();
            if (currentUser.getState() == AWAITING_INPUT && "ТАК".equalsIgnoreCase(callbackQueryText)) {
                finishRegistration(currentUser, chatId);
            } else if ("НАЗАД".equalsIgnoreCase(callbackQueryText)) {
                    currentUser.setState(MAIN_MENU);
            } else if (currentUser.getState() == NULL_GROUP) {
                if ("НІ".equalsIgnoreCase(callbackQueryText)
                        || GroupDB.getGroups().containsKey(callbackQueryText)) {
                    currentUser.setState(AWAITING_INPUT);
                }
            } else if ("ЗГ".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(NULL_GROUP);
            } else if ("DAY".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(AWAITING_THIS_DAY_SCHEDULE);
            } else if ("NDAY".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(AWAITING_NEXT_DAY_SCHEDULE);
            } else if ("WEEK".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(AWAITING_THIS_WEEK_SCHEDULE);
            } else if ("NWEEK".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(AWAITING_NEXT_WEEK_SCHEDULE);
            } else if ("CUSTOM".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
            } else if ("НЛ".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setState(SETTINGS);
            } else if ("DIS".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setAreInBroadcastGroup(false);
            } else if ("ENA".equalsIgnoreCase(callbackQueryText)) {
                currentUser.setAreInBroadcastGroup(true);
            }
        }
        else {
            System.out.printf("Стан користувача в replyToButtons() на даний момент: %s%n", currentUser.getState());
            if ("/stop".equalsIgnoreCase(messageText)) {
                currentUser.setState(STOP);
            }
            if (currentUser.getState() == UserState.NULL_GROUP || "Змінити групу".equalsIgnoreCase(messageText) && currentUser.getState() != null) {
                currentUser.setState(AWAITING_INPUT);
            }
            if (currentUser.getState() == AWAITING_INPUT) {
                if ("Так".equalsIgnoreCase(messageText)) {
                    finishRegistration(currentUser, chatId);
                } else if ("Ні".equalsIgnoreCase(messageText)) {
                    currentUser.setState(SETTINGS);
                }
            } else if ("/menu".equalsIgnoreCase(messageText) ||
                    "/menu@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(MAIN_MENU);
            } else if ("/settings".equalsIgnoreCase(messageText) ||
                    "/settings@rozkad_bot".equalsIgnoreCase(messageText) || (currentUser.getGroup() == null)) {
                if (GroupDB.getGroups().containsKey(messageText.toUpperCase())) {
                    currentUser.setState(AWAITING_INPUT);
                } else currentUser.setState(SETTINGS);
            } else if ("/week".equalsIgnoreCase(messageText) ||
                    "/week@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(AWAITING_THIS_WEEK_SCHEDULE);
            } else if ("/nextWeek".equalsIgnoreCase(messageText) ||
                    "/nextWeek@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(AWAITING_NEXT_WEEK_SCHEDULE);
            } else if ("/day".equalsIgnoreCase(messageText) ||
                    "/day@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(AWAITING_THIS_DAY_SCHEDULE);
            } else if ("/nextDay".equalsIgnoreCase(messageText) ||
                    "/nextDay@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(AWAITING_NEXT_DAY_SCHEDULE);
            } else if ("/custom".equalsIgnoreCase(messageText) ||
                    "/custom@rozkad_bot".equalsIgnoreCase(messageText)) {
                currentUser.setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
            }
            if (currentUser.getState() != null && currentUser.getRole() == UserRole.ADMIN) {
                if (messageText.toLowerCase().startsWith("/sendmessage ")) {
                    currentUser.setState(ADMIN_SEND_MESSAGE);
                } else if (messageText.toLowerCase().startsWith("/synchronize ")) {
                    try {
                        AdminCommands.synchronize(messageText.split("\\s"));
                    } catch (IOException exception) {
                        System.err.println("Помилка в методі AdminCommands.synchronize()");
                        exception.printStackTrace();
                    } finally {
                        currentUser.setState(IDLE);
                    }
                } else if ("/viewUsers".equalsIgnoreCase(messageText)) {
                    AdminCommands.viewUsers(sender, chatId);
                    currentUser.setState(IDLE);
                } else if ("/terminateSession".equalsIgnoreCase(messageText)) {
                    currentUser.setState(IDLE);
                    AdminCommands.terminateSession();
                } else if ("/forceFetch".equalsIgnoreCase(messageText)) {
                    AdminCommands.forceFetch();
                    currentUser.setState(IDLE);
                }
            }
        }
        try {
            switch (currentUser.getState()) {
                case MAIN_MENU -> getMenu(currentUser, chatId);
                case AWAITING_THIS_WEEK_SCHEDULE,
                        AWAITING_NEXT_DAY_SCHEDULE,
                        AWAITING_THIS_DAY_SCHEDULE,
                        AWAITING_NEXT_WEEK_SCHEDULE -> getSchedule(currentUser, chatId);
                case AWAITING_CUSTOM_SCHEDULE_INPUT -> getCustomSchedulePrepare(currentUser, chatId);
                case AWAITING_CUSTOM_SCHEDULE -> splitAndSend(currentUser, chatId, update);
                case SETTINGS -> getSettings(currentUser, chatId);
                case AWAITING_INPUT, NULL_GROUP -> registerUser(currentUser, chatId, update);
                case STOP -> stopDialog(chatId);
                case ADMIN_SEND_MESSAGE -> AdminCommands.sendMessage(update.getMessage().getText());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void getMenu(User currentUser, long chatId) throws IOException, TelegramApiException {
        if (currentUser.getGroup() == null) {
            currentUser.setState(NULL_GROUP);
        }
        String menu = UserCommands.getMenu();
        if (UserDB.getAllUsers().get(chatId).getRole() == UserRole.ADMIN) {
            menu += '\n' + AdminCommands.getAllCommands();
        }
        SendPhoto sendPhoto = getSendPhoto(chatId, menu);
        sendPhoto.setReplyMarkup(KeyBoardFactory.getCommandsList());
        abilityBot.execute(sendPhoto);
    }

    public CompletableFuture<Void> getSchedule(User currentUser, long chatId) {
        return CompletableFuture.runAsync(() -> {
            log.info("Користувач з id {} увійшов у getSchedule()", chatId);
            System.out.printf("Користувач з id %d увійшов у getSchedule()%n", chatId);
            if (currentUser.getGroup() == null || currentUser.getState() == AWAITING_INPUT) {
                currentUser.setState(UserState.NULL_GROUP);
                return;
            }
            SendPhoto sendPhoto;
            try {
                sendPhoto = getSendPhoto(chatId, "");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try {
                switch (currentUser.getState()) {
                    case AWAITING_THIS_WEEK_SCHEDULE -> {
                        logScheduleAttempt("цей тиждень", chatId);
                        sendPhoto = getSendPhoto(chatId, """
                                %s Розклад на цей тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisWeekSchedule(currentUser)));
                        logScheduleSuccess("цей тиждень", chatId);
                    }
                    case AWAITING_NEXT_WEEK_SCHEDULE -> {
                        logScheduleAttempt("наступний тиждень", chatId);
                        sendPhoto = getSendPhoto(chatId, """
                                %s Розклад на наступний тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getNextWeekSchedule(currentUser)));
                        logScheduleSuccess("наступний тиждень", chatId);
                    }
                    case AWAITING_THIS_DAY_SCHEDULE -> {
                        logScheduleAttempt("цей день", chatId);
                        sendPhoto = getSendPhoto(chatId, """
                                %s Розклад на сьогодні:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisDaySchedule(currentUser)));
                        logScheduleSuccess("цей день", chatId);
                    }
                    case AWAITING_NEXT_DAY_SCHEDULE -> {
                        logScheduleAttempt("наступний день", chatId);
                        sendPhoto = getSendPhoto(chatId, """
                                %s Розклад на завтра:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getTomorrowSchedule(currentUser)));
                        logScheduleSuccess("наступний день", chatId);
                    }
                }
            } catch (IOException exception) {
                logScheduleError(chatId, exception);
                sendPhoto.setCaption("""
                        Щось пішло не так :(
                        Спробуйте пізніше.
                        """);
            } finally {
                sendPhoto.setReplyMarkup(KeyBoardFactory.getBackButton());
                try {
                    abilityBot.execute(sendPhoto);
                } catch (TelegramApiException e) {
                    log.error("Помилка під час відправлення повідомлення для користувача з id {}", chatId, e);
                }
                currentUser.setState(UserState.IDLE);
            }
        });
    }

    private void logScheduleAttempt(String period, long chatId) {
        System.out.printf("Розпочато спробу отримання розкладу на %s для користувача з id %d%n", period, chatId);
        log.info("Розпочато спробу отримання розкладу на {} для користувача з id {}", period, chatId);
    }

    private void logScheduleSuccess(String period, long chatId) {
        System.out.printf("Успішно завершено спробу отримання розкладу на %s для користувача з id %d%n", period, chatId);
        log.info("Успішно завершено спробу отримання розкладу на {} для користувача з id {}", period, chatId);
    }

    private void logScheduleError(long chatId, IOException exception) {
        System.out.printf("Виникла помилка під час отримання розкладу для користувача з id %d. Повідомлення помилки: %s%n", chatId, exception.getMessage());
        log.error("Виникла помилка під час отримання розкладу для користувача з id {}. Повідомлення помилки: {}", chatId, exception.getMessage());
    }

    public void getCustomSchedulePrepare(User currentUser, long chatId) {
        System.out.printf("""
                Розпочато підготовку до отримання розкладу за власними параметрами
                для користувача з id %d в методі getCustomSchedulePrepare()
                """, chatId);
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
        sendMessage.setReplyMarkup(KeyBoardFactory.getBackButton());
        sender.execute(sendMessage);
        currentUser.setState(AWAITING_CUSTOM_SCHEDULE);

    }

    public void splitAndSend(User currentUser, long chatId, Update update) {
        System.out.printf("""
                Розпочато спробу отримати дані для отримання розкладу за власними параметрами
                для користувача з id %d
                """, chatId);
        log.info("""
                Розпочато спробу отримати дані для отримання розкладу за власними параметрами
                для користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        try {
            System.out.printf("""
                    Розпочато спробу розпарсити ввід користувача з id %d.
                    Повідомлення для парсингу: %s
                    """, chatId, update.getMessage().getText());
            log.info("""
                    Розпочато спробу розпарсити ввід користувача з id {}.
                    Повідомлення для парсингу: {}
                    """, chatId, update.getMessage().getText());
            System.out.println(update.getMessage().getText());
            String[] data = update.getMessage().getText().split("\\sз\\s|\\sпо\\s");
            if (data.length != 3) throw new Exception("Довжина масива менша за 3(%d)".formatted(data.length));
            sendMessage.enableHtml(true);
            System.out.printf("""
                    Успішно завершено спробу розпарсити ввід користувача з id %d
                    """, chatId);
            log.info("""
                    Успішно завершено спробу розпарсити ввід користувача з id {}
                    """, chatId);
            System.out.printf("""
                    Розпочато спробу отримати розклад для користувача з id %d.
                    Дата для отримання: з %s по %s
                    """, chatId, data[1], data[2]);
            log.info("""
                    Розпочато спробу отримати розклад для користувача з id {}.
                    Дата для отримання: з {} по {}
                    """, chatId, data[1], data[2]);
            Table response = DAOImpl.getInstance()
                    .getCustomTable(String.valueOf(GroupDB.getGroups().get(data[0])), data[1],
                            DateUtils.toString(DateUtils.parseDate(data[2]).plusDays(1)),
                            String.valueOf(data[0].charAt(data[0].indexOf('-') + 1)));
            System.out.printf("""
                    Успішно завершено спробу отримати розклад для користувача з id %d.
                    Дата для отримання: з %s по %s
                    """, chatId, data[1], data[2]);
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
                    sendMessage.setReplyMarkup(KeyBoardFactory.getBackButton());
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
            currentUser.setState(IDLE);
            System.out.printf("""
                    Успішно завершено спробу отримати дані для отримання розкладу за власними параметрами
                    для користувача з id %d
                    """, chatId);
            log.info("""
                    Успішно завершено спробу отримати дані для отримання розкладу за власними параметрами
                    для користувача з id {}
                    """, chatId);
        } catch (Exception exception) {
            System.out.printf("""
                    Помилка під час спроби розпарсити ввід користувача з id %d.
                    Повідомлення для парсингу: %s.
                    Повідомлення помилки: %s
                    """, chatId, update.getMessage(), exception.getMessage());
            log.info("""
                    Помилка під час спроби розпарсити ввід користувача з id {}.
                    Повідомлення для парсингу: {}.
                    Повідомлення помилки: {}
                    """, chatId, update.getMessage(), exception.getMessage());
            sendMessage.setText("Дані не є коректними! Спробуйте ще раз.");
            currentUser.setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
            sender.execute(sendMessage);
        }
    }

    public void getSettings(User currentUser, long chatId) {
        System.out.printf("""
                Розпочато отримання даних користувача з id %d
                """, chatId);
        log.info("""
                Розпочато отримання даних користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (currentUser.getGroup() == null) {
            currentUser.setState(AWAITING_INPUT);
        } else {
            sendMessage.setText(UserCommands.getUserSettings(currentUser));
            sendMessage.setReplyMarkup(KeyBoardFactory.getSettings(currentUser.isAreInBroadcastGroup()));
        }
        sender.execute(sendMessage);
        System.out.printf("""
                Закінчено отримання даних користувача %d
                """, chatId);
        log.info("""
                Закінчено отримання даних користувача {}
                """, chatId);
    }

    public void registerUser(User currentUser, long chatId, Update update) {
        System.out.printf("""
                Розпочато реєстрацію користувача з id %d
                """, chatId);
        log.info("""
                Розпочато реєстрацію користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        String group = update.hasMessage() ? update.getMessage().getText().toUpperCase() : update.getCallbackQuery().getData();
        if (GroupDB.getGroups().containsKey(group)) {
            sendMessage.setText("Ваша група: %s?".formatted(group));
            sendMessage.setReplyMarkup(KeyBoardFactory.getYesOrNoInline());
            currentUser.getLastMessages().addLast(update.getCallbackQuery().getData());
        } else {
            String stringBuffer = (currentUser.getGroup() == null ? "Схоже, що ви не зареєстровані.\n" :
                    "Для того, щоб змінити дані,\n") +
                    """
                            Будь-ласка, виберіть Вашу группу.
                            Групи, які наразі підтримуються:
                            """;
            sendMessage.enableHtml(true);
            sendMessage.setReplyMarkup(KeyBoardFactory.getGroupsKeyboardInline());
            sendMessage.setText(stringBuffer);
            currentUser.setState(AWAITING_INPUT);
        }
        sender.execute(sendMessage);
    }

    public void finishRegistration(User currentUser, long chatId) {
        System.out.printf("""
                Розпочато спробу завершити рєстрацію користувача з id %d
                """, chatId);
        log.info("""
                Розпочато спробу завершити рєстрацію користувача з id {}
                """, chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        UserCommands.finishRegistration(currentUser);
        sendMessage.setText(currentUser.getState() == null ? "Ви були успішно зареєстровані!" : "Ви успішно змінили налаштування групи!");
        sender.execute(sendMessage);
        currentUser.setState(MAIN_MENU);
        System.out.printf("""
                Завершено спробу завершити рєстрацію користувача з id %d
                """, chatId);
        log.info("""
                Завершено спробу зареєструвати користувача з id {}
                """, chatId);
    }

    public static void sendMessage(SilentSender sender, String params, String message) {
        Set<Object> values = parseParams(params);
        System.out.println(values);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        Set<Long> presentIds = UserDB.getAllUsers().keySet();
        if (values.isEmpty()) return;
        if (values.contains("-all")) {
            sendBroadcast(sendMessage, sender, presentIds, message);
        } else {
            sendMulticast(sendMessage, sender, presentIds, values, message);
        }

    }

    private static void sendMulticast(SendMessage sendMessage, SilentSender sender, Set<Long> presentIds, Set<Object> values, String message) {
        values.stream().filter(value -> value instanceof Long).filter(value -> presentIds.contains((Long) value)).forEach(send -> {
            sendMessage.setText(message);
            sendMessage.setChatId(Long.parseLong(send.toString()));
            sender.execute(sendMessage);
        });
    }

    private static void sendBroadcast(SendMessage sendMessage, SilentSender sender, Set<Long> presentIds, String message) {
        presentIds.forEach(user -> {
            sendMessage.setChatId(user);
            sendMessage.setText(message);
            sender.execute(sendMessage);
        });
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
            } catch (NumberFormatException ignored) {
            }
        });
        return userIds;
    }

    private void stopDialog(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("""
                Ви зупинили бота.
                Для того, щоб знову його використовувати,
                використовуйте /start
                """);
        sendMessage.setChatId(chatId);
        sender.execute(sendMessage);
        UserDB.getAllUsers().remove(chatId);
    }
    public static SilentSender getSilentSender() {
        return sender;
    }
}
