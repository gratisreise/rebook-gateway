# Rebook Gateway

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.13-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.6-blue)
![Java](https://img.shields.io/badge/Java-17-orange)

Rebook 마이크로서비스 아키텍처의 API Gateway. 모든 클라이언트 요청의 단일 진입점으로 JWT 인증, 동적 라우팅, 서비스 디스커버리를 담당합니다.

---

## 아키텍처

- 라우팅
![라우팅](https://diagrams-noaahh.s3.ap-northeast-2.amazonaws.com/gateway.png)

## 기능

### 인증

- JWT 토큰 검증 (HMAC-SHA 서명 + 만료 확인)
- 연결 타입별 토큰 추출 전략 (Header / Query Parameter)
- Passport 기반 사용자 컨텍스트 전파 (`X-Passport` 헤더)
- 인증 불필요 경로 화이트리스트 관리

### 라우팅

| 서비스 | 경로 | 프로토콜 |
|--------|------|----------|
| AUTH-SERVICE | `/api/auth/**` | HTTP |
| USER-SERVICE | `/api/users/**` | HTTP |
| BOOK-SERVICE | `/api/books/**` | HTTP |
| TRADING-SERVICE | `/api/tradings/**` | HTTP |
| NOTIFICATION-SERVICE | `/api/notifications/**` | HTTP + SSE |
| CHAT-SERVICE | `/api/chats/**`, `/api/ws-chat/**` | HTTP + WebSocket |

- Eureka 기반 서비스 디스커버리 + 클라이언트 사이드 로드 밸런싱 (`lb://`)
- WebSocket 전용 라우팅 (`lb:ws://CHAT-SERVICE`)

### CORS

| 환경 | 허용 Origin |
|------|-------------|
| Dev | `localhost:5173`, `host.docker.internal:5173` |
| Prod | `rebookk.click` |

---

## 기술 스택

### Language & Framework
- **Java 17**, **Spring Boot 3.3.13**, **Spring WebFlux**

### Microservices
- **Spring Cloud Gateway 2023.0.6** — 라우팅 및 필터링
- **Eureka Client** — 서비스 디스커버리 및 등록

### Security
- **Spring Security (Reactive)**, **jjwt 0.13.0**

### Monitoring
- **Actuator**, **Prometheus**, **Sentry 8.13.2**

### Build & Deploy
- **Gradle**, **Docker** (eclipse-temurin:17-jre)

---

## 프로젝트 구조

```
src/main/java/com/example/rebookgateway/
├── RebookGatewayApplication.java   # @SpringBootApplication 진입점
├── CustomFilter.java               # 글로벌 인증 필터 (Order: -10)
├── JwtUtil.java                    # JWT 파싱, 서명 검증, 사용자 ID 추출
└── SecurityConfig.java             # 화이트리스트, CORS, CSRF 비활성화

src/main/resources/
├── application.yaml                # 공통 설정 (port 8080, JWT_SECRET)
├── application-dev.yaml            # Dev: localhost CORS, 전체 Actuator 노출
└── application-prod.yaml           # Prod: rebookk.click CORS, 제한적 Actuator
```
