# CertiControl 控制系统诊断

轻舟原生应用，用于登记连续时间线性控制系统、检查模型健康状态和分析单通道失效。数值诊断逻辑源于 [CertiControl](https://github.com/dongxuelian2/CertiControl)，使用 Java 8 和 Apache Commons Math 实现。

## 构建和部署

在轻舟源码根目录执行：

```sh
mvn -pl apps/qingzhou-app-certicontrol -am clean package
```

将生成的应用 jar 放入轻舟实例的 apps 目录，重启实例。在管理员管控台打开 CertiControl → 系统模型，新增模型即可自动计算诊断结果。模型保存于实例根目录的 certicontrol/systems.properties。

## 示例

双状态振荡系统：A = -1,-2;2,-1，B = 1;0，C = 1,0，K 留空。可控、可观，开环 Hurwitz，谱裕度 1。

双执行器系统：A = 1,0;0,-2，B = 1,0;0,1，C = 1,0;0,1，K = 3,0;0,0。闭环 A-BK 稳定，第一执行器失效后闭环不稳定。点击单通道故障分析查看所有输入、输出通道失效结果。

## 诊断

- 可控矩阵 [B,AB,...]、可观矩阵 [C;CA;...]：SVD 秩、奇异值及阈值，并以 PBH 交叉检查；近阈值或判据冲突返回 INCONCLUSIVE。
- 开环及可选状态反馈闭环：极点、带符号谱裕度、Hurwitz 判定；虚轴附近单独分类。
- Hurwitz 系统：求解 A^T P + PA = -I，检查残差与 P 的正定性。
- 单输入或输出通道失效：逐列或逐行置零，重新计算可控性/可观性；已给定 K 时计算执行器失效后的闭环谱裕度。
- 诊断和故障分析由轻舟 ModelAction 暴露给管控台、REST 与 MCP；Monitor 读取登记模型的谱裕度。

矩阵使用分号或换行分行，逗号或空格分列；最多 12×12。应用管理已登记的数学模型，数值指标来自模型参数，实际设备运行数据需由设备侧提供。状态反馈假定完整状态可用，传感器失效分析针对可观性。

本模块实现 CertiControl 的数值诊断子集，使用原项目同样的矩阵秩及谱判据；符号计算、LQR 求解与 Kalman 分解由原工具提供。
