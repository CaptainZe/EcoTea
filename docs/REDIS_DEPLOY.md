# ECS 本机 Redis 部署与运维

> 适用：阿里云 ECS（Alibaba Cloud Linux 3），与 `tea-admin` / `server-api` **同机**部署。  
> 目标参数：**端口 6379**、**内存 256MB**、**淘汰策略 allkeys-lru**、**不持久化**（纯缓存，重启丢数据）。  
> 下文已按本机实装经验校正（精简 conf、`Type=simple`）。

admin / api **仅共享** MySQL 与 Redis，业务 key 前缀见各模块 `RedisConstant`（如 `ecotea_`）。  
应用侧业务缓存约定（字典 / 品牌 / 保质期等）见 `REDIS_CACHE.md`（若有）。

---

## 1. 版本选择

从 [Redis 官方 releases](https://download.redis.io/releases/) 选 **稳定版补丁**，不要用带 `-rc` / `-m01` 的包。

| 推荐 | 说明 |
|------|------|
| `redis-8.8.2.tar.gz` | 推荐（下文示例，已在本机验证） |
| `redis-8.10.1.tar.gz` | 更新的稳定线亦可 |
| `redis-8.2.9.tar.gz` | 偏保守可选 |

---

## 2. 编译安装

```bash
# 安装编译工具链与下载/解压工具（-y 表示无需交互确认）
# 若已安装，yum 可能只做小版本升级，属正常
yum install -y gcc make wget tar

# 进入源码常用目录（需 root 或有写权限）
cd /usr/local/src

# 下载 Redis 源码包（版本号可换成 8.10.1 等）
wget https://download.redis.io/releases/redis-8.8.2.tar.gz

# 解压：x=解压，z=gzip，f=指定文件名
tar xzf redis-8.8.2.tar.gz

# 进入解压后的源码目录
cd redis-8.8.2

# 编译；-j$(nproc) 按 CPU 核数并行，加快编译
# 结束时若提示 Hint: run 'make test'，生产装机可跳过（完整测试很久）
make -j$(nproc)

# 安装二进制到系统路径（默认 /usr/local/bin：redis-server、redis-cli）
make install

# 确认安装成功
which redis-server redis-cli
redis-server --version

# 创建系统用户 redis：-r 系统账户，-s /sbin/nologin 禁止登录 shell
# 若用户已存在则忽略错误（|| true）
useradd -r -s /sbin/nologin redis 2>/dev/null || true

# 创建配置目录、数据目录、日志目录
mkdir -p /etc/redis /var/lib/redis /var/log/redis

# 把数据/日志目录属主改成 redis，否则服务以 redis 用户启动会写不进去
chown -R redis:redis /var/lib/redis /var/log/redis
```

若换版本，把路径与包名中的 `8.8.2` 改成实际版本号即可。

---

## 3. 配置文件（推荐：精简覆盖）

官方自带的 `redis.conf` 注释极多，**生产可直接写成精简文件**（未写的项走 Redis 内置默认值），不必逐项在大文件里改。

```bash
# 用 heredoc 覆盖写入精简配置（把「换成强密码」改成真实密码后再执行）
cat > /etc/redis/redis.conf << 'EOF'
bind 127.0.0.1
protected-mode yes
port 6379
daemonize no
supervised systemd

dir /var/lib/redis
logfile /var/log/redis/redis-server.log
pidfile /var/run/redis/redis-server.pid

requirepass 换成强密码

maxmemory 256mb
maxmemory-policy allkeys-lru

save ""
appendonly no
EOF

# 核对内容
cat /etc/redis/redis.conf
```

| 配置项 | 值 | 作用 |
|--------|-----|------|
| `bind` | `127.0.0.1` | 只监听本机，外网无法直连 |
| `protected-mode` | `yes` | 保护模式，降低误暴露风险 |
| `port` | `6379` | 服务端口 |
| `daemonize` | `no` | 不自己后台化，交给 systemd 管进程 |
| `supervised` | `systemd` | 与 systemd 配合（本方案配合 `Type=simple` 即可） |
| `dir` | `/var/lib/redis` | 工作目录（本方案无持久化文件，仍建议保留） |
| `logfile` | `...` | 日志路径 |
| `pidfile` | `...` | PID 文件路径（配合 `RuntimeDirectory=redis`） |
| `requirepass` | 强密码 | 客户端需认证 |
| `maxmemory` | `256mb` | 内存上限 |
| `maxmemory-policy` | `allkeys-lru` | 满时按 LRU 淘汰任意 key |
| `save ""` | — | 关闭 RDB 定时快照 |
| `appendonly` | `no` | 关闭 AOF |

**安全：**

- 只绑 `127.0.0.1`，不对公网监听。
- 阿里云安全组 **不要** 对公网放行 `6379`。
- 生产必须设置 `requirepass`；勿把密码贴到公开聊天或提交进 git。

---

## 4. systemd 托管

新建 `/etc/systemd/system/redis.service`：

```bash
cat > /etc/systemd/system/redis.service << 'EOF'
[Unit]
Description=Redis Server
After=network.target

[Service]
Type=simple
User=redis
Group=redis
ExecStart=/usr/local/bin/redis-server /etc/redis/redis.conf
ExecStop=/bin/kill -s TERM $MAINPID
Restart=always
RestartSec=3
LimitNOFILE=10032
RuntimeDirectory=redis
RuntimeDirectoryMode=0755

[Install]
WantedBy=multi-user.target
EOF
```

| 字段 | 作用 |
|------|------|
| `After=network.target` | 网络就绪后再启动 |
| `Type=simple` | **推荐默认**。源码自编译的 Redis 常未带 libsystemd 通知，用 `Type=notify` 会一直停在 `activating (start)` |
| `User` / `Group` | 以 redis 用户运行，降低权限 |
| `ExecStart` | 启动命令与配置文件路径 |
| `ExecStop` | 优雅结束主进程（避免在 unit 里写明文密码） |
| `Restart=always` | 异常退出后自动拉起 |
| `RuntimeDirectory=redis` | 自动创建 `/run/redis`，供 pidfile 等使用 |

启用并启动：

```bash
# 让 systemd 重新读取 unit 文件（新建/修改 service 后必做）
systemctl daemon-reload

# enable：开机自启；--now：同时立刻启动
systemctl enable --now redis

# 查看运行状态；期望 Active: active (running)
systemctl status redis --no-pager
```

若误写成 `Type=notify` 且卡在 `activating`：进程可能已在监听（`redis-cli ping` 能通），按下面修正即可：

```bash
systemctl stop redis
sed -i 's/^Type=notify/Type=simple/' /etc/systemd/system/redis.service
systemctl daemon-reload
systemctl start redis
systemctl status redis --no-pager
```

---

## 5. 安装验收

```bash
# 向 Redis 发 PING；-a 指定密码。期望返回 PONG
# （命令行 -a 会有 unsafe 警告，可忽略；生产也可改用 redis-cli 交互 AUTH）
redis-cli -a '你的密码' ping

# 读取内存上限，期望 268435456（即 256MB）
redis-cli -a '你的密码' CONFIG GET maxmemory

# 读取淘汰策略，期望 allkeys-lru
redis-cli -a '你的密码' CONFIG GET maxmemory-policy

# 读取 AOF 开关，期望 no
redis-cli -a '你的密码' CONFIG GET appendonly

# 查看谁在监听 6379：应为 127.0.0.1:6379，不要出现 0.0.0.0:6379
ss -lntp | grep 6379
```

本机实装通过示例：`active (running)` + `PONG` + 仅本机监听 + `maxmemory=268435456` + `allkeys-lru` + `appendonly=no`。

---

## 6. 应用侧配置

`tea-admin` / `server-api` 的 `application-prod.yml`（或外置配置）示例：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 你的密码
    # database: 0   # 可省略，见下
    timeout: 3000ms
```

**关于 `database`：**

- Spring Boot / Spring Data Redis 中 `spring.redis.database` **默认就是 `0`**。
- **不写 `database` 与显式写 `database: 0` 效果相同**，都会连 Redis 的 DB0。
- 只有要用 DB1、DB2… 做隔离时才需要改这个值（本项目同机共享，建议统一用默认 0，靠 key 前缀区分业务）。

改完后重启对应服务：

```bash
systemctl restart tea-admin
# api 部署后：按实际 unit 名重启
# systemctl restart tea-api
```

api 健康检查可看 Redis：`GET http://127.0.0.1:8081/health`。

---

## 7. 常用运维命令

### 服务启停

```bash
systemctl status redis              # 查看是否 running、最近日志摘要
systemctl start redis               # 启动
systemctl stop redis                # 停止
systemctl restart redis             # 重启（不持久化时内存数据会清空）
systemctl is-enabled redis          # 是否开机自启（enabled/disabled）
journalctl -u redis -n 50 --no-pager  # 看该服务最近 50 条日志
journalctl -u redis -f              # 跟踪实时日志（Ctrl+C 退出）
```

### 连通与信息

```bash
redis-cli -a '你的密码' ping           # 连通性探测
redis-cli -a '你的密码' INFO server    # 版本、运行时长等
redis-cli -a '你的密码' INFO memory    # 已用内存、maxmemory 等
redis-cli -a '你的密码' INFO clients   # 客户端连接数
redis-cli -a '你的密码' DBSIZE         # 当前库 key 数量
```

### 配置核对（运行时）

```bash
redis-cli -a '你的密码' CONFIG GET maxmemory         # 内存上限
redis-cli -a '你的密码' CONFIG GET maxmemory-policy  # 淘汰策略
redis-cli -a '你的密码' CONFIG GET appendonly        # 是否 AOF
redis-cli -a '你的密码' CONFIG GET save              # RDB 规则
redis-cli -a '你的密码' CONFIG GET requirepass       # 是否设置了密码（会回显配置值）
```

改 **配置文件** 后一般需 `systemctl restart redis` 才全部生效；部分项可用 `CONFIG SET` 临时改（重启后仍以 conf 为准）。

### 键与调试（慎用）

```bash
# 按前缀渐进扫描（生产大数据量不要用 KEYS *）
redis-cli -a '你的密码' --scan --pattern 'ecotea_*'

redis-cli -a '你的密码' GET 某个key   # 读字符串值
redis-cli -a '你的密码' TTL 某个key   # 剩余过期秒数，-1 永不过期，-2 不存在
redis-cli -a '你的密码' DEL 某个key   # 删除 key

# 清空当前 DB（危险，确认库号后再用）
# redis-cli -a '你的密码' -n 0 FLUSHDB
```

### 监听与资源

```bash
ss -lntp | grep 6379                          # 确认监听地址与端口
ps aux | grep redis-server | grep -v grep     # 看 redis 进程是否在、占用大致内存
free -h                                       # 看整机内存余量
```

---

## 8. 注意与边界

- **不持久化**：`systemctl restart redis` 或机器重启后，缓存全部丢失；应用应能重建缓存 / 可降级。
- **同机部署**：当前绑定本机即可；若以后拆到独立实例，再改 `bind`、安全组与应用 `host`。
- **密码**：勿把生产密码提交进 git；只写在服务器外置配置或环境变量中。
- **与 MySQL 备份无关**：Redis 本方案无 RDB/AOF，升配或重装前无需单独备份 Redis 数据。
- **systemd Type**：本机源码安装请用 `Type=simple`；`Type=notify` 易卡在 `activating`。

---

## 9. 快速清单

- [ ] 编译安装目标版本（如 8.8.2）；可跳过 `make test`
- [ ] 精简 `/etc/redis/redis.conf`：bind / 密码 / 256mb / allkeys-lru / 关持久化
- [ ] `redis.service` 使用 `Type=simple`，enable + start → `active (running)`
- [ ] `ping` 与 CONFIG 验收通过；仅 `127.0.0.1:6379`
- [ ] 安全组未放行 6379
- [ ] admin / api 写入 Redis 连接（`database` 可省略，默认 0）并重启验证
