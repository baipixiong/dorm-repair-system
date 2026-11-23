package com.gzhu.repair.security;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public AuthInterceptor(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw BusinessException.unauthorized("请先登录");
        }
        Claims claims = jwtUtil.parse(auth.substring(7).trim());
        if (claims == null) {
            throw BusinessException.unauthorized("登录已过期，请重新登录");
        }
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            throw BusinessException.unauthorized("登录凭证无效，请重新登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("账号不存在，请重新登录"));
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("账号已被停用，请联系管理员");
        }
        UserContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
