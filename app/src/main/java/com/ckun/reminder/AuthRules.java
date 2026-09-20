package com.ckun.reminder;

import java.util.regex.Pattern;

public final class AuthRules {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private AuthRules() {}
    public static String validateEmail(String email) {
        return email == null || !EMAIL.matcher(email.trim()).matches() ? "请输入有效的邮箱地址" : null;
    }
    public static String validatePassword(String password) {
        if (password == null || password.length() < 8) return "密码至少需要 8 个字符";
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) return "密码需要同时包含字母和数字";
        return null;
    }
}
