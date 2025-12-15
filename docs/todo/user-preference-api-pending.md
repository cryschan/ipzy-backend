# 사용자 환경설정/스타일 선호도 API - 보류

> 작성일: 2025-12-11
> 상태: 보류 (Swagger에서 숨김 처리)

---

## 보류된 API

### 1. 환경설정 수정 API

**엔드포인트**: `PUT /api/users/me/preferences`

**용도**: 다크모드, 알림 등 앱 환경설정 수정

**Request Body**:
```java
public record UpdatePreferencesRequest(
    Map<String, Object> preferences
) {}
```

**관련 파일**:
- `UserController.java` - `updatePreferences()` 메서드
- `UpdatePreferencesRequest.java`
- `UserService.java` - `updatePreferences()` 메서드

---

### 2. 스타일 선호도 수정 API

**엔드포인트**: `PUT /api/users/me/style-preference`

**용도**: 추천에 사용되는 스타일 선호도(색상, 나이, 성별, 스타일) 수정

**Request Body**:
```java
public record UpdateStylePreferenceRequest(
    List<String> preferredColors,
    Integer ageGroup,
    Gender gender,
    List<String> preferredStyles
) {
    public UserStylePreference toVo() { ... }
}
```

**관련 파일**:
- `UserController.java` - `updateStylePreference()` 메서드
- `UpdateStylePreferenceRequest.java`
- `UserStylePreference.java` (VO)
- `UserService.java` - `updateStylePreference()` 메서드

---

## 보류 사유

1. 현재 MVP에서 사용하지 않음
2. 프론트엔드 화면 미구현
3. 추후 마이페이지 기능 확장 시 활성화 예정

---

## 활성화 방법

`UserController.java`에서 `@Hidden` 어노테이션 제거:

```java
// 현재 (숨김)
@Hidden
@Operation(summary = "환경설정 수정", ...)
@PutMapping("/me/preferences")

// 활성화 시
@Operation(summary = "환경설정 수정", ...)
@PutMapping("/me/preferences")
```

---

## 체크리스트

- [ ] 프론트엔드 마이페이지 환경설정 화면 구현
- [ ] 프론트엔드 스타일 선호도 설정 화면 구현
- [ ] API 테스트 및 검증
- [ ] Swagger `@Hidden` 제거
