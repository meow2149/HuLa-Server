# Redis / MySQL / RocketMQ / Nacos 一键部署（Docker Compose）

## 部署目标

本文档用于在一台 Linux 服务器上通过 `docker-compose.yml` 一键部署 HuLa-Server 运行所需的基础组件（Redis / MySQL / RocketMQ / Nacos 等）。

> [!NOTE]
> 推荐与 `docs/install/服务端部署文档.md` 配合使用：本文仅聚焦“基础组件的 Docker 部署”，不包含 Jenkins/项目启动等内容。

## 前置条件（务必确认）

- **服务器**：Linux
- **软件**：Docker、Docker Compose（或 Docker Compose v2）
- **权限**：可使用 `sudo`

## 一、上传部署目录

将仓库内的 `docs/install/docker/` 整个目录上传到服务器，并放到（示例）：

- **目标目录**：`/home/docker`

目录结构示例：

```bash
/home/docker/docker-compose.yml
/home/docker/env/
/home/docker/rocketmq/
...
```

## 二、修改关键配置（必做）

### 1. 修改环境变量（按需）

- `/home/docker/env/` 下的各类 `*.env`

### 2. 修改 RocketMQ 配置（必做）

重点文件（以你上传后的目录为准）：

- `/home/docker/rocketmq/broker/conf/broker.conf`

必须修改项：

- `brokerIP1`：改成你的服务器 **对外可达 IP**

> [!WARNING]
> `brokerIP1` 不正确时，常见现象是：服务启动成功但消息收发异常、客户端连接失败等。

## 三、启动容器

### 1. 给 RocketMQ 目录授权（避免写入失败）

```bash
sudo chmod -R 777 /home/docker/rocketmq
```

### 2. 启动

```bash
cd /home/docker && docker-compose up -d
```

> [!TIP]
> 你可以用 `docker ps` 查看容器是否全部启动；用 `docker logs <容器名>` 查看启动失败原因。

## 四、初始化 Nacos（否则 Nacos 可能无法启动）

### 1. 导入 Nacos 数据库表结构

- SQL 文件：`docs/install/mysql-schema.sql`

### 2. 导入 Nacos 命名空间/配置（可选）

- 配置文件：`docs/install/nacos/nacos_config.zip`

> [!NOTE]
> 导入方式取决于你的 Nacos 部署形态与版本；若你已在其他环境维护 Nacos 配置，可忽略本步骤。
