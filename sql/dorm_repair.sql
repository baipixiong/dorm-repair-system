-- ============================================================
-- 广州大学 宿舍报修与工单管理系统 数据库脚本
-- 适用: MySQL 8.0+
-- 说明: 直接整段执行即可创建数据库、数据表与演示数据
-- 演示账号密码统一为: 123456
-- ============================================================

DROP DATABASE IF EXISTS dorm_repair;
CREATE DATABASE dorm_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dorm_repair;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 1. 楼栋表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS building;
CREATE TABLE building (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(60)  NOT NULL COMMENT '楼栋名称',
    campus      VARCHAR(60)  NOT NULL DEFAULT '大学城校区' COMMENT '所属校区',
    address     VARCHAR(120) DEFAULT NULL COMMENT '地址',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_building_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '宿舍楼栋';

-- ------------------------------------------------------------
-- 2. 房间表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS room;
CREATE TABLE room (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    building_id BIGINT       NOT NULL COMMENT '所属楼栋',
    room_no     VARCHAR(30)  NOT NULL COMMENT '房间号',
    floor       INT          DEFAULT NULL COMMENT '楼层',
    room_type   VARCHAR(20)  NOT NULL DEFAULT '四人间' COMMENT '房型',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_room (building_id, room_no),
    KEY idx_room_building (building_id),
    CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '宿舍房间';

-- ------------------------------------------------------------
-- 3. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(40)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
    real_name   VARCHAR(40)  NOT NULL COMMENT '真实姓名',
    role        VARCHAR(16)  NOT NULL DEFAULT 'student' COMMENT '角色: student/technician/admin',
    student_no  VARCHAR(30)  DEFAULT NULL COMMENT '学号/工号',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(120) DEFAULT NULL COMMENT '邮箱',
    gender      TINYINT      NOT NULL DEFAULT 0 COMMENT '性别: 0未知 1男 2女',
    building_id BIGINT       DEFAULT NULL COMMENT '所属楼栋(学生)',
    room_id     BIGINT       DEFAULT NULL COMMENT '所属房间(学生)',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role (role),
    KEY idx_user_building (building_id),
    CONSTRAINT fk_user_building FOREIGN KEY (building_id) REFERENCES building (id),
    CONSTRAINT fk_user_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT chk_user_role CHECK (role IN ('student', 'technician', 'admin'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

-- ------------------------------------------------------------
-- 4. 工单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ticket;
CREATE TABLE ticket (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no     VARCHAR(32)  NOT NULL COMMENT '工单编号',
    student_id    BIGINT       NOT NULL COMMENT '报修学生',
    building_id   BIGINT       NOT NULL COMMENT '楼栋(冗余便于统计)',
    room_id       BIGINT       NOT NULL COMMENT '报修房间',
    title         VARCHAR(100) NOT NULL COMMENT '报修标题',
    description   TEXT         NOT NULL COMMENT '故障描述',
    category      VARCHAR(20)  NOT NULL DEFAULT 'other' COMMENT '类别: water/electric/door/furniture/other',
    priority      VARCHAR(10)  NOT NULL DEFAULT 'normal' COMMENT '优先级: low/normal/high/urgent',
    status        VARCHAR(20)  NOT NULL DEFAULT 'pending' COMMENT '状态: pending/assigned/working/review/completed/cancelled',
    assignee_id   BIGINT       DEFAULT NULL COMMENT '维修人员',
    contact_phone VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    scheduled_at  DATETIME     DEFAULT NULL COMMENT '预约上门时间',
    finished_at   DATETIME     DEFAULT NULL COMMENT '维修完成时间',
    completed_at  DATETIME     DEFAULT NULL COMMENT '学生验收完成时间',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_status (status),
    KEY idx_ticket_student (student_id),
    KEY idx_ticket_assignee (assignee_id),
    KEY idx_ticket_building (building_id),
    KEY idx_ticket_created (created_at),
    CONSTRAINT fk_ticket_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_ticket_building FOREIGN KEY (building_id) REFERENCES building (id),
    CONSTRAINT fk_ticket_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES users (id),
    CONSTRAINT chk_ticket_category CHECK (category IN ('water', 'electric', 'door', 'furniture', 'other')),
    CONSTRAINT chk_ticket_status CHECK (status IN ('pending', 'assigned', 'working', 'review', 'completed', 'cancelled')),
    CONSTRAINT chk_ticket_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报修工单';

-- ------------------------------------------------------------
-- 5. 工单流转记录表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ticket_event;
CREATE TABLE ticket_event (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT      NOT NULL COMMENT '工单',
    actor_id    BIGINT      NOT NULL COMMENT '操作人',
    action      VARCHAR(30) NOT NULL COMMENT '动作: create/assign/start/finish/accept/reject/cancel/comment/classify/evaluate',
    from_status VARCHAR(20) NOT NULL DEFAULT '' COMMENT '原状态',
    to_status   VARCHAR(20) NOT NULL DEFAULT '' COMMENT '新状态',
    note        TEXT        DEFAULT NULL COMMENT '备注',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_event_ticket (ticket_id),
    CONSTRAINT fk_event_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_event_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单流转记录';

-- ------------------------------------------------------------
-- 6. 附件表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS attachment;
CREATE TABLE attachment (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT       NOT NULL COMMENT '工单',
    uploader_id BIGINT       NOT NULL COMMENT '上传人',
    file_name   VARCHAR(150) NOT NULL COMMENT '原始文件名',
    file_path   VARCHAR(255) NOT NULL COMMENT '存储路径',
    file_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    mime_type   VARCHAR(80)  DEFAULT NULL COMMENT '文件类型',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_attachment_ticket (ticket_id),
    CONSTRAINT fk_attachment_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_attachment_user FOREIGN KEY (uploader_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单附件(图片)';

-- ------------------------------------------------------------
-- 7. 评价表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS evaluation;
CREATE TABLE evaluation (
    id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id  BIGINT   NOT NULL COMMENT '工单',
    student_id BIGINT   NOT NULL COMMENT '评价学生',
    score      INT      NOT NULL COMMENT '评分 1-5',
    comment    VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_evaluation_ticket (ticket_id),
    CONSTRAINT fk_evaluation_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_evaluation_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT chk_evaluation_score CHECK (score >= 1 AND score <= 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '工单评价';

-- ------------------------------------------------------------
-- 8. 操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS audit_log;
CREATE TABLE audit_log (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    actor_id   BIGINT      DEFAULT NULL COMMENT '操作人',
    actor_name VARCHAR(40) DEFAULT NULL COMMENT '操作人姓名',
    action     VARCHAR(60) NOT NULL COMMENT '操作类型',
    detail     VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_audit_actor (actor_id),
    KEY idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 演示数据
-- ============================================================

-- 楼栋
INSERT INTO building (id, name, campus, address, remark) VALUES
(1, '梅苑1栋', '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(2, '梅苑2栋', '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(3, '兰苑1栋', '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(4, '竹苑1栋', '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(5, '菊苑1栋', '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(6, '桂花岗1栋', '桂花岗校区', '广州大学桂花岗校区', '综合宿舍');

-- 房间
INSERT INTO room (id, building_id, room_no, floor, room_type) VALUES
(1, 1, '101', 1, '四人间'),
(2, 1, '102', 1, '四人间'),
(3, 1, '201', 2, '四人间'),
(4, 1, '202', 2, '四人间'),
(5, 2, '101', 1, '四人间'),
(6, 2, '102', 1, '四人间'),
(7, 2, '301', 3, '四人间'),
(8, 2, '302', 3, '四人间'),
(9, 3, '101', 1, '四人间'),
(10, 3, '102', 1, '四人间'),
(11, 3, '201', 2, '四人间'),
(12, 3, '202', 2, '四人间'),
(13, 4, '101', 1, '四人间'),
(14, 4, '102', 1, '四人间'),
(15, 4, '301', 3, '四人间'),
(16, 4, '302', 3, '四人间'),
(17, 5, '101', 1, '四人间'),
(18, 5, '102', 1, '四人间'),
(19, 5, '201', 2, '四人间'),
(20, 5, '202', 2, '四人间'),
(21, 6, '101', 1, '六人间'),
(22, 6, '102', 1, '六人间'),
(23, 6, '201', 2, '六人间'),
(24, 6, '202', 2, '六人间');

-- 用户 (密码均为明文 123456)
INSERT INTO users (id, username, password, real_name, role, student_no, phone, email, gender, building_id, room_id, status) VALUES
(1, 'admin',   '123456', '系统管理员', 'admin',      'A0001', '020-39366000', 'admin@gzhu.edu.cn',   0, NULL, NULL, 1),
(2, 'student', '123456', '陈嘉怡',     'student',    '2022100101', '13800000001', 'student@gzhu.edu.cn', 2, 1, 1, 1),
(3, 'repair',  '123456', '李伟明',     'technician', 'W0001', '13900000001', 'repair@gzhu.edu.cn',  1, NULL, NULL, 1),
(4, 'student2','123456', '王志强',     'student',    '2022100102', '13800000002', 'student2@gzhu.edu.cn',1, 2, 5, 1),
(5, 'repair2', '123456', '黄师傅',     'technician', 'W0002', '13900000002', 'repair2@gzhu.edu.cn', 1, NULL, NULL, 1);

-- 工单
INSERT INTO ticket (id, ticket_no, student_id, building_id, room_id, title, description, category, priority, status, assignee_id, contact_phone, scheduled_at, finished_at, completed_at, version, created_at) VALUES
(1, 'BX202611180001', 2, 1, 1, '洗手台水龙头漏水', '洗手台水龙头关不紧，一直滴水，晚上声音比较大，麻烦尽快处理。', 'water', 'normal', 'pending', NULL, '13800000001', NULL, NULL, NULL, 0, '2026-11-18 09:20:00'),
(2, 'BX202611190001', 4, 2, 5, '宿舍插座没电', '靠窗的两个插座都没有电，手机无法充电，已排除跳闸。', 'electric', 'high', 'assigned', 3, '13800000002', '2026-11-20 10:00:00', NULL, NULL, 1, '2026-11-19 14:05:00'),
(3, 'BX202611190002', 2, 1, 1, '衣柜门合页损坏', '衣柜左侧门合页松动，门板下垂关不上，需要更换合页。', 'furniture', 'normal', 'working', 3, '13800000001', '2026-11-20 15:00:00', NULL, NULL, 2, '2026-11-19 16:40:00'),
(4, 'BX202611170001', 4, 2, 5, '阳台推拉门卡住', '阳台推拉门轨道变形，推拉很费力，需要维修。', 'door', 'normal', 'completed', 5, '13800000002', '2026-11-18 09:00:00', '2026-11-18 10:30:00', '2026-11-18 12:00:00', 3, '2026-11-17 08:30:00');

-- 工单流转记录
INSERT INTO ticket_event (ticket_id, actor_id, action, from_status, to_status, note, created_at) VALUES
(1, 2, 'create', '', 'pending', '学生提交报修', '2026-11-18 09:20:00'),
(2, 4, 'create', '', 'pending', '学生提交报修', '2026-11-19 14:05:00'),
(2, 1, 'assign', 'pending', 'assigned', '已安排李伟明师傅上门，请保持电话畅通', '2026-11-19 15:00:00'),
(3, 2, 'create', '', 'pending', '学生提交报修', '2026-11-19 16:40:00'),
(3, 1, 'assign', 'pending', 'assigned', '安排李伟明师傅处理', '2026-11-19 17:10:00'),
(3, 3, 'start', 'assigned', 'working', '已联系学生，开始上门维修', '2026-11-20 15:10:00'),
(4, 4, 'create', '', 'pending', '学生提交报修', '2026-11-17 08:30:00'),
(4, 1, 'assign', 'pending', 'assigned', '安排黄师傅处理', '2026-11-17 10:00:00'),
(4, 5, 'start', 'assigned', 'working', '已到现场检查', '2026-11-18 09:05:00'),
(4, 5, 'finish', 'working', 'review', '已更换推拉门滑轮并校正轨道，请验收', '2026-11-18 10:30:00'),
(4, 4, 'accept', 'review', 'completed', '验收通过，维修及时', '2026-11-18 12:00:00');

-- 评价
INSERT INTO evaluation (ticket_id, student_id, score, comment, created_at) VALUES
(4, 4, 5, '师傅很专业，维修速度快，态度也很好。', '2026-11-18 12:05:00');

-- 操作日志
INSERT INTO audit_log (actor_id, actor_name, action, detail, created_at) VALUES
(1, '系统管理员', '初始化', '导入演示数据', '2026-11-20 08:00:00');
