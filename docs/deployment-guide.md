# 어디가까 배포 가이드

EC2 기반 백엔드 배포와 도메인/HTTPS 적용 과정을 기록합니다.

## 현재 배포 상태

- EC2 Ubuntu 인스턴스 생성 완료
- Docker, Docker Compose 설치 완료
- 백엔드 Docker Compose 배포 완료
- Nginx 설치 및 80 포트 reverse proxy 설정 완료
- `http://EC2_PUBLIC_IP/actuator/health` 응답 확인 완료
- PostgreSQL은 RDS로 분리 완료
- Redis는 현재 EC2 Docker 컨테이너로 운영 중
- 도메인 `eodigakka.xyz` 구매 완료
- Namecheap 자동 갱신 OFF 확인 완료
- Cloudflare에 `eodigakka.xyz` 사이트 추가 완료
- Cloudflare DNS에 `api.eodigakka.xyz` A 레코드 추가 완료
- Namecheap 네임서버를 Cloudflare 네임서버로 변경 완료
- `http://api.eodigakka.xyz/actuator/health` 응답 확인 완료
- Cloudflare 프록시와 Flexible SSL/TLS로 `https://api.eodigakka.xyz/actuator/health` 응답 확인 완료

## 배포 구조

```text
Client
  -> Domain / DNS
  -> Nginx
  -> Spring Boot Docker Container
  -> RDS PostgreSQL
  -> Redis Docker Container
```

현재 1차 목표는 EC2 단일 서버에 백엔드와 Redis를 올리고, DB만 RDS로 분리하는 구조입니다.
Cloudflare Flexible SSL/TLS를 사용해 브라우저와 Cloudflare 사이의 HTTPS는 적용된 상태입니다.
발표 전 안정화 이후에는 프론트 배포, Cookie 보안 설정, Kakao Redirect URI를 함께 정리합니다.

## EC2 배포

### Docker 설치 확인

```bash
docker --version
docker compose version
```

### 운영 환경 변수

EC2의 프로젝트 루트에 `.env`를 생성합니다.
운영 비밀값은 Git에 커밋하지 않습니다.

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://{rds-endpoint}:5432/eodigakka?sslmode=require
DB_USERNAME=eodigakka
DB_PASSWORD={rds-master-password}

REDIS_HOST=redis
REDIS_PORT=6379

FRONTEND_ORIGINS=http://localhost:5173
SERVER_PORT=8080
BACKEND_PORT=8080

KAKAO_REST_API_KEY={kakao-rest-api-key}
KAKAO_CLIENT_SECRET={kakao-client-secret}
KAKAO_ALLOWED_REDIRECT_URIS=http://localhost:5173/oauth/kakao/callback

JWT_SECRET={long-random-secret}
JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=1800
JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=1209600

APP_COOKIE_SECURE=false
APP_COOKIE_SAME_SITE=Lax
APP_GUEST_COOKIE_SECURE=false
APP_GUEST_COOKIE_SAME_SITE=Lax
APP_GUEST_COOKIE_MAX_AGE_DAYS=30
```

도메인과 HTTPS가 적용되면 Cookie 설정은 `Secure=true` 기준으로 변경합니다.

```text
APP_COOKIE_SECURE=true
APP_COOKIE_SAME_SITE=None
APP_GUEST_COOKIE_SECURE=true
APP_GUEST_COOKIE_SAME_SITE=None
```

### 운영 Compose 실행

RDS를 사용하므로 운영에서는 `docker-compose.prod.yml`을 사용합니다.

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
```

### 백엔드 로그 확인

```bash
docker compose -f docker-compose.prod.yml logs -f backend
```

정상 기준:

```text
Started EodigakkaBackendApplication
```

### Health Check

EC2 내부:

```bash
curl http://localhost:8080/actuator/health
```

Nginx 적용 후:

```bash
curl http://localhost/actuator/health
```

브라우저:

```text
http://EC2_PUBLIC_IP/actuator/health
```

정상 응답:

```json
{"status":"UP"}
```

## Nginx

Nginx는 EC2 80 포트 요청을 Spring Boot 8080 포트로 전달합니다.

```text
80 -> Nginx -> 8080 Spring Boot
```

Nginx 설정 변경 후 문법 확인:

```bash
sudo nginx -t
```

Nginx 재시작:

```bash
sudo systemctl reload nginx
```

## RDS PostgreSQL

### 생성 기준

- 엔진: PostgreSQL
- 초기 데이터베이스 이름: `eodigakka`
- 마스터 사용자: `eodigakka`
- SSL 연결 사용: `sslmode=require`
- 백업 보존 기간: 1일
- 삭제 방지: 현재는 OFF

### EC2에서 접속 확인

```bash
psql -h {rds-endpoint} -U eodigakka -d eodigakka
```

정상 접속 후 확인:

```sql
select current_database(), current_user;
```

정상 결과:

```text
current_database | current_user
-----------------+-------------
eodigakka        | eodigakka
```

### 주의사항

- RDS 보안 그룹은 EC2에서 접근 가능해야 합니다.
- RDS 접속에는 `sslmode=require`를 사용합니다.
- 로컬 Docker PostgreSQL 비밀번호와 RDS 비밀번호는 달라도 됩니다.
- 운영 DB 비밀번호는 팀 전체 공유 대상이 아닙니다.

## 도메인 구매

### 구매 정보

- 등록기관: Namecheap
- 도메인: `eodigakka.xyz`
- 기간: 1년
- Domain Privacy: ON
- Auto-Renew: OFF 확인 완료

### 구매 직후 필수 확인

1. Namecheap `Domain List`에서 `Auto-Renew: OFF` 확인
2. Domain Privacy ON 확인
3. Namecheap ICANN 연락처 인증 메일 확인
4. `Please verify your contact information` 메일의 인증 링크 클릭

연락처 인증을 하지 않으면 지정된 기한 이후 도메인이 정지될 수 있습니다.

## Cloudflare 연결

### Cloudflare 사이트 추가

1. Cloudflare 대시보드 접속
2. `웹사이트`
3. `도메인 연결`
4. `eodigakka.xyz` 입력
5. 무료 플랜 선택

### Cloudflare DNS 레코드

현재 백엔드 API용 레코드는 다음 기준으로 추가합니다.

```text
형식: A
이름: api
콘텐츠: {EC2_PUBLIC_IP}
프록시 상태: 프록시됨
TTL: 자동
```

초기 연결 확인 단계에서는 `DNS 전용`으로 두고 HTTP health check를 확인했습니다.
이후 HTTPS 적용을 위해 `프록시됨`으로 변경했습니다.

### Namecheap 네임서버 변경

Namecheap `Domain List > eodigakka.xyz > Manage > Nameservers`에서 `Custom DNS`를 선택하고 Cloudflare 네임서버를 입력합니다.

현재 할당된 Cloudflare 네임서버:

```text
keira.ns.cloudflare.com
leonard.ns.cloudflare.com
```

Namecheap에 저장하면 다음 안내가 표시될 수 있습니다.

```text
DNS server update may take up to 48 hours to take effect.
```

이는 정상이며 네임서버 전파 대기 상태입니다.
현재는 Cloudflare 네임서버 전파가 완료된 상태입니다.

### 전파 확인

Windows PowerShell:

```powershell
nslookup -type=ns eodigakka.xyz 1.1.1.1
```

성공 기준:

```text
eodigakka.xyz nameserver = keira.ns.cloudflare.com
eodigakka.xyz nameserver = leonard.ns.cloudflare.com
```

API 서브도메인 확인:

```powershell
nslookup api.eodigakka.xyz 1.1.1.1
```

성공 기준은 EC2 Public IP가 반환되는 것입니다.

브라우저 확인:

```text
http://api.eodigakka.xyz/actuator/health
```

정상 응답:

```json
{"status":"UP"}
```

## HTTPS 적용 상태

현재 Cloudflare SSL/TLS 모드는 `Flexible`입니다.

```text
Browser -> Cloudflare: HTTPS
Cloudflare -> EC2 Nginx: HTTP
EC2 Nginx -> Spring Boot: HTTP
```

확인 주소:

```text
https://api.eodigakka.xyz/actuator/health
```

정상 응답:

```json
{"status":"UP"}
```

### Flexible 모드 사용 이유

EC2 Nginx에 아직 원본 서버 인증서를 설치하지 않은 상태이므로 Cloudflare `Full` 또는 `Full (strict)` 모드에서는 원본 연결이 실패할 수 있습니다.
발표 전 빠른 HTTPS 확보를 위해 우선 `Flexible` 모드로 적용했습니다.

### 후속 개선 방향

최종 운영에서는 EC2 Nginx에도 인증서를 설치하고 Cloudflare SSL/TLS 모드를 `Full (strict)`로 변경합니다.

```text
Browser -> Cloudflare: HTTPS
Cloudflare -> EC2 Nginx: HTTPS
EC2 Nginx -> Spring Boot: HTTP
```

## HTTPS 이후 보류 항목

백엔드 HTTPS 주소는 열렸지만 프론트 배포 주소가 아직 확정되지 않았으므로 아래 작업은 보류합니다.

- EC2 Nginx 원본 인증서 적용
- Cloudflare SSL/TLS `Full (strict)` 전환
- Kakao Redirect URI를 운영 HTTPS 주소로 변경
- 운영 Cookie `Secure=true`, `SameSite=None` 최종 검증
- 프론트 운영 API Base URL 확정

## HTTPS 적용 후 변경 예정

프론트와 백엔드가 분리 도메인으로 운영되는 경우 다음 설정을 적용합니다.

```text
FRONTEND_ORIGINS=https://{frontend-domain}
KAKAO_ALLOWED_REDIRECT_URIS=https://{frontend-domain}/oauth/kakao/callback
APP_COOKIE_SECURE=true
APP_COOKIE_SAME_SITE=None
APP_GUEST_COOKIE_SECURE=true
APP_GUEST_COOKIE_SAME_SITE=None
```

프론트 요청에서는 Cookie 인증이 필요한 API에 다음 설정이 필요합니다.

```text
fetch: credentials: "include"
axios: withCredentials: true
```

## 다음 작업

1. 프론트 배포 주소 확정
2. 백엔드 `FRONTEND_ORIGINS` 운영 주소 반영
3. Kakao Redirect URI 운영 주소 추가
4. 운영 Cookie `Secure=true`, `SameSite=None` 적용
5. 게스트 세션 Cookie와 Refresh Token Cookie 운영 브라우저 검증
6. EC2 Nginx 원본 인증서 적용
7. Cloudflare SSL/TLS `Full (strict)` 전환
