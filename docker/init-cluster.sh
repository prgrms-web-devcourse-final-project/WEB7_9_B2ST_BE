#!/bin/sh
set -e

# Redis Cluster 초기화 스크립트 (컨테이너 내부 실행용)
# redis-cli만 사용하여 컨테이너 내부에서 실행

REDIS_PASSWORD="${REDIS_PASSWORD:-tt_redis_pass}"
MAX_WAIT=60

echo "============================================"
echo "Redis Cluster 자동 초기화"
echo "============================================"
echo ""

# 모든 Redis 노드가 준비될 때까지 대기
echo "[1] Redis 노드 준비 대기..."
for node_num in 1 2 3 4 5 6; do
  port=$((7000 + node_num - 1))
  node_name="redis-node-${node_num}"

  echo "    ${node_name} (port ${port}) 대기 중..."
  wait_count=0
  while [ $wait_count -lt $MAX_WAIT ]; do
    if redis-cli -h "$node_name" -p "$port" -a "$REDIS_PASSWORD" ping >/dev/null 2>&1; then
      echo "    ✅ ${node_name} 준비 완료"
      break
    fi
    sleep 1
    wait_count=$((wait_count + 1))
  done

  if [ $wait_count -eq $MAX_WAIT ]; then
    echo "❌ ${node_name}가 준비되지 않았습니다 (타임아웃)"
    exit 1
  fi
done

echo ""
echo "[2] 클러스터 상태 확인..."

# 클러스터가 이미 구성되어 있는지 확인
CLUSTER_INFO=$(redis-cli -h redis-node-1 -p 7000 -a "$REDIS_PASSWORD" cluster info 2>/dev/null || echo "")

if echo "$CLUSTER_INFO" | grep -q "cluster_state:ok"; then
  echo "    ✅ 클러스터가 이미 구성되어 있습니다."
  echo ""
  echo "============================================"
  echo "✅ Redis Cluster 준비 완료 (스킵)"
  echo "============================================"
  redis-cli -h redis-node-1 -p 7000 -a "$REDIS_PASSWORD" cluster info | grep -E "cluster_state|cluster_slots_assigned|cluster_known_nodes" || true
  exit 0
fi

echo "    클러스터가 아직 구성되지 않았습니다. 초기화를 진행합니다..."
echo ""

echo "[3] 클러스터 생성..."
redis-cli --cluster create \
  redis-node-1:7000 redis-node-2:7001 redis-node-3:7002 \
  redis-node-4:7003 redis-node-5:7004 redis-node-6:7005 \
  --cluster-replicas 1 \
  --cluster-yes \
  -a "$REDIS_PASSWORD"

echo ""
echo "[4] 클러스터 상태 확인..."
sleep 2
redis-cli -h redis-node-1 -p 7000 -a "$REDIS_PASSWORD" cluster info | grep -E "cluster_state|cluster_slots_assigned|cluster_known_nodes" || true

echo ""
echo "============================================"
echo "✅ Redis Cluster 초기화 완료"
echo "============================================"
