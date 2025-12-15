---
name: ipzy-test-strategy
description: 테스트 전략 수립. 새 기능 테스트 계획, 테스트 커버리지 향상, 테스트 설계 시 사용.
allowed-tools: Read, Grep, Glob
---

# IPZY Test Strategy

기능별 테스트 전략을 수립하고 테스트 케이스를 설계합니다.

## IPZY 테스트 규칙

### 기본 구조
```java
@Nested
@DisplayName("{메서드명} 메서드는")
class {MethodName} {

    @Test
    @DisplayName("{조건}일 때 {결과}를 반환한다")
    void {testMethodName}() {
        // given

        // when

        // then
    }
}
```

### 사용 라이브러리
- **Mocking**: BDDMockito (`given`, `willReturn`, `then`, `should`)
- **Assertion**: AssertJ (`assertThat`, `assertThatThrownBy`)
- **테스트 구조**: JUnit 5 (`@Nested`, `@DisplayName`)

## 워크플로우

```
테스트 대상 분석
       ↓
[1단계] 테스트 범위 결정
       ↓
[2단계] 테스트 케이스 도출
       ↓
[3단계] 모킹 전략 수립
       ↓
[4단계] 테스트 데이터 설계
       ↓
테스트 전략서 출력
```

## 출력 템플릿

```markdown
# {대상} 테스트 전략

> 작성일: {YYYY-MM-DD}
> 대상: {클래스/기능명}

---

## 1. 테스트 범위

### 테스트 유형

| 유형 | 적용 | 이유 |
|------|------|------|
| 단위 테스트 | O/X | {이유} |
| 통합 테스트 | O/X | {이유} |
| E2E 테스트 | O/X | {이유} |

### 테스트 대상 메서드

| 메서드 | 우선순위 | 복잡도 |
|--------|----------|--------|
| `{method1}()` | 높음/중간/낮음 | 높음/중간/낮음 |
| `{method2}()` | 높음/중간/낮음 | 높음/중간/낮음 |

---

## 2. 테스트 케이스

### 2.1 {메서드명}()

#### 성공 케이스

| ID | 시나리오 | 입력 | 예상 결과 |
|----|----------|------|-----------|
| TC-001 | {시나리오} | {입력} | {결과} |
| TC-002 | {시나리오} | {입력} | {결과} |

#### 실패 케이스

| ID | 시나리오 | 입력 | 예상 예외 |
|----|----------|------|-----------|
| TC-003 | {시나리오} | {입력} | {예외} |
| TC-004 | {시나리오} | {입력} | {예외} |

#### 경계값 케이스

| ID | 시나리오 | 입력 | 예상 결과 |
|----|----------|------|-----------|
| TC-005 | {시나리오} | {입력} | {결과} |

### 2.2 {메서드명}()

...

---

## 3. 모킹 전략

### 모킹 대상

| 의존성 | 모킹 여부 | 이유 |
|--------|-----------|------|
| `{Repository}` | O | DB 의존성 제거 |
| `{ExternalClient}` | O | 외부 API 의존성 제거 |
| `{OtherService}` | X | 실제 동작 검증 필요 |

### 모킹 설정 예시

```java
@ExtendWith(MockitoExtension.class)
class {TargetClass}Test {

    @Mock
    private {Repository} {repository};

    @Mock
    private {ExternalClient} {client};

    @InjectMocks
    private {TargetClass} {target};

    @Nested
    @DisplayName("{methodName} 메서드는")
    class {MethodName} {

        @Test
        @DisplayName("정상 입력일 때 성공한다")
        void success() {
            // given
            given({repository}.findById(anyLong()))
                .willReturn(Optional.of({entity}));

            // when
            var result = {target}.{methodName}({params});

            // then
            assertThat(result).isNotNull();
            then({repository}).should().findById(anyLong());
        }
    }
}
```

---

## 4. 테스트 데이터

### Fixture 설계

```java
class {Domain}Fixture {

    public static {Domain} create{Domain}() {
        return {Domain}.builder()
            .id(1L)
            .{field}("{value}")
            .build();
    }

    public static {Domain} create{Domain}WithId(Long id) {
        return {Domain}.builder()
            .id(id)
            .{field}("{value}")
            .build();
    }
}
```

### Request DTO 테스트 데이터

```java
class {Request}Fixture {

    public static {Request} createValid{Request}() {
        return new {Request}("{validValue}");
    }

    public static {Request} createInvalid{Request}() {
        return new {Request}("");  // validation 실패
    }
}
```

---

## 5. 테스트 코드 템플릿

### Service 테스트

```java
@ExtendWith(MockitoExtension.class)
class {Service}Test {

    @Mock
    private {Repository} {repository};

    @InjectMocks
    private {Service} {service};

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 응답을 반환한다")
        void success_whenExists() {
            // given
            Long id = 1L;
            {Entity} entity = {Entity}Fixture.create{Entity}();
            given({repository}.findById(id))
                .willReturn(Optional.of(entity));

            // when
            {Response} result = {service}.findById(id);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(id);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외를 던진다")
        void fail_whenNotExists() {
            // given
            Long id = 999L;
            given({repository}.findById(id))
                .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> {service}.findById(id))
                .isInstanceOf({Domain}Exception.class)
                .hasMessageContaining("찾을 수 없습니다");
        }
    }
}
```

### Controller 테스트

```java
@WebMvcTest({Controller}.class)
@Import(SecurityConfig.class)
class {Controller}Test {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private {Service} {service};

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("GET /api/{path}/{id}")
    class FindById {

        @Test
        @DisplayName("인증된 사용자가 조회하면 200을 반환한다")
        @WithMockUser
        void success() throws Exception {
            // given
            Long id = 1L;
            {Response} response = new {Response}(id, "value");
            given({service}.findById(id)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/{path}/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        }

        @Test
        @DisplayName("인증되지 않은 사용자는 401을 반환한다")
        void fail_whenUnauthorized() throws Exception {
            // when & then
            mockMvc.perform(get("/api/{path}/1"))
                .andExpect(status().isUnauthorized());
        }
    }
}
```

---

## 6. 검증 체크리스트

### 테스트 품질

- [ ] 모든 public 메서드 테스트 커버
- [ ] 성공/실패 케이스 모두 포함
- [ ] 경계값 테스트 포함
- [ ] 예외 메시지 검증

### 테스트 독립성

- [ ] 테스트 간 의존성 없음
- [ ] 테스트 순서 무관
- [ ] 외부 의존성 모킹 완료

### 가독성

- [ ] @DisplayName 의미 명확
- [ ] given-when-then 구조 준수
- [ ] 테스트 메서드명 명확

---

## 7. 실행 명령어

```bash
# 전체 테스트
./gradlew test

# 특정 클래스 테스트
./gradlew test --tests "{ClassName}Test"

# 특정 메서드 테스트
./gradlew test --tests "{ClassName}Test.{methodName}"

# 커버리지 리포트
./gradlew jacocoTestReport
```
```

## 테스트 우선순위 기준

| 우선순위 | 기준 |
|----------|------|
| 높음 | 핵심 비즈니스 로직, 결제/인증 |
| 중간 | 일반 CRUD, 데이터 변환 |
| 낮음 | 단순 getter, 설정 클래스 |

## 사용 예시

### 입력
```
"PaymentService 테스트 전략 세워줘"
```

### 출력 (요약)
```markdown
# PaymentService 테스트 전략

## 테스트 대상
- createPayment() - 우선순위 높음
- cancelPayment() - 우선순위 높음
- getPaymentHistory() - 우선순위 중간

## 테스트 케이스
### createPayment()
- TC-001: 정상 결제 요청 → 결제 성공
- TC-002: 잔액 부족 → PaymentException
- TC-003: 중복 결제 요청 → PaymentException
- TC-004: 외부 API 실패 → PaymentException

## 모킹 대상
- PaymentRepository (O)
- TossPaymentClient (O) - 외부 API
- UserRepository (O)

## Fixture
- PaymentFixture.createValidPayment()
- PaymentRequestFixture.createValidRequest()
```

## 트리거 키워드

- "테스트 전략", "테스트 계획"
- "테스트 케이스", "테스트 설계"
- "어떻게 테스트", "뭘 테스트"
