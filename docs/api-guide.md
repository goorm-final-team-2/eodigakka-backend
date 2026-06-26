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

게스트 사용자는 게스트 입장 API에서 받은 `guestToken`을 `X-Guest-Token` 헤더로 전달합니다.

```http
X-Guest-Token: {guestToken}
```

게스트 토큰은 초대 코드가 아니라 게스트 본인 식별용 토큰입니다.
프론트는 게스트 입장 성공 후 받은 `guestToken`을 저장해야 하며, 이후 장소 후보, 투표, 확정 장소 조회, 위치 공유 API 호출 시 함께 전달해야 합니다.

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
X-Guest-Token: {guestToken}
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

응답의 `guest.guestToken`은 이후 게스트 인증에 사용합니다.

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
      "guestName": "철수",
      "guestToken": "raw-guest-token"
    }
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
X-Guest-Token: {guestToken}
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
X-Guest-Token: {guestToken}
```

### 장소 후보 삭제

```http
DELETE /api/appointments/{appointmentId}/place-candidates/{placeCandidateId}
Authorization: Bearer {accessToken}
```

또는:

```http
X-Guest-Token: {guestToken}
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
X-Guest-Token: {guestToken}
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
X-Guest-Token: {guestToken}
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

### 확정 장소 조회

```http
GET /api/appointments/{appointmentId}/confirmed-place
Authorization: Bearer {accessToken}
```

또는:

```http
X-Guest-Token: {guestToken}
```

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
X-Guest-Token: {guestToken}
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
X-Guest-Token: {guestToken}
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
- 게스트 사용자는 `X-Guest-Token: {guestToken}` 사용
- 초대 코드는 방 입장/미리보기용이고 게스트 본인 식별용이 아님
- 게스트 본인 식별은 게스트 입장 응답의 `guestToken`으로 처리
- 게스트 토큰은 같은 브라우저 재접속을 위해 프론트에서 저장 필요
- 실제 `.env` 파일은 커밋 금지
- API 서버 주소는 환경변수로 관리 권장
- 브라우저 위치 권한 요청은 프론트에서 처리 필요
- 위치 정보는 민감 정보이므로 화면 노출 범위 주의
