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
import org.rozkladbot.exceptions.InvalidDataException;
import org.rozkladbot.factories.KeyBoardFactory;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.GroupMediaSender;
import org.rozkladbot.utils.date.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.rozkladbot.constants.UserState.*;

@Component("ResponseHandler")
public class ResponseHandler {
    private static final ConsoleLineLogger<ResponseHandler> log = new ConsoleLineLogger<>(ResponseHandler.class);
    private GroupMediaSender messageSender;

    @Autowired
    public ResponseHandler(GroupMediaSender messageSender) {
        this.messageSender = messageSender;
    }
    public ResponseHandler() {

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
                user.setLastSentMessage(messageSender.getMessageId(update));
            } else {
                user = UserDB.getAllUsers().get(chatId);
                user.setLastSentMessage(messageSender.getMessageId(update));
            }
            log.info("Розпочато діалог з користувачем з id {%s}".formatted(chatId));
            messageSender.sendMessage(user, welcomeMessage, KeyBoardFactory.getCommandsList(), false);
        } catch (Exception exception) {
            log.error("""
                    Виникла помилка під час створення діалогу із користувачем з id {%s}.
                    Повідомлення помилки: {%s}
                    """.formatted(chatId, exception.getMessage()));
        }
    }
    public boolean userIsActive(long chatId) {
        return UserDB.getAllUsers().containsKey(chatId);
    }

    public void replyToButtons(Long chatId, Update update) {
        CompletableFuture.runAsync(() -> {
            User currentUser = UserDB.getAllUsers().get(chatId);
            setUserName(update, currentUser);
            String messageText = "";
            if (update.hasCallbackQuery()) {
                if (currentUser.getUserName().isEmpty()) {
                    currentUser.setUserName(update.getCallbackQuery().getMessage().getChat().getUserName());
                }
                String callbackQueryText = update.getCallbackQuery().getData();
                currentUser.setLastSentMessage(update.getCallbackQuery().getMessage().getMessageId() - 1);
                handleCallbackQuery(update, currentUser, chatId, callbackQueryText);
            } else if (update.hasMessage()) {
                System.out.println(update.hasMessage());
                Message message = update.getMessage();
                if (currentUser.getUserName().isEmpty() && currentUser.getGroup() != null) {
                    currentUser.setUserName(message.getChat().getUserName());
                }
                if (message.hasText()) {
                    messageText  = message.getText();
                }
                if (message.hasPhoto()) {
                    messageText = message.getCaption() == null ? "" : message.getCaption();
                }
                currentUser.setLastSentMessage(message.getMessageId());
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
        messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
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
                         "Інститут захисту інформації",
                         "Інститут менеджменту та підприєм." -> {
                        currentUser.setState(AWAITING_COURSE);
                        currentUser.getLastMessages().addLast(callbackQueryText);
                    }
                }
            }
        }
    }

    private void handleMessage(Update update, User currentUser, Long chatId, String messageText) {
        messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
        if ("/stop".equalsIgnoreCase(messageText)) {
            currentUser.setState(STOP);
            messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
        }
        if (currentUser.getState() == UserState.NULL_GROUP || "Змінити групу".equalsIgnoreCase(messageText) && currentUser.getState() != null) {
            currentUser.setState(AWAITING_INPUT);
            messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
        }
        if (currentUser.getState() == AWAITING_INPUT) {
            handleAwaitingInput(currentUser, chatId, messageText);
        } else {
            handleOtherMessages(update, currentUser, chatId, messageText);
        }
    }

    private void handleAwaitingInput(User currentUser, Long chatId, String messageText) {
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
            messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
        } else {
            currentUser.setState(SETTINGS);
        }
    }

    private void handleAdminCommands(Update update, User currentUser, Long chatId, String messageText) {
        messageSender.setMsgIdIfLimitIsPassed(currentUser, update);
        if (currentUser.getState() != null && currentUser.getRole() == UserRole.ADMIN) {
            if (messageText.toLowerCase().startsWith("/sendmessage ")) {
                currentUser.setState(ADMIN_SEND_MESSAGE);
            } else if (messageText.toLowerCase().startsWith("/removeuser")) {
                removeUser(currentUser, messageText);
                currentUser.setState(IDLE);
            }
            else if (messageText.toLowerCase().startsWith("/synchronize ")) {
                try {
                    AdminCommands.synchronize(messageText.split("\\s"));
                } catch (IOException exception) {
                    log.error("Помилка в методі AdminCommands.synchronize()");
                    exception.printStackTrace();
                } finally {
                    currentUser.setState(IDLE);
                }
            } else if ("/viewUsers".equalsIgnoreCase(messageText)) {
                AdminCommands.viewUsers(messageSender.getSilentSender(), chatId);
                currentUser.setState(IDLE);
            } else if ("/terminateSession".equalsIgnoreCase(messageText)) {
                currentUser.setState(IDLE);
                AdminCommands.terminateSession();
            } else if ("/forceFetch".equalsIgnoreCase(messageText)) {
                AdminCommands.forceFetch();
                currentUser.setState(IDLE);
            } else if (messageText.toLowerCase().startsWith("/forstart")) {
                messageSender.sendMessage(currentUser, "Готовий до відправки повідомлень", null, false);
                messageSender.setIds(messageSender.parseParams(messageText));
                currentUser.setState(AWAITING_FORWARD_MESSAGE);
            } else if (messageText.toLowerCase().startsWith("/forstop")) {
                messageSender.sendMessage(currentUser, "Починаю пересилання повідомлень", null, false);
                messageSender.resendMediaGroup();
                messageSender.clearIds();
                currentUser.setState(IDLE);
                messageSender.sendMessage(currentUser, "Закінчено пересилання повідомлень", null, false);
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
            case STOP -> stopDialog(currentUser, chatId);
            case ADMIN_SEND_MESSAGE -> AdminCommands.sendMessage(update.getMessage().getText());
            case AWAITING_FORWARD_MESSAGE -> {
                messageSender.getMessageData(update, currentUser);
            }
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
        messageSender.sendMessage(currentUser, menu, KeyBoardFactory.getCommandsList(), override);
        currentUser.setState(IDLE);
    }

    public CompletableFuture<Void> getSchedule(User currentUser, long chatId, boolean override) {
        return CompletableFuture.runAsync(() -> {
            log.info("Користувач з id {%s} увійшов у getSchedule()".formatted(chatId));
            if (currentUser.getGroup() == null || currentUser.getState() == AWAITING_INPUT) {
                currentUser.setState(UserState.NULL_GROUP);
                return;
            }
            try {
                messageSender.sendMessage(currentUser, "Намагаюся отримати розклад...", null, override);
                switch (currentUser.getState()) {
                    case AWAITING_THIS_WEEK_SCHEDULE -> {
                        log.info("Спроба завантажити розклад за цей тиждень для користувача {%s}".formatted(chatId));
                        messageSender.sendMessage(currentUser, """
                                %s Розклад на цей тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisWeekSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.success("Успішно закінчена спроба завантажити розклад на цей тиждень для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_NEXT_WEEK_SCHEDULE -> {
                        log.info("Спроба завантажити розклад на наступний тиждень для користувача {%s}".formatted(chatId));
                        messageSender.sendMessage(currentUser, """
                                %s Розклад на наступний тиждень:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getNextWeekSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.success("Успішно закінчена спроба завантажити розклад на наступний тиждень для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_THIS_DAY_SCHEDULE -> {
                        log.success("Успішно закінчена спроба завантажити розклад на сьогодні для користувача {%s}".formatted(chatId));
                        messageSender.sendMessage(currentUser, """
                                %s Розклад на сьогодні:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getThisDaySchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.success("Успішно закінчена спроба завантажити розклад на сьогодні для користувача {%s}".formatted(chatId));
                    }
                    case AWAITING_NEXT_DAY_SCHEDULE -> {
                        log.success("Успішно закінчена спроба завантажити розклад на завтра для користувача {%s}".formatted(chatId));
                        messageSender.sendMessage(currentUser, """
                                %s Розклад на завтра:

                                %s
                                """.formatted(EmojiList.NERD_FACE, UserCommands.getTomorrowSchedule(currentUser)), KeyBoardFactory.getBackButton(), true);
                        log.success("Успішно закінчена спроба завантажити розклад на завтра для користувача {%s}".formatted(chatId));
                    }
                }
            } catch (Exception exception) {
                log.error(("Помилка під час спроби завантажити розклад на завтра для користувача {%s}." +
                        "Підстава: %s").formatted(chatId, exception.getCause()));
                messageSender.sendMessage(currentUser, """
                        Виникла помилка під час отримання розкладу.
                        Спробуйте пізніше. 🥺
                        """, KeyBoardFactory.getBackButton(), true);
            } finally {
                currentUser.setState(UserState.IDLE);
            }
        });
    }

    public void getCustomSchedulePrepare(User currentUser, long chatId, boolean override) {
        log.info("""
                Розпочато підготовку до отримання розкладу за власними параметрами
                для користувача з id {%d} в методі getCustomSchedulePrepare()""".formatted(chatId));
        messageSender.sendMessage(currentUser, """
                Для того, щоб отримати власні дані,
                Використовуйте синтаксис:
                <b>[група]</b> з <b>[дата початку]</b> по <b>[дата кінця]</b>
                Наприклад: <b>%s</b> з <b>%s</b> по <b>%s</b>
                """.formatted(currentUser.getGroup().getGroupName(), DateUtils.getTodayDateString(), DateUtils.toString(DateUtils.addDays(DateUtils.getTodayDate(), 7))), KeyBoardFactory.getBackButton(), override);
        currentUser.setState(AWAITING_CUSTOM_SCHEDULE);

    }

    public void splitAndSend(User currentUser, long chatId, Update update, boolean override) {
        log.info("""
                Розпочато спробу отримати дані для отримання розкладу за власними параметрами
                для користувача з id {%d}""".formatted(chatId));
        try {
            log.info("""
                    Розпочато спробу розпарсити ввід користувача з id {%d}.
                    Повідомлення для парсингу: {%s}""".formatted(chatId, update.getMessage().getText()));
            String[] data = update.getMessage().getText().split("\\sз\\s|\\sпо\\s");
            if (data.length != 3) throw new InvalidDataException("Довжина масива менша за 3(%d)".formatted(data.length));
            log.success("""
                    Успішно завершено спробу розпарсити ввід користувача з id {%d}
                    """.formatted(chatId));
            log.info("""
                    Розпочато спробу отримати розклад для користувача з id {%d}.
                    Дата для отримання: з {%s} по {%s}""".formatted(chatId, data[1], data[2]));
            Table response = DAOImpl.getInstance()
                    .getCustomTable(currentUser, data[0], data[1],
                            DateUtils.toString(DateUtils.parseDate(data[2]).plusDays(1)));
            log.success("""
                    Успішно завершено спробу отримати розклад для користувача з id {%d}.
                    Дата для отримання: з {%s} по {%s}""".formatted(chatId, data[1], data[2]));
            int weekDelimiter = 0;
            int finish = response.getTable().size();
            int globalCounter = 0;
            StringBuilder buffer = new StringBuilder();
            for (DayOfWeek day : response.getTable()) {
                weekDelimiter++;
                globalCounter++;
                if (globalCounter == finish) {
                    messageSender.sendMessage(currentUser, buffer.toString(), null, false);
                }
                if (weekDelimiter == 7) {
                    messageSender.sendMessage(currentUser, buffer.toString(), null, false);
                    weekDelimiter = 0;
                    buffer.delete(0, buffer.length() - 1);
                }
                buffer.append(day.toStringIfMany()).append('\n');
            }
            messageSender.sendMessage(currentUser, """
                    Натисни кнопку,
                    щоб повернутися до головного меню
                    """, KeyBoardFactory.getBackButton(), false);
            currentUser.setState(IDLE);
            log.success("""
                    Успішно завершено спробу отримати дані для отримання розкладу за власними параметрами
                    для користувача з id {%d}""".formatted(chatId));
        } catch (RuntimeException exception) {
            log.error("""
                    Помилка під час спроби розпарсити ввід користувача з id {%d}.
                    Повідомлення для парсингу: {%s}.
                    Повідомлення помилки: {%s}""".formatted(chatId, update.getMessage(), exception.getMessage()));
            messageSender.sendMessage(currentUser, "Дані не є коректними! Спробуйте ще раз.", KeyBoardFactory.getBackButton(), override);
            currentUser.setState(AWAITING_CUSTOM_SCHEDULE);
        } catch (InterruptedException | ExecutionException exception) {
            messageSender.sendMessage(currentUser, "Виникла помилка під час отримання розкладу.\nСпробуйте пізніше. %s".formatted(EmojiList.DISAPPOINTMENT),
                    KeyBoardFactory.getBackButton(), override);
            currentUser.setState(IDLE);
        }
    }

    public void getSettings(User currentUser, long chatId, boolean override) {
        try {
            log.info("""
                    Розпочато отримання даних користувача з id {%d}""".formatted(chatId));
            if (currentUser.getGroup() == null) {
                currentUser.setState(AWAITING_INPUT);
            } else {
                messageSender.sendMessage(currentUser, UserCommands.getUserSettings(currentUser), KeyBoardFactory.getSettings(currentUser.isAreInBroadcastGroup()), override);
            }
            log.success("""
                    Успішно закінчено отримання даних користувача {%d}""".formatted(chatId));
        } catch (NullPointerException exception) {
            log.error("NullPointer в методі getSettings");
        }
    }

    public void registerUser(Update update, User currentUser, long chatId) {
        log.info("""
                Розпочато реєстрацію користувача з id {%d}""".formatted(chatId));
        String group = update.hasMessage() ? update.getMessage().getText().toUpperCase() : update.getCallbackQuery().getData();
        if (currentUser.getState() == AWAITING_COURSE) {
            messageSender.sendMessage(currentUser, """
                    Виберіть курс.
                    Курси, які наразі підтримуються:
                    """, KeyBoardFactory.getCourseKeyBoard(currentUser), true);
        } else if (currentUser.getState() == AWAITING_GROUP) {
            if (GroupDB.getGroups().containsKey(group)) {
                messageSender.sendMessage(currentUser, "Ваша група: %s?".formatted(group), KeyBoardFactory.getYesOrNoInline(), true);
                currentUser.getLastMessages().addLast(group);
            } else {
                messageSender.sendMessage(currentUser, """
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
            messageSender.sendMessage(currentUser, stringBuffer, KeyBoardFactory.getInstitutesKeyboardInline(), true);
            currentUser.setState(AWAITING_INSTITUTE);
        }
    }

    public void finishRegistration(User currentUser, long chatId) {
        log.info("""
                Розпочато спробу завершити рєстрацію користувача з id {%d}""".formatted(chatId));
        UserCommands.finishRegistration(currentUser);
        messageSender.sendMessage(currentUser, currentUser.getGroup() == null ? "Ви були успішно зареєстровані!" : "Ви успішно змінили налаштування групи!", KeyBoardFactory.getBackButton(), true);
        currentUser.setState(MAIN_MENU);
        log.info("""
                Завершено спробу зареєструвати користувача з id {%d}""".formatted(chatId));
    }

    private void stopDialog(User currentUser, long chatId) {
        messageSender.sendMessage(currentUser, """
                Ви зупинили бота.
                Для того, щоб знову його використовувати,
                використовуйте /start
                """, null, true);
        log.info("Користувач {%d} закінчив діалог з ботом".formatted(chatId));
        UserDB.getAllUsers().remove(chatId);
    }

    private void setUserName(Update update, User currentUser) {
        if (update.hasCallbackQuery()) {
            if (currentUser.getUserName().isEmpty()) {
                Message message = update.getCallbackQuery().getMessage();
                if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                    currentUser.setUserName(message.getChat().getTitle());
                } else {
                    currentUser.setUserName("@" + message.getChat().getUserName());
                }
            }
        }
        else if (currentUser.getUserName().isEmpty()) {
            Message message = update.getMessage();
            if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                currentUser.setUserName(message.getChat().getTitle());
            } else {
                currentUser.setUserName("@" + message.getChat().getUserName());
            }
        }
    }

    private void removeUser(User currentUser, String message) {
        try {
            log.info("Намагаюся видалити користувача...");
            long userId = Long.parseLong(message.split(" ")[1]);
            UserDB.removeUserById(userId);
            String success = "Успішно видалив користувача з id %d".formatted(userId);
            log.success(success);
            messageSender.sendMessage(currentUser,  success, null, false);
        } catch (Exception exception) {
            log.error("Не вдалося видалити користувача. Привід: %s".formatted(exception.getCause().getMessage()));
        }
    }
}
