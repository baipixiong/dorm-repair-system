package com.gzhu.repair.repository;

import com.gzhu.repair.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    boolean existsByName(String name);

    Optional<Building> findByName(String name);

    List<Building> findAllByOrderByIdAsc();
}
