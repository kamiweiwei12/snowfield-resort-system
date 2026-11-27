---
name: project-governance
description: Mandatory per-round engineering governance for the Snowfield resort project — restore real project state before acting, classify the task, plan before writing, keep docs / git / GitHub / teammate collaboration in sync, and close every round with verifiable evidence. Use before any substantive development, debugging, review, database, test, docs, architecture, Git, or collaboration task in this repository.
---

# Project Governance

本 Skill 是雪野营地项目每轮实质性开发工作的**强制治理流程**，跨开发者、跨 AI 会话长期有效。目标是让每一轮对话都能从真实状态出发、遵守冻结架构与契约、及时同步文档、不越权、可复核。

除非某项明确不适用（记 `NOT NEEDED`）或受阻（记 `NOT RUN`/`BLOCKED`），否则必须执行；不得用形式化日志掩盖未执行的操作。

---

## 1. 适用范围

以下任务开始前必须执行本 Skill：

- 代码开发、调试、重构（后端/前端）。
- 数据库、Migration、SQL、数据访问。
- 测试、构建、CI。
- 架构、设计、ADR、契约变更。
- 文档修改、开发日志、审计记录。
- Git 提交、分支、PR、Review、合并。
- 与队友协作、任务认领、Issue 管理。

简单的问答、纯聊天、单个文件的错别字修正可不走完整流程，但仍需遵守"不虚构、不越权"。

---

## 2. 每轮启动（先恢复真实状态，不靠记忆）

### STEP 1 — 恢复上下文（按任务相关性读取，勿全仓通读）

1. 根目录 `AGENTS.md` 与本 Skill。
2. `docs/DEVELOPMENT.md`（阶段/里程碑/环境）。
3. `docs/TEAM_TASK_BOARD.md`（任务认领/状态/阻塞）。
4. `docs/development-log/` 最近日志、`docs/audit/OPERATION_LOG.md`、`docs/audit/ISSUES.md`。
5. 当前分支、HEAD、工作树、远端状态（见 STEP 2）。
6. 按任务类型补读：
   - 数据库 → `docs/DATABASE_CONTRACT.md`、`docs/design/DATABASE_DESIGN.md`、`docs/audit/DECISION_LOG.md`。
   - API → `docs/API_CONTRACT.md`、`docs/ARCHITECTURE.md` 模块边界。
   - 枚举/状态 → `docs/ENUMS.md`。
   - 前端 → `docs/ARCHITECTURE.md`、`docs/API_CONTRACT.md`、`docs/TEAM_TASK_BOARD.md` 分工。
   - Git/PR → `git-pr` Skill、当前分支与 PR 状态。

### STEP 2 — 检查 Git 与工作树

记录：`git branch --show-current`、`git rev-parse --short HEAD`、`git status --short --branch`、`git remote -v`、`git branch -vv`、`git log --oneline -5`。

- 有未提交修改/未跟踪文件时：先识别归属，判断是否属于本任务、是否属于他人/其他 AI。
- **不得**擅自覆盖、删除、清理或提交他人工作。
- 无法确认归属的修改 → 停止涉及它的操作并报告用户。

### STEP 3 — 检查 GitHub（有访问能力时）

用 `gh` 核实 PR/Issue/Review/CI 真实状态，不以本地 Git 推断远端。无法访问时记 `UNKNOWN`，不得假装已同步。

### STEP 4 — 输出简短状态摘要

只输出与当前任务有关的真实状态：当前阶段、分支/HEAD、工作树是否干净、任务是否已被认领、是否有 PR/Review、依赖与阻塞、本轮计划、是否需要用户批准。

### STEP 5 — 任务分类

`DOC` / `DESIGN` / `FEAT` / `FIX` / `TEST` / `DB` / `REVIEW` / `GIT` / `INTEGRATION` / `SECURITY` / `RESEARCH` / `MAINTENANCE`（可多选）。

### STEP 6 — 评估风险与影响

逐项判断是否涉及：冻结契约、数据库结构/Migration、跨模块、认证授权/密码/Token、他人正在改的文件、Git 历史、GitHub 权限/仓库设置、删除/覆盖/不可逆操作、部署/生产、测试数据库、队友确认、新 ADR。涉及高风险或范围不明时，先报计划并等待批准，不得直接写。

---

## 3. 执行原则

- **先计划后执行**：代码/架构/数据库/跨模块/Git/权限/多文件任务，先给出「目标·影响范围·拟改文件·不碰文件·步骤·测试·文档·Git·风险·是否需队友/批准」。用户已授权范围的除外。
- **最小改动**：只改必要文件；不擅自重构、不擅自换技术栈/加依赖/加模块/改公共契约/改冻结规则/删功能。发现额外问题记入 `docs/audit/ISSUES.md`，仅当其影响当前任务正确性时才一并处理。
- **禁止虚构**：测试、GitHub 状态、Review、任务认领、命令结果必须可复核；`UNKNOWN`/`NOT RUN` 不得写成 `PASS`；"理论上可行"不得写成"已运行成功"。
- **实时记录**：多步骤/多文件/含测试或 Git 的任务，过程中记录关键事件（计划、批准、改文件、命令、测试结果、失败、阻塞、Git/PR、队友沟通、决策、完成/暂停），不凭记忆事后补。

---

## 4. 文档同步机制

每轮结束后必须逐项检查文档影响；只有实际受影响的才更新，未更新的写明原因。完整矩阵见 `references/document-sync-matrix.md`。

**每轮至少检查**：`docs/development-log/<月>.md`、`docs/TEAM_TASK_BOARD.md`、`CHANGELOG.md`、`docs/audit/TEST_EVIDENCE.md`、`docs/audit/OPERATION_LOG.md`、`docs/audit/SKILL_COMPLIANCE.md`。

发现应更新而未更新的文档时，不得宣布任务完成——补齐更新，或明确阻塞原因、责任人与后续任务。

---

## 5. 开发日志制度

- 位置：`docs/development-log/YYYY-MM.md`（按月）。
- 每条记录用固定模板（见下），状态只能取 `COMPLETED` / `PARTIAL` / `BLOCKED` / `CANCELLED`。
- 只记录真实执行，禁止删除/覆盖历史日志。

```text
## YYYY-MM-DD | TASK-ID | 任务名称
- Developer: / Branch: / Start: / End:
- Issue: / PR: / Commit:
### 目标
### 实际实施
### 修改文件
### 执行命令
### 测试结果
### 问题与风险
### 文档同步
### Git / GitHub 操作
### 队友协作
### 下一步
### 状态
```

---

## 6. Git 规范

- **提交时机**：完成一个可独立验证的工作单元后提交；不长期累积无法审查的大量改动。
- **提交格式**：Conventional Commits（`feat/fix/refactor/test/docs/chore/security(scope): summary`）；单个可审查单元；不含无关修改、未验证文件、敏感信息、他人未授权工作。
- **分支**：`main`（版本）/ `develop`（集成）/ `feature/*`、`fix/*`、`docs/*`、`test/*`、`chore/*`；不在 `main` 直接开发，不在 `develop` 直接做未批准业务修改。
- **提交前检查**：`git status`、`git diff`、`git diff --check`、相关测试、文档同步、敏感信息、提交内容。

### 危险操作（必须先说明风险并取得明确授权）

`git reset --hard`、`git clean`、强推、rebase 已公开分支、删分支/删提交、覆盖他人工作、改历史、批量删文件、改 GitHub 仓库设置/分支保护/权限/仓库名、改全局 Git 配置。不得以"修复工程"为由擅自执行。

---

## 7. GitHub 协作规范

- **Issue**：独立功能/Bug/重要架构/数据库/阻塞/需 Review 时创建；含背景、目标、范围、验收标准、依赖、负责人、优先级；不创建无明确目标与验收条件的 Issue。
- **PR**：目标分支明确（日常 → `develop`，版本 → `main`）；含任务关联、实施内容、修改文件、架构/数据库/API/安全影响、测试结果、文档同步、风险、未完成项、Review 要求；不得擅自把 feature PR 合到 `main`。
- **Review**：由合适成员审查（业务、架构边界、跨层调用、模块依赖、SQL 安全、并发、事务、数据完整性、API、权限、测试、文档、Git 历史）；**AI 不得代替队友 Review/Approve，不得用队友账号**。
- **Merge**：仅在目标正确、Review 满足、CI 通过、冲突已解决、测试证据存在、文档已同步、无高风险未决、已授权时合并；合并后检查目标分支/合并提交/CI/任务板/日志/审计，不得仅凭 GitHub 显示 `MERGED` 即认为集成成功。

---

## 8. 队友协作

- **需要联系队友**：改公共契约/共享表/Migration/公共 API/DTO/类型/认证授权/前端公共组件/分工，或影响队友已认领任务、需要合并/Review、存在冲突或阻塞。
- **原则**：先查任务板与 Issue/PR、确认归属；不覆盖队友文件、不改队友分支、不代替队友提交/Review/Approve/合并；无真实证据不得把队友工作写成已完成。
- **共享文件**（多人改前必须查认领）：`AGENTS.md`、`DEVELOPMENT.md`、`TEAM_TASK_BOARD.md`、OpenAPI 契约、公共 DTO、Migration、公共类型、共享组件、架构图、ADR、CI 配置。Migration 遵守统一版本编号与合并规则。

---

## 9. 架构与代码约束（遵守当前冻结架构）

- **C2 分层**：Controller 不碰 Mapper；Domain 不依赖 Spring/MyBatis/HTTP/DB；Application 负责用例编排与事务；Infrastructure 实现持久化接口；跨模块只走公开契约，不跨模块调 Mapper、不产生循环依赖、不往 `common` 塞业务逻辑。
- **C3 模块边界**：改跨模块前查模块所有权、依赖图、数据所有权、公开 API、事务边界、是否循环/重复逻辑、是否需 ADR。
- **C5 数据访问**：`#{}` 参数绑定、避免不受控动态 SQL、SQL 归属模块明确、事务/幂等/并发明确、数据库约束优先保证完整性、复杂 SQL 与 Migration 必须有测试、不在 Java 与数据库函数重复实现同一套业务规则。
- **安全**：认证、角色授权、对象级授权、密码存储、JWT、敏感信息、SQL 注入、CORS/CSRF、日志泄漏、核销码、幂等、频率限制、管理接口、审计；安全修改必须有测试。

架构文档与代码冲突时先记录问题，不通过改代码来推翻冻结架构。

---

## 10. 测试与证据

- 按影响范围选择：单元 / Application / API / 数据库集成 / Migration / 并发 / 安全 / 架构 / 前端 / 构建 / CI。
- 结果分类：`PASS`（真实执行且通过）/ `FAIL` / `PARTIAL` / `NOT RUN` / `BLOCKED` / `UNKNOWN`；**不得凭代码阅读标记 PASS**。
- 记录：时间、命令、环境、范围、结果、退出码、失败原因、是否重试、环境限制。
- 数据库相关：真实 PostgreSQL 验证 Migration/约束/外键/唯一/幂等/并发/回滚/完整性；并发核心不可只用 H2 或 Mock。
- 证据写入 `docs/audit/TEST_EVIDENCE.md`，失败保留并追加复测，不覆盖。

---

## 11. ADR 与决策

- 需要 ADR：模块划分、分层、数据库模型、技术栈、API/安全/事务/并发/价格/Migration/Git/测试架构变化。
- 原则：追加式；记录背景、问题、备选、选择、理由、影响、风险、状态、日期、相关任务；已批准 ADR 不覆盖/删除；被取代时标注旧 ADR 状态（`SUPERSEDED`）与取代关系。
- 状态：`PROPOSED` / `ACCEPTED` / `REJECTED` / `SUPERSEDED` / `DEPRECATED`；不得把 `PROPOSED` 写成 `ACCEPTED`。
- 决策记录在 `docs/audit/DECISION_LOG.md`。

---

## 12. 问题与风险

- 严重度：`P0`（安全/数据损坏/不可恢复/核心系统）、`P1`（核心正确性/架构/阻塞）、`P2`（功能/质量/协作）、`P3`（优化/体验/非紧急）。
- 记录：编号、时间、来源、影响、严重度、状态、责任人、方案、关联 Issue/PR、关闭证据。
- 写入 `docs/audit/ISSUES.md`；只有实际追踪价值的问题才建正式 Issue，不为普通问题过度流程。

---

## 13. 每轮结束闭环（强制）

1. **检查修改**：`git status`、`git diff`、`git diff --check`、有无意外文件、敏感信息、未决问题。
2. **测试**：运行本轮适用测试，记录真实命令/结果/退出码。
3. **文档同步**：逐项检查文档矩阵，至少更新开发日志、任务板、CHANGELOG、`TEST_EVIDENCE`、`OPERATION_LOG`；其余按实际影响。
4. **Git**：判断是否提交/推送/建 PR/更新 PR/关联 Issue/同步分支；未经批准不执行危险操作。
5. **队友协作**：判断是否通知/Review/认领/阻塞/同步契约/安排下一任务。
6. **审计记录**：按实际更新 `OPERATION_LOG`、`TEST_EVIDENCE`、`SKILL_COMPLIANCE`、`AUDIT_LOG`、`DECISION_LOG`、`ISSUES`（不要求每轮全改）。
7. **输出报告**：本轮完成、修改文件、测试结果、Git 状态、文档同步（已更新/无需/未更新及原因）、队友协作、未决问题、下一步。

---

## 14. 异常与阻塞

发现冻结契约冲突、架构冲突、数据库设计缺陷、Migration 冲突、Git 历史异常、队友同改同一文件、GitHub 权限不足、测试无法运行、环境不兼容、安全风险、范围不明、可能破坏数据或影响他人时——**暂停受影响操作**，报告：真实问题、证据、影响、可选方案、推荐方案、风险、需要谁批准。不为了完成任务绕过门禁。

---

## 15. 禁止事项（红线）

- 不得擅自合并 PR、改分支保护、改 GitHub 权限/仓库设置、改全局 Git 配置、重写历史、强推。
- 不得用 `reset --hard` / `git clean -fd` / 强制覆盖解决普通工作树问题。
- 不得删除/覆盖队友或他人文件、不得冒用队友身份。
- 不得在契约冻结前创建业务表/接口、不得修改冻结契约、不得初始化业务模块（除非用户明确批准）。
- 不得虚构测试/Review/命令结果；不得把 `UNKNOWN`/`NOT RUN` 写成 `PASS`。
- 不得为形式上的架构完整性引入不必要的抽象/模块/依赖。
