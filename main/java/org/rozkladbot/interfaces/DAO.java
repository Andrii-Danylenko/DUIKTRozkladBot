package org.rozkladbot.interfaces;

import org.rozkladbot.entities.Table;
import org.rozkladbot.entities.User;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface DAO {
    Table getWeeklyTable(User user) throws IOException, ExecutionException, InterruptedException;

    Table getNextWeekTable(User user) throws IOException, ExecutionException, InterruptedException;

    Table getTodayTable(User user) throws IOException, ExecutionException, InterruptedException;

    Table getTomorrowTable(User user) throws IOException, ExecutionException, InterruptedException;

    Table getCustomTable(User user, String group, String dateFrom, String dateTo) throws ExecutionException, InterruptedException;
}