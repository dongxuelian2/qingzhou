# 轻舟审计验证摘要

审计基线为 `c3f880be34467c00d67be5772d6ace5962d65cd2`。复核记录面向镜像 PR，保留了命令、夹具和关键观测值。完整运行日志及实验脚本保存在本地审计包中。

## 构建与覆盖率

| 环境 | 命令 | 观测 |
| --- | --- | --- |
| Windows 11 x64，Microsoft JDK 21.0.9 | `mvn -B -ntp clean verify` | 48 个 reactor 项目，310 项测试通过 |
| Windows 11 x64，Azul JDK 8u514 | `mvn -B -ntp clean verify` | 48 个 reactor 项目，310 项测试通过 |
| openEuler 24.03 LTS x86_64 WSL，Azul JDK 21.0.13.1 | `mvn -B -ntp -Prelease clean verify` | 最小环境在 `qingzhou-image` 的 2 项测试失败，首个报错为缺少 `libXext.so.6` |
| 同一 openEuler 环境 | 加 `-Djava.awt.headless=true -pl components/qingzhou-image -am verify` | 二维码测试通过，验证码仍报字体配置不可用 |
| 同一 openEuler 环境，隔离安装 Fontconfig、DejaVu 等字体依赖 | 使用无头 JVM 选项运行 `mvn -B -ntp -Prelease clean verify` | 48 个 reactor 项目，310 项测试通过 |

使用 Maven 3.9.11。Surefire 数据按模块读取，TestNG 嵌套导出的重复报告没有计入。JaCoCo 0.8.13 在独立检出上运行。

```sh
mvn -B -ntp org.jacoco:jacoco-maven-plugin:0.8.13:prepare-agent test \
    org.jacoco:jacoco-maven-plugin:0.8.13:report
```

| 覆盖范围 | 已覆盖行 / 总行 | 行覆盖 |
| --- | ---: | ---: |
| 14 个有报告的模块合计 | 2,268 / 5,086 | 44.6% |
| Registry | 227 / 1,335 | 17.0% |
| HTTP Server | 626 / 1,086 | 57.6% |
| AI Memory | 411 / 586 | 70.1% |

14 个模块的分支覆盖合计为 1,062 / 2,995（35.5%）。这些数据是单模块报告的加权汇总。

## 发行包冒烟

Windows 用实际 `start.bat` 启动。JDK 8 和 21 各使用普通路径和中文含空格路径，两种路径均初始化成功，入口 JS 为 200，缺失的 JS 为 404，登录为 200，匿名注册中心访问为 401，登录后为 200。

openEuler 在中文含空格路径执行实际 `init.sh`、`start.sh`。双中心和 Agent 绑定本机回环地址。两轮实际包验证覆盖登录、REST、远端采集和主备切换，第二轮增加 MCP 初始化和工具枚举。

| 环节 | 观测 |
| --- | --- |
| 控制台、入口 JS、登录后 REST | HTTP 200，匿名访问注册中心为 401 |
| MCP | 初始化和 `tools/list` 返回 200，协议版本 2025-03-26，13 个工具 |
| 远端监控 | HTTP 200，输出 45 条远端指标序列 |
| 主备 | 两轮接管约 3.6 秒，主进程恢复后回切约 5.9 秒 |

## 版本包恢复

实际 Windows Launcher 先加载健康版本包。随后使用普通测试条目构造会在解压中途失败的高版本 ZIP。首次失败后验证残留目录存在，覆写为同名有效 ZIP，再检查首次启动。用更高版本号投放同一份有效 ZIP 作为对照。

| 阶段 | 观测 |
| --- | --- |
| 正常版本 | 退出码 0 |
| 解压失败 | 退出码 1，最终版本目录残留 |
| 同名正常包覆盖 | 退出码 1，`ClassNotFoundException: qingzhou.command.Command` |
| 更高版本的正常包 | 退出码 0 |

## AI 对话重新激活

使用真实 `AiChat`，注入模型和 HTTP 响应替身，经 `init()`、`handle()`、`deactivate()` 触发两轮生命周期。

| 激活轮次 | 累计模型调用 | Streams active | 观测 |
| --- | ---: | ---: | --- |
| 第一轮 | 1 | 0 | 正常完成 |
| 第二轮 | 1 | 1 | `RejectedExecutionException` |

## Nacos 契约诊断

使用真实轻舟 HTTP Client 和 Nacos 模型。回环 HTTP 替身模拟 Nacos v1 的令牌及服务目录响应。

- `tokenTtl=1`。到期前可读取配置。到期后服务返回 403，轻舟只发起过 1 次登录，配置读取返回错误响应正文。
- 搜索 `svc-7` 时，列表仍返回全部 10 个测试服务，`total=10`。一次列表与计数共产生 2 次列表请求和 10 次实例详情请求。

## Tomcat 进程探测

分别启动两个真实测试 JVM，传入同一 Tomcat 识别标记，以及普通和含空格的 `-Dcatalina.home`，探测来源仅启用进程扫描。

| 系统 | 普通目录 | 含空格目录 |
| --- | --- | --- |
| Windows 11 | 未命中 | 未命中 |
| openEuler 24.03 LTS | 命中 | 未命中 |

## 远端指标调度

对真实 provider 的 HTTP 协作者注入固定 100 ms 延迟后失败。50 个实例均使用本地测试夹具。

| 并发抓取 | 上游请求数 | 工作线程峰值 | 完成时间 |
| ---: | ---: | ---: | ---: |
| 1 | 50 | 5 | 1,052 ms |
| 4 | 200 | 5 | 4,217 ms |

## Redis 审计存储

通过 `AuditStore` 公开入口，用实际 JSON 实现和临时文件系统写入 10,020 条记录。内存和重启后均保留 10,000 条，磁盘实际 10,020 行，共 1,911,644 字节。这次读回耗时 167 ms。以页码 2,000,000,000、页长 10 查询，抛出 `IndexOutOfBoundsException`。

## Agent 续期序列

使用真实 `Heartbeat` 调度，注入受控中心响应。观测顺序为主中心完整注册 200、主中心续期 400、备中心完整注册 200、主中心完整注册 200。该实验描述续期丢失分支，实际包的主中心停机和恢复结果见上面的发行包冒烟。

## 上游查重

对 AtomGit 两个仓库读取全状态分页列表。轻舟项目覆盖 65 条 Issue 和 953 条 PR。实习仓读取 205 条 Issue，再按标题与项目链接筛出 37 条轻舟相关记录。两条开放 PR 的功能范围及相关历史改动在正文注明。

[轻舟 Issue 列表](https://atomgit.com/openeuler/qingzhou/issues) · [轻舟 PR 列表](https://atomgit.com/openeuler/qingzhou/pulls) · [开源实习 Issue 列表](https://atomgit.com/openeuler/opensource-intern/issues)
