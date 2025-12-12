# IPZY 코드 컨벤션

## 네이밍 규칙

| 계층 | 패턴 | 예시 |
|------|------|------|
| Controller | `{Domain}Controller` | `UserController` |
| Service | `{Domain}Service` | `UserService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| Request DTO | `{Action}{Entity}Request` | `UpdateProfileRequest` |
| Response DTO | `{Entity}Response` | `UserProfileResponse` |
| Exception | `{Domain}Exception` | `UserException` |

## 필수 어노테이션

- **Controller**: `@RestController`, `@RequiredArgsConstructor`, `@Tag`
- **Service**: `@Service`, `@Transactional(readOnly=true)`, `@Slf4j`
- **Entity**: `@Entity`, `@Getter`, `@NoArgsConstructor(access=PROTECTED)`, `@Builder`

## 핵심 패턴

### DTO
- Record + Bean Validation
- `from()` 팩토리 메서드
- 참고: `domain/auth/dto/AuthMeResponse.java`

### 예외
- 도메인별 Exception + static 팩토리
- 참고: `domain/user/exception/UserException.java`

## ErrorCode 네이밍

- 형식: `{DOMAIN}_{번호}` (예: `USER_001`, `AUTH_001`, `REC_001`)

## 테스트 패턴

### 구조
- `@Nested` + `@DisplayName` 사용
- Given-When-Then 패턴
- BDDMockito 사용

### 메서드명 규칙
- 성공: `success()`, `success_{상세설명}()`
- 실패: `fail_{실패이유}()`

### 테스트 실행
```bash
./gradlew test                              # 전체 테스트
./gradlew test --tests "*UserServiceTest"   # 단일 클래스
./gradlew test --tests "*UserServiceTest.success*"  # 단일 메서드
```

## API 응답 형식

```json
{ "success": true, "data": {...}, "error": null }
{ "success": false, "data": null, "error": { "code": "USER_001", "message": "..." } }
```

## 주의사항

- Swagger `ApiResponse` 이름 충돌 → FQCN 사용: `@io.swagger.v3.oas.annotations.responses.ApiResponse`
- 테스트 DB는 H2 인메모리 사용 (application-test.yml)
