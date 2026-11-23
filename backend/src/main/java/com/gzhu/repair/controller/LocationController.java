package com.gzhu.repair.controller;

import com.gzhu.repair.common.BusinessException;
import com.gzhu.repair.common.Result;
import com.gzhu.repair.dto.BuildingRequest;
import com.gzhu.repair.dto.RoomRequest;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.entity.Room;
import com.gzhu.repair.security.UserContext;
import com.gzhu.repair.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/locations")
    public Result<List<Building>> locations() {
        UserContext.require();
        return Result.success(locationService.buildings());
    }

    @PostMapping("/admin/locations/buildings")
    public Result<Building> addBuilding(@Valid @RequestBody BuildingRequest request) {
        return Result.success(locationService.addBuilding(request));
    }

    @PostMapping("/admin/locations/rooms")
    public Result<Room> addRoom(@Valid @RequestBody RoomRequest request) {
        return Result.success(locationService.addRoom(request));
    }

    @GetMapping("/admin/locations/check")
    public Result<String> adminCheck() {
        if (!UserContext.isAdmin()) {
            throw BusinessException.forbidden("仅管理员可以访问");
        }
        return Result.success("ok");
    }
}
