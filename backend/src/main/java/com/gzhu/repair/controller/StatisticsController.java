package com.gzhu.repair.controller;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Result;
import com.gzhu.repair.security.UserContext;
import com.gzhu.repair.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        UserContext.require();
        return Result.success(statisticsService.overview(buildingId, parseDate(start), parseDate(end)));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw BusinessException.badRequest("日期格式应为 yyyy-MM-dd");
        }
    }
}
