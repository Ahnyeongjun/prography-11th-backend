# Prography 11th Backend - 출결관리 시스템

프로그라피 11기 정기 모임 출결 관리 Backend API 서버입니다.

## 기술 스택

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 4.0.3
- **Database**: H2 (In-Memory)
- **ORM**: Spring Data JPA (Hibernate 7)
- **Password**: BCrypt (Spring Security Crypto)
- **Test**: JUnit 6 + Mockito

## 실행 방법

### 사전 요구사항
- JDK 17 이상

### 서버 실행
```bash
./gradlew bootRun
```

서버가 시작되면 `http://localhost:8080`에서 접근 가능합니다.

### 시드 데이터
서버 시작 시 자동으로 아래 데이터가 로드됩니다:

| 데이터 | 내용 |
|--------|------|
| 기수 | 10기, 11기 |
| 파트 | 기수별 SERVER, WEB, iOS, ANDROID, DESIGN (총 10개) |
| 팀 | 11기 Team A, Team B, Team C (총 3개) |
| 관리자 | loginId: `admin`, password: `admin1234`, role: ADMIN |
| 보증금 | 관리자 초기 보증금 100,000원 |

### 로그인 테스트
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"loginId": "admin", "password": "admin1234"}'
```

### H2 콘솔
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:prography`
- Username: `sa`
- Password: (빈 값)

### 테스트 실행
```bash
./gradlew test
```

## API 목록

### 필수 API (16개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 1 | POST | /api/v1/auth/login | 로그인 |
| 2 | GET | /api/v1/members/{id} | 회원 조회 |
| 3 | POST | /api/v1/admin/members | 회원 등록 |
| 4 | GET | /api/v1/admin/members | 회원 대시보드 |
| 5 | GET | /api/v1/admin/members/{id} | 회원 상세 |
| 6 | PUT | /api/v1/admin/members/{id} | 회원 수정 |
| 7 | DELETE | /api/v1/admin/members/{id} | 회원 탈퇴 |
| 8 | GET | /api/v1/admin/cohorts | 기수 목록 |
| 9 | GET | /api/v1/admin/cohorts/{id} | 기수 상세 |
| 10 | GET | /api/v1/sessions | 일정 목록 (회원) |
| 11 | GET | /api/v1/admin/sessions | 일정 목록 (관리자) |
| 12 | POST | /api/v1/admin/sessions | 일정 생성 |
| 13 | PUT | /api/v1/admin/sessions/{id} | 일정 수정 |
| 14 | DELETE | /api/v1/admin/sessions/{id} | 일정 삭제 |
| 15 | POST | /api/v1/admin/sessions/{id}/qrcodes | QR 생성 |
| 16 | PUT | /api/v1/admin/qrcodes/{id} | QR 갱신 |

### 가산점 API (9개)

| # | Method | Path | 설명 |
|---|--------|------|------|
| 17 | POST | /api/v1/attendances | QR 출석 체크 |
| 18 | GET | /api/v1/attendances | 내 출결 기록 |
| 19 | GET | /api/v1/members/{id}/attendance-summary | 내 출결 요약 |
| 20 | POST | /api/v1/admin/attendances | 출결 등록 |
| 21 | PUT | /api/v1/admin/attendances/{id} | 출결 수정 |
| 22 | GET | /api/v1/admin/attendances/sessions/{id}/summary | 일정별 출결 요약 |
| 23 | GET | /api/v1/admin/attendances/members/{id} | 회원 출결 상세 |
| 24 | GET | /api/v1/admin/attendances/sessions/{id} | 일정별 출결 목록 |
| 25 | GET | /api/v1/admin/cohort-members/{id}/deposits | 보증금 이력 |

## 프로젝트 구조

```
src/main/kotlin/com/prography11thbackend/
├── common/              # ApiResponse, ErrorCode, BusinessException, GlobalExceptionHandler
├── config/              # AppConfig (BCrypt), DataInitializer (시드 데이터)
├── controller/          # REST 컨트롤러 (5개)
├── dto/
│   ├── request/         # 요청 DTO (8개)
│   └── response/        # 응답 DTO (19개)
├── entity/              # JPA 엔티티 (9개) + Enum (5개)
├── repository/          # Spring Data JPA 리포지토리 (9개)
└── service/             # 비즈니스 로직 서비스 (5개)

docs/
├── ERD.md                  # ERD (Mermaid)
├── SYSTEM_ARCHITECTURE.md  # 시스템 설계 아키텍처
├── DECISIONS.md            # 설계 결정 및 고민 기록
└── AI_USAGE.md             # AI 사용 사례
```

## 문서

| 문서 | 설명 |
|------|------|
| [ERD](docs/ERD.md) | 엔티티 관계 다이어그램 (Mermaid) |
| [시스템 아키텍처](docs/SYSTEM_ARCHITECTURE.md) | 현재 구현 + 이상적 프로덕션 아키텍처 |
| [설계 결정](docs/DECISIONS.md) | 주요 설계 결정 및 고민 기록 |
| [AI 사용사례](docs/AI_USAGE.md) | AI 도구 활용 내역 |

## 테스트 현황

총 **91개** 테스트 (모두 통과) — 단위 테스트 56개 + 통합 테스트 34개 + 컨텍스트 1개

### 단위 테스트 (Mockito 기반)

| 테스트 클래스 | 테스트 수 | 대상 |
|--------------|-----------|------|
| AuthServiceTest | 4 | 로그인 성공/실패, 탈퇴 회원 |
| MemberServiceTest | 12 | CRUD, 대시보드, 상세조회, 수정, 탈퇴 |
| CohortServiceTest | 4 | 기수 목록/상세, 미존재 기수 |
| SessionServiceTest | 11 | CRUD, QR 생성/갱신, 필터링 |
| AttendanceServiceTest | 25 | QR 체크인, 출결 등록/수정, 패널티, 보증금, 요약/상세 |

### 통합 테스트 (MockMvc + H2)

| 테스트 클래스 | 테스트 수 | 대상 |
|--------------|-----------|------|
| AuthIntegrationTest | 3 | 로그인 API 성공/실패 |
| MemberIntegrationTest | 8 | 회원 CRUD, 대시보드, 상세 API |
| CohortIntegrationTest | 3 | 기수 목록/상세 API |
| SessionIntegrationTest | 9 | 일정 CRUD, QR 생성/갱신 API |
| AttendanceIntegrationTest | 11 | 출결 등록/수정, QR 체크인, 보증금 이력 API |
| ApplicationTests | 1 | 컨텍스트 로딩 |
