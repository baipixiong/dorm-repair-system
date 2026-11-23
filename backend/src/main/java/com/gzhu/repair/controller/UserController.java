package com.gzhu.repair.controller;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.PageResult;
import com.gzhu.repair.common.Result;
import com.gzhu.repair.dto.CreateUserRequest;
import com.gzhu.repair.dto.StatusRequest;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.security.UserContext;
import com.gzhu.repair.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public Result<User> me() {
        return Result.success(userService.enrich(UserContext.require()));
    }

    @GetMapping("/users")
    public Result<PageResult<User>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireAdmin();
        return Result.success(userService.list(keyword, role, page, size));
    }

    @PostMapping("/admin/users")
    public Result<User> create(@Valid @RequestBody CreateUserRequest request) {
        requireAdmin();
        return Result.success(userService.create(request));
    }

    @PutMapping("/admin/users/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        requireAdmin();
        userService.updateStatus(id, request.getStatus());
        return Result.success();
    }

    @GetMapping("/technicians")
    public Result<List<User>> technicians() {
        requireAdmin();
        return Result.success(userService.technicians());
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可以执行该操作");
        }
    }
}
