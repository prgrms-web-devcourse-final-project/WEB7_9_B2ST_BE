#!/bin/bash

# Redis Cluster 초기화 스크립트
# Docker Compose로 Redis Cluster를 시작한 후 실행
# 사용법:
#   - docker 폴더에서: ./init-redis-cluster.sh [password]
#   - 프로젝트 루트에서: bash docker/init-redis-cluster.sh [password]

# 비밀번호 설정 (첫 번째 인자 또는 기본값)
REDIS_PASSWORD=${1:-"your-password"}

# cluster-announce-ip 설정 (환경 변수 또는 기본값: localhost)
# 로컬 개발: localhost (기본값)
# 프로덕션: 실제 서버 IP 또는 도메인 (환경 변수로 설정)
# 예: CLUSTER_ANNOUNCE_IP=192.168.1.100 bash init-redis-cluster.sh [password]
CLUSTER_ANNOUNCE_IP=${CLUSTER_ANNOUNCE_IP:-"localhost"}

# 스크립트 위치 확인
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# docker-compose 파일 경로 확인
if [ -f "$PROJECT_ROOT/docker-compose.redis-cluster.yml" ]; then
    # 프로젝트 루트에 있는 경우
    COMPOSE_FILE="$PROJECT_ROOT/docker-compose.redis-cluster.yml"
    cd "$PROJECT_ROOT"
elif [ -f "$SCRIPT_DIR/docker-compose.redis-cluster.yml" ]; then
    # docker 폴더 안에서 실행
    COMPOSE_FILE="$SCRIPT_DIR/docker-compose.redis-cluster.yml"
    cd "$SCRIPT_DIR"
elif [ -f "$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml" ]; then
    # 프로젝트 루트/docker 폴더에 있는 경우
    COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
    cd "$PROJECT_ROOT"
else
    echo "❌ 오류: docker-compose.redis-cluster.yml 파일을 찾을 수 없습니다."
    echo "   다음 위치를 확인하세요:"
    echo "   - $PROJECT_ROOT/docker-compose.redis-cluster.yml"
    echo "   - $SCRIPT_DIR/docker-compose.redis-cluster.yml"
    echo "   - $PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
    exit 1
fi

echo "============================================"
echo "Redis Cluster 초기화 시작"
echo "============================================"
echo "비밀번호: ${REDIS_PASSWORD}"
echo "cluster-announce-ip: ${CLUSTER_ANNOUNCE_IP} (로컬 개발: localhost, 프로덕션: 실제 IP/도메인)"
echo "작업 디렉토리: $(pwd)"
echo "Compose 파일: ${COMPOSE_FILE}"
echo ""

# 기존 컨테이너와 볼륨 정리 (재초기화 시)
echo "0. 기존 컨테이너 및 볼륨 정리..."
docker-compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
echo "   기존 리소스 정리 완료"
echo ""

# Docker Compose로 Redis Cluster 시작
echo "1. Docker Compose로 Redis Cluster 시작..."
# 환경 변수 REDIS_PASSWORD를 설정하여 Docker Compose에 전달
export REDIS_PASSWORD="${REDIS_PASSWORD}"
docker-compose -f "$COMPOSE_FILE" up -d

# 컨테이너가 준비될 때까지 대기
echo ""
echo "2. 컨테이너 준비 대기 (10초)..."
sleep 10

# Redis Cluster 초기화 (내부 IP로 먼저 초기화)
echo ""
echo "3. Redis Cluster 초기화..."
echo "   (이 작업은 몇 분 정도 걸릴 수 있습니다. 잠시만 기다려주세요...)"
# Windows에서는 timeout 명령이 다르게 동작하므로 직접 실행
CLUSTER_OUTPUT=$(echo 'yes' | docker exec -i redis-node-1 redis-cli --cluster create \
  redis-node-1:7000 \
  redis-node-2:7001 \
  redis-node-3:7002 \
  redis-node-4:7003 \
  redis-node-5:7004 \
  redis-node-6:7005 \
  --cluster-replicas 1 \
  -a ${REDIS_PASSWORD} 2>&1 || echo "CLUSTER_CREATE_ERROR")

echo "$CLUSTER_OUTPUT"

# 에러 체크
if echo "$CLUSTER_OUTPUT" | grep -q "TIMEOUT_OR_ERROR\|\[ERR\]"; then
    echo ""
    echo "⚠️  경고: Cluster 초기화 중 오류가 발생했습니다."
    echo "   기존 데이터가 있는 경우 다음 명령으로 완전히 삭제 후 재시도하세요:"
    echo "   docker-compose -f \"$COMPOSE_FILE\" down -v"
    echo "   그 후 이 스크립트를 다시 실행하세요."
    exit 1
fi

# 성공 확인
if echo "$CLUSTER_OUTPUT" | grep -q "All nodes agree about slots configuration\|All 16384 slots covered"; then
    echo ""
    echo "✅ Cluster 초기화 성공!"
else
    echo ""
    echo "⚠️  경고: Cluster 초기화 결과를 확인할 수 없습니다."
    echo "   수동으로 확인해주세요: docker exec -it redis-node-1 redis-cli -a ${REDIS_PASSWORD} cluster info"
fi

# cluster-announce-ip 설정 (클러스터 초기화 후에 설정)
# 클러스터 초기화는 내부 IP로 진행하고, 초기화 후에 cluster-announce-ip를 설정합니다.
echo ""
echo "5. cluster-announce-ip 설정 (클러스터 초기화 후) (현재: ${CLUSTER_ANNOUNCE_IP})..."
if [ "${CLUSTER_ANNOUNCE_IP}" = "localhost" ]; then
    echo "   !!로컬 개발 환경 모드: localhost 사용"
    echo "   프로덕션 환경에서는 CLUSTER_ANNOUNCE_IP 환경 변수를 설정하세요."
    echo "   예: CLUSTER_ANNOUNCE_IP=192.168.1.100 bash init-redis-cluster.sh [password]"
else
    echo "   프로덕션 환경 모드: ${CLUSTER_ANNOUNCE_IP} 사용"
fi
for i in {1..6}; do
    PORT=$((7000 + i - 1))
    BUS_PORT=$((17000 + i - 1))
    echo "   redis-node-${i} 설정 중 (포트: ${PORT}, Bus 포트: ${BUS_PORT})..."
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CONFIG SET cluster-announce-ip ${CLUSTER_ANNOUNCE_IP} 2>/dev/null || true
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CONFIG SET cluster-announce-port ${PORT} 2>/dev/null || true
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CONFIG SET cluster-announce-bus-port ${BUS_PORT} 2>/dev/null || true
    # 설정을 영구적으로 저장 (config file이 없으면 오류가 나지만 무시)
    docker exec -i redis-node-${i} redis-cli -a ${REDIS_PASSWORD} -p ${PORT} CONFIG REWRITE 2>&1 | grep -v "ERR The server is running without a config file" || true
done
echo "   cluster-announce-ip 설정 완료 (${CLUSTER_ANNOUNCE_IP})"

# 클러스터 노드를 재시작하여 cluster-announce-ip 설정이 클러스터 메타데이터에 반영되도록 함
echo ""
echo "6. 클러스터 노드 재시작 (cluster-announce-ip 설정 반영)..."
for i in {1..6}; do
    echo "   redis-node-${i} 재시작 중..."
    docker restart redis-node-${i} >/dev/null 2>&1 || true
done
echo "   노드 재시작 완료. 클러스터 재연결 대기 (10초)..."
sleep 10

echo ""
echo "============================================"
echo "Redis Cluster 초기화 완료!"
echo "============================================"
echo ""
echo "Cluster 상태 확인:"
echo "docker exec -it redis-node-1 redis-cli -a ${REDIS_PASSWORD} cluster info"
echo ""
echo "Cluster 노드 확인:"
echo "docker exec -it redis-node-1 redis-cli -a ${REDIS_PASSWORD} cluster nodes"

