#!/bin/bash

# Redis Cluster 상태 확인 스크립트
# 사용법: bash docker/check-cluster-status.sh [비밀번호]
# 또는: cd docker && ./check-cluster-status.sh [비밀번호]

REDIS_PASSWORD=${1:-"tt_redis_pass"}

echo "============================================"
echo "Redis Cluster 상태 확인"
echo "============================================"
echo "비밀번호: ${REDIS_PASSWORD}"
echo ""

echo "1. 클러스터 정보:"
echo "--------------------------------------------"
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 cluster info 2>&1 | grep -v "Warning:"
echo ""

echo "2. 클러스터 노드 목록 (IP 주소 확인):"
echo "--------------------------------------------"
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 cluster nodes 2>&1 | grep -v "Warning:"
echo ""

echo "3. 각 노드의 cluster-announce-ip 확인:"
echo "--------------------------------------------"
for i in {1..6}; do
    PORT=$((7000 + i - 1))
    echo "redis-node-${i} (포트 ${PORT}):"
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CONFIG GET cluster-announce-ip 2>&1 | grep -v "Warning:" | tail -1
    echo ""
done

echo "4. 클러스터 상태 요약:"
echo "--------------------------------------------"
CLUSTER_INFO=$(docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 cluster info 2>&1 | grep -v "Warning:")

if echo "$CLUSTER_INFO" | grep -q "cluster_state:ok"; then
    echo "✅ 클러스터 상태: OK"
else
    echo "❌ 클러스터 상태: FAIL"
fi

SLOTS=$(echo "$CLUSTER_INFO" | grep "cluster_slots_assigned" | cut -d: -f2 | tr -d ' ')
if [ "$SLOTS" = "16384" ]; then
    echo "✅ 슬롯 할당: 완료 (16384/16384)"
else
    echo "⚠️  슬롯 할당: ${SLOTS}/16384"
fi

NODES=$(echo "$CLUSTER_INFO" | grep "cluster_known_nodes" | cut -d: -f2 | tr -d ' ')
echo "📊 알려진 노드 수: ${NODES}"

echo ""
echo "============================================"
echo "확인 완료"
echo "============================================"

