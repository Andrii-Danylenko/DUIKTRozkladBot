package org.rozkladbot.handlers;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.EmojiList;
import org.rozkladbot.constants.UserRole;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.DayOfWeek;
import org.rozkladbot.entities.Group;
import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;
import org.rozkladbot.factories.KeyBoardFactory;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.DateUtils;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.rozkladbot.constants.UserState.*;

@Component("ResponseHandler")
public class ResponseHandler {
    private static final ConsoleLineLogger<ResponseHandler> log = new ConsoleLineLogger<>(ResponseHandler.class);
    private static SilentSender sender;
    private final AbilityBot abilityBot;

    public ResponseHandler(SilentSender sender, AbilityBot abilityBot) {
        ResponseHandler.sender = sender;
        this.abilityBot = abilityBot;
    }

    public synchronized void replyToStart(Update update, long chatId) {
        try {
            String welcomeMessage = "";
            User user;
            if (!UserDB.getAllUsers().containsKey(chatId)) {
                user = new User(chatId, NULL_GROUP);
                user.setRole(UserRole.USER);
                UserDB.getAllUsers().put(chatId, user);
                welcomeMessage = "Цей бот створений, щоб зручно проглядати розклад ДУІКТ.%n%s".formatted(UserCommands.getMenu());
                user.setLastSentMessage(getMessageId(update));
            } else {
                user = UserDB.getAllUsers().get(chatId);
                user.setLastSentMessage(getMessageId(update));
            }
            log.logAttempt("Розпочато діалог з користувачем з id {%s}".formatted(chatId));
            sendMessage(user, welcomeMessage, KeyBoardFactory.getCommandsList(), false);
        } catch (Exception exception) {
            log.logIfError("""
                    Виникла помилка під час створення діалогу із користувачем з id {%s}.
                    Повідомлення помилки: {%s}
                    """.formatted(chatId, exception.getMessage()));
        }
    }

    private void sendPhoto(Update update, User currentUser, String caption, ReplyKeyboard keyboard) throws IOException, TelegramApiException {
        long subtract = (getMessageId(update) - currentUser.getLastSentMessage());
        if (subtract > 0) {
            overridePhoto(currentUser, caption, keyboard);
        } else {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setParseMode("html");
            sendPhoto.setChatId(currentUser.getChatID());
            sendPhoto.setCaption(caption);
            if (keyboard != null) {
                sendPhoto.setReplyMarkup(keyboard);
            }
            sendPhoto.setPhoto(new InputFile(new FileInputStream(Paths.get("funnyImages/image.jpg").toFile()), "meme"));
            abilityBot.execute(sendPhoto);
        }
    }

    private void overridePhoto(User currentUser, String caption, ReplyKeyboard keyboard) {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setCaption(caption);
        editMessageCaption.setChatId(currentUser.getChatID());
        editMessageCaption.setMessageId((int) currentUser.getLastSentMessage() + 1);
        editMessageCaption.setParseMode("html");
        if (keyboard != null) editMessageCaption.setReplyMarkup((InlineKeyboardMarkup) keyboard);
        sender.execute(editMessageCaption);
    }

    private void sendMessage(User currentUser, String message, ReplyKeyboard keyboard, boolean overrideMessage) {
        if (overrideMessage) {
            overrideMessage(currentUser, message, keyboard);
        } else {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(currentUser.getChatID());
            sendMessage.setText(message);
            sendMessage.setParseMode("html");
            if (keyboard != null) {
                sendMessage.setReplyMarkup(keyboard);
            }
            sender.execute(sendMessage);
        }
    }

    private void overrideMessage(User currentUser, String message, ReplyKeyboard keyboard) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(message);
        editMessageText.setChatId(currentUser.getChatID());
        editMessageText.setMessageId((int) currentUser.getLastSentMessage() + 1);
        editMessageText.setParseMode("html");
        if (keyboard != null) editMessageText.setReplyMarkup((InlineKeyboardMarkup) keyboard);
        sender.execute(editMessageText);
    }

    public boolean userIsActive(long chatId) {
        return UserDB.getAllUsers().containsKey(chatId);
    }

    public void replyToButtons(Long chatId, Update update) {
        CompletableFuture.runAsync(() -> {
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            User currentUser = UserDB.getAllUsers().get(chatId);
            String messageText = "";
            lock.unlock();
            if (update.hasCallbackQuery()) {
                String callbackQueryText = update.getCallbackQuery().getData();
                currentUser.setLastSentMessage(update.getCallbackQuery().getMessage().getMessageId() - 1);
                handleCallbackQuery(update, currentUser, chatId, callbackQueryText);
            } else if (update.hasMessage()) {
                messageText = update.getMessage().getText();
                currentUser.setLastSentMessage(update.getMessage().getMessageId());
                currentUser.getLastMessages().addLast(messageText);
                handleMessage(update, currentUser, chatId, messageText);
            }

            try {
                handleState(update, currentUser, chatId);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    private void handleCallbackQuery(Update update, User currentUser, Long chatId, String callbackQueryText) {
        setMsgIdIfLimitIsPassed(currentUser, update);
        if (currentUser.getState() == AWAITING_GROUP && "ТАК".equalsIgnoreCase(callbackQueryText)) {
            finishRegistration(currentUser, chatId);
        } else if ("НАЗАД".equalsIgnoreCase(callbackQueryText)) {
            currentUser.setState(MAIN_MENU);
        } else if (currentUser.getState() == NULL_GROUP) {
            if ("НІ".equalsIgnoreCase(callbackQueryText) || GroupDB.getGroups().containsKey(callbackQueryText)) {
                currentUser.setState(AWAITING_INPUT);
            }
        } else {
            handleOtherCallbackQueries(currentUser, callbackQueryText);
        }
    }

    private void handleOtherCallbackQueries(User currentUser, String callbackQueryText) {
        if (currentUser.getGroup() != null &&
                currentUser.getState() != AWAITING_INSTITUTE &&
                currentUser.getState() != AWAITING_INPUT &&
                currentUser.getState() != AWAITING_COURSE &&
                currentUser.getState() != AWAITING_GROUP) {
            if ("ЗГ".equalsIgnoreCase(callbackQueryText)) {
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
        } else {
            if (currentUser.getState() == AWAITING_COURSE) {
                Set<String> courses = GroupDB.getGroups().values().stream()
                        .filter(x -> x.getInstitute().equalsIgnoreCase(currentUser.getLastMessages().getLast()))
                        .map(Group::getCourse).collect(Collectors.toSet());
                if (courses.contains(callbackQueryText)) {
                    currentUser.setState(AWAITING_GROUP);
                    currentUser.getLastMessages().addLast(callbackQueryText);
                }
            } else if (currentUser.getGroup() == null &&
                    currentUser.getState() != AWAITING_INSTITUTE &&
                    currentUser.getState() != AWAITING_INPUT &&
                    currentUser.getState() != AWAITING_COURSE &&
                    currentUser.getState() != AWAITING_GROUP) {
                currentUser.setState(AWAITING_INPUT);
            } else if (currentUser.getState() == AWAITING_INSTITUTE) {
                switch (callbackQueryText) {
                    case "Інститут Інформаційних технологій",
                            "Інститут Телекомунікацій",
                            "Навчально-науковий інститут захисту інформації",
                            "інститут менеджменту та підприємництва" -> {
                        currentUser.setState(AWAITING_COURSE);
                        currentUser.getLastMessages().addLast(callbackQueryText);
                    }
                }
            }
        }
    }

    private void handleMessage(Update update, User currentUser, Long chatId, String messageText) {
        setMsgIdIfLimitIsPassed(currentUser, update);
        if ("/stop".equalsIgnoreCase(messageText)) {
            currentUser.setState(STOP);
            setMsgIdIfLimitIsPassed(currentUser, update);
        }
        if (currentUser.getState() == UserState.NULL_GROUP || "Змінити групу".equalsIgnoreCase(messageText) && currentUser.getState() != null) {
            currentUser.setState(AWAITING_INPUT);
            setMsgIdIfLimitIsPassed(currentUser, update);
        }
        if (currentUser.getState() == AWAITING_INPUT) {
            handleAwaitingInput(update, currentUser, chatId, messageText);
        } else {
            handleOtherMessages(update, currentUser, chatId, messageText);
        }
    }

    private void handleAwaitingInput(Update update, User currentUser, Long chatId, String messageText) {
        if ("Так".equalsIgnoreCase(messageText)) {
            finishRegistration(currentUser, chatId);
        } else if ("Ні".equalsIgnoreCase(messageText)) {
            currentUser.setState(SETTINGS);
        }
    }

    private void handleOtherMessages(Update update, User currentUser, Long chatId, String messageText) {
        if ("/menu".equalsIgnoreCase(messageText) || "/menu@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(MAIN_MENU);
        } else if ("/settings".equalsIgnoreCase(messageText) || "/settings@rozkad_bot".equalsIgnoreCase(messageText) || (currentUser.getGroup() == null)) {
            handleSettings(update, currentUser, messageText);
        } else if ("/week".equalsIgnoreCase(messageText) || "/week@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(AWAITING_THIS_WEEK_SCHEDULE);
        } else if ("/nextWeek".equalsIgnoreCase(messageText) || "/nextWeek@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(AWAITING_NEXT_WEEK_SCHEDULE);
        } else if ("/day".equalsIgnoreCase(messageText) || "/day@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(AWAITING_THIS_DAY_SCHEDULE);
        } else if ("/nextDay".equalsIgnoreCase(messageText) || "/nextDay@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(AWAITING_NEXT_DAY_SCHEDULE);
        } else if ("/custom".equalsIgnoreCase(messageText) || "/custom@rozkad_bot".equalsIgnoreCase(messageText)) {
            currentUser.setState(AWAITING_CUSTOM_SCHEDULE_INPUT);
        }
        handleAdminCommands(update, currentUser, chatId, messageText);
    }

    private void handleSettings(Update update, User currentUser, String messageText) {
        if (GroupDB.getGroups().containsKey(messageText.toUpperCase())) {
            currentUser.setState(AWAITING_INPUT);
            setMsgIdIfLimitIsPassed(currentUser, update);
        } else {
            currentUser.setState(SETTINGS);
        }
    }

    private void handleAdminCommands(Update update, User currentUser, Long chatId, String messageText) {
        setMsgIdIfLimitIsPassed(currentUser, update);
        if (currentUser.getState() != null && currentUser.getRole() == UserRole.ADMIN) {
            if (messageText.toLowerCase().startsWith("/sendmessage ")) {
            } else if (messageText.toLowerCase().startsWith("/synchronize ")) {
                try {
                    AdminCommands.synchronize(messageText.split("\\s"));
                } catch (IOException exception) {
                    log.logIfError("Помилка в методі AdminCommands.synchronize()");
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

    private void handleState(Update update, User currentUser, Long chatId) {
        boolean override = !update.hasMessage();
        switch (currentUser.getState()) {
            case MAIN_MENU -> getMenu(currentUser, chatId, override);
            case AWAITING_THIS_WEEK_SCHEDULE,
                    AWAITING_NEXT_DAY_SCHEDULE,
                    AWAITING_THIS_DAY_SCHEDULE,
                    AWAITING_NEXT_WEEK_SCHEDULE -> getSchedule(currentUser, chatId, override);
            case AWAITING_CUSTOM_SCHEDULE_INPUT -> getCustomSchedulePrepare(currentUser, chatId, override);
            case AWAITING_CUSTOM_SCHEDULE -> splitAndSend(currentUser, chatId, update, override);
            case SETTINGS -> getSettings(currentUser, chatId, override);
            case AWAITING_INPUT, NULL_GROUP, AWAITING_COURSE, AWAITING_GROUP ->
                    registerUser(update, currentUser, chatId);
            case STOP -> stopDialog(chatId);
            case ADMIN_SEND_MESSAGE -> AdminCommands.sendMessage(update.getMessage().getText());
        }
    }

    public void getMenu(User currentUser, long chatId, boolean override) {
        if (currentUser.getGroup() == null) {
            currentUser.setState(NULL_GROUP);
        }
        String menu = UserCommands.getMenu();
        if (UserDB.getAllUsers().get(chatId).getRole() == UserRole.ADMIN) {
            menu += '\n' + AdminCommands.getAllCommands();
        }
        sendMessage(currentUser, menu, KeyBoardFactory.getCommandsList(), override);
        currentUser.setState(IDLE);
    }

    public CompletableFuture<Void> getSchedule(User currentUser, long chatId, boolean override) {
        return CompletableFuture.runAsync(() -> {
            log.logAttempt("Користувач з id {%s} увійшов у getSchedule()".formatted(chatId));
            if (currentUser.getGroup() == null || currentUser.getState() == AWAITING_INPUT) {
                currentUser.setState(UserState.NULL_GROUP);
                return;
            }
            try {
                sendMessage(currentUser, "Намагаюся отримати розклад...", null, override);
                switch (currentUser.getState()) {
                    case AWAITING_THIS_WEEK_SCHEDULE -> {
                        log.logAttempt("Спроба завантажити розклад за цей тиждень для користувача {%s}".formatted(chatId));
                        sendMessage(currentUser, """
                                %s Розклад на цей тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisWeekSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на цей тиждень для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_NEXT_WEEK_SCHEDULE -> {
                        log.logAttempt("Спроба завантажити розклад на наступний тиждень для користувача {%s}".formatted(chatId));
                        sendMessage(currentUser, """
                                %s Розклад на наступний тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getNextWeekSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на наступний тиждень для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_THIS_DAY_SCHEDULE -> {
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на сьогодні для користувача {%s}".formatted(chatId));
                        sendMessage(currentUser, """
                                %s Розклад на сьогодні:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisDaySchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на сьогодні для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_NEXT_DAY_SCHEDULE -> {
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на завтра для користувача {%s}".formatted(chatId));
                        sendMessage(currentUser, """
                                %s Розклад на завтра:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getTomorrowSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.logIfSuccess("Успішно закінчена спроба завантажити розклад на завтра для користувача {%s}".formatted(chatId));
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                log.logIfError(("Помилка під час спроби завантажити розклад на завтра для користувача {%s}." +
                        "Підстава: %s").formatted(chatId, exception));
                sendMessage(currentUser, """
                        Щось пішло не так :(
                        Спробуйте пізніше.
                        """, KeyBoardFactory.getBackButton(), override);
            } finally {
                currentUser.setState(UserState.IDLE);
            }
        });
    }

    public void getCustomSchedulePrepare(User currentUser, long chatId, boolean override) {
        log.logAttempt("""
                Розпочато підготовку до отримання розкладу за власними параметрами
                для користувача з id {%d} в методі getCustomSchedulePrepare()""".formatted(chatId));
        sendMessage(currentUser, """
                Для того, щоб отримати власні дані,
                Використовуйте синтаксис:
                <b>[група]</b> з <b>[дата початку]</b> по <b>[дата кінця]</b>
                Наприклад: <b>ІСД-22</b> з <b>19.04.2024</b> по <b>29.04.2024</b>
                """, KeyBoardFactory.getBackButton(), override);
        currentUser.setState(AWAITING_CUSTOM_SCHEDULE);

    }

    public void splitAndSend(User currentUser, long chatId, Update update, boolean override) {
        log.logAttempt("""
                Розпочато спробу отримати дані для отримання розкладу за власними параметрами
                для користувача з id {%d}""".formatted(chatId));
        try {
            log.logAttempt("""
                    Розпочато спробу розпарсити ввід користувача з id {%d}.
                    Повідомлення для парсингу: {%s}""".formatted(chatId, update.getMessage().getText()));
            String[] data = update.getMessage().getText().split("\\sз\\s|\\sпо\\s");
            if (data.length != 3) throw new Exception("Довжина масива менша за 3(%d)".formatted(data.length));
            log.logIfSuccess("""
                    Успішно завершено спробу розпарсити ввід користувача з id {%d}
                    """.formatted(chatId));
            log.logAttempt("""
                    Розпочато спробу отримати розклад для користувача з id {%d}.
                    Дата для отримання: з {%s} по {%s}""".formatted(chatId, data[1], data[2]));
            Table response = DAOImpl.getInstance()
                    .getCustomTable(String.valueOf(GroupDB.getGroups().get(data[0])), data[1],
                            DateUtils.toString(DateUtils.parseDate(data[2]).plusDays(1)),
                            String.valueOf(data[0].charAt(data[0].indexOf('-') + 1)));
            log.logIfSuccess("""
                    Успішно завершено спробу отримати розклад для користувача з id {%d}.
                    Дата для отримання: з {%s} по {%s}""".formatted(chatId, data[1], data[2]));
            int weekDelimiter = 0;
            int finish = response.getTable().size();
            int globalCounter = 0;
            StringBuffer buffer = new StringBuffer();
            for (DayOfWeek day : response.getTable()) {
                weekDelimiter++;
                globalCounter++;
                if (globalCounter == finish) {
                    sendMessage(currentUser, buffer.toString(), KeyBoardFactory.getBackButton(), false);
                }
                if (weekDelimiter == 7) {
                    sendMessage(currentUser, buffer.toString(), null, false);
                    weekDelimiter = 0;
                    buffer.delete(0, buffer.length() - 1);
                }
                buffer.append(day.toStringIfMany()).append('\n');
            }
            currentUser.setState(IDLE);
            log.logIfSuccess("""
                    Успішно завершено спробу отримати дані для отримання розкладу за власними параметрами
                    для користувача з id {%d}""".formatted(chatId));
        } catch (Exception exception) {
            log.logIfError("""
                    Помилка під час спроби розпарсити ввід користувача з id {%d}.
                    Повідомлення для парсингу: {%s}.
                    Повідомлення помилки: {%s}""".formatted(chatId, update.getMessage(), exception.getMessage()));
            sendMessage(currentUser, "Дані не є коректними! Спробуйте ще раз.", KeyBoardFactory.getBackButton(), override);
            currentUser.setState(AWAITING_CUSTOM_SCHEDULE);
        }
    }

    public void getSettings(User currentUser, long chatId, boolean override) {
        try {
            log.logAttempt("""
                    Розпочато отримання даних користувача з id {%d}""".formatted(chatId));
            if (currentUser.getGroup() == null) {
                currentUser.setState(AWAITING_INPUT);
            } else {
                sendMessage(currentUser, UserCommands.getUserSettings(currentUser), KeyBoardFactory.getSettings(currentUser.isAreInBroadcastGroup()), override);
            }
            log.logIfSuccess("""
                    Успішно закінчено отримання даних користувача {%d}""".formatted(chatId));
        } catch (NullPointerException exception) {
            System.out.println("Null pointer");
        }
    }

    public void registerUser(Update update, User currentUser, long chatId) {
        log.logAttempt("""
                Розпочато реєстрацію користувача з id {%d}""".formatted(chatId));
        String group = update.hasMessage() ? update.getMessage().getText().toUpperCase() : update.getCallbackQuery().getData();
        if (currentUser.getState() == AWAITING_COURSE) {
            sendMessage(currentUser, """
                        Виберіть курс.
                        Курси, які наразі підтримуються:
                        """, KeyBoardFactory.getCourseKeyBoard(currentUser), true);
        } else if (currentUser.getState() == AWAITING_GROUP) {
            if (GroupDB.getGroups().containsKey(group)) {
                sendMessage(currentUser, "Ваша група: %s?".formatted(group), KeyBoardFactory.getYesOrNoInline(), true);
                currentUser.getLastMessages().addLast(group);
            } else {
                sendMessage(currentUser, """
                        Виберіть групу.
                        Групи, які наразі підтримуються:
                        """, KeyBoardFactory.getGroupsKeyboardInline(currentUser), true);
            }
        } else {
            String stringBuffer = (currentUser.getGroup() == null ? "Схоже, що ви не зареєстровані.\n" :
                    "Для того, щоб змінити дані,\n") +
                    """
                            Виберіть інститут.
                            Інститути, які наразі підтримуються:
                            """;
            sendMessage(currentUser, stringBuffer, KeyBoardFactory.getInstitutesKeyboardInline(), true);
            currentUser.setState(AWAITING_INSTITUTE);
        }
    }

    public void finishRegistration(User currentUser, long chatId) {
        log.logAttempt("""
                Розпочато спробу завершити рєстрацію користувача з id {%d}""".formatted(chatId));
        UserCommands.finishRegistration(currentUser);
        sendMessage(currentUser, currentUser.getGroup() == null ? "Ви були успішно зареєстровані!" : "Ви успішно змінили налаштування групи!", KeyBoardFactory.getBackButton(), true);
        currentUser.setState(MAIN_MENU);
        log.logAttempt("""
                Завершено спробу зареєструвати користувача з id {%d}""".formatted(chatId));
    }

    public static void sendMessage(SilentSender sender, String params, String message) {
        Set<Object> values = parseParams(params);
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
        log.logAttempt("Користувач {%d} закінчив діалог з ботом".formatted(chatId));
        UserDB.getAllUsers().remove(chatId);
    }

    public static SilentSender getSilentSender() {
        return sender;
    }

    private int getMessageId(Update update) {
        int messageId = 0;
        if (update.hasMessage()) {
            messageId = update.getMessage().getMessageId();
        } else if (update.hasCallbackQuery()) {
            messageId = update.getCallbackQuery().getMessage().getMessageId();
        }
        return messageId;
    }

    private void setMsgIdIfLimitIsPassed(User currentUser, Update update) {
        if (getMessageId(update) - currentUser.getLastSentMessage() > 1) {
            currentUser.setLastSentMessage(getMessageId(update));
        }
    }
}
