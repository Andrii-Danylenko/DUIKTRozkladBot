package org.rozkladbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Логгирование в файл nohup.out
public class ConsoleLineLogger<T> {
    private Class<T> type;
    private final Logger log;
    public void logAttempt(String message) {
        String logMessage = "[%s] %s%n".formatted(DateUtils.now(), message);
        System.out.printf(logMessage);
        log.info(logMessage);
    }

    public void logIfSuccess(String message) {
        String logMessage = "[%s] %s%n".formatted(DateUtils.now(), message);
        System.out.printf(logMessage);
        log.info(logMessage);
    }

    public void logIfError(String message) {
        String logMessage = "[%s] %s%n".formatted(DateUtils.now(), message);
        System.err.printf(logMessage);
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
