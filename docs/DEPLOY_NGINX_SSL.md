# ECS Nginx + SSL 部署（api / admin / 官网根域）

> 适用：阿里云 ECS（Alibaba Cloud Linux 3），与 `server-admin` / `server-api` **同机**部署。  
> 域名与反代：`api.ecotea.cn` → `127.0.0.1:8081`；`admin.ecotea.cn` → `127.0.0.1:8080`；  
> **官网根域** `ecotea.cn` / `www.ecotea.cn` → 同上游 api（静态 H5，根路径跳转 `/h5/index.html`）。  
> 证书：阿里云 **个人测试证书 pro**（DV，约 6 个月）；下载格式选 **Nginx**。本期不用 Let’s Encrypt / certbot（后续可选）。

相关文档：MySQL 见 `DEPLOY_MYSQL.md`；Redis 见 `DEPLOY_REDIS.md`；api 进程部署见 `DEPLOY_API.md`；业务缓存规划见 `TODO_REDIS_CACHE.md`；微信订阅号需求见 `PRD_WX_MP.md`。

---

## 1. 架构与端口


| 对外                          | TLS | 本机上游                                  |
| --------------------------- | --- | ------------------------------------- |
| `https://api.ecotea.cn`     | 443 | `http://127.0.0.1:8081`（server-api）   |
| `https://admin.ecotea.cn`   | 443 | `http://127.0.0.1:8080`（server-admin） |
| `https://ecotea.cn`（含 www） | 443 | `http://127.0.0.1:8081`（官网，反代 api）   |
| `http://` 上述域名              | 80  | **301** 跳转到对应 HTTPS                   |


公网只开放 **80 / 443**（及运维用 SSH）。**不要**长期对公网开放 8080 / 8081。

公安备案：主域名为 `ecotea.cn`，审核会访问根域；**网站访问地址**建议填 `https://ecotea.cn/` 或 `https://ecotea.cn/h5/index.html`（勿只填 `api` 子域深链）。

---



## 2. 前置条件

- [x] 域名 ICP 备案已通过  
- [ ] 云解析：`api` / `admin` / **`@`（根域）** / 可选 `www` 已指向本机公网 IP  
- [x] 安全组入站已放行 **80**、**443**  
- [x] admin（8080）、api（8081）进程已在本机监听  
- [x] SSL：`api.ecotea.cn`、`admin.ecotea.cn` 已签发  
- [ ] SSL：**`ecotea.cn`（建议含 www）** 已签发并部署（公安备案根域可访问所需）

---



## 3. 域名解析

云解析 DNS → 域名 `ecotea.cn` → 添加记录：


| 主机记录    | 类型  | 记录值          | TTL   |
| ------- | --- | ------------ | ----- |
| `@`     | A   | ECS 公网 IP    | 10 分钟 |
| `www`   | A   | 同一 ECS 公网 IP | 10 分钟 |
| `api`   | A   | 同一 ECS 公网 IP | 10 分钟 |
| `admin` | A   | 同一 ECS 公网 IP | 10 分钟 |


证书可能附赠 `www.api.ecotea.cn` / `www.admin.ecotea.cn`，不使用则可不解析。

本机验证：

```bash
# 应解析到本机公网 IP
ping -c 3 ecotea.cn
ping -c 3 www.ecotea.cn
ping -c 3 api.ecotea.cn
ping -c 3 admin.ecotea.cn
```

自动 DNS 验证签发证书时，解析中可能出现 `_dnsauth` / `_dnsauth.api` 等 TXT 记录；**证书已签发后可删可留**，不影响网站访问。

---



## 4. 安全组建议


| 端口          | 建议                                |
| ----------- | --------------------------------- |
| 80 / TCP    | 放行（HTTP → HTTPS）                  |
| 443 / TCP   | 放行（HTTPS）                         |
| 22 / TCP    | 放行；宜限制为运维出口 IP                    |
| 8080 / 8081 | Nginx 上线并验收后 **不要对 0.0.0.0/0 开放** |
| 3389        | Linux 主机通常删除                      |


---



## 5. SSL 证书（阿里云个人测试 pro）



### 5.1 申请要点

- 产品：**个人测试证书 (pro)**（约 6 个月；一般 **不支持一键续费**，到期需重新购买/申请）  
- 业务域名各一张（绑定后通常不可改域名）：  
  - `api.ecotea.cn`  
  - `admin.ecotea.cn`  
  - **`ecotea.cn`**（官网根域；若控制台支持「主域+www」可一并申请，否则再申请 `www.ecotea.cn` 或只把 www 301 到裸域并用一张主域证——以证书绑定名为准）  
- 验证方式：当前产品多为 **自动 DNS 验证**（需域名解析在同一账号云解析下）  
- CSR：**系统生成**；密钥算法：`RSA_2048`  
- 签发后：证书列表 → **下载** → 服务器类型选 **Nginx**

说明：个人测试类证书偏测试用途、无 SLA；生产长期也可日后改 Let’s Encrypt 自动续期，Nginx 反代配置可复用，仅换证书路径。`api` 证书**不能**用于 `ecotea.cn`。

### 5.2 上传与目录

将下载的 zip 传到 ECS（如 `/root/ssl-upload/`），再解压到固定目录：

```bash
# 创建证书目录（含官网根域）
mkdir -p /etc/nginx/ssl/api.ecotea.cn \
         /etc/nginx/ssl/admin.ecotea.cn \
         /etc/nginx/ssl/ecotea.cn

# 假设 zip 已在 /root/ssl-upload/（文件名以控制台下载为准）
cd /root/ssl-upload
unzip -o 27041553_api.ecotea.cn_nginx.zip -d /etc/nginx/ssl/api.ecotea.cn
unzip -o 27041598_admin.ecotea.cn_nginx.zip -d /etc/nginx/ssl/admin.ecotea.cn
unzip -o *_ecotea.cn_nginx.zip -d /etc/nginx/ssl/ecotea.cn

# 确认文件（实际名称以解压结果为准）
ls -la /etc/nginx/ssl/api.ecotea.cn
ls -la /etc/nginx/ssl/admin.ecotea.cn
ls -la /etc/nginx/ssl/ecotea.cn
```

期望文件（根域 pem/key 文件名以阿里云下载包为准，下面示例为常见命名）：

```text
/etc/nginx/ssl/api.ecotea.cn/api.ecotea.cn.pem
/etc/nginx/ssl/api.ecotea.cn/api.ecotea.cn.key
/etc/nginx/ssl/admin.ecotea.cn/admin.ecotea.cn.pem
/etc/nginx/ssl/admin.ecotea.cn/admin.ecotea.cn.key
/etc/nginx/ssl/ecotea.cn/ecotea.cn.pem
/etc/nginx/ssl/ecotea.cn/ecotea.cn.key
```

权限：

```bash
chmod 600 /etc/nginx/ssl/api.ecotea.cn/*.key
chmod 600 /etc/nginx/ssl/admin.ecotea.cn/*.key
chmod 600 /etc/nginx/ssl/ecotea.cn/*.key
chmod 644 /etc/nginx/ssl/api.ecotea.cn/*.pem
chmod 644 /etc/nginx/ssl/admin.ecotea.cn/*.pem
chmod 644 /etc/nginx/ssl/ecotea.cn/*.pem
```

私钥勿提交 git、勿公开传播。

### 5.3 换证 / 到期

1. 控制台重新申请并下载 Nginx 包
2. 覆盖同名 `.pem` / `.key`
3. `nginx -t && systemctl reload nginx`

当前批次有效期约至 **2027-03-04**，建议提前 2～4 周处理。日历提醒即可。

---



## 6. 安装 Nginx

```bash
# 使用系统仓库安装（Alibaba Cloud Linux / CentOS 系）
yum install -y nginx

# 查看版本
nginx -v

# 开机自启并启动
systemctl enable nginx
systemctl start nginx
systemctl status nginx --no-pager
```

若提示找不到包，可尝试 `yum list available nginx` 或按阿里云文档启用对应 yum 源后再安装。

---



## 7. 站点配置



### 7.1 写入配置

文件：`/etc/nginx/conf.d/ecotea.conf`

```bash
cat > /etc/nginx/conf.d/ecotea.conf << 'EOF'
# api.ecotea.cn → 127.0.0.1:8081
server {
    listen 80;
    server_name api.ecotea.cn;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name api.ecotea.cn;

    ssl_certificate     /etc/nginx/ssl/api.ecotea.cn/api.ecotea.cn.pem;
    ssl_certificate_key /etc/nginx/ssl/api.ecotea.cn/api.ecotea.cn.key;

    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 20m;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
}

# admin.ecotea.cn → 127.0.0.1:8080
server {
    listen 80;
    server_name admin.ecotea.cn;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name admin.ecotea.cn;

    ssl_certificate     /etc/nginx/ssl/admin.ecotea.cn/admin.ecotea.cn.pem;
    ssl_certificate_key /etc/nginx/ssl/admin.ecotea.cn/admin.ecotea.cn.key;

    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 50m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
}

# 官网根域 ecotea.cn / www → 127.0.0.1:8081（公安备案可访问；内容为 api 静态 H5）
# 证书路径/文件名以实际解压为准；若证不含 www，则 www 的 server 单独用 301 到 https://ecotea.cn$request_uri
server {
    listen 80;
    server_name ecotea.cn www.ecotea.cn;
    return 301 https://ecotea.cn$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ecotea.cn www.ecotea.cn;

    ssl_certificate     /etc/nginx/ssl/ecotea.cn/ecotea.cn.pem;
    ssl_certificate_key /etc/nginx/ssl/ecotea.cn/ecotea.cn.key;

    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;

    client_max_body_size 20m;

    # 根路径进官网首页
    location = / {
        return 302 /h5/index.html;
    }

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
    }
}
EOF
```



### 7.2 默认站点冲突

若 `/etc/nginx/conf.d/default.conf`（或其它文件）占用 `listen 80` 且影响跳转，可先备份停用：

```bash
ls /etc/nginx/conf.d/
# 按需备份默认站
# mv /etc/nginx/conf.d/default.conf /etc/nginx/conf.d/default.conf.bak
```



### 7.3 检查并重载

```bash
# 配置语法检查（必须成功）
nginx -t

# 平滑重载
systemctl reload nginx
```

---



## 8. 验收

本机上游：

```bash
ss -lntup | grep -E '8080|8081'
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/health
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/
```

经域名：

```bash
# 应 301 到 https
curl -I http://ecotea.cn
curl -I http://api.ecotea.cn
curl -I http://admin.ecotea.cn

# -k 仅排查用；浏览器应显示正常锁头
curl -Ik https://ecotea.cn/
curl -Ik https://ecotea.cn/h5/index.html
curl -Ik https://api.ecotea.cn/health
curl -Ik https://admin.ecotea.cn/
```

浏览器建议打开：

- `https://ecotea.cn/`（官网首页，公安备案访问地址）
- `https://api.ecotea.cn/h5/chai/recycle.html`（业务 H5）
- `https://admin.ecotea.cn/`

确认证书域名匹配、无过期告警。

---



## 9. 上线后收尾

1. 安全组去掉公网 **8080**（及无用的 3389 等）
2. 配置 api `ecotea.beian` 填入真实 ICP（公安备案号批下后再填）
3. 微信公众平台服务器 URL：`https://api.ecotea.cn/wx/mp/callback`
4. 菜单 / 业务 H5 链接继续使用 `https://api.ecotea.cn/...`；公安备案「网站访问地址」用 `https://ecotea.cn/`
5. 公安备案：应急联络人须与网站负责人**不是同一人**；主域名可访问后再提交

---



## 10. 日常运维


| 项    | 命令 / 路径                              |
| ---- | ------------------------------------ |
| 重载配置 | `nginx -t && systemctl reload nginx` |
| 启停   | `systemctl start\|stop\|restart nginx` |
| 访问日志 | `/var/log/nginx/access.log`          |
| 错误日志 | `/var/log/nginx/error.log`           |
| 站点配置 | `/etc/nginx/conf.d/ecotea.conf`      |
| api 证书 | `/etc/nginx/ssl/api.ecotea.cn/`    |
| admin 证书 | `/etc/nginx/ssl/admin.ecotea.cn/` |
| 官网根域证书 | `/etc/nginx/ssl/ecotea.cn/`      |


---



## 11. 常见问题


| 现象                    | 处理                                   |
| --------------------- | ------------------------------------ |
| `nginx -t` 报证书路径错误    | 核对 `.pem` / `.key` 文件名与配置是否一致        |
| 502 Bad Gateway       | 8080/8081 未启动，或只监听了非本机地址             |
| 能 HTTP 不能 HTTPS       | 安全组未放 443；或仅配置了 80                   |
| 证书名称不匹配               | api / admin / 根域 的 pem、key 装反或用错证    |
| `ecotea.cn` 无法访问      | 缺 `@` A 记录、缺根域证、或 conf 未含官网 server   |
| 公安备案主域名无法访问           | 先保证浏览器打开 `https://ecotea.cn/` 正常     |
| 80 行为异常               | 默认 `default.conf` 抢站，备份后 reload      |
| admin 登录跳转异常 / 静态资源异常 | 确认已传 `X-Forwarded-Proto`；查 error.log |


---



## 12. 与业务模块对应关系

```text
Internet
   │
   ├─ https://ecotea.cn (/ www) → Nginx:443 → 127.0.0.1:8081  (官网 → /h5/index.html)
   │
   ├─ https://api.ecotea.cn     → Nginx:443 → 127.0.0.1:8081  (server-api)
   │                              /h5/**、/chai/**、/wx/mp/callback、/site/beian …
   │
   └─ https://admin.ecotea.cn   → Nginx:443 → 127.0.0.1:8080  (server-admin)
```

