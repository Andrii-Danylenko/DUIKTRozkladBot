package org.rozkladbot.dao;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.constants.UserState;
import org.rozkladbot.entities.Group;
import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;
import org.rozkladbot.interfaces.DAO;
import org.rozkladbot.utils.ConsoleLineLogger;
import org.rozkladbot.utils.data.GroupUtils;
import org.rozkladbot.utils.date.DateUtils;
import org.rozkladbot.utils.schedule.ScheduleParser;
import org.rozkladbot.web.Requester;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.*;

@Repository("DAOImpl")
public class DAOImpl implements DAO {
    private final ScheduleParser parser = new ScheduleParser();
    private static final String baseUrl = "https://skedy.api.yacode.dev/v1/student/schedule?";
    private static volatile DAOImpl dao;
    private static final ConsoleLineLogger<DAOImpl> log = new ConsoleLineLogger<>(DAOImpl.class);

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
    public Table getWeeklyTable(User user) throws ExecutionException, InterruptedException {
        LocalDate startOfWeek = DateUtils.getStartOfWeek(DateUtils.getTodayDateString());
        HashMap<String, String> params = new HashMap<>() {{
            put("group", String.valueOf(user.getGroup().getGroupNumber()));
            put("course", user.getGroup().getCourse());
            put("dateFrom", DateUtils.toString(startOfWeek));
            put("dateTo", DateUtils.toString(startOfWeek.plusDays(6)));
            put("faculty", "1");
        }};
        return getTable(user, params, UserState.AWAITING_THIS_WEEK_SCHEDULE);
    }

    @Override
    public Table getNextWeekTable(User user) throws ExecutionException, InterruptedException {
        LocalDate startOfWeek = DateUtils.getStartOfWeek(DateUtils.getTodayDateString());
        HashMap<String, String> params = new HashMap<>() {{
            put("group", String.valueOf(user.getGroup().getGroupNumber()));
            put("course", user.getGroup().getCourse());
            put("dateFrom", DateUtils.toString(startOfWeek.plusDays(7)));
            put("dateTo", DateUtils.toString(startOfWeek.plusDays(13)));
            put("faculty", "1");
        }};
        return getTable(user, params, UserState.AWAITING_NEXT_WEEK_SCHEDULE);
    }

    @Override
    public Table getTodayTable(User user) throws ExecutionException, InterruptedException {
        HashMap<String, String> params = new HashMap<>() {{
            put("group", String.valueOf(user.getGroup().getGroupNumber()));
            put("course", user.getGroup().getCourse());
            put("dateFrom", DateUtils.getTodayDateString());
            put("dateTo", DateUtils.getTodayDateString());
            put("faculty", "1");
        }};
        return getTable(user, params, UserState.AWAITING_THIS_WEEK_SCHEDULE);
    }

    @Override
    public Table getTomorrowTable(User user) throws ExecutionException, InterruptedException {
        HashMap<String, String> params = new HashMap<>() {{
            put("group", String.valueOf(user.getGroup().getGroupNumber()));
            put("course", user.getGroup().getCourse());
            put("dateFrom", DateUtils.toString(DateUtils.getTodayDate().plusDays(1)));
            put("dateTo", DateUtils.toString(DateUtils.getTodayDate().plusDays(1)));
            put("faculty", "1");
        }};
        return getTable(user, params, DateUtils.getDayOfWeek(DateUtils.getTodayDateString()).equalsIgnoreCase("Неділя")
                ? UserState.AWAITING_NEXT_WEEK_SCHEDULE : UserState.AWAITING_THIS_WEEK_SCHEDULE);
    }

    @Override
    public Table getCustomTable(User user, String group, String dateFrom, String dateTo) throws ExecutionException, InterruptedException {
        if (DateUtils.parseDate(dateTo).isBefore(DateUtils.parseDate(dateFrom))) {
            dateTo = DateUtils.toString(DateUtils.getTodayDate().plusDays(1));
        }
        String finalDateTo = dateTo;
        HashMap<String, String> params = new HashMap<>() {{
            put("group", Group.getGroupNumberAsString(group));
            put("dateFrom", dateFrom);
            put("dateTo", finalDateTo);
            put("course", user.getGroup().getCourse());
            put("faculty", "1");
        }};
        return getTable(user, params, UserState.IDLE);
    }

    private Table getTable(User user, HashMap<String, String> params, UserState userState) throws ExecutionException, InterruptedException {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Requester.makeRequest(baseUrl, params);
            } catch (IOException e) {
                log.error("Помилка під час запиту до API! Привід: %s".formatted(e.getMessage()));
                throw new RuntimeException("");
            }
        }, Executors.newSingleThreadExecutor()).exceptionally((ex) -> {
            if (ex != null) System.out.printf("Executor executed unsuccessfully! Error message: %s%n", ex.getMessage());
            String result;
            try {
                if (userState == UserState.AWAITING_THIS_WEEK_SCHEDULE) {
                    result = Files.readString(Path.of("groupsSchedules/%s(%d)_thisWeek.json".formatted(user.getGroup().getGroupName(), user.getGroup().getGroupNumber())));
                    System.out.printf("groupsSchedules/%s(%d)_thisWeek.json%n", user.getGroup().getGroupName(), user.getGroup().getGroupNumber());
                } else if (userState == UserState.AWAITING_NEXT_WEEK_SCHEDULE) {
                    result = Files.readString(Path.of("groupsSchedules/%s(%d)_nextWeek.json".formatted(user.getGroup().getGroupName(), user.getGroup().getGroupNumber())));
                    System.out.printf("groupsSchedules/%s(%d)_nextWeek.json".formatted(user.getGroup().getGroupName(), user.getGroup().getGroupNumber()));
                } else {
                    result = "null";
                }
            } catch (IOException exception) {
                log.error("Помилка під час парсингу локального файлу! Привід: %s".formatted(exception.getCause()));
                throw new RuntimeException(exception);
            }
            return result;
        }).thenApply(result -> parser.getTable(result, params)).get();
    }

    public static String getBaseUrl() {
        return baseUrl;
    }
}