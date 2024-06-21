package org.rozkladbot.constants;

public enum UserRole {
    USER,
    ADMIN;
    public static UserRole getUserRoleFromString(String state) {
        return UserRole.valueOf(state);
    }
}
