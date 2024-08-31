package org.rozkladbot.constants;

public enum UserState {
    MAIN_MENU,
    AWAITING_INPUT,
    AWAITING_THIS_WEEK_SCHEDULE,
    AWAITING_THIS_DAY_SCHEDULE,
    AWAITING_NEXT_WEEK_SCHEDULE,
    AWAITING_NEXT_DAY_SCHEDULE,
    AWAITING_CUSTOM_SCHEDULE,
    AWAITING_CUSTOM_SCHEDULE_INPUT,
    SETTINGS,
    AWAITING_INSTITUTE,
    AWAITING_COURSE,
    AWAITING_GROUP,
    REGISTERED,
    STOP,
    GROUP_CHANGE,
    NULL_GROUP,
    ADMIN_SEND_MESSAGE,
    IDLE,
    AWAITING_FORWARD_MESSAGE;
    public static UserState getUserStateFromString(String state) {
        return UserState.valueOf(state);
    }
}