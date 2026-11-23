package com.gzhu.repair.service;

import com.gzhu.repair.common.Constants;
import com.gzhu.repair.entity.Ticket;
import com.gzhu.repair.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecs {

    private TicketSpecs() {
    }

    public static Specification<Ticket> scoped(User user) {
        return (root, query, cb) -> {
            if (Constants.ROLE_ADMIN.equals(user.getRole())) {
                return cb.conjunction();
            }
            if (Constants.ROLE_TECHNICIAN.equals(user.getRole())) {
                return cb.equal(root.get("assigneeId"), user.getId());
            }
            return cb.equal(root.get("studentId"), user.getId());
        };
    }

    public static Specification<Ticket> filter(String status,
                                               String category,
                                               Long buildingId,
                                               String keyword,
                                               LocalDateTime start,
                                               LocalDateTime end) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (buildingId != null) {
                predicates.add(cb.equal(root.get("buildingId"), buildingId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("ticketNo"), like),
                        cb.like(root.get("title"), like),
                        cb.like(root.get("description"), like)
                ));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
