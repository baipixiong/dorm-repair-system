package com.gzhu.repair.service;

import com.gzhu.repair.common.Constants;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.entity.Evaluation;
import com.gzhu.repair.entity.Ticket;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.repository.EvaluationRepository;
import com.gzhu.repair.repository.TicketRepository;
import com.gzhu.repair.security.UserContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    private final TicketRepository ticketRepository;
    private final EvaluationRepository evaluationRepository;
    private final BuildingRepository buildingRepository;

    public StatisticsService(TicketRepository ticketRepository,
                             EvaluationRepository evaluationRepository,
                             BuildingRepository buildingRepository) {
        this.ticketRepository = ticketRepository;
        this.evaluationRepository = evaluationRepository;
        this.buildingRepository = buildingRepository;
    }

    public Map<String, Object> overview(Long buildingId, LocalDate start, LocalDate end) {
        User user = UserContext.require();
        LocalDateTime startTime = start == null ? null : start.atStartOfDay();
        LocalDateTime endTime = end == null ? null : end.atTime(23, 59, 59);
        Specification<Ticket> spec = TicketSpecs.scoped(user)
                .and(TicketSpecs.filter(null, null, buildingId, null, startTime, endTime));
        List<Ticket> tickets = ticketRepository.findAll(spec);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", tickets.size());

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (String status : Constants.STATUS_LABELS.keySet()) {
            statusCounts.put(status, 0L);
        }
        for (Ticket ticket : tickets) {
            statusCounts.merge(ticket.getStatus(), 1L, Long::sum);
        }
        statusCounts.forEach(result::put);

        long completed = statusCounts.getOrDefault(Constants.STATUS_COMPLETED, 0L);
        long cancelled = statusCounts.getOrDefault(Constants.STATUS_CANCELLED, 0L);
        long unfinished = tickets.size() - completed - cancelled;
        result.put("unfinished", unfinished);
        result.put("completionRate", tickets.isEmpty() ? 0
                : Math.round(completed * 1000.0 / tickets.size()) / 10.0);

        LocalDate today = LocalDate.now();
        long todayNew = tickets.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(today))
                .count();
        result.put("todayNew", todayNew);

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        for (Map.Entry<String, String> entry : Constants.CATEGORY_LABELS.entrySet()) {
            long count = tickets.stream().filter(t -> entry.getKey().equals(t.getCategory())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("name", entry.getValue());
            item.put("value", count);
            categoryStats.add(item);
        }
        result.put("categoryStats", categoryStats);

        List<Map<String, Object>> priorityStats = new ArrayList<>();
        for (Map.Entry<String, String> entry : Constants.PRIORITY_LABELS.entrySet()) {
            long count = tickets.stream().filter(t -> entry.getKey().equals(t.getPriority())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", entry.getKey());
            item.put("name", entry.getValue());
            item.put("value", count);
            priorityStats.add(item);
        }
        result.put("priorityStats", priorityStats);

        Map<Long, Building> buildingMap = buildingRepository.findAllById(
                        tickets.stream().map(Ticket::getBuildingId).filter(java.util.Objects::nonNull)
                                .collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Building::getId, b -> b));
        Map<String, Long> buildingCounts = new LinkedHashMap<>();
        for (Ticket ticket : tickets) {
            Building building = buildingMap.get(ticket.getBuildingId());
            String name = building == null ? "未知楼栋" : building.getName();
            buildingCounts.merge(name, 1L, Long::sum);
        }
        List<Map<String, Object>> buildingStats = buildingCounts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .sorted(Comparator.comparingLong((Map<String, Object> m) -> (Long) m.get("value")).reversed())
                .collect(Collectors.toList());
        result.put("buildingStats", buildingStats);

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long created = tickets.stream()
                    .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(day))
                    .count();
            long done = tickets.stream()
                    .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().equals(day))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(MONTH_DAY));
            item.put("created", created);
            item.put("completed", done);
            trend.add(item);
        }
        result.put("trend", trend);

        double avgHours = tickets.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCompletedAt() != null)
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getCompletedAt()).toMinutes())
                .average()
                .orElse(0) / 60.0;
        result.put("avgCompletionHours", Math.round(avgHours * 10) / 10.0);

        List<Long> ticketIds = tickets.stream().map(Ticket::getId).collect(Collectors.toList());
        List<Evaluation> evaluations = ticketIds.isEmpty()
                ? new ArrayList<>() : evaluationRepository.findByTicketIdIn(ticketIds);
        double avgScore = evaluations.stream().mapToInt(Evaluation::getScore).average().orElse(0);
        result.put("avgScore", Math.round(avgScore * 10) / 10.0);
        result.put("evaluationCount", evaluations.size());

        return result;
    }
}
