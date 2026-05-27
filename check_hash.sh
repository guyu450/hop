#!/bin/bash

# 原始哈希值
original_hash="23Wsx75CUCaoHPgMb32++43LpwEYpLkBATLdnWhWB8Y="

# 测试一些常见密码
test_passwords=("test" "Test" "TEST" "test123" "admin" "password" "123456" "cluster")

echo "测试原始哈希: $original_hash"
echo ""

for pwd in "${test_passwords[@]}"; do
    calculated=$(echo -n "$pwd" | shasum -a 256 | awk '{print $1}' | xxd -r -p | base64)
    match="❌"
    if [ "$calculated" = "$original_hash" ]; then
        match="✅ 匹配!"
    fi
    echo "密码: '$pwd' -> $calculated $match"
done
