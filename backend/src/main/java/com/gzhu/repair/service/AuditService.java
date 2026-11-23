package com.gzhu.repair.service;

import com.gzhu.repair.entity.AuditLog;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.AuditLogRepository;
import com.gzhu.repair.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String action, String detail) {
        log(UserContext.get(), action, detail);
    }

    public void log(User actor, String action, String detail) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorId(actor.getId());
            log.setActorName(actor.getRealName());
        } else {
            log.setActorName("系统");
        }
        log.setAction(action);
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    public List<AuditLog> recent() {
        return auditLogRepository.findTop100ByOrderByCreatedAtDesc();
    }
}
