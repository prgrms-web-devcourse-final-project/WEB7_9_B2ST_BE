#!/bin/bash

# Redis Cluster 노드 연결 복구 스크립트
# 클러스터가 fail 상태일 때 노드들을 다시 연결합니다.
# 사용법: bash docker/fix-cluster-connection.sh tt_redis_pass

REDIS_PASSWORD=${1:-"tt_redis_pass"}
CLUSTER_ANNOUNCE_IP=${CLUSTER_ANNOUNCE_IP:-"127.0.0.1"}

echo "============================================"
echo "Redis Cluster 연결 복구"
echo "============================================"
echo "비밀번호: ${REDIS_PASSWORD}"
echo ""

echo "1. 모든 노드가 실행 중인지 확인..."
echo "--------------------------------------------"
for i in {1..6}; do
    if docker ps --format "{{.Names}}" | grep -q "redis-node-${i}"; then
        echo "✅ redis-node-${i}: 실행 중"
    else
        echo "❌ redis-node-${i}: 실행 중이 아님"
        exit 1
    fi
done
echo ""

echo "2. 모든 노드 리셋..."
echo "--------------------------------------------"
for i in {1..6}; do
    PORT=$((7000 + i - 1))
    echo "redis-node-${i} 리셋 중..."
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CLUSTER RESET HARD 2>&1 | grep -v "Warning:" || true
done
sleep 2
echo "✅ 리셋 완료"
echo ""

echo "3. Docker 내부 IP 주소 확인..."
echo "--------------------------------------------"
NODE1_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-1)
NODE2_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-2)
NODE3_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-3)
NODE4_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-4)
NODE5_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-5)
NODE6_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-node-6)

echo "  redis-node-1: ${NODE1_IP}"
echo "  redis-node-2: ${NODE2_IP}"
echo "  redis-node-3: ${NODE3_IP}"
echo "  redis-node-4: ${NODE4_IP}"
echo "  redis-node-5: ${NODE5_IP}"
echo "  redis-node-6: ${NODE6_IP}"
echo ""

echo "4. 노드 간 연결 설정..."
echo "--------------------------------------------"
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MEET ${NODE2_IP} 7001 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MEET ${NODE3_IP} 7002 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MEET ${NODE4_IP} 7003 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MEET ${NODE5_IP} 7004 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MEET ${NODE6_IP} 7005 2>&1 | grep -v "Warning:" || true
echo "✅ 연결 설정 완료"
echo ""

echo "5. 노드 연결 대기 (5초)..."
sleep 5
echo ""

echo "6. 슬롯 할당 (3 Master 노드에 16384 슬롯 분배)..."
echo "--------------------------------------------"
echo "  노드 1에 슬롯 0-5460 할당 중..."
SLOTS1=""
for i in $(seq 0 5460); do
    SLOTS1="${SLOTS1} $i"
done
docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER ADDSLOTS ${SLOTS1} 2>&1 | grep -v "Warning:" || true

echo "  노드 2에 슬롯 5461-10922 할당 중..."
SLOTS2=""
for i in $(seq 5461 10922); do
    SLOTS2="${SLOTS2} $i"
done
docker exec -i redis-node-2 redis-cli -a ${REDIS_PASSWORD} -p 7001 CLUSTER ADDSLOTS ${SLOTS2} 2>&1 | grep -v "Warning:" || true

echo "  노드 3에 슬롯 10923-16383 할당 중..."
SLOTS3=""
for i in $(seq 10923 16383); do
    SLOTS3="${SLOTS3} $i"
done
docker exec -i redis-node-3 redis-cli -a ${REDIS_PASSWORD} -p 7002 CLUSTER ADDSLOTS ${SLOTS3} 2>&1 | grep -v "Warning:" || true
echo "✅ 슬롯 할당 완료"
echo ""

echo "7. Replica 설정..."
echo "--------------------------------------------"
sleep 2
NODE1_ID=$(docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER MYID 2>&1 | grep -v "Warning:" | tr -d '\r\n')
NODE2_ID=$(docker exec -i redis-node-2 redis-cli -a ${REDIS_PASSWORD} -p 7001 CLUSTER MYID 2>&1 | grep -v "Warning:" | tr -d '\r\n')
NODE3_ID=$(docker exec -i redis-node-3 redis-cli -a ${REDIS_PASSWORD} -p 7002 CLUSTER MYID 2>&1 | grep -v "Warning:" | tr -d '\r\n')

echo "  Master 노드 ID:"
echo "    redis-node-1: ${NODE1_ID}"
echo "    redis-node-2: ${NODE2_ID}"
echo "    redis-node-3: ${NODE3_ID}"

docker exec -i redis-node-4 redis-cli -a ${REDIS_PASSWORD} -p 7003 CLUSTER REPLICATE ${NODE1_ID} 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-5 redis-cli -a ${REDIS_PASSWORD} -p 7004 CLUSTER REPLICATE ${NODE2_ID} 2>&1 | grep -v "Warning:" || true
docker exec -i redis-node-6 redis-cli -a ${REDIS_PASSWORD} -p 7005 CLUSTER REPLICATE ${NODE3_ID} 2>&1 | grep -v "Warning:" || true
echo "✅ Replica 설정 완료"
echo ""

echo "8. 클러스터 구성 대기 (5초)..."
sleep 5
echo ""

echo "9. 최종 상태 확인..."
echo "--------------------------------------------"
CLUSTER_STATE=$(docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER INFO 2>&1 | grep -v "Warning:" | grep "cluster_state" | cut -d: -f2 | tr -d '\r\n ')
CLUSTER_NODES=$(docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER INFO 2>&1 | grep -v "Warning:" | grep "cluster_known_nodes" | cut -d: -f2 | tr -d '\r\n ')
CLUSTER_SLOTS=$(docker exec -i redis-node-1 redis-cli -a ${REDIS_PASSWORD} -p 7000 CLUSTER INFO 2>&1 | grep -v "Warning:" | grep "cluster_slots_assigned" | cut -d: -f2 | tr -d '\r\n ')

if [ "$CLUSTER_STATE" = "ok" ]; then
    echo "✅ 클러스터 상태: OK"
else
    echo "❌ 클러스터 상태: ${CLUSTER_STATE}"
fi

if [ "$CLUSTER_SLOTS" = "16384" ]; then
    echo "✅ 슬롯 할당: 완료 (16384/16384)"
else
    echo "⚠️  슬롯 할당: ${CLUSTER_SLOTS}/16384"
fi

echo "📊 알려진 노드 수: ${CLUSTER_NODES}"
echo ""

if [ "$CLUSTER_STATE" = "ok" ] && [ "$CLUSTER_SLOTS" = "16384" ]; then
    echo "============================================"
    echo "✅ 클러스터 복구 성공!"
    echo "============================================"
    echo ""
    echo "다음 명령으로 상태를 확인하세요:"
    echo "bash docker/check-cluster-status.sh"
else
    echo "============================================"
    echo "⚠️  클러스터 복구가 완전하지 않습니다."
    echo "============================================"
    echo ""
    echo "다음 명령으로 전체 재초기화를 시도하세요:"
    echo "cd docker"
    echo "./init-redis-cluster.sh ${REDIS_PASSWORD}"
fi


