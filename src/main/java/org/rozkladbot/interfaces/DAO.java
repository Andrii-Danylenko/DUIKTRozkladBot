package org.rozkladbot.interfaces;

import org.rozkladbot.entities.Table;

import java.io.IOException;

public interface DAO {
    Table getWeeklyTable(String group, String course) throws IOException;

    Table getNextWeekTable(String group, String course) throws IOException;

    Table getTodayTable(String group, String course) throws IOException;

    Table getTomorrowTable(String group, String course) throws IOException;

    Table getCustomTable(String group, String dateFrom, String dateTo, String course) throws IOException;
}