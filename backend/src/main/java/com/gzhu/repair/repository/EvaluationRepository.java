package com.gzhu.repair.repository;

import com.gzhu.repair.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    List<Evaluation> findByTicketIdIn(List<Long> ticketIds);
}
