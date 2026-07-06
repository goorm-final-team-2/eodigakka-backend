# 어디가까 Backend API Guide

프론트 연동을 위한 백엔드 API 계약 문서입니다.

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

로컬 HTTP 환경에서는 Refresh Token Cookie와 게스트 세션 Cookie 모두 `Secure=false`, `SameSite=Lax`를 사용합니다.
HTTPS 배포 환경에서 프론트와 백엔드 도메인이 분리되면 두 Cookie 모두 `Secure=true`, `SameSite=None` 조합을 검토해야 합니다.

## 약속방 상태

```text
PLANNING -> CONFIRMED -> CLOSED
```

- `PLANNING`: 장소 후보 등록/삭제, 투표, 확정 장소 선택 가능
- `CONFIRMED`: 위치 공유/조회 가능
- `CLOSED`: 게스트 참여, 위치 공유/조회 불가

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
