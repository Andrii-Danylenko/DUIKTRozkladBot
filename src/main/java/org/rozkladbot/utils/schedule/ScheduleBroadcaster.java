package org.rozkladbot.utils.schedule;

import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.entities.User;
import org.rozkladbot.handlers.UserCommands;
import org.rozkladbot.utils.MessageSender;
import org.rozkladbot.utils.date.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component("Broadcaster")
public class ScheduleBroadcaster {
    private static final Logger log = LoggerFactory.getLogger(ScheduleBroadcaster.class);
    private final MessageSender messageSender;

    @Autowired
    public ScheduleBroadcaster(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Scheduled(cron = "0 0 19 * * *", zone = "Europe/Kiev")
    public void broadcastAndPinTomorrowSchedule() {
        System.out.printf("Час на сервері: %s%nЧас у Києві: %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy")), DateUtils.timeOfNow());
        log.warn("Час на сервері: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss dd.MM.yyyy")));
        System.out.println("Розпочато широкомовну розсилку розписів на завтра");
        log.info("Розпочато широкомовну розсилку розписів на завтра");
        try {
            Map<Long, User> users = UserDB.getAllUsers();
            CompletableFuture<?>[] futures = users.entrySet().stream()
                    .filter(entry -> entry.getValue().isAreInBroadcastGroup())
                    .map(entry -> CompletableFuture.runAsync(() -> processUser(entry.getKey(), entry.getValue())))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();
        } finally {
            log.info("Успішно закінчено широкомовну розсилку розписів на завтра");
        }
    }

    private void processUser(Long chatId, User user) {
        if (user.getLastPinnedMessageId() != null) {
            log.info("Розпочато видалення минулого закріпленого повідомлення користувача з id {}", chatId);
            UnpinChatMessage unpinMessage = new UnpinChatMessage(chatId.toString(), user.getLastPinnedMessageId());
            messageSender.getSilentSender().execute(unpinMessage);
            log.info("Завершено видалення минулого закріпленого повідомлення користувача з id {}", chatId);
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.setParseMode("html");
        try {
            sendMessage.setText(UserCommands.getTomorrowSchedule(user));
            Optional<Message> message = messageSender.getSilentSender().execute(sendMessage);
            log.info("Розпочато закріплення повідомлення користувача з id {}", chatId);
            PinChatMessage pinMessage = new PinChatMessage(chatId.toString(), message.orElseThrow(Exception::new).getMessageId());
            messageSender.getSilentSender().execute(pinMessage);
            user.setLastPinnedMessageId(message.get().getMessageId());
            log.info("Успішно завершено закріплення повідомлення користувача з id {}", chatId);
        } catch (Exception exception) {
            sendMessage.setText("Не вдалося отримати розклад під час широкомовної розсилки :(");
        }
    }

    private void sendMessageOnError(Long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.setParseMode("html");
        sendMessage.setText("""
                Не вдалося отримати розклад :(
                Задача додалася до списку невиконаних завдань та буде дороблена пізніше.
                Очікуйте!
                """);
        messageSender.getSilentSender().execute(sendMessage);
    }
}
