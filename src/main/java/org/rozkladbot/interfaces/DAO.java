package org.rozkladbot.interfaces;

import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface DAO {
    Table getWeeklyTable(String group, String course) throws IOException;

    Table getNextWeekTable(String group, String course) throws IOException;

    Table getTodayTable(String group, String course) throws IOException;

    Table getTomorrowTable(String group, String course) throws IOException;

    Table getCustomTable(String group, String dateFrom, String dateTo, String course) throws IOException;
}