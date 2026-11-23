package com.gzhu.repair.controller;

import com.gzhu.repair.common.Result;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("database", "UP");
        return Result.success(result);
    }
}
