package org.rozkladbot.utils;

import org.rozkladbot.constants.Colors;
import org.rozkladbot.utils.date.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Логгирование в файл nohup.out
public class ConsoleLineLogger<T> {
    private Class<T> type;
    private final Logger log;
    public void info(String message) {
        String logMessage = "%s(%s) [%s] %s%s%n".formatted(Colors.ANSI_YELLOW, type.getSimpleName(), DateUtils.now(), message, Colors.ANSI_RESET);
        System.out.printf(logMessage);
        log.info(logMessage);
    }

    public void success(String message) {
        String logMessage = "%s(%s) [%s] %s%s%n".formatted(Colors.ANSI_GREEN, type.getSimpleName(), DateUtils.now(), message, Colors.ANSI_RESET);
        System.out.printf(logMessage);
        log.info(logMessage);
    }

    public void error(String message) {
        String logMessage = "%s(%s) [%s] %s%s%n".formatted(Colors.ANSI_RED, type.getSimpleName(), DateUtils.now(), message, Colors.ANSI_RESET);
        System.out.printf(logMessage);
        log.error(logMessage);
    }
    public ConsoleLineLogger(Class<T> type) {
        this.type = type;
        log = LoggerFactory.getLogger(type);
    }
    public Class<T> getType() {
        return type;
    }
}
