# Claude Skills 가이드

Claude Code의 Skills 기능을 활용하여 개발 워크플로우를 자동화하는 방법을 정리한 문서입니다.

## 목차

1. [Skills 개요](./01-overview.md) - Skills란 무엇인가
2. [시작하기](./02-getting-started.md) - 단계별 스킬 생성 가이드
3. [실전 예제](./03-examples.md) - 다양한 스킬 예제
4. [프로젝트 스킬](./04-project-skills.md) - 이 프로젝트에서 사용하는 스킬
5. [IPZY Skills 사용법](./05-ipzy-skills-usage.md) - 설치된 6개 스킬 상세 가이드

## 설치된 IPZY Skills (6개)

| 스킬 | 용도 | 트리거 예시 |
|------|------|-------------|
| `ipzy-domain-generator` | 새 도메인 패키지 생성 | "coupon 도메인 만들어줘" |
| `ipzy-api-endpoint` | API 엔드포인트 추가 | "검색 API 추가해줘" |
| `ipzy-test-generator` | 테스트 코드 생성 | "UserService 테스트 만들어줘" |
| `ipzy-code-reviewer` | 코드 품질 검토 | "코드 리뷰해줘" |
| `ipzy-exception-handler` | ErrorCode/Exception 추가 | "새 에러코드 추가해줘" |
| `ipzy-dto-generator` | DTO 생성 | "Response DTO 만들어줘" |

## 빠른 시작

```bash
# 개인 스킬 생성
mkdir -p ~/.claude/skills/my-skill
touch ~/.claude/skills/my-skill/SKILL.md

# 프로젝트 스킬 생성
mkdir -p .claude/skills/my-skill
touch .claude/skills/my-skill/SKILL.md
```

## 참고 자료

- [Claude Code 공식 문서](https://docs.anthropic.com/claude-code)
- [영상: Claude Skills 활용법 (앵프렌)](https://www.youtube.com/watch?v=6Rayxu3rZOU)
