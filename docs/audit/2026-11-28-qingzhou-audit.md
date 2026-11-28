# 轻舟源码与工程架构审计候选任务清单

**供 openEuler 实习导师审核 · 2026 年 11 月 28 日**

建议先交付 **QZ-01 版本包升级可恢复**或 **QZ-02 AI 对话组件重新激活**。两项均已复现，涉及模块少，修复结果可从产品入口验收。**QZ-05 远端指标采集调度**适合作为中大型主任务，已有并发基线，可以量化延迟和上游请求量。若导师更关注应用侧落地，可选 **QZ-06 Redis 本地持久化**或 **QZ-07 Nacos 多命名空间纳管**。

## 1. 审计基线与去重

- **源码基线**为 AtomGit `master` 的 `c3f880be34467c00d67be5772d6ace5962d65cd2`，提交于 2026 年 11 月 20 日。检出包含 2,732 个可达提交、606 个跟踪文件和 48 个 Maven reactor 项目，其中有聚合 POM。先盘点构建、运行时和应用目录，再沿启动、应用探测、REST/MCP、Agent 心跳、监控、AI 对话及内置应用的数据路径抽查实现与测试。[固定提交源码树](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/README.md#L9) · [根 POM](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/pom.xml#L11)
- **架构**从 Launcher 选择版本并载入 Command 开始，Engine 启动 Felix/OSGi 和应用；普通 Java 应用经 Bundle 转换，驱动读取模型元数据，注册中心为控制台、REST 和 AI 工具提供动作执行入口。Agent 通过注册与心跳进入纳管，指标由独立组件收集并渲染成 Prometheus 格式。审计沿这些边界追踪问题归属，任务验收覆盖受影响的公开入口。[Engine 装配](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-engine/src/main/java/qingzhou/engine/QingzhouMain.java#L38) · [应用开发指南](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/docs/%E8%BD%BB%E8%88%9F%E5%BA%94%E7%94%A8%E5%BC%80%E5%8F%91%E6%8C%87%E5%8D%97.md#L5) · [接口与集成](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/docs/%E8%BD%BB%E8%88%9F%E5%B9%B3%E5%8F%B0%E4%BD%BF%E7%94%A8%E6%89%8B%E5%86%8C.md#L80)
- **上游去重**读取了轻舟项目 API 返回的全状态列表，65 条 Issue 均关闭，953 条 PR 中 882 条合并、69 条关闭、2 条开放。另读取实习仓 205 条 Issue，按标题和项目链接筛出 37 条轻舟相关记录，其中 1 条开放。核读了与候选项相关的条目描述及 Git 变更。开放的轻舟 PR 为 HTTPS 信任证书和 CertiControl 应用，证书对应开放实习任务 #212。已合入的 AI 会话配额、监控协议解耦、Windows 中文启动路径和 AppBuilderSkill 不再列为原题。[PR !924](https://atomgit.com/openeuler/qingzhou/merge_requests/924) · [PR !955](https://atomgit.com/openeuler/qingzhou/merge_requests/955) · [实习 Issue #212](https://atomgit.com/openeuler/opensource-intern/issues/212) · [PR !892](https://atomgit.com/openeuler/qingzhou/merge_requests/892) · [PR !885](https://atomgit.com/openeuler/qingzhou/merge_requests/885) · [PR !601](https://atomgit.com/openeuler/qingzhou/merge_requests/601) · [PR !866](https://atomgit.com/openeuler/qingzhou/merge_requests/866) · [PR !952](https://atomgit.com/openeuler/qingzhou/merge_requests/952)
- **交付规范**遵循仓库约定，回归从公开入口和外部配置驱动，保留 OSGi 的生命周期契约。跨模块设计和构建变更先让维护者审阅边界。正式交付时按社区要求完成 CLA、人工复核并在 PR 中披露实际 AI 工具使用。[仓库 AGENTS.md](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/AGENTS.md#L26) · [openEuler 贡献攻略](https://www.openeuler.org/zh/community/contribution/detail.html) · [社区生成式 AI 贡献策略](https://www.openeuler.org/zh/community/ai-coding-assistants/)

## 2. 构建、运行与覆盖率

| 验证场景 | 结果 |
| --- | --- |
| Windows 11 x64、Maven 3.9.11、Microsoft JDK 21.0.9，`clean verify` | 48 个 reactor 项目通过，310 项测试，0 失败、0 跳过。[构建日志](2026-11-28-verification.md#构建与覆盖率) |
| Windows 11 x64、Maven 3.9.11、Azul JDK 8u514，`clean verify` | 48 个 reactor 项目通过，310 项测试，0 失败、0 跳过。[构建日志](2026-11-28-verification.md#构建与覆盖率) |
| Windows 发行包、JDK 8 和 21，普通路径及中文含空格路径 | 4 组均初始化成功。`/web` 和入口 JS 为 200，登录为 200，匿名访问注册中心为 401，登录后为 200。[JDK 8](2026-11-28-verification.md#发行包冒烟) · [JDK 21](2026-11-28-verification.md#发行包冒烟) |
| openEuler 24.03 LTS x86_64 WSL、Azul JDK 21、Maven 3.9.11，`-Prelease clean verify` | 最小环境在 `qingzhou-image` 的 2 项测试失败，首个缺失库为 `libXext.so.6`。单独设置 `java.awt.headless=true` 后二维码测试通过，验证码仍报字体配置不可用。[首轮](2026-11-28-verification.md#构建与覆盖率) · [无头对照](2026-11-28-verification.md#构建与覆盖率) |
| openEuler 加无头 JVM 选项和隔离字体依赖，JDK 21.0.13.1 | 48 个 reactor 项目通过，310 项测试，0 失败、0 跳过。release 产物在中文含空格目录启动，入口 JS、登录后 REST 及 MCP 初始化均为 200，MCP 列出 13 个工具。双中心与 Agent 的本机冒烟成功导出 45 条远端指标序列。[构建日志](2026-11-28-verification.md#构建与覆盖率) · [运行日志](2026-11-28-verification.md#发行包冒烟) |
| Windows JDK 21 下用 JaCoCo 0.8.13 对原有测试计量 | 14 个模块生成报告，按这 14 个模块的行/分支总数加权分别为 **44.6%**（2,268/5,086）和 **35.5%**（1,062/2,995）。跨模块执行没有计入被依赖模块的单模块报告。[模块数据](2026-11-28-verification.md#构建与覆盖率) · [构建日志](2026-11-28-verification.md#构建与覆盖率) |

源码中有 36 个 `*Test.java`，分布在 14 个模块。启动器、Engine、App Driver、Web、Agent、Monitor、AI Chat、MCP 及内置应用目录无随该提交跟踪的测试类。Registry 行覆盖 17.0%，HTTP Server 为 57.6%，AI Memory 为 70.1%。这些差异支持先补包级和跨模块回归。基线未跟踪 Maven Wrapper 或仓库内工作流定义，托管平台可能还有项目设置或社区流水线。同一 Maven 3.9.11 reactor 中，Launcher 使用 Resources Plugin 3.3.1，Common 等 bundle 则解析到 3.5.0，构建工具链仍有收敛空间。[构建插件配置](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/pom.xml#L81) · [产品组装及外部 Java 命令](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/qingzhou/pom.xml#L33)

## 3. 候选任务速览

| 编号 | 任务 | 建议优先级 | 规模 / 人日 | 证据 |
| --- | --- | --- | --- | --- |
| QZ-01 | 版本包解压原子发布与失败恢复 | P1 | 中 / 4–6 | 发行包复现 |
| QZ-02 | AI 对话组件重新激活与流资源回收 | P1 | 小 / 2–4 | 组件入口复现 |
| QZ-03 | Nacos 令牌续期与错误状态传递 | P1 | 小 / 3–4 | 回环 HTTP 复现 |
| QZ-04 | Tomcat 运行实例跨平台自动发现 | P1 | 中 / 4–7 | Windows 与 openEuler 对照 |
| QZ-05 | 远端指标采集与抓取请求解耦 | P1 | 大 / 10–15 | 可控延迟负载实验 |
| QZ-06 | Redis 审计文件有界保留与可靠恢复 | P1 | 中大 / 8–12 | 文件持久化实测 |
| QZ-07 | Nacos 服务列表按命名空间和分组纳管 | P2 | 中大 / 7–11 | HTTP 契约替身及源码 |
| QZ-08 | 内置应用大页码溢出与分页契约 | P2 | 中 / 4–6 | Redis 公开模型存储入口复现 |
| QZ-09 | 最小 openEuler 发布基线与包级 CI | P1 | 中 / 5–8 | 全量构建与包级实测 |
| QZ-10 | 前端构建来源、依赖锁定与兼容冒烟 | P2 | 大 / 10–15 | Git 树和包级静态资源验证，依赖源码授权 |
| QZ-11 | Agent 主备续期恢复与切换可观测性 | P2 | 中大 / 7–10 | 心跳状态序列复现 |

P1 表示优先解决影响运行、恢复或交付的缺陷，P2 表示功能边界或架构储备。人日包含实现、回归和说明，依赖维护者确认的条件列在对应条目。验收时延是建议预算。涉及性能的任务应在固定配置和同一环境复跑基线。

## 4. 候选任务详述

### QZ-01 版本包解压原子发布与失败恢复

- **问题与源码**。Launcher 直接解压到最终版本目录，后续启动只判断该目录存在。将产品包作为正常对照，另用无业务数据的坏包模拟解压中断。首次解压失败后残留半成品目录，用同名正常包覆盖后仍因 `ClassNotFoundException` 退出。更高版本的同一正常包可启动。项目历史已经支持版本管理和多实例升级，本条修复的是升级失败后的版本可用性判定。[解压与版本选择](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-launcher/src/main/java/qingzhou/launcher/Launcher.java#L47) · [逐项写入](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-launcher/src/main/java/qingzhou/launcher/Launcher.java#L95) · [发行包复现](2026-11-28-verification.md#版本包恢复) · [项目 Issue #11](https://atomgit.com/openeuler/qingzhou/issues/11) · [PR !251](https://atomgit.com/openeuler/qingzhou/merge_requests/251)
- **工程价值**。镜像发布、包拷贝中断及多实例首次启动期间，错误升级不会长期挡住正常版本，恢复过程可纳入运维自动化。
- **实现方案**。对同一个安装根目录的升级解压加跨进程文件锁。解压到同文件系统的临时目录并验证引导所需结构，再发布为可用版本。旧版解压目录按结构兼容识别，失败时保留已验证版本并报告失败阶段。无可用版本时给出明确退出码。
- **模块及成本**。`qingzhou-launcher`，必要时补产品包结构契约和使用手册。P1，中，4–6 人日。
- **验收**。Windows 与 Linux 各验证正常包、坏包、解压中止、同名正常包恢复、旧目录兼容，以及两个进程同时首次启动。并行首启 20 轮无半成品被选中。可用旧版本存在时服务可恢复，空安装的无效包应明确失败。

### QZ-02 AI 对话组件重新激活与流资源回收

- **问题与源码**。`AiChat.deactivate()` 关闭静态看门狗执行器，`getExecutorService()` 只在引用为 null 时创建执行器。第二次激活沿公开 `handle()` 处理同样请求时，触发 `RejectedExecutionException`，模型调用数保持在 1，`streams_active` 留在 1。该指标在任务调度前已递增。历史 !911 修复的是客户端停止后取消模型。[组件停用和初始化](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/AI/qingzhou-ai-chat/src/main/java/qingzhou/ai/chat/impl/AiChat.java#L73) · [执行器生命周期](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/AI/qingzhou-ai-chat/src/main/java/qingzhou/ai/chat/impl/SseWatchdog.java#L16) · [流启动计数](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/AI/qingzhou-ai-chat/src/main/java/qingzhou/ai/chat/impl/SseChatListener.java#L45) · [组件复现](2026-11-28-verification.md#ai-对话重新激活) · [PR !911](https://atomgit.com/openeuler/qingzhou/merge_requests/911)
- **工程价值**。OSGi 服务重新绑定或配置重新激活后，对话能力可恢复，在途流与指标可正确收尾。
- **实现方案**。将看门狗生命周期改为组件实例所有；将启动计数与任务登记作为一次状态转换。停用时终结在途下游流，取消上游订阅及定时任务，关闭操作保持幂等。
- **模块及成本**。`qingzhou-ai-chat`，使用现有 `HttpResponse` 和模型订阅契约。P1，小，2–4 人日。
- **验收**。重复激活和停用 20 轮，每轮至少一次对话可进入模型并完成。20 条并发流在停用后 2 秒内全部收尾，活跃流计数回零，下一轮可正常调度；终止事件和上游取消各至多一次。回归覆盖正常完结、调度失败和已有客户端取消。

### QZ-03 Nacos 令牌续期与错误状态传递

- **问题与源码**。`getAccessToken()` 只在缓存为 null 时登录，读取 `accessToken` 后丢弃 `tokenTtl`。`getConfigContent()` 直接返回响应体，没有检查 HTTP 状态。本地 v1 契约替身返回 1 秒有效的令牌，到期后查询收到 403，轻舟仍仅登录 1 次，并把错误正文当作配置内容返回。[令牌缓存及配置读取](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-nacos/src/main/java/qingzhou/app/nacos/NacosApi.java#L34) · [响应解析及状态判断](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-nacos/src/main/java/qingzhou/app/nacos/NacosApi.java#L196) · [Nacos v1 认证契约](https://nacos.io/en/docs/v1/auth/) · [回环 HTTP 复现](2026-11-28-verification.md#nacos-契约诊断) · [PR !794](https://atomgit.com/openeuler/qingzhou/merge_requests/794)
- **工程价值**。启用鉴权的 Nacos 可长期纳管，运维能够区分配置内容、凭据过期与权限不足。
- **实现方案**。根据服务端 TTL 按需刷新并合并并发登录。保存令牌与截止时间的同一快照。统一检查读取状态，仅对可确认的令牌失效做一次有限重试。写请求先刷新再发送，结果不明确时不重放。为认证服务不可用设置有限请求超时。
- **模块及成本**。`qingzhou-app-nacos` 的 HTTP 适配与错误映射。P1，小，3–4 人日。
- **验收**。同一进程经历两次令牌到期后仍可读取。20 个并发请求触发同一轮刷新时只发起一次登录。真实权限 403、配置 404 和远端 5xx 均返回明确错误，错误正文不会作为配置展示。一次写动作最多执行一次。

### QZ-04 Tomcat 运行实例跨平台自动发现

- **问题与源码**。Windows 取的是进程可执行文件 `Path`，Tomcat 的识别标记与 `catalina.home` 在 JVM 参数中。Unix 读进程参数文本，但清单正则用 `\S+` 提取路径。以两个仅携带 Tomcat 标记的测试 JVM、两个真实特征目录验证进程探测。Windows 两个目录都漏报，openEuler 命中普通目录、漏报含空格目录。已合并的 !934、!935、!951 涉及一般进程探测、动态启停和编号，这里是特定平台和命令行参数的回归。[Windows 与 Unix 采集命令](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-engine/src/main/java/qingzhou/engine/detector/ProcessParser.java#L112) · [目录提取](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-engine/src/main/java/qingzhou/engine/detector/ProcessParser.java#L54) · [Tomcat 清单](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-tomcat/pom.xml#L19) · [Windows 实测](2026-11-28-verification.md#tomcat-进程探测) · [openEuler 实测](2026-11-28-verification.md#tomcat-进程探测) · [PR !934](https://atomgit.com/openeuler/qingzhou/merge_requests/934) · [PR !935](https://atomgit.com/openeuler/qingzhou/merge_requests/935) · [PR !951](https://atomgit.com/openeuler/qingzhou/merge_requests/951)
- **工程价值**。非默认目录中的 Tomcat 可以被自动纳管，多实例部署降低人工配置量。
- **实现方案**。Windows 使用可读取本用户进程命令行的实现，采集失败时明确上报并保留其他探测来源。优先使用结构化参数或按平台解析引号，明确 UTF-8 和路径归一化语义。把识别、目录检验和调度回归分开测试，延续 JDK 8 兼容约定。
- **模块及成本**。`qingzhou-engine` 探测器、`qingzhou-app-tomcat` 清单和包级 fixture。P1，中，4–7 人日。
- **验收**。Windows 和 openEuler 各部署 2 个真实 Tomcat，覆盖普通、空格、中文目录组合。仅靠进程来源在 2 个配置探测周期加一次扫描预算内发现全部实例，重复扫描无重复条目。进程退出按既有探测语义验收，并确认 Nginx 及环境变量来源的现有用例通过。

### QZ-05 远端指标采集与抓取请求解耦

- **问题与源码**。每次抓取为全部实例入队，固定 5 个工作线程使用无界任务队列，`allOf().join()` 等待全部任务。实例上限为 1,000，连接与读取各设 3 秒，无整次抓取截止时间。测试在真实 provider 的 HTTP 协作者注入 100 ms 后失败的固定延迟。50 个实例，单抓取执行 50 请求、约 1.05 秒，4 个并发抓取执行 200 请求、约 4.22 秒，工作线程峰值均为 5。此结果直接体现重复扇出和排队，真实网络时延需另行校准。现有 !876 与 !885 已完成 Prometheus 出口及协议解耦。[调度与等待](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-registry/src/main/java/qingzhou/registry/monitor/RemoteMetricsProvider.java#L35) · [单请求超时](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-registry/src/main/java/qingzhou/registry/monitor/RemoteMetricsProvider.java#L130) · [实例配额](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-registry/src/main/java/qingzhou/registry/impl/Register.java#L38) · [同步渲染](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-monitor/src/main/java/qingzhou/monitor/MonitorHandler.java#L38) · [受控负载记录](2026-11-28-verification.md#远端指标调度) · [PR !876](https://atomgit.com/openeuler/qingzhou/merge_requests/876) · [PR !885](https://atomgit.com/openeuler/qingzhou/merge_requests/885)
- **工程价值**。多台 Prometheus 或并行监控请求时，慢节点不会阻塞所有抓取，本地指标可按预算返回，慢节点和过期数据可定位。
- **实现方案**。第一阶段为每轮设全局截止时间、限制并发和排队数量、记录超时/丢弃。第二阶段按实例定期刷新不可变快照，同一实例合并在途任务并设置最大数据年龄，抓取只读取快照。公布每实例采集结果和快照年龄，实例移除时清理旧状态。保持指标标签、健康节点样本和现有中心到 Agent 链路契约。
- **模块及成本**。`qingzhou-registry`、`qingzhou-monitor`，按需扩展 `qingzhou-monitor-api` 指标。P1，大，10–15 人日。可先做 4–6 人日的有界采集，再做快照与可观测性。
- **验收**。复跑 50 实例、4 并发、100 ms 延迟的基线，任一刷新窗口内上游请求≤50，热快照 `/monitor/metrics` p95≤250 ms。100 个实例、20% 超时的压测中，抓取持续输出本地和健康节点数据，远端在途、队列及快照大小均有配置上界。过期实例在 TTL 到期后消失，导出结果通过 `promtool check metrics`。

### QZ-06 Redis 审计文件有界保留与可靠恢复

- **问题与源码**。AuditStore 将内存限制在 10,000 条，持久化日志持续 APPEND，重启把整文件读入后才裁剪。告警和指标快照直接覆盖目标文件，缺少原子发布保障。Windows 测试写入 10,020 条，内存及重启后均为 10,000 条，但磁盘仍有 10,020 行、约 1.91 MB。本次测得复读 167 ms，作为小数据基线。快照损坏风险由代码确认，尚未做断电实验。[审计内存保留](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-redis/src/main/java/qingzhou/app/redis/store/AuditStore.java#L10) · [追加和重载](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-redis/src/main/java/qingzhou/app/redis/store/AbstractStore.java#L75) · [快照覆盖写](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-redis/src/main/java/qingzhou/app/redis/store/SnapshotFile.java#L34) · [文件实测](2026-11-28-verification.md#redis-审计存储) · [PR !795](https://atomgit.com/openeuler/qingzhou/merge_requests/795)
- **工程价值**。日常操作高频的 Redis 纳管实例可长期运行，审计磁盘占用可估算，重启无需扫描所有历史。
- **实现方案**。按字节上限轮转日志，分离常驻读窗口和磁盘保留策略。单写入路径或文件锁保障记录顺序，重启仅加载保留段并恢复记录 ID。快照以同目录临时文件和原子替换发布，记录写失败及淘汰。先为审计日志交付，后续复用到命令日志及快照。
- **模块及成本**。`qingzhou-app-redis` 的 store、配置与文档。P1，中大，8–12 人日。
- **验收**。导入旧 JSONL 后连续写入 100,000 条，内存保留最新 10,000 条，磁盘用量符合测试配置上限及单条记录边界。8 个并发写入者完成后无重复 ID 或交错损坏行。对写入及轮转阶段注入中止，重启可读此前提交的记录，单条不完整尾部可定位，旧快照在替换失败时保持可读。

### QZ-07 Nacos 服务列表按命名空间和分组纳管

- **问题与源码**。`NacosService` 声明服务名可搜索，但 `page()` 和 `total()` 都未使用查询条件。查询列表及详情也未带服务所在 namespace 和 group。真实轻舟模型连回环替身，搜索 `svc-7` 仍返回 10 个服务，`page()+total()` 产生 2 次列表请求及 10 次逐服务请求。“添加实例”已接收 namespace 参数，读写范围存在差异。[列表、详情和搜索字段](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-nacos/src/main/java/qingzhou/app/nacos/NacosService.java#L14) · [服务 API 参数](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-nacos/src/main/java/qingzhou/app/nacos/NacosApi.java#L85) · [Nacos v1 服务契约](https://nacos.io/en/docs/open-api/) · [回环请求计数](2026-11-28-verification.md#nacos-契约诊断) · [PR !794](https://atomgit.com/openeuler/qingzhou/merge_requests/794)
- **工程价值**。在 dev、test、prod 命名空间中使用相同服务名时，清单、健康详情和操作能够锁定正确目标。浏览长列表不再为每一行查实例。
- **实现方案**。以 namespace、group、name 作为服务定位键，保留 public/默认分组的旧用法。将查询条件用于列表与总数；若目标 v1 接口无法直接按关键词过滤，采用有上限的分页查询并给出准确的计数语义。将实例明细延迟到展开或详情，列表所需计数通过兼容的聚合或缓存获得。先用 Nacos 2.x 的现有 v1 API 确认契约，再决定独立的新版适配需求。
- **模块及成本**。`qingzhou-app-nacos` 的 model、API、测试和示例。可用 QZ-03 的状态映射作为基础。P2，中大，7–11 人日。
- **验收**。集成环境放置 2 个命名空间、2 个分组的同名服务。列表、搜索、详情和指定测试服务的写操作不串范围；跨两页且每页 10 条时，结果与 `total` 一致。普通列表每页最多 2 次列表类请求，实例详情按需请求，默认分组旧用例通过。

### QZ-08 内置应用大页码溢出与分页契约

- **问题与源码**。框架对页长设上限 100，但允许可用 `int` 表示的大正页码。多个内置应用用 `int` 计算 `(pageNum - 1) * pageSize`，总页数计算也有类似加法边界。Redis `AuditStore.query()` 的正常公开入口实测以页码 2,000,000,000、页长 10 调用，抛 `IndexOutOfBoundsException`。仓库正则扫描返回 15 处相似乘法，需要按各调用方上下界处理逐个确认。此前 !719 实现后端分页，!893 增加 AI 翻页提示。[框架分页参数](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/runtime/qingzhou-app-driver/src/main/java/qingzhou/app/driver/DefaultAction.java#L90) · [Redis 分页运算](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/apps/qingzhou-app-redis/src/main/java/qingzhou/app/redis/store/AbstractStore.java#L60) · [AI 总页数计算](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-registry/src/main/java/qingzhou/registry/ai/ModelActionToolsRegistry.java#L251) · [公开入口复现](2026-11-28-verification.md#redis-审计存储) · [PR !719](https://atomgit.com/openeuler/qingzhou/merge_requests/719) · [PR !893](https://atomgit.com/openeuler/qingzhou/merge_requests/893)
- **工程价值**。REST 集成、AI 翻页和界面输入遇到超范围页时，结果一致且可预测。
- **实现方案**。统一应用内存分页的长整数偏移与出界语义。仅在确认索引处于容器范围后转换为 `int`。对可直接下推分页的 API 保留服务端分页方式，AI 页数计算复用同一边界定义。保留默认参数和现有总数字段语义。
- **模块及成本**。`qingzhou-app-driver`、`qingzhou-registry` 与 Redis、Demo、Tomcat、Nginx、ZooKeeper 内置应用。P2，中，4–6 人日；可先交付共用逻辑与示例回归。
- **验收**。页码 1、末页、末页+1、2,000,000,000、`Integer.MAX_VALUE` 和非法输入均有结果断言。至少覆盖一个远程分页应用及两个本地分页应用。大正页码按契约返回空集合或明确客户端错误，REST/MCP 常规页面的条目和总数一致。

### QZ-09 最小 openEuler 发布基线与包级 CI

- **问题与源码**。项目内测试集中在组件层，启动、发行包、静态资源和插件加载之间缺少仓库内可重复回归。使用手册的环境条目列 JDK 与 Maven，最小 openEuler 实测还需要明确 AWT 无头模式与字体依赖。编译器使用 `source/target=8`，产品组装再从 PATH 单独调用 `java`、`javac`。已有历史构建兼容修复和旧 Gitee 流水线入口，需要确认社区现有门禁可承载的平台。[平台手册](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/docs/%E8%BD%BB%E8%88%9F%E5%B9%B3%E5%8F%B0%E4%BD%BF%E7%94%A8%E6%89%8B%E5%86%8C.md#L6) · [编译器和测试插件](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/pom.xml#L81) · [实际打包入口](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/qingzhou/pom.xml#L57) · [验证码 AWT 与字体](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/components/qingzhou-image/src/main/java/qingzhou/image/impl/CaptchaImpl.java#L18) · [PR !618](https://atomgit.com/openeuler/qingzhou/merge_requests/618) · [旧 Gitee 流水线入口](https://gitee.com/openeuler/qingzhou/gitee_go)
- **工程价值**。贡献者从干净环境可以可靠构建和启动。升级、中文安装路径、图形字体以及真实包中的 OSGi 装配可在合入前回归。
- **实现方案**。先把最小系统依赖、默认无头策略和本地包级冒烟收敛成可复用命令。固定 Maven/插件版本，明确组装使用同一 JDK，必要时增加 Java 8 API 兼容检查。接入社区已有门禁或可用 runner，上传 Surefire、覆盖和启动失败日志。Windows 含空格中文路径作为已通过的回归夹具，Linux 加入无桌面环境。
- **模块及成本**。根 POM、组装、`qingzhou-image`、产品脚本和文档。P1，中，5–8 人日。分为最小运行基线和门禁集成两项可独立操作。
- **验收**。openEuler x86_64 和 Windows 各通过 JDK 8、21 两套基线矩阵，至少一个 release 产物完成初始化、启动、访问入口 JS、登录、受保护 REST 请求、MCP 初始化及正常停机。Linux 无 X 服务时验证码和二维码均可生成。门禁可因缺失 jar、遗漏静态资源或错用 JDK 失败并给出诊断。

### QZ-10 前端构建来源、依赖锁定与兼容冒烟

- **问题与源码**。`qingzhou-web` 将已有 `webapp` 资源原样纳入 bundle，HTML 引用构建后的散列文件。审计提交及可达历史检索未找到 Vue 源文件、前端包清单和锁文件。包级冒烟能证明当前产物可访问，无法从当前仓库重建它。历史 !927、!938 的前端修复均应作为兼容回归场景。[Web 打包配置](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-web/pom.xml#L21) · [构建产物引用](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-web/src/main/resources/webapp/index.html#L17) · [前后端分离部署](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/docs/%E8%BD%BB%E8%88%9F%E5%B9%B3%E5%8F%B0%E4%BD%BF%E7%94%A8%E6%89%8B%E5%86%8C.md#L94) · [PR !927](https://atomgit.com/openeuler/qingzhou/merge_requests/927) · [PR !938](https://atomgit.com/openeuler/qingzhou/merge_requests/938)
- **工程价值**。前端问题可从源码定位，依赖升级、API 兼容及发布产物有可追溯来源。
- **实现方案**。先由导师确认前端源仓、许可和可公开范围。有独立源仓时固定可访问的 commit、依赖锁文件和构建环境，建立校验清单及生成产物的构建入口；源代码适合并仓时再讨论统一提交。补两类发布验收，校验入口资源完整性，以及从真实浏览器走登录、应用列表和模型动作。
- **模块及成本**。`qingzhou-web`、组装及前端源仓或依赖清单。P2，大，10–15 人日。前端源仓不可访问时可独立交付产物来源清单与 API 冒烟，约 3–4 人日。
- **验收**。两个干净环境按固定配置产出同一 SHA-256 资源清单；独立构建失败时给出缺失步骤。控制台冒烟覆盖普通根路径与文档支持的反代部署，API 和静态资源无意外 404。未授权源仓访问时，验收范围以可复核产物清单和公开接口为限。

### QZ-11 Agent 主备续期恢复与切换可观测性

- **问题与源码**。全部非 200/304 都走下一中心。当前中心失去续期记录且仍在线时，Agent 收到旧续期的普通 400，先转备用，再在下一心跳周期向主中心完整注册。以可控中心响应驱动真实 Heartbeat，观察到主注册 200、主续期 400、备注册 200、主再注册 200。该续期故障序列来自组件级验证。另在 openEuler 的真实 release 产物上验证主中心停止、备中心接管及主中心恢复后的回切，单次观测约为 3.6 秒和 5.9 秒。现有主备功能由提交 `87967b93` 引入。[心跳分支及状态](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-agent/src/main/java/qingzhou/agent/Heartbeat.java#L130) · [中心续期入口](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/modules/qingzhou-registry/src/main/java/qingzhou/registry/impl/Register.java#L138) · [主备配置](https://atomgit.com/openeuler/qingzhou/blob/c3f880be34467c00d67be5772d6ace5962d65cd2/docs/qingzhou.properties.readme#L74) · [状态序列](2026-11-28-verification.md#agent-续期序列) · [三进程冒烟](2026-11-28-verification.md#发行包冒烟) · [主备提交](https://atomgit.com/openeuler/qingzhou/commit/87967b93)
- **工程价值**。中心滚动重启或重建注册表时减少无意义切换。区分接管、主动回切、注册恢复与网络故障。
- **实现方案**。单独定义续期、完整注册、故障转移与回切状态。保持普通 400 的语义，评估对当前中心做一次有预算的完整注册，由现有认证与返回验证确定结果。为整轮心跳设限并标记中心状态，同时兼容老中心。
- **模块及成本**。`qingzhou-agent`、`qingzhou-registry`、主备配置和三进程模板。P2，中大，7–10 人日。与 !924 的证书改动共同触及 HTTP 调用处，实施前复核合并顺序。[PR !924](https://atomgit.com/openeuler/qingzhou/merge_requests/924)
- **验收**。双中心 + Agent 场景覆盖主重启、主不可达、备不可达、主恢复和连续抖动。主可达且仅租约丢失时，在一轮心跳内重新注册主且备用注册数为 0。主掉线后在 2 个周期内完成接管，回切次数可从指标对账。旧中心可部署混跑，注册和续期契约通过回归。

## 5. 推荐交付组合

| 实习安排 | 推荐组合 | 交付时验证 |
| --- | --- | --- |
| 首个独立任务 | QZ-01 或 QZ-02 | 复现脚本、修复、跨重启回归 |
| 偏插件开发 | QZ-03 后接 QZ-07 | Nacos 真实实例认证续期、分组隔离和请求计数 |
| 偏系统工程 | QZ-05 为主，QZ-09 为后续 | 有界并发、采集时延、真实包门禁 |
| 偏存储工程 | QZ-06，可搭配 QZ-08 | 保留上限、老格式迁移、重启和异常分页 |

可优先向维护者讨论的普通缺陷标题为「升级解压失败后，同名有效版本包无法恢复启动」「OSGi 对话组件重新激活后，看门狗拒绝调度」和「Nacos 令牌过期后，配置读取未续期」。本次审计材料在 GitHub 镜像供评审。AtomGit 上游尚未创建 Issue、认领正式实习任务或提交 PR。

## 6. 复核材料与范围

- [验证入口和证据索引](2026-11-28-verification.md) 包含验证命令、夹具和关键观测值。两套 Windows 构建与包级冒烟、openEuler 构建、真实 release 主备、MCP 与远端指标验证、Nacos 回环 HTTP、Agent 续期与指标调度的受控协作者实验、Redis 临时文件写入均使用同一个源码提交。应用及组件探针部署在审计目录，生产源码未改动。
- macOS、ARM、完整浏览器交互、真实 Nacos/Redis 服务集群和大规模多机性能未实测。Redis 部分是轻舟自身日志存储，Nacos/Agent/监控夹具用于确认调用契约及成功或失败的状态转换。
- 模块级覆盖结果来自原有测试。仓库中有既有测试和人工记录可复核，但没有组织级流水线配置权限。涉及真实部署 SLO、前端源码开放范围和大模块重构的条目应结合维护侧环境评估。

