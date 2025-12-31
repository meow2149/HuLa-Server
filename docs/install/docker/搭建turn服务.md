# 搭建 TURN 服务（coturn）部署指南

## 部署目标

本文档用于在一台 Linux 服务器上部署并启动 **coturn（TURN/STUN）**，供 HuLa 的音视频/语音通话在复杂网络（对称 NAT、企业网、UDP 受限等）下通过中继连通。

> [!NOTE]
> 推荐使用普通用户 + `sudo` 操作，不建议长期使用 root 直接登录执行命令。

## 前置条件（务必确认）

- **服务器**：Linux（Ubuntu / Debian / CentOS 均可）
- **权限**：可使用 `sudo`
- **网络**：有公网 IP（或可被外网访问的 NAT 映射）；已确定服务器 **内网 IP** 与 **公网 IP**
- **域名（可选）**：若你希望使用域名作为 `realm`，请提前解析到该服务器公网 IP

## 端口放行清单（建议先做）

> [!WARNING]
> 如果你用的是云服务器：需要同时在 **云厂商安全组** 与 **服务器防火墙/面板（1panel、宝塔等）** 放行端口，否则服务起来也无法正常走 relay。

- **TURN/STUN（必开）**：`3478/tcp`、`3478/udp`
- **TURN TLS/DTLS（可选）**：`5349/tcp`、`5349/udp`
- **中继端口范围（强烈建议显式配置并放行）**：建议 `49152-65535/tcp`、`49152-65535/udp`
  - 如果你在 `turnserver.conf` 里配置了 `min-port/max-port`，以你的配置为准

## 一、安装方式选择

coturn 有两种常见安装方式：

- **方式 A：包管理器安装（推荐）**：更省事、系统自带服务管理（`systemctl`）
- **方式 B：源码编译安装**：适合需要固定版本或系统仓库过旧的场景

> [!TIP]
> 本仓库提供了配置模板：`docs/install/docker/turn/turnserver.conf`，无论你用哪种安装方式都可复用。

## 二、方式 A：包管理器安装（推荐）

### 1. 安装 coturn

Ubuntu / Debian：

```bash
sudo apt update
sudo apt install -y coturn openssl
```

CentOS / Rocky / Alma（不同发行版仓库包名可能略有差异）：

```bash
sudo yum install -y coturn openssl
```

> [!NOTE]
> 如果你的系统仓库没有 `coturn` 包，或版本不符合预期，请跳到「三、方式 B：源码编译安装」。

## 三、方式 B：源码编译安装（可选）

> [!NOTE]
> 以下示例沿用旧文档中的版本：`libevent-2.1.12-stable`、`coturn-4.5.1.1`。你也可以换成更新版本，但请同步调整文件名。

### 1. 安装编译依赖

CentOS：

```bash
sudo yum groupinstall -y "Development Tools"
sudo yum install -y openssl-devel
```

Ubuntu / Debian：

```bash
sudo apt update
sudo apt install -y build-essential libssl-dev
```

### 2. 安装 libevent

```bash
tar -zxvf libevent-2.1.12-stable.tar.gz
cd libevent-2.1.12-stable/
./configure
make -j"$(nproc)"
sudo make install
```

### 3. 编译安装 coturn

```bash
tar -zxvf coturn-4.5.1.1.tar.gz
cd coturn-4.5.1.1
./configure
make -j"$(nproc)"
sudo make install
```

## 四、证书与 DH 参数（开启 TLS/DTLS 才需要）

> [!TIP]
> 如果你只使用 `3478`（非 TLS/DTLS），可先跳过本节；但生产建议尽量规范化配置，减少客户端兼容问题。

### 1. 生成自签证书（示例）

```bash
sudo openssl req -x509 -newkey rsa:2048 \
  -keyout /etc/turn_server_pkey.pem \
  -out /etc/turn_server_cert.pem \
  -days 3650 -nodes
```

### 2. 生成 DH 参数文件（与仓库模板一致）

> [!NOTE]
> 生成 DH 参数可能需要几十秒到几分钟不等。

```bash
sudo mkdir -p /etc/coturn
sudo openssl dhparam -out /etc/coturn/dhparam.pem 2048
```

## 五、配置 turnserver.conf

本仓库内置模板：`docs/install/docker/turn/turnserver.conf`

### 1. 放置配置文件

#### 1.1 包管理器安装（常见路径）

不同系统可能在以下路径之一读取配置：

- `/etc/turnserver.conf`
- `/etc/turnserver/turnserver.conf`

你可以先把模板复制过去：

```bash
sudo cp -f docs/install/docker/turn/turnserver.conf /etc/turnserver.conf
```

#### 1.2 源码安装（旧文档路径）

```bash
sudo cp -f docs/install/docker/turn/turnserver.conf /usr/local/etc/turnserver.conf
```

### 2. 必改项说明（按你的服务器填写）

打开配置文件后，至少确认并修改这些字段：

- **`listening-ip`**：服务器内网 IP（例如 `172.16.0.4`）
- **`external-ip`**：服务器公网 IP（如果在 NAT 后面，可用 `公网IP/内网IP` 的映射形式）
- **`realm`**：建议填域名（示例 `hulaspark.com`），需与客户端配置一致
- **账号密码（二选一）**：
  - **静态用户**：配置 `user=用户名:密码`（模板已包含示例）
  - **turnadmin 用户库**：用 `turnadmin -a` 创建（见下一节）

> [!WARNING]
> `external-ip` 不正确是最常见问题之一：服务看似启动正常，但客户端拿不到可用的 `relay` 候选或连通性极差。

## 六、（可选）使用 turnadmin 创建用户

如果你选择“用户库”的方式（而不是在配置文件里写 `user=...`），可用以下命令创建用户：

```bash
sudo turnadmin -a -u chr -p 123456 -r hulaspark.com
```

> [!NOTE]
> 上面的示例账号密码与仓库模板 `turn/turnserver.conf` 保持一致；如果你改了配置文件里的账号密码，请同步修改这里。

## 七、启动与自启

### 1. 启动（通用：直接运行 turnserver）

#### 1.1 源码安装（常见）

```bash
sudo turnserver -c /usr/local/etc/turnserver.conf -o -a -f
```

#### 1.2 包管理器安装（不同系统略有差异）

```bash
sudo systemctl enable coturn
sudo systemctl restart coturn
sudo systemctl status coturn --no-pager
```

> [!TIP]
> 若你的系统服务名不是 `coturn`，以 `systemctl list-unit-files | grep -i turn` 的结果为准。

### 2. 检查监听端口

```bash
sudo ss -lntup | grep -E ":(3478|5349)\b" || true
```

## 八、验证服务（Trickle ICE）

- 测试页面：`https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/`
- 典型填写示例（以你的公网 IP / 域名为准）：
  - **TURN（UDP）**：`turn:<你的公网IP>:3478?transport=udp`
  - **TURN（TCP）**：`turn:<你的公网IP>:3478?transport=tcp`
  - **用户名/密码**：与 `turnserver.conf` 的 `user=...` 或 `turnadmin` 创建的一致

结果解释（页面返回的候选类型）：

- **host**：本地直连（局域网内常见）
- **srflx**：公网映射/打洞结果（不一定总有）
- **relay**：走 TURN 中继（我们最关注；网络受限时能拿到 relay 并连通才算成功）

![img_13.png](../image/img_13.png)

## 九、常见问题（排错）

- **拿不到 relay 候选**：
  - 检查云安全组/防火墙是否放行：`3478` + **中继端口范围**（例如 `49152-65535`）
  - 检查 `external-ip` 是否正确（NAT 场景建议用 `公网IP/内网IP`）
- **日志出现** `Empty cli-password ... telnet cli interface is disabled`：
  - 这是提示信息，不影响基础服务；模板已设置 `cli-password`，或你也可以配置 `no-cli` 禁用 CLI
- **证书相关报错**：
  - 确认 `cert/pkey/dh-file` 路径存在且 turnserver 进程有权限读取
