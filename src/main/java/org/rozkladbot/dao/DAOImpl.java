package org.rozkladbot.dao;
import org.rozkladbot.entities.Table;
import org.rozkladbot.interfaces.DAO;
import org.rozkladbot.utils.DateUtils;
import org.rozkladbot.utils.JSONParser;
import org.rozkladbot.utils.Requester;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;

public class DAOImpl implements DAO {
    private final JSONParser parser = new JSONParser();
    private static final String baseUrl = "https://skedy.api.yacode.dev/v1/student/schedule?";
    private static volatile DAOImpl dao;
    private DAOImpl() {

    }
    public static DAOImpl getInstance() {
        DAOImpl daoToReturn = dao;
        if (daoToReturn != null) {
            return daoToReturn;
        }
        synchronized (DAOImpl.class) {
            if (dao == null) {
                dao = new DAOImpl();
            }
            return dao;
        }
    }
    @Override
    public Table getWeeklyTable(String group, String course) throws IOException {
        LocalDate startOfWeek = DateUtils.getStartOfWeek(DateUtils.getTodayDateString());
        HashMap<String, String> params = new HashMap<>() {{
            put("group", group);
            put("course", course);
            put("dateFrom", DateUtils.toString(startOfWeek));
            put("dateTo", DateUtils.toString(startOfWeek.plusDays(6)));
            put("faculty", "1");
        }};
        String serverResponse = Requester.makeRequest(baseUrl, params);
        return parser.table(serverResponse, params);
    }

    @Override
    public Table getNextWeekTable(String group, String course) throws IOException {
        LocalDate startOfWeek = DateUtils.getStartOfWeek(DateUtils.getTodayDateString());
        HashMap<String, String> params = new HashMap<>() {{
            put("group", group);
            put("course", course);
            put("dateFrom", DateUtils.toString(startOfWeek.plusDays(7)));
            put("dateTo", DateUtils.toString(startOfWeek.plusDays(13)));
            put("faculty", "1");
        }};
        String serverResponse;
        try {
            serverResponse = Requester.makeRequest(baseUrl, params);
        } catch (IOException exception) {
            serverResponse = Files.readString(Path.of("GroupsSchedules/" + group + "NextWeek.json"));
        }
        return parser.table(serverResponse, params);
    }

    @Override
    public Table getTodayTable(String group, String course) throws IOException {
        HashMap<String, String> params = new HashMap<>() {{
            put("group", group);
            put("course", course);
            put("dateFrom", DateUtils.getTodayDateString());
            put("dateTo", DateUtils.getTodayDateString());
            put("faculty", "1");
        }};
        return getTable(group, params);
    }

    @Override
    public Table getTomorrowTable(String group, String course) throws IOException {
        HashMap<String, String> params = new HashMap<>() {{
           put("group", group);
           put("course", course);
           put("dateFrom", DateUtils.toString(DateUtils.getTodayDate().plusDays(1)));
           put("dateTo", DateUtils.toString(DateUtils.getTodayDate().plusDays(1)));
           put("faculty", "1");
        }};
        return getTable(group, params);
    }

    @Override
    public Table getCustomTable(String group, String dateFrom, String dateTo, String course) throws IOException {
        if (DateUtils.parseDate(dateTo).isBefore(DateUtils.parseDate(dateFrom))) {
            dateTo = DateUtils.toString(DateUtils.getTodayDate().plusDays(1));
        }
        String finalDateTo = dateTo;
        HashMap<String, String> params =  new HashMap<>(){{
            put("group", group);
            put("dateFrom", dateFrom);
            put("dateTo", finalDateTo);
            put("course", course);
            put("faculty", "1");
        }};
        return getTable(group, params);
    }

    private Table getTable(String group, HashMap<String, String> params) throws IOException {
        String serverResponse;
        try {
            serverResponse = Requester.makeRequest(baseUrl, params);
        } catch (IOException exception) {
            serverResponse = Files.readString(Path.of("GroupsSchedules/" + group + ".json"));
        }
        return parser.table(serverResponse, params);
    }

    public static String getBaseUrl() {
        return baseUrl;
    }
}