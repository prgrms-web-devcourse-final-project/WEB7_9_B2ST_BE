#!/bin/bash
set -euo pipefail

# Redis Cluster 초기화 스크립트 (로컬/프로덕션 공용 - 안전 버전)
#
# 사용 예시:
# 1) 로컬: 클러스터 초기화만 (권장)
#    bash docker/init-redis-cluster.sh
#
# 2) 비밀번호 지정
#    bash docker/init-redis-cluster.sh "tt_redis_pass"
#
# 3) 초기화 전에 완전 초기화(컨테이너+볼륨 삭제)까지 하고 싶을 때
#    RESET=1 bash docker/init-redis-cluster.sh
#
# 4) 프로덕션에서 announce까지 설정하고 싶을 때 (외부 접근 설계가 확정된 경우에만)
#    CLUSTER_ANNOUNCE_IP=10.0.0.12 APPLY_ANNOUNCE=1 bash docker/init-redis-cluster.sh

# 비밀번호: 인자 > 환경변수 > 기본값
REDIS_PASSWORD="${1:-${REDIS_PASSWORD:-tt_redis_pass}}"

# 0이면 안전 모드(기본). 1이면 down -v 수행 (데이터/볼륨 삭제)
RESET="${RESET:-0}"

# announce 설정 적용 여부 (기본 0)
APPLY_ANNOUNCE="${APPLY_ANNOUNCE:-0}"
CLUSTER_ANNOUNCE_IP="${CLUSTER_ANNOUNCE_IP:-}"

# 스크립트 위치
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# compose 파일 자동 탐색 (우선순위: docker/ 아래 > 루트)
if [ -f "$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml" ]; then
  COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
elif [ -f "$PROJECT_ROOT/docker-compose.redis-cluster.yml" ]; then
  COMPOSE_FILE="$PROJECT_ROOT/docker-compose.redis-cluster.yml"
elif [ -f "$SCRIPT_DIR/docker-compose.redis-cluster.yml" ]; then
  COMPOSE_FILE="$SCRIPT_DIR/docker-compose.redis-cluster.yml"
else
  echo "❌ 오류: docker-compose.redis-cluster.yml 파일을 찾을 수 없습니다."
  echo "   확인 경로:"
  echo "   - $PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
  echo "   - $PROJECT_ROOT/docker-compose.redis-cluster.yml"
  echo "   - $SCRIPT_DIR/docker-compose.redis-cluster.yml"
  exit 1
fi

cd "$PROJECT_ROOT"

echo "============================================"
echo "Redis Cluster 초기화 (안전 버전)"
echo "============================================"
echo "Compose 파일: $COMPOSE_FILE"
echo "RESET(볼륨삭제): $RESET"
echo "APPLY_ANNOUNCE: $APPLY_ANNOUNCE"
echo "CLUSTER_ANNOUNCE_IP: ${CLUSTER_ANNOUNCE_IP:-<empty>}"
echo ""

export REDIS_PASSWORD="$REDIS_PASSWORD"

# RESET=1일 때만 파괴적 초기화 수행
if [ "$RESET" = "1" ]; then
  echo "[0] 기존 컨테이너/볼륨 정리 (RESET=1)..."
  docker compose -f "$COMPOSE_FILE" down -v || true
  echo "    정리 완료"
  echo ""
fi

echo "[1] Redis 노드 기동..."
docker compose -f "$COMPOSE_FILE" up -d

echo "[2] 노드 준비 대기..."
# 노드 1이 PONG 응답할 때까지 대기
for i in {1..40}; do
  if docker exec -i redis-node-1 redis-cli -a "$REDIS_PASSWORD" -p 7000 ping >/dev/null 2>&1; then
    echo "    redis-node-1 OK"
    break
  fi
  sleep 1
  if [ "$i" -eq 40 ]; then
    echo "❌ redis-node-1이 준비되지 않았습니다. 로그 확인: docker logs redis-node-1"
    exit 1
  fi
done
echo ""

echo "[3] 클러스터 상태 확인 (이미 구성되어 있으면 스킵)..."
CLUSTER_INFO="$(docker exec -i redis-node-1 redis-cli -a "$REDIS_PASSWORD" -p 7000 cluster info 2>/dev/null || true)"

if echo "$CLUSTER_INFO" | grep -q "cluster_state:ok"; then
  echo "    ✅ 이미 cluster_state:ok 입니다. (create 스킵)"
else
  echo "    클러스터 create 수행..."
  # 컨테이너 네트워크 DNS(hostname) 기반이 가장 안정적
  echo yes | docker exec -i redis-node-1 redis-cli --cluster create \
    redis-node-1:7000 redis-node-2:7001 redis-node-3:7002 \
    redis-node-4:7003 redis-node-5:7004 redis-node-6:7005 \
    --cluster-replicas 1 -a "$REDIS_PASSWORD"

  echo "    create 완료 후 상태 확인..."
  docker exec -i redis-node-1 redis-cli -a "$REDIS_PASSWORD" -p 7000 cluster info \
    | grep -E "cluster_state|cluster_slots_assigned|cluster_known_nodes" || true
fi
echo ""

# announce 설정은 기본적으로 하지 않음 (로컬에서 특히 위험)
if [ "$APPLY_ANNOUNCE" = "1" ]; then
  if [ -z "$CLUSTER_ANNOUNCE_IP" ]; then
    echo "❌ APPLY_ANNOUNCE=1 인데 CLUSTER_ANNOUNCE_IP가 비어있습니다."
    echo "   예) CLUSTER_ANNOUNCE_IP=10.0.0.12 APPLY_ANNOUNCE=1 bash docker/init-redis-cluster.sh"
    exit 1
  fi

  echo "[4] cluster-announce 설정 적용 (프로덕션 용도)..."
  for i in {1..6}; do
    PORT=$((7000 + i - 1))
    BUS_PORT=$((17000 + i - 1))
    echo "    redis-node-$i (port=$PORT bus=$BUS_PORT) 설정 중..."
    docker exec -i "redis-node-$i" redis-cli -a "$REDIS_PASSWORD" -p "$PORT" CONFIG SET cluster-announce-ip "$CLUSTER_ANNOUNCE_IP" >/dev/null || true
    docker exec -i "redis-node-$i" redis-cli -a "$REDIS_PASSWORD" -p "$PORT" CONFIG SET cluster-announce-port "$PORT" >/dev/null || true
    docker exec -i "redis-node-$i" redis-cli -a "$REDIS_PASSWORD" -p "$PORT" CONFIG SET cluster-announce-bus-port "$BUS_PORT" >/dev/null || true
  done

  echo "[5] 노드 재시작 (announce 반영)..."
  for i in {1..6}; do
    docker restart "redis-node-$i" >/dev/null 2>&1 || true
  done
  sleep 5
  echo "    재시작 완료"
  echo ""
else
  echo "[4] announce 설정 스킵 (기본/로컬 권장)"
  echo ""
fi

echo "============================================"
echo "✅ Redis Cluster 준비 완료"
echo "============================================"
echo "상태 확인:"
echo "  docker exec -it redis-node-1 redis-cli -a \"$REDIS_PASSWORD\" -p 7000 cluster info"
echo "노드 확인:"
echo "  docker exec -it redis-node-1 redis-cli -a \"$REDIS_PASSWORD\" -p 7000 cluster nodes"
