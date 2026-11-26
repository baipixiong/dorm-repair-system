-- ============================================================
-- 广州大学 宿舍报修与工单管理系统 数据库脚本
-- 适用: MySQL 8.0+
-- 说明: 直接整段执行即可创建数据库、数据表与演示数据
-- 演示账号密码统一为: 123456
-- ============================================================

-- 说明: 不删除数据库，只创建（若不存在）并选中，随后逐表 DROP 重建，可重复执行
CREATE DATABASE IF NOT EXISTS dorm_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dorm_repair;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清理旧版本遗留的房间表（新版本已不再使用房间表）
DROP TABLE IF EXISTS room;

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
-- 2. 用户表
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
    room_no     VARCHAR(30)  DEFAULT NULL COMMENT '房间号(学生自定义填写)',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role (role),
    KEY idx_user_building (building_id),
    CONSTRAINT fk_user_building FOREIGN KEY (building_id) REFERENCES building (id),
    CONSTRAINT chk_user_role CHECK (role IN ('student', 'technician', 'admin'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户';

-- ------------------------------------------------------------
-- 3. 工单表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ticket;
CREATE TABLE ticket (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no     VARCHAR(32)  NOT NULL COMMENT '工单编号',
    student_id    BIGINT       NOT NULL COMMENT '报修学生',
    building_id   BIGINT       NOT NULL COMMENT '楼栋(冗余便于统计)',
    room_no       VARCHAR(30)  NOT NULL COMMENT '报修房间号',
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
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES users (id),
    CONSTRAINT chk_ticket_category CHECK (category IN ('water', 'electric', 'door', 'furniture', 'other')),
    CONSTRAINT chk_ticket_status CHECK (status IN ('pending', 'assigned', 'working', 'review', 'completed', 'cancelled')),
    CONSTRAINT chk_ticket_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '报修工单';

-- ------------------------------------------------------------
-- 4. 工单流转记录表
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
-- 5. 附件表
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
-- 6. 评价表
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
-- 7. 操作日志表
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

-- 楼栋（房间号由学生报修时自行填写，无需维护房间表）
INSERT INTO building (id, name, campus, address, remark) VALUES
(1,  '梅苑1栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(2,  '梅苑2栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(3,  '梅苑3栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(4,  '梅苑4栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(5,  '梅苑5栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(6,  '梅苑6栋',  '大学城校区', '广州大学大学城校区生活区', '男生宿舍'),
(7,  '兰苑1栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(8,  '兰苑2栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(9,  '兰苑3栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(10, '兰苑4栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(11, '兰苑5栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(12, '兰苑6栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(13, '竹苑1栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(14, '竹苑2栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(15, '竹苑3栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(16, '竹苑4栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(17, '竹苑5栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(18, '竹苑6栋',  '大学城校区', '广州大学大学城校区生活区', '女生宿舍'),
(19, '菊苑1栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(20, '菊苑2栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(21, '菊苑3栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(22, '菊苑4栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(23, '菊苑5栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(24, '菊苑6栋',  '大学城校区', '广州大学大学城校区生活区', '研究生宿舍'),
(25, '桂花岗1栋', '桂花岗校区', '广州大学桂花岗校区', '综合宿舍'),
(26, '桂花岗2栋', '桂花岗校区', '广州大学桂花岗校区', '综合宿舍'),
(27, '桂花岗3栋', '桂花岗校区', '广州大学桂花岗校区', '综合宿舍'),
(28, '桂花岗4栋', '桂花岗校区', '广州大学桂花岗校区', '综合宿舍'),
(29, '黄埔1栋',  '黄埔研究院', '广州大学黄埔研究院', '研究生宿舍'),
(30, '黄埔2栋',  '黄埔研究院', '广州大学黄埔研究院', '研究生宿舍');

-- 用户 (密码均为明文 123456)
INSERT INTO users (id, username, password, real_name, role, student_no, phone, email, gender, building_id, room_no, status) VALUES
(1, 'admin',   '123456', '系统管理员', 'admin',      'A0001', '020-39366000', 'admin@gzhu.edu.cn',   0, NULL, NULL, 1),
(2, 'student', '123456', '陈嘉怡',     'student',    '2022100101', '13800000001', 'student@gzhu.edu.cn', 2, 1, '101', 1),
(3, 'repair',  '123456', '李伟明',     'technician', 'W0001', '13900000001', 'repair@gzhu.edu.cn',  1, NULL, NULL, 1),
(4, 'student2','123456', '王志强',     'student',    '2022100102', '13800000002', 'student2@gzhu.edu.cn',1, 2, '101', 1),
(5, 'repair2', '123456', '黄师傅',     'technician', 'W0002', '13900000002', 'repair2@gzhu.edu.cn', 1, NULL, NULL, 1);

-- 工单
INSERT INTO ticket (id, ticket_no, student_id, building_id, room_no, title, description, category, priority, status, assignee_id, contact_phone, scheduled_at, finished_at, completed_at, version, created_at) VALUES
(1, 'BX202611180001', 2, 1, '101', '洗手台水龙头漏水', '洗手台水龙头关不紧，一直滴水，晚上声音比较大，麻烦尽快处理。', 'water', 'normal', 'pending', NULL, '13800000001', NULL, NULL, NULL, 0, '2026-11-18 09:20:00'),
(2, 'BX202611190001', 4, 2, '101', '宿舍插座没电', '靠窗的两个插座都没有电，手机无法充电，已排除跳闸。', 'electric', 'high', 'assigned', 3, '13800000002', '2026-11-20 10:00:00', NULL, NULL, 1, '2026-11-19 14:05:00'),
(3, 'BX202611190002', 2, 1, '101', '衣柜门合页损坏', '衣柜左侧门合页松动，门板下垂关不上，需要更换合页。', 'furniture', 'normal', 'working', 3, '13800000001', '2026-11-20 15:00:00', NULL, NULL, 2, '2026-11-19 16:40:00'),
(4, 'BX202611170001', 4, 2, '101', '阳台推拉门卡住', '阳台推拉门轨道变形，推拉很费力，需要维修。', 'door', 'normal', 'completed', 5, '13800000002', '2026-11-18 09:00:00', '2026-11-18 10:30:00', '2026-11-18 12:00:00', 3, '2026-11-17 08:30:00');

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
