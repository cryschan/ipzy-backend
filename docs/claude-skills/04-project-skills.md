# 프로젝트 스킬

이 프로젝트(ipzy-backend)에서 사용할 수 있는 스킬 모음입니다.

## 스킬 설치 방법

프로젝트 스킬은 `.claude/skills/` 폴더에 위치합니다.

```bash
# 스킬 폴더 생성
mkdir -p .claude/skills/스킬명

# SKILL.md 생성
touch .claude/skills/스킬명/SKILL.md
```

---

## 추천 스킬 1: ipzy-api-generator

이 프로젝트의 구조에 맞는 API 생성기입니다.

### 설치

```bash
mkdir -p .claude/skills/ipzy-api-generator
```

### SKILL.md

**`.claude/skills/ipzy-api-generator/SKILL.md`**

```yaml
---
name: ipzy-api-generator
description: ipzy-backend REST API 생성. 새 API, Controller 생성 시 사용.
---

# ipzy-backend API 생성기

## 프로젝트 구조
```
src/main/java/com/ipzy/
├── domain/
│   └── [도메인명]/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
└── global/
    ├── config/
    ├── exception/
    └── common/
```

## 생성 순서
1. Entity 생성
2. Repository 생성
3. DTO (Request/Response) 생성
4. Service 생성
5. Controller 생성
6. 테스트 코드 생성

## 컨벤션
- Controller: `@RestController`, `@RequestMapping("/api/v1/도메인")`
- Service: `@Service`, `@Transactional(readOnly = true)`
- Repository: `JpaRepository` 상속
- Entity: `@Entity`, BaseTimeEntity 상속
```

---

## 추천 스킬 2: ipzy-code-reviewer

### 설치

```bash
mkdir -p .claude/skills/ipzy-code-reviewer
```

### SKILL.md

**`.claude/skills/ipzy-code-reviewer/SKILL.md`**

```yaml
---
name: ipzy-code-reviewer
description: ipzy-backend 코드 리뷰. PR 리뷰, 코드 검토 시 사용.
allowed-tools: Read, Grep, Glob
---

# ipzy-backend 코드 리뷰어

## 프로젝트 규칙 체크

### 1. 레이어 규칙
- [ ] Controller는 Service만 의존
- [ ] Service는 Repository만 의존
- [ ] Entity는 다른 레이어에 노출 금지 (DTO 사용)

### 2. 네이밍 컨벤션
- Controller: `*Controller`
- Service: `*Service`
- Repository: `*Repository`
- Entity: 단수형 명사
- DTO: `*Request`, `*Response`

### 3. 보안 체크
- [ ] @Valid 검증 사용
- [ ] SQL Injection 방지
- [ ] 민감 정보 로깅 금지

### 4. 성능 체크
- [ ] FetchType.LAZY 사용
- [ ] N+1 쿼리 방지
- [ ] 페이징 처리

## Output Format
```markdown
## 코드 리뷰 결과

### 준수 사항
-

### 개선 필요
- 파일:라인 - 설명

### 필수 수정
- 파일:라인 - 설명
```
```

---

## 추천 스킬 3: ipzy-test-writer

### 설치

```bash
mkdir -p .claude/skills/ipzy-test-writer
```

### SKILL.md

**`.claude/skills/ipzy-test-writer/SKILL.md`**

```yaml
---
name: ipzy-test-writer
description: ipzy-backend 테스트 코드 생성. 테스트 작성, 유닛 테스트 시 사용.
---

# ipzy-backend 테스트 생성기

## 테스트 구조
```
src/test/java/com/ipzy/
├── domain/
│   └── [도메인명]/
│       ├── controller/
│       │   └── *ControllerTest.java
│       ├── service/
│       │   └── *ServiceTest.java
│       └── repository/
│           └── *RepositoryTest.java
└── support/
    └── TestFixture.java
```

## Service 테스트 템플릿
```java
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Nested
    @DisplayName("findById 메서드는")
    class FindById {

        @Test
        @DisplayName("존재하는 ID로 조회하면 리소스를 반환한다")
        void success() {
            // given
            // when
            // then
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외를 던진다")
        void notFound() {
            // given
            // when & then
        }
    }
}
```

## Controller 테스트 템플릿
```java
@WebMvcTest(ResourceController.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/resources/{id} - 성공")
    void findById() throws Exception {
        // given
        // when & then
        mockMvc.perform(get("/api/v1/resources/{id}", 1L))
                .andExpect(status().isOk());
    }
}
```

## 커버리지 목표
- Line: 80%+
- Branch: 75%+
```

---

## 스킬 활성화 확인

Claude Code에서 다음과 같이 확인할 수 있습니다:

```
"사용 가능한 스킬 알려줘"
```

## 팀 공유

스킬을 팀과 공유하려면:

```bash
git add .claude/skills/
git commit -m "Add project skills"
git push
```

팀원은 `git pull` 후 자동으로 스킬을 사용할 수 있습니다.
