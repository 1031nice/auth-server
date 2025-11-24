# Auth Platform

Centralized authentication and authorization platform. OAuth2/OIDC-compliant authentication server for multiple side projects.

## Overview

A centralized authentication system that can be shared across multiple applications. Use this platform to handle user authentication for various side projects like Slack clones, blogs, e-commerce sites, etc.

### Key Features

- **Centralized Authentication**: Manage user authentication for multiple applications with a single auth server
- **OAuth2/OIDC Standard**: Compatible with various clients following standard protocols
- **User Signup Support**: Automatic redirect for signup requests from external applications
- **Enhanced Security**: PKCE, Refresh Token Rotation, Rate Limiting, and more

## Project Structure

This project consists of two submodules:

- **oauth2-server**: OAuth2 Authorization Server (Port: 8081) - User authentication and token issuance
- **resource-server**: OAuth2 Resource Server (Port: 8082) - Protected resource provider

## Tech Stack

- Java 21
- Spring Boot 3.2
- Spring Security
- Spring Security OAuth2 Authorization Server
- JPA/Hibernate
- H2 (In-memory DB for development)
- Redis (for Rate Limiting)
- Bucket4j (Rate Limiting library)

## Features

### oauth2-server (OAuth2 Authorization Server)

- **User Signup**: `/signup` - Email and password signup with redirect_uri support
- **User Login**: `/login` - Form-based login
- **OAuth2 Authorization Server**: Standard OAuth2/OIDC endpoints
- **OAuth2 Client Management**: Client registration and management API
- **Supported Grant Types**:
  - `authorization_code`: Authorization code flow (for web applications)
  - `client_credentials`: Client credentials flow (for server-to-server communication)
  - `refresh_token`: Refresh token flow (for token renewal)
- **JWK Set Endpoint**: `/oauth2/jwks` - Used by Resource Server for token validation
- **Role-Based Access Control**: RBAC implementation
- **Password Encryption**: BCrypt
- **Refresh Token Rotation (RTR)**: Automatic rotation and reuse detection
- **PKCE (Proof Key for Code Exchange)**: Enhanced security for public clients
- **Audit Logging**: Authentication and token issuance event logging
- **Rate Limiting**: Redis-based request limiting

### resource-server (OAuth2 Resource Server)

- **OIDC UserInfo Endpoint**: `/userinfo` - User information via OAuth2 token
- **JWT Token Validation**: Validates tokens issued by OAuth2 Authorization Server
- **Protected Resources**: Resources requiring OAuth2 token-based authentication

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose (for Redis)

### 1. Start Redis

```bash
docker-compose up -d redis
```

### 2. Run Applications

#### oauth2-server

```bash
./gradlew :oauth2-server:bootRun
```

#### resource-server

```bash
./gradlew :resource-server:bootRun
```

#### Build

```bash
./gradlew build
```

### Stop Redis

```bash
docker-compose down
```

## Integration Guide

### 1. Register OAuth2 Client

Register an OAuth2 client before using this auth platform in your application.

```bash
POST http://localhost:8081/api/v1/oauth2/clients
Content-Type: application/json
Authorization: Bearer {admin_token}

{
  "clientId": "my-slack-clone",
  "clientSecret": "my-secret-key",
  "redirectUris": [
    "http://localhost:3000/auth/callback",
    "http://localhost:3000/signup/callback"
  ],
  "scopes": ["read", "write"],
  "grantTypes": ["authorization_code", "refresh_token"]
}
```

**Important**: Include the callback URL for signup completion in `redirectUris`.

### 2. Signup Integration

Redirect users to the auth platform when they click the signup button:

```javascript
// Example: In Slack clone app
function redirectToSignup() {
  const redirectUri = encodeURIComponent('http://localhost:3000/auth/callback');
  const clientId = 'my-slack-clone';
  window.location.href = `http://localhost:8081/signup?redirect_uri=${redirectUri}&client_id=${clientId}`;
}
```

After signup, users are automatically redirected to `redirect_uri?success=true`.

### 3. Login Integration (OAuth2 Authorization Code Flow)

```javascript
// 1. Redirect user to auth server
function redirectToLogin() {
  const params = new URLSearchParams({
    client_id: 'my-slack-clone',
    response_type: 'code',
    redirect_uri: 'http://localhost:3000/auth/callback',
    scope: 'read write',
    state: generateRandomState() // For CSRF protection
  });
  window.location.href = `http://localhost:8081/oauth2/authorize?${params}`;
}

// 2. Exchange authorization code for tokens in callback
async function handleCallback(code) {
  const response = await fetch('http://localhost:8081/oauth2/token', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Basic ' + btoa('my-slack-clone:my-secret-key')
    },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      code: code,
      redirect_uri: 'http://localhost:3000/auth/callback'
    })
  });
  
  const tokens = await response.json();
  // Store access_token and refresh_token
}
```

### 4. Get User Info

```javascript
// Get user information from Resource Server
async function getUserInfo(accessToken) {
  const response = await fetch('http://localhost:8082/userinfo', {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  });
  
  const userInfo = await response.json();
  // { sub, name, email, email_verified, preferred_username }
}
```

## API Endpoints

### oauth2-server Endpoints

#### User Signup

Used when external applications request user signup.

**Signup Page**
```
GET /signup?redirect_uri={callback_url}&client_id={client_id}
```

**Signup Processing**
```
POST /signup
Content-Type: application/x-www-form-urlencoded

email=user@example.com&password=password123&redirectUri={callback_url}&clientId={client_id}
```

**Response**: Redirects to `redirect_uri?success=true` on success

#### User Login

Login page used with OAuth2 Authorization Code Flow.

`GET /login` - Login page
`POST /login` - Login processing

#### OAuth2 Client Registration

Register a client before using OAuth2.

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

#### OAuth2 Client Management

- `GET /api/v1/oauth2/clients/{clientId}` - Get client
- `GET /api/v1/oauth2/clients` - List all clients
- `DELETE /api/v1/oauth2/clients/{clientId}` - Delete client

#### OAuth2 Authorization Code Flow

1. **Authorization Request**: Redirect user to auth server
```
GET /oauth2/authorize?client_id=my-client&response_type=code&redirect_uri=http://localhost:3000/callback&scope=read write
```

2. **Token Exchange**: Exchange authorization code for access token
```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&code={authorization_code}&client_id=my-client&client_secret=my-secret&redirect_uri=http://localhost:3000/callback
```

Response:
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

For server-to-server communication.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=client_credentials&scope=read write
```

Response:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "scope": "read write"
}
```

#### OAuth2 Token Refresh

Get a new access token using refresh token.

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: Basic {base64(client_id:client_secret)}

grant_type=refresh_token&refresh_token={refresh_token}
```

#### Supported OAuth2 Grant Types

- `authorization_code`: Authorization code flow (for web applications)
- `client_credentials`: Client credentials flow (for server-to-server communication)
- `refresh_token`: Refresh token flow (for token renewal)

### resource-server Endpoints

#### OIDC UserInfo

Get user information using access token issued by OAuth2 Authorization Server.

```
GET /userinfo
Authorization: Bearer {access_token}
```

Response:
```json
{
  "sub": "1",
  "name": "user@example.com",
  "email": "user@example.com",
  "email_verified": true,
  "preferred_username": "user@example.com"
}
```

**Note**: `name` and `preferred_username` are the user's email address. This platform uses email as the user identifier.

## Usage Example

### Scenario: Using Auth Platform in Slack Clone App

1. **Initial Setup**
   ```bash
   # Register OAuth2 Client
   curl -X POST http://localhost:8081/api/v1/oauth2/clients \
     -H "Content-Type: application/json" \
     -d '{
       "clientId": "slack-clone",
       "clientSecret": "secret123",
       "redirectUris": ["http://localhost:3000/callback"],
       "scopes": ["read", "write"],
       "grantTypes": ["authorization_code", "refresh_token"]
     }'
   ```

2. **Signup Flow**
   - User clicks "Sign Up" in Slack clone app
   - Redirect to `http://localhost:8081/signup?redirect_uri=http://localhost:3000/callback&client_id=slack-clone`
   - User enters email/password on auth platform
   - After signup, redirect to `http://localhost:3000/callback?success=true`

3. **Login Flow**
   - User clicks "Login" in Slack clone app
   - Start OAuth2 Authorization Code Flow
   - User logs in on auth platform
   - Exchange authorization code for access token
   - Get user information using token

## Security Considerations

- **redirect_uri Validation**: Only registered redirect_uris are allowed for signup and authentication
- **PKCE**: Recommended for public clients (mobile apps, etc.)
- **HTTPS**: Must use HTTPS in production
- **Client Secret Management**: Keep Client Secret secure and never expose it in client code
