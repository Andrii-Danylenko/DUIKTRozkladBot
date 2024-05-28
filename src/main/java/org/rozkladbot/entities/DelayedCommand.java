package org.rozkladbot.entities;

import org.rozkladbot.constants.CommandFlags;
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
}
