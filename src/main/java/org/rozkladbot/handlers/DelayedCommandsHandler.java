package org.rozkladbot.handlers;

import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.DelayedCommand;
import org.rozkladbot.interfaces.DelayedExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component("DelayedCommandsHandler")
public class DelayedCommandsHandler implements DelayedExecutor {
    private static final List<DelayedCommand> delayedCommands = new CopyOnWriteArrayList<>();
    private static ResponseHandler rh;

    public DelayedCommandsHandler(ResponseHandler responseHandler) {
        if (rh == null) {
            DelayedCommandsHandler.rh = responseHandler;
        }
    }
    @Async
    @Scheduled(cron = "0/10 * * * * *")
    public void executeIfPresent() {
        delayedCommands.forEach(this::execute);
    }
    @Override
    public void execute(DelayedCommand delayedCommand) {
        switch (delayedCommand.getDelayedCommand()) {
            case TODAY_SCHEDULE -> DelayedCommandsHandler.rh.getTodaySchedule(delayedCommand.getUser().getChatID());
            case NEXT_DAY_SCHEDULE -> DelayedCommandsHandler.rh.getTomorrowSchedule(delayedCommand.getUser().getChatID());
            case THIS_WEEK_SCHEDULE -> DelayedCommandsHandler.rh.getThisWeekSchedule(delayedCommand.getUser().getChatID());
            case NEXT_WEEK_SCHEDULE -> DelayedCommandsHandler.rh.getNextWeekSchedule(delayedCommand.getUser().getChatID());
        }
        delayedCommands.remove(delayedCommand);
    }
    public static void addDelayedCommand(DelayedCommand delayedCommand) {
        DelayedCommandsHandler.delayedCommands.add(delayedCommand);
    }
    public boolean checkIfPresent() {
        return delayedCommands.isEmpty();
    }
}
