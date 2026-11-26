package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Constants;
import com.gzhu.repair.common.PageResult;
import com.gzhu.repair.dto.CreateUserRequest;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       BuildingRepository buildingRepository,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.auditService = auditService;
    }

    public User enrich(User user) {
        if (user == null) {
            return null;
        }
        user.setRoleLabel(Constants.ROLE_LABELS.getOrDefault(user.getRole(), user.getRole()));
        if (user.getBuildingId() != null) {
            buildingRepository.findById(user.getBuildingId())
                    .ifPresent(b -> user.setBuildingName(b.getName()));
        }
        return user;
    }

    public PageResult<User> list(String keyword, String role, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(root.get("realName"), like),
                        cb.like(root.get("studentNo"), like),
                        cb.like(root.get("phone"), like)
                ));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "id")));
        result.getContent().forEach(this::enrich);
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, size);
    }

    @Transactional
    public User create(CreateUserRequest request) {
        String role = request.getRole();
        if (!Constants.ROLE_STUDENT.equals(role)
                && !Constants.ROLE_TECHNICIAN.equals(role)
                && !Constants.ROLE_ADMIN.equals(role)) {
            throw BusinessException.badRequest("角色不合法");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw BusinessException.conflict("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName().trim());
        user.setRole(role);
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(request.getGender() == null ? 0 : request.getGender());
        user.setBuildingId(request.getBuildingId());
        user.setRoomNo(request.getRoomNo());
        user.setStatus(1);
        userRepository.save(user);
        auditService.log("新增用户", "创建账号 " + user.getUsername() + "（" + user.getRealName() + "）");
        return enrich(user);
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        User current = com.gzhu.repair.security.UserContext.require();
        User target = userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
        if (target.getId().equals(current.getId())) {
            throw BusinessException.badRequest("不能修改自己的账号状态");
        }
        if (Constants.ROLE_ADMIN.equals(target.getRole())) {
            throw BusinessException.badRequest("不能停用管理员账号");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态值不合法");
        }
        target.setStatus(status);
        userRepository.save(target);
        auditService.log("修改用户状态", (status == 1 ? "启用" : "停用") + "账号 " + target.getUsername());
    }

    public List<User> technicians() {
        List<User> list = userRepository.findByRoleAndStatusOrderByIdAsc(Constants.ROLE_TECHNICIAN, 1);
        list.forEach(this::enrich);
        return list;
    }
}
