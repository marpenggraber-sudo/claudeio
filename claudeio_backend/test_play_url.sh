#!/bin/bash
# 测试播放 URL API

echo "=== 测试播放 URL API ==="
echo ""
echo "请求: GET /play-url?songId=2653714443&userId=13879884891"
echo ""

curl -s "http://localhost:8080/api/music/play-url?songId=2653714443&userId=13879884891" | python -m json.tool

echo ""
echo "=== 测试完成 ==="
