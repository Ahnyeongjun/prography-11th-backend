[← README](../README.md) | [ERD](ERD.md) | [시스템 아키텍처](SYSTEM_ARCHITECTURE.md) | [AI 사용사례](AI_USAGE.md)

# 설계 결정 및 고민 기록

## 1. 엔티티 설계

### CohortMember 중간 테이블
- Member와 Cohort를 직접 연결하지 않고 CohortMember를 통해 연결
- 이유: 기수별로 보증금(deposit)과 공결 횟수(excuseCount)가 다르므로, 기수-회원 관계에 종속되는 데이터를 별도 테이블로 관리
- 한 회원이 여러 기수에 참여 가능한 구조 (10기 → 11기 전환 등)

### Soft Delete 패턴
- Member: status를 WITHDRAWN으로 변경 (실제 삭제 안 함)
- Session: status를 CANCELLED로 변경
- 이유: 출결/보증금 이력이 회원/일정과 연결되어 있으므로, 물리적 삭제 시 FK 제약 위반 및 이력 데이터 손실 발생

### DepositHistory 이벤트 기록
- 모든 보증금 변동을 이력으로 기록 (INITIAL, PENALTY, REFUND)
- 이유: 보증금 감사(Audit) 추적성 확보. balanceAfter 필드로 특정 시점의 잔액 확인 가능

## 2. 패널티 계산 로직

### calculatePenalty를 companion object으로 분리
```kotlin
companion object {
    fun calculatePenalty(status: AttendanceStatus, lateMinutes: Int?): Int = when (status) {
        PRESENT -> 0
        ABSENT -> 10000
        LATE -> minOf((lateMinutes ?: 0) * 500, 10000)
        EXCUSED -> 0
    }
}
```
- 이유: 순수 함수로 분리하여 테스트 용이성 확보
- 인스턴스 의존성 없이 독립적으로 테스트 가능

### 출결 수정 시 보증금 차이 계산
- `diff = newPenalty - oldPenalty`로 차이만큼만 조정
- diff > 0: 추가 차감 (PENALTY)
- diff < 0: 환급 (REFUND)
- diff == 0: 보증금 변동 없음
- 이유: 전액 환급 후 재차감 방식보다 diff 계산이 트랜잭션이 단순하고 이력이 깔끔함

## 3. QR 출석 체크 검증 순서

HELP.md에 명시된 7단계 순서를 그대로 구현:
1. QR hashValue 유효성
2. QR 만료 여부
3. 세션 상태 (IN_PROGRESS)
4. 회원 존재 여부
5. 회원 탈퇴 여부
6. 중복 출결 여부
7. 기수 회원 정보 존재 여부

- 고민: 검증 순서에 따라 에러 메시지가 달라지므로, 명세에 정의된 순서를 엄격히 따름
- 지각 판정: 일정의 date + time을 Asia/Seoul 기준으로 변환 후 현재 시각과 비교

## 4. 기수 고정 설정

```properties
current-cohort.generation=11
```
- `@Value`로 주입하여 서비스 레이어에서 사용
- 이유: 하드코딩 대신 설정 파일로 분리하여 기수 변경 시 코드 수정 불필요
- 향후 여러 기수 동시 운영 시 쉽게 확장 가능

## 5. 공결(EXCUSED) 횟수 관리

- CohortMember.excuseCount로 기수별 관리
- 출결 등록/수정 시 양방향 카운트 조정:
  - 다른 상태 → EXCUSED: +1
  - EXCUSED → 다른 상태: -1
- 이유: Attendance 테이블을 매번 카운트 쿼리하는 것보다 효율적

## 6. 테스트 전략

- 서비스 레이어 단위 테스트에 집중 (Mockito 활용)
- Repository는 Mock 처리하여 비즈니스 로직만 검증
- 이유: 과제 요구사항이 "서비스 레이어 단위 테스트"이므로, 통합 테스트보다 단위 테스트 우선
- 총 47개 테스트로 주요 비즈니스 시나리오 커버
