package com.gzhu.repair.init;

import com.gzhu.repair.common.Constants;
import com.gzhu.repair.entity.Building;
import com.gzhu.repair.entity.User;
import com.gzhu.repair.repository.BuildingRepository;
import com.gzhu.repair.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;

    public DataInitializer(UserRepository userRepository,
                           BuildingRepository buildingRepository) {
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initBuildings();
        createUserIfMissing("admin", "系统管理员", Constants.ROLE_ADMIN, "A0001", "020-39366000", null, null);
        createUserIfMissing("student", "陈嘉怡", Constants.ROLE_STUDENT, "2022100101", "13800000001",
                buildingId("梅苑1栋"), "101");
        createUserIfMissing("student2", "王志强", Constants.ROLE_STUDENT, "2022100102", "13800000002",
                buildingId("梅苑2栋"), "101");
        createUserIfMissing("repair", "李伟明", Constants.ROLE_TECHNICIAN, "W0001", "13900000001", null, null);
        createUserIfMissing("repair2", "黄师傅", Constants.ROLE_TECHNICIAN, "W0002", "13900000002", null, null);
        log.info("演示账号检查完成，默认密码：{}", DEFAULT_PASSWORD);
    }

    private void initBuildings() {
        if (buildingRepository.count() > 0) {
            return;
        }
        String[] zones = {"梅苑", "兰苑", "竹苑", "菊苑"};
        for (String zone : zones) {
            for (int i = 1; i <= 6; i++) {
                createBuilding(zone + i + "栋", "大学城校区", zoneRemark(zone));
            }
        }
        for (int i = 1; i <= 4; i++) {
            createBuilding("桂花岗" + i + "栋", "桂花岗校区", "综合宿舍");
        }
        for (int i = 1; i <= 2; i++) {
            createBuilding("黄埔" + i + "栋", "黄埔研究院", "研究生宿舍");
        }
    }

    private String zoneRemark(String zone) {
        return switch (zone) {
            case "梅苑" -> "男生宿舍";
            case "菊苑" -> "研究生宿舍";
            default -> "女生宿舍";
        };
    }

    private void createBuilding(String name, String campus, String remark) {
        Building building = new Building();
        building.setName(name);
        building.setCampus(campus);
        building.setAddress("广州大学" + campus);
        building.setRemark(remark);
        buildingRepository.save(building);
    }

    private void createUserIfMissing(String username, String realName, String role, String studentNo,
                                     String phone, Long buildingId, String roomNo) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(DEFAULT_PASSWORD);
        user.setRealName(realName);
        user.setRole(role);
        user.setStudentNo(studentNo);
        user.setPhone(phone);
        user.setEmail(username + "@gzhu.edu.cn");
        user.setGender(Constants.ROLE_STUDENT.equals(role) ? 2 : 1);
        user.setBuildingId(buildingId);
        user.setRoomNo(roomNo);
        user.setStatus(1);
        userRepository.save(user);
    }

    private Long buildingId(String name) {
        return buildingRepository.findByName(name).map(Building::getId).orElse(null);
    }
}
