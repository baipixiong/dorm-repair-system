package com.gzhu.repair.repository;

import com.gzhu.repair.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findByRoleAndStatusOrderByIdAsc(String role, Integer status);

    List<User> findByRoleOrderByIdAsc(String role);

    long countByRole(String role);
}
