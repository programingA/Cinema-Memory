# Cinema Memory

Cinema Memory는 개인의 추억을 필름과 장면 단위로 저장하고, 영화관 상영 화면처럼 다시 감상할 수 있는 웹 애플리케이션입니다.

## 프로젝트 구조

```text
.
├── apps
│   ├── api   # Spring Boot 백엔드
│   └── web   # Next.js 프론트엔드
├── infra     # 로컬 개발용 Docker Compose
└── package.json
```

## 기술 스택

- Frontend: Next.js App Router, React, TypeScript, Tailwind CSS, Three.js, Framer Motion
- Backend: Spring Boot, Spring Security, OAuth2 Client, JPA, Flyway, springdoc OpenAPI
- Database: MySQL
- Session: Redis refresh token store
- Media: 로컬 개발 업로드 디렉터리, S3 presigned upload 구조

## 주요 기능

- 이메일 회원가입/로그인
- Google/Kakao OAuth 로그인 설정 기반
- 필름 생성, 수정, 삭제
- 필름별 장면 생성, 수정, 삭제
- 이미지/동영상 미디어 업로드
- 영사기 기반 필름 재생 화면
- 관리자 페이지
  - 일반 사용자/관리자 수 확인
  - 필름/장면/미디어 수 확인
  - 사용자 검색 및 역할 필터
  - 사용자 `USER`/`ADMIN` 역할 변경

## 저장 위치

- 사용자 계정: MySQL `users`
- 관리자 권한: MySQL `users.role`
- 필름: MySQL `films`
- 장면: MySQL `memory_scenes`
- 미디어 메타데이터: MySQL `media_assets`
- refresh token: Redis
- 로컬 개발 미디어 파일: `apps/api/uploads/media`

## 요구 사항

- JDK 21
- Maven
- Node.js
- npm
- Docker Desktop

## 로컬 실행

### 1. MySQL/Redis 실행

```powershell
docker compose -f infra/docker-compose.yml up -d
```

기본 포트:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`

MySQL `3306` 포트가 이미 사용 중이면 `MySQL80` 같은 로컬 MySQL 서비스가 켜져 있을 수 있습니다.

```powershell
Get-Service *mysql*
```

관리자 권한 PowerShell에서 중지할 수 있습니다.

```powershell
Stop-Service MySQL80
```

### 2. 백엔드 실행

Spring Boot는 OS 환경변수와 `application.yml` 설정을 읽습니다. PowerShell에서 같은 터미널에 필요한 값을 설정한 뒤 실행합니다.

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/cinema_memory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "cinema"
$env:DB_PASSWORD = "cinema"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
$env:FRONTEND_URL = "http://localhost:3000"

$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "change-this-password"
$env:ADMIN_DISPLAY_NAME = "Administrator"

cd apps/api
mvn spring-boot:run
```

백엔드 URL:

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 3. 프론트엔드 실행

```powershell
npm.cmd install
npm.cmd run dev:web
```

프론트엔드 URL:

- `http://localhost:3000`

프론트 API 주소는 `apps/web/.env.example` 기준으로 설정합니다.

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Next.js 로컬 환경 파일을 쓰려면 `apps/web/.env.local`에 위 값을 넣습니다.

## 관리자 계정

백엔드 시작 시 아래 값이 설정되어 있으면 관리자 계정을 생성합니다.

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "change-this-password"
$env:ADMIN_DISPLAY_NAME = "Administrator"
```

동작 방식:

- `ADMIN_EMAIL` 계정이 없으면 새 `ADMIN` 계정을 생성합니다.
- 같은 이메일의 계정이 이미 있으면 `ADMIN` 역할을 보장합니다.
- 기존 계정에 비밀번호가 이미 있으면 자동으로 덮어쓰지 않습니다.

관리자 계정으로 일반 로그인 후 `/admin`에 접근할 수 있습니다.

## 데이터베이스 초기화

Docker MySQL 데이터는 named volume `mysql-data`에 저장됩니다. 컨테이너를 다시 만들어도 데이터가 남습니다.

데이터까지 완전히 삭제하려면:

```powershell
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
```

주의: `down -v`는 MySQL 데이터도 삭제합니다.

## 검증 명령

백엔드:

```powershell
cd apps/api
mvn test
```

프론트엔드:

```powershell
npm.cmd run lint:web
npm.cmd run build:web
```

## 로컬 미디어 업로드

로컬 개발에서는 프론트가 `/media/upload`로 파일을 업로드합니다. 백엔드는 파일을 아래 경로에 저장하고, MySQL `media_assets`에는 URL과 메타데이터를 저장합니다.

```text
apps/api/uploads/media
```

이 디렉터리는 Git에 포함하지 않습니다.

S3 presigned upload용 API도 남아 있으므로, 운영 환경에서는 AWS/S3 설정을 연결해 확장할 수 있습니다.

## 자주 발생하는 문제

### Docker MySQL이 시작되지 않고 3306 포트 오류가 나는 경우

다른 MySQL이 이미 `3306`을 사용 중입니다.

```powershell
netstat -ano | findstr :3306
Get-Service *mysql*
```

로컬 MySQL 서비스를 끄거나 `docker-compose.yml`의 MySQL 호스트 포트를 `3307:3306`처럼 바꾸고 `DB_URL`도 같은 포트로 맞춰야 합니다.

### 관리자 계정이 반영되지 않는 경우

백엔드를 재시작해야 합니다. `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `ADMIN_DISPLAY_NAME`은 백엔드 프로세스 시작 시 읽습니다.

### MySQL에서 데이터가 보이지 않는 경우

백엔드가 연결한 DB와 확인 중인 DB가 같은지 확인합니다.

기본 연결 정보:

- Host: `localhost`
- Port: `3306`
- Database: `cinema_memory`
- User: `cinema`

확인 쿼리:

```sql
USE cinema_memory;
SHOW TABLES;
SELECT id, email, display_name, role, created_at FROM users;
SELECT id, user_id, title, created_at FROM films;
```
