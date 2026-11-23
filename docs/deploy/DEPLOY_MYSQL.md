# ECS 本机 MySQL 5.7 部署与运维

> 适用：阿里云 ECS（Alibaba Cloud Linux 3），与 `tea-admin` / `server-api` **同机**部署。  
> 业务库名约定：**`tea`**。两端仅共享库与 Redis，不共享代码包。  
> 本文以 **全新安装 MySQL 5.7** 为主线；若机器上已有 `mysqld.service`，可从「常用运维 / 备份」章节直接使用。

**安全底线：** 安全组 **不要** 对公网放行 `3306`；生产建议只允许本机 `127.0.0.1`（或 VPC 内网）访问。

---

## 1. 环境与版本约定

| 项 | 约定 |
|----|------|
| OS | Alibaba Cloud Linux 3（yum/dnf 系） |
| MySQL | **5.7**（与当前生产及 admin 驱动习惯一致） |
| 服务名 | `mysqld.service` |
| 默认数据目录 | `/var/lib/mysql` |
| 业务库 | `tea` |
| 字符集 | `utf8mb4` |

> Alibaba Cloud Linux 3 偏 EL8 兼容。下文用 **MySQL 官方 5.7 Community Yum 源（el7 包）** 是云上常见装法之一。若官方链接变更，以 [MySQL Yum 仓库说明](https://dev.mysql.com/doc/mysql-yum-repo/en/) 为准。  
> **已安装过的机器不要重复执行「初始化 / 改临时密码」步骤**，否则可能损坏数据。

---

## 2. 全新安装（一步步）

### 2.1 安装前检查

```bash
# 看本机是否已有 mysqld / mysql 服务（有则勿重复装，改去看运维章节）
systemctl status mysqld --no-pager 2>/dev/null || systemctl status mysql --no-pager 2>/dev/null

# 看 3306 是否已被占用
ss -lntp | grep 3306

# 看磁盘空间（数据目录通常在根分区或 /data）
df -h
```

### 2.2 安装依赖与仓库

```bash
# 安装下载与本地装 rpm 所需工具
yum install -y wget yum-utils

# 进入可写目录，准备下载仓库安装包
cd /usr/local/src

# 下载 MySQL 5.7 社区版 yum 仓库包（文件名以官网当前为准，示例常用 el7）
# 也可从 https://dev.mysql.com/downloads/repo/yum/ 获取最新链接
wget https://dev.mysql.com/get/mysql57-community-release-el7-11.noarch.rpm

# 安装仓库配置（会在 /etc/yum.repos.d/ 下生成 mysql 相关 .repo）
rpm -ivh mysql57-community-release-el7-11.noarch.rpm

# （可选）若 GPG 校验失败，可先导入密钥或按报错提示处理后再装
# rpm --import https://repo.mysql.com/RPM-GPG-KEY-mysql-2022
```

若 `yum install` 时提示模块/过滤冲突，可先查看启用情况：

```bash
# 查看已启用的 mysql 相关仓库与版本流（不同系统输出略有差异）
yum repolist enabled | grep -i mysql
```

确保 **mysql57-community** 启用，**mysql80-community** 等更高主版本禁用（避免误装 8.0）：

```bash
# 启用 5.7 仓库、禁用 8.0 仓库（yum-config-manager 来自 yum-utils）
yum-config-manager --enable mysql57-community
yum-config-manager --disable mysql80-community
```

### 2.3 安装 MySQL Server

```bash
# 安装 MySQL 5.7 服务端（含 mysqld、客户端工具、systemd unit）
yum install -y mysql-community-server

# 确认版本（应类似 5.7.x）
mysqld --version
# 或
mysql --version
```

### 2.4 启动并设置开机自启

```bash
# 重新加载 systemd（一般装完已有 unit，稳妥起见可执行）
systemctl daemon-reload

# 启动 MySQL，并写入开机自启
systemctl enable --now mysqld

# 查看是否 active (running)
systemctl status mysqld --no-pager
```

首次启动会初始化数据目录，并生成 **临时 root 密码**（写在错误日志里）。

### 2.5 取出临时 root 密码并登录

```bash
# 从 mysqld 日志中抓取 temporary password（首次初始化后才有）
grep 'temporary password' /var/log/mysqld.log

# 使用临时密码登录（-p 后直接回车，再粘贴密码更安全）
mysql -uroot -p
```

登录成功后进入 `mysql>` 提示符。

### 2.6 修改 root 密码并做基础加固

在 `mysql>` 中执行（把密码换成自己的强密码，需满足 5.7 默认密码策略，通常含大小写、数字、特殊字符）：

```sql
-- 修改 root 密码（5.7 常用写法）
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的强密码';

-- 刷新权限，使变更立即生效
FLUSH PRIVILEGES;

-- 退出客户端
EXIT;
```

也可用官方辅助工具（交互式）：

```bash
# 引导修改 root 密码、是否移除匿名用户、是否禁止 root 远程等
mysql_secure_installation
```

建议：

- 移除匿名用户：是  
- 禁止 root 远程登录：是（同机应用用 `localhost`）  
- 删除 test 库：是  
- 刷新权限表：是  

### 2.7 字符集与绑定地址（推荐）

```bash
# 编辑主配置（Alibaba Cloud / 官方包常见路径如下；若没有则创建）
# 先看已有配置：
ls -l /etc/my.cnf /etc/my.cnf.d/ 2>/dev/null
```

编辑 `/etc/my.cnf`（或 `/etc/my.cnf.d/eco-tea.cnf`），建议包含：

```ini
[mysqld]
datadir=/var/lib/mysql
socket=/var/lib/mysql/mysql.sock
pid-file=/var/run/mysqld/mysqld.pid

# 只监听本机（同机 Java 连接 127.0.0.1）；若必须内网其它机器访问，改为内网 IP 并收紧安全组
bind-address=127.0.0.1
port=3306

character-set-server=utf8mb4
collation-server=utf8mb4_general_ci

# 4C8G 同机跑 admin+api+redis 时的保守示例；可按监控再调
innodb_buffer_pool_size=1G
max_connections=200

[client]
default-character-set=utf8mb4

[mysql]
default-character-set=utf8mb4
```

| 配置项 | 作用 |
|--------|------|
| `bind-address=127.0.0.1` | 只接受本机 TCP 连接 |
| `character-set-server=utf8mb4` | 库默认 utf8mb4，避免表情/生僻字问题 |
| `innodb_buffer_pool_size` | InnoDB 缓冲池，越大读越快，但勿占满整机内存 |

改完后：

```bash
# 检查配置语法是否明显有问题（有的环境提供 mysqld --validate-config，5.7 不一定有）
# 直接重启使配置生效
systemctl restart mysqld

# 确认仍在运行
systemctl status mysqld --no-pager

# 确认监听在本机 3306
ss -lntp | grep 3306
```

### 2.8 创建业务库与应用账号

```bash
# 用新 root 密码登录
mysql -uroot -p
```

```sql
-- 创建业务库，字符集 utf8mb4
CREATE DATABASE IF NOT EXISTS `tea`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 创建仅本机登录的应用账号（示例用户名 tea_app，密码自行替换）
CREATE USER IF NOT EXISTS 'tea_app'@'localhost' IDENTIFIED BY '应用账号强密码';

-- 只授权业务库（按需可改为 ALL，生产更建议精确到 CRUD）
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP
  ON `tea`.* TO 'tea_app'@'localhost';

FLUSH PRIVILEGES;

-- 核对
SHOW DATABASES;
SELECT user, host FROM mysql.user;
EXIT;
```

用应用账号验证：

```bash
# 以应用账号连接并选中 tea 库；成功则会进入 mysql 提示符
mysql -utea_app -p -h127.0.0.1 tea -e "SELECT DATABASE();"
```

> 表结构请按项目 SQL / 业务上线脚本另行导入，本文不展开建表。

### 2.9 应用侧连接示例

`application-prod.yml`（勿提交真实密码到 git）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/tea?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: tea_app
    password: 应用账号强密码
    driver-class-name: com.mysql.jdbc.Driver   # 以各模块实际驱动为准
```

---

## 3. 安装验收清单

```bash
systemctl is-active mysqld          # 期望 active
systemctl is-enabled mysqld         # 期望 enabled
mysqld --version                    # 期望 5.7.x
ss -lntp | grep 3306                # 期望 127.0.0.1:3306 或 ::1
mysql -uroot -p -e "SHOW DATABASES;"   # 应能看到 tea
```

---

## 4. 全库备份与恢复

### 4.1 备份前

```bash
# 建备份目录（-p：父目录不存在则创建）
mkdir -p /data/backup/mysql

# 看 /data 剩余空间，避免备份写满磁盘
df -h /data

# （可选）看库大约体积（MB）
mysql -uroot -p -e "SELECT ROUND(SUM(data_length+index_length)/1024/1024,1) AS MB FROM information_schema.tables;"
```

### 4.2 逻辑全库备份（推荐）

```bash
# mysqldump：导出库结构+数据
# --all-databases：全实例
# --single-transaction：InnoDB 热备，减少锁表
# --routines / --triggers / --events：过程、触发器、事件
# 管道给 gzip 压缩；文件名带时间戳
# 注意：若未开启 binlog，不要加 --master-data（会报 Binlogging on server not active）
mysqldump -uroot -p \
  --all-databases \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --default-character-set=utf8mb4 \
  | gzip > /data/backup/mysql/full_$(date +%Y%m%d_%H%M%S).sql.gz
```

校验：

```bash
# 列出备份文件及大小（-h 人类可读）；过小（如几百字节）多半失败残留
ls -lh /data/backup/mysql/

# 测试 gzip 是否完整可解；成功通常无输出
gzip -t /data/backup/mysql/full_*.sql.gz
```

**务必再拷一份到机外**（本机 scp、OSS 等）。只放在同一块云盘上，盘故障时备份会一起丢。

### 4.3 恢复（慎用）

```bash
# 恢复前建议停应用，避免写入冲突
systemctl stop tea-admin
# systemctl stop tea-api

# 解压并导入到 MySQL（会覆盖同名对象，生产务必确认备份文件）
gunzip < /data/backup/mysql/full_YYYYMMDD_HHMMSS.sql.gz | mysql -uroot -p

systemctl start tea-admin
```

### 4.4 云盘快照（升配 / 重装前）

在阿里云控制台对 **系统盘 / 数据盘** 创建快照，与 `mysqldump` 互补：快照可整盘回滚；dump 可单库逻辑恢复。

---

## 5. 常用运维命令

### 5.1 服务启停

```bash
systemctl status mysqld --no-pager   # 看运行状态与最近日志
systemctl start mysqld               # 启动
systemctl stop mysqld                # 停止（会中断所有连接）
systemctl restart mysqld             # 重启（短暂停服）
systemctl is-enabled mysqld          # 是否开机自启
journalctl -u mysqld -n 50 --no-pager  # 最近 50 条服务日志
journalctl -u mysqld -f              # 跟踪实时日志
```

错误日志文件（官方包常见路径）：

```bash
# 查看错误日志尾部（路径以本机 my.cnf / 实际为准）
tail -n 100 /var/log/mysqld.log
```

### 5.2 登录与基础查询

```bash
mysql -uroot -p                      # 交互登录
mysql -uroot -p -e "SELECT 1;"       # 非交互执行一条 SQL
mysql -uroot -p -e "SHOW DATABASES;" # 列出库
mysql -uroot -p tea                  # 登录并默认选中 tea 库
```

在 `mysql>` 内：

```sql
SHOW VARIABLES LIKE 'character_set%';  -- 字符集相关变量
SHOW VARIABLES LIKE 'bind_address';     -- 绑定地址
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW PROCESSLIST;                      -- 当前连接与正在执行的语句
SHOW STATUS LIKE 'Threads_connected';  -- 当前连接数
```

### 5.3 进程与端口、资源

```bash
ss -lntp | grep 3306                 # 谁在监听 3306
ps aux | grep mysqld | grep -v grep  # mysqld 进程与大致内存
free -h                              # 整机内存
df -h /var/lib/mysql                 # 数据目录所在分区空间
```

### 5.4 权限与用户（排查连不上时）

```sql
SELECT user, host, plugin FROM mysql.user;
SHOW GRANTS FOR 'tea_app'@'localhost';
```

---

## 6. 与本项目相关的注意点

- 库名 **`tea`**；chai 域表与旧 tea 表可同库并行（见上线清单）。
- admin / api 各自配置数据源，**不要**把生产密码写进 git。
- 同机部署时应用用 `127.0.0.1` 或 `localhost`；改 `bind-address` 后需重启 `mysqld`。
- 当前环境若 **未开 binlog**，备份不要加 `--master-data`。
- 升配 ECS **不会**自动清空 MySQL 数据，但仍建议先快照 + dump。

---

## 7. 快速清单（全新机）

- [ ] 确认无旧 mysqld / 3306 占用
- [ ] 安装 mysql57 仓库并 `yum install mysql-community-server`
- [ ] `systemctl enable --now mysqld`
- [ ] 改 root 密码 / `mysql_secure_installation`
- [ ] `my.cnf`：utf8mb4、`bind-address`、合理 `innodb_buffer_pool_size`
- [ ] 创建库 `tea` 与应用账号并授权
- [ ] 安全组未放行 3306
- [ ] 导入业务表结构 / 数据
- [ ] admin、api 数据源指向本机并验证

---

## 8. 已有实例（跳过安装时）

本仓库对应生产机典型状态示例：

- 单元：`mysqld.service` → `enabled` + `active`
- 业务库：`tea`
- 备份目录建议：`/data/backup/mysql`

直接使用第 4、5 节做备份与日常运维即可。
