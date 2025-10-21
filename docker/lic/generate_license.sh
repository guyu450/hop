#!/bin/bash
set -euo pipefail

# ==============================================
# 跨平台许可证生成脚本（修复base64错误）
# ==============================================

# 1. 常量与环境检测
PRIVATE_KEY="private.key"
# 临时文件路径改为当前目录，避免/tmp权限问题
TEMP_CONTENT="./.license_content.tmp"
TEMP_SIGNATURE="./.license_signature.tmp"
OUTPUT_LICENSE="license.lic"

if [ "$(uname)" = "Darwin" ]; then
  IS_MAC=true
else
  IS_MAC=false
fi

# 2. 参数校验
if [ $# -ne 3 ]; then
  echo "ERROR: 参数错误！用法：$0 <用户名> <有效期(YYYY-MM-DD)> <MAC地址>"
  exit 1
fi

USER_NAME="$1"
EXPIRY_DATE="$2"
BIND_MAC="$3"

# 3. 输入合法性检查
if [ ! -f "${PRIVATE_KEY}" ]; then
  echo "ERROR: 缺少私钥文件 ${PRIVATE_KEY}，请先生成"
  exit 1
fi

# 日期格式校验
if $IS_MAC; then
  if ! date -jf "%Y-%m-%d" "${EXPIRY_DATE}" >/dev/null 2>&1; then
    echo "ERROR: 有效期格式错误（需YYYY-MM-DD）"
    exit 1
  fi
else
  if ! date -d "${EXPIRY_DATE}" +%Y-%m-%d >/dev/null 2>&1; then
    echo "ERROR: 有效期格式错误（需YYYY-MM-DD）"
    exit 1
  fi
fi

# 有效期在未来
if $IS_MAC; then
  CURRENT_TIMESTAMP=$(date +%s)
  EXPIRY_TIMESTAMP=$(date -jf "%Y-%m-%d" "${EXPIRY_DATE}" +%s)
else
  CURRENT_TIMESTAMP=$(date +%s)
  EXPIRY_TIMESTAMP=$(date -d "${EXPIRY_DATE}" +%s)
fi
if [ "${EXPIRY_TIMESTAMP}" -le "${CURRENT_TIMESTAMP}" ]; then
  echo "ERROR: 有效期必须晚于当前日期"
  exit 1
fi

# MAC格式校验
MAC_PATTERN='^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$'
if ! echo "${BIND_MAC}" | grep -qE "${MAC_PATTERN}"; then
  echo "ERROR: MAC地址格式错误"
  exit 1
fi

# 避免覆盖已有文件
if [ -f "${OUTPUT_LICENSE}" ]; then
  echo "ERROR: ${OUTPUT_LICENSE} 已存在，请删除后重试"
  exit 1
fi

# 4. 生成许可证内容
cat >"${TEMP_CONTENT}" <<EOF
USER:${USER_NAME}
EXPIRY:${EXPIRY_DATE}
BIND_MAC:${BIND_MAC}
GENERATE_TIME:$(date +%Y-%m-%dT%H:%M:%S)
EOF

# 5. 生成签名并检查文件是否存在
echo "生成签名..."
if ! openssl dgst -sha256 -sign "${PRIVATE_KEY}" -out "${TEMP_SIGNATURE}" "${TEMP_CONTENT}"; then
  echo "ERROR: openssl签名失败，请检查私钥"
  exit 1
fi
if [ ! -f "${TEMP_SIGNATURE}" ] || [ ! -s "${TEMP_SIGNATURE}" ]; then
  echo "ERROR: 签名文件生成失败（为空或不存在）"
  exit 1
fi

# 6. 组装许可证（兼容base64跨平台）
echo "编码签名并生成许可证..."
cat "${TEMP_CONTENT}" | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}' >"${OUTPUT_LICENSE}"
echo "SIGNATURE:" >>"${OUTPUT_LICENSE}"
# 用-i参数增强兼容性，确保读取文件成功
if $IS_MAC; then
  base64 -i "${TEMP_SIGNATURE}" >>"${OUTPUT_LICENSE}"
else
  base64 -w 0 -i "${TEMP_SIGNATURE}" >>"${OUTPUT_LICENSE}"  # -w 0 取消换行（Linux）
fi

# 7. 清理临时文件
rm -f "${TEMP_CONTENT}" "${TEMP_SIGNATURE}"

echo "成功生成许可证：${OUTPUT_LICENSE}"