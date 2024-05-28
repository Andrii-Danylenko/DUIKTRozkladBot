package org.rozkladbot.entities;

import org.rozkladbot.constants.EmojiList;
import org.rozkladbot.utils.DateUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DayOfWeek {
    LocalDate date;
    String dayOfWeek;
    List<Classes> pairsList;

    public DayOfWeek(LocalDate date, String dayOfWeek, List<Classes> pairsList) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.pairsList = pairsList;
    }

    public DayOfWeek() {
        pairsList = new ArrayList<>();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<Classes> getPairsList() {
        return pairsList;
    }

    public void setPairsList(List<Classes> pairsList) {
        this.pairsList = pairsList;
    }

    public static String getAmountOfPairs(DayOfWeek day) {
        switch (day.pairsList.size()) {
            case 1:
                return "(1 пара)";
            case 2:
                return "(2 пари)";
            case 3:
                return "(3 пари)";
            case 4:
                return "(4 пари)";
            case 5:
                return "(5 пар)";
            case 6:
                return "(6 пар)";
            default:
                return "(пар немає)";
        }
    }

    public String toStringIfMany() {
        boolean hasPairs = false;
        StringBuilder builder = new StringBuilder();
        String[] splittedDate = DateUtils.toString(date).split("\\.");
        builder.append("%s  <b>%s, %s %s %s</b>".formatted(EmojiList.TIME, dayOfWeek, splittedDate[0], DateUtils.getMonthName(splittedDate[1]), DayOfWeek.getAmountOfPairs(this))).append("\n");
        for (Classes clazz : pairsList) {
            if (!clazz.getPairDetails().isEmpty()) {
                hasPairs = true;
                String[] splittedDetails = clazz.pairDetails.split(",");
                builder.append("""
                        %s  <i>%s</i> - <b>%s</b> %s
                        """.formatted(EmojiList.getPairEmoji(clazz.pairNumber), clazz.pairTime, clazz.subject, splittedDetails[0]));
            }
        }
        return hasPairs ? builder.toString() : builder.append("Пар немає %s! Відпочиваємо%s!\n".formatted(EmojiList.HAPPY, EmojiList.BEER)).toString();
    }

    public String toStringIfOne() {
        boolean hasPairs = false;
        String[] splittedDate = DateUtils.toString(date).split("\\.");
        StringBuilder builder = new StringBuilder();
        builder.append("%s <b>%s, %s %s %s</b>%n".formatted(EmojiList.TIME, dayOfWeek, splittedDate[0], DateUtils.getMonthName(splittedDate[1]), DayOfWeek.getAmountOfPairs(this))).append("\n");
        for (Classes clazz : pairsList) {
            if (!clazz.getPairDetails().isEmpty()) {
                String[] splittedInfo = clazz.pairDetails.split(",");
                hasPairs = true;
                builder.append("""
                        %s пара | <b>%s</b>
                        %s <b>%s</b> %s
                        %s%s
                        %s %s
                        
                        """.formatted(EmojiList.getPairEmoji(clazz.pairNumber), clazz.pairTime,
                        EmojiList.SUBJECT, clazz.subject, splittedInfo[0],
                        EmojiList.LECTOR, splittedInfo[2],
                        EmojiList.ROOM, splittedInfo[1]));
            }
        }
        return hasPairs ? builder.toString() : builder.append("Пар немає %s! Відпочиваємо%s!\n".formatted(EmojiList.HAPPY, EmojiList.BEER)).toString();
    }
}