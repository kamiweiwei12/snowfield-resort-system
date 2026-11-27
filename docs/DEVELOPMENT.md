# 开发计划、环境与风险

## 当前仓库状态（2026-11-28 更新）

- 公共契约仍是 `v1.0-rc`：PR #2 可合并但为 `REVIEW_REQUIRED`，0 review，等待 `Janovo2117`；尚不能称为正式冻结。
- 后端 S1/S2 已完成；PR #3 在 `feature/database-core` 上继续累积不依赖 Review 的实现，同样等待 Review，尚未集成到 `develop`。
- 数据库已实现 V1~V5；Spring Security/JWT 边界和模拟支付 HTTP 入口已接入；真实 PostgreSQL `-Pdb` 回归 146 个测试通过（TE-20261128-01）。
- BK-02 application 生命周期已完成，支付 HTTP 已验证从 JWT `sub` 获取用户并执行对象所有权/幂等检查；注册/登录、当前用户、创建/查询/取消订单 HTTP 因 DTO 契约不完整暂缓（R6-11）。BK-01 已接入维修阻断；独立可用性查询和开放规则语义仍有缺口。
- 前端工程尚未创建。
- 默认测试已恢复为无数据库构建：13 个单元/架构测试通过；数据库上下文门禁改为 `SnowfieldApplicationIT`，仅在 `-Pdb` 运行（TE-20261127-14）。

## S2 数据库环境（已验证）

本机开发环境事实：本机已运行原生 `postgresql-x64-18` 服务占用 `0.0.0.0:5432`，因此项目容器的默认端口是 **5433**。

```powershell
# 1. 准备环境变量（首次）
Copy-Item .env.example .env

# 启动后端前必须在当前终端/IDE注入真实值；不要把真实密钥提交到 .env.example。
$env:SNOWFIELD_DB_PASSWORD = "<local-db-password>"
$env:SNOWFIELD_JWT_SECRET = "<at-least-32-random-bytes>"

# 2. 启动数据库（映射 5433 -> 容器 5432）
docker compose up -d db

# 3. 创建隔离测试库 snowfield_test（幂等）
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-db-init.ps1

# 4. 默认构建（不连数据库）
Push-Location backend
.\mvnw.cmd -B test

# 5. 数据库集成测试（必须显式 -Pdb）
.\mvnw.cmd -B -Pdb test
Pop-Location
```

- 开发库 `snowfield`、测试库 `snowfield_test` 分离，测试不得污染开发库。
- 密码只通过环境变量 `SNOWFIELD_DB_PASSWORD` 注入，不写入仓库。
- 测试库证据见 `docs/audit/TEST_EVIDENCE.md` TE-20261126-16。

## 建议工程初始化（第二阶段）

1. 从已存在的 `develop` 派生 `feature/database-core` 与 `feature/frontend`。
2. 生成 Spring Boot Maven 工程，仅加入已冻结依赖；禁用任何自动 DDL。
3. 建立 Docker Compose PostgreSQL 和独立测试数据库。
4. 以已确认契约创建 V1/V2 Flyway 基线与种子数据。
5. 建立 React/Vite/TypeScript 工程和契约类型目录。
6. 先完成健康检查、迁移测试和统一响应，再做业务。

## 里程碑

| 日期 | 目标 | 验收证据 |
|---|---|---|
| 11/25–11/30 | 第一阶段与契约冻结 | 文档审查、ER/流程确认 |
| 12/01–12/08 | P0-A 数据库与后端骨架 | 空库迁移、约束/事务测试 |
| 12/09–12/16 | 住宿预订闭环 | API集成、并发抢订 |
| 12/17–12/23 | P0-B 票务/缆车/装备 | 容量/库存并发测试 |
| 12/24–12/28 | 前端联调和管理端 | 真实API演示、响应式界面 |
| 12/29–12/31 | 性能与系统测试 | JMeter、EXPLAIN证据 |
| 01/01–01/02 | 报告、视频、重装验证 | 3000字报告、干净环境复现 |

## 测试命令（S2 起）

```powershell
Push-Location backend
.\mvnw.cmd -B test              # 单元 + 架构测试，不连数据库
.\mvnw.cmd -B -Pdb test         # 追加真实 PostgreSQL 集成测试（*IT）
Pop-Location
npm --prefix frontend run lint  # 前端工程建立后
docker compose up -d db
```

不得提前把尚未执行的命令写成已通过。

## 主要风险

1. **范围过大**：四季住宿+票务+缆车+装备可能导致每个模块只剩静态页面。用 P0-A 门禁保护评分主线。
2. **统一订单过度抽象**：万能明细表会弱化外键。采用订单主表 + 类型明确的业务占用表。
3. **并发超卖**：先查后写存在竞态。住宿用排斥约束，容量/批量库存用事务行锁，资产用排斥约束。
4. **价格不可追溯**：规则修改影响旧单。订单明细和价格分解保存快照。
5. **Flyway 冲突**：两人同时编号。实施版本占用与PR检查。
6. **时间语义**：入住/退房、日营和时区混乱。契约统一半开区间 `[start,end)` 和 `Asia/Shanghai` 业务时区。
7. **参考代码许可证**：Louez 为 AGPL-3.0 且部分图标另有许可；仅研究思想，不复制代码/资产。
8. **课程证据不足**：所有性能数值和并发结果必须来自本项目真实运行。

## 第二阶段首批具体任务

- A：建立 Spring Boot、Flyway/PostgreSQL、V1基础字典与用户权限、迁移测试。
- B：建立 React/Vite/Ant Design、路由和 API client，依据契约实现登录/错误状态骨架。
- 共同：由 `Janovo2117` 审查已同步D-001/D-002的 `CONTRACT_FREEZE v1.0-rc` 并留下真实PR Review；等待期间只继续不改变候选契约语义的实现，正式集成仍需 Review。
