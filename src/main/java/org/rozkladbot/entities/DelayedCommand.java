package org.rozkladbot.entities;

import org.rozkladbot.constants.CommandFlags;

import java.util.Objects;

public class DelayedCommand {
    private User user;
    private CommandFlags delayedCommand;

    public DelayedCommand(User user, CommandFlags delayedCommand) {
        this.delayedCommand = delayedCommand;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CommandFlags getDelayedCommand() {
        return delayedCommand;
    }

    public void setDelayedCommand(CommandFlags delayedCommand) {
        this.delayedCommand = delayedCommand;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        DelayedCommand that = (DelayedCommand) object;
        return Objects.equals(user, that.user) && delayedCommand == that.delayedCommand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, delayedCommand);
    }

    @Override
    public String toString() {
        return "DelayedCommand{user=" + user + ", delayedCommand=" + delayedCommand + '}';
    }
}
