# 启动方式

## 第一次启动容器：
- chmod +x init-hop-data.sh
- ./init-hop-data.sh
- docker-compose up -d

## 后续重新启动：
- docker-compose up -d

# 登录功能

## 访问地址

| 功能 | URL | 说明 |
|------|-----|------|
| **Web UI** | http://localhost:9003/ui | 图形化界面（需登录） |
| **Hop Server** | http://localhost:9003/hop/* | API接口（需登录） |
| **登录页面** | http://localhost:9003/login/login.html | Web登录页 |
| **静态资源** | http://localhost:9003/static/* | CSS/JS/图片 |

## 默认账号

- **用户名**: `cluster`
- **密码**: `cluster`

## 修改密码

编辑 `hop-data/pwd/hop.pwd` 文件：

```
# 格式：username: password,role
cluster: cluster,default
admin: yourpassword,default
```

## 密码加密

在容器内生成加密密码：

```bash
docker exec -it hfxt-web bash
cd /usr/local/tomcat/webapps/ROOT
./hop-encrypt.sh
```

输入密码后会生成 `OBF:xxx` 格式的加密字符串，复制到 `hop.pwd` 文件中。

## 登出

- Web界面登出：刷新页面重新登录
- Hop Server登出：访问 http://localhost:9003/logout