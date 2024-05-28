package org.rozkladbot.interfaces;

import org.rozkladbot.entities.DelayedCommand;

// Отложенное выполнение, если сервис ложится
public interface DelayedExecutor {
    void executeIfPresent();
    void execute(DelayedCommand delayedCommand);
}
