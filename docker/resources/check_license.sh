#!/bin/bash

# 解码URL安全的Base64并处理填充
urlsafe_b64decode() {
    local input="$1"
    local pad=$(( (4 - ${#input} % 4) % 4 ))
    echo -n "$input$(printf "%0.s=" $(seq 1 $pad))" | tr '-' '+' | tr '_' '/' |base64 -d 2>/dev/null
}

# 解析license token
unpack_license() {
    local token="$1"
    IFS='.' read -r payload_b64 sig_b64 <<< "$token"
    if [ -z "$payload_b64" ] || [ -z "$sig_b64" ]; then
        return 1
    fi
    local payload=$(urlsafe_b64decode "$payload_b64")
    if [ -z "$payload" ]; then
        return 1
    fi
    local signature=$(urlsafe_b64decode "$sig_b64" | xxd -p -c 256)
    if [ -z "$signature" ]; then
        return 1
    fi
    echo "$payload" > /tmp/payload.tmp
    echo "$signature" > /tmp/signature.tmp
    return 0
}

# 验证签名
verify_signature() {
    local public_key="$1"
    local payload="$2"
    local signature="$3"

    echo "$signature" | xxd -r -p > /tmp/signature.bin
    echo -n "$payload" > /tmp/payload.bin
    openssl dgst -sha256 -verify <(echo "$public_key") -signature /tmp/signature.bin /tmp/payload.bin >/dev/null 2>&1
    return $?
}

# 验证license token主函数（新增machine_code参数）
verify_license_token() {
    local token="$1"
    local public_key="$2"
    local machine_fp="$3"  # 新增机器码参数

    # 检查机器码是否提供
    if [ -z "$machine_fp" ]; then
        echo "false|machine fp is required|"
        return 1
    fi

    # 解析token
    if ! unpack_license "$token"; then
        echo "false|invalid token format|"
        return 1
    fi
    local payload=$(cat /tmp/payload.tmp)
    local signature=$(cat /tmp/signature.tmp)
    # 验证签名
    if ! verify_signature "$public_key" "$payload" "$signature"; then
        echo "false|signature invalid|"
        return 1
    fi

    # 解析JSON payload
    local expire=$(echo "$payload" | jq -r '.expire' 2>/dev/null)
    local token_machine_fp=$(echo "$payload" | jq -r '.machine_id' 2>/dev/null)  # 从payload提取机器码

    # 验证机器码匹配
    if [ "$token_machine_fp" != "$machine_fp" ] || [ "$token_machine_fp" = "null" ]; then
        echo "false|machine code mismatch|$payload"
        return 1
    fi

    # 验证过期时间
    if [ "$expire" = "null" ] || [ -z "$expire" ]; then
        echo "false|bad expire format|$payload"
        return 1
    fi

    local now=$(date -u +"%Y-%m-%d")
    if [ "$expire" \< "$now" ]; then
        echo "false|expired|$payload"
        return 1
    fi

    # 验证通过
    echo "true|ok|$payload"
    return 0
}

LICENSE_DIR="/etc/lic"
#echo "begin"
 # 基础检查
 if [ ! -f "${LICENSE_DIR}/license.lic" ]; then
   echo "false: 许可证文件不存在！"
   exit 1
 fi

 if [ ! -f "${LICENSE_DIR}/public.pem" ]; then
   echo "false: 公钥文件不存在！"
   exit 1
 fi

 if [ ! -f "${LICENSE_DIR}/machine_code" ]; then
   echo "false: 机器码文件不存在！"
   exit 1
 fi
# 使用示例：
# verify_license_token "your.token.here" "$(cat public_key.pem)" "your_machine_code_here"
#verify_license_token  "eyJhY2NvdW50IjoiMSIsImV4cGlyZSI6IjIwMjUtMTEtMTEiLCJpc3N1ZWRfYXQiOjE3NjE2NjMyMjYsIm1hY2hpbmVfaWQiOiJsMTdTRkotWlVNd2NQOE5OeXFLTFBKbmF3UGxLWHFFMW5Bc0oyemk5SHNZIn0.L4RqU-9sLjJ8-lprbyofjWIqg3wY814TYu4MwgUlATTw3Q_gJ_ejoPQU8rJ4qhqV5NBkOLVSwkY3gI8YEiloeL3qVCh7kQkPqcX4YjTxirNxZv9ISpauElya1_Pk-S8ayrHUyjgX4FN0eQSgSGoWhvc7f-AVFTfVibz52O6cMI37MrK6-N4XRWdVVcqv2y7HYcKTDQPAFMc3gj7Ft4gfq1fcQ2lUpkSPQCJYovJpGhEtcI1PRPSpU7PHIidYVz-M6EO3RyrQHXu3nF8uvZdcN3rPmNIRboyR3X-oaCthZS30CG-JacuXpt5OR79b3vqDeP8CU7vOEpADAKvtTLCwhA" "$(cat public.pem)" "l17SFJ-ZUMwcP8NNyqKLPJnawPlKXqE1nAsJ2zi9HsY"
verify_license_token  "$(cat $LICENSE_DIR/license.lic)" "$(cat $LICENSE_DIR/public.pem)" "$(cat $LICENSE_DIR/machine_code)"
