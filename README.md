# 广州大学宿舍报修与工单管理系统

一个基于 **Spring Boot 3 + Vue 3 + MySQL 8** 的宿舍报修与工单管理系统，参考了 `wwhhorizon/dorm-repair-system` 的业务流程，并针对广州大学宿舍场景进行了重新设计。

## 一、功能概览

系统包含三类角色：

- **学生**：注册登录、提交报修、上传现场图片、查看工单进度、取消待派单工单、申请返修、验收、评价。
- **维修人员**：查看分配给自己的工单、开始维修、提交完工说明。
- **管理员**：查看全校工单、确认分类、派单/改派、查看统计、导出 CSV、管理用户、管理楼栋房间、查看操作日志。

核心状态流转：

```text
待派单 → 已派单 → 维修中 → 待验收 → 已完成
   └→ 已取消       待验收 → 维修中（申请返修）
```

## 二、项目结构

```text
dorm-repair-system/
├─ sql/
│  └─ dorm_repair.sql                  # MySQL 建库、建表、演示数据
├─ backend/                             # Spring Boot 后端
│  ├─ pom.xml
│  └─ src/main/java/com/gzhu/repair/
│     ├─ common/                        # 统一返回、异常与常量
│     ├─ config/                        # Web、密码加密配置
│     ├─ controller/                    # REST 接口
│     ├─ dto/                           # 请求对象
│     ├─ entity/                        # JPA 实体
│     ├─ init/                          # 开发环境演示数据初始化
│     ├─ repository/                    # 数据访问层
│     ├─ security/                      # JWT 与登录拦截
│     └─ service/                       # 业务服务层
└─ frontend/                            # Vue3 前端
   ├─ package.json
   ├─ vite.config.js
   └─ src/
      ├─ api/                           # Axios 接口封装
      ├─ components/                    # 通用组件
      ├─ layout/                        # 系统布局与菜单
      ├─ router/                        # 路由与权限守卫
      ├─ store/                         # 用户登录状态
      ├─ styles/                        # 全局样式
      ├─ utils/                         # 状态、类别等字典
      └─ views/                         # 页面
```

## 三、数据库初始化

确认本机 MySQL 8 已启动，然后使用 MySQL 客户端执行：

```bash
mysql -uroot -p < sql/dorm_repair.sql
```

也可以用 Navicat、MySQL Workbench 或 IntelliJ IDEA Database 工具打开并执行 `sql/dorm_repair.sql`。

脚本会：

1. 删除并重新创建 `dorm_repair` 数据库；
2. 创建楼栋、房间、用户、工单、流转记录、附件、评价、操作日志等表；
3. 插入广州大学大学城校区、桂花岗校区的部分演示楼栋和房间；
4. 插入演示账号与工单数据。

> 注意：脚本开头包含 `DROP DATABASE IF EXISTS dorm_repair`，如果数据库中已有重要数据，请先备份或删除该行。

## 四、演示账号

| 账号 | 角色 | 密码 |
|---|---|---|
| `student` | 学生 | `123456` |
| `student2` | 学生 | `123456` |
| `repair` | 维修人员 | `123456` |
| `repair2` | 维修人员 | `123456` |
| `admin` | 管理员 | `123456` |

后端第一次启动时也会自动检查基础演示账号和楼栋数据；已有账号不会被覆盖。

## 五、启动后端

环境要求：

- JDK 17+
- Maven 3.8+
- MySQL 8+

修改 `backend/src/main/resources/application.yml` 中的数据库连接配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dorm_repair?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

进入后端目录启动：

```bash
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080`

健康检查：`http://localhost:8080/health`，返回应用和数据库连通状态。

也可以先打包：

```bash
mvn clean package -DskipTests
java -jar target/dorm-repair-backend-1.0.0.jar
```

上传图片默认保存到后端工作目录下的 `uploads/` 文件夹。

## 六、启动前端

环境要求：

- Node.js 18+
- npm 9+

```bash
cd frontend
npm install
npm run dev
```

打开：`http://localhost:5173`

Vite 已配置 `/api` 代理到 `http://localhost:8080`，因此开发环境下前端可以直接调用后端接口。

生产构建：

```bash
npm run build
```

## 七、主要接口

认证：

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/auth/me`
- `PUT /api/auth/profile`
- `PUT /api/auth/password`

工单：

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/actions`
- `POST /api/tickets/{id}/evaluation`
- `POST /api/tickets/{id}/attachments`
- `GET /api/attachments/{id}`

管理：

- `GET /api/statistics`
- `GET /api/admin/export`
- `GET /api/users`
- `POST /api/admin/users`
- `PUT /api/admin/users/{id}/status`
- `GET /api/technicians`
- `GET /api/locations`
- `POST /api/admin/locations/buildings`
- `POST /api/admin/locations/rooms`
- `GET /api/admin/audit`

工单动作请求示例：

```json
{
  "action": "assign",
  "version": 0,
  "assigneeId": 3,
  "scheduledAt": "2026-11-21 10:00:00",
  "note": "请携带相关配件上门处理"
}
```

支持的动作：`cancel`、`assign`、`start`、`finish`、`accept`、`reject`、`comment`、`classify`。

## 八、已完成验证

已在当前环境验证：

- Spring Boot 后端 `mvn -DskipTests compile` 通过；
- BCrypt 演示密码 `123456` 校验通过；
- Vue 前端 `npm run build` 通过；
- Vue 路由、请求封装、角色菜单与接口权限已配置；
- 当前 IDE 代码诊断未发现错误。

## 九、开发说明

当前版本重点完成了完整业务骨架和可运行的前后端页面，适合继续进行：

- 更换广州大学真实楼栋、房间、用户数据；
- 对接学校统一身份认证；
- 增加 WebSocket 实时通知；
- 增加 AI 故障分类服务；
- 增加图片压缩、对象存储与访问权限；
- 增加分页统计 SQL 优化和更细粒度的管理员权限。
