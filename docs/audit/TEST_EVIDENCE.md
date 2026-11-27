# 测试与检查证据

## 证据状态

- `PASS`：实际执行、退出码0且覆盖目标。
- `FAIL`：实际执行失败或输出违反目标。
- `NOT RUN`：未执行并说明原因。
- `UNVERIFIED`：现有证据不足以覆盖目标。

## TE-20261125-01：开始前Git状态

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `288273692bdd48eb597b163f72c7c8e0af90ea9d` |
| 环境 | Windows PowerShell；工作目录 `D:/Will/SYSU/数据库系统原理/大作业` |
| 命令 | `git status --short --branch`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep` |
| 实际输出摘要 | 工作区干净；ahead/behind为 `0 0` |
| 退出码 | 0 |
| 结论 | PASS：仅证明开始时分支正确、工作区干净且跟踪分支无分歧 |

## TE-20261125-02：历史来源读取

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `288273692bdd` |
| 命令 | `git show 2882736^:docs/PHASE1_REVIEW.md`；`git show 2882736^:docs/PHASE2_DECISIONS.md`；`git diff 2882736^ 2882736 -- <contract files>` |
| 实际输出摘要 | 取回 `0cd6bae` 中18项原始提案、PROPOSED状态和集中待回复问题；确认 `2882736` 将其改写为确认结果并标记FROZEN v1.0 |
| 退出码 | 0 |
| 结论 | PASS：证明本轮历史恢复基于Git对象而非记忆；不证明业务决定已获双人批准 |

## 未运行测试

| 类别 | 状态 | 原因 |
|---|---|---|
| Spring Boot/JUnit | NOT RUN | 本轮禁止初始化或开发业务代码，仓库无后端工程 |
| React lint/build | NOT RUN | 本轮禁止初始化前端工程，仓库无前端工程 |
| PostgreSQL/Flyway | NOT RUN | 本轮禁止初始化数据库及创建迁移 |
| 并发/性能测试 | NOT RUN | 无可运行系统，不能产生真实业务结果 |

## TE-20261125-03：项目Skill结构验证

| 尝试 | 命令/环境 | 实际输出摘要 | 退出码 | 结论 |
|---|---|---|---|---|
| 1 | `python quick_validate.py <skill>` | WindowsApps占位解释器不可用，三个Skill均返回9009 | 9009（各调用） | FAIL；未执行到Skill内容验证 |
| 2 | `py quick_validate.py <skill>` | Python 3.14按GBK读取UTF-8中文，三个Skill均抛出 `UnicodeDecodeError` | 1（各调用） | FAIL；编码环境不兼容 |
| 3 | 当前进程设置 `PYTHONUTF8=1` 后执行 `py quick_validate.py <skill>` | contract-change、git-pr、test-evidence均输出 `Skill is valid!` | 0（各调用） | PASS；仅证明frontmatter、命名和脚手架结构通过官方快速校验，不证明规则设计正确 |

## TE-20261125-04：文档与契约静态检查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 16:29 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `288273692bdd`（未提交工作区） |
| 命令 | `git diff 0cd6bae --`历史文件；`rg`版本/契约术语；PowerShell Markdown内部链接检查；`rg`凭据特征；`git diff --check` |
| 实际输出摘要 | 历史文件相对0cd6bae只新增顶部历史说明和末尾确认引用；候选状态命中符合预期；八项术语在对应事实源/Issue中出现；内部链接OK；未发现GitHub token/私钥特征；diff check无错误，仅CRLF转换警告 |
| 退出码 | 0（组合检查命令） |
| 结论 | PASS：覆盖文档结构、链接、已登记术语和空白错误；不证明待决业务方案、运行时行为或独立审计结论 |

## TE-20261125-05：完整diff人工自检

| 字段 | 内容 |
|---|---|
| 命令 | 按文件组执行 `git diff --no-ext-diff -- <changed tracked files>`；新文件使用 `Get-Content -Raw` 完整读取 |
| 实际输出摘要 | 已检查状态文档、三份契约、业务流程、历史恢复、审计文件及三个Skill；发现并补充 `customer_order.create_request_fingerprint` 后继续检查 |
| 退出码 | 0 |
| 结论 | PASS：证明本轮自检覆盖全部变更文本；这是Codex自检，不是独立审计或双人Review |

## TE-20261125-06：主整改提交

| 字段 | 内容 |
|---|---|
| 命令 | `git commit -m "docs(audit): remediate R-3 and R-4 findings"`；`git rev-parse HEAD`；`git status --short --branch` |
| 实际输出摘要 | 创建提交 `0586c32`，26个文件、508行新增、51行删除；完整SHA为 `0586c32a457e8063534ecb753ba323cbe5b764a9`；提交后ahead 1且工作区干净 |
| 退出码 | 0 |
| 结论 | PASS：证明主整改已提交到授权feature分支；不代表已推送、已Review或已合并 |

## TE-20261125-07：推送和远端核对

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 16:32:13 +08:00 |
| 命令 | `git push origin feature/collaboration-prep`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep`；`git ls-remote --heads origin feature/collaboration-prep develop main` |
| 实际输出摘要 | 推送 `2882736..566e53b`；ahead/behind为 `0 0`；远端feature为 `566e53b84ff2a3d4223bb144013471f0bd25bbfd`；main/develop仍为 `9c9cd3c8bbc7391da9638140a47bd6d92175ef57` |
| 退出码 | 0 |
| 结论 | PASS：证明两项整改提交已推送且未修改main/develop；不代表PR已Review或合并 |

## TE-20261125-08：批准同步开始状态

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 17:05:49 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `131ec982be0af8f2afe8e272a66086505430e6a7` |
| 命令 | `git status --short --branch`；`git rev-parse HEAD`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep` |
| 实际输出摘要 | 开始工作区干净；跟踪授权feature分支；ahead/behind为 `0 0` |
| 退出码 | 0 |
| 结论 | PASS：仅证明本次同步开始时Git前置状态符合要求 |

## TE-20261125-09：批准决策文档静态检查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 17:08:46 +08:00 |
| 环境 | Windows PowerShell；工作目录 `D:/Will/SYSU/数据库系统原理/大作业`；未提交工作区 |
| 命令 | `rg`决策状态/跨契约术语；PowerShell Markdown内部链接检查；`rg`凭据特征；分组 `git diff --no-ext-diff`；`git diff --check` |
| 实际输出摘要 | D-001/B和D-002/A在DB/API/ENUM/需求/流程/治理文档中可追踪；内部链接输出 `Markdown internal links: OK`；凭据扫描输出 `no matches`；diff check无空白错误，仅显示预期LF→CRLF警告 |
| 退出码 | 0（各次可见重跑） |
| 结论 | PASS：证明文档链接、登记术语和diff格式通过静态检查；完整diff为Codex自检，不是独立审计或人工PR Review |

## TE-20261125-10：业务与运行时测试

| 类别 | 状态 | 原因 |
|---|---|---|
| D-001 DDL/发布/报价测试 | NOT RUN | 本轮禁止创建Flyway和业务代码，当前只有候选契约 |
| D-002调度/时钟/并发测试 | NOT RUN | 本轮禁止初始化Spring Boot或实现调度器 |
| PostgreSQL事务与库存测试 | NOT RUN | 无数据库工程或迁移，本次批准不等于运行时验证 |

## TE-20261125-11：批准同步提交与推送

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-25 +08:00 |
| 命令 | `git diff --cached --check`；`git commit -m "docs(contract): approve D-001 and D-002"`；`git push origin feature/collaboration-prep`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep`；`git ls-remote --heads origin feature/collaboration-prep develop main` |
| 实际输出摘要 | cached diff check无错误；创建 `df0e33cbc67bafcef546692334f6b47145f6d6a7`；推送 `131ec98..df0e33c`；ahead/behind `0 0`；远端feature指向df0e33c，main/develop仍指向9c9cd3c |
| 退出码 | 0 |
| 结论 | PASS：证明批准同步主提交已推送到授权feature分支且未推进main/develop；不证明PR已创建、Review或合并 |

## TE-20261126-01：开始前Git状态

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:42:13 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `08ff87f1fbb7b511628da5ec22ca26ec873ae6c6` |
| 环境 | Windows Git Bash；工作目录 `D:/Will/SYSU/数据库系统原理/大作业` |
| 命令 | `git branch --show-current`；`git rev-parse HEAD`；`git status --short --branch`；`git remote -v`；`git log -6 --decorate --oneline --all --graph`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep`；`git ls-remote --heads origin feature/collaboration-prep develop main` |
| 实际输出摘要 | 分支为 `feature/collaboration-prep`；HEAD `08ff87f`；工作区无变更；ahead/behind `0 0`；远端feature `08ff87f`，`develop`/`main` 同为 `9c9cd3c`；最近提交为 `08ff87f`、`df0e33c`、`131ec98`、`566e53b`、`0586c32`、`2882736` |
| 退出码 | 0（各命令） |
| 结论 | PASS：仅证明开始时分支正确、跟踪远端无分歧、工作区干净；不证明PR或Review状态 |

## TE-20261126-02：PR、Review、协作者与分支保护核查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:43–09:45 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `08ff87f` |
| 命令 | `gh auth status`；`gh pr list --state all`；`gh pr view 1 --json <metadata>`；`gh pr view 1 --json reviews,reviewRequests,reviewDecision`；`gh api repos/kamiweiwei12/-/pulls/1/reviews`；`gh api repos/kamiweiwei12/-/issues/1/timeline`；`gh api repos/kamiweiwei12/-/collaborators`；`gh api repos/kamiweiwei12/-/invitations`；`gh api repos/kamiweiwei12/-/branches`；`gh pr checks 1` |
| 实际输出摘要 | 登录账号 `kamiweiwei12`，scope `gist,read:org,repo`。PR #1 `OPEN`，作者 `kamiweiwei12`，创建于 `2026-11-25T08:47:47Z`，原 `baseRefName=main`，`headRefOid=08ff87f`，28文件+985/−61，`body=""`，`isDraft=false`，`mergeable=UNKNOWN`。`reviews=[]`、`reviewRequests=[]`、`reviewDecision=""`；`pulls/1/reviews` 返回 `[]`；timeline 仅8条 `committed`，无 `review_requested`/`reviewed`/`commented`。协作者：`kamiweiwei12`(admin)、`Janovo2117`(write)；`invitations=[]`。三分支 `protected:false`。`gh pr checks` 输出 `no checks reported on the 'feature/collaboration-prep' branch` |
| 退出码 | 0（各命令）；`gh pr checks` 为1（无检查项，非CI失败） |
| 结论 | PASS：证明PR存在、原目标分支为`main`、无任何Review与CI；`Janovo2117` 具备write权限但无Review活动。**不证明**双人Review已完成——“Review完成”结论为 `UNVERIFIED` |

## TE-20261126-03：纠正PR目标分支

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:46 +08:00 |
| 命令 | `gh pr edit 1 --repo kamiweiwei12/- --base develop`；随后 `gh pr view 1 --json baseRefName,headRefName,state,headRefOid,baseRefOid,changedFiles,additions,deletions,reviews,reviewDecision` |
| 实际输出摘要 | 命令输出PR链接，退出码0；复核结果 `baseRefName=develop`、`headRefName=feature/collaboration-prep`、`state=OPEN`、`baseRefOid=9c9cd3c`、`headRefOid=08ff87f`、28文件+985/−61、`reviews=[]`、`reviewDecision=""` |
| 退出码 | 0 |
| 结论 | PASS：证明PR目标分支已由`main`纠正为`develop`，未新增提交、未改写历史、未合并、未代替他人Review |

## TE-20261126-04：文档静态检查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:47 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `08ff87f`（未提交工作区） |
| 命令 | `rg` 交叉检索 `occupancy_status`、`resource_type_id IS NULL`、`v1.0-rc`；Markdown内部链接检查；`git diff --check`；按文件分组 `git diff` |
| 实际输出摘要 | `occupancy_status` 在DATABASE_CONTRACT出现1处、ENUMS无对应枚举，证实R5-05；`price_rule` 定义中 `resource_type_id FK NULL` 与 `stay_mode NULL` 并存且无组合CHECK，证实R5-04；`v1.0-rc` 状态在冻结记录/三份契约/README一致；内部链接通过；`git diff --check` 无空白错误 |
| 退出码 | 0 |
| 结论 | PASS：证明本轮文档编辑的链接与空白格式正确，且R5-04/R5-05为文档可见的真实缺口（尚待Reviewer判定，未擅自修改契约条文） |

## TE-20261126-05：业务与运行时测试

| 类别 | 状态 | 原因 |
|---|---|---|
| Spring Boot/JUnit | NOT RUN | 仓库无后端工程，本轮未获授权初始化 |
| React lint/build | NOT RUN | 仓库无前端工程，本轮未获授权初始化 |
| PostgreSQL/Flyway | NOT RUN | 未获授权创建迁移或启动数据库 |
| D-001规则匹配/冲突测试 | NOT RUN | 仅候选契约，无实现 |
| D-002调度/时钟/并发测试 | NOT RUN | 无调度器实现 |
| 并发/性能测试 | NOT RUN | 无可运行系统 |

## TE-20261126-06：提交与推送

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 09:49 +08:00 |
| 命令 | `git add <6 files>`；`git diff --cached --check`；`git commit -m "docs(audit): record PR inspection and correct target branch"`；`git push origin feature/collaboration-prep`；`git rev-list --left-right --count HEAD...origin/feature/collaboration-prep`；`git ls-remote --heads origin feature/collaboration-prep develop main` |
| 实际输出摘要 | cached diff check退出码0；创建提交 `16e194e9daa1e78d45baa676518f0b0666a2fc45`（6文件，+134/−1）；推送 `08ff87f..16e194e`；ahead/behind `0 0`；远端feature为 `16e194e`，`develop`/`main` 仍为 `9c9cd3c` |
| 退出码 | 0（各命令） |
| 结论 | PASS：证明审计记录与目标分支纠正已推送到授权feature分支，且未推进main/develop；不证明PR已Review或合并 |

## TE-20261126-07：契约术语交叉检索与链接检查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 10:20 +08:00 |
| 分支/HEAD | `feature/collaboration-prep` / `f3612db`（未提交工作区） |
| 命令 | `grep -rn "idempotency_key" docs/`；`grep -n "Occupan\|occupan" docs/ENUMS.md`；`grep -n "PENDING_PAYMENT" docs/DATABASE_CONTRACT.md`；Python 脚本检查 Markdown 内部链接；`git diff --check` |
| 实际输出摘要 | `idempotency_key` 命中 4 个文件：DATABASE_CONTRACT×9、DATABASE_DESIGN×3（均为复合候选码）、DECISION_LOG×1、ISSUES×1，无“全局唯一”表述残留；`ENUMS.md` 第20行出现 `OccupancyStatus`、第54行起三条说明；`DATABASE_CONTRACT` 中 `PENDING_PAYMENT` 仅剩第129行解释性表述，排斥谓词已为 `HELD`；链接检查输出 `broken: none`；`git diff --check` 无空白错误 |
| 退出码 | 0（各命令）；链接脚本 0 |
| 结论 | PASS：证明6处契约编辑的术语、链接与空白格式一致；不证明运行时DDL正确性（无数据库，NOT RUN） |

## TE-20261126-08：项目 Skill 结构校验（含一次失败）

| 尝试 | 命令/环境 | 实际输出摘要 | 退出码 | 结论 |
|---|---|---|---|---|
| 1 | `PYTHONUTF8=1 py quick_validate.py .agents/skills/contract-change`（`py` = Python 3.14.3） | `ModuleNotFoundError: No module named 'yaml'` | 1（三个Skill均同） | FAIL：解释器缺少 PyYAML，未执行到结构校验 |
| 2 | `py -m pip install pyyaml` | 提示 `Requirement already satisfied ... pythoncore-3.14-64`，说明存在另一个 3.14 安装 | 0 | 仅诊断，非校验结果 |
| 3 | 改用 `C:/Users/willz/AppData/Local/Python/pythoncore-3.14-64/python.exe` 执行同一脚本 | `contract-change`、`git-pr`、`test-evidence` 均输出 `Skill is valid!` | 0（各） | PASS：仅证明frontmatter、命名与脚手架结构通过官方快速校验，不证明规则设计正确 |

## TE-20261126-09：业务与运行时测试

| 类别 | 状态 | 原因 |
|---|---|---|
| PostgreSQL 排斥约束 DDL 验证 | NOT RUN | 未获授权创建 Flyway 或启动数据库 |
| 核销并发双写（部分唯一索引） | NOT RUN | 无实现，本轮仅改候选契约 |
| 占用状态流转测试 | NOT RUN | 无实现 |
| Spring Boot/Java 编译与 JUnit | NOT RUN | 仓库无后端工程 |
| React lint/build | NOT RUN | 仓库无前端工程 |

## TE-20261126-10：S1 工程骨架真实构建（含一次失败后修复）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 22:21–22:22 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `3404221` |
| 环境 | Windows Git Bash；JDK 21.0.12；Maven Wrapper 3.9.16（`distributionType=only-script`） |
| 命令 | `./mvnw -B test`（backend 目录） |
| 第一次输出摘要 | Spring Boot 4.1.3 上下文加载成功（`Started SnowfieldApplicationTests`）；`SnowfieldApplicationTests`/`ApiResponseTest`/`GlobalExceptionHandlerTest` 通过；`ArchitectureTest` 4 条分层规则（R5/R7/R8/R9）因 S1 无 `domain/api/infrastructure.persistence` 包触发 `failOnEmptyShould` 报错 |
| 第一次退出码 | 1（surefire 测试失败，非编译失败） |
| 修复 | 对 4 条分层规则加 `.allowEmptyShould(true)`（S1 尚无可匹配类；业务模块创建后自动成为真约束） |
| 第二次输出摘要 | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 8.629 s` |
| 第二次退出码 | 0 |
| 结论 | PASS：证明 Spring Boot 4.1.3 + webmvc + validation + ArchUnit 在 Java 21 下真实编译与测试通过；不代表 MyBatis/Flyway/PostgreSQL 已接入（见 TE-20261126-11） |

## TE-20261126-11：尚未运行

| 类别 | 状态 | 原因 |
|---|---|---|
| MyBatis 4.1.0 集成 | NOT RUN | S3 才接入，尚未加入 pom |
| Flyway/PostgreSQL 迁移 | NOT RUN | 契约冻结（PR #2）未完成，S3 才建迁移 |
| Spring Security/JWT | NOT RUN | S4 才接入 |
| 并发/性能测试 | NOT RUN | 无可运行业务 |

## TE-20261126-12：治理 Skill 建立（文档任务）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 22:5x +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `2bce281` |
| 命令 | `git diff --check` |
| 输出摘要 | 无空白/冲突错误输出 |
| 退出码 | 0 |
| 结论 | PASS：本轮仅新增/修改文档与 Skill，无业务、数据库、并发测试需求（NOT NEEDED） |

## TE-20261126-13：S2 依赖解析（MyBatis 4.1.0 / Flyway / PostgreSQL 驱动）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:09–23:10 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `eb67a1d` |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16（Wrapper 缓存）；Maven Central 联网 |
| 命令 | `<maven>/mvn.cmd -B dependency:tree`（backend 目录） |
| 输出摘要 | `mybatis-spring-boot-starter:4.1.0`、`mybatis:3.5.19`、`mybatis-spring:4.1.0`、`postgresql:42.7.14`、`spring-boot-starter-flyway:4.1.3`、`flyway-core:12.4.0`、`flyway-database-postgresql:12.4.0`；`BUILD SUCCESS` |
| 退出码 | 0 |
| 结论 | PASS：依赖可从 Maven Central 真实解析，版本与 `docs/ARCHITECTURE.md` §7 版本矩阵一致。仅证明「可解析」，不证明运行时兼容，后者见 TE-20261126-16 |

## TE-20261126-14：首次运行数据库集成测试失败（保留失败记录）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:11 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` |
| 命令 | `mvn -B -Pdb verify` |
| 失败摘要 | `FlywaySqlUnableToConnectToDbException` → `PSQLException`，`SQL State: 28P01`（password authentication failed for user "snowfield"）；`AppUserMapperIT` 6 errors、`FlywayMigrationIT` 9 errors |
| 退出码 | 非 0 |
| 根因（已定位） | 本机存在原生 PostgreSQL 18 服务（`postgresql-x64-18`，`D:\PostgreSQL`，PID 11152）监听 `0.0.0.0:5432`，与 Docker 容器端口映射冲突。Java 的 JDBC 连接实际到达本机 PG 18 实例而非容器，故密码必然不匹配。另发现 `localhost` 在 Windows 先解析为 `::1`，会命中 `pg_hba` 的 `scram-sha-256` 规则而非 `127.0.0.1` 的 `trust` 规则，加剧不一致 |
| 处置 | 将容器端口由 5432 改为 5433（`${POSTGRES_PORT:-5433}`），JDBC URL 显式使用 `127.0.0.1:5433`；经原生 JDBC 探针确认两库均可连接。未修改、未停止本机原生 PostgreSQL 18 服务 |
| 结论 | FAIL（保留），复测见 TE-20261126-16 |

## TE-20261126-15：真实 PostgreSQL 连通性探针（根因确认）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:21 +08:00（Asia/Shanghai） |
| 命令 | `java -cp postgresql-42.7.14.jar T3.java`（JDK 21 单文件源码模式） |
| 输出摘要 | `OK jdbc:postgresql://127.0.0.1:5433/snowfield_test db=snowfield_test`；`OK jdbc:postgresql://127.0.0.1:5433/snowfield db=snowfield` |
| 退出码 | 0 |
| 结论 | PASS：5433 端口下两个库均以 `snowfield` 账号 + `.env.example` 占位密码连接成功，证明 TE-20261126-14 的 28P01 由端口冲突而非代码或配置引起 |

## TE-20261126-16：S2 数据库集成测试复测（Flyway + MyBatis + 约束；含默认构建）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:22–23:24 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；Docker 29.6.1；容器 `snowfield-db`（`postgres:16-alpine`，PostgreSQL 16.16，healthy，映射 `0.0.0.0:5433->5432`）；测试库 `snowfield_test` 由 `scripts/test-db-init.ps1` 创建 |
| 命令 A | `mvn -B -Pdb test`（`db` profile 才包含 `*IT`） |
| 命令 A 输出摘要 | `Migrating schema "public" to version "1 - baseline"`；`Successfully applied 1 migration to schema "public", now at version v1 (execution time 00:00.088s)`；`ArchitectureTest` 8、`ApiResponseTest` 2、`GlobalExceptionHandlerTest` 3、`AppUserMapperIT` 6、`FlywayMigrationIT` 9；合计 `Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 命令 A 退出码 | 0 |
| 命令 B | `mvn -B test`（默认构建，不连数据库） |
| 命令 B 输出摘要 | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 命令 B 退出码 | 0 |
| 隔离性核对 | `snowfield`（开发库）`public` schema 表数 = `0`；`snowfield_test` 的 `flyway_schema_history` = `1|baseline|t` |
| 结论 | PASS：证明 MyBatis 4.1.0 + Boot 4.1.3 + PostgreSQL 16 + Flyway 12.4.0 在 Java 21 下真实可用；V1 迁移在空测试库成功执行；唯一约束 `uq_app_user_email`、检查约束 `ck_app_user_email_lower`/`ck_app_user_status`、外键 `fk_user_role_user`、复合主键 `pk_user_role` 均能拒绝非法数据；失败语句不残留行；`#{}` 参数绑定生效。测试库与开发库隔离成立 |
| 验证边界 | 本证据只覆盖 `app_user`/`role`/`user_role` 三张表与连通链路；营地/资源/价格/订单/库存等契约表 **NOT RUN**；并发、幂等、性能测试 **NOT RUN**（S3/S4 范围） |

## TE-20261126-17：本任务未运行的测试

| 类别 | 状态 | 原因 |
|---|---|---|
| 并发预订 / 超卖 | NOT RUN | 无业务表与业务用例 |
| 订单幂等 / 支付 | NOT RUN | S4 范围 |
| Spring Security / JWT | NOT RUN | S4 范围 |
| 前端 lint/build | NOT RUN | 本次未触碰 `frontend/` |
| JMeter / EXPLAIN 性能 | NOT RUN | 无真实业务数据 |

## TE-20261126-18：springdoc 版本兼容性核查（拒绝不匹配版本）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:36–23:38 +08:00（Asia/Shanghai） |
| 目的 | 为 INF-04 引入 OpenAPI 文档，先核实版本与 Boot 4.1.3 同代 |
| 方法 | 读取 Maven Central 上各版本 `springdoc-openapi-<v>.pom` 的 `parent.version` |
| 输出摘要 | `3.0.3 -> parent Boot 4.0.5`；`3.1.1 -> parent Boot 4.1.0`；`3.2.0 -> parent Boot 4.1.2`；`3.2.1 -> parent Boot 4.2.0` |
| 结论 | PASS（核查）：`3.2.0` 与本项目 Boot 4.1.3 同代，采用；`3.2.1` 已跳到 Boot 4.2.0，与 ADR-012「不用 Boot 4.2」冲突，**拒绝采用** |
| 文档冲突登记 | `docs/ARCHITECTURE.md` 版本矩阵原写「springdoc 3.2.x」，未区分 3.2.0/3.2.1 的 Boot 代际差异，已在本轮修正为 3.2.0 并注明升级前须复核父 POM |

## TE-20261126-19：真实 HTTP 契约测试（INF-04）

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-26 23:40–23:44 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `cf6a77b` + 本轮工作区改动 |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；`@SpringBootTest(webEnvironment=RANDOM_PORT)` 真实启动内嵌服务器；Jackson 3（`tools.jackson`） |
| 命令 | `mvn -B -Pdb test`（含 `WebContractIT`） |
| 首次失败（保留） | `No qualifying bean of type 'org.springframework.web.client.RestClient$Builder'` —— Boot 4 不再自动注册该 Builder，5 个用例全部 error，退出码 1 |
| 修复 | 改用 `RestClient.create(baseUrl)`；JSON 解析由 `com.fasterxml.jackson` 切换为 Boot 4 的 `tools.jackson.databind.json.JsonMapper` |
| 复测输出摘要 | `WebContractIT`：`Tests run: 5, Failures: 0, Errors: 0`；全量 `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 覆盖内容 | ① `/api/v1/health` 返回契约 §1 成功信封（`success/data/traceId/timestamp`，timestamp 匹配 `+08:00`）；② 每次请求 traceId 唯一；③ 未知路径返回 404 且 `error.code=RESOURCE_NOT_FOUND`、`fieldErrors` 为数组、`details` 为对象；④ 错误响应体不泄漏 `java.`/`org.springframework`/`edu.sysu`/`Exception`/`.java`/`SQL`；⑤ `/v3/api-docs` 返回 OpenAPI 3 文档且包含 `/api/v1/health` |
| 退出码 | 0（`-Pdb` 与默认构建均为 0） |
| 结论 | PASS：统一响应/异常/健康/OpenAPI 在真实 HTTP 线上格式与契约一致；安全要求「不泄漏内部细节」有可执行证据 |
| 验证边界 | 仅覆盖平台级接口；业务端点（认证/订单/库存）尚未实现，**NOT RUN** |

## TE-20261126-20：本轮未运行

| 类别 | 状态 | 原因 |
|---|---|---|
| 业务端点 HTTP 测试 | NOT RUN | 认证/订单/库存接口未实现（S4/S5） |
| Spring Security / JWT | NOT RUN | S4 范围 |
| CORS 策略验证 | NOT RUN | 需前端源配置，S5 联调时验证 |
| 前端 lint/build | NOT RUN | 未触碰 `frontend/` |

## TE-20261127-01：DB-02/DB-03 迁移（V2/V3）在干净库全量执行

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 00:04–00:10 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `719e92e` + 本轮工作区改动 |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；`snowfield-db`（PostgreSQL 16.16, 5433）；测试库 `snowfield_test` **先 DROP 再 CREATE**，确保为空库 |
| 命令 | `docker exec ... psql -c 'DROP DATABASE snowfield_test'` → `CREATE DATABASE`；`mvn -B -Pdb test` |
| 输出摘要 | `Migrating schema "public" to version "1 - baseline"`、`"2 - campground and resource"`、`"3 - price rules and calendar"`；`Successfully applied 3 migrations to schema "public", now at version v3 (execution time 00:00.351s)`；合计 `Tests run: 54, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 退出码 | 0 |
| 结论 | PASS：满足 AGENTS.md §4「迁移必须在干净数据库从 V1 全量成功执行」；V1→V2→V3 链式迁移无顺序依赖问题 |

## TE-20261127-02：DB-02/DB-03 约束反例测试

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 00:06–00:09 +08:00（Asia/Shanghai） |
| 命令 | `mvn -B -Pdb test -Dtest=ResourceAndPriceConstraintIT` |
| 输出摘要 | `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 首次失败（保留） | `COMPILATION ERROR ... ResourceAndPriceConstraintIT.java:[247,16] 未报告的异常错误java.sql.SQLException` —— `holidayIsUniquePerCampgroundAndDate` 缺少 `throws SQLException`，退出码 1 |
| 修复 | 为该测试方法补 `throws SQLException` |
| 覆盖内容 | 营地：编码唯一、币种必须 CNY、时区必须 Asia/Shanghai；季节：`end<start` 被拒、同日边界合法；资源：`kind` 枚举、`base_capacity>0`、类型内编码唯一、`stay_mode` 枚举；开放规则：星期 1..7、`close_time>open_time`；维修：空区间被拒、合法区间通过；价格：`operation` 枚举、`value>=0`、`stay_mode` 枚举、**D-001 方案B 的 `resource_type_id=NULL` 允许**、`price_rule_weekday` 级联删除、节假日按营地+日期唯一 |
| 退出码 | 0 |
| 结论 | PASS：上述约束均由数据库真实拒绝/接受，证据来自反例而非代码阅读 |

## TE-20261127-03：DB-02/DB-03 未覆盖范围

| 类别 | 状态 | 原因 |
|---|---|---|
| 住宿时间排斥约束（`EXCLUDE USING gist`） | NOT RUN | 依赖 `lodging_reservation`（订单域 V4），S3 尚未建表 |
| 住宿并发预订/超卖 | NOT RUN | 同上 |
| 价格规则冲突检测（`PRICE_RULE_CONFLICT`） | NOT RUN | 属应用层报价逻辑（BK-01），尚未实现 |
| `R5-04` 双空语义裁定 | OPEN | 契约未裁定，迁移刻意不加 CHECK，测试仅固定当前行为 |
| 开发库 `snowfield` 迁移 | NOT RUN | 刻意不迁移开发库，只在隔离测试库验证 |

## TE-20261127-04：DB-04 订单/支付迁移（V4）在干净库全量执行

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 10:04–10:08 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `f7c3ae3` + 本轮工作区改动 |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；`snowfield-db`（PostgreSQL 16.16, 5433，Up 11 hours healthy）；测试库**先 DROP 再 CREATE** |
| 命令 | `DROP DATABASE snowfield_test` → `CREATE DATABASE`；`mvn -B -Pdb test`；`mvn -B test` |
| 输出摘要 | `Migrating schema "public" to version "1..4"`；`Successfully applied 4 migrations to schema "public", now at version v4 (execution time 00:00.551s)`；合计 `Tests run: 74, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 默认构建 | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 隔离性 | 开发库 `snowfield` 表数 = `0`；测试库 `flyway_schema_history` 中 `1/2/3/4` 均 `success = true` |
| 退出码 | 0（两条命令均为 0） |
| 结论 | PASS：V1→V2→V3→V4 链式迁移在空库全量成功，未污染开发库 |

## TE-20261127-05：订单/支付/套餐/快照约束测试

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 10:06–10:08 +08:00（Asia/Shanghai） |
| 命令 | `mvn -B -Pdb test -Dtest=OrderAndPaymentConstraintIT` |
| 首次失败（保留） | `Tests run: 20, Failures: 1` —— `orderRejectsUnknownStatus` 断言「预期抛异常但未抛」，退出码 1 |
| 根因 | 测试自身缺陷：辅助方法 `insertOrder(orderNo, idemKey, fingerprint)` 的第三个参数是**指纹**而非状态，导致测试实际插入了合法订单。**非产品代码缺陷** |
| 修复 | 改为直接构造带 `status='PAID'` 的 INSERT 语句 |
| 复测输出摘要 | `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 覆盖内容 | 订单：`order_no` 唯一、**创建幂等键作用域 `(user_id,key)`**（同用户复键冲突、跨用户可复用）、状态枚举、总额非负、币种必须 CNY；明细：`item_type` 五类枚举全接受、非法类型被拒、`quantity>0`、订单删除级联明细；支付：**幂等键作用域 `(order_id,key)`**（跨订单可复用、同订单复键冲突）、**`uq_payment_succeeded_per_order` 部分唯一索引保证一单一成功**、多次失败可重试、`method` 恒为 `SIMULATED`（拒真实支付）、`SUCCEEDED` 必须有 `paid_at`；套餐：优惠不得超过小计、`DISCOUNT_RATE` 不得超过 1；快照：被快照引用的价格规则不可删除（保证可追溯性） |
| 退出码 | 0 |
| 结论 | PASS：契约中「数据库负责不可绕过的完整性」的幂等作用域与单一成功支付两条硬约束均有反例证据 |
| 验证边界 | 见 TE-20261127-06 |

## TE-20261127-06：DB-04 本轮刻意未覆盖的范围

| 类别 | 状态 | 原因 |
|---|---|---|
| `lodging_reservation` 住宿排斥约束 | NOT RUN | 按用户决定，需与 BK-03 并发测试（100 并发抢 1 资源）一起验证，单独建表会造成未验证的假象 |
| `package_component` | NOT RUN | 外键指向 `ski_product`/`lift`/`equipment_model`，属 B 侧模块（SKI-01/LIFT-01/EQ-01）尚未建立 |
| 订单并发/幂等运行时测试 | NOT RUN | 无业务用例，待 BK-02 |
| 订单金额汇总一致性 | NOT RUN | 契约要求「服务层/过程校验」，应用层未实现 |

### 关于「订单总额 = 明细小计 − 优惠」的数据库层处理

契约原文为「订单总额为全部明细小计减去 `order_package.discount_amount` 之和且不得为负」，并要求「住宿逐日合计以 `price_snapshot_line` 汇总并在**服务层/过程**校验」。因此 V4 **未**在数据库用触发器强制跨行汇总（跨表聚合触发器会与 ADR-009 的事务编排设计冲突），只落地了可独立验证的局部不变量：`total_amount >= 0`、`quantity > 0`、`subtotal >= 0`、`discount_amount <= subtotal_before_discount`。跨行汇总一致性属 BK-02 的应用层职责，尚未实现也未验证。

## TE-20261127-07：BK-03 住宿占用迁移（V5）在干净库全量执行

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 11:45–11:56 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `3540d59` + 本轮工作区改动 |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；`snowfield-db`（PostgreSQL 16.16, 5433，Up 12 hours healthy）；测试库**先 DROP 再 CREATE** |
| 命令 | `DROP DATABASE snowfield_test` → `CREATE DATABASE`；`mvn -B -Pdb test`；`mvn -B test` |
| 输出摘要 | `Migrating schema "public" to version "1..5"`；`Successfully applied 5 migrations to schema "public", now at version v5 (execution time 00:00.814s)`；合计 `Tests run: 89, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 默认构建 | `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 隔离性 | 开发库 `snowfield` 表数 = `0`；测试库 `flyway_schema_history` 中 `1..5` 均 `success = true` |
| 退出码 | 0 |
| 结论 | PASS：V1→V5 链式迁移在空库全量成功，开发库未被污染 |

## TE-20261127-08：住宿排斥约束语义测试

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 11:46–11:47 +08:00（Asia/Shanghai） |
| 命令 | `mvn -B -Pdb test -Dtest=LodgingExclusionConstraintIT` |
| 首次失败（保留） | `Tests run: 11, Failures: 1` —— `emptyPeriodIsRejected` 期望错误消息含 `ck_lodging_reservation_period_nonempty`，实际 PostgreSQL 先触发 `ck_lodging_reservation_period_bounded`（`empty` 区间的 `lower()` 为 NULL）。**约束行为正确，是测试断言绑定了错误的约束名** |
| 修复 | 改为断言 `ck_lodging_reservation_period_` 前缀（不绑定触发顺序），并新增 `unboundedPeriodIsRejected` 单独验证无界区间 |
| 复测输出摘要 | `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS` |
| 覆盖内容 | 重叠占用被拒（含完全包含）；**首尾相接的半开区间不算重叠**（`[start,end)` 语义）；**`RELEASED` 从排斥谓词排除**——释放后同区间可再次占用，避免已取消订单永久阻塞资源；不同资源单元同时段互不排斥；空区间被拒；无界区间被拒；非法占用状态被拒（`PENDING_PAYMENT` 不是 `OccupancyStatus`）；`order_item_id` 唯一；订单删除级联清除占用 |
| 退出码 | 0 |
| 结论 | PASS：契约「数据库负责不可绕过的完整性」在住宿占用上成立 |

## TE-20261127-09：BK-03 一百并发抢单一住宿资源

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 11:48–11:55 +08:00（Asia/Shanghai） |
| 验收标准 | `docs/testing/TEST_PLAN.md` §并发方案：「100 线程抢最后一个住宿资源：成功1，冲突99，无有效重叠」；任务板 BK-03：「100并发抢1资源仅1成功且无残留」 |
| 命令 | `mvn -B -Pdb test -Dtest=LodgingConcurrencyIT`（连跑 4 轮：1 次初测 + 3 次稳定性复测） |
| 测试设计 | **100 个线程各自从 Hikari 连接池取独立连接**并发 INSERT，用 `CountDownLatch` 同时放行；不使用单一测试事务模拟并发，确保真正触发数据库级竞争；100 个订单项预建，隔离出「占用插入」这一步 |
| 首次错误（保留） | `concurrentBookingsOnDisjointPeriodsAllSucceed` 报 `date/time field value out of range: "2027-03-33T12:00+08"` —— 测试用 `1 + i*3` 手算日期，超出 3 月长度。**测试自身缺陷，非产品代码问题** |
| 修复 | 改用 `LocalDate.of(2027,3,1).plusDays(i*3L)` 计算 |
| 输出摘要 | 4 轮均为 `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；单轮耗时 11.70 / 12.02 / 12.00 s |
| 断言结果 | 成功计数 = **1**；冲突计数 = **99**；其他失败 = **0**；数据库残留占用记录 = **1**；有效占用 = **1** |
| 冲突判定 | SQLState `23P01`（排斥约束冲突），另将 `40001`/`40P01`（串行化失败/死锁）也计入「未抢到」；任何其他 SQLState 都会使测试失败 |
| 退出码 | 0 |
| 结论 | PASS：BK-03 验收标准达成——100 并发抢 1 资源仅 1 成功、99 冲突、无残留 |
| 验证边界 | 验证的是**数据库排斥约束**这一层的正确性；应用层（HELD→CONFIRMED→RELEASED 状态流转、订单与占用同事务同步、可解释报价）尚未实现，属 BK-01/BK-02，**NOT RUN** |

## TE-20261127-10：BK-03 本轮未覆盖范围

| 类别 | 状态 | 原因 |
|---|---|---|
| 应用层订单创建/支付/取消事务 | NOT RUN | BK-02 未实现 |
| 订单与占用状态同事务同步 | NOT RUN | 契约要求在同一业务事务内同步（ADR-009），应用层未实现 |
| 可用性查询与维修/开放规则联动 | NOT RUN | BK-01 未实现 |
| 200+ 并发压力（JMeter） | NOT RUN | PERF-01，需真实业务接口 |
| 开发库迁移 | NOT RUN | 刻意只迁测试库 |

## TE-20261127-11：BK-02 第一阶段——创建住宿订单事务编排

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 12:23–12:46 +08:00（Asia/Shanghai） |
| 分支/HEAD | `feature/database-core` / `ee3606b` + 本轮工作区改动 |
| 环境 | Windows；JDK 21.0.12；Maven 3.9.16；`snowfield-db`（PostgreSQL 16.16, 5433）；测试库**先 DROP 再 CREATE** |
| 命令 | `mvn -B -Pdb test -Dtest=BookingApplicationServiceIT`；`mvn -B -Pdb test`；`mvn -B test` |
| 输出摘要 | `Successfully applied 5 migrations`；`BookingApplicationServiceIT`：`Tests run: 12, Failures: 0, Errors: 0`；全量 `Tests run: 101, Failures: 0, Errors: 0, Skipped: 0`；默认构建 `Tests run: 14`；`BUILD SUCCESS` |
| 退出码 | 0 |
| 隔离性 | 开发库 `snowfield` 表数 = `0` |
| 覆盖内容 | ① 单间住宿：建订单 + 1 明细 + 2 个服务日快照 + 1 条 HELD 占用，金额 `1280×2=2560.00`；② **多间房生成独立订单项与独立占用**（契约：每间房数量为 1）；③ **幂等重放**返回原订单不新建；④ **同键不同请求** → `IDEMPOTENCY_CONFLICT`；⑤ **重叠冲突整单回滚**——B 房已成功占用也要撤销，不留孤儿订单/明细/占用；⑥ `expectedTotal` 不匹配 → `PRICE_CHANGED` 且不建单；⑦ 同优先级两条规则 → `PRICE_RULE_CONFLICT`（不静默选胜者）；⑧ 高优先级规则胜出（`1280×1.5×2=3840.00`）；⑨ 起止倒置 → `INVALID_TIME_RANGE`；⑩ 不存在资源 → `RESOURCE_NOT_FOUND`；⑪ 超容量 → `VALIDATION_FAILED` |
| 结论 | PASS：契约「混合购物车在单个事务内原子创建」与「价格服务端权威」两条核心要求均有可执行证据 |

### 首次失败（保留）

| 项 | 内容 |
|---|---|
| 现象 | `Tests run: 12, Failures: 1, Errors: 1`：`overlappingReservationRollsBackWholeOrder`、`multipleRoomsProduceSeparateItemsAndReservations` |
| 报错 | `DataIntegrityViolationException: value too long for type character varying(128)`，位于 `OrderMapper.insertOrder` |
| 根因 | **产品代码缺陷**：`CreateLodgingOrderCommand.fingerprint()` 把各住宿行原始拼接成字符串，两间房的指纹超过 `customer_order.create_request_fingerprint varchar(128)` 上限。两个测试失败同一根因 |
| 修复 | 指纹改为 **SHA-256 十六进制摘要（定长 64 字符）**，并保持行序排序保证同一购物车不同顺序指纹一致 |
| 复测 | `Tests run: 12, Failures: 0, Errors: 0`，两个用例同时转绿 |

### 另一次编译期失败（保留）

`CreateLodgingOrderCommand` 的两个辅助重载在 `command(key, line, null)` 处产生歧义（`null` 同时匹配 `String` 与 `LodgingLine`），编译失败。已把双行版本重命名为 `twoLineCommand`。

## TE-20261127-12：BK-02 本轮未覆盖范围

| 类别 | 状态 | 原因 |
|---|---|---|
| 模拟支付 `POST /orders/{id}/payments/simulate` | NOT RUN | BK-02 第二阶段 |
| 取消订单 `POST /orders/{id}/cancel` | NOT RUN | BK-02 第二阶段 |
| 过期扫描（15 分钟释放） | NOT RUN | 需要定时任务，BK-02 第二阶段 |
| 订单/明细状态历史写入 | NOT RUN | 状态流转随支付与取消一并实现 |
| HTTP 层下单接口 `POST /orders` | NOT RUN | 需先接 Spring Security（创建订单要求认证，S4）；本轮只验证 application 层事务 |
| 套餐、滑雪、缆车、装备混购 | NOT RUN | 依赖 B 侧模块表与 `package_component` |
| 容量资源（ski_session/lift_slot 行锁） | NOT RUN | SKI-01/LIFT-01 |

## TE-20261127-13：当前进度复核与回归检查

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:22–22:25 +08:00（Asia/Taipei） |
| 分支/HEAD | `feature/database-core` / `66910b1cacce7c2722f4f534a0f893a61311c102`；工作树在审查前干净；相对 `origin/develop` ahead 10 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；`snowfield-db` PostgreSQL 16.16，容器 healthy，端口 5433 |
| 默认测试命令 | 在 `backend/` 执行 `.\mvnw.cmd -B test`，未注入数据库凭据 |
| 默认测试输出 | `Tests run: 14, Failures: 0, Errors: 1`；`SnowfieldApplicationTests.contextLoads` 因 Flyway 无法连接 PostgreSQL 失败；退出码 1 |
| 默认测试结论 | **FAIL**：证明当前默认测试实际依赖数据库，和 POM/开发文档“默认不连数据库”的声明不一致；沙箱网络限制是本次连接失败的直接触发条件，但静态配置同时证明该测试会启动数据源/Flyway |
| 数据库回归命令 | 从健康容器读取现有测试凭据但不输出密码，执行 `.\mvnw.cmd -B -Pdb test` |
| 数据库回归输出 | Flyway 校验 5 个迁移，测试库已在 v5、无需迁移；`Tests run: 101, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码 0 |
| 数据库回归结论 | **PASS（有限边界）**：当前现有测试库上的 101 项回归通过；因为数据库不是本轮 DROP/CREATE 的干净库，不能把本条证据表述为空库全量迁移验证 |
| GitHub | PR #2/#3 均 OPEN、MERGEABLE、`REVIEW_REQUIRED`、0 reviews、无 `statusCheckRollup`；均请求 `Janovo2117` Review |
| 静态审查 | 发现 R6-02~R6-05；DAY_PASS、资源类型/模式兼容、并发幂等均无现有测试覆盖 |

### 本轮未运行

| 类别 | 状态 | 原因 |
|---|---|---|
| 干净库 DROP/CREATE 后 V1~V5 全量迁移 | NOT RUN | 本轮是进度审查，避免破坏正在共享使用的测试库；沿用既有 TE-20261127-11 证据，本条不重复声称 |
| DAY_PASS 同日与跨时区边界 | NOT RUN | 现有测试未覆盖，已登记 R6-04 |
| 同一创建幂等键并发 | NOT RUN | 现有测试仅顺序重放，已登记 R6-02 |
| 前端/认证/支付/取消/过期/性能 | NOT RUN | 对应实现尚不存在或未进入本轮范围 |

## TE-20261127-14：创建订单并发幂等、住宿模式、DAY_PASS 与默认测试隔离

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:33–22:37 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `66910b1` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；真实 PostgreSQL 16.16 测试库，schema 已在 v5 |
| 默认命令 | 在 `backend/` 无数据库凭据执行 `.\mvnw.cmd -B test` |
| 默认结果 | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 定向命令 | `.\mvnw.cmd -B -Pdb test -Dtest=BookingApplicationServiceIT` |
| 定向结果 | `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 完整命令 | `.\mvnw.cmd -B -Pdb test` |
| 完整结果 | Flyway 成功校验5个迁移；`Tests run: 110, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 并发断言 | 同用户/同键/同请求8线程同时进入：仅1个结果 `replayed=false`、7个重放、数据库仅1张订单；同键异请求2线程：1成功、1稳定 `IDEMPOTENCY_CONFLICT` |
| 模式/时间断言 | 不支持 DAY_PASS 的资源在写订单前返回 `RESOURCE_NOT_OPEN`；支持后同一业务日生成1条价格快照；跨 `-08:00` 输入偏移量仍归一到 `Asia/Shanghai` 正确服务日 |
| 其他边界 | 同一请求非重叠重复资源被拒；空幂等键、人数0均返回 `VALIDATION_FAILED`，不依赖数据库异常 |
| 结论 | PASS：R6-02~R6-05、R6-07 的修复均有真实可执行证据；公共契约状态仍为候选，结论不等于人工Review |
| 验证边界 | 本轮未 DROP/CREATE 测试库，不新增/修改 Flyway；维修/开放规则、支付、取消、过期释放、HTTP/认证仍 NOT RUN |

## TE-20261127-15：创建订单维修区间联动

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 22:41–22:43 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `68efd91` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；真实 PostgreSQL 16.16 测试库，schema 已在 v5 |
| 定向命令 | `.\mvnw.cmd -B -Pdb test -Dtest=BookingApplicationServiceIT` |
| 定向结果 | `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 默认命令 | 在 `backend/` 无数据库凭据执行 `.\mvnw.cmd -B test` |
| 默认结果 | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 完整命令 | `.\mvnw.cmd -B -Pdb test` |
| 完整结果 | Flyway 成功校验5个迁移；`Tests run: 112, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；退出码0 |
| 维修断言 | 与请求 `[startAt,endAt)` 重叠的 `IN_PROGRESS` 维修返回 `RESOURCE_NOT_OPEN`，且订单/占用均无残留；重叠的 `COMPLETED`、`CANCELLED` 不阻断创建 |
| 结论 | PASS：创建住宿订单已接入有效维修区间阻断；未改变公共契约或 Flyway |
| 验证边界 | 本轮未重建测试库；独立可用性查询、开放规则、支付、取消、过期释放、HTTP/认证仍 NOT RUN；开放规则语义见 R6-08 |

## TE-20261127-16：Jackson 3 JSONB、真实资源名与 clean 全量回归

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:09–23:12 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `f28e8ad` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；Spring Boot 4.1.3；Jackson 3；真实 PostgreSQL 16.16 测试库，schema v5 |
| JSONB 单项命令 | `.\mvnw.cmd -B -Pdb '-Dtest=BookingApplicationServiceIT#jacksonSerializesControlCharactersAndJsonbStoresRuleExplanation' test` |
| JSONB 单项输出 | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 17.258 s`；`TARGET_EXIT_CODE=0` |
| JSONB 数据断言 | 规则描述为实际包含 LF 的 `跨行规则\n含"引号"、\反斜杠与中文`；订单成功创建；`price_explanation` 的 `jsonb_typeof` 为 `object`；用 `#>> '{explanation,0}'` 从 PostgreSQL 解码后包含原始规则描述（含真实换行） |
| 资源名单项命令 | `.\mvnw.cmd -B -Pdb '-Dtest=BookingApplicationServiceIT#itemNameSnapshotUsesActualResourceUnitName' test` |
| 资源名单项输出 | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 13.093 s`；`TARGET_EXIT_CODE=0` |
| 资源名数据断言 | 资源表 `resource_unit.name='甲'`；创建后查询 `order_item.item_name_snapshot`，实测等于 `甲`，不再生成“木屋资源 ID” |
| clean 命令 | `.\mvnw.cmd -B clean` |
| clean 完整结果 | Maven `clean:3.5.0:clean` 删除 `backend/target`；`BUILD SUCCESS`；`Total time: 0.758 s`；`CLEAN_EXIT_CODE=0`；随后核查 `DiagnosticProbeTest.txt` 与旧 `SnowfieldApplicationTests.txt` 均 `ABSENT` |
| clean 后全量命令 | `.\mvnw.cmd -B -Pdb test`（凭据从本机健康容器环境注入但不输出） |
| 编译/迁移输出 | clean 后重新编译30个主源码和13个测试源码；Flyway 连接 `snowfield_test` PostgreSQL 16.16，成功校验5个迁移，当前 schema v5、无需迁移 |
| 各测试类完整汇总 | `ArchitectureTest 8`；`BookingApplicationServiceIT 24`；`ApiResponseTest 2`；`GlobalExceptionHandlerTest 3`；`AppUserMapperIT 6`；`SnowfieldApplicationIT 1`；`FlywayMigrationIT 9`；`LodgingConcurrencyIT 3`；`LodgingExclusionConstraintIT 12`；`OrderAndPaymentConstraintIT 20`；`ResourceAndPriceConstraintIT 21`；`WebContractIT 5`；各类均0失败/0错误/0跳过 |
| Maven 最终原始汇总 | `Tests run: 114, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 01:10 min`；`Finished at: 2026-11-27T23:12:07+08:00`；`FULL_DB_EXIT_CODE=0` |
| 报告目录终检 | surefire 仅含本次12个当前测试类报告；`DIAGNOSTIC_PROBE_FINAL=ABSENT`，`OLD_CONTEXT_REPORT_FINAL=ABSENT` |
| 结论 | PASS：R6-09/R6-10 均有独立新增 IT 和 clean 后完整 PostgreSQL 回归证据；陈旧报告误导已消除 |
| 验证边界 | 测试库未 DROP/CREATE；本条证明 clean 构建与当前 schema 回归，不替代既有空库迁移证据；支付/取消/过期、HTTP认证、开放规则仍 NOT RUN |

## TE-20261127-17：模拟支付事务、幂等与并发成功唯一

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:23–23:25 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `24fdc40` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；真实 PostgreSQL 16.16 测试库，schema v5 |
| 定向命令 | `.\mvnw.cmd -B -Pdb '-Dtest=PaymentApplicationServiceIT' test` |
| 定向结果 | `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 17.300 s`；`TARGET_EXIT_CODE=0` |
| 成功路径 | 模拟成功写一条 `SUCCEEDED/SIMULATED` 支付，金额等于订单总额；订单 `PENDING_PAYMENT→CONFIRMED`、明细 `HELD→CONFIRMED`、住宿占用 `HELD→CONFIRMED`，三类历史各写一次，订单版本递增 |
| 失败/重试 | `FAILURE` 写 `FAILED` 且订单/明细/占用保持待支付；使用新键可再次尝试并成功 |
| 幂等 | 同键同结果返回原支付且不重复写；同键异结果返回 `IDEMPOTENCY_CONFLICT`；成功后使用新键也返回既有成功支付，不新增记录 |
| 权限/过期 | 非订单所有者得到 `ORDER_NOT_FOUND` 且无支付残留；已过期待支付订单得到 `ORDER_EXPIRED` 且无支付残留 |
| 并发 | 同一订单两个不同幂等键并发模拟成功：两个调用返回同一 paymentId，恰好一个 `replayed=false`、一个 `replayed=true`；数据库仅一条 `SUCCEEDED`，三类状态历史各一条 |
| 默认命令 | `.\mvnw.cmd -B test` |
| 默认结果 | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`DEFAULT_EXIT_CODE=0` |
| 全量命令 | `.\mvnw.cmd -B -Pdb test` |
| 全量结果 | Flyway 成功校验5个迁移；`Tests run: 124, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 53.785 s`；`FULL_DB_EXIT_CODE=0` |
| 结论 | PASS：模拟支付 application 事务、对象所有权、幂等、状态同步及并发成功唯一均有真实 PostgreSQL 证据 |
| 验证边界 | 未新增 HTTP 接口或认证；取消、退款、过期扫描仍 NOT RUN；测试库未 DROP/CREATE，本条不替代空库迁移证据 |

## TE-20261127-18：整单取消、模拟退款与并发幂等释放

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:29–23:31 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `6ca8c05` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；真实 PostgreSQL 16.16 测试库，schema v5 |
| 定向命令 | `.\mvnw.cmd -B -Pdb '-Dtest=PaymentApplicationServiceIT' test` |
| 定向结果 | `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 27.292 s`；`TARGET_EXIT_CODE=0` |
| 待支付取消 | 订单 `PENDING_PAYMENT→CANCELLED`、明细 `HELD→CANCELLED`、住宿 `HELD→RELEASED`；三类历史各一条；无退款 |
| 已支付取消 | 支付 `SUCCEEDED→REFUNDED`；订单/明细取消、住宿释放；支付与取消产生的两阶段历史均保留 |
| 幂等/并发 | 重复取消返回终态且不重复写历史；两个不同键并发取消恰好一个执行、一个重放，订单及住宿历史各仅一条 |
| 状态/权限 | 明细已 `IN_USE` 时返回 `INVALID_STATE_TRANSITION` 且成功支付、订单和占用保持原状；非所有者返回 `ORDER_NOT_FOUND` |
| 默认命令 | `.\mvnw.cmd -B test` |
| 默认结果 | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`DEFAULT_EXIT_CODE=0` |
| 全量命令 | `.\mvnw.cmd -B -Pdb test` |
| 全量结果 | Flyway 成功校验5个迁移；`Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 01:14 min`；`FULL_DB_EXIT_CODE=0` |
| 结论 | PASS：整单取消、模拟退款、所有权、非法状态回滚及并发一次释放均有真实 PostgreSQL 证据 |
| 验证边界 | 取消没有业务请求体，终态提供重放语义；当前仍要求 Idempotency-Key 非空但未新增契约外取消事件表；HTTP认证与过期扫描仍 NOT RUN |

## TE-20261127-19：待支付订单过期扫描与幂等释放

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-27 23:33–23:37 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `48a5aba` + 本轮工作区改动 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；真实 PostgreSQL 16.16 测试库，schema v5 |
| 定向命令 | `.\mvnw.cmd -B -Pdb '-Dtest=PaymentApplicationServiceIT' test` |
| 定向结果 | `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 26.995 s`；`TARGET_EXIT_CODE=0` |
| 到期断言 | 到期 `PENDING_PAYMENT` 订单转 `EXPIRED` 且版本递增；HELD 明细转 `EXPIRED`；住宿占用转 `RELEASED`；订单、明细、住宿三类历史各写一次 |
| 幂等/过滤 | 第二次扫描返回0且不重复历史；即使 `expires_at` 已过，已支付 `CONFIRMED` 订单也不被扫描 |
| 并发机制 | 每批最多100条，查询按订单号稳定排序并使用 `FOR UPDATE SKIP LOCKED`；与支付/取消的订单行锁互斥，多实例扫描跳过其他事务已领取行 |
| 调度隔离 | 生产默认首次延迟/固定延迟均为60秒；测试 profile 把首次延迟设为3600000ms，集成测试直接调用服务，避免后台线程污染共享测试数据 |
| 默认命令 | 在 `backend/` 执行 `.\mvnw.cmd -B test` |
| 默认结果 | `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 7.593 s`；`DEFAULT_EXIT_CODE=0` |
| 全量命令 | `.\mvnw.cmd -B -Pdb test` |
| 全量结果 | Flyway 成功校验5个迁移；`Tests run: 133, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 01:19 min`；`Finished at: 2026-11-27T23:36:56+08:00`；`FULL_DB_EXIT_CODE=0` |
| 结论 | PASS：待支付订单过期、状态历史、住宿释放、重复扫描幂等及非待支付过滤均有真实 PostgreSQL 证据 |
| 验证边界 | 未实际等待墙钟触发调度，测试直接调用同一事务方法；未做多 JVM 实例竞争实测；测试库未 DROP/CREATE；HTTP/认证仍 NOT RUN |

## TE-20261128-01：Spring Security/JWT 与模拟支付真实 HTTP

| 字段 | 内容 |
|---|---|
| 日期 | 2026-11-28 00:27–00:43 +08:00（Asia/Taipei） |
| 分支/基线 | `feature/database-core` / `97818a0` + 本轮工作区改动；fetch 后与 `origin/feature/database-core` 一致、相对 `origin/develop` ahead16/behind0 |
| 环境 | Windows；Java 21.0.12；Maven Wrapper 3.9.16；Spring Boot 4.1.3；Spring Security 7.1.3；真实 PostgreSQL 16.16 测试库，schema v5 |
| 首次默认命令/失败 | `.\mvnw.cmd -B test` 在沙箱内首次执行；Maven 无权创建用户 `.m2` 的新依赖目录，`AccessDeniedException`；`BUILD FAILURE`；退出码1。获准写缓存并下载官方依赖后，同命令13/13 PASS、退出码0；这是环境权限失败，不冒充代码失败 |
| 首次定向命令/失败 | `.\mvnw.cmd -B -Pdb '-Dtest=SecurityPaymentWebIT' test` 在沙箱内无法连接 `127.0.0.1:5433`，根因 `SocketException: Permission denied: getsockopt`；10项上下文错误、退出码1。获准访问本机测试库后复测10/10 PASS、退出码0 |
| 扩展定向首次结果 | 增至13项后首次运行12通过/1错误、退出码1；错误为 issuer 普通字符串不能由 `Jwt.getIssuer()` 转为 URL。改成 URI issuer 后同命令 `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 18.868 s`；退出码0 |
| CORS统一错误首次结果 | 为把未知Origin的默认纯文本403改为统一错误信封，首次加入自定义 `CorsFilter` 后应用上下文因存在2个 `CorsConfigurationSource` 候选而无法启动；`Tests run: 13, Failures: 0, Errors: 13, Skipped: 0`；退出码1。对项目配置源使用明确 `@Qualifier` 后复测 `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 32.187 s`；退出码0 |
| CORS强化终检 | 增加“不返回 `Access-Control-Allow-Origin` 且响应正文不回显恶意Origin”断言后，再次执行同一定向命令；`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 23.239 s`；`Finished at: 2026-11-28T00:43:02+08:00`；退出码0 |
| 安全断言 | 缺令牌→`AUTH_REQUIRED`；畸形/非法subject/非法role→`TOKEN_INVALID`；过期→`TOKEN_EXPIRED`；CUSTOMER访问admin→`ACCESS_DENIED`；ADMIN通过安全层后得到路由404；错误信封不泄漏异常/包名/SQL |
| 令牌/密码/CORS | HS256密钥至少32字节且拒绝占位符；`iss/sub/roles/iat/exp` 验证；签发时效1800秒、无refresh；BCrypt strength12 单向哈希匹配；仅配置的 `http://localhost:5173` 预检通过，未知来源返回403与统一 `ACCESS_DENIED` JSON且不回显该Origin |
| 支付 HTTP | Owner JWT 的 `sub` 被映射为 application userId；成功响应ID为字符串且无内部 `replayed` 字段；同HTTP幂等键重放仅1条支付；非Owner得到 `ORDER_NOT_FOUND`；缺幂等头/非法结果为 `VALIDATION_FAILED` 且不写支付 |
| Web组合命令 | `.\mvnw.cmd -B -Pdb '-Dtest=SecurityPaymentWebIT,WebContractIT' test` |
| Web组合结果 | `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`；既有健康、404、不泄漏内部信息和OpenAPI均未被安全过滤链破坏；`BUILD SUCCESS`；退出码0 |
| 最终默认命令/结果 | `.\mvnw.cmd -B test`；`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 5.059 s`；`Finished at: 2026-11-28T00:40:11+08:00`；退出码0 |
| 最终全量命令/结果 | `.\mvnw.cmd -B -Pdb test`；Flyway校验5个迁移，schema v5；`Tests run: 146, Failures: 0, Errors: 0, Skipped: 0`；`BUILD SUCCESS`；`Total time: 01:23 min`；`Finished at: 2026-11-28T00:41:43+08:00`；退出码0 |
| 结论 | PASS：JWT验证/授权/CORS/统一安全错误与模拟支付真实HTTP均有可执行证据；失败与修复链均保留 |
| 验证边界 | 测试库未DROP/CREATE；未实现注册/登录/当前用户、创建/查询/取消订单HTTP（R6-11）；未做登录限流、密钥轮换、真实前端或JMeter；这些均为 NOT RUN |
