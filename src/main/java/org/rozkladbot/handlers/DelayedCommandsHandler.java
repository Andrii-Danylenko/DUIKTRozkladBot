package org.rozkladbot.handlers;

import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.DelayedCommand;
import org.rozkladbot.entities.User;
import org.rozkladbot.interfaces.DelayedExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component("DelayedCommandsHandler")
public class DelayedCommandsHandler implements DelayedExecutor {
    private static final Set<DelayedCommand> delayedCommands = Collections.synchronizedSet(new HashSet<>());
    private static ResponseHandler rh;

    public DelayedCommandsHandler(ResponseHandler responseHandler) {
        if (rh == null) {
            DelayedCommandsHandler.rh = responseHandler;
        }
    }
    @Async
    @Scheduled(cron = "* 0/1 * * * *")
    public void executeIfPresent() {
        delayedCommands.forEach(this::execute);
    }
    @Override
    public void execute(DelayedCommand delayedCommand) {
        try {
            User user = delayedCommand.getUser();
            switch (delayedCommand.getDelayedCommand()) {
                case TODAY_SCHEDULE -> user.setState(UserState.AWAITING_THIS_DAY_SCHEDULE);
                case NEXT_WEEK_SCHEDULE -> user.setState(UserState.AWAITING_NEXT_WEEK_SCHEDULE);
                case NEXT_DAY_SCHEDULE -> user.setState(UserState.AWAITING_NEXT_DAY_SCHEDULE);
                case THIS_WEEK_SCHEDULE -> user.setState(UserState.AWAITING_THIS_WEEK_SCHEDULE);
            }
            DelayedCommandsHandler.rh.getSchedule(user, user.getChatID(), false);
            delayedCommands.remove(delayedCommand);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    public static void addDelayedCommand(DelayedCommand delayedCommand) {
        DelayedCommandsHandler.delayedCommands.add(delayedCommand);
    }
    public static Set<DelayedCommand> getDelayedCommands() {
        return delayedCommands;
    }
    public boolean checkIfPresent() {
        return delayedCommands.isEmpty();
    }
}
