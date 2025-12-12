# raeperd/realworld CRUD 패턴 코드 예시

> 분석일: 2025-12-13
> 로컬 경로: `/Users/chan/workspace/raeperd-realworld`

## 1. 전체 CRUD 흐름 다이어그램

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Controller    │────▶│     Service     │────▶│     Entity      │────▶│   Repository    │
│  (application)  │     │    (domain)     │     │    (domain)     │     │    (domain)     │
└─────────────────┘     └─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  RequestDTO     │     │  UpdateRequest  │     │  Value Object   │
│  ResponseModel  │     │  (Optional 활용) │     │  (@Embeddable)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

---

## 2. CREATE 패턴

### 2.1 Controller (Create)

```java
// application/article/ArticleRestController.java
@RestController
class ArticleRestController {

    private final ArticleService articleService;

    // 생성자 주입 (Lombok 없이)
    ArticleRestController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/articles")
    public ArticleModel postArticle(
            @AuthenticationPrincipal UserJWTPayload jwtPayload,
            @Valid @RequestBody ArticlePostRequestDTO dto) {

        // 1. DTO → Domain 객체 변환
        // 2. Service 호출
        // 3. Entity → Response Model 변환
        var articleCreated = articleService.createNewArticle(
            jwtPayload.getUserId(),
            dto.toArticleContents()
        );
        return ArticleModel.fromArticle(articleCreated);
    }
}
```

### 2.2 Request DTO (Create)

```java
// application/article/ArticlePostRequestDTO.java
@JsonTypeName("article")
@JsonTypeInfo(include = WRAPPER_OBJECT, use = NAME)
@Value  // Lombok: final 필드 + getter + equals/hashCode
class ArticlePostRequestDTO {

    @NotBlank
    String title;

    @NotBlank
    String description;

    @NotBlank
    String body;

    @NotNull
    Set<Tag> tagList;

    // DTO → Domain 변환 메서드
    ArticleContents toArticleContents() {
        return new ArticleContents(
            description,
            ArticleTitle.of(title),  // Value Object 생성
            body,
            tagList
        );
    }
}
```

### 2.3 Service (Create)

```java
// domain/article/ArticleService.java
@Service
public class ArticleService implements ArticleFindService {

    private final UserFindService userFindService;
    private final TagService tagService;
    private final ArticleRepository articleRepository;

    @Transactional
    public Article createNewArticle(long authorId, ArticleContents contents) {
        // 1. 태그 재로드 (이미 존재하는 태그면 기존 것 사용)
        final var tagsReloaded = tagService.reloadAllTagsIfAlreadyPresent(contents.getTags());
        contents.setTags(tagsReloaded);

        // 2. 사용자 조회 → 글 작성 → 저장
        return userFindService.findById(authorId)
                .map(author -> author.writeArticle(contents))  // Entity가 생성 담당
                .map(articleRepository::save)
                .orElseThrow(NoSuchElementException::new);
    }
}
```

### 2.4 Entity (Create - 생성 로직)

```java
// domain/user/User.java
@Entity
public class User {

    // User가 Article 생성을 담당 (Rich Domain Model)
    public Article writeArticle(ArticleContents contents) {
        return new Article(this, contents);
    }
}

// domain/article/Article.java
@Entity
public class Article {

    public Article(User author, ArticleContents contents) {
        this.author = author;
        this.contents = contents;
    }

    protected Article() { }  // JPA용
}
```

---

## 3. READ 패턴

### 3.1 Controller (Read - 단건/목록)

```java
// application/article/ArticleRestController.java

// 단건 조회 - Optional → ResponseEntity
@GetMapping("/articles/{slug}")
public ResponseEntity<ArticleModel> getArticleBySlug(@PathVariable String slug) {
    return of(articleService.getArticleBySlug(slug)
            .map(ArticleModel::fromArticle));
}

// 목록 조회 - Page 활용
@GetMapping("/articles")
public MultipleArticleModel getArticles(Pageable pageable) {
    final var articles = articleService.getArticles(pageable);
    return MultipleArticleModel.fromArticles(articles);
}

// 조건 조회 - @RequestParam + params 속성
@GetMapping(value = "/articles", params = {"author"})
public MultipleArticleModel getArticlesByAuthor(
        @RequestParam String author,
        Pageable pageable) {
    final var articles = articleService.getArticlesByAuthorName(author, pageable);
    return MultipleArticleModel.fromArticles(articles);
}

@GetMapping(value = "/articles", params = {"tag"})
public MultipleArticleModel getArticlesByTag(
        @RequestParam String tag,
        Pageable pageable) {
    final var articles = articleService.getArticlesByTag(tag, pageable);
    return MultipleArticleModel.fromArticles(articles);
}
```

### 3.2 Service (Read)

```java
// domain/article/ArticleService.java

// 조회용 인터페이스 구현
@Service
public class ArticleService implements ArticleFindService {

    @Transactional(readOnly = true)  // 읽기 전용 트랜잭션
    public Page<Article> getArticles(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> getArticleBySlug(String slug) {
        return articleRepository.findFirstByContentsTitleSlug(slug);
    }

    @Transactional(readOnly = true)
    public Page<Article> getArticlesByAuthorName(String authorName, Pageable pageable) {
        return articleRepository.findAllByAuthorProfileUserName(
            new UserName(authorName), pageable);
    }
}
```

### 3.3 조회 전용 인터페이스

```java
// domain/article/ArticleFindService.java
public interface ArticleFindService {
    Optional<Article> getArticleBySlug(String slug);
}

// domain/user/UserFindService.java
public interface UserFindService {
    Optional<User> findById(long id);
    Optional<User> findByUsername(UserName userName);
}
```

### 3.4 Repository (Read)

```java
// domain/article/ArticleRepository.java
interface ArticleRepository extends Repository<Article, Long> {

    // Spring Data JPA 네이밍 규칙 활용
    Page<Article> findAll(Pageable pageable);

    // Embedded 객체의 필드로 조회
    Optional<Article> findFirstByContentsTitleSlug(String slug);

    // 연관 Entity의 Embedded 필드로 조회
    Page<Article> findAllByAuthorProfileUserName(UserName authorName, Pageable pageable);

    // 컬렉션 포함 여부로 조회
    Page<Article> findAllByUserFavoritedContains(User user, Pageable pageable);
    Page<Article> findAllByContentsTagsContains(Tag tag, Pageable pageable);
}
```

### 3.5 Response Model

```java
// application/article/ArticleModel.java
@Value
class ArticleModel {

    ArticleModelNested article;

    // 정적 팩토리 메서드
    static ArticleModel fromArticle(Article article) {
        return new ArticleModel(ArticleModelNested.fromArticle(article));
    }

    @Value
    static class ArticleModelNested {
        String slug;
        String title;
        String description;
        String body;
        Set<String> tagList;
        ZonedDateTime createdAt;
        ZonedDateTime updatedAt;
        boolean favorited;
        int favoritesCount;
        ProfileModelNested author;

        static ArticleModelNested fromArticle(Article article) {
            final var contents = article.getContents();
            final var titleFromArticle = contents.getTitle();
            return new ArticleModelNested(
                    titleFromArticle.getSlug(),
                    titleFromArticle.getTitle(),
                    contents.getDescription(),
                    contents.getBody(),
                    contents.getTags().stream()
                        .map(Tag::toString)
                        .collect(toSet()),
                    article.getCreatedAt().atZone(ZoneId.of("Asia/Seoul")),
                    article.getUpdatedAt().atZone(ZoneId.of("Asia/Seoul")),
                    article.isFavorited(),
                    article.getFavoritedCount(),
                    ProfileModelNested.fromProfile(article.getAuthor().getProfile())
            );
        }
    }
}
```

---

## 4. UPDATE 패턴

### 4.1 Controller (Update)

```java
// application/article/ArticleRestController.java
@PutMapping("/articles/{slug}")
public ArticleModel putArticleBySlug(
        @AuthenticationPrincipal UserJWTPayload jwtPayload,
        @PathVariable String slug,
        @RequestBody ArticlePutRequestDTO dto) {

    final var articleUpdated = articleService.updateArticle(
        jwtPayload.getUserId(),
        slug,
        dto.toUpdateRequest()
    );
    return ArticleModel.fromArticle(articleUpdated);
}
```

### 4.2 Request DTO (Update) - Optional 활용

```java
// application/article/ArticlePutRequestDTO.java
@JsonTypeName("article")
@JsonTypeInfo(include = WRAPPER_OBJECT, use = NAME)
@Value
class ArticlePutRequestDTO {

    String title;        // nullable - 부분 업데이트
    String description;  // nullable
    String body;         // nullable

    ArticleUpdateRequest toUpdateRequest() {
        return builder()
            .titleToUpdate(ofNullable(title)
                .map(ArticleTitle::of)
                .orElse(null))
            .descriptionToUpdate(description)
            .bodyToUpdate(body)
            .build();
    }
}
```

### 4.3 UpdateRequest (Domain 객체) - Optional 반환

```java
// domain/article/ArticleUpdateRequest.java
public class ArticleUpdateRequest {

    private final ArticleTitle titleToUpdate;
    private final String descriptionToUpdate;
    private final String bodyToUpdate;

    // Optional로 반환 → null 체크 없이 깔끔한 업데이트
    Optional<ArticleTitle> getTitleToUpdate() {
        return ofNullable(titleToUpdate);
    }

    Optional<String> getDescriptionToUpdate() {
        return ofNullable(descriptionToUpdate);
    }

    Optional<String> getBodyToUpdate() {
        return ofNullable(bodyToUpdate);
    }

    // Builder 패턴 (Lombok 없이 직접 구현)
    public static ArticleUpdateRequestBuilder builder() {
        return new ArticleUpdateRequestBuilder();
    }

    private ArticleUpdateRequest(ArticleUpdateRequestBuilder builder) {
        this.titleToUpdate = builder.titleToUpdate;
        this.descriptionToUpdate = builder.descriptionToUpdate;
        this.bodyToUpdate = builder.bodyToUpdate;
    }

    public static class ArticleUpdateRequestBuilder {
        private ArticleTitle titleToUpdate;
        private String descriptionToUpdate;
        private String bodyToUpdate;

        public ArticleUpdateRequestBuilder titleToUpdate(ArticleTitle t) {
            this.titleToUpdate = t;
            return this;
        }
        // ... 생략
        public ArticleUpdateRequest build() {
            return new ArticleUpdateRequest(this);
        }
    }
}
```

### 4.4 Service (Update) - mapIfAllPresent 활용

```java
// domain/article/ArticleService.java
@Transactional
public Article updateArticle(long userId, String slug, ArticleUpdateRequest request) {
    // 두 Optional이 모두 존재할 때만 실행
    return mapIfAllPresent(
            userFindService.findById(userId),
            getArticleBySlug(slug),
            (user, article) -> user.updateArticle(article, request)
    ).orElseThrow(NoSuchElementException::new);
}
```

### 4.5 Entity (Update) - 권한 검증 포함

```java
// domain/user/User.java
public Article updateArticle(Article article, ArticleUpdateRequest request) {
    // 권한 검증: 작성자만 수정 가능
    if (article.getAuthor() != this) {
        throw new IllegalAccessError("Not authorized to update this article");
    }
    article.updateArticle(request);
    return article;
}

// domain/article/ArticleContents.java (Embedded)
void updateArticleContentsIfPresent(ArticleUpdateRequest updateRequest) {
    // Optional.ifPresent 활용 - null 체크 없이 깔끔
    updateRequest.getTitleToUpdate()
        .ifPresent(titleToUpdate -> title = titleToUpdate);
    updateRequest.getDescriptionToUpdate()
        .ifPresent(descriptionToUpdate -> description = descriptionToUpdate);
    updateRequest.getBodyToUpdate()
        .ifPresent(bodyToUpdate -> body = bodyToUpdate);
}
```

---

## 5. DELETE 패턴

### 5.1 Controller (Delete)

```java
// application/article/ArticleRestController.java
@ResponseStatus(NO_CONTENT)  // 204 반환
@DeleteMapping("/articles/{slug}")
public void deleteArticleBySlug(
        @AuthenticationPrincipal UserJWTPayload jwtPayload,
        @PathVariable String slug) {
    articleService.deleteArticleBySlug(jwtPayload.getUserId(), slug);
}
```

### 5.2 Service (Delete)

```java
// domain/article/ArticleService.java
@Transactional
public void deleteArticleBySlug(long userId, String slug) {
    userFindService.findById(userId)
        .ifPresentOrElse(
            user -> articleRepository.deleteArticleByAuthorAndContentsTitleSlug(user, slug),
            () -> { throw new NoSuchElementException(); }
        );
}
```

### 5.3 Repository (Delete)

```java
// domain/article/ArticleRepository.java
interface ArticleRepository extends Repository<Article, Long> {
    // 작성자 + slug로 삭제 (권한 검증 포함)
    void deleteArticleByAuthorAndContentsTitleSlug(User author, String slug);
}
```

---

## 6. Value Object 패턴

### 6.1 ArticleTitle (자동 slug 생성)

```java
// domain/article/ArticleTitle.java
@Embeddable
public class ArticleTitle {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String slug;

    // 정적 팩토리 - title로부터 slug 자동 생성
    public static ArticleTitle of(String title) {
        return new ArticleTitle(title, slugFromTitle(title));
    }

    private ArticleTitle(String title, String slug) {
        this.title = title;
        this.slug = slug;
    }

    protected ArticleTitle() { }  // JPA용

    private static String slugFromTitle(String title) {
        return title.toLowerCase()
                .replaceAll("\\$,'\"|\\s|\\.|\\?", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-)|(-$)", "");
    }

    // equals/hashCode - slug 기준
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleTitle that = (ArticleTitle) o;
        return slug.equals(that.slug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(slug);
    }
}
```

### 6.2 Email (단순 래핑)

```java
// domain/user/Email.java
@Embeddable
public class Email {

    @Column(name = "email", nullable = false)
    private String address;

    public Email(String address) {
        this.address = address;
    }

    protected Email() { }

    @Override
    public String toString() {
        return address;
    }

    // equals/hashCode 구현
}
```

### 6.3 Password (인코딩 로직 캡슐화)

```java
// domain/user/Password.java
@Embeddable
class Password {  // package-private

    @Column(name = "password", nullable = false)
    private String encodedPassword;

    // 정적 팩토리 - 인코딩 로직 캡슐화
    static Password of(String rawPassword, PasswordEncoder passwordEncoder) {
        return new Password(passwordEncoder.encode(rawPassword));
    }

    private Password(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    protected Password() { }

    // 비밀번호 검증
    boolean matchesPassword(String rawPassword, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

---

## 7. IPZY 적용 체크리스트

### 즉시 적용 가능

- [ ] Controller class에서 `public` 제거
- [ ] `@ResponseStatus(NO_CONTENT)` 사용 (DELETE)
- [ ] `ResponseEntity.of()` 사용 (Optional → ResponseEntity)
- [ ] 조회 메서드에 `@Transactional(readOnly = true)` 명시

### 중기 적용

- [ ] UpdateRequest 클래스에 Optional getter 패턴 도입
- [ ] 조회 전용 인터페이스 분리 (UserFindService 패턴)
- [ ] Value Object로 원시 타입 래핑 (Email 등)

### 장기 고려

- [ ] Entity에 비즈니스 로직 이동 (Rich Domain Model)
- [ ] `mapIfAllPresent` 유틸리티 활용
