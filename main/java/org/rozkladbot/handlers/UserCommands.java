package org.rozkladbot.handlers;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.dao.DAOImpl;
import org.rozkladbot.entities.User;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class UserCommands {
    private static final DAOImpl dao = DAOImpl.getInstance();
    public static String getMenu() {
        return """
                Ось команди, які ти можеш використати:
                
                /day - розклад на сьогодні.
                /nextDay - розклад на завтра.
                /week - розклад на цей тиждень.
                /nextWeek - розклад на наступний тиждень.
                /custom - розклад за своїми параметрами.
                /settings - змінити налаштування.""";
    }
    public static String getThisDaySchedule(User user) throws ExecutionException, InterruptedException {
        return dao.getTodayTable(user).toString();
    }
    public static String getThisWeekSchedule(User user) throws ExecutionException, InterruptedException {
        return dao.getWeeklyTable(user).toString();
    }
    public static String getNextWeekSchedule(User user) throws ExecutionException, InterruptedException {
        return dao.getNextWeekTable(user).toString();
    }
    public static String getTomorrowSchedule(User user) throws ExecutionException, InterruptedException {
        return dao.getTomorrowTable(user).toString();
    }
    public static String getCustomSchedule(User user, String group, String dateFrom, String dateTo) throws ExecutionException, InterruptedException {
        return dao.getCustomTable(user, group, dateFrom, dateTo).toString();
    }
    public static String getUserSettings(User user) {
        return """
               Група: %s
               Номер групи: %d
               Курс: %s
               Щоденні повідомлення: %s
               """.formatted(user.getGroup().getGroupName(),
                user.getGroup().getGroupNumber(),
                user.getGroup().getCourse(),
                user.isAreInBroadcastGroup() ? "увімкнені" : "вимкнені");
    }
    public static void finishRegistration(User user) {
        String group = user.getLastMessages().getLast().toUpperCase();
        user.setGroup(GroupDB.getGroups().get(group));
        user.setState(UserState.REGISTERED);
    }
}
