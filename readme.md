# 课程报名管理系统

## 项目简介

当前系统课程报名管理系统，用于培训课程的发布、报名、审核和名额控制。系统面向管理员和员工两类用户，管理员负责创建和维护课程、发布课程、查看报名名单、审核报名结果以及处理取消和驳回后的状态联动，员工负责查询课程列表、查看课程详情、在满足条件时报名、在允许范围内取消报名并查看自己的报名和审核状态。

## 技术栈

- **后端**: SpringBoot 2.7.18 + JPA + H2 Database
- **前端**: Vue 3 + Vue Router + Vuex + Element Plus + Axios
- **数据库**: H2 (嵌入式，支持数据持久化)

## 核心流程

### 课程管理流程
1. 管理员创建课程（草稿状态）
2. 管理员发布课程（发布后员工可见可报名）
3. 员工浏览课程列表，查看课程详情
4. 员工报名课程（校验：已发布、未结束、未满员、未重复报名）
5. 管理员审核报名（通过/驳回）
6. 员工可在允许时间内取消报名
7. 管理员可关闭课程或取消员工报名

### 状态流转

#### 课程状态机
```
草稿(DRAFT) --[发布]--> 已发布(PUBLISHED) --[关闭]--> 已关闭(CLOSED)
```

#### 报名状态机
```
                        ┌──[审核通过]──> 已通过(APPROVED) ──┐
                        │                                      ├──[用户/管理员取消]──> 已取消(CANCELLED)
未报名 ──[提交报名]──> 待审核(PENDING) ──────────────────────┘
                        │
                        └──[审核驳回]──> 已驳回(REJECTED) ───┐
                                                             ├──[重新报名]──> 待审核(PENDING)
已取消(CANCELLED) ───────────────────────────────────────────┘
```

| 当前状态 | 允许动作 | 目标状态 | 说明 |
|---------|---------|---------|------|
| 未报名 | SUBMIT(提交报名) | PENDING | 用户首次报名 |
| PENDING | APPROVE(审核通过) | APPROVED | 管理员审核通过 |
| PENDING | REJECT(审核驳回) | REJECTED | 管理员审核驳回 |
| PENDING | CANCEL_BY_USER(用户取消) | CANCELLED | 用户在截止时间前取消 |
| PENDING | CANCEL_BY_ADMIN(管理员取消) | CANCELLED | 管理员强制取消 |
| APPROVED | CANCEL_BY_USER(用户取消) | CANCELLED | 用户在截止时间前取消 |
| APPROVED | CANCEL_BY_ADMIN(管理员取消) | CANCELLED | 管理员强制取消 |
| REJECTED | RE_SUBMIT(重新报名) | PENDING | 用户重新报名 |
| CANCELLED | RE_SUBMIT(重新报名) | PENDING | 用户重新报名 |

**重新报名规则**: 已驳回(REJECTED)或已取消(CANCELLED)的报名记录不影响重新报名，员工可以对同一课程再次发起报名

### 核心校验规则
| 场景 | 校验规则 |
|------|----------|
| 报名课程 | 必须是已发布、未开始、未满员、当前无待审核或已通过的报名记录 |
| 取消报名 | 必须在取消截止时间前，且报名状态为待审核或已通过 |
| 审核通过 | 报名状态必须为待审核，且课程未满员 |
| 审核驳回 | 报名状态必须为待审核，驳回后自动回退课程名额 |
| 重新报名 | 已驳回或已取消的报名不影响，允许再次报名 |
| 课程容量 | 必须大于0，且不能小于当前已报名人数 |
| 时间规则 | 结束时间必须晚于开始时间，取消截止时间必须早于开始时间 |

## 目录结构

```
course-registration/
├── pom.xml                          # 后端 Maven 配置
├── readme.md                        # 项目说明文档
├── src/
│   └── main/
│       ├── java/com/example/courseregistration/
│       │   ├── CourseRegistrationApplication.java   # 启动类
│       │   ├── common/
│       │   │   └── Result.java                      # 统一响应结果
│       │   ├── config/
│       │   │   └── DataInitializer.java             # 数据初始化
│       │   ├── controller/
│       │   │   ├── AuthController.java              # 认证接口
│       │   │   ├── CourseController.java            # 课程接口
│       │   │   └── RegistrationController.java      # 报名接口
│       │   ├── dto/
│       │   │   ├── CourseQuery.java                 # 课程查询参数
│       │   │   ├── LoginRequest.java                # 登录请求
│       │   │   └── ReviewRequest.java               # 审核请求
│       │   ├── entity/
│       │   │   ├── Course.java                      # 课程实体
│       │   │   ├── Registration.java                # 报名实体
│       │   │   ├── RegistrationStatusLog.java       # 报名状态变更日志
│       │   │   └── User.java                        # 用户实体
│       │   ├── enums/
│       │   │   ├── CourseStatus.java                # 课程状态枚举
│       │   │   ├── RegistrationAction.java          # 报名动作枚举
│       │   │   ├── RegistrationStatus.java          # 报名状态枚举
│       │   │   └── UserRole.java                    # 用户角色枚举
│       │   ├── exception/
│       │   │   └── GlobalExceptionHandler.java      # 全局异常处理
│       │   ├── repository/
│       │   │   ├── CourseRepository.java            # 课程数据访问
│       │   │   ├── RegistrationRepository.java      # 报名数据访问
│       │   │   ├── RegistrationStatusLogRepository.java # 状态日志数据访问
│       │   │   └── UserRepository.java              # 用户数据访问
│       │   ├── service/
│       │   │   ├── CourseService.java               # 课程业务逻辑
│       │   │   ├── RegistrationService.java         # 报名业务逻辑
│       │   │   ├── RegistrationStateService.java    # 状态机服务
│       │   │   └── UserService.java                 # 用户业务逻辑
│       │   └── statemachine/
│       │       ├── RegistrationStateMachine.java    # 状态机接口
│       │       └── RegistrationStateMachineImpl.java # 状态机实现
│       └── resources/
│           ├── application.yml                      # 应用配置
│           └── data/                                # H2 数据库文件目录
│               └── .gitkeep
└── frontend/                                        # 前端项目
    ├── package.json                                 # 前端依赖配置
    ├── vue.config.js                                # Vue 配置
    ├── public/
    │   └── index.html
    └── src/
        ├── main.js                                  # 入口文件
        ├── App.vue                                  # 根组件
        ├── router/
        │   └── index.js                             # 路由配置
        ├── store/
        │   └── index.js                             # 状态管理
        ├── utils/
        │   └── api.js                               # API 封装
        └── views/
            ├── Login.vue                            # 登录页
            ├── CourseList.vue                       # 课程列表页
            ├── CourseDetail.vue                     # 课程详情页
            ├── MyRegistrations.vue                  # 我的报名页
            └── AdminReview.vue                      # 管理员审核页
```

## 关键代码模块

### 后端核心模块
1. **CourseService**: 课程管理核心服务，包含课程的增删改查、发布、关闭、名额增减等操作，使用悲观锁保证并发安全。
2. **RegistrationService**: 报名管理核心服务，包含报名、取消报名、审核通过/驳回等操作，所有操作都保证课程名额与报名状态的一致性。
3. **DataInitializer**: 系统启动时自动初始化测试用户和示例课程数据。

### 前端核心模块
1. **CourseList.vue**: 课程列表页，支持多条件查询，动态显示报名/取消按钮，展示当前可报名名额。
2. **CourseDetail.vue**: 课程详情页，展示完整课程信息和用户报名状态。
3. **MyRegistrations.vue**: 我的报名页，展示用户所有报名记录和操作入口。
4. **AdminReview.vue**: 管理员审核工作台，升级功能包括：
   - **数据统计看板**: 实时展示待审核、已通过、已驳回、已取消和总计数量
   - **多维度筛选**: 支持按课程、报名人、审核状态、时间范围筛选
   - **批量审核**: 支持批量通过和批量驳回操作
   - **审核详情弹窗**: 展示完整报名信息和状态变更历史时间线
   - **常用审核意见模板**: 提供5种常用审核意见快速选择
   - **名额进度条**: 直观展示课程名额使用情况

## 后端启动命令

```bash
# 进入项目根目录
cd course-registration

# 方式一：使用 Maven 启动
mvn spring-boot:run

# 方式二：先打包再运行
mvn clean package
java -jar target/course-registration-0.0.1-SNAPSHOT.jar
```

后端启动后默认端口: **8080**

## 前端启动命令

```bash
# 进入前端目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run serve

# 构建生产版本
npm run build
```

前端启动后默认端口: **3000**

## 访问地址

| 名称 | 地址 | 说明 |
|------|------|------|
| 前端页面 | http://localhost:3000 | 课程报名管理系统前端 |
| 后端接口 | http://localhost:8080/api | 后端 API 接口 |
| H2 控制台 | http://localhost:8080/h2-console | H2 数据库管理控制台 |

## H2 控制台配置

登录 H2 控制台时使用以下配置：
- **JDBC URL**: `jdbc:h2:file:./src/main/resources/data/course-registration`
- **用户名**: `sa`
- **密码**: (空)

数据库文件存储在 `src/main/resources/data/` 目录下，支持追加数据存储，重启服务数据不会丢失。

## 默认账号

系统启动时会自动创建以下测试账号：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | 管理员 | 系统管理员，可管理课程和审核报名 |
| employee1 | 123456 | 员工 | 普通员工，可报名课程 |
| employee2 | 123456 | 员工 | 普通员工，可报名课程 |
| employee3 | 123456 | 员工 | 普通员工，可报名课程 |

## 角色切换说明

### 方式一：使用不同账号登录
系统通过登录账号自动识别角色：
- 使用 `admin` 账号登录 → 管理员角色
- 使用 `employee1/employee2/employee3` 账号登录 → 员工角色

### 方式二：前端切换（开发测试用）
在浏览器开发者工具 Application → Local Storage 中修改：
- 修改 `currentUser` 的 `role` 字段为 `ADMIN` 或 `EMPLOYEE`
- 刷新页面后生效

## 主要接口列表

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出
- `GET /api/auth/current` - 获取当前用户信息

### 课程接口
- `GET /api/courses` - 分页查询课程列表
- `GET /api/courses/{id}` - 获取课程详情
- `POST /api/courses` - 创建课程
- `PUT /api/courses/{id}` - 更新课程
- `DELETE /api/courses/{id}` - 删除课程
- `POST /api/courses/{id}/publish` - 发布课程
- `POST /api/courses/{id}/close` - 关闭课程
- `GET /api/courses/{id}/registrations` - 获取课程报名名单
- `GET /api/courses/published` - 获取已发布课程
- `GET /api/courses/available` - 获取可报名课程

### 报名接口
- `GET /api/registrations` - 分页查询报名列表
- `GET /api/registrations/{id}` - 获取报名详情
- `GET /api/registrations/my` - 获取我的报名列表
- `GET /api/registrations/check` - 检查是否已报名
- `GET /api/registrations/statistics` - 获取报名统计数据
- `GET /api/registrations/{id}/history` - 获取报名状态变更历史
- `GET /api/registrations/{id}/chain` - 获取同一课程完整报名历史链
- `GET /api/registrations/course/{courseId}/user/{userId}/history` - 获取指定用户指定课程的报名历史
- `GET /api/registrations/all` - 获取所有报名记录
- `POST /api/registrations/register` - 报名课程
- `POST /api/registrations/{id}/cancel` - 取消报名（员工）
- `POST /api/registrations/{id}/cancel-by-admin` - 取消报名（管理员）
- `POST /api/registrations/{id}/approve` - 审核通过
- `POST /api/registrations/{id}/reject` - 审核驳回
- `POST /api/registrations/batch/approve` - 批量审核通过
- `POST /api/registrations/batch/reject` - 批量审核驳回

## 核心特性

1. **并发安全**: 使用悲观锁（PESSIMISTIC_WRITE）保证名额操作的原子性，防止超报。
2. **重复报名校验**: 数据库唯一约束 + 业务层双重校验，确保一人一课程只能报名一次。
3. **状态联动**: 报名、取消、审核操作自动同步更新课程名额和报名状态，保证数据一致性。
4. **动态按钮**: 前端根据课程状态、报名状态、用户角色动态显示或隐藏操作按钮。
5. **多条件查询**: 课程列表支持按名称、类型、讲师、时间范围、状态查询。
6. **数据持久化**: H2 数据库文件存储，支持重启后数据保留。
7. **重新报名历史追踪**: 完整追踪员工对同一课程的所有报名历史，包括：
   - 报名实体添加 `originalRegistrationId`（原始报名ID）和 `registrationCount`（报名次数）字段
   - 后端自动关联同课程同用户的历史报名记录，形成报名链
   - 课程列表页显示报名次数标签（如"第2次报名"）
   - 课程详情页展示完整报名历史时间线
   - 我的报名页可查看每门课程的报名历史
   - 管理员审核页详情弹窗展示完整报名历史链和状态变更历史
