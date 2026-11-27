# Skill合规记录

## SC-20261125-01

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| skill-creator | `C:/Users/willz/.codex/skills/.system/skill-creator/SKILL.md` | 适用：本轮创建三个项目Skill | 完整读取；按名称、YAML frontmatter、范围、步骤、禁止事项、验收标准建立自包含Skill；官方validator三轮结果均记录 | PASS_WITH_RETRIES；见TE-20261125-03 |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 适用：本轮修改DB/API/ENUM/业务流程和版本状态 | 创建后完整复读；检查四份事实源；恢复历史来源；将无证据选项登记为D-001/D-002；使用rc候选状态 | APPLIED；正式正确性等待人工Review |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮提交并推送feature分支 | 创建后完整复读；确认授权分支、HEAD和远端；用语义化提交保存并推送feature；未合并、未改保护、未改历史 | APPLIED；推送证据见TE-20261125-07，Review仍待人工 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：本轮执行文档、Skill和Git检查 | 创建后完整复读；失败与成功复测均保留；前置/静态证据记录真实命令、输出摘要、退出码；业务测试明确NOT RUN | APPLIED |
| documents/docx | 未读取 | 不适用：本轮仅修改仓库Markdown，不创建或编辑DOCX | 记录不适用原因，无执行步骤 | NOT APPLICABLE |
| Pages/Google Docs | 未读取 | 不适用：用户明确要求仓库文件，不写外部页面或云文档 | 记录不适用原因，无执行步骤 | NOT APPLICABLE |

本记录只证明列出的文件读取和项目内步骤，不证明模型隐藏思考过程，也不构成独立审计。

## SC-20261125-02

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| contract-change | `.agents/skills/contract-change/SKILL.md` | 适用：批准结果修改DB/API/ENUM和业务流程契约 | 完整读取；核对四类事实源、原始选项和批准边界；保留历史选项；完整diff自检时纠正未获批准的混合外键方案；保持v1.0-rc | APPLIED；静态证据见TE-20261125-09，正式正确性待PR Review |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮进行feature分支提交与推送 | 完整读取；确认授权分支、HEAD、远端同步状态；创建并推送 `df0e33c`；远端核对main/develop未变；未改历史、未创建或合并PR | APPLIED；证据见TE-20261125-11 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：需记录批准同步的真实验证 | 完整读取；工具输出截断的组合检查未记PASS，改为拆分重跑；记录命令、环境、输出摘要、退出码；运行时测试记NOT RUN | APPLIED |
| skill-creator | 未读取 | 不适用：本轮没有创建或修改Skill | 记录不适用原因，无执行步骤 | NOT APPLICABLE |

本记录仅提供项目内可复核的过程证据，不证明隐藏思考过程，也不替代 `Janovo2117` 的GitHub PR Review。

## SC-20261126-01

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| contract-change | `.agents/skills/contract-change/SKILL.md` | 有限适用：只更正 `CONTRACT_FREEZE.md` 的风险陈述与登记审计条目，未修改DB/API/ENUM/业务流程任何规则条文 | 完整读取；核对四份事实源与版本状态；确认R5-04/R5-05为未见批准证据的歧义，登记为待Reviewer判定而不擅自改契约；保持 `CANDIDATE v1.0-rc` | APPLIED_LIMITED；契约条文未变，证据见TE-20261126-02/04 |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：核查PR、纠正目标分支、提交并推送feature分支 | 完整读取；先记录分支/HEAD/工作区/远端；只读核查PR元数据后，用 `gh pr edit --base develop` 纠正目标分支；未创建Review、未合并、未改分支保护、未改历史 | APPLIED；证据见TE-20261126-03 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：本轮执行Git/GitHub核查与文档静态检查 | 完整读取；未预填PASS；`git diff --check` 首次因两文件EOF多余空行返回退出码2，修正后重跑；业务与运行时测试均记NOT RUN | APPLIED_WITH_RETRY；证据见TE-20261126-04/05 |
| skill-creator | 未读取 | 不适用：本轮未创建或修改Skill | 记录不适用原因，无执行步骤 | NOT APPLICABLE |

本记录仅证明列出的文件读取与项目内命令执行，不构成独立审计，也不代表 `Janovo2117` 已完成PR Review。

## SC-20261126-02

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| contract-change | `.agents/skills/contract-change/SKILL.md` | 适用：修改DB/API/ENUM契约与设计文档 | 完整读取；**首次按改写后的8项消费者清单逐项执行**（DATABASE_CONTRACT/API_CONTRACT/ENUMS/BUSINESS_FLOWS/DATABASE_DESIGN/CONTRACT_FREEZE/REQUIREMENTS/TEAM_TASK_BOARD），逐项结论记入 OP-20261126-02；为两项无批准证据的歧义登记 D-003/D-004 并取得负责人批准；未升级版本状态 | APPLIED；证据见TE-20261126-07 |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：提交并推送 feature 分支 | 完整读取；确认授权分支、HEAD、远端无分歧；只暂存明确文件；未合并、未代Review、未改分支保护 | APPLIED；证据见TE-20261126-10 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：本轮执行术语检索、Skill校验与diff检查 | 完整读取；未预填PASS；Skill校验首次因解释器缺PyYAML返回退出码1，如实保留并按 test-evidence 规则以新条目追加复测；运行时测试记NOT RUN | APPLIED_WITH_RETRY；证据见TE-20261126-08/09 |
| skill-creator | 未读取 | 不适用：本轮只改写既有 Skill 的步骤文本，未创建新 Skill | 记录不适用原因；仍用其 `quick_validate.py` 做结构校验 | NOT APPLICABLE |

## SC-20261126-03

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：本轮只修订 `docs/ARCHITECTURE.md`（设计文档，非 DB/API/ENUM 三份契约） | 读取并确认其适用边界；未修改任何契约条文，未触碰 8 项消费者清单 | NOT APPLICABLE |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：记录分支/HEAD/工作区，提交前检查 diff | 记录 `git branch/status/remote`；执行 `git diff --check`；未 reset、未强推、未改历史 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：S1 真实构建需记录命令/输出/退出码 | 记录 `./mvnw -B test` 首次失败与修复后通过的真实输出与退出码；业务/DB 测试记 NOT RUN | APPLIED |
| skill-creator | 未读取 | 不适用：本轮未创建或修改 Skill | — | NOT APPLICABLE |

本记录仅证明列出的文件读取与项目内命令执行，不构成独立审计。

## SC-20261126-04

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | 本轮新建 `.agents/skills/project-governance/SKILL.md` | 适用：本轮即按该 Skill 流程自举执行 | 调查状态→创建 Skill→改 AGENTS.md→建开发日志→`git diff --check`→记录审计 | APPLIED（新建并自举） |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：未修改 DB/API/ENUM 契约 | 记录不适用原因 | NOT APPLICABLE |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：记录分支/HEAD/diff 检查 | 记录 branch/HEAD/status；`git diff --check` | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：记录本轮检查证据 | 记录命令与退出码；业务测试 NOT NEEDED | APPLIED |

## SC-20261126-05

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：本轮为实质性后端/数据库开发 | 完整读取；执行启动检查（分支/HEAD/工作树/远端/PR/分支保护）；任务分类 `FEAT`+`DB`+`TEST`；风险判断（不触碰公共契约，故不请求队友 Review）；按 §13 逐项闭环并更新开发日志、任务板、CHANGELOG、TEST_EVIDENCE、OPERATION_LOG、本文件 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮提交 `eb67a1d` 及主线提交 | 完整读取；确认授权分支 `feature/database-core`；只暂存明确文件；`git diff --check`；语义化提交；**未推送、未建 PR、未合并、未改分支保护** | APPLIED；证据见 OP-20261126-05 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：本轮执行真实构建与数据库集成测试 | 完整读取；运行前记录命令与环境；首次失败记录为 TE-20261126-14 并保留；复测追加 TE-20261126-16，不覆盖失败；未运行项 TE-20261126-17 明确标 NOT RUN；记录真实退出码 0 | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：本轮未修改 `DATABASE_CONTRACT.md`/`API_CONTRACT.md`/`ENUMS.md` 条文 | 记录不适用原因：`V1__baseline.sql` 是既有契约「身份与权限」节的落地实现，未新增或变更契约语义；D-005 等仍待 PR #2 评审 | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261126-06

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：本轮为实质性后端开发 | 完整读取；启动检查（分支 `feature/database-core`、HEAD `cf6a77b`、工作树干净、容器 healthy）；任务分类 `FEAT`+`TEST`；风险判断：不改公共契约条文，故不触发队友 Review；按 §13 闭环并同步六类文档 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮产生提交 | 完整读取；确认授权分支与范围；只暂存明确文件；提交前 `git diff --check`；**未推送、未建 PR、未合并** | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：本轮执行真实构建与 HTTP 测试 | 完整读取；首次 `RestClient.Builder` 失败保留为 TE-19 并追加复测；记录真实退出码 0；未运行项 TE-20 标 NOT RUN | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 边界适用：未改契约条文，但新增针对 `API_CONTRACT.md` §1/§4 的验证测试 | 仅读取 `API_CONTRACT.md` 作为断言依据；未修改 `DATABASE_CONTRACT.md`/`API_CONTRACT.md`/`ENUMS.md` 任何条文；`ARCHITECTURE.md` 版本矩阵修正属事实同步 | NOT APPLICABLE（契约条文无变更） |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-01

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：推送、PR、数据库迁移均属实质性工作 | 完整读取；启动检查（分支/HEAD/工作树/远端/PR）；任务分类 `GIT`+`DB`+`TEST`；风险评估：迁移基于未冻结契约，已登记风险；按 §13 闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮推送并创建 PR | 完整读取；推送前 `git fetch` 核对 ahead/behind 与 `origin/develop` 未变；PR 正文含目标/范围/契约影响/测试证据/风险/回滚/未完成项；未合并、未改分支保护与权限 | APPLIED；PR #3 证据见 OP-20261127-01 |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：执行迁移与约束测试 | 完整读取；空库重建后全量执行；首次编译错误保留为 TE-20261127-02 失败记录；未覆盖项 TE-20261127-03 明确 NOT RUN；记录真实退出码 0 | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | **不适用**：V2/V3 是既有契约结构的 DDL 落地 | 逐条对照 `DATABASE_CONTRACT.md` §2 与 `ENUMS.md` 的枚举值编写约束，**未修改任何契约条文**；`R5-04` 未裁定的语义刻意不写死，避免事实上改动契约 | NOT APPLICABLE（契约条文无变更） |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-02

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：PR 催办与数据库迁移 | 完整读取；启动检查（分支/HEAD/工作树/远端 fetch 后仍 0 behind）；任务分类 `DB`+`TEST`+`GIT`；风险判断：V4 基于未冻结契约、部分表依赖 B 侧，已登记；§13 闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮提交与推送 | 完整读取；只暂存明确文件；`git diff --check`；提交前核对 `origin/develop` 未变；**未合并 PR、未改分支保护、未代替队友 approve** | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：迁移与约束测试 | 完整读取；空库重建后全量执行；首次失败（测试自身缺陷）保留为 TE-20261127-05 并追加复测；刻意未做项 TE-20261127-06 标 NOT RUN 并说明原因；记录真实退出码 0 | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | **不适用**：未修改契约条文 | 仅引用 `DATABASE_CONTRACT.md` §2 与 `ENUMS.md` 作为 DDL 依据；围绕未裁定的 D-005 决策在 PR #2 评论中说明下游影响，而未擅自改契约 | NOT APPLICABLE（契约条文无变更） |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-03

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：数据库迁移 + 并发测试 | 完整读取；启动检查（分支/HEAD/工作树/`git fetch` 后仍 0 behind/`origin/develop` 未变）；任务分类 `DB`+`TEST`；风险评估：迁移基于未冻结契约、并发测试稳定性；§13 闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮提交与推送 | 完整读取；只暂存明确文件；`git diff --check`；未合并 PR、未改分支保护 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：迁移、约束、并发测试 | 完整读取；空库重建后全量执行；并发测试连跑 4 轮记录稳定性；两次失败（均为测试缺陷）保留并追加复测；TE-20261127-10 标注未覆盖范围与原因 | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | **不适用**：未修改契约条文 | 仅按 `DATABASE_CONTRACT.md` §2 的 `lodging_reservation` 定义与排斥谓词原文落地 DDL；`OccupancyStatus` 取值域直接取自 `ENUMS.md` | NOT APPLICABLE（契约条文无变更） |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-04

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨模块应用层开发 | 完整读取；启动检查（分支/HEAD/工作树/fetch 后 0 behind/`origin/develop` 未变）；任务分类 `FEAT`+`TEST`；风险判断：未改契约、未暴露无认证端点；§13 闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md` | 适用：本轮提交与推送 | 完整读取；只暂存明确文件；`git diff --check`；未合并 PR、未改分支保护 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：事务与幂等测试 | 完整读取；空库重建后全量执行；首次失败（产品代码缺陷）与编译歧义均保留并追加复测；TE-20261127-12 标注未覆盖范围 | APPLIED |
| contract-change | `.agents/skills/contract-change/SKILL.md` | **不适用**：未修改契约条文 | 以 `API_CONTRACT.md` §1/§2/§4 与 `DATABASE_CONTRACT.md` §2 为行为依据实现；未改动任何契约文件 | NOT APPLICABLE（契约条文无变更） |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-05

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：本轮为实质性进度/缺陷审查 | 完整读取；恢复分支/HEAD/工作树/远端/任务板/开发日志/审计/PR；分类 REVIEW+TEST+GIT；结束时同步审计与进度文档 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：执行 fetch 与只读 PR 核查 | 核对 ahead/behind、PR base/head、review、mergeability、checks；未提交/推送/Review/合并/改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：重跑默认测试和 PostgreSQL 回归 | 保留默认测试退出码1；追加 `-Pdb` 101 测试退出码0；明确干净库、DAY_PASS、并发幂等等 NOT RUN 边界 | APPLIED；TE-20261127-13 |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：只读取契约判断实现一致性，未修改任何公共契约语义 | 未执行八消费者变更流程；发现的问题记入 ISSUES，等待后续实现修复或人工 Review | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-06

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨模块修复与真实数据库并发测试 | 恢复 Git/远端/任务板/契约/问题状态；限定不改迁移和契约；完整 diff、测试和文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：继续在已有 PR #3 分支工作并准备提交 | fetch 后确认 `origin/develop` 无新增分歧；保留上一轮自有文档修改；提交前执行完整 diff 与 `git diff --check`；未合并/改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：默认、定向及全量 PostgreSQL 测试 | 记录13/13、20/20、110/110与退出码0；明确非空库、未运行范围 | APPLIED；TE-20261127-14 |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：实现并验证候选契约既有规则，未修改 DB/API/ENUM/流程条文 | 未新增表、迁移、枚举、端点或状态；公共契约仍保持 `v1.0-rc` | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-07

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨 catalog/booking 的数据库业务校验 | 恢复 Git/任务板/开发日志/契约；限定不改迁移和契约；执行测试、完整 diff 与文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：继续向已有 PR #3 提交 | 只暂存本轮文件；提交前执行完整 diff 与 `git diff --check`；不合并、不改设置、不代替 Review | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：维修正反例与全量回归 | 记录22/22、13/13、112/112及退出码0；明确非空库及未运行边界 | APPLIED；TE-20261127-15 |
| contract-change | `.agents/skills/contract-change/SKILL.md` | 不适用：只实现候选契约已有维修状态/区间语义 | 未修改 DB/API/ENUM/流程；开放规则歧义登记 R6-08 并停止对应实现 | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-08

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：独立审计整改、跨 catalog/booking 修复与 clean 回归 | 完整读取；恢复 Git/任务板/开发状态；限定任务 B/C；测试、完整 diff、文档和 Git 闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：继续向已有 PR #3 提交 | fetch 后确认分支与工作树；只暂存本轮文件；提交前执行完整 diff/`git diff --check`；不合并、不改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：两个新增 IT、clean 与完整 PostgreSQL 回归 | 两项 IT 各自记录实测输出/退出码；clean 后记录114项逐类汇总、Maven最终输出、退出码和陈旧报告终检 | APPLIED；TE-20261127-16 |
| contract-change | 未读取（本轮不适用） | 不适用：没有修改 DB/API/ENUM/业务流程公共条文 | 复用既有 `resource_unit.name` 与 `item_name_snapshot` 字段；内部快照增加字段不改变外部契约 | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-09

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨 payment/order/lodging 的事务功能 | 完整读取；恢复 Git/任务板/日志/契约/状态机；限定不改HTTP/认证/迁移；执行测试、完整diff与文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：继续向 PR #3 提交 | 工作前确认干净分支；仅暂存支付单元文件；提交前完整 diff/`git diff --check`；不合并、不改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：支付事务、幂等与并发验证 | 真实 PostgreSQL 定向10项、默认13项、全量124项；记录输出、退出码和未覆盖范围 | APPLIED；TE-20261127-17 |
| contract-change | 未读取（本轮不适用） | 不适用：严格实现既有支付请求、幂等、状态机和数据库表 | 未修改 DB/API/ENUM/业务流程条文或 Flyway | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-10

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨 booking/order/payment/lodging 的取消事务 | 复用本轮已恢复状态与契约；限定独立取消单元；执行测试、完整diff与文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：向 PR #3 追加独立提交 | 仅暂存取消/退款工作单元；提交前完整 diff/`git diff --check`；不合并、不改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：退款、状态回滚及并发取消验证 | 真实 PostgreSQL 定向16项、默认13项、全量130项；记录输出、退出码和边界 | APPLIED；TE-20261127-18 |
| contract-change | 未读取（本轮不适用） | 不适用：实现既有整单取消、REFUNDED及状态转换 | 未修改 DB/API/ENUM/业务流程条文或 Flyway | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261127-11

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：跨 booking/order/lodging 的定时事务功能 | 复用已恢复状态与候选状态机；限定过期扫描独立单元；执行测试、完整diff与文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：向 PR #3 追加独立提交 | 仅暂存过期扫描单元；提交前完整 diff/`git diff --check`；不合并、不改设置、不代替 Review | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：过期事务、过滤与幂等释放验证 | 真实 PostgreSQL 定向19项、默认13项、全量133项；记录原始汇总、退出码及未实测多 JVM 边界 | APPLIED；TE-20261127-19 |
| contract-change | 未读取（本轮不适用） | 不适用：实现既有 EXPIRED 状态转换与住宿释放 | 未修改 DB/API/ENUM/业务流程条文或 Flyway | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。

## SC-20261128-01

| Skill | 实际读取文件 | 适用性 | 实际执行步骤 | 验证状态 |
|---|---|---|---|---|
| project-governance | `.agents/skills/project-governance/SKILL.md` | 适用：认证安全、跨 identity/payment/common 的 HTTP 集成 | 完整恢复 Git/远端/任务板/日志/契约/架构；分类 SECURITY+FEAT+TEST；遇 DTO 歧义停止对应实现；测试与文档闭环 | APPLIED |
| git-pr | `.agents/skills/git-pr/SKILL.md`、`docs/TEAM_WORKFLOW.md` | 适用：继续向 PR #3 增加独立安全提交 | fetch确认0 behind及工作树干净；只暂存本单元文件；提交前完整diff/敏感信息/`git diff --check`；不合并、不改设置 | APPLIED |
| test-evidence | `.agents/skills/test-evidence/SKILL.md` | 适用：安全、HTTP、数据库与构建验证 | 保留3类首次失败；追加定向13、组合18、默认13、全量146的实测输出/退出码和NOT RUN边界 | APPLIED；TE-20261128-01 |
| contract-change | 未读取（本轮不适用） | 不适用：只实现候选契约已有Bearer/JWT/角色/PaymentResponse；发现Auth/Order DTO缺口后停止并登记R6-11 | 未修改 API/DB/ENUM/流程条文或Flyway，等待另一成员Review | NOT APPLICABLE |

本记录只证明列出的文件读取与项目内步骤，不证明模型隐藏思考过程，也不替代 `Janovo2117` 的 GitHub Review。
