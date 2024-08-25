package org.rozkladbot.entities;

import java.util.ArrayList;
import java.util.List;

public class Table {
    private List<DayOfWeek> dayOfWeeks;
    public Table() {
        dayOfWeeks = new ArrayList<>();
    }
    public List<DayOfWeek> getTable() {
        return dayOfWeeks;
    }
    public void setDayOfWeeks(List<DayOfWeek> dayOfWeeks) {
        this.dayOfWeeks = dayOfWeeks;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (dayOfWeeks.size() > 1) {
            dayOfWeeks.forEach(x -> builder.append(x.toStringIfMany()).append("\n"));
        }
        else {
            dayOfWeeks.forEach(x -> builder.append(x.toStringIfOne()).append("\n"));
        }
        return builder.toString();
    }
    public void addDay(DayOfWeek day) {
        dayOfWeeks.add(day);
    }
}