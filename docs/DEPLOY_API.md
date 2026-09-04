# ECS Java 服务部署（tea-api）与通用模板

> 适用：阿里云 ECS（Alibaba Cloud Linux 3），与 `tea-admin` **同机**。  
> 本文以 **tea-api** 为完整示例；文末「通用模板」可供后续其它 Java 服务照抄。  
> 前置：MySQL / Redis / Nginx+SSL 见 `DEPLOY_MYSQL.md`、`DEPLOY_REDIS.md`、`DEPLOY_NGINX_SSL.md`。

对外域名：`https://api.ecotea.cn` → Nginx:443 → `127.0.0.1:8081`。

---

## 1. 约定对照（tea-admin ↔ tea-api）

| 项 | tea-admin | tea-api |
|----|-----------|---------|
| 部署目录 | `/data/www/tea-admin` | `/data/www/tea-api` |
| 环境变量文件 | `env.txt` | `env.txt` |
| 外置配置 | `conf/application.yml` + `application-prod.yml` | 同左 |
| 日志配置 | `conf/logback.xml` | `conf/logback-spring.xml` |
| 日志目录 | `/data/logs/tea-admin` | `/data/logs/tea-api` |
| jar | `tea-admin-1.0.0-RELEASE.jar` | `tea-api-1.0.0-SNAPSHOT.jar` |
| 端口 | 8080 | 8081 |
| 堆内存 | `-Xms512m -Xmx512m` | `-Xms1g -Xmx1g`（对外服务，可按 `free -h` 调整） |
| 运行用户 | `www` / `www` | 同左 |
| systemd | `tea-admin.service` | `tea-api.service` |

启动方式：`EnvironmentFile` + `/usr/bin/java $JAVA_OPTS -jar … $SPRING_OPTS`（**不写 JAVA_HOME**，使用系统 PATH / `/usr/bin/java`）。

---

## 2. 目录与文件布局

```text
/data/www/tea-api/
  env.txt
  tea-api-1.0.0-SNAPSHOT.jar
  conf/
    application.yml
    application-prod.yml
    logback-spring.xml

/data/logs/tea-api/          # logback 写入，必须对 www 可写
```

```bash
mkdir -p /data/www/tea-api/conf
mkdir -p /data/logs/tea-api
```

上传 jar、conf、env 后继续。

---

## 3. 用户 www 与权限（必读，可复用）

### 3.1 原则

| 路径 | 建议属主 | 权限要点 |
|------|----------|----------|
| `/data` | `root:root` | `755`，保证 www **可穿越**（进入子目录） |
| `/data/www/<app>` | 可为 `root:root`（与现网 admin 一致） | 目录 `755`；jar/yml/env `644`，保证 www **可读** |
| `/data/logs/<app>` | **`www:www`（必须）** | 目录 `755` 且属主 www，保证 www **可写日志** |

systemd 中 `User=www` / `Group=www` 时：

- **读** jar、conf、env：其它用户有读权限即可（`root:root` + `644`/`755` 可行，与 tea-admin 相同）。  
- **写** 日志：若 `/data/logs/<app>` 不可写，Logback 会直接导致进程退出（`Permission denied`）。

历史踩坑（tea-api 首次启动失败）：

```text
FileNotFoundException: /data/logs/tea-api/access.YYYY-MM-DD.log (Permission denied)
```

### 3.2 部署时固定执行

```bash
# 日志目录必须对 www 可写
mkdir -p /data/logs/tea-api
chown -R www:www /data/logs/tea-api

# 可选：程序目录也交给 www（更省心）；现网 admin 多为 root:root 亦可
# chown -R www:www /data/www/tea-api

# 校验 www 能写日志（应无报错）
sudo -u www touch /data/logs/tea-api/write-test.log
rm -f /data/logs/tea-api/write-test.log

# 查看路径权限
ls -ld /data /data/logs /data/logs/tea-api
ls -la /data/www/tea-api/
```

期望示例：

```text
drwxr-xr-x  root root  /data
drwxr-xr-x  www  www   /data/logs
drwxr-xr-x  www  www   /data/logs/tea-api
```

### 3.3 确认运行用户存在

```bash
id www
# uid=xxx(www) gid=xxx(www)
```

若不存在，需先按运维规范创建 `www` 用户/组（与 tea-admin 共用同一账号）。

---

## 4. env.txt

文件：`/data/www/tea-api/env.txt`  
风格与 tea-admin 一致：**无 JAVA_HOME**；`SPRING_OPTS` 可多行（行末 `\`）。

```bash
JAVA_OPTS="-server -Xms1g -Xmx1g -XX:+UseG1GC -Dfile.encoding=UTF-8"

SPRING_OPTS="\
--spring.profiles.active=prod \
--spring.config.location=/data/www/tea-api/conf/application.yml,/data/www/tea-api/conf/application-prod.yml \
--logging.config=/data/www/tea-api/conf/logback-spring.xml \
"
```

说明：

- `--spring.config.location=...` 只加载列出的外置 yml（与 admin 相同），公共项与密钥都要写在这两个文件里。  
- `--logging.config=...` 指向外置 **`logback-spring.xml`**。  
- 堆 1g 前建议 `free -h`：同机还有 admin(512m)、MySQL、Redis(256m)、Nginx；机器偏小可改为 `512m`/`768m`。

写入示例：

```bash
cat > /data/www/tea-api/env.txt << 'EOF'
JAVA_OPTS="-server -Xms1g -Xmx1g -XX:+UseG1GC -Dfile.encoding=UTF-8"

SPRING_OPTS="\
--spring.profiles.active=prod \
--spring.config.location=/data/www/tea-api/conf/application.yml,/data/www/tea-api/conf/application-prod.yml \
--logging.config=/data/www/tea-api/conf/logback-spring.xml \
"
EOF
```

---

## 5. 外置配置与日志

- `conf/application.yml`：端口 `8081`、MyBatis 等公共配置。  
- `conf/application-prod.yml`：数据源、Redis、`wx.mp`、`ecotea.beian`、`ecotea.security.api-key` 等（对照仓库 `application-env.yml.example`）。  
- `conf/logback-spring.xml`：`logsDir` 必须为 `/data/logs/tea-api`（与仓库 `server-api/src/main/resources/logback-spring.xml` 一致）。

---

## 6. systemd 单元

与 `tea-admin.service` 对齐，仅改名称 / 路径 / jar：

```bash
cat > /etc/systemd/system/tea-api.service << 'EOF'
[Unit]
Description=tea-api
After=network.target

[Service]
Type=simple
User=www
Group=www
WorkingDirectory=/data/www/tea-api
EnvironmentFile=/data/www/tea-api/env.txt
ExecStart=/usr/bin/java $JAVA_OPTS -jar /data/www/tea-api/tea-api-1.0.0-SNAPSHOT.jar $SPRING_OPTS
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable tea-api
systemctl start tea-api
systemctl status tea-api --no-pager
```

对照现网 admin：

```bash
systemctl cat tea-admin
```

现网 `tea-admin.service` 参考形态：

```ini
[Unit]
Description=tea-admin
After=network.target

[Service]
Type=simple
User=www
Group=www
WorkingDirectory=/data/www/tea-admin
EnvironmentFile=/data/www/tea-admin/env.txt
ExecStart=/usr/bin/java $JAVA_OPTS -jar /data/www/tea-admin/tea-admin-1.0.0-RELEASE.jar $SPRING_OPTS
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

## 7. 打包与发布

本机 / CI：

```bash
mvn -pl server-api -am -DskipTests package
# 产物：server-api/target/tea-api-1.0.0-SNAPSHOT.jar
```

ECS 覆盖 jar 后：

```bash
systemctl restart tea-api
```

---

## 8. 验收

```bash
systemctl status tea-api --no-pager
ss -lntup | grep 8081
curl -s http://127.0.0.1:8081/health
# 期望：mysql/redis/status 均为 UP

curl -Ik https://api.ecotea.cn/health
# 期望：HTTP/2 200

ls -la /data/logs/tea-api/
journalctl -u tea-api -n 50 --no-pager
```

健康检查成功示例：

```json
{"code":0,"message":"success","data":{"mysql":"UP","redis":"UP","status":"UP"}}
```

浏览器可选：`https://api.ecotea.cn/h5/index.html`、`/h5/chai/sale.html`（见 `PRD_WX_MP.md`）。

---

## 9. 日常运维

| 操作 | 命令 |
|------|------|
| 启动 / 停止 / 重启 | `systemctl start\|stop\|restart tea-api` |
| 状态 | `systemctl status tea-api --no-pager` |
| 日志（journal） | `journalctl -u tea-api -f` |
| 应用日志文件 | `/data/logs/tea-api/` |
| 改 yml / env 后 | `systemctl restart tea-api` |
| 仅换 jar | 覆盖 jar → `systemctl restart tea-api` |

排错顺序：`systemctl stop tea-api` → `journalctl -u tea-api -n 100 --no-pager` → `sudo -u www` 手动启动同命令 → 查日志目录权限与 yml。

手动排查示例：

```bash
cd /data/www/tea-api
sudo -u www bash -c 'set -a; . /data/www/tea-api/env.txt; set +a; /usr/bin/java $JAVA_OPTS -jar /data/www/tea-api/tea-api-1.0.0-SNAPSHOT.jar $SPRING_OPTS'
```

---

## 10. 通用模板：再部署一个 Java 服务

将下列占位符替换后即可复用。

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `<APP>` | 服务短名 | `tea-xxx` |
| `<JAR>` | jar 文件名 | `tea-xxx-1.0.0.jar` |
| `<PORT>` | 本机端口 | `8082` |
| `<LOGBACK>` | 日志配置文件名 | `logback.xml` 或 `logback-spring.xml` |
| `<HEAP>` | 堆参数 | `-Xms512m -Xmx512m` |

### 10.1 目录与 www 权限

```bash
mkdir -p /data/www/<APP>/conf
mkdir -p /data/logs/<APP>
chown -R www:www /data/logs/<APP>
# 程序目录可读即可；可选：chown -R www:www /data/www/<APP>
sudo -u www touch /data/logs/<APP>/write-test.log && rm -f /data/logs/<APP>/write-test.log
ls -ld /data /data/logs /data/logs/<APP>
```

### 10.2 env.txt

```bash
JAVA_OPTS="-server <HEAP> -XX:+UseG1GC -Dfile.encoding=UTF-8"

SPRING_OPTS="\
--spring.profiles.active=prod \
--spring.config.location=/data/www/<APP>/conf/application.yml,/data/www/<APP>/conf/application-prod.yml \
--logging.config=/data/www/<APP>/conf/<LOGBACK> \
"
```

### 10.3 systemd

```ini
[Unit]
Description=<APP>
After=network.target

[Service]
Type=simple
User=www
Group=www
WorkingDirectory=/data/www/<APP>
EnvironmentFile=/data/www/<APP>/env.txt
ExecStart=/usr/bin/java $JAVA_OPTS -jar /data/www/<APP>/<JAR> $SPRING_OPTS
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable <APP>
systemctl start <APP>
systemctl status <APP> --no-pager
```

### 10.4 检查清单（新服务上线前）

- [ ] `id www` 存在  
- [ ] `/data/logs/<APP>` 属主 `www`，且 `sudo -u www touch` 成功  
- [ ] jar / conf / env 路径与 unit、env 中一致  
- [ ] logback 内 `logsDir` = `/data/logs/<APP>`  
- [ ] 端口未冲突；若对外，Nginx 已反代且安全组只放 80/443  
- [ ] `systemctl status` 为 `active (running)`，本机 `curl` 健康检查通过  

---

## 11. 相关文档

- `DEPLOY_NGINX_SSL.md`：`api.ecotea.cn` / `admin.ecotea.cn` HTTPS  
- `DEPLOY_MYSQL.md` / `DEPLOY_REDIS.md`：依赖中间件  
- `PRD_WX_MP.md`：微信订阅号需求；回调 `https://api.ecotea.cn/wx/mp/callback`
- `CODING_API.md`：server-api 编码规范  
