#!/bin/bash

# Redis Cluster 초기화 스크립트
# Docker Compose로 Redis Cluster를 시작한 후 실행
# 사용법:
#   - docker 폴더에서: ./init-redis-cluster.sh [password]
#   - 프로젝트 루트에서: bash docker/init-redis-cluster.sh [password]

# 비밀번호 설정 (첫 번째 인자 또는 기본값)
REDIS_PASSWORD=${1:-"your-password"}

# 스크립트 위치 확인
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# docker-compose 파일 경로 확인
if [ -f "$SCRIPT_DIR/docker-compose.redis-cluster.yml" ]; then
    # docker 폴더 안에서 실행
    COMPOSE_FILE="$SCRIPT_DIR/docker-compose.redis-cluster.yml"
    cd "$SCRIPT_DIR"
elif [ -f "$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml" ]; then
    # 프로젝트 루트에서 실행
    COMPOSE_FILE="$PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
    cd "$PROJECT_ROOT"
else
    echo "   오류: docker-compose.redis-cluster.yml 파일을 찾을 수 없습니다."
    echo "   다음 위치를 확인하세요:"
    echo "   - $SCRIPT_DIR/docker-compose.redis-cluster.yml"
    echo "   - $PROJECT_ROOT/docker/docker-compose.redis-cluster.yml"
    exit 1
fi

echo "============================================"
echo "Redis Cluster 초기화 시작"
echo "============================================"
echo "비밀번호: ${REDIS_PASSWORD}"
echo "작업 디렉토리: $(pwd)"
echo "Compose 파일: ${COMPOSE_FILE}"
echo ""

# Docker Compose로 Redis Cluster 시작
echo "1. Docker Compose로 Redis Cluster 시작..."
docker-compose -f "$COMPOSE_FILE" up -d

# 컨테이너가 준비될 때까지 대기
echo ""
echo "2. 컨테이너 준비 대기 (10초)..."
sleep 10

# Redis Cluster 초기화
echo ""
echo "3. Redis Cluster 초기화..."
echo "yes" | docker exec -i redis-node-1 redis-cli --cluster create \
  redis-node-1:6379 \
  redis-node-2:6379 \
  redis-node-3:6379 \
  redis-node-4:6379 \
  redis-node-5:6379 \
  redis-node-6:6379 \
  --cluster-replicas 1 \
  -a ${REDIS_PASSWORD}

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

