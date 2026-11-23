package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Constants;
import com.gzhu.repair.dto.ChangePasswordRequest;
import com.gzhu.repair.dto.LoginRequest;
import com.gzhu.repair.dto.RegisterRequest;
import com.gzhu.repair.dto.UpdateProfileRequest;
import com.gzhu.repair.entity.Room;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.RoomRepository;
import com.gzhu.repair.repository.UserRepository;
import com.gzhu.repair.security.JwtUtil;
import com.gzhu.repair.security.UserContext;
import com.gzhu.repair.vo.LoginVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RoomRepository roomRepository,
                       JwtUtil jwtUtil,
                       UserService userService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.auditService = auditService;
    }

    public LoginVO login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> BusinessException.badRequest("未找到该用户"));
        if (!request.getPassword().equals(user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已被停用，请联系管理员");
        }
        String token = jwtUtil.generate(user);
        auditService.log(user, "登录", "用户 " + user.getUsername() + " 登录系统");
        return new LoginVO(token, userService.enrich(user));
    }

    @Transactional
    public LoginVO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw BusinessException.conflict("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName().trim());
        user.setRole(Constants.ROLE_STUDENT);
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setGender(0);
        user.setBuildingId(request.getBuildingId());
        user.setRoomId(request.getRoomId());
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> BusinessException.badRequest("房间不存在"));
            user.setBuildingId(room.getBuildingId());
        }
        user.setStatus(1);
        userRepository.save(user);
        auditService.log(user, "注册", "新学生账号 " + user.getUsername() + " 注册成功");
        String token = jwtUtil.generate(user);
        return new LoginVO(token, userService.enrich(user));
    }

    public User me() {
        return userService.enrich(UserContext.require());
    }

    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        User user = UserContext.require();
        user.setRealName(request.getRealName().trim());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> BusinessException.badRequest("房间不存在"));
            user.setRoomId(room.getId());
            user.setBuildingId(room.getBuildingId());
        } else if (request.getBuildingId() != null) {
            user.setBuildingId(request.getBuildingId());
            user.setRoomId(null);
        }
        userRepository.save(user);
        auditService.log(user, "修改资料", "用户 " + user.getUsername() + " 更新了个人资料");
        return userService.enrich(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = UserContext.require();
        if (!request.getOldPassword().equals(user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw BusinessException.badRequest("新密码不能与原密码相同");
        }
        user.setPassword(request.getNewPassword());
        userRepository.save(user);
        auditService.log(user, "修改密码", "用户 " + user.getUsername() + " 修改了登录密码");
    }
}
