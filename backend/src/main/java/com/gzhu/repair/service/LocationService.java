package com.gzhu.repair.service;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.dto.BuildingRequest;
import com.gzhu.repair.dto.RoomRequest;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.entity.Room;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.repository.RoomRepository;
import com.gzhu.repair.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocationService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final AuditService auditService;

    public LocationService(BuildingRepository buildingRepository,
                           RoomRepository roomRepository,
                           AuditService auditService) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.auditService = auditService;
    }

    public List<Building> buildings() {
        List<Building> buildings = buildingRepository.findAllByOrderByIdAsc();
        List<Room> rooms = roomRepository.findAllByOrderByBuildingIdAscRoomNoAsc();
        Map<Long, List<Room>> grouped = new HashMap<>();
        for (Room room : rooms) {
            grouped.computeIfAbsent(room.getBuildingId(), k -> new ArrayList<>()).add(room);
        }
        for (Building building : buildings) {
            List<Room> list = grouped.getOrDefault(building.getId(), new ArrayList<>());
            for (Room room : list) {
                room.setBuildingName(building.getName());
            }
            building.setRooms(list);
            building.setRoomCount((long) list.size());
        }
        return buildings;
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
        building.setRooms(new ArrayList<>());
        building.setRoomCount(0L);
        return building;
    }

    @Transactional
    public Room addRoom(RoomRequest request) {
        requireAdmin();
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> BusinessException.notFound("楼栋不存在"));
        String roomNo = request.getRoomNo().trim();
        if (roomRepository.existsByBuildingIdAndRoomNo(building.getId(), roomNo)) {
            throw BusinessException.conflict("该楼栋下已存在房间 " + roomNo);
        }
        Room room = new Room();
        room.setBuildingId(building.getId());
        room.setRoomNo(roomNo);
        room.setFloor(request.getFloor());
        room.setRoomType(request.getRoomType());
        room.setRemark(request.getRemark());
        roomRepository.save(room);
        auditService.log("新增房间", "新增房间 " + building.getName() + " " + roomNo);
        room.setBuildingName(building.getName());
        return room;
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可以执行该操作");
        }
    }
}
