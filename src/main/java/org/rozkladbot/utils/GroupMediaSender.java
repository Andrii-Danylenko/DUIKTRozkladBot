package org.rozkladbot.utils;

import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component("GroupMediaResender")
public class GroupMediaSender extends MessageSender {
    private static final Map<Long, List<Message>> groupMedia = new ConcurrentHashMap<>();
    private Set<Long> idsToSend = new HashSet<>();
    @Autowired
    public GroupMediaSender(AbilityBot abilityBot) {
        super(abilityBot);
    }
    public void getMessageData(Update update, User currentUser) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.getMediaGroupId() != null) {
                if (groupMedia.containsKey(message.getChatId())) {
                    groupMedia.get(message.getChatId()).add(message);
                }
                else {
                    groupMedia.put(message.getChatId(), new ArrayList<>() {{
                        add(message);
                    }});
                }
            } else {
                if (message.hasPhoto()) {
                    sendPhoto(message);
                } else if (message.hasAnimation()) {
                    sendAnimation(message);
                } else if (message.hasText()) {
                    if (message.getText().toLowerCase().startsWith("/forstart")) return;
                    sendText(message);
                } else if (message.hasAudio()) {
                    sendAudio(message);
                } else if (message.hasVideo()) {
                    sendVideo(message);
                } else if (message.hasLocation()) {
                    sendLocation(message);
                } else if (message.hasVoice()) {
                    sendVoice(message);
                } else if (message.hasSticker()) {
                    sendSticker(message);
                }
                currentUser.setState(UserState.IDLE);
            }
        }
    }

    private void sendAnimation(Message message) {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setAnimation(new InputFile(message.getAnimation().getFileId()));
        sendAnimation.setCaption(message.getCaption());
        for (long id : idsToSend) {
            try {
                sendAnimation.setChatId(id);
                abilityBot.execute(sendAnimation);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void resendMediaGroup() {
        System.out.println("----------------------");
        if (groupMedia.isEmpty()) {
            System.out.println("=================");
            return;
        }
        SendMediaGroup mediaGroup = new SendMediaGroup();
        List<InputMedia> inputMediaList = new ArrayList<>();
        List<Message> messages = groupMedia.values().stream().flatMap(Collection::stream).toList();
        for (Message message : messages) {
            if (message.hasPhoto()) {
                InputMediaPhoto inputMediaPhoto = new InputMediaPhoto();
                inputMediaPhoto.setMedia(message.getPhoto().get(0).getFileId());
                inputMediaPhoto.setCaption(message.getCaption());
                inputMediaList.add(inputMediaPhoto);
            } else if (message.hasVideo()) {
                InputMediaVideo inputMediaVideo = new InputMediaVideo(message.getVideo().getFileId());
                inputMediaVideo.setCaption(message.getCaption());
                inputMediaList.add(inputMediaVideo);
            }
        }
        mediaGroup.setMedias(inputMediaList);
        for (long userId : idsToSend) {
            try {
                mediaGroup.setChatId(userId);
                abilityBot.execute(mediaGroup);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
        groupMedia.clear();
    }

    public void sendPhoto(Message message) {
        SendPhoto photo = new SendPhoto();
        photo.setCaption(message.getCaption());
        photo.setPhoto(new InputFile(
                message.getPhoto().stream().max(Comparator.comparingInt(PhotoSize::getFileSize)
                ).orElseThrow(RuntimeException::new).getFileId()));
        for (long id : idsToSend) {
            try {
                photo.setChatId(id);
                abilityBot.execute(photo);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendVideo(Message message) {
        SendVideo video = new SendVideo();
        video.setCaption(message.getCaption());
        video.setVideo(new InputFile(message.getVideo().getFileId()));
        for (long id : idsToSend) {
            try {
                video.setChatId(id);
                abilityBot.execute(video);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendText(Message message) {
        ForwardMessage forwardMessage = new ForwardMessage();
        forwardMessage.setMessageId(message.getMessageId());
        forwardMessage.setFromChatId(message.getChatId());
        for (long id : idsToSend) {
            try {
                forwardMessage.setChatId(id);
                abilityBot.execute(forwardMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendAudio(Message message) {
        SendAudio sendAudio = new SendAudio();
        sendAudio.setCaption(message.getCaption());
        sendAudio.setAudio(new InputFile(message.getAudio().getFileId()));
        for (long id : idsToSend) {
            try {
                sendAudio.setChatId(id);
                abilityBot.execute(sendAudio);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendVoice(Message message) {
        SendVoice sendVoice = new SendVoice();
        sendVoice.setCaption(message.getCaption());
        sendVoice.setVoice(new InputFile(message.getVoice().getFileId()));
        for (long id : idsToSend) {
            try {
                sendVoice.setChatId(id);
                abilityBot.execute(sendVoice);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendSticker(Message message) {
        SendSticker sendSticker = new SendSticker();
        sendSticker.setSticker(new InputFile(message.getSticker().getFileId()));
        for (long id : idsToSend) {
            try {
                sendSticker.setChatId(id);
                abilityBot.execute(sendSticker);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendLocation(Message message) {
        SendLocation sendLocation = new SendLocation();
        sendLocation.setLatitude(message.getLocation().getLatitude());
        sendLocation.setLongitude(message.getLocation().getLongitude());
        sendLocation.setLivePeriod(message.getLocation().getLivePeriod());
        for (long id : idsToSend) {
            try {
                sendLocation.setChatId(id);
                abilityBot.execute(sendLocation);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
    public void setIds(Set<Long> ids) {
        idsToSend = new HashSet<>(ids);
    }
    public void clearIds() {
        idsToSend.clear();
    }
}
