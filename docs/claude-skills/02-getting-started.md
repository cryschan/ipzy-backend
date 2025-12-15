# 시작하기: 단계별 스킬 생성 가이드

## 기본 구조

```yaml
---
name: skill-name
description: 스킬이 무엇을 하는지, 언제 사용하는지 설명
---

# Skill 제목

## Instructions
단계별 지시사항

## Examples
구체적인 사용 예제
```

## YAML 필드 설명

```yaml
---
# 필수: 소문자, 숫자, 하이픈만 사용 (최대 64자)
name: my-skill-name

# 필수: Skill 설명과 사용 시점 (최대 1024자)
description: PDF 파일에서 텍스트 추출. PDF 파일 작업 시 사용.

# 선택: 사용 가능한 도구 제한 (보안)
allowed-tools: Read, Grep, Glob
---
```

## Description 작성 팁 (중요!)

Claude가 Skill을 발견할 수 있도록 **"무엇을 하는가" + "언제 사용하는가"**를 함께 작성:

```yaml
# 나쁜 예
description: 데이터 작업 도우미

# 좋은 예
description: Excel 스프레드시트 분석, 피벗테이블 생성. Excel 파일이나 .xlsx 형식 데이터 분석 시 사용.
```

---

## 예제 1: 커밋 메시지 생성기

### Step 1: 스킬 폴더 생성

```bash
mkdir -p ~/.claude/skills/commit-helper
```

### Step 2: SKILL.md 파일 생성

```bash
touch ~/.claude/skills/commit-helper/SKILL.md
```

### Step 3: 내용 작성

**`~/.claude/skills/commit-helper/SKILL.md`**

```yaml
---
name: commit-helper
description: Git 커밋 메시지 자동 생성. 커밋 작성, 커밋 메시지 시 사용.
---

# 커밋 메시지 생성기

## Instructions
1. `git diff --staged` 실행하여 변경사항 확인
2. 변경 내용 분석
3. Conventional Commits 형식으로 메시지 제안

## Format
- 50자 이하 제목
- 현재형 사용 (Add, Fix, Update)
- 타입: feat, fix, docs, style, refactor, test, chore

## Example
```
feat(auth): add login validation

- Add email format validation
- Add password strength check

Closes #123
```
```

### Step 4: Claude Code 재시작

터미널에서 Claude Code를 재시작하거나 새 세션을 시작합니다.

### Step 5: 테스트

Claude Code에서 다음과 같이 질문:

```
"커밋 메시지 만들어줘"
```

Claude가 자동으로 `commit-helper` 스킬을 인식하고 사용합니다.

---

## 예제 2: 코드 리뷰어 (프로젝트 스킬)

### Step 1: 프로젝트 스킬 폴더 생성

```bash
# 프로젝트 루트에서 실행
mkdir -p .claude/skills/code-reviewer
```

### Step 2: SKILL.md 작성

**`.claude/skills/code-reviewer/SKILL.md`**

```yaml
---
name: code-reviewer
description: 코드 품질 및 보안 검토. 코드 리뷰, PR 검토, 품질 분석 시 사용.
allowed-tools: Read, Grep, Glob
---

# 코드 리뷰어

## 검토 항목

### 1. 코드 품질
- 단일 책임 원칙
- 중복 코드
- 네이밍 컨벤션

### 2. 보안
- SQL 인젝션
- XSS 취약점
- 하드코딩된 시크릿

### 3. 성능
- N+1 쿼리
- 불필요한 렌더링
- 메모리 누수

## Output Format
```markdown
## 리뷰 결과

### 잘된 점
-

### 개선 권장
- 파일:라인 - 설명

### 필수 수정
- 파일:라인 - 설명
```
```

### Step 3: Git에 커밋 (팀 공유)

```bash
git add .claude/skills/code-reviewer/
git commit -m "Add code-reviewer skill"
git push
```

### Step 4: 테스트

```
"이 PR 코드 리뷰해줘"
```

---

## 예제 3: 여러 파일로 구성된 스킬

### Step 1: 폴더 구조 생성

```bash
mkdir -p ~/.claude/skills/api-generator
mkdir -p ~/.claude/skills/api-generator/templates
```

### Step 2: 메인 SKILL.md

**`~/.claude/skills/api-generator/SKILL.md`**

```yaml
---
name: api-generator
description: REST API 엔드포인트 생성. API 추가, 엔드포인트 생성 시 사용.
---

# API 생성기

## Instructions
1. 리소스명 확인
2. CRUD 엔드포인트 생성
3. 타입 정의 추가
4. 에러 핸들링 포함

## Reference
- [템플릿](templates/route.md)
- [에러 처리](templates/error.md)
```

### Step 3: 템플릿 파일 추가

**`~/.claude/skills/api-generator/templates/route.md`**

```markdown
# Route 템플릿

## GET (목록)
```typescript
export async function GET(req: Request) {
  try {
    const items = await db.resource.findMany();
    return Response.json({ success: true, data: items });
  } catch (error) {
    return Response.json(
      { success: false, error: '조회 실패' },
      { status: 500 }
    );
  }
}
```

## POST (생성)
```typescript
export async function POST(req: Request) {
  try {
    const body = await req.json();
    const item = await db.resource.create({ data: body });
    return Response.json({ success: true, data: item }, { status: 201 });
  } catch (error) {
    return Response.json(
      { success: false, error: '생성 실패' },
      { status: 500 }
    );
  }
}
```
```

### Step 4: 테스트

```
"User API 엔드포인트 만들어줘"
```

---

## 스킬 생성 흐름 요약

```
1. 폴더 생성
   └── mkdir -p ~/.claude/skills/스킬명

2. SKILL.md 작성
   └── YAML 헤더 (name, description)
   └── 마크다운 본문 (Instructions, Examples)

3. (선택) 추가 파일
   └── reference.md, templates/, scripts/

4. Claude Code 재시작

5. 테스트
   └── description에 있는 키워드로 질문
```

## 문제 해결

### Claude가 Skill을 사용하지 않음

**원인 1: description이 너무 모호**
```yaml
# 나쁜 예
description: 파일 처리

# 좋은 예
description: Excel 스프레드시트 분석. Excel 파일이나 .xlsx 형식 데이터 작업 시 사용.
```

**원인 2: 파일 경로 오류**
```bash
# 개인 스킬
~/.claude/skills/skill-name/SKILL.md

# 프로젝트 스킬
.claude/skills/skill-name/SKILL.md
```

**원인 3: YAML 문법 오류**
- 첫 줄이 `---` 인지 확인
- closing `---` 이 있는지 확인
- 탭 대신 스페이스 사용 (YAML은 탭 금지)
