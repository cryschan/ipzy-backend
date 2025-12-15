# Java 직렬화 (Serialization)

## 개념

**객체를 바이트 스트림으로 변환하는 것**

```
[메모리 속 객체]  →  직렬화  →  [바이트 데이터]  →  저장/전송 가능
                                     ↓
                    역직렬화  ←  [바이트 데이터]  ←  불러오기
                        ↓
                [메모리 속 객체 복원]
```

---

## 왜 필요한가?

### 메모리 주소의 한계

객체는 메모리에서 **주소(포인터)**로 연결되어 있다.

```
메모리 (서버 A)
┌─────────────────────────────────────┐
│  주소 0x7F3A  →  userId: 1         │
│               →  email: 0x8B2C ────┼──→ "user@example.com"
└─────────────────────────────────────┘
```

**문제:** 메모리 주소는 해당 프로세스/서버에서만 유효하다.

```
서버 A: "userId는 메모리 0x7F3A에 있어"
          ↓
        Redis나 서버 B로 이동
          ↓
서버 B: "0x7F3A? 그게 뭔데?" → 💥 오류
```

### 직렬화의 해결

```
직렬화 전 (메모리 주소 기반)
│ email → 0x8B2C (포인터)  │  ← 서버 A에서만 유효

직렬화 후 (실제 값으로 변환)
│ { userId: 1, email: "user@example.com" } │  ← 어디서든 읽을 수 있음
```

**직렬화 = "주소" 대신 "실제 값"으로 바꿔서 보내는 것**

---

## 사용 사례

| 용도 | 설명 |
|------|------|
| 세션 저장 | Redis에 사용자 정보 저장 |
| 세션 클러스터링 | 다중 서버 간 세션 공유 |
| 캐시 | 객체를 파일/DB에 저장 |
| 네트워크 전송 | 서버 간 객체 주고받기 |

---

## Serializable 인터페이스

```java
public class CustomUserPrincipal implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String email;
}
```

- **마커 인터페이스**: 메서드 없음, "이 객체는 직렬화 가능" 표시
- 구현하지 않으면 직렬화 시 `NotSerializableException` 발생

---

## serialVersionUID

### 역할

**클래스 버전 번호** - 직렬화/역직렬화 시 버전 호환성 체크

### 동작 방식

```
저장된 바이트 데이터
┌─────────────────────────────┐
│ serialVersionUID: 1L        │ ← 저장할 때 같이 들어감
│ userId: 1                   │
│ email: "user@example.com"   │
└─────────────────────────────┘
              ↓
         역직렬화 시도
              ↓
JVM: "저장된 UID(1L) == 현재 클래스 UID(1L)?"
              ↓
         ✅ 일치 → 복원
         ❌ 불일치 → InvalidClassException
```

### 선언 방식

| 방식 | 설명 |
|------|------|
| 명시적 선언 | `private static final long serialVersionUID = 1L;` |
| 선언 안 함 | JVM이 클래스 구조 기반으로 자동 계산 |

**주의:** 자동 계산 시 필드 하나만 바꿔도 값이 달라져서 기존 세션 무효화됨

### 클래스 정보 접근 원리

serialVersionUID는 **static 필드**라 객체 인스턴스에 없지만, 직렬화 시 리플렉션으로 읽어옴:

```java
// ObjectOutputStream 내부 (간략화)
void writeObject(Object obj) {
    Class<?> clazz = obj.getClass();  // 객체에서 클래스 정보 획득
    Field field = clazz.getDeclaredField("serialVersionUID");
    long uid = field.getLong(null);   // static이라 null로 접근

    writeClassDescriptor(clazz.getName(), uid);
    writeFields(obj);
}
```

---

## @Serial 어노테이션

```java
@Serial
private static final long serialVersionUID = 1L;
```

| 역할 | 설명 |
|------|------|
| 컴파일러 검증 | 직렬화 필드/메서드가 올바른 형태인지 체크 |
| IDE 지원 | "직렬화 관련 코드"임을 인식 |
| 가독성 | 개발자에게 용도 명시 |

**Java 14+에서 추가된 선택적 어노테이션** - 없어도 동작하지만 실수 방지용

---

## 정리

| 환경 | 직렬화 필요? | 이유 |
|------|-------------|------|
| 같은 서버 메모리 | ❌ | 주소가 유효함 |
| Redis 저장 | ✅ | 별도 프로세스, 주소 무의미 |
| 다른 서버 전송 | ✅ | 완전히 다른 메모리 공간 |
| 파일 저장 | ✅ | 재시작하면 주소 바뀜 |
