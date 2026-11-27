# 文档同步矩阵

每轮实质性任务结束时，逐项判断是否受影响；只有实际受影响的才更新，未更新的写明原因。至少检查并更新开发日志、任务板、CHANGELOG、TEST_EVIDENCE、OPERATION_LOG。

| 文档 | 触发条件 | 更新要求 |
|---|---|---|
| `docs/development-log/<月>.md` | 每轮实质性任务 | 追加任务记录（模板见 SKILL §5） |
| `docs/TEAM_TASK_BOARD.md` | 任务开始/认领/状态变化/完成/阻塞 | 更新责任人、状态、依赖、阻塞 |
| `CHANGELOG.md` | 功能、修复、架构或重要工程变更 | 追加本次变更 |
| `docs/audit/TEST_EVIDENCE.md` | 每次真实测试执行 | 记录命令、输出摘要、退出码、结论 |
| `docs/audit/OPERATION_LOG.md` | 实质性开发操作 | 记录真实操作与命令 |
| `docs/audit/SKILL_COMPLIANCE.md` | Skill 执行或规则偏离 | 记录适用/不适用及理由 |
| `docs/audit/ISSUES.md` | 新问题、阻塞、风险 | 更新问题状态 |
| `docs/audit/DECISION_LOG.md` | 新决策、确认或改变 | 追加决策（不覆盖） |
| `docs/audit/AUDIT_LOG.md` | 重要审计/PR/合规/治理事件 | 记录证据 |
| `README.md` | 启动方式、功能、环境或使用方式改变 | 更新用户入口 |
| `AGENTS.md` | 项目长期规则改变 | 更新强制规则 |
| `docs/ARCHITECTURE.md` | 模块、依赖、数据所有权、层级变化 | 同步架构 |
| ADR（`docs/ARCHITECTURE.md` 内或 `docs/design/`） | 重大架构决策/冻结契约改变 | 追加新 ADR，禁止覆盖旧决策 |
| `docs/API_CONTRACT.md` | API 请求/响应/错误码/权限/路径改变 | 同步契约 |
| `docs/DATABASE_CONTRACT.md` | 表/字段/约束/索引/Migration 变化 | 同步数据库契约 |
| `docs/ENUMS.md` | 枚举值或状态转换变化 | 同步枚举 |
| `docs/design/BUSINESS_FLOWS.md` | 事务边界/状态转换变化 | 同步流程 |
| `docs/REQUIREMENTS.md` | 业务规则/验收场景变化 | 同步需求 |
| 安全设计（`docs/ARCHITECTURE.md` §安全） | 权限/认证/敏感数据/安全策略改变 | 同步安全设计 |
| `docs/DEVELOPMENT.md` | 阶段、工程流程、环境、里程碑变化 | 更新当前真实状态 |
| `docs/testing/TEST_PLAN.md` | 测试范围/策略/验收标准改变 | 更新测试计划 |
| GitHub Issue | 独立任务/缺陷/阻塞/待解决需求 | 创建、更新或关联 |
| GitHub PR | 代码准备审查或合并 | 创建、更新或关联 |

判定问题（每轮结束自查）：

1. 是否修改代码？ 2. 是否修改 API？ 3. 是否修改数据库？ 4. 是否修改模块职责？ 5. 是否修改架构？ 6. 是否修改测试？ 7. 是否修改安全规则？ 8. 是否修改任务状态？ 9. 是否产生新问题？ 10. 是否产生新决策？ 11. 是否执行 Git 操作？ 12. 是否与队友协作？ 13. 是否修改 GitHub？ 14. 其他受影响文档？

结论示例：`UPDATED`（已更新）/ `NOT NEEDED`（无需）/ `NOT RUN`（未执行，说明原因）/ `BLOCKED`（阻塞）。
