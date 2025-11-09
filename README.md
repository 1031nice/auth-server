# Auth Server

Spring Boot 기반 JWT 인증 서버.

## 기술 스택
- Java 21
- Spring Boot 3.2
- Spring Security
- JPA/Hibernate
- H2 (개발용 인메모리 DB)

## 현재 제공 기능
- 로그인: `username`/`password`로 인증 후 JWT 발급
- JWT 토큰 발급·검증: HS512 서명, 만료 시간 설정
- 리프레시 토큰 발급 및 갱신
- 역할 기반 접근 제어: 사용자 권한(RBAC) 적용
- 비밀번호 암호화: BCrypt 이용
- 전역 예외 처리: 유효성 검증 및 런타임 예외 응답 관리
- 헬스 체크: `/api/v1/auth/health`

## 실행 방법
```bash
./gradlew build
./gradlew bootRun
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

### JWT 만료 설정
`application.yml`에서 액세스/리프레시 토큰 만료 시간을 조정할 수 있습니다.
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000        # access token (24h)
  refresh-expiration: 604800000 # refresh token (7d)
```
