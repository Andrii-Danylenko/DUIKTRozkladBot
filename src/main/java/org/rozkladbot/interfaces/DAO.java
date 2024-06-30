package org.rozkladbot.interfaces;

import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface DAO {
    Table getWeeklyTable(User user) throws IOException;

    Table getNextWeekTable(User user) throws IOException;

    Table getTodayTable(User user) throws IOException;

    Table getTomorrowTable(User user) throws IOException;

    Table getCustomTable(User user, String dateFrom, String dateTo);
}