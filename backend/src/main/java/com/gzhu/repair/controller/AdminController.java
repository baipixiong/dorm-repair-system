package com.gzhu.repair.controller;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Result;
import com.gzhu.repair.entity.AuditLog;
import com.gzhu.repair.security.UserContext;
import com.gzhu.repair.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuditService auditService;

    public AdminController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public Result<List<AuditLog>> audit() {
        if (!UserContext.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可以查看操作日志");
        }
        return Result.success(auditService.recent());
    }
}
