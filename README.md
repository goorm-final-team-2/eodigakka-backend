# 어디가까 Backend

약속 장소 탐색, 후보 투표, 최종 장소 확정과 실시간 위치 공유를 제공하는 어디가까 서비스의 백엔드 API입니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.15
- Gradle 8.14.5
- Spring Data JPA
- Spring Security
- PostgreSQL 17
- Redis 7.4
- Flyway
- WebSocket/STOMP
- Springdoc OpenAPI
- Docker Compose

## 사전 준비

- JDK 21
- Docker Desktop
- Git

## 로컬 실행

### 1. 저장소 Clone

```bash
git clone https://github.com/goorm-final-team-2/eodigakka-backend.git
cd eodigakka-backend
git switch develop
```

### 2. 환경 변수 파일 준비

```powershell
Copy-Item .env.example .env
```

기본 로컬 설정은 별도 수정 없이 사용할 수 있습니다. 실제 API 키와 운영 비밀값은 `.env`에만 입력하고 Git에 커밋하지 않습니다.

> Docker Compose는 프로젝트 루트의 `.env`를 자동으로 읽습니다. 반면 `gradlew bootRun`과 IntelliJ는 `.env`를 자동으로 읽지 않습니다. Spring Boot를 로컬에서 직접 실행할 때 필요한 비밀값은 현재 터미널 또는 IntelliJ Run Configuration의 환경변수로 등록해야 합니다.

### 3. PostgreSQL과 Redis 실행

```bash
docker compose up -d
docker compose ps
```

기본 호스트 포트는 Backend `8080`, PostgreSQL `5432`, Redis `6379`입니다. 기존 프로그램과 충돌하면 `.env`에서 `BACKEND_PORT`, `POSTGRES_HOST_PORT`, `REDIS_HOST_PORT`를 변경할 수 있습니다.

Spring Boot를 IDE에서 직접 실행할 때 PostgreSQL 또는 Redis 호스트 포트를 변경했다면 `DB_URL`과 `REDIS_PORT`도 같은 값으로 맞춰야 합니다.

Windows에서 PostgreSQL 서비스 확인:

```powershell
Get-Service | Where-Object { $_.Name -match "postgres" }
```

관리자 PowerShell에서 서비스 일시 중지:

```powershell
Stop-Service {PostgreSQL 서비스 이름}
```

### 4. Spring Boot 실행

Windows:

```powershell
.\gradlew bootRun
```

macOS/Linux:

```bash
./gradlew bootRun
```

백엔드까지 모두 Docker로 실행하려면 다음 명령을 사용합니다.

```bash
docker compose --profile full up --build -d
```

## 접속 주소

| 항목 | URL |
| --- | --- |
| API 서버 | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Health Check | `http://localhost:8080/actuator/health` |
| PostgreSQL | `localhost:5432` (`POSTGRES_HOST_PORT`로 변경 가능) |
| Redis | `localhost:6379` (`REDIS_HOST_PORT`로 변경 가능) |

## 테스트와 빌드

Docker Desktop을 실행한 상태에서 수행합니다. 테스트는 Testcontainers가 별도의 PostgreSQL 17 컨테이너를 생성합니다.

```powershell
.\gradlew spotlessApply
.\gradlew check
```

- `spotlessApply`: Java 코드를 팀 공통 포맷으로 자동 정리합니다.
- `check`: 테스트와 Spotless 포맷 검사를 함께 실행합니다.
- 서버 실행은 `.\gradlew bootRun`을 사용합니다.

Docker 엔진이 꺼져 있으면 Testcontainers 테스트가 실패합니다.

## 환경 프로필

| 프로필 | 파일 | 용도 |
| --- | --- | --- |
| 기본 | `application.yml` | 공통 설정 |
| local | `application-local.yml` | 로컬 개발 |
| prod | `application-prod.yml` | 운영 배포 |

기본 활성 프로필은 `local`입니다. 운영에서는 다음과 같이 환경변수로 지정합니다.

```text
SPRING_PROFILES_ACTIVE=prod
```

## DB 마이그레이션

DB 스키마는 `src/main/resources/db/migration`의 Flyway SQL로 관리합니다.

```text
V1__baseline.sql
V2__create_initial_schema.sql
V3__{next_change}.sql
```

- 마이그레이션 번호는 작업 시작 전에 팀 채널에서 선점합니다.
- 이미 공유된 마이그레이션 파일은 수정하지 않고 새 번호의 파일을 추가합니다.
- `V2__create_initial_schema.sql`은 최신 ERD 기준 초기 스키마이므로 병합 후 수정하지 않습니다.
- 운영 환경에서 `ddl-auto=create` 또는 `ddl-auto=update`를 사용하지 않습니다.

## 브랜치 전략

```text
main
develop
feature/{기능명}
fix/{수정명}
chore/{작업명}
```

기능 브랜치는 최신 `develop`에서 생성하고, 작업 후 `develop`을 대상으로 Pull Request를 생성합니다.

```bash
git switch develop
git pull origin develop
git switch -c feature/appointment-room
```

## 공통 응답

성공 응답:

```json
{
  "data": {},
  "message": "success"
}
```

오류 응답:

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청값이 올바르지 않습니다.",
  "errors": {
    "title": "약속 이름은 필수입니다."
  },
  "timestamp": "2026-06-15T12:00:00Z"
}
```

## 현재 공통 설정 주의사항

- 인증 기능 구현 전이므로 `SecurityConfig`의 일반 API 요청은 임시로 허용되어 있습니다.
- 카카오 로그인 기능 개발 시 JWT 인증 필터와 엔드포인트별 권한 정책을 적용해야 합니다.
- Kakao REST API Key, Client Secret, JWT Secret은 백엔드 환경변수로만 관리합니다.
