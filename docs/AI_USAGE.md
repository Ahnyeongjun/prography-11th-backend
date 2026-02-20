[← README](../README.md) | [ERD](ERD.md) | [시스템 아키텍처](SYSTEM_ARCHITECTURE.md) | [설계 결정](DECISIONS.md)

# AI 사용사례

## 사용 도구
- **Claude Code** (Claude Opus 4.6) - Anthropic CLI 기반 AI 코딩 어시스턴트

## 활용 내역

### 1. 프로젝트 초기 설계
- HELP.md 요구사항 분석 및 API 명세 파악
- 엔티티 관계 설계 (9개 테이블, FK 관계, Enum 정의)
- 프로젝트 패키지 구조 설계

### 2. 코드 구현
- JPA 엔티티, Repository, DTO, Service, Controller 전체 구현
- Spring Boot 4.0.3 + Kotlin 2.2.21 환경에서의 호환성 이슈 해결
  - `ApplicationArguments` nullable 변경 (SB4)
  - `passwordEncoder.encode()` 반환 타입 `String?` 처리 (Spring Security 7)
  - Jackson 3.x 모듈명 변경 (`tools.jackson.module`)
- 비즈니스 로직 구현 (패널티 계산, 보증금 자동 조정, QR 7단계 검증)

### 3. 시드 데이터
- DataInitializer 구현 (기수, 파트, 팀, admin 계정, 보증금 이력)

### 4. 테스트 코드 작성
- 5개 서비스 클래스에 대한 47개 단위 테스트 작성 (Mockito 기반)
- 정상 케이스 + 예외 케이스 커버

### 5. 디버깅
- Gradle 테스트 실행 시 `ClassNotFoundException` 해결
  - 원인: 프로젝트 경로에 한글(`진행 중`)이 포함되어 Gradle test worker의 classpath 해석 실패
  - 해결: 비ASCII 경로 감지 시 build 디렉토리를 시스템 temp로 리다이렉트

### 6. 문서 작성
- ERD (Mermaid 다이어그램)
- System Design Architecture (현재 + 이상적 아키텍처)
- 설계 결정 기록 (DECISIONS.md)
- README (실행 방법, API 목록, 프로젝트 구조)

## AI 활용 방식
- 요구사항 분석 → 설계 → 구현 → 테스트 → 디버깅 → 문서화 전 과정에서 활용
- 커밋은 기능 단위로 분리하여 10개 커밋으로 관리
