package org.rozkladbot.utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;

public class LimitedDeque<T> extends ArrayDeque<T> {
    private final int maxSize;

    public LimitedDeque(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    @Override
    public void addFirst(@NotNull T element) {
        super.addFirst(element);
        if (size() > maxSize) {
            removeLast();
        }
    }

    @Override
    public void addLast(@NotNull T element) {
        super.addLast(element);
        if (size() > maxSize) {
            removeFirst();
        }
    }

    @Override
    public boolean offerFirst(@NotNull T element) {
        super.offerFirst(element);
        if (size() > maxSize) {
            removeLast();
        }
        return true;
    }

    @Override
    public boolean offerLast(@NotNull T element) {
        super.offerLast(element);
        if (size() > maxSize) {
            removeFirst();
        }
        return true;
    }
    @Override
    public String toString() {
        String first = getFirst().toString() == null ? "null" : getFirst().toString();
        String last = getLast().toString() == null ? "null" : getFirst().toString();
        return "First: %s \nLast: %s".formatted(first, last);
    }
}
