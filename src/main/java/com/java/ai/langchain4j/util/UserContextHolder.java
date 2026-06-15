package com.java.ai.langchain4j.util;

/**
 * 当前请求用户上下文。
 */
public final class UserContextHolder {

    private static final InheritableThreadLocal<Long> USER_ID_HOLDER = new InheritableThreadLocal<>();
    private static final InheritableThreadLocal<String> USERNAME_HOLDER = new InheritableThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(Long userId, String username) {
        USER_ID_HOLDER.set(userId);
        USERNAME_HOLDER.set(username);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
