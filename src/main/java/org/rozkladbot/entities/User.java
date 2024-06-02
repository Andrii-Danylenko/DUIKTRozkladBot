package org.rozkladbot.entities;

import org.rozkladbot.constants.UserState;
import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.utils.LimitedDeque;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Objects;

@Component
public class User implements Serializable {
    private long chatID;
    private String group;
    private String faculty = "1";
    private String course;
    private UserState state;
    private ArrayDeque<Message> lastMessages = new LimitedDeque<>(2);
    private Integer lastPinnedMessageId;

    public User(long chatID) {
        this.chatID = chatID;
    }
    public User(long chatID, String group, String faculty, String course, UserState userState, Integer lastPinnedMessageId) {
        this.chatID = chatID;
        this.group = group;
        this.faculty = faculty;
        this.course = course;
        this.state = userState;
        this.lastPinnedMessageId = lastPinnedMessageId;
    }

    public User(long chatID, UserState userState) {
        this.chatID = chatID;
        this.state = userState;
    }

    public User() {

    }

    public long getChatID() {
        return chatID;
    }

    public void setChatID(long chatID) {
        this.chatID = chatID;
    }

    public UserState getState() {
        return state;
    }

    public void setState(UserState state) {
        this.state = state;
    }

    public ArrayDeque<Message> getLastMessages() {
        return lastMessages;
    }

    public void setLastMessages(ArrayDeque<Message> lastMessages) {
        this.lastMessages = lastMessages;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }
    public HashMap<String, String> getUserParams() {
        return new HashMap<>() {{
            put("course", course);
            put("faculty", faculty);
            put("group", getGroupNumber().toString());
        }};
    }
    public Long getGroupNumber() {
        return GroupDB.getGroups().get(group);
    }
    public Integer getLastPinnedMessageId() {
        return lastPinnedMessageId;
    }
    public void setLastPinnedMessageId(Integer lastPinnedMessageId) {
        this.lastPinnedMessageId = lastPinnedMessageId;
    }
    @Override
    public String toString() {
        return """
                Id-чату: %d
                Група: %s
                Курс: %s
                """.formatted(chatID, group, course);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        User user = (User) object;
        return chatID == user.chatID && Objects.equals(group, user.group) && Objects.equals(faculty, user.faculty) && Objects.equals(course, user.course) && state == user.state && Objects.equals(lastMessages, user.lastMessages) && Objects.equals(lastPinnedMessageId, user.lastPinnedMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatID, state);
    }
}