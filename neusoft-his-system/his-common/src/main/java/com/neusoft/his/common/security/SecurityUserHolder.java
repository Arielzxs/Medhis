package com.neusoft.his.common.security;

public final class SecurityUserHolder {
    private static final ThreadLocal<SecurityUser> HOLDER = new ThreadLocal<>();

    private SecurityUserHolder() {
    }

    public static void set(SecurityUser user) {
        HOLDER.set(user);
    }

    public static SecurityUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
