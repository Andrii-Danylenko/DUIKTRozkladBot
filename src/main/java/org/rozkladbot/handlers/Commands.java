package org.rozkladbot.handlers;

import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.User;
import org.rozkladbot.utils.GroupDB;
import org.rozkladbot.dao.DAOImpl;

import java.io.IOException;

public final class Commands {
    private static final DAOImpl dao = DAOImpl.getInstance();
    public static String getSettings() {
        return "";
    }
    public static String getMenu() {
        return """
                /day - розклад на сьогодні.
                /nextDay - розклад на завтра.
                /week - розклад на цей тиждень.
                /nextWeek - розклад на наступний тиждень.
                /custom - розклад за своїми параметрами.
                /settings - змінити налаштування.
                """;
    }
    public static String getThisDaySchedule(User user) throws IOException {
        return dao.getTodayTable(GroupDB.getGroups().get(user.getGroup()).toString(), user.getCourse()).toString();
    }
    public static String getThisWeekSchedule(User user) throws IOException {
        return dao.getWeeklyTable(GroupDB.getGroups().get(user.getGroup()).toString(), user.getCourse()).toString();
    }
    public static String getNextWeekSchedule(User user) throws IOException {
        return dao.getNextWeekTable(GroupDB.getGroups().get(user.getGroup()).toString(), user.getCourse()).toString();
    }
    public static String getTomorrowSchedule(User user) throws IOException {
        return dao.getTomorrowTable(GroupDB.getGroups().get(user.getGroup()).toString(), user.getCourse()).toString();
    }
    public static String getCustomSchedule(String group, String dateFrom, String dateTo) throws IOException {
        return dao.getCustomTable(GroupDB.getGroups().get(group).toString(), dateFrom, dateTo, String.valueOf(group.split("-")[1].charAt(0))).toString();
    }
    public static String getUserSettings(User user) {
        return """
               Група: %s
               Номер групи: %d
               Курс: %s
               """.formatted(user.getGroup(), user.getGroupNumber(), user.getCourse());
    }
    public static void finishRegistration(User user) {
        String group = user.getLastMessages().getFirst().getText().toUpperCase();
        user.setGroup(group);
        user.setState(UserState.REGISTERED);
        user.setCourse(String.valueOf(group.charAt(group.lastIndexOf("-") + 1)));
    }
}
