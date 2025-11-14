# Auth Server

Spring Boot 기반 JWT 인증 서버.

## 기술 스택
- Java 21
- Spring Boot 3.2
- Spring Security
- Spring Security OAuth2 Authorization Server
- JPA/Hibernate
- H2 (개발용 인메모리 DB)
- Redis (Rate Limiting용)
- Bucket4j (Rate Limiting 라이브러리)

## 현재 제공 기능
- 로그인: `username`/`password`로 인증 후 JWT 발급
- JWT 토큰 발급·검증: HS512 서명, 만료 시간 설정
- 리프레시 토큰 발급 및 갱신
- 역할 기반 접근 제어: 사용자 권한(RBAC) 적용
- 비밀번호 암호화: BCrypt 이용
- 전역 예외 처리: 유효성 검증 및 런타임 예외 응답 관리
- 헬스 체크: `/api/v1/auth/health`
- **OAuth2 Authorization Server**: 표준 OAuth2 엔드포인트 제공
- **OAuth2 Client 관리**: OAuth2 클라이언트 등록 및 관리 API

## 실행 방법

### 사전 요구사항
- Java 21
- Docker & Docker Compose (Redis 실행용)

### 1. Redis 실행
```bash
docker-compose up -d redis
```

### 2. 애플리케이션 실행
```bash
./gradlew build
./gradlew bootRun
```

### Redis 중지
```bash
docker-compose down
```

### 주요 엔드포인트
#### 로그인
`POST /api/v1/auth/login`
```json
{
  "username": "demo",
  "password": "password"
}
```

응답:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "demo",
  "email": "demo@example.com",
  "roles": ["ROLE_USER"]
}
```

#### 헬스 체크
`GET /api/v1/auth/health`

#### 토큰 갱신
`POST /api/v1/auth/refresh`
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

응답:
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "demo",
  "email": "demo@example.com",
  "roles": ["ROLE_USER"]
}
```

### OAuth2 엔드포인트

#### OAuth2 Client 등록
OAuth2를 사용하기 전에 클라이언트를 등록해야 합니다.

`POST /api/v1/oauth2/clients`
```json
{
  "clientId": "my-client",
  "clientSecret": "my-secret",
  "redirectUris": ["http://localhost:3000/callback"],
  "scopes": ["read", "write"],
  "grantTypes": ["authorization_code", "refresh_token", "client_credentials"]
}
```

#### OAuth2 Client 관리
- `GET /api/v1/oauth2/clients/{clientId}` - 클라이언트 조회
- `GET /api/v1/oauth2/clients` - 전체 클라이언트 목록
- `DELETE /api/v1/oauth2/clients/{clientId}` - 클라이언트 삭제

#### OAuth2 Authorization Code Flow
1. **인증 요청**: 사용자를 인증 서버로 리다이렉트
```
GET /oauth2/authorize?client_id=my-client&response_type=code&redirect_uri=http://localhost:3000/callback&scope=read write
```

2. **토큰 발급**: 인증 코드로 액세스 토큰 교환
```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code={authorization_code}&client_id=my-client&client_secret=my-secret&redirect_uri=http://localhost:3000/callback
```

응답:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",
  "scope": "read write"
}
```

#### OAuth2 Client Credentials Flow
서버 간 통신에 사용하는 플로우입니다.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=client_credentials&scope=read write
```

응답:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "scope": "read write"
}
```

#### OAuth2 Token Refresh
리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=refresh_token&refresh_token={refresh_token}
```

#### OAuth2 Grant Types 지원
- `authorization_code`: 인증 코드 플로우 (웹 애플리케이션용)
- `client_credentials`: 클라이언트 자격 증명 플로우 (서버 간 통신용)
- `refresh_token`: 리프레시 토큰 플로우 (토큰 갱신용)

### JWT 만료 설정
`application.yml`에서 액세스/리프레시 토큰 만료 시간을 조정할 수 있습니다.
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000        # access token (24h)
  refresh-expiration: 604800000 # refresh token (7d)
```
