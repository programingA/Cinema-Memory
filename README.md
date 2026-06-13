# Cinema Memory

Cinema Memory는 개인 추억을 필름과 장면 단위로 기록하고, 이미지/동영상 미디어를 함께 저장해 영화 재생 화면처럼 다시 볼 수 있는 웹 애플리케이션입니다.

## 프로젝트 구조

```text
.
|-- apps
|   |-- api   # Spring Boot 백엔드 API
|   `-- web   # Next.js 프론트엔드
|-- docs      # 운영/구현 문서
|-- infra     # 로컬 개발용 Docker Compose
|-- package.json
`-- package-lock.json
```

## 기술 스택

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, Three.js, Framer Motion
- Backend: Java 21, Spring Boot, Spring Security, JWT, OAuth2 Client, JPA, Flyway
- Database: MySQL
- Session/Token: Redis refresh token store
- Media: local filesystem storage 또는 S3/CloudFront

## 주요 기능

- 이메일 회원가입/로그인
- Google/Kakao OAuth 로그인
- JWT access token 및 Redis 기반 refresh token
- 필름 생성, 수정, 삭제
- 필름별 장면 생성, 수정, 삭제, 순서 변경
- 이미지/동영상 미디어 업로드
- 필름 재생 화면
- 관리자 대시보드
  - 사용자/관리자/정지 계정 수 확인
  - 필름/장면/미디어 수 확인
  - 사용자 목록 및 필름 목록 확인
  - 사용자 역할 변경: `USER`, `ADMIN`
  - 사용자 상태 변경: `ACTIVE`, `SUSPENDED`
  - 사용자 삭제
- 보안 설정
  - CORS 허용 origin 설정
  - 운영 환경 HTTPS 강제 옵션
  - 회원가입 rate limit
  - 보안 응답 헤더

## 요구 사항

- JDK 21
- Maven
- Node.js 및 npm
- Docker Desktop

## 로컬 실행

### 1. MySQL/Redis 실행

```powershell
docker compose -f infra/docker-compose.yml up -d
```

기본 포트:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`

MySQL 3306 포트가 이미 사용 중이면 로컬 MySQL 서비스가 실행 중인지 확인합니다.

```powershell
Get-Service *mysql*
netstat -ano | findstr :3306
```

필요하면 관리자 권한 PowerShell에서 로컬 MySQL 서비스를 중지합니다.

```powershell
Stop-Service MySQL80
```

### 2. 백엔드 실행

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/cinema_memory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "cinema"
$env:DB_PASSWORD = "cinema"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:FRONTEND_URL = "http://localhost:3000"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000"

$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "change-this-password"
$env:ADMIN_DISPLAY_NAME = "Administrator"

$env:JWT_SECRET = "change-this-secret-to-at-least-256-bits-before-production"
$env:MEDIA_STORAGE_MODE = "local"
$env:OPENAPI_ENABLED = "true"

cd apps/api
mvn spring-boot:run
```

백엔드 URL:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

`OPENAPI_ENABLED`의 기본값은 `false`입니다. Swagger UI가 필요하면 위 예시처럼 `true`로 설정해야 합니다.

### 3. 프론트엔드 실행

```powershell
cd ../..
npm.cmd install
npm.cmd run dev:web
```

프론트엔드 URL:

- `http://localhost:3000`

프론트엔드 API 주소는 `apps/web/.env.local`에 설정할 수 있습니다.

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

설정하지 않으면 개발 환경에서는 `http://localhost:8080`을 기본값으로 사용합니다.

## 주요 환경 변수

### 백엔드

```text
DB_URL=jdbc:mysql://localhost:3306/cinema_memory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=cinema
DB_PASSWORD=cinema
REDIS_HOST=localhost
REDIS_PORT=6379
FRONTEND_URL=http://localhost:3000
CORS_ALLOWED_ORIGINS=http://localhost:3000

ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=change-this-password
ADMIN_DISPLAY_NAME=Administrator

JWT_SECRET=change-this-secret-to-at-least-256-bits-before-production
JWT_ACCESS_TOKEN_MINUTES=15
JWT_REFRESH_TOKEN_DAYS=14

MEDIA_STORAGE_MODE=local
MEDIA_MAX_UPLOAD_BYTES=52428800

AWS_REGION=ap-northeast-2
MEDIA_S3_BUCKET=cinema-memory-dev
MEDIA_S3_PREFIX=uploads/media
CLOUDFRONT_BASE_URL=

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=

APP_REQUIRE_HTTPS=false
SIGNUP_RATE_LIMIT_PER_MINUTE=5
OPENAPI_ENABLED=false
```

### 프론트엔드

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
API_PROXY_TARGET=https://your-backend.example.com
ALLOW_INSECURE_API_PROXY=false
```

운영 빌드에서 `NEXT_PUBLIC_API_BASE_URL`을 설정하지 않으면 프론트엔드는 `/api/backend`를 사용합니다. 이 경로는 `apps/web/next.config.mjs`의 rewrite 설정을 통해 `API_PROXY_TARGET`으로 프록시됩니다.

## Vercel 배포 메모

Vercel에는 프론트엔드만 배포합니다. 백엔드 Spring Boot API, MySQL, Redis는 별도 서버 또는 클라우드 환경에서 실행되어야 합니다.

Vercel 프로젝트 설정:

- Framework Preset: `Next.js`
- Root Directory: `apps/web`
- API 프록시 대상: `API_PROXY_TARGET`

운영 환경에서는 `API_PROXY_TARGET`을 HTTPS URL로 설정해야 합니다. HTTP URL을 반드시 써야 하는 테스트 상황에서는 `ALLOW_INSECURE_API_PROXY=true`를 함께 설정해야 합니다.

## 데이터베이스

스키마는 Flyway 마이그레이션으로 관리합니다.

```text
apps/api/src/main/resources/db/migration
```

현재 DB 구조를 볼 때는 `V1`만 보면 안 되고, `V1`부터 최신 버전까지 순서대로 누적 적용된 결과를 기준으로 봐야 합니다.

현재 주요 테이블:

- `users`
- `oauth_accounts`
- `films`
- `memory_scenes`
- `media_assets`
- `tags`
- `film_tags`

## 로컬 미디어 저장소

`MEDIA_STORAGE_MODE=local`이면 업로드 파일은 아래 경로에 저장됩니다.

```text
apps/api/uploads/media
```

해당 디렉터리는 Git에 포함하지 않습니다.

`MEDIA_STORAGE_MODE=s3`이면 S3 업로드 흐름을 사용합니다. S3 사용 시 `AWS_REGION`, `MEDIA_S3_BUCKET`, `MEDIA_S3_PREFIX`, `CLOUDFRONT_BASE_URL`을 환경에 맞게 설정합니다.

## 검증 명령

백엔드 테스트:

```powershell
cd apps/api
mvn test
```

프론트엔드 lint:

```powershell
npm.cmd run lint:web
```

프론트엔드 빌드:

```powershell
npm.cmd run build:web
```

## 자주 발생하는 문제

### MySQL 3306 포트 충돌

다른 MySQL 서버가 `3306`을 이미 사용 중일 수 있습니다.

```powershell
netstat -ano | findstr :3306
Get-Service *mysql*
```

해결 방법은 둘 중 하나입니다.

- 로컬 MySQL 서비스를 중지한다.
- `infra/docker-compose.yml`의 MySQL host port를 예를 들어 `3307:3306`으로 바꾸고 `DB_URL`도 같은 포트로 맞춘다.

### Swagger UI가 열리지 않음

`OPENAPI_ENABLED` 기본값은 `false`입니다.

```powershell
$env:OPENAPI_ENABLED = "true"
```

백엔드를 다시 실행한 뒤 `http://localhost:8080/swagger-ui/index.html`에 접속합니다.

### 운영 프론트엔드에서 API 호출 실패

Vercel 환경 변수 `API_PROXY_TARGET`이 실제 백엔드 HTTPS URL을 가리키는지 확인합니다. 프론트엔드의 운영 기본 API base URL은 `/api/backend`이고, 이 요청은 Next.js rewrite를 통해 백엔드로 전달됩니다.
