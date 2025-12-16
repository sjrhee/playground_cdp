# Kubernetes Migration Guide for Playground CDP

이 문서는 웹 애플리케이션(`playground_cdp`)을 Kubernetes(k8s) 환경으로 마이그레이션하기 위한 가이드입니다.
MySQL 데이터베이스는 외부(기존 서버, `192.168.100.13`)를 계속 사용하며, 웹 애플리케이션만 컨테이너화하여 배포합니다.

## 1. 사전 준비 사항 (Prerequisites)

*   **Java JDK 11** 이상
*   **Maven**
*   **Docker**
*   **Kubernetes Cluster**
*   **kubectl** CLI 도구

## 2. 파일 구성

*   `Dockerfile`: 웹 애플리케이션(Tomcat) 이미지를 생성하는 명세
*   `build_and_save.sh`: Maven 빌드, Docker 이미지 빌드 및 tar 아카이브 저장을 수행하는 스크립트
*   `mysql-external.yaml`: 외부 MySQL(`192.168.100.13`)을 클러스터 내에서 `mysql` 호스트명으로 연결해주는 Service & Endpoints 설정
*   `web-deployment.yaml`: 웹 애플리케이션 Deployment 및 Service(LoadBalancer) 정의

## 3. 이미지 빌드 및 추출 (Build & Save)

`k8s-migration` 폴더에 생성된 스크립트를 실행하여 애플리케이션을 빌드하고 Docker 이미지를 tar 파일로 추출합니다.

```bash
cd k8s-migration
./build_and_save.sh
```

이 스크립트 실행 후 `playground-web.tar` 파일이 생성됩니다.


## 4. 원격 배포 (Transfer & Deploy)

`deploy_remote.sh` 스크립트를 사용하여 원격 서버(`192.168.100.11`)로 배포를 자동화할 수 있습니다.
이 스크립트는 원격 서버에 Docker가 설치되어 있지 않아도 `ctr` (containerd)을 사용하여 이미지를 로드합니다.

```bash
cd k8s-migration
./deploy_remote.sh
```

**스크립트 동작 과정:**
1.  **SCP 전송**: `playground-web.tar`, `mysql-external.yaml`, `web-deployment.yaml` 파일을 원격 서버로 전송합니다.
2.  **이미지 임포트 (`ctr`)**: `docker load` 대신 `sudo ctr -n k8s.io images import` 명령을 사용하여 Kubernetes 네임스페이스에 이미지를 등록합니다.
3.  **K8s 리소스 배포**: `kubectl apply` 명령을 원격에서 실행하여 배포를 완료합니다.

## 5. 수동 배포 (Manual Steps)

스크립트를 사용하지 않고 수동으로 진행하려면 다음 명령을 참고하세요.

**1. 파일 전송:**
```bash
scp k8s-migration/playground-web.tar k8s-migration/*.yaml ubuntu@192.168.100.11:~/
```

**2. 이미지 로드 (Remote Server):**
원격 서버에 접속하여 실행합니다.
```bash
ssh ubuntu@192.168.100.11
sudo ctr -n k8s.io images import playground-web.tar
```

**3. 리소스 적용 (Remote Server):**
```bash
kubectl apply -f mysql-external.yaml
kubectl apply -f web-deployment.yaml
```

## 6. 확인 및 주의사항

*   **DB 연결 확인**: 웹 애플리케이션 파드가 정상적으로 구동(`Running`)되면, 서비스의 External IP로 접속하여 DB 데이터를 잘 불러오는지 확인합니다.
*   **방화벽(Firewall)**: `192.168.100.13` 서버의 MySQL(3306 포트)이 Kubernetes 클러스터 대역(Pod Network IP 등)에서의 접근을 허용하는지 반드시 확인해야 합니다.
    *   MySQL user(`testuser`)가 해당 IP 대역에서 접속 가능한 권한을 가지고 있어야 합니다.
