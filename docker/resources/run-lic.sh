#!/bin/bash
# 判断当前用户是root，则退出程序，否则执行
if [ "$(id -u)" -eq 0 ]; then
    echo "Error: 禁止以root用户运行容器" >&2
    exit 1
else
  /opt/hop/check_license.sh
  # 如果检查到没有授权，则退出程序
  if [ $? -eq 1 ]; then
    echo "Error: 授权失败" >&2
    exit 1
  else
    echo "授权成功，启动主程序"
    # 再执行主程序
    exec /bin/bash /opt/hop/run.sh "$@"
  fi
fi