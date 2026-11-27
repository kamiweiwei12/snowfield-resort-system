# 操作日志

## OP-20261125-01：R-3 + R-4闭合整改

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 16:26:23–16:32:13 +08:00 |
| 任务 | 恢复历史决策可追溯性、补齐八项契约审计项、建立六类审计文件及三个项目Skill |
| 分支 | `feature/collaboration-prep`，开始HEAD `288273692bdd48eb597b163f72c7c8e0af90ea9d` |
| 修改文件 | `AGENTS.md`、`CHANGELOG.md`、`README.md`、公共契约/流程/状态文档、`PHASE2_CONFIRMATIONS.md`、`.agents/skills/**`、`docs/audit/**`；未创建代码、迁移或工程配置 |
| 操作 | 读取指定文档；检查Git分支/HEAD/状态/远端/历史；用 `git show 2882736^:<file>` 恢复历史；以补丁修改文档；创建并复读项目Skill |
| Skill | 已读主机 `skill-creator/SKILL.md`；已创建并读项目 `contract-change`、`git-pr`、`test-evidence`，详情见 `SKILL_COMPLIANCE.md` |
| 测试 | 已执行历史追加性、契约术语、内部链接、凭据模式、Skill结构及 `git diff --check`；Skill验证前两轮失败、第三轮通过，均保留；业务/数据库/性能测试NOT RUN |
| 结果 | `PUSHED_FOR_REVIEW`；历史原文已恢复，候选状态已纠正，D-001/D-002保持待决；未Review、未合并 |
| Commit | 主整改 `0586c32a457e8063534ecb753ba323cbe5b764a9`；证据回填 `566e53b84ff2a3d4223bb144013471f0bd25bbfd`；本段所在闭合提交以Git历史为准（提交对象无法在自身内容中自引用SHA） |
| 风险 | 外部审计报告原文未入库；确认者A/B身份为UNKNOWN；D-001/D-002待人工决定；回滚方式为revert本轮未来提交，不改写历史 |

### 已实际执行的关键命令

```text
Get-Content AGENTS.md -Raw
git branch --show-current
git rev-parse HEAD
git status --short --branch
git remote -v
git log -6 --decorate --oneline --all --graph
git rev-list --left-right --count HEAD...origin/feature/collaboration-prep
git show 2882736^:docs/PHASE1_REVIEW.md
git show 2882736^:docs/PHASE2_DECISIONS.md
git diff 2882736^ 2882736 -- <contract files>
```

上述命令均在PowerShell、工作目录 `D:/Will/SYSU/数据库系统原理/大作业` 执行；关键输出与后续验证见 `TEST_EVIDENCE.md`。本段保留记录创建时状态；随后实际执行的检查追加在下方。

### 整改阶段追加操作

```text
py quick_validate.py <skill>                       # 第一次有效解释器复测，编码失败
$env:PYTHONUTF8='1'; py quick_validate.py <skill> # 编码修正后复测
git diff 0cd6bae -- docs/PHASE1_REVIEW.md docs/PHASE2_DECISIONS.md
rg <version and contract terms> <contract files>
PowerShell Markdown内部链接检查
rg <credential-like patterns> .
git diff --check
git diff --no-ext-diff -- <all tracked changed files, split into review groups>
```

完整结果、退出码与首次 `python` 不可用失败见TE-20261125-03/04。提交、推送和最终工作区状态将在实际发生后追加，不在此预填。

### 主整改提交结果

- 实际命令：`git commit -m "docs(audit): remediate R-3 and R-4 findings"`。
- 实际结果：创建提交 `0586c32a457e8063534ecb753ba323cbe5b764a9`，26个文件，508行新增、51行删除；退出码0。
- 提交后状态：`feature/collaboration-prep` 相对远端 ahead 1，工作区干净。
- 推送：本段写入时尚未执行，不提前写成功。

### 推送结果

- 实际命令：`git push origin feature/collaboration-prep`，退出码0。
- 实际输出：远端从 `2882736` 前进到 `566e53b`；随后ahead/behind为 `0 0`。
- `git ls-remote`：feature为 `566e53b84ff2a3d4223bb144013471f0bd25bbfd`；`main`、`develop` 均保持 `9c9cd3c8bbc7391da9638140a47bd6d92175ef57`。
- 本段将通过独立审计闭合提交保存；其SHA和最终推送状态以Git历史及最终任务报告共同核验。

## OP-20261125-02：批准D-001与D-002并同步候选契约

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 17:05:49–17:08:46 +08:00（提交与推送结果在下方追加） |
| 任务 | 根据项目负责人批准，将D-001=B、D-002=A同步到公共契约和治理文档；保持v1.0-rc |
| 分支 | `feature/collaboration-prep`，开始HEAD `131ec982be0af8f2afe8e272a66086505430e6a7`；开始时与远端ahead/behind `0 0` |
| 修改文件 | `CHANGELOG.md`、`README.md`、DB/API/ENUM/需求/业务流程/冻结/开发/任务板/确认文档，以及审计决策、问题、操作、测试和Skill合规记录；未创建代码、迁移或工程配置 |
| 操作 | 读取AGENTS与三个项目Skill；核对分支/HEAD/状态；按批准同步契约；完整diff自检时发现并移除未获批准的“方案B+复合外键”混合约束；分拆重跑静态检查 |
| Skill | `contract-change`、`git-pr`、`test-evidence` 均完整读取并应用，详见 `SC-20261125-02` |
| 测试 | 文档术语交叉检索、Markdown内部链接、凭据模式、完整diff和 `git diff --check`；业务/数据库/调度测试NOT RUN |
| 结果 | D-001/B与D-002/A已写入未提交工作区；候选状态未升级；未创建、批准或合并PR |
| Commit | 本段创建时未提交；实际提交与推送结果仅在执行后追加 |
| 风险 | 定时任务周期仍是部署配置；运行时约束和并发行为尚无实现测试；正式冻结仍依赖 `Janovo2117` 的真实PR Review；回滚使用revert未来提交，不改写历史 |

### 已实际执行的关键命令

```text
git status --short --branch
git rev-parse HEAD
git rev-list --left-right --count HEAD...origin/feature/collaboration-prep
git diff --name-status
git diff --stat
rg <D-001/D-002/status and cross-contract terms> <contract files>
PowerShell Markdown内部链接检查
rg <credential-like patterns> .
git diff --no-ext-diff -- <all changed files, split into groups>
git diff --check
```

第一次组合静态检查的工具输出因显示上限截断，未据此宣称PASS；随后拆分命令重跑并记录可见输出和退出码。完整diff自检发现草稿曾将D-001/B与复合外键混合，已在提交前纠正为批准的纯方案B。

### 提交与推送结果

- 主提交：`df0e33cbc67bafcef546692334f6b47145f6d6a7`，信息 `docs(contract): approve D-001 and D-002`，16个文件、127行新增、29行删除；提交命令退出码0。
- 推送：`git push origin feature/collaboration-prep`，远端从 `131ec98` 前进到 `df0e33c`，退出码0。
- 推送后核对：ahead/behind为 `0 0`；远端feature为 `df0e33cbc67bafcef546692334f6b47145f6d6a7`；`main`、`develop` 均保持 `9c9cd3c8bbc7391da9638140a47bd6d92175ef57`。
- 本段证据回填将以独立闭合提交保存；该闭合提交SHA以Git历史为准，避免文件自引用自身提交对象。

## OP-20261126-01：核查PR与Review真实状态并纠正目标分支

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:42:13–09:47 +08:00（Asia/Shanghai） |
| 任务 | 按交接指令做只读状态核查；确认PR是否存在、来源/目标分支、Review状态与协作者权限；纠正错误目标分支并回填审计 |
| 分支 | `feature/collaboration-prep`，开始HEAD `08ff87f1fbb7b511628da5ec22ca26ec873ae6c6`；开始时工作区干净、ahead/behind `0 0` |
| 修改文件 | `docs/audit/ISSUES.md`、`docs/audit/OPERATION_LOG.md`、`docs/audit/TEST_EVIDENCE.md`、`docs/audit/SKILL_COMPLIANCE.md`、`docs/CONTRACT_FREEZE.md`、`CHANGELOG.md`；未创建代码、迁移、依赖或工程配置，未修改任何契约规则条文 |
| 操作 | 读取AGENTS.md、三个项目Skill、README/CHANGELOG/冻结记录/确认记录/四份事实源/任务板/审计文件；执行Git与GitHub只读核查；用 `gh pr edit` 纠正PR目标分支；追加审计条目 |
| Skill | `contract-change`（有限适用：仅更正风险陈述，未改变契约条文）、`git-pr`、`test-evidence` 均完整读取并应用，详见 `SC-20261126-01` |
| 测试 | Git/GitHub状态核查、PR元数据核查、枚举交叉检索、Markdown内部链接、完整diff与 `git diff --check`；业务/数据库/前端/并发/性能测试NOT RUN |
| 结果 | PR #1 目标分支由 `main` 纠正为 `develop`；R5-01..R5-05 已登记；契约仍为 `v1.0-rc`；未创建Review、未合并、未改分支保护 |
| Commit | 本段创建时未提交；实际提交与推送结果在下方追加 |
| 风险 | 分支保护缺失需人工配置；D-001/D-002仍无另一成员PR Review；R5-04/R5-05为待Reviewer判定的歧义，未擅自改契约；回滚方式为revert本轮追加提交，不改写历史 |

### 核查发现（与交接描述的差异）

1. 交接称“尚未创建PR”，实际 PR #1 已存在且为 `OPEN`，创建于 `2026-11-25T08:47:47Z`（16:47:47 +08:00），早于 `OP-20261125-02` 记录的 17:05–17:08。该条目的“未创建PR”陈述与外部事实不一致，已登记 R5-02，原文不删除，以追加方式纠正。
2. PR #1 的来源分支正确（`feature/collaboration-prep`），但目标分支为 `main`，违反 AGENTS.md §6；已登记 R5-01 并纠正为 `develop`。
3. PR 描述正文为空，无指派Reviewer，`reviews` 为空数组，timeline 仅有 `committed` 事件，无CI检查；因此“双人Review已完成”为 `UNVERIFIED`。
4. `Janovo2117` 在协作者列表中为 `write` 角色，`/invitations` 为空，可核验其邀请已被接受并具备写入/批准权限；但这只证明权限，不证明已实际执行Review。
5. PR 作者为 `kamiweiwei12` 本人，GitHub 禁止自我Approve，故 `DECISION_LOG.md` 中 D-001/D-002 的批准不构成PR Review证据。
6. `main`、`develop`、`feature/collaboration-prep` 均 `protected:false`，已登记 R5-03。

### 已实际执行的关键命令

```text
git branch --show-current
git rev-parse HEAD
git status --short --branch
git remote -v
git log -6 --decorate --oneline --all --graph
git rev-list --left-right --count HEAD...origin/feature/collaboration-prep
git ls-remote --heads origin feature/collaboration-prep develop main
gh auth status
gh pr list --repo kamiweiwei12/- --state all --limit 20
gh pr view 1 --repo kamiweiwei12/- --json <metadata,reviews,reviewRequests,reviewDecision,files>
gh api repos/kamiweiwei12/-/pulls/1/reviews
gh api repos/kamiweiwei12/-/issues/1/timeline
gh api repos/kamiweiwei12/-/collaborators
gh api repos/kamiweiwei12/-/invitations
gh api repos/kamiweiwei12/-/branches
gh pr checks 1
gh pr edit 1 --repo kamiweiwei12/- --base develop
git diff --stat 9c9cd3c..HEAD
```

上述命令均在Git Bash、工作目录 `D:/Will/SYSU/数据库系统原理/大作业` 执行；输出摘要与退出码见 `TEST_EVIDENCE.md` TE-20261126-01..03。

### 目标分支纠正结果

- 实际命令：`gh pr edit 1 --repo kamiweiwei12/- --base develop`，退出码0。
- 实际效果：PR #1 `baseRefName` 由 `main` 变为 `develop`；该操作只改PR元数据，不新增提交、不改写历史、不需要强制推送。
- 因 `main` 与 `develop` 均指向 `9c9cd3c8bbc7391da9638140a47bd6d92175ef57`，PR变更内容不变（28文件，+985/−61）。

### 提交与推送结果

- 主提交：`16e194e9daa1e78d45baa676518f0b0666a2fc45`，信息 `docs(audit): record PR inspection and correct target branch`，6个文件、134行新增、1行删除；提交命令退出码0。
- 推送：`git push origin feature/collaboration-prep`，远端从 `08ff87f` 前进到 `16e194e`，退出码0。
- 推送后核对：ahead/behind 为 `0 0`；远端feature为 `16e194e9daa1e78d45baa676518f0b0666a2fc45`；`main`、`develop` 均保持 `9c9cd3c8bbc7391da9638140a47bd6d92175ef57`；PR #1 `baseRefName=develop`、`headRefOid` 随推送更新。
- 本段证据回填以独立闭合提交保存；该闭合提交SHA以Git历史为准，避免文件自引用自身提交对象。

## OP-20261126-02：按第二次审计修复契约一致性缺口

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 10:19:35–10:30 +08:00（Asia/Shanghai）；提交与推送时间见下方追加段 |
| 任务 | 处理第二次独立审计的 P1-1、P1-2、P2-1、P2-2、P2-3、P2-4、P2-5 与 G-3；按 D-003/D-004 落地占用枚举与核销唯一约束 |
| 分支 | `feature/collaboration-prep`，开始HEAD `f3612dbd9eb8cffc12dbbf863296a5d55998b563`；开始时工作区干净、ahead/behind `0 0` |
| 修改文件 | `docs/design/DATABASE_DESIGN.md`、`docs/DATABASE_CONTRACT.md`、`docs/API_CONTRACT.md`、`docs/ENUMS.md`、`docs/CONTRACT_FREEZE.md`、`docs/TEAM_TASK_BOARD.md`、`docs/audit/{ISSUES,DECISION_LOG,OPERATION_LOG,TEST_EVIDENCE,SKILL_COMPLIANCE}.md`、`.agents/skills/contract-change/SKILL.md`、`CHANGELOG.md`、新增 `.gitattributes` |
| 操作 | 读取AGENTS与三个Skill；逐项核对审计报告行号；按D-003/D-004改契约；补ER与码摘要；扩写Skill同步清单；追加审计条目 |
| Skill | `contract-change`（首次按新8项清单执行，逐项结论见下）、`git-pr`、`test-evidence`；详见 `SC-20261126-02` |
| 测试 | 术语交叉检索、Markdown内部链接、Skill结构校验（含一次解释器缺 PyYAML 的失败）、`git diff --check`；业务/数据库/前端/并发测试NOT RUN |
| 结果 | 6项审计问题修复并登记 R5-06..R5-10；D-003/D-004 落为 `APPROVED_IN_RC`；契约仍为 `v1.0-rc`；未合并、未Review、未改分支保护 |
| Commit | 本段内容作为单个提交进入 `feature/collaboration-prep`；SHA 以 `git log -1` 为准，不在文件内自引用 |
| 风险 | R5-04（`price_rule` 类型与模式同时为空的语义）仍需业务裁定，本轮未擅自改；运行时测试全部NOT RUN；回滚为 revert 本轮提交 |

### contract-change 8项消费者清单执行结果

| 消费者 | 结论 |
|---|---|
| `docs/DATABASE_CONTRACT.md` | 已改：排斥谓词改 `HELD`；核销补 `result CHECK` 与部分唯一索引 |
| `docs/API_CONTRACT.md` | 已改：住宿 `quantity` 歧义表述 |
| `docs/ENUMS.md` | 已改：新增 `OccupancyStatus`、`VerificationResult` 及转换说明 |
| `docs/design/BUSINESS_FLOWS.md` | 无需修改：核销流程第6节已描述“锁令牌→校验→写事件→改USED”，与新增约束不冲突 |
| `docs/design/DATABASE_DESIGN.md` | 已改：候选码、ER 关系、基数说明 |
| `docs/CONTRACT_FREEZE.md` | 已改：追加 v1.0-rc 修订2 版本记录 |
| `docs/REQUIREMENTS.md` | 无需修改：FR-E 已要求“核销码只可成功使用一次”，新约束只是把它落到数据库 |
| `docs/TEAM_TASK_BOARD.md` | 已改：GOV-02 改名，新增 GOV-06 |

### 已实际执行的关键命令

```text
git branch --show-current / git rev-parse HEAD / git status --short --branch
grep -rn "idempotency_key" docs/
grep -n "Occupan|occupan" docs/ENUMS.md
grep -n "PENDING_PAYMENT" docs/DATABASE_CONTRACT.md
py -m pip install pyyaml            # 发现 pythoncore-3.14-64 已装，py 3.14.3 shim 缺该模块
PYTHONUTF8=1 <pythoncore-3.14-64>/python.exe quick_validate.py .agents/skills/<skill>
Markdown 内部链接检查（Python 脚本）
git diff --check
git diff -- <contract files>
```

上述命令在 Git Bash、工作目录 `D:/Will/SYSU/数据库系统原理/大作业` 执行；输出摘要与退出码见 TE-20261126-07/08。

### 提交与推送结果

- 本段全部改动作为**单个提交**进入 `feature/collaboration-prep`，不再拆分为“内容提交+证据回填提交”，以避免重复 P3-1 指出的“一个可审查单元被拆成多个提交”。
- 提交 SHA 与推送状态不写入文件（提交对象无法在自身内容中自引用），以 `git log -1` 和本轮任务报告为准。

## OP-20261126-03：按架构审查意见修订设计并建立 S1 工程骨架

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 22:05–22:25 +08:00（Asia/Shanghai） |
| 任务 | 理解项目负责人架构审查意见，修订架构设计文档（C2 分层/C3 模块/数据所有权/安全/事务/时间/测试/版本），并执行 S1 工程骨架 |
| 分支 | `feature/database-core`，开始 HEAD `3404221`（== origin/develop）；开始工作区含未跟踪 `.env.example`、`backend/`、`docker-compose.yml` |
| 修改/新增文件 | 修改 `docs/ARCHITECTURE.md`、`CHANGELOG.md`；重写未跟踪的 `backend/pom.xml`、`backend/src/**`；新增 `backend/mvnw`、`backend/.mvn/wrapper/`、`backend/src/test/**/architecture/ArchitectureTest.java` |
| 操作 | 核实 Boot 4.1.3 与 mybatis-spring-boot-starter 4.1.0 官方兼容；用缓存 Maven 3.9.16 生成 Maven Wrapper；`./mvnw -B test` 真实构建（首次 ArchUnit 空规则失败，修复后重跑通过）；执行 `git diff --check` |
| Skill | `contract-change` 不适用（仅改 ARCHITECTURE 设计文档，未改 DB/API/ENUM 三份契约）；`git-pr`、`test-evidence` 应用；详见 `SC-20261126-03` |
| 测试 | `./mvnw -B test`：Spring Boot 4.1.3 上下文加载 + 14 测试通过（首次 4 条 ArchUnit 分层规则因 S1 无 domain/api/infrastructure 包触发 `failOnEmptyShould`，加 `.allowEmptyShould(true)` 后全绿）；业务/数据库/MyBatis/并发测试 NOT RUN |
| 结果 | 架构设计 v2 已落盘（ADR-006~012）；S1 骨架可复现构建；未创建业务迁移（契约冻结 PR #2 未合并）；提交与推送结果以 git 历史为准 |
| 风险 | MyBatis 4.1.0/Flyway/PostgreSQL 尚未接入验证；`docker-compose.yml`/`.env.example` 保留为未跟踪（属 S2 DB 环境）；本地 `develop` 落后 origin/develop 12 提交（本次未触碰） |

## OP-20261126-04：建立项目级工程治理 Skill 与开发日志入口

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 22:47–22:5x +08:00（Asia/Shanghai） |
| 任务 | 按任务书建立项目级工程治理 Skill，并让 `AGENTS.md` 每轮强制触发；建立开发日志入口 |
| 分支 | `feature/database-core`，开始 HEAD `2bce281`；工作区含未跟踪 `.env.example`、`.workbuddy/`、`docker-compose.yml` |
| 修改文件 | 新增 `.agents/skills/project-governance/SKILL.md`、`references/document-sync-matrix.md`、`docs/development-log/2026-11.md`；修改 `AGENTS.md`、`CHANGELOG.md`、`docs/audit/*` |
| 操作 | 调查状态（branch/HEAD/status/gh pr/issue）；创建 Skill 与 reference；改 `AGENTS.md` §0；建开发日志；`git diff --check` |
| Skill | `project-governance`（本轮新建并自举）；`git-pr`、`test-evidence` 应用；`contract-change` 不适用（未改契约）；详见 `SC-20261126-04` |
| 测试 | `git diff --check` 退出码 0；无业务/数据库/并发测试（文档治理任务，NOT NEEDED） |
| 结果 | 治理 Skill 落盘并通过 `AGENTS.md` 强制触发；开发日志入口建立；未触碰业务/契约/数据库/GitHub 权限 |
| 风险 | 无业务风险；未推送、未建 PR、未合并（等人工审查） |

## OP-20261126-05：S2 数据库环境与 MyBatis/Flyway 接入验证

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:05–23:30 +08:00（Asia/Shanghai） |
| 任务 | 执行上一轮盘点建议：收编游离的 compose/env 文件；接入 MyBatis 4.1.0 + Flyway + PostgreSQL，并在真实 PostgreSQL 上验证；解除 CHANGELOG「Known limitations」中的待验证项 |
| 分支 | `feature/database-core`，开始 HEAD `d59c796`；工作区含未跟踪 `.env.example`、`.workbuddy/`、`docker-compose.yml` |
| 用户授权 | 用户指示「除影响双方合并的内容外，不随意触发队友 Review，其余按建议执行」。本轮全部为 A 侧后端/数据库自检，不含公共契约修改，因此**不请求队友 Review、不创建 PR** |
| 修改文件 | 新增 `scripts/test-db-init.ps1`、`backend/src/main/resources/db/migration/V1__baseline.sql`、`backend/src/test/resources/application-test.yml`、`backend/src/test/java/edu/sysu/snowfield/support/{AbstractPostgresIntegrationTest,FlywayMigrationIT}.java`、`backend/src/main/java/edu/sysu/snowfield/identity/infrastructure/persistence/{AppUserRecord,AppUserMapper}.java`、`backend/src/test/java/edu/sysu/snowfield/identity/infrastructure/persistence/AppUserMapperIT.java`；修改 `backend/pom.xml`、`backend/src/main/resources/application.yml`、`backend/src/test/java/edu/sysu/snowfield/SnowfieldApplicationTests.java`、`docker-compose.yml`、`.env.example` |
| 操作 | 先提交命名的 compose/env 收编（`eb67a1d`）；接入依赖并真实解析（TE-13）；首次集成测试失败并定位根因（TE-14）；用原生 JDBC 探针确认端口冲突（TE-15）；改容器端口 5433 后复测全绿（TE-16） |
| Skill | `project-governance`（启动检查/任务分类/文档同步/结束闭环）、`git-pr`（分支、提交、未推送未开 PR）、`test-evidence`（失败与复测均追加，不覆盖）；`contract-change` 不适用（未改 DB/API/ENUM 三份契约条文，V1 是其 §2「身份与权限」的落地子集） |
| 测试 | `mvn -B -Pdb test` → 28 测试全绿，BUILD SUCCESS，退出码 0；`mvn -B test` → 14 测试全绿，退出码 0；开发库表数 0、测试库 Flyway 记录 `1|baseline|t` |
| 结果 | MyBatis 4.1.0 + Boot 4.1.3 + PostgreSQL 16 + Flyway 12.4.0 真实链路打通；V1 基线迁移可在空测试库执行；约束反例测试通过；未触碰公共契约、未推送、未开 PR |
| Commit | 收编提交 `eb67a1d`；本轮主线提交 SHA 以 `git log -1` 为准，不在文件内自引用 |
| 风险 | ① 本机原生 PostgreSQL 18 占用 5432，已将容器改到 5433，其他人克隆后须同步 `.env` 的 `POSTGRES_PORT`；② `V1__baseline.sql` 只覆盖身份与权限三张表，其余契约表待 S3；③ 首次集成测试失败已保留为 TE-20261126-14，不隐藏 |

### 已实际执行的关键命令

```text
git add docker-compose.yml .env.example && git commit -m "chore(db): add local postgres compose and env template"
<wrapper-maven>/mvn.cmd -B dependency:tree
<wrapper-maven>/mvn.cmd -B -Pdb verify          # 首次失败，见 TE-20261126-14
java -cp postgresql-42.7.14.jar <probe>.java    # 根因确认，见 TE-20261126-15
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-db-init.ps1
docker compose up -d db                          # POSTGRES_PORT=5433
<wrapper-maven>/mvn.cmd -B -Pdb test            # TE-20261126-16，28 测试通过
<wrapper-maven>/mvn.cmd -B test                 # 默认构建，14 测试通过
git diff --check
```

### 环境事实（供后续会话复用）

- 本机已运行原生 `postgresql-x64-18` 服务（`D:\PostgreSQL`），占用 `0.0.0.0:5432`。
- 项目 PostgreSQL 容器 `snowfield-db`（`postgres:16-alpine`）映射 `5433->5432`，命名卷 `snowfield_pgdata`。
- 数据库账号 `snowfield`，密码取 `.env.example` 占位值；开发库 `snowfield`、测试库 `snowfield_test`。
- 集成测试必须用 `-Pdb`；默认 `./mvnw test` 不连库。

## OP-20261126-06：INF-04 统一响应/异常/健康/OpenAPI 的真实 HTTP 验证

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:33–23:50 +08:00（Asia/Shanghai） |
| 任务 | 执行 INF-04：让统一响应、全局异常、健康检查与 OpenAPI 文档在**真实启动的 HTTP 服务**上得到验证 |
| 分支 | `feature/database-core`，开始 HEAD `cf6a77b`，工作区干净 |
| 用户授权 | 用户指示「可以，先做吧」，按上一轮建议执行不依赖 PR #2 的 INF-04；未授权推送、未要求催队友 Review，故均未执行 |
| 修改文件 | 修改 `backend/pom.xml`（新增 springdoc 3.2.0）、`docs/ARCHITECTURE.md`（版本矩阵修正）；新增 `backend/src/test/java/edu/sysu/snowfield/support/WebContractIT.java`；审计与日志文件 |
| 操作 | 核查 springdoc 各版本父 POM 的 Boot 代际（TE-18）；锁定 3.2.0 并真实解析；编写 `RANDOM_PORT` 真实启动的 HTTP 契约测试；首次因 Boot 4 不提供 `RestClient.Builder` 失败，改用 `RestClient.create` 与 Jackson 3 后全绿（TE-19） |
| Skill | `project-governance`、`git-pr`、`test-evidence` 应用；`contract-change` **边界适用**：未修改 `API_CONTRACT.md` 任何条文，仅新增对该契约的测试；`ARCHITECTURE.md` 版本矩阵修正属设计文档同步，详见下 |
| 测试 | `mvn -B -Pdb test` → 33 测试全绿，退出码 0；`mvn -B test` → 14 测试全绿，退出码 0；首次失败保留为 TE-20261126-19 |
| 结果 | INF-04 平台层验收达成：成功信封、错误信封、404 映射、不泄漏内部细节、OpenAPI 文档均经真实 HTTP 验证；未推送、未开 PR |
| 风险 | ① springdoc 3.2.1 会引入 Boot 4.2，ADR-012 禁止，已在 pom 注释与版本矩阵中锁定 3.2.0 及复核要求；② 业务端点测试仍 NOT RUN，待 S4/S5 |

### 关于 `docs/ARCHITECTURE.md` 的修改说明

原版本矩阵写「springdoc-openapi | 3.2.x | Boot 4.x 配 3.x | S5 引入（暂缓）」。实测 3.2.1 的父 POM 为 Boot 4.2.0，与 ADR-012 冲突。本轮仅把该行的版本写实为 `3.2.0` 并注明「升级前须复核父 POM 的 Boot 版本」、状态改为「S2 已引入（3.2.0）」。这是**事实修正**，未改变任何架构决策或契约条文，不构成需队友评审的公共契约变更。

## OP-20261127-01：推送 PR #3 并按 v1.0-rc 落地 DB-02/DB-03 迁移

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:53–2026-11-27 00:15 +08:00（Asia/Shanghai） |
| 任务 | 按用户「先 3 然后 2」的顺序：先推送并开 PR，再按当前 `v1.0-rc` 写 DB-02/DB-03 迁移 |
| 分支 | `feature/database-core`，开始 HEAD `719e92e`，ahead origin/develop 6、behind 0 |
| 用户授权 | 明确授权推送与创建 PR（选项 3）、按 `v1.0-rc` 写迁移（选项 2） |
| 修改/新增文件 | 新增 `V2__campground_and_resource.sql`、`V3__price_rules_and_calendar.sql`、`ResourceAndPriceConstraintIT.java`；修改 `AbstractPostgresIntegrationTest.java`（清理表清单）、审计与日志文件 |
| 操作 | `git fetch` 核对远端；推送 feature 分支；`gh pr create` 建 PR #3；按契约 §2 写 V2/V3；空库重建后全量执行；补 21 个约束反例测试 |
| Skill | `project-governance`、`git-pr`、`test-evidence` 应用；`contract-change` **不适用**：V2/V3 只是把既有契约结构落为 DDL，未修改 `DATABASE_CONTRACT.md` 任何条文 |
| 测试 | `mvn -B -Pdb test`（空库）→ 54 测试全绿，退出码 0；`ResourceAndPriceConstraintIT` 21 用例全绿；首次编译错误已保留（TE-20261127-02） |
| 结果 | PR #3 已创建（`base=develop`，6 提交，41 文件）；V1→V3 迁移在干净库全量成功；14 张表落地 |
| 风险 | ① 迁移基于 `v1.0-rc`，若 PR #2 的评审改动契约结构，已合并的迁移不可编辑，需新迁移修正；② `R5-04` 双空语义未裁定，V3 刻意不加 CHECK；③ 住宿排斥约束要等 V4（订单域）才能真正验证 |

### 推送与 PR 结果

- `git push origin feature/database-core`：新分支创建成功，远端 `719e92e`；核对 ahead/behind `0 0`，`origin/develop` 保持 `3404221` 未动。
- 推送使用 `git -c credential.helper=...` 临时注入 gh token，**未写入全局 Git 配置、未修改仓库设置**。
- `gh pr create --base develop --head feature/database-core` → PR #3：`https://github.com/kamiweiwei12/snowfield-resort-system/pull/3`，状态 `OPEN`、`REVIEW_REQUIRED`、`mergeStateStatus=BLOCKED`（符合分支保护要求 1 个 approve）。

### 迁移设计说明（重要）

`price_rule` 的 `resource_type_id` 与 `stay_mode` **同时为 NULL** 的语义，在 `v1.0-rc` 与已合入的 `develop` 中均未裁定（ISSUES `R5-04` 状态 `OPEN_FOR_REVIEW`），PR #2 拟以 D-005 补充。因此 V3 **刻意不添加「至少一项非空」CHECK**：在契约裁定前把某一方语义写死进已发布迁移，会导致后续无法在不改迁移的前提下调整。`ResourceAndPriceConstraintIT.priceRuleBothNullIsCurrentlyPermitted` 固定当前实际行为，便于契约裁定后以新迁移显式变更。

### 环境注意

- 本机原生 PostgreSQL 18 占用 5432，项目容器映射 5433。
- 干净库重建命令：`DROP DATABASE snowfield_test` → `CREATE DATABASE snowfield_test` → `mvn -B -Pdb test`。

## OP-20261127-02：催办 PR #2 评审并落地 DB-04 订单主表（不含住宿占用）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 10:02–10:12 +08:00（Asia/Shanghai） |
| 任务 | 按用户「同意，执行」：执行上一轮建议的第 3 项（催 PR #2 评审）与第 1 项中不涉及住宿占用约束的订单主表部分 |
| 分支 | `feature/database-core`，开始 HEAD `f7c3ae3`，ahead origin/develop 7、behind 0；`origin/develop` 仍为 `3404221` |
| 用户授权 | 明确同意催促 PR #2 评审，并同意先建订单主表（暂不做 `lodging_reservation` 与排斥约束） |
| 修改/新增文件 | 新增 `V4__order_and_payment.sql`、`OrderAndPaymentConstraintIT.java`；审计与日志文件 |
| 操作 | `gh pr comment 2` 留评审请求；按契约 §2 写 V4（7 张表）；空库重建后 V1→V4 全量执行；补 20 个约束测试 |
| Skill | `project-governance`、`git-pr`、`test-evidence` 应用；`contract-change` **不适用**：V4 是既有契约结构的 DDL 落地，未改任何契约条文 |
| 测试 | `mvn -B -Pdb test`（空库）→ 74 测试全绿，退出码 0；`OrderAndPaymentConstraintIT` 20 用例全绿；首次失败已保留（TE-20261127-05）；`mvn -B test` → 14 测试全绿 |
| 结果 | PR #2 已留评审评论；V1→V4 迁移链在干净库成功；19 张业务表落地 |
| 风险 | ① V4 基于未冻结契约；② `package_component` 待 B 侧表；③ `lodging_reservation` 与排斥约束刻意未做；④ 订单金额跨行汇总一致性未在数据库强制 |

### 催办 PR #2 的具体动作

- 复核发现 `Janovo2117` **已在** `reviewRequests` 中（`REVIEW_REQUIRED`，0 条 review），重复 re-request 只会产生噪音，故未重复请求。
- 改为在 PR #2 留评论（[issuecomment-7207792703](https://github.com/kamiweiwei12/snowfield-resort-system/pull/2#issuecomment-7207792703)），聚焦说明：① DB-04 依赖本 PR 的契约表述；② **D-005 批准与否对下游迁移的直接后果**——批准则 V4 无需改动，否决则需新增迁移加 CHECK；③ 其余 18 个文件为状态与审计回填，非新业务规则。
- 未代替队友评审、未 approve、未合并。

### V4 的范围决策（重要）

契约中订单域有 9 组表，本轮**只建其中 7 张**：

- 已建：`package_offer`、`customer_order`、`order_package`、`order_item`、`order_status_history`、`order_item_status_history`、`payment`、`price_snapshot_line`（8 张，见迁移注释）。
- **未建 `package_component`**：其外键指向 `ski_product`/`lift`/`equipment_model`，属 B 侧模块（SKI-01/LIFT-01/EQ-01）尚未建立，强建会引入指向不存在表的悬空设计。
- **未建 `lodging_reservation`**：住宿时间排斥约束是并发正确性核心（BK-03 要求 100 并发抢 1 资源仅 1 成功），必须与并发测试同轮验证；先建表会给出「已实现」的假象。

### 关于订单总额汇总的数据库层选择

契约要求「住宿逐日合计以 `price_snapshot_line` 汇总并在服务层/过程校验」，且 ADR-009 规定跨模块事务由 `BookingApplicationService` 编排。若在数据库用跨表聚合触发器强制「订单总额 = Σ明细 − Σ优惠」，会与事务编排设计冲突且难以在并发下正确表达。因此 V4 只落地可独立验证的局部不变量，跨行汇总一致性明确留给 BK-02 应用层，并在 TEST_EVIDENCE TE-20261127-06 中标注 NOT RUN，不声称已完成。

## OP-20261127-03：BK-03 住宿占用迁移与一百并发抢订验证

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 11:44–12:00 +08:00（Asia/Shanghai） |
| 任务 | 按用户「可以，做1」执行 BK-03：住宿占用表、排斥约束与 100 并发抢 1 资源测试同轮完成 |
| 分支 | `feature/database-core`，开始 HEAD `3540d59`，ahead origin/develop 8、behind 0；`origin/develop` 仍为 `3404221` |
| 用户授权 | 明确同意执行 BK-03 |
| 修改/新增文件 | 新增 `V5__lodging_reservation.sql`、`LodgingExclusionConstraintIT.java`、`LodgingConcurrencyIT.java`；审计与日志文件 |
| 操作 | 按契约 §2 建 `lodging_reservation` 与 `EXCLUDE USING gist` 约束；空库重建后 V1→V5 全量执行；写 12 个约束语义测试 + 3 个并发测试；并发测试连跑 3 轮稳定性复测 |
| Skill | `project-governance`、`git-pr`、`test-evidence` 应用；`contract-change` **不适用**：V5 是既有契约结构的 DDL 落地，未改任何契约条文 |
| 测试 | `mvn -B -Pdb test`（空库）→ 89 测试全绿，退出码 0；`LodgingConcurrencyIT` 4 轮全绿（100 并发 → 成功 1 / 冲突 99 / 残留 1）；`mvn -B test` → 14 测试全绿 |
| 结果 | 排斥约束落地并经反例与并发双重验证；BK-03 验收标准达成 |
| 风险 | ① 迁移基于未冻结契约；② 只验证数据库层，应用层状态流转与事务同步未实现；③ 并发测试耗时约 12 s，随测试量增长需关注 |

### 排斥约束的实现要点

```sql
ALTER TABLE lodging_reservation
    ADD CONSTRAINT ex_lodging_reservation_no_overlap
    EXCLUDE USING gist (resource_unit_id WITH =, stay_period WITH &&)
    WHERE (occupancy_status IN ('HELD', 'CONFIRMED', 'IN_USE'));
```

- 依赖 `btree_gist` 扩展（已在 V2 中 `CREATE EXTENSION IF NOT EXISTS btree_gist` 创建）。
- `RELEASED` 被排除在谓词外：这是契约强调的关键点，否则已取消/过期订单会永久阻塞同一资源。测试 `releasedReservationDoesNotBlockResource` 与 `releasedIsTerminalAndNoLongerExcluded` 直接验证了这一点。
- 半开区间 `[start, end)` 语义由测试 `adjacentReservationsAreAllowed` 验证：26 日 12:00 退房、同日 12:00 入住不判为重叠。

### 关于「订单状态与占用状态同事务同步」

契约要求两者同事务维护，并明确「不得由两个任意入口各自维护」。本轮**未**用数据库触发器强制双写，理由：ADR-009 规定跨模块事务由 `BookingApplicationService` 统一编排，用触发器复制业务规则会与 Java 侧逻辑重复并难以在并发下正确表达（AGENTS.md §9 禁止「在 Java 与数据库函数重复实现同一套业务规则」）。该项留待 BK-02 在应用层实现，已在 TE-20261127-10 标注 NOT RUN。

### 首次失败两次（均保留）

1. `LodgingExclusionConstraintIT.emptyPeriodIsRejected`：断言绑定了 `period_nonempty`，实际 PostgreSQL 先触发 `period_bounded`。约束行为正确，测试断言过窄，已改为断言约束名前缀并补 `unboundedPeriodIsRejected`（TE-20261127-08）。
2. `LodgingConcurrencyIT.concurrentBookingsOnDisjointPeriodsAllSucceed`：测试用 `1 + i*3` 手算日期，生成 `2027-03-33` 越界。改用 `LocalDate.plusDays`（TE-20261127-09）。

两次均为**测试自身缺陷**，不是产品代码缺陷；均已修复并复测通过，失败记录保留未删除。

## OP-20261127-04：BK-02 第一阶段——创建住宿订单事务编排

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 12:23–12:50 +08:00（Asia/Shanghai） |
| 任务 | 按用户「按照建议继续」执行 BK-02 第一部分：创建订单 + 占用 + 幂等（应用层事务） |
| 分支 | `feature/database-core`，开始 HEAD `ee3606b`，ahead origin/develop 9、behind 0；`origin/develop` 仍为 `3404221` |
| 用户授权 | 同意按建议继续 BK-02，允许分两次交付 |
| 修改/新增文件 | 新增 `catalog/{application,infrastructure/persistence}`、`pricing/{domain,application,infrastructure/persistence}`、`lodging/{application,infrastructure/persistence}`、`order/{application,infrastructure/persistence}`、`booking/application` 共 12 个类；修改 `common/web/BusinessException.java`（新增带 details 的构造）；新增 `BookingApplicationServiceIT` |
| 操作 | 按 ARCHITECTURE.md §1 四层与 §2.1 依赖图建模块；`BookingApplicationService` 作为唯一 `@Transactional` 边界；空库重建后全量测试 |
| Skill | `project-governance`、`git-pr`、`test-evidence` 应用；`contract-change` **不适用**：未改契约条文，只是实现既有契约行为 |
| 测试 | `mvn -B -Pdb test`（空库）→ 101 测试全绿，退出码 0；`BookingApplicationServiceIT` 12 用例；默认构建 14 测试；开发库 0 表；ArchUnit 8 规则全绿 |
| 结果 | 创建住宿订单的完整事务路径可用：价格服务端重算、幂等、原子创建、冲突整单回滚 |
| 风险 | ① 未接 HTTP 层（需 Spring Security，S4）；② 只覆盖住宿，套餐/滑雪/装备待 B 侧表；③ 订单号生成器为单进程方案 |

### 架构落地说明

严格按 `docs/ARCHITECTURE.md` §2.1 依赖图实现，`BookingApplicationService` 是**唯一开启跨模块事务的地方**（ADR-009）：

```
booking/application/BookingApplicationService  (@Transactional)
  ├─► catalog/application/CatalogQueryPort          ← ResourceSnapshot
  ├─► pricing/application/PricingQueryPort          ← PriceCalculator.Rule
  ├─► order/application/OrderWritePort              ← 写订单/明细/快照
  └─► lodging/application/LodgingReservationPort    ← 写占用
```

- 模块间只经 `application` 层端口协作，不跨模块访问 Mapper（R10）。
- `pricing/domain/PriceCalculator` 是纯领域类，不依赖 Spring/MyBatis/DB（R9）。
- 住宿冲突不继承 `BusinessException`，由 booking 编排层翻译为 `BOOKING_CONFLICT`，使 `lodging` 模块不依赖 `common.web` 协议层。

### 发现并修复的产品代码缺陷

`CreateLodgingOrderCommand.fingerprint()` 原先拼接原始字符串，两间房的指纹超出 `customer_order.create_request_fingerprint varchar(128)`，导致多间房下单失败（TE-20261127-11 首次失败）。改为 SHA-256 定长摘要，同时保留行序排序语义。这是**本轮自己引入并自己发现的缺陷**，不是既有代码问题。

### 关于未接 HTTP 层

契约要求「创建订单必须登录」，而 Spring Security/JWT 属 S4 范围（`ARCHITECTURE.md` 版本矩阵）。本轮**只实现并验证 application 层事务**，不暴露 `POST /orders` 端点，避免产生一个无认证的订单创建入口。这是有意的顺序选择，已在 TE-20261127-12 标注。

## OP-20261127-05：当前进度、缺陷与下一步审查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:18–22:35 +08:00（Asia/Taipei） |
| 分类 | REVIEW + TEST + GIT |
| 授权边界 | 只审查与记录；不改业务代码/契约语义，不提交、不推送、不合并、不修改 GitHub 设置 |
| 启动状态 | `feature/database-core` / `66910b1`；工作树干净；fetch 后相对 `origin/develop` ahead 10；本地 `develop` behind 12 |
| GitHub | PR #2/#3 均 OPEN、MERGEABLE、REVIEW_REQUIRED、0 review、无 CI checks；请求 `Janovo2117` |
| 审查结果 | 数据库/约束/并发基础较扎实，但契约未批准且实现未集成；登记 R6-01~R6-06，其中并发幂等、stayMode 校验、DAY_PASS 与默认测试隔离为优先修复项 |
| 测试 | 默认测试 FAIL（退出码1）；真实 PostgreSQL `-Pdb` 回归 101/101 PASS（退出码0），边界见 TE-20261127-13 |
| 文档同步 | 更新 DEVELOPMENT、CHANGELOG Known limitations、ISSUES、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志；TEAM_TASK_BOARD 无状态变化，未改；公共契约无语义变更 |
| 外部操作 | 仅 `git fetch` 与只读 `gh pr view/list/status`；未留言、未 Review、未改 PR、未合并 |
| 推荐顺序 | 先完成 PR #2 人工 Review → 修 R6-02~R6-05 并补测试 → 完成 BK-01 可用性与 BK-02 支付/取消/过期 → 接认证/HTTP → B 侧前端与 P0-B |

## OP-20261127-06：不等待 Review，先修创建订单核心缺陷

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:30–22:40 +08:00（Asia/Taipei） |
| 分类 | FIX + TEST + DB + GIT |
| 用户授权 | `Janovo2117` Review 预计1–2天；明确要求先完成不依赖 Review 的后续工作 |
| 范围 | 修 R6-02~R6-05 与请求内重复资源；不改公共契约、V1~V5、前端或B侧模块 |
| 实施 | advisory transaction lock 串行同用户同键；调用现有 stayMode 支持关系；按上海时区计算 OVERNIGHT/DAY_PASS；默认上下文测试改 `*IT`；补输入边界 |
| 测试 | 默认13/13、定向20/20、完整PostgreSQL 110/110，全部退出码0；TE-20261127-14 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、ISSUES、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志 |
| 契约/迁移 | 无契约语义变更；无 Flyway 变更；`contract-change` 不适用 |
| 剩余 | BK-01 可用性/维修/开放规则；BK-02 支付/取消/过期/状态历史；认证/HTTP；前端 |

## OP-20261127-07：BK-01 维修区间联动

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:40–22:47 +08:00（Asia/Taipei） |
| 分类 | FIX + TEST + DB + GIT |
| 用户授权 | Review 预计1–2天；继续完成不依赖 Review 的后续工作 |
| 范围 | 在既有 V2 维修模型上接入创建订单校验；不改公共契约、迁移、枚举、端点或前端 |
| 实施 | catalog 查询端口按资源和半开请求区间查询 `OPEN/IN_PROGRESS` 维修；booking 在任何订单写入前以 `RESOURCE_NOT_OPEN` 拒绝；完成/取消维修不阻断 |
| 测试 | 定向22/22、默认13/13、完整PostgreSQL 112/112，全部退出码0；TE-20261127-15 |
| 契约判断 | OVERNIGHT 开放规则校验粒度在候选契约中不明确，登记 R6-08，不擅自实现；`contract-change` 不适用 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、ISSUES、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志 |
| 剩余 | BK-01 独立可用性查询/开放规则；BK-02 支付/取消/过期/状态历史；认证/HTTP；前端 |

## OP-20261127-08：第三次独立审计任务 B/C 整改

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:04–23:16 +08:00（Asia/Taipei） |
| 分类 | FIX + TEST + DB + GIT |
| 输入 | 用户提供第三次独立只读审计报告，明确授权任务 B/C |
| 范围 | Jackson 3 JSON 序列化、真实资源名快照、两个新增 IT、Maven clean 与完整 PostgreSQL 回归；不改公共契约、Flyway、API或前端 |
| 实施 | 移除手写 JSON 转义；catalog 资源快照携带 `resource_unit.name`；booking 写真实名称；新增控制字符 JSONB 与资源名两项 IT |
| 单项测试 | 两个新增 IT 分别单独运行，均1/1 PASS、退出码0；含真实 LF 的规则描述成功写入并从 PostgreSQL JSONB 解码 |
| clean 回归 | `mvn clean` 退出码0；陈旧报告消失；clean 后 `-Pdb` 114/114 PASS、退出码0；TE-20261127-16 |
| Diff 门禁 | 检查完整代码/测试/文档 diff；`git diff --check` 无空白错误、退出码0，仅有既有 CRLF→LF 提示 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、ISSUES、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志 |
| 契约/迁移 | 只补齐已有 `resource_unit.name` → `item_name_snapshot` 数据链路；无 DB/API/ENUM/业务流程条文变化，`contract-change` 不适用 |
| 剩余 | R6-08 开放规则待 Review；BK-02 支付/取消/过期；认证/HTTP；前端 |

## OP-20261127-09：BK-02 模拟支付事务

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:17–23:30 +08:00（Asia/Taipei） |
| 分类 | FEAT + TEST + DB + GIT |
| 用户授权 | 明确要求继续后续任务；本轮选取可独立验证的模拟支付工作单元 |
| 范围 | payment application/persistence、order 支付端口、lodging 确认端口及真实 PostgreSQL IT；不改公共契约、Flyway、HTTP或认证 |
| 实施 | `FOR UPDATE` 锁定所有者订单；订单内串行支付；同键指纹校验；成功/失败记录；成功时同步订单、明细、住宿占用和三类历史 |
| 并发防线 | 应用订单行锁保证同订单串行；数据库 `uq_payment_succeeded_per_order` 保持最终兜底；并发不同键成功测试仅一条成功支付 |
| 测试 | 定向10/10、默认13/13、完整PostgreSQL 124/124，全部0失败/0跳过/退出码0；TE-20261127-17 |
| Diff 门禁 | 完整审查21个 staged 文件（含全部新文件）；`git diff --cached --check` 无空白错误、退出码0，仅有既有行尾提示 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志；无新增问题需写 ISSUES |
| 契约/迁移 | 实现既有支付契约和状态机；未改 DB/API/ENUM/流程条文，`contract-change` 不适用 |
| 剩余 | 取消/退款、过期释放、认证HTTP入口；开放规则 R6-08；前端 |

## OP-20261127-10：BK-02 整单取消与模拟退款

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:27–23:35 +08:00（Asia/Taipei） |
| 分类 | FEAT + TEST + DB + GIT |
| 用户授权 | 模拟支付提交后继续 BK-02 后续工作 |
| 范围 | booking 取消编排、order 取消端口、payment 退款、lodging 整单释放及 PostgreSQL IT；不改公共契约、Flyway、HTTP或认证 |
| 实施 | 所有者订单 `FOR UPDATE`；仅 PENDING_PAYMENT/CONFIRMED 且无 IN_USE/FULFILLED 明细可取消；成功支付改 REFUNDED；状态与历史同事务同步 |
| 幂等 | 取消无业务请求体；订单 CANCELLED 终态直接返回重放结果，仍要求非空 Idempotency-Key；并发取消由订单行锁串行且只写一次历史 |
| 测试 | 定向16/16、默认13/13、完整PostgreSQL 130/130，全部0失败/0跳过/退出码0；TE-20261127-18 |
| Diff 门禁 | 完整审查20个 staged 文件（含全部新文件）；`git diff --cached --check` 无空白错误、退出码0，仅有既有行尾提示 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志；无新增问题需写 ISSUES |
| 契约/迁移 | 实现既有整单取消/退款状态机；未改 DB/API/ENUM/流程条文，`contract-change` 不适用 |
| 剩余 | 过期释放、认证HTTP入口；开放规则 R6-08；前端 |

## OP-20261127-11：BK-02 待支付订单过期扫描

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:33–23:40 +08:00（Asia/Taipei） |
| 分类 | FEAT + TEST + DB + GIT |
| 用户授权 | 明确要求继续完成未完成任务；Review 仍异步等待，不阻塞不改契约的后续实现 |
| 范围 | booking 过期编排、order 过期端口/持久化、Spring 定时启用及 PostgreSQL IT；不改公共契约、Flyway、HTTP或认证 |
| 实施 | 定时事务每批领取最多100条已到期 PENDING_PAYMENT 订单；`FOR UPDATE SKIP LOCKED` 支持多实例竞争；订单/明细转 EXPIRED、住宿转 RELEASED并写三类历史 |
| 测试隔离 | test profile 首次调度延迟1小时；IT 直接调用服务，验证到期、已确认过滤和重复扫描零写入 |
| 测试 | 定向19/19、默认13/13、完整PostgreSQL 133/133，全部0失败/0错误/0跳过/退出码0；TE-20261127-19 |
| Diff 门禁 | 完整审查14个 staged 文件（含4个新文件）；`git diff --cached --check` 无空白错误、退出码0 |
| 文档 | 更新 DEVELOPMENT、任务板、CHANGELOG、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志；无新增问题需写 ISSUES |
| 契约/迁移 | 实现既有 PENDING_PAYMENT→EXPIRED 与占用释放流程；未改 DB/API/ENUM/流程条文，`contract-change` 不适用 |
| 剩余 | 认证HTTP入口；开放规则 R6-08；独立可用性查询；前端 |

## OP-20261128-01：S4 JWT 安全边界与支付 HTTP

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-28 00:20–00:44 +08:00（Asia/Taipei） |
| 分类 | SECURITY + FEAT + TEST + INTEGRATION + GIT |
| 用户授权 | 同意按推荐继续认证基础与订单 HTTP；Review 仍异步等待 |
| 启动状态 | `feature/database-core` / `97818a0`，工作树干净；fetch 后远端功能分支一致，相对 `origin/develop` ahead16/behind0；PR #3 待 Review |
| 范围 | Security/JWT/CORS/统一401/403、访问令牌输出端口、模拟支付 Controller/DTO、真实 HTTP/PostgreSQL IT；不改公共契约、Flyway、注册/登录或不完整订单DTO |
| 实施 | 无状态 JWT Resource Server；HS256环境密钥、最长30分钟；CUSTOMER/ADMIN路径授权；JWT `sub` 唯一提供 userId；支付继续在 application 做对象所有权二次校验 |
| 测试 | 定向最终13/13、Web组合18/18、默认13/13、完整PostgreSQL146/146，最终均退出码0；首次缓存权限、DB沙箱权限、issuer格式及CORS Bean歧义失败均保留在 TE-20261128-01 |
| 提交门禁 | 完整审查25个暂存文件（918新增/12删除）；敏感信息扫描只命中环境变量、占位符及测试专用密钥；`git diff --cached --check` 无输出、退出码0 |
| 发现 | R6-11：认证与部分订单HTTP DTO不完整，停止对应实现并等待双方 Review；未修改候选契约 |
| 文档 | 更新 README、DEVELOPMENT、任务板、ARCHITECTURE版本矩阵、CHANGELOG、ISSUES、TEST_EVIDENCE、OPERATION_LOG、SKILL_COMPLIANCE、开发日志和 `.env.example` |
| 剩余 | R6-11 契约 Review 后实现登录/注册/当前用户/创建查询取消 HTTP；R6-08；独立可用性；前端 |
