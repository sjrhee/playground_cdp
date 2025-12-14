# Playground CDP

**Playground CDP**는 MySQL 샘플 데이터베이스(Employees)를 활용하여 민감 정보(예: 주민등록번호)에 대한 **CADP(CipherTrust Application Data Protection)** 및 **CRDP(Container Real-time Data Protection)** 암호화/복호화 통합 기능을 시연하는 Java 웹 애플리케이션입니다.

## 주요 기능 (Features)

*   **직원 목록 조회**: 페이징 처리된 직원 목록 및 신상 정보 조회.
*   **원문 조회 (Encrypted View)**: 데이터베이스에 암호화되어 저장된 상태 그대로의 SSN(주민등록번호) 확인.
*   **CADP 복호화 (Decrypted View)**: Thales CADP Java 라이브러리를 사용하여 애플리케이션 레벨에서 복호화된 SSN 확인.
*   **CRDP 복호화 (Decrypted View)**: Thales CRDP REST API를 호출하여 컨테이너 환경에서 실시간으로 복호화된 SSN 확인.

## 사전 요구 사항 (Prerequisites)

이 프로젝트를 실행하기 위해서는 다음 도구들이 설치되어 있어야 합니다.

*   **Docker & Docker Compose**: 데이터베이스(MySQL) 및 웹 애플리케이션 서버(Tomcat) 컨테이너 실행용.
*   **Java 11 이상**: 애플리케이션 빌드용 (컨테이너 내부가 아닌 로컬 빌드 시).
*   **Maven**: 프로젝트 의존성 관리 및 패키징용.

## 설치 및 실행 방법 (Installation)

### 1. 프로젝트 설정
스크립트 실행 권한을 부여합니다.
```bash
cd playgroud_cdp
chmod +x scripts/*.sh
```

### 2. 데이터베이스 및 워크스페이스 준비
터미널에서 제공된 스크립트를 사용하여 Maven 빌드를 수행하고, 생성된 WAR 파일을 배포 경로로 복사합니다.
```bash
cd scripts
./01_setup_workspace.sh
```
이 스크립트는 다음 작업을 수행합니다:
1.  `mvn clean package`를 실행하여 `java-db-docker` 프로젝트 빌드.
2.  빌드된 `ROOT.war` (또는 압축 해제된 폴더)를 `tomcat_mysql_docker/target/ROOT`로 배포.

### 3. 데이터 초기화 (필요 시)
MySQL 데이터베이스에 샘플 데이터를 로드하거나 초기화하려면 다음 스크립트를 사용합니다. (Docker 컨테이너가 실행 중이어야 함)
```bash
./i03_reload_employees.sh
```

## 설정 (Configuration)

애플리케이션이 CRDP 및 CADP 서비스와 올바르게 통신하기 위해 다음 설정이 필요합니다.

### 1. CRDP 설정 (`crdp.properties`)
CRDP 복호화 기능을 사용하기 위해서는 `src/main/resources/crdp.properties` 파일에 올바른 연결 정보를 입력해야 합니다.

**파일 경로:** `src/main/resources/crdp.properties`

```properties
crdp_endpoint=192.168.0.10:32182   # CRDP 서비스 IP 및 포트
crdp_policy=dev-users-policy       # CRDP 보호 정책 이름
crdp_user_name=dev-user01          # 인증에 사용할 CRDP 사용자명
crdp_jwt=eyJ...                    # 유효한 JWT 인증 토큰
```
### 2. CADP 설정
CADP 라이브러리 설정은 `src/main/resources/` 내의 관련 파일들(`cadp.properties` 등)을 참고하거나, 소스 코드 내의 `CadpClient` 설정을 따릅니다.

### 3. 데이터베이스 연결 설정
기본적으로 DB 연결 정보는 서블릿 코드 내에 설정되어 있거나 컨테이너 환경 변수를 따릅니다.
*   **URL**: `jdbc:mysql://mysql:3306/mysql_employees`
*   **User**: `testuser`
*   **Password**: `testpassword`

## 사용 방법 (Usage)

모든 컨테이너가 실행 중이고 배포가 완료되면 브라우저를 열고 다음 주소로 접속합니다.

**메인 페이지:**
[http://localhost:8080/index.html](http://localhost:8080/index.html)

*   **직원 신상 정보(원문조회)** 버튼: 암호화된 데이터 확인.
*   **직원 신상 정보(CADP 복호화)** 버튼: CADP를 통해 복호화된 데이터 확인.
*   **직원 신상 정보(CRDP 복호화)** 버튼: CRDP를 통해 복호화된 데이터 확인.
