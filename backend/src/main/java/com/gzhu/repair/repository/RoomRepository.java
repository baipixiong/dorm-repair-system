package com.gzhu.repair.repository;

import com.gzhu.repair.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByBuildingIdOrderByRoomNoAsc(Long buildingId);

    List<Room> findAllByOrderByBuildingIdAscRoomNoAsc();

    boolean existsByBuildingIdAndRoomNo(Long buildingId, String roomNo);

    long countByBuildingId(Long buildingId);
}
