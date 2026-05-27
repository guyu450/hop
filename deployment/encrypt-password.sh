#!/bin/bash

#
# Hop 密码加密工具
# 使用方法: ./encrypt-password.sh <password> [format]
#
# 输出格式:
#   base64 (默认): SHA256:<base64_hash>    - Hop Web UI 登录
#   hex:          SHA-256:<hex_hash>      - Jetty Basic Auth (REST API)
#
# 说明: 与 Hop 的 Java 代码使用相同的加密算法
# - Java: MessageDigest.getInstance("SHA-256") + Base64/Hex
# - Shell: sha256sum/shasum + base64/hex
#

set -e

PASSWORD="$1"
FORMAT="${2:-base64}"  # 默认使用 base64 格式

if [ -z "$PASSWORD" ]; then
    echo "Hop 密码加密工具"
    echo ""
    echo "使用方法: $0 <password> [format]"
    echo ""
    echo "参数:"
    echo "  password - 要加密的密码"
    echo "  format   - 输出格式 (可选)"
    echo "            base64 (默认) - 用于 Web UI 登录"
    echo "            hex         - 用于 Jetty Basic Auth (REST API)"
    echo ""
    echo "示例:"
    echo "  $0 test              -> SHA256:n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg="
    echo "  $0 admin base64      -> SHA256:jGl25bVBBBW96Qi9Te4V37Fnqchz/Eu4qB9vKrRIqRg="
    echo "  $0 admin hex         -> SHA-256:8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918"
    echo ""
    echo "格式说明:"
    echo "  base64 格式: SHA256:<base64>  - Web UI 登录使用"
    echo "  hex 格式:    SHA-256:<hex>    - REST API Basic Auth 使用"
    echo ""
    echo "推荐: 使用 hex 格式可以同时支持 Web UI 和 REST API"
    exit 1
fi

# 使用系统自带的工具进行 SHA-256 加密
# macOS: 使用 shasum
# Linux: 使用 sha256sum

if [ "$FORMAT" = "hex" ]; then
    # Jetty 格式: SHA-256:hex (64个十六进制字符)
    if command -v shasum &> /dev/null; then
        # macOS
        HASH=$(printf "%s" "$PASSWORD" | shasum -a 256 | awk '{print $1}')
    elif command -v sha256sum &> /dev/null; then
        # Linux
        HASH=$(printf "%s" "$PASSWORD" | sha256sum | awk '{print $1}')
    elif command -v openssl &> /dev/null; then
        # 使用 openssl (跨平台备用方案)
        HASH=$(printf "%s" "$PASSWORD" | openssl dgst -sha256 | awk '{print $2}')
    else
        echo "错误: 找不到 sha256sum/shasum/openssl 工具"
        echo ""
        echo "请安装其中一个工具："
        echo "  macOS:   brew install coreutils  (提供 shasum)"
        echo "  Ubuntu:  sudo apt-get install coreutils"
        echo "  CentOS:  sudo yum install coreutils"
        exit 1
    fi
    echo "SHA-256:$HASH"
else
    # Hop 格式: SHA256:base64
    if command -v shasum &> /dev/null; then
        # macOS (优先使用，因为更常见)
        # 使用 printf 而不是 echo -n，确保跨 shell 兼容（bash/dash/sh）
        HASH=$(printf "%s" "$PASSWORD" | shasum -a 256 | awk '{print $1}' | xxd -r -p | base64)
    elif command -v sha256sum &> /dev/null; then
        # Linux
        HASH=$(printf "%s" "$PASSWORD" | sha256sum | awk '{print $1}' | xxd -r -p | base64)
    elif command -v openssl &> /dev/null; then
        # 使用 openssl (跨平台备用方案)
        HASH=$(printf "%s" "$PASSWORD" | openssl dgst -sha256 -binary | base64)
    else
        echo "错误: 找不到 sha256sum/shasum/openssl 工具"
        echo ""
        echo "请安装其中一个工具："
        echo "  macOS:   brew install coreutils  (提供 shasum)"
        echo "  Ubuntu:  sudo apt-get install coreutils"
        echo "  CentOS:  sudo yum install coreutils"
        exit 1
    fi
    echo "SHA256:$HASH"
fi
