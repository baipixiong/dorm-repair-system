package com.gzhu.repair;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    @Test
    void seedPasswordHashMatchesDefaultPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = "$2a$10$0LzZm6Waf1F7zho5G6QZo.0l4oZTgzPgoct0A6aAEsKDTC/BTyTLO";
        assertTrue(encoder.matches("123456", hash), "演示账号密码哈希必须对应 123456");
    }
}
