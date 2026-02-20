[← README](../README.md) | [시스템 아키텍처](SYSTEM_ARCHITECTURE.md) | [설계 결정](DECISIONS.md) | [AI 사용사례](AI_USAGE.md)

# ERD (Entity-Relationship Diagram)

```mermaid
erDiagram
    MEMBERS {
        bigint id PK
        varchar login_id UK "로그인 ID (중복 불가)"
        varchar password "BCrypt 해싱"
        varchar name
        varchar phone
        enum status "ACTIVE | INACTIVE | WITHDRAWN"
        enum role "MEMBER | ADMIN"
        timestamp created_at
        timestamp updated_at
    }

    COHORTS {
        bigint id PK
        int generation "기수 번호"
        varchar name "기수 이름"
        timestamp created_at
    }

    PARTS {
        bigint id PK
        varchar name "SERVER | WEB | iOS | ANDROID | DESIGN"
        bigint cohort_id FK
    }

    TEAMS {
        bigint id PK
        varchar name "Team A | Team B | ..."
        bigint cohort_id FK
    }

    COHORT_MEMBERS {
        bigint id PK
        bigint member_id FK
        bigint cohort_id FK
        bigint part_id FK "nullable"
        bigint team_id FK "nullable"
        int deposit "보증금 잔액 (초기 100000)"
        int excuse_count "공결 사용 횟수 (최대 3)"
        timestamp created_at
    }

    SESSIONS {
        bigint id PK
        bigint cohort_id FK
        varchar title
        date date
        time time
        varchar location
        enum status "SCHEDULED | IN_PROGRESS | COMPLETED | CANCELLED"
        timestamp created_at
        timestamp updated_at
    }

    QR_CODES {
        bigint id PK
        bigint session_id FK
        varchar hash_value "UUID 기반"
        timestamp created_at
        timestamp expires_at "생성 후 24시간"
    }

    ATTENDANCES {
        bigint id PK
        bigint session_id FK
        bigint member_id FK
        bigint qr_code_id FK "nullable (QR 체크인 시)"
        enum status "PRESENT | ABSENT | LATE | EXCUSED"
        int late_minutes "nullable"
        int penalty_amount "0 ~ 10000"
        varchar reason "nullable"
        timestamp checked_in_at "nullable"
        timestamp created_at
        timestamp updated_at
    }

    DEPOSIT_HISTORIES {
        bigint id PK
        bigint cohort_member_id FK
        enum type "INITIAL | PENALTY | REFUND"
        int amount "양수 또는 음수"
        int balance_after "변동 후 잔액"
        bigint attendance_id FK "nullable"
        varchar description "nullable"
        timestamp created_at
    }

    COHORTS ||--o{ PARTS : "has"
    COHORTS ||--o{ TEAMS : "has"
    COHORTS ||--o{ SESSIONS : "has"
    COHORTS ||--o{ COHORT_MEMBERS : "has"
    MEMBERS ||--o{ COHORT_MEMBERS : "joins"
    PARTS ||--o{ COHORT_MEMBERS : "assigned"
    TEAMS ||--o{ COHORT_MEMBERS : "assigned"
    SESSIONS ||--o{ QR_CODES : "has"
    SESSIONS ||--o{ ATTENDANCES : "has"
    MEMBERS ||--o{ ATTENDANCES : "checks in"
    QR_CODES ||--o{ ATTENDANCES : "via"
    COHORT_MEMBERS ||--o{ DEPOSIT_HISTORIES : "tracks"
    ATTENDANCES ||--o{ DEPOSIT_HISTORIES : "causes"
```

## 테이블 관계 요약

| 관계 | 설명 |
|------|------|
| Cohort → Part | 기수별 파트 (SERVER, WEB, iOS, ANDROID, DESIGN) |
| Cohort → Team | 기수별 팀 (Team A, B, C) |
| Cohort → Session | 기수별 정기 모임 일정 |
| Member ↔ Cohort | CohortMember를 통한 다대다 (보증금, 공결 횟수 포함) |
| Session → QrCode | 일정당 QR 코드 (활성 1개 제한) |
| Session + Member → Attendance | 일정별 회원 출결 기록 |
| CohortMember → DepositHistory | 보증금 변동 이력 |
| Attendance → DepositHistory | 출결로 인한 패널티/환급 기록 |
