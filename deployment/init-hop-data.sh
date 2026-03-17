#!/bin/bash

# 临时运行容器（不挂载目录）
#docker run --name hfxt-web-temp registry.cn-hangzhou.aliyuncs.com/hfxt/hfxt-web:latest -v ~/hop-data/lic:/etc/lic sleep 30
docker run --name hfxt-web-temp hfxt-web:latest -v ./hop-data/lic:/etc/lic sleep 30

# 复制初始数据到主机目录
docker cp hfxt-web-temp:/usr/local/tomcat/webapps/ROOT/config/. ./hop-data/config/
docker cp hfxt-web-temp:/usr/local/tomcat/webapps/ROOT/audit/. ./hop-data/audit/

# 清理临时容器
docker stop hfxt-web-temp
docker rm hfxt-web-temp

# 设置权限
chown -R 501:501 ~/hop-data/

echo "初始化完成，数据已复制到 ~/hop-data 目录"