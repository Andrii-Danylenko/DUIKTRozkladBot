package org.rozkladbot.entities;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.utils.LimitedDeque;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.HashMap;

@Entity
@Table(name = "users")
public class User implements Serializable {
    @Id
    private long chatID;
    @Column(name = "userName", unique = true, nullable = false)
    private String userName = "";
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    @Enumerated(EnumType.STRING)
    private UserState state;
    @Transient
    private ArrayDeque<String> lastMessages = new LimitedDeque<>(2);
    @Column(name = "lastPinnedMessageId")
    private Integer lastPinnedMessageId;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRole role;
    @Column(name = "areInBroadcastGroup", nullable = false)
    private boolean areInBroadcastGroup = true;
    @Column(name = "lastSentMessage", nullable = false)
    private long lastSentMessage = 0;

    public User(long chatID) {
        this.chatID = chatID;
    }
    public User(long chatID, Group group, UserState userState, UserRole userRole, Integer lastPinnedMessageId, boolean areInBroadcastGroup, long lastSentMessage, String userName) {
        this.chatID = chatID;
        this.group = group;
        this.state = userState;
        this.role = userRole;
        this.lastPinnedMessageId = lastPinnedMessageId;
        this.areInBroadcastGroup = areInBroadcastGroup;
        this.lastSentMessage = lastSentMessage;
        this.userName = userName;
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

    public ArrayDeque<String> getLastMessages() {
        return lastMessages;
    }

    public void setLastMessages(ArrayDeque<String> lastMessages) {
        this.lastMessages = lastMessages;
    }

    public Group getGroup() {
        return group;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    public void setGroup(Group group) {
        this.group = group;
    }
    public HashMap<String, String> getUserParams() {
        return new HashMap<>() {{
            put("course", group.getCourse() + "");
            put("faculty", group.getFaculty() + "");
            put("group", group.getGroupNumber() + "");
        }};
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
                Назва чату: %s
                Id-чату: %d
                Група: %s
                Курс: %s
                Роль: %s
                """.formatted(userName == null ? "" : userName,
                chatID,
                group == null ? "null" : group.getGroupName(),
                group == null ? "null" : group.getCourse(), role);
    }
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAreInBroadcastGroup() {
        return areInBroadcastGroup;
    }

    public void setAreInBroadcastGroup(boolean areInBroadcastGroup) {
        this.areInBroadcastGroup = areInBroadcastGroup;
    }
    public void setLastSentMessage(long lastSentMessage) {
        this.lastSentMessage = lastSentMessage;
    }
    public long getLastSentMessage() {
        return lastSentMessage;
    }

    public String toStringFull() {
        return "User{" +
               "chatID=" + chatID +
               ", userName='" + userName + '\'' +
               ", group=" + group +
               ", state=" + state +
               ", lastMessages=" + lastMessages +
               ", lastPinnedMessageId=" + lastPinnedMessageId +
               ", role=" + role +
               ", areInBroadcastGroup=" + areInBroadcastGroup +
               ", lastSentMessage=" + lastSentMessage +
               '}';
    }
}