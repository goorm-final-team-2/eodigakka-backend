# 어디가까 Backend API Guide

프론트 연동을 위한 백엔드 API 계약 문서입니다.

## 기본 URL

로컬 개발:

```text
http://localhost:8080
ws://localhost:8080/ws
```

배포 환경:

```text
https://api.eodigakka.xyz
wss://api.eodigakka.xyz/ws
```

프론트에서는 API 서버 주소를 환경변수로 관리합니다.

## 공통 응답

성공 응답은 `data`, `message` 형식입니다.

```json
{
  "data": {},
  "message": "success"
}
```

`data`가 없는 성공 응답은 `message`만 내려갈 수 있습니다.

```json
{
  "message": "success"
}
```

## 인증 방식

### 로그인 사용자

로그인 사용자는 Access Token을 `Authorization` 헤더로 전달합니다.

```http
Authorization: Bearer {accessToken}
```

### 게스트 사용자

게스트 사용자는 게스트 입장 API 응답의 `Set-Cookie`로 내려온 `guestSession` 쿠키로 인증됩니다.

```http
Cookie: guestSession={guestSession}
```

`guestSession`은 HttpOnly Cookie로 관리되므로 프론트 JavaScript에서 직접 읽거나 저장하지 않습니다.
게스트 인증이 필요한 요청은 `fetch`의 `credentials: "include"` 또는 axios의 `withCredentials: true`를 사용해 쿠키를 함께 전송해야 합니다.

프론트 저장소에는 게스트 토큰을 저장하지 않습니다.
게스트 세션 유지와 만료 검증은 백엔드가 담당합니다.
`X-Guest-Token`, `X-Guest-Session` 헤더는 프론트 연동 계약으로 사용하지 않습니다.

로컬 HTTP 환경에서는 Refresh Token Cookie와 게스트 세션 Cookie 모두 `Secure=false`, `SameSite=Lax`를 사용합니다.
HTTPS 배포 환경에서 프론트와 백엔드 도메인이 분리되면 두 Cookie 모두 `Secure=true`, `SameSite=None` 조합을 검토해야 합니다.

## 약속방 상태

```text
PLANNING -> CONFIRMED -> CLOSED
```

- `PLANNING`: 장소 후보 등록/삭제, 투표, 확정 장소 선택 가능
- `CONFIRMED`: 위치 공유/조회 가능
- `CLOSED`: 게스트 참여, 위치 공유/조회 불가

## 권한 요약

| 기능 | 로그인 사용자 | 게스트 사용자 | 비고 |
| --- | --- | --- | --- |
| 카카오 로그인 | 가능 | 불가 | 로그인/회원가입 통합 |
| Access Token 재발급 | 가능 | 불가 | Refresh Token Cookie 기준 |
| 약속방 생성 | 가능 | 불가 | 로그인 사용자만 가능 |
| 약속방 목록/상세 조회 | 가능 | 불가 | 로그인 사용자 본인 약속방 기준 |
| 약속방 수정/삭제/종료 | 방장만 가능 | 불가 | 게스트는 방장 불가 |
| 초대 코드 로그인 참여 | 가능 | 불가 | `Authorization` 필요 |
| 초대 코드 미리보기 | 가능 | 가능 | 로그인 없이 호출 가능 |
| 게스트 입장 | 불가 | 가능 | 성공 시 `guestSession` Cookie 발급 |
| 참여자 목록 조회 | 참여자만 가능 | 참여자만 가능 | `appointmentMember` 기준 |
| 장소 검색 | 참여자만 가능 | 참여자만 가능 | 검색 결과는 저장하지 않음 |
| 장소 후보 등록/목록/삭제 | 참여자만 가능 | 참여자만 가능 | 삭제는 등록자 또는 방장 |
| 투표/투표 취소/결과 조회 | 참여자만 가능 | 참여자만 가능 | 투표 변경은 PUT으로 처리 |
| 확정 장소 선택 | 방장만 가능 | 불가 | 성공 시 `CONFIRMED` |
| 확정 장소 조회 | 참여자만 가능 | 참여자만 가능 | 확정 장소가 있어야 함 |
| 위치 HTTP fallback | 참여자만 가능 | 참여자만 가능 | `CONFIRMED` 상태만 가능 |
| WebSocket 위치 공유 | 참여자만 가능 | 참여자만 가능 | STOMP 인증 후 사용 |

## Auth API

### 카카오 로그인

```http
POST /api/auth/kakao
```

요청:

```json
{
  "code": "kakao-auth-code",
  "redirectUri": "http://localhost:3000/oauth/kakao/callback"
}
```

응답:

```json
{
  "data": {
    "accessToken": "access-token",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "nickname": "홍길동",
      "profileImage": "https://..."
    },
    "isNewUser": true
  },
  "message": "success"
}
```

Refresh Token은 HttpOnly Cookie로 내려갑니다.

### Access Token 재발급

```http
POST /api/auth/refresh
```

Refresh Token Cookie를 함께 전송해야 합니다.

### 로그아웃

```http
POST /api/auth/logout
```

Refresh Token Cookie를 제거합니다.

## User API

### 내 정보 조회

```http
GET /api/users/me
Authorization: Bearer {accessToken}
```

## Appointment API

### 약속방 생성

```http
POST /api/appointments
Authorization: Bearer {accessToken}
```

요청:

```json
{
  "title": "강남 저녁 약속",
  "appointmentDate": "2026-07-01",
  "appointmentTime": "19:00:00",
  "description": "저녁 먹을 장소 정하기",
  "preferredArea": "강남역",
  "notice": "늦지 않기"
}
```

### 내 약속방 목록 조회

```http
GET /api/appointments
Authorization: Bearer {accessToken}
```

### 약속방 단건 조회

```http
GET /api/appointments/{appointmentId}
Authorization: Bearer {accessToken}
```

### 약속방 수정

```http
PATCH /api/appointments/{appointmentId}
Authorization: Bearer {accessToken}
```

방장만 가능하며 `PLANNING` 상태에서만 가능합니다.

### 약속방 삭제

```http
DELETE /api/appointments/{appointmentId}
Authorization: Bearer {accessToken}
```

방장만 가능하며 `PLANNING` 상태에서만 가능합니다.

### 약속방 종료

```http
PATCH /api/appointments/{appointmentId}/close
Authorization: Bearer {accessToken}
```

방장만 가능하며 `CONFIRMED` 상태에서만 가능합니다.
성공 시 약속방 상태가 `CLOSED`로 변경됩니다.

### 약속방 참여자 목록 조회

```http
GET /api/appointments/{appointmentId}/members
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

약속방 참여자만 조회할 수 있습니다.

응답:

```json
{
  "data": [
    {
      "memberId": 1,
      "memberType": "USER",
      "role": "HOST",
      "displayName": "홍길동",
      "profileImage": "https://...",
      "joinedAt": "2026-06-21T12:00:00Z"
    },
    {
      "memberId": 2,
      "memberType": "GUEST",
      "role": "MEMBER",
      "displayName": "철수",
      "profileImage": null,
      "joinedAt": "2026-06-21T12:01:00Z"
    }
  ],
  "message": "success"
}
```

## Invite / Guest API

### 초대 코드 약속방 참여

```http
POST /api/appointments/join
Authorization: Bearer {accessToken}
```

요청:

```json
{
  "inviteCode": "A7K2P9QX"
}
```

### 초대 미리보기

```http
GET /api/appointments/invite/{inviteCode}
```

로그인 없이 호출 가능합니다.

### 게스트 입장

```http
POST /api/appointments/guests
```

요청:

```json
{
  "inviteCode": "A7K2P9QX",
  "guestName": "철수"
}
```

응답 시 `guestSession` HttpOnly Cookie가 함께 내려갑니다.
프론트는 응답 본문에 게스트 토큰을 저장하지 않고, 이후 게스트 요청에 쿠키가 포함되도록 `credentials` 설정만 유지합니다.

```json
{
  "data": {
    "appointment": {
      "id": 10,
      "title": "강남 저녁 약속",
      "role": "MEMBER"
    },
    "guest": {
      "memberId": 100,
      "guestName": "철수"
    }
  },
  "message": "success"
}
```

게스트 세션 정책:

- `guestSession` 기본 유지 기간은 `APP_GUEST_COOKIE_MAX_AGE_DAYS` 기준이며 기본값은 30일입니다.
- 기존 브라우저에 유효한 `guestSession` 쿠키가 있으면 이후 게스트 API 요청에 그대로 사용할 수 있습니다.
- 쿠키가 없거나 유효하지 않으면 `GUEST_SESSION_INVALID`가 내려갈 수 있으며, 프론트는 게스트 입장 화면으로 유도합니다.
- 쿠키가 만료되었거나 폐기된 세션이면 `GUEST_SESSION_EXPIRED`가 내려갈 수 있으며, 프론트는 다시 게스트 입장을 안내합니다.
- 같은 약속방에서 이미 사용 중인 게스트 이름으로 새로 입장하면 `GUEST_NAME_ALREADY_EXISTS`가 내려갑니다.

## Place Search API

카카오 Local API 기반 장소 검색입니다.
로그인 사용자와 게스트 모두 호출할 수 있지만, 해당 약속방 참여자여야 합니다.
검색 결과는 DB에 저장되지 않으며, 프론트에서 선택한 장소를 장소 후보 등록 API에 전달할 때 사용합니다.

### 카카오 장소 검색

```http
GET /api/appointments/{appointmentId}/places/search?query=강남역
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

요청 파라미터:

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `query` | 필수 | 검색 키워드 |
| `x` | 선택 | 중심 좌표 경도 |
| `y` | 선택 | 중심 좌표 위도 |
| `radius` | 선택 | 중심 좌표 기준 검색 반경, 최대 20000m |
| `page` | 선택 | 결과 페이지, 1~45 |
| `size` | 선택 | 페이지당 결과 수, 1~15 |
| `sort` | 선택 | `accuracy` 또는 `distance` |
| `categoryGroupCode` | 선택 | 카카오 카테고리 그룹 코드 |

`sort=distance` 또는 `radius`를 사용할 때는 `x`, `y`를 함께 전달해야 합니다.

응답:

```json
{
  "data": {
    "items": [
      {
        "kakaoPlaceId": "26338954",
        "name": "강남역",
        "address": "서울 강남구 역삼동 858",
        "roadAddress": "서울 강남구 강남대로 396",
        "category": "교통,수송 > 지하철,전철 > 수도권2호선",
        "placeUrl": "https://place.map.kakao.com/26338954",
        "phone": "02-6110-2221",
        "latitude": 37.4979,
        "longitude": 127.0276,
        "distance": 120
      }
    ],
    "page": 1,
    "size": 15,
    "totalCount": 100,
    "pageableCount": 45,
    "isEnd": false
  },
  "message": "success"
}
```

## Place Candidate API

로그인 사용자와 게스트 모두 호출할 수 있습니다.

### 장소 후보 등록

```http
POST /api/appointments/{appointmentId}/place-candidates
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

요청:

```json
{
  "kakaoPlaceId": "12345",
  "name": "강남역",
  "address": "서울 강남구 강남대로 396",
  "roadAddress": "서울 강남구 강남대로 396",
  "category": "지하철역",
  "placeUrl": "https://place.map.kakao.com/12345",
  "phone": "02-123-4567",
  "latitude": 37.4979,
  "longitude": 127.0276
}
```

`PLANNING` 상태에서만 가능합니다.

### 장소 후보 목록 조회

```http
GET /api/appointments/{appointmentId}/place-candidates
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

### 장소 후보 삭제

```http
DELETE /api/appointments/{appointmentId}/place-candidates/{placeCandidateId}
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

후보 등록자 또는 방장만 삭제할 수 있으며 `PLANNING` 상태에서만 가능합니다.

## Vote API

### 장소 후보 투표

```http
PUT /api/appointments/{appointmentId}/votes
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

요청:

```json
{
  "placeCandidateId": 1
}
```

`PLANNING` 상태에서만 가능합니다.
이미 투표한 참여자가 다른 후보로 다시 요청하면 기존 투표가 변경됩니다.

### 투표 결과 조회

```http
GET /api/appointments/{appointmentId}/votes/results
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

응답:

```json
{
  "data": [
    {
      "placeCandidateId": 1,
      "voteCount": 3,
      "votedByMe": true
    },
    {
      "placeCandidateId": 2,
      "voteCount": 0,
      "votedByMe": false
    }
  ],
  "message": "success"
}
```

## Confirmed Place API

### 확정 장소 선택

```http
PUT /api/appointments/{appointmentId}/confirmed-place
Authorization: Bearer {accessToken}
```

요청:

```json
{
  "placeCandidateId": 1
}
```

방장만 가능하며 `PLANNING` 상태에서만 가능합니다.
성공 시 약속방 상태가 `CONFIRMED`로 변경됩니다.
확정 이후 장소 후보 등록/삭제와 투표 생성/변경/취소는 불가능합니다.

응답:

```json
{
  "data": {
    "id": 1,
    "appointmentId": 10,
    "placeCandidateId": 1000,
    "confirmedByUserId": 1,
    "confirmedAt": "2026-06-21T00:00:00Z",
    "kakaoPlaceId": "26338954",
    "name": "강남역",
    "address": "서울 강남구 역삼동 858",
    "roadAddress": "서울 강남구 강남대로 396",
    "category": "교통,수송 > 지하철,전철 > 수도권2호선",
    "placeUrl": "https://place.map.kakao.com/26338954",
    "phone": "02-6110-2221",
    "latitude": 37.4979,
    "longitude": 127.0276
  },
  "message": "success"
}
```

### 확정 장소 조회

```http
GET /api/appointments/{appointmentId}/confirmed-place
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

응답은 확정 장소 선택 API와 동일하며, 확정 메타데이터와 선택된 장소 후보 상세를 함께 반환합니다.
확정 장소가 없으면 `CONFIRMED_PLACE_NOT_FOUND`로 실패합니다.

## Location API

위치 공유와 조회는 `CONFIRMED` 상태에서만 가능합니다.
참여자 최신 위치는 Redis에 우선 저장하고, Redis에 데이터가 없거나 조회할 수 없는 경우 DB의 마지막 위치 정보를 fallback으로 조회합니다.
요청/응답 형식은 저장소 변경과 무관하게 동일합니다.

### 내 위치 공유/갱신

```http
PUT /api/appointments/{appointmentId}/locations/me
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

요청:

```json
{
  "latitude": 37.4979,
  "longitude": 127.0276,
  "accuracy": 20.5
}
```

### 참여자 위치 목록 조회

```http
GET /api/appointments/{appointmentId}/locations
Authorization: Bearer {accessToken}
```

또는:

```http
Cookie: guestSession={guestSession}
```

응답:

```json
{
  "data": [
    {
      "appointmentId": 10,
      "memberId": 100,
      "latitude": 37.4979,
      "longitude": 127.0276,
      "accuracy": 20.5,
      "updatedAt": "2026-06-21T12:00:00Z"
    }
  ],
  "message": "success"
}
```

## WebSocket/STOMP Location API

실시간 위치 공유는 WebSocket/STOMP를 사용합니다.
HTTP 위치 API는 WebSocket 연결 실패 또는 재조회가 필요한 경우 fallback으로 사용할 수 있습니다.
위치 공유는 약속방이 `CONFIRMED` 상태일 때만 가능합니다.

### 연결 endpoint

로컬:

```text
ws://localhost:8080/ws
```

배포:

```text
wss://api.eodigakka.xyz/ws
```

### STOMP CONNECT 인증

로그인 사용자는 STOMP CONNECT native header에 Access Token을 전달합니다.

```text
Authorization: Bearer {accessToken}
```

게스트 사용자는 `guestSession` HttpOnly Cookie로 인증합니다.
프론트는 쿠키 값을 직접 읽어서 STOMP header에 넣지 않습니다.
브라우저가 WebSocket handshake에 쿠키를 포함할 수 있도록 게스트 입장 요청부터 같은 API origin 기준으로 `credentials: "include"` 또는 `withCredentials: true` 설정을 유지합니다.

```javascript
// fetch 예시
await fetch(`${API_BASE_URL}/api/appointments/guests`, {
  method: "POST",
  credentials: "include",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ inviteCode: "A7K2P9QX", guestName: "철수" })
});
```

### 위치 publish

```text
PUB /app/appointments/{appointmentId}/locations
```

payload:

```json
{
  "latitude": 37.4979,
  "longitude": 127.0276,
  "accuracy": 20.5
}
```

### 위치 subscribe

```text
SUB /topic/appointments/{appointmentId}/locations
```

message:

```json
{
  "appointmentId": 10,
  "memberId": 100,
  "latitude": 37.4979,
  "longitude": 127.0276,
  "accuracy": 20.5,
  "updatedAt": "2026-06-21T12:00:00Z"
}
```

### WebSocket 연동 주의사항

- `/ws` handshake는 열려 있지만, STOMP `CONNECT`, `SEND`, `SUBSCRIBE` 단계에서 인증과 약속방 참여자 검증을 수행합니다.
- 로그인 사용자는 `Authorization` header, 게스트 사용자는 `guestSession` Cookie 기준입니다.
- 게스트 인증에 `X-Guest-Token`, `X-Guest-Session` header를 사용하지 않습니다.
- 약속방 참여자가 아닌 사용자는 해당 약속방 위치 topic 구독 또는 publish가 거부됩니다.
- 현재는 단일 서버 기준 simple broker를 사용합니다. 서버 다중화 시 Redis Pub/Sub 또는 외부 broker 검토가 필요합니다.

## 상태별 가능 작업

| 작업 | PLANNING | CONFIRMED | CLOSED |
| --- | --- | --- | --- |
| 장소 후보 등록 | 가능 | 불가 | 불가 |
| 장소 후보 삭제 | 가능 | 불가 | 불가 |
| 투표 | 가능 | 불가 | 불가 |
| 투표 결과 조회 | 가능 | 가능 | 가능 |
| 확정 장소 선택 | 가능 | 불가 | 불가 |
| 확정 장소 조회 | 확정 장소 없음 | 가능 | 가능 |
| 참여자 목록 조회 | 가능 | 가능 | 가능 |
| 위치 공유/갱신 | 불가 | 가능 | 불가 |
| 위치 목록 조회 | 불가 | 가능 | 불가 |
| 약속방 종료 | 불가 | 가능 | 불가 |

## 프론트 주의사항

- 로그인 사용자는 `Authorization: Bearer {accessToken}` 사용
- 게스트 사용자는 `guestSession` HttpOnly Cookie 사용
- 게스트 인증에 `X-Guest-Token`, `X-Guest-Session` 헤더를 사용하지 않음
- 초대 코드는 방 입장/미리보기용이고 게스트 본인 식별용이 아님
- 게스트 본인 식별은 서버가 발급한 `guestSession`으로 처리
- 게스트 세션은 같은 브라우저 재접속을 위해 쿠키로 유지
- 게스트 요청은 `credentials: "include"` 또는 `withCredentials: true` 설정 필요
- 게스트 토큰을 `localStorage`, `sessionStorage`, JS 변수에 저장하지 않음
- HTTPS 환경에서 프론트/백엔드 도메인이 분리되면 Cookie는 `Secure=true`, `SameSite=None` 필요
- `SameSite=None` Cookie는 브라우저 정책상 HTTPS와 함께 사용해야 함
- Swagger에는 `bearerAuth`와 `guestSessionCookie` 인증 스키마가 함께 표시됨
- 실제 `.env` 파일은 커밋 금지
- API 서버 주소는 환경변수로 관리 권장
- 브라우저 위치 권한 요청은 프론트에서 처리 필요
- 위치 정보는 민감 정보이므로 화면 노출 범위 주의
