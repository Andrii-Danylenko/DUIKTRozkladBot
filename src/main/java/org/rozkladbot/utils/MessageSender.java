package org.rozkladbot.utils;

import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Component("MessageSender")
@Primary
public class MessageSender {
    protected final AbilityBot abilityBot;
    protected final SilentSender sender;
    private static final ConsoleLineLogger<MessageSender> log = new ConsoleLineLogger<>(MessageSender.class);

    @Autowired
    public MessageSender(AbilityBot abilityBot) {
        this.sender = abilityBot.silent();
        this.abilityBot = abilityBot;
    }

    public void sendMessage(String params, String message) {
        System.out.println(message);
        Set<Long> values = parseParams(params);
        System.out.println(values);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        Set<Long> presentIds = UserDB.getAllUsers().keySet();
        if (values.isEmpty()) return;
        if (values.toString().equals("-all")) {
            sendBroadcast(sendMessage, presentIds, message);
        } else {
            sendMulticast(sendMessage, presentIds, values, message);
        }
    }

    private void sendMulticast(SendMessage sendMessage, Set<Long> presentIds, Set<Long> values, String message) {
        values.stream().filter(Objects::nonNull).filter(presentIds::contains).forEach(send -> {
            long id = Long.parseLong(send.toString());
            try {
                sendMessage.setText(message);
                sendMessage.setChatId(id);
                sender.execute(sendMessage);
            } catch (Exception e) {
                log.error("Помилка відправлення користувача з id: %d".formatted(id));
            }
        });
    }

    private void sendBroadcast(SendMessage sendMessage, Set<Long> presentIds, String message) {
        presentIds.forEach(id -> {
            try {
                sendMessage.setChatId(id);
                sendMessage.setText(message);
                sender.execute(sendMessage);
            } catch (Exception e) {
                log.error("Помилка відправлення користувача з id: %d".formatted(id));
            }
        });
    }

    public Set<Long> parseParams(String params) {
        String[] splitted = params.split(" ");
        if (splitted[1].equalsIgnoreCase("-all")) {
            return UserDB.getAllUsers().keySet();
        }
        Set<Long> userIds = new HashSet<>();
        Arrays.stream(params.split(" ")).forEach(number -> {
            try {
                userIds.add(Long.parseLong(number));
            } catch (NumberFormatException ignored) {
            }
        });
        return userIds;
    }

    public AbilityBot getAbilityBot() {
        return abilityBot;
    }

    public SilentSender getSilentSender() {
        return sender;
    }

    public int getMessageId(Update update) {
        int messageId = 0;
        if (update.hasMessage()) {
            messageId = update.getMessage().getMessageId();
        } else if (update.hasCallbackQuery()) {
            messageId = update.getCallbackQuery().getMessage().getMessageId();
        }
        return messageId;
    }

    public void setMsgIdIfLimitIsPassed(User currentUser, Update update) {
        if (getMessageId(update) - currentUser.getLastSentMessage() > 1) {
            currentUser.setLastSentMessage(getMessageId(update));
        }
    }

    public void sendGroupMedia(Message message, Set<Long> ids) {
        List<InputMedia> inputMediaList = new ArrayList<>();
        List<PhotoSize> photos = message.getPhoto();
        System.out.println(photos);
        SendMediaGroup sendMediaGroup = new SendMediaGroup();
    }
    public void sendMessage(User currentUser, String message, ReplyKeyboard keyboard, boolean overrideMessage) {
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
}
