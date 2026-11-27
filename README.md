# 雪野营地四季度假综合运营与预订管理系统

Snowfield Four-Season Resort Management System 是中山大学数据库系统课程设计项目。系统面向全年运营的山地度假营地，统一管理住宿/营位预订、季节价格、模拟支付、滑雪票与课程、缆车时段容量、装备租赁和运营统计。

当前状态：**公共契约仍为 v1.0-rc 候选版，尚未通过双人 Review。** 后端已落地 V1~V5、住宿创建/支付/取消/过期 application 生命周期、Spring Security/JWT 边界与模拟支付 HTTP 入口；真实 PostgreSQL 全量回归 146 项通过（TE-20261128-01）。这些实现仍在 PR #3 等待 `Janovo2117` 审查，尚未集成到 `develop`；前端尚未创建。

## 冻结技术栈

- 前端：React、TypeScript、Vite、Ant Design、ECharts、React Router、Axios。
- 后端：Java 21、Spring Boot、Spring Security、JWT、MyBatis、Bean Validation、Springdoc OpenAPI。
- 数据库：PostgreSQL，所有结构变更由 Flyway 管理。
- 测试：JUnit、Postman/Swagger UI、JMeter。
- 环境：Docker Compose、Git、GitHub。

## 第一阶段成果

- [项目范围](docs/PROJECT_SCOPE.md)
- [需求说明](docs/REQUIREMENTS.md)
- [系统架构](docs/ARCHITECTURE.md)
- [数据库契约](docs/DATABASE_CONTRACT.md)
- [API 契约](docs/API_CONTRACT.md)
- [枚举契约](docs/ENUMS.md)
- [ER 图与3NF分析](docs/design/DATABASE_DESIGN.md)
- [业务流程](docs/design/BUSINESS_FLOWS.md)
- [参考项目比较](docs/references/REFERENCE_COMPARISON.md)
- [团队协作](docs/TEAM_WORKFLOW.md)
- [开发和测试计划](docs/DEVELOPMENT.md)
- [第一阶段评审包与待确认项](docs/PHASE1_REVIEW.md)
- [第2阶段18项决策确认清单](docs/PHASE2_DECISIONS.md)
- [两人团队任务板](docs/TEAM_TASK_BOARD.md)
- [公共契约候选冻结记录](docs/CONTRACT_FREEZE.md)
- [第2阶段追加确认记录](docs/PHASE2_CONFIRMATIONS.md)
- [审计与证据索引](docs/audit/AUDIT_LOG.md)

## 工程结构

```text
backend/                         Spring Boot + Security/JWT + MyBatis + Flyway
  src/main/resources/db/migration/  V1~V5 Flyway SQL
frontend/                        React + TypeScript（尚未建立）
docs/
  design/                       ER、流程和模块设计
  references/                   开源项目研究
  testing/                      测试计划与真实结果
  report/                       课程报告材料
```

## 协作原则

1. 从 `develop` 创建 `feature/*` 分支，不在 `main` 直接开发。
2. 公共契约变更必须经另一名成员审查。
3. 已合并的 Flyway 迁移禁止修改；新变化追加新版本。
4. 前端不得直连数据库，金额、价格、容量和库存由后端/数据库权威计算。
5. 禁止复制完整开源项目；引用必须记录来源和许可证。
6. 没有真实测试证据的功能不能标记为完成。

## 本地前置环境

已在启动机器上确认 Git 2.48、Java 21、Node.js 24、npm 11 和 Docker 29 可用。具体版本基线和启动步骤将在第二阶段工程初始化后固定到 [开发文档](docs/DEVELOPMENT.md)。
