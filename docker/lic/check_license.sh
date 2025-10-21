#!/bin/bash
set -euo pipefail

LICENSE_FILE="/etc/lic/license.lic"
PUBLIC_KEY="/usr/local/share/public.key"
TEMP_CONTENT="/tmp/license_content_$(date +%s%N).tmp"
TEMP_SIGNATURE="/tmp/license_signature_$(date +%s%N).tmp"

if [ "$(uname)" = "Darwin" ]; then
  IS_MAC=true
else
  IS_MAC=false
fi

# 临时文件清理
cleanup() {
  rm -f "${TEMP_CONTENT}" "${TEMP_SIGNATURE}"
}
trap cleanup EXIT

# 基础检查
if [ ! -f "${LICENSE_FILE}" ]; then
  echo "ERROR: 许可证文件不存在！"
  exit 1
fi

if [ ! -f "${PUBLIC_KEY}" ]; then
  echo "ERROR: 公钥文件不存在！"
  exit 1
fi

if [ -z "${HOST_MAC:-}" ]; then
  echo "ERROR: 未设置HOST_MAC！"
  exit 1
fi

# 解析内容与签名（核心修正）
echo "解析许可证文件..."
sed '/^SIGNATURE:/,$d' "${LICENSE_FILE}" | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}' >"${TEMP_CONTENT}"
SIGNATURE_LINE=$(sed -n '/^SIGNATURE:/,$p' "${LICENSE_FILE}" | sed -e ':a' -e '/^\n*$/{$d;N;ba' -e '}' | sed -n '$p')
echo "${SIGNATURE_LINE}" | sed 's/^SIGNATURE://' | base64 -d >"${TEMP_SIGNATURE}"

if [ ! -s "${TEMP_CONTENT}" ] || [ ! -s "${TEMP_SIGNATURE}" ]; then
  echo "ERROR: 许可证格式错误！"
  exit 1
fi

# 签名验证
echo "验证签名..."
if ! openssl dgst -sha256 -verify "${PUBLIC_KEY}" -signature "${TEMP_SIGNATURE}" "${TEMP_CONTENT}"; then
  echo "ERROR: 签名验证失败！"
  exit 1
fi

# 解析信息
USER=$(grep "^USER:" "${TEMP_CONTENT}" | sed 's/^USER://' | sed 's/^[ \t]*//;s/[ \t]*$//')
EXPIRY_DATE=$(grep "^EXPIRY:" "${TEMP_CONTENT}" | sed 's/^EXPIRY://' | sed 's/^[ \t]*//;s/[ \t]*$//')
BIND_MAC=$(grep "^BIND_MAC:" "${TEMP_CONTENT}" | sed 's/^BIND_MAC://' | sed 's/^[ \t]*//;s/[ \t]*$//')

if [ -z "${USER}" ] || [ -z "${EXPIRY_DATE}" ] || [ -z "${BIND_MAC}" ]; then
  echo "ERROR: 许可证内容不完整！"
  exit 1
fi

# 有效期检查
echo "检查有效期..."
if $IS_MAC; then
  CURRENT_TIMESTAMP=$(date +%s)
  EXPIRY_TIMESTAMP=$(date -jf "%Y-%m-%d" "${EXPIRY_DATE}" +%s 2>/dev/null || true)
else
  CURRENT_TIMESTAMP=$(date +%s)
  EXPIRY_TIMESTAMP=$(date -d "${EXPIRY_DATE}" +%s 2>/dev/null || true)
fi

if [ -z "${EXPIRY_TIMESTAMP}" ] || [ "${CURRENT_TIMESTAMP}" -gt "${EXPIRY_TIMESTAMP}" ]; then
  echo "ERROR: 许可证已过期或格式错误！"
  exit 1
fi

# MAC绑定检查
echo "检查MAC绑定..."
NORMALIZED_HOST_MAC=$(echo "${HOST_MAC}" | tr '-' ':')
NORMALIZED_BIND_MAC=$(echo "${BIND_MAC}" | tr '-' ':')

if [ "${NORMALIZED_HOST_MAC}" != "${NORMALIZED_BIND_MAC}" ]; then
  echo "ERROR: MAC不匹配！"
  exit 1
fi

echo "✅ 验证通过！"
exec "$@"