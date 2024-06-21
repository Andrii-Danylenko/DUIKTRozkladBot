package org.rozkladbot.dao;

import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.Table;
import org.rozkladbot.interfaces.DAO;
import org.rozkladbot.utils.DateUtils;
import org.rozkladbot.utils.ScheduleParser;
import org.rozkladbot.web.Requester;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Repository("DAOImpl")
public class DAOImpl implements DAO {
    private final ScheduleParser parser = new ScheduleParser();
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
        return getTable(group, params, UserState.AWAITING_THIS_WEEK_SCHEDULE);
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
        return getTable(group, params, UserState.AWAITING_NEXT_WEEK_SCHEDULE);
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
        return getTable(group, params, UserState.AWAITING_THIS_WEEK_SCHEDULE);
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
        return getTable(group, params, DateUtils.getDayOfWeek(DateUtils.getTodayDateString()).equalsIgnoreCase("Неділя")
                ? UserState.AWAITING_NEXT_WEEK_SCHEDULE : UserState.AWAITING_THIS_WEEK_SCHEDULE);
    }

    @Override
    public Table getCustomTable(String group, String dateFrom, String dateTo, String course) throws IOException {
        if (DateUtils.parseDate(dateTo).isBefore(DateUtils.parseDate(dateFrom))) {
            dateTo = DateUtils.toString(DateUtils.getTodayDate().plusDays(1));
        }
        String finalDateTo = dateTo;
        HashMap<String, String> params = new HashMap<>() {{
            put("group", group);
            put("dateFrom", dateFrom);
            put("dateTo", finalDateTo);
            put("course", course);
            put("faculty", "1");
        }};
        return getTable(group, params, UserState.IDLE);
    }

    private Table getTable(String group, HashMap<String, String> params, UserState userState) {
        Table table;
        try {
          table = CompletableFuture.supplyAsync(() -> {
                try {
                    return Requester.makeRequest(baseUrl, params);
                } catch (IOException e) {
                    System.out.println("Помилка під час запиту до API!");
                    throw new RuntimeException(e);
                }
            }, Executors.newSingleThreadExecutor()).exceptionally((ex) -> {
                if (ex != null) System.out.printf("Executor executed unsuccessfully! Error message: %s%n", ex.getMessage());
                String result;
                try {
                    if (userState == UserState.AWAITING_THIS_WEEK_SCHEDULE) {
                        result = Files.readString(Path.of("groupsSchedules/" + group + ".json"));
                    } else if (userState == UserState.AWAITING_NEXT_WEEK_SCHEDULE) {
                        result = Files.readString(Path.of("groupsSchedules/" + group + "NextWeek.json"));
                    } else {
                        result = "";
                    }
                } catch (IOException exception) {
                    System.out.println("Помилка під час парсингу локального файлу!");
                    throw new RuntimeException(exception);
                }
                return result;
            }).thenApply(result -> parser.getTable(result, params)).get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.printf("Помилка під час асинхронного виконання! Привід: %n%s", e.getCause());
            table = new Table();
        }
        return table;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }
}