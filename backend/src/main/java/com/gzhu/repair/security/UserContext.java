package com.gzhu.repair.security;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Constants;
import com.gzhu.repair.entity.User;

public final class UserContext {

    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(User user) {
        HOLDER.set(user);
    }

    public static User get() {
        return HOLDER.get();
    }

    public static User require() {
        User user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return user;
    }

    public static boolean isAdmin() {
        User user = get();
        return user != null && Constants.ROLE_ADMIN.equals(user.getRole());
    }

    public static void clear() {
        HOLDER.remove();
    }
}
