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
    AWAITING_REGISTRATION,
    REGISTERED,
    STOP,
    GROUP_CHANGE,
    NULL_GROUP,
    ADMIN_SEND_MESSAGE,
    IDLE;
    public static UserState getUserStateFromString(String state) {
        return UserState.valueOf(state);
    }
}