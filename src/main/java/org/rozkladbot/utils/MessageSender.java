package org.rozkladbot.utils;

import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component("MessageSender")
public class MessageSender {
    private AbilityBot abilityBot;
    private SilentSender sender;
    private static final ConsoleLineLogger<MessageSender> log = new ConsoleLineLogger<>(MessageSender.class);

    @Autowired
    public MessageSender(AbilityBot abilityBot, SilentSender sender) {
        this.sender = sender;
        this.abilityBot = abilityBot;
    }
    public void sendMessage(String params, String message) {
        Set<Object> values = parseParams(params);
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        Set<Long> presentIds = UserDB.getAllUsers().keySet();
        if (values.isEmpty()) return;
        if (values.contains("-all")) {
            sendBroadcast(sendMessage, presentIds, message);
        } else {
            sendMulticast(sendMessage, presentIds, values, message);
        }

    }

    private void sendMulticast(SendMessage sendMessage, Set<Long> presentIds, Set<Object> values, String message) {
        values.stream().filter(value -> value instanceof Long).filter(value -> presentIds.contains((Long) value)).forEach(send -> {
            sendMessage.setText(message);
            sendMessage.setChatId(Long.parseLong(send.toString()));
            sender.execute(sendMessage);
        });
    }

    private void sendBroadcast(SendMessage sendMessage, Set<Long> presentIds, String message) {
        presentIds.forEach(user -> {
            sendMessage.setChatId(user);
            sendMessage.setText(message);
            sender.execute(sendMessage);
        });
    }

    private Set<Object> parseParams(String params) {
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
    public void sendPhoto(Update update, User currentUser, String caption, ReplyKeyboard keyboard) throws IOException, TelegramApiException {
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

    private void overridePhoto(User currentUser, String caption, ReplyKeyboard keyboard) throws TelegramApiException {
        EditMessageCaption editMessageCaption = new EditMessageCaption();
        editMessageCaption.setCaption(caption);
        editMessageCaption.setChatId(currentUser.getChatID());
        editMessageCaption.setMessageId((int) currentUser.getLastSentMessage() + 1);
        editMessageCaption.setParseMode("html");
        if (keyboard != null) editMessageCaption.setReplyMarkup((InlineKeyboardMarkup) keyboard);
        sender.execute(editMessageCaption);
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
