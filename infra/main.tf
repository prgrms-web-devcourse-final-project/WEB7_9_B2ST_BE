// 테라폼 설정의 시작
terraform {
  // 필요한 프로바이더(클라우드 서비스 제공자)를 설정
  required_providers {
    // AWS 프로바이더를 사용한다고 선언
    aws = {
      // AWS 프로바이더의 출처를 hashicorp/aws로 지정
      source = "hashicorp/aws"
    }
  }
}

// AWS를 제공자로 사용한다고 선언
provider "aws" {
  region = var.region
  default_tags {
    tags = {
      Project   = var.project_name
      ManagedBy = "Terraform"
      Team      = var.team_tag
    }
  }
}

locals {
  name = var.project_name
}

// AWS VPC(Virtual Private Cloud) 리소스를 생성하고 이름을 'vpc_1'로 설정
resource "aws_vpc" "vpc_1" {
  // VPC의 IP 주소 범위를 설정
  cidr_block = "10.0.0.0/16"

  // DNS 지원을 활성화
  enable_dns_support = true
  // DNS 호스트 이름 지정을 활성화
  enable_dns_hostnames = true

  // 리소스에 대한 태그를 설정
  tags = {
    Name = "${local.name}-vpc"
    Team = var.team_tag
  }
}

// AWS 서브넷 리소스를 생성하고 이름을 'subnet_1'로 설정
resource "aws_subnet" "subnet_1" {
  // 이 서브넷이 속할 VPC를 지정. 여기서는 'vpc_1'를 선택
  vpc_id = aws_vpc.vpc_1.id
  // 서브넷의 IP 주소 범위를 설정
  cidr_block = "10.0.1.0/24"
  // 서브넷이 위치할 가용 영역을 설정
  availability_zone = "${var.region}a"
  // 이 서브넷에 배포되는 인스턴스에 공용 IP를 자동으로 할당
  map_public_ip_on_launch = true

  // 리소스에 대한 태그를 설정
  tags = {
    Name = "${local.name}-subnet-1"
    Team = var.team_tag
  }
}

// AWS 서브넷 리소스를 생성하고 이름을 'subnet_2'로 설정
resource "aws_subnet" "subnet_2" {
  // 이 서브넷이 속할 VPC를 지정. 여기서는 'vpc_1'를 선택
  vpc_id = aws_vpc.vpc_1.id
  // 서브넷의 IP 주소 범위를 설정
  cidr_block = "10.0.2.0/24"
  // 서브넷이 위치할 가용 영역을 설정
  availability_zone = "${var.region}b"
  // 이 서브넷에 배포되는 인스턴스에 공용 IP를 자동으로 할당
  map_public_ip_on_launch = true

  // 리소스에 대한 태그를 설정
  tags = {
    Name = "${local.name}-subnet-2"
    Team = var.team_tag
  }
}

// AWS 인터넷 게이트웨이 리소스를 생성하고 이름을 'igw_1'로 설정
resource "aws_internet_gateway" "igw_1" {
  // 이 인터넷 게이트웨이가 연결될 VPC를 지정. 여기서는 'vpc_1'를 선택
  vpc_id = aws_vpc.vpc_1.id

  // 리소스에 대한 태그를 설정
  tags = {
    Name = "${local.name}-internet-gateway"
    Team = var.team_tag
  }
}


// AWS 라우트 테이블 리소스를 생성하고 이름을 'rt_1'로 설정
resource "aws_route_table" "rt_1" {
  // 이 라우트 테이블이 속할 VPC를 지정. 여기서는 'vpc_1'를 선택
  vpc_id = aws_vpc.vpc_1.id

  // 라우트 규칙을 설정. 여기서는 모든 트래픽(0.0.0.0/0)을 'igw_1' 인터넷 게이트웨이로 보냄
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  // 리소스에 대한 태그를 설정
  tags = {
    Name = "${local.name}-routing-table"
    Team = var.team_tag
  }
}

// 라우트 테이블 'rt_1'과 서브넷 'subnet_1'을 연결
resource "aws_route_table_association" "association_1" {
  // 연결할 서브넷을 지정
  subnet_id = aws_subnet.subnet_1.id
  // 연결할 라우트 테이블을 지정
  route_table_id = aws_route_table.rt_1.id
}

// 라우트 테이블 'rt_1'과 서브넷 'subnet_2'을 연결
resource "aws_route_table_association" "association_2" {
  // 연결할 서브넷을 지정
  subnet_id = aws_subnet.subnet_2.id
  // 연결할 라우트 테이블을 지정
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "sg_1" {
  name = "${local.name}-sg-1"

  # HTTP
  ingress {
    from_port = 80
    to_port   = 80
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # 운영이면 내 IP로 제한 권장
  }

  ingress {
    description = "SSH access"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${local.name}-sg-1"
    Team = var.team_tag
  }
}

# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "ec2_role_1" {
  name = "${local.name}-ec2-role-1"

  # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "instance_profile_1" {
  name = "${local.name}-instance-profile-1"
  role = aws_iam_role.ec2_role_1.name
}
# Elastic IP
# 이미 콘솔로 할당받음
#resource "aws_eip" "ec2" {
 # domain = "vpc"

#  tags = {
#    Name = "${local.name}-eip"
# }
#}

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# nginx 설치
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest
# ha proxy 설치
## 설정파일을 위한 디렉토리 생성
mkdir -p /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/lua
cat << 'EOF' > /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/lua/retry_on_502_504.lua
core.register_action("retry_on_502_504", { "http-res" }, function(txn)
  local status = txn.sf:status()
  if status == 502 or status == 504 then
    txn:Done()
  end
end)
EOF
## 설정파일 생성
echo -e "
global
    lua-load /usr/local/etc/haproxy/lua/retry_on_502_504.lua
resolvers docker
    nameserver dns1 127.0.0.11:53
    resolve_retries       3
    timeout retry         1s
    hold valid            10s
defaults
    mode http
    timeout connect 5s
    timeout client 60s
    timeout server 60s
frontend http_front
    bind *:80
    acl host_app1 hdr_beg(host) -i api.p-14626.qqwas.shop
    use_backend http_back_1 if host_app1
backend http_back_1
    balance roundrobin
    option httpchk GET /actuator/health
    default-server inter 2s rise 1 fall 1 init-addr last,libc,none resolvers docker
    option redispatch
    http-response lua.retry_on_502_504
    server app_server_1_1 app1_1:8080 check
    server app_server_1_2 app1_2:8080 check
" > /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/haproxy.cfg
docker run \
  -d \
  --network common \
  -p 8090:80 \
  -v /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy:/usr/local/etc/haproxy \
  -e TZ=Asia/Seoul \
  --name ha_proxy_1 \
  haproxy

# redis 설치
docker run -d \
  --name=redis_1 \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  redis --requirepass ${var.password_1}

echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin
END_OF_FILE
}


# EC2 인스턴스 생성
resource "aws_instance" "ec2" {
  # 사용할 AMI ID
  ami = "ami-062cddb9d94dcf95d"
  # EC2 인스턴스 유형
  instance_type = var.instance_type
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.subnet_2.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.sg_1.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = aws_iam_instance_profile.instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    Name = "${local.name}-ec2-1"
    Team = var.team_tag
  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_size
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

# 생성한 EIP와 실행 중인 EC2를 연결
#resource "aws_eip_association" "ec2" {
#  instance_id   = aws_instance.ec2.id
#  allocation_id = aws_eip.ec2.id
#}