package org.rozkladbot.utils.date;

import org.springframework.cglib.core.Local;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateUtils {
    private static final Locale locale = new Locale("uk");
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withLocale(locale);
    public static final ZoneId zoneId = ZoneId.of("Europe/Kiev");

    public static boolean isDayOfWeek(String info) {
        return switch (info.toLowerCase()) {
            case "пн", "вт", "ср", "чт", "пт", "сб", "нд" -> true;
            default -> false;
        };
    }

    public static String getFullName(String name) {
        return switch (name.toLowerCase()) {
            case "пн" -> "Понеділок";
            case "вт" -> "Вівторок";
            case "ср" -> "Середа";
            case "чт" -> "Четвер";
            case "пт" -> "П'ятниця";
            case "сб" -> "Субота";
            case "нд" -> "Неділя";
            default -> "Undefined";
        };
    }

    public static LocalDate getTodayDate() {
        return ZonedDateTime.now(zoneId).toLocalDate();
    }

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date, formatter);
    }

    public static LocalDate addDays(LocalDate date, int daysToAdd) {
        return date.plusDays(daysToAdd);
    }

    public static String getTodayDateString() {
        return ZonedDateTime.now(zoneId).format(formatter);
    }

    public static String toString(LocalDate date) {
        return date.format(formatter);
    }

    public static String getDayOfWeek(String date) {
        String dayOfWeek = LocalDate.parse(date, formatter).getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        return dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1);
    }

    public static LocalDate getStartOfWeek(String date) {
        LocalDate inputDate = LocalDate.parse(date, formatter);
        DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;
        int daysUntilStartOfWeek = inputDate.getDayOfWeek().getValue() - firstDayOfWeek.getValue();
        return inputDate.minusDays(daysUntilStartOfWeek);
    }

    public static String getPairTime(String pairNumber) {
        return switch (pairNumber) {
            case "1" -> "8:00-9:35";
            case "2" -> "9:45-11:20";
            case "3" -> "11:45-13:20";
            case "4" -> "13:30-15:05";
            case "5" -> "15:15-16:50";
            case "6" -> "17:00-18:35";
            default -> "18:45-20:15";
        };
    }

    public static String getMonthName(String date) {
        int month = Integer.parseInt(date);
        return switch (month) {
            case 1 -> "січня";
            case 2 -> "лютого";
            case 3 -> "березня";
            case 4 -> "квітня";
            case 5 -> "травня";
            case 6 -> "червня";
            case 7 -> "липня";
            case 8 -> "серпня";
            case 9 -> "вересня";
            case 10 -> "жовтня";
            case 11 -> "листопада";
            case 12 -> "грудня";
            default -> "undefined";
        };
    }
    public static LocalDateTime timeOfNow() {
        return ZonedDateTime.now(zoneId).toLocalDateTime();
    }
    public static LocalDate instantToLocalDate(Instant instant) {
        LocalDate localDate = LocalDate.ofInstant(instant, zoneId);
        return localDate;
    }
    public static String now() {
        return LocalDateTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS dd.MM.yyyy"));
    }
}
