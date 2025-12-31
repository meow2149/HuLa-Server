# 设计思想：API 外观 + 单体/微服务双实现

## 背景

本项目**立志**打造为一套代码同时兼容“单体版”和“微服务版”的项目：两者业务功能一致，但实现方式不同——前者使用 Spring Boot 构建，后者使用 Spring Cloud 构建。

## 核心问题

微服务模式下存在“跨服务”的接口调用，而单体模式下没有“跨服务”概念：如何让**同一段业务代码**在微服务版走 OpenFeign 跨服务调用，在单体版走本地实现调用？

## 解决方案：接口下沉 + 双实现

我们将服务能力抽象到 `luohuo-xxx-api`，并分别提供单体/微服务两套实现：

- **`luohuo-xxx-api`**：接口层（对外暴露服务能力）
- **`luohuo-xxx-boot-impl`**：单体版实现（本地接口方式）
- **`luohuo-xxx-cloud-impl`**：微服务版实现（OpenFeign/跨服务调用方式）

依赖约束（避免实现冲突）：

- 单体版 server（例如 `luohuo-boot-server`）只引入 `luohuo-xxx-boot-impl`（不要引入 `luohuo-xxx-cloud-impl`）
- 微服务版 server（各 `*-server` 模块）只引入 `luohuo-xxx-cloud-impl`（不要引入 `luohuo-xxx-boot-impl`）

最终效果：业务侧只依赖 `luohuo-xxx-api` 面向接口编程，通过依赖组合选择运行形态，实现代码复用与形态切换。
