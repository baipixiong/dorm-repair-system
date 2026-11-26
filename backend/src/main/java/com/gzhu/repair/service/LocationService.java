package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.dto.BuildingRequest;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LocationService {

    private final BuildingRepository buildingRepository;
    private final AuditService auditService;

    public LocationService(BuildingRepository buildingRepository,
                           AuditService auditService) {
        this.buildingRepository = buildingRepository;
        this.auditService = auditService;
    }

    public List<Building> buildings() {
        return buildingRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public Building addBuilding(BuildingRequest request) {
        requireAdmin();
        String name = request.getName().trim();
        if (buildingRepository.existsByName(name)) {
            throw BusinessException.conflict("楼栋名称已存在");
        }
        Building building = new Building();
        building.setName(name);
        building.setCampus(request.getCampus());
        building.setAddress(request.getAddress());
        building.setRemark(request.getRemark());
        buildingRepository.save(building);
        auditService.log("新增楼栋", "新增楼栋 " + name);
        return building;
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可以执行该操作");
        }
    }
}
