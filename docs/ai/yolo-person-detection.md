# YOLOv8을 활용한 이미지 내 사람 탐지

## 개요

무신사 등 패션 플랫폼에서 크롤링한 이미지 중 **모델(사람)이 없는 이미지만 필터링**하기 위한 가이드.

## 배경

- 무신사는 대부분 모델 착용샷을 사용
- 크림(KREAM) 스타일의 누끼 이미지(제품만 있는 이미지)를 원할 경우
- AI로 사람 유무를 판별하여 필터링 필요

## 모델 비교

| 모델 | 속도 | 정확도 | 사용 난이도 | 특징 |
|------|------|--------|-------------|------|
| **YOLOv8** | 빠름 | 높음 | 쉬움 | 가장 범용적, 권장 |
| DETR | 중간 | 높음 | 쉬움 | HuggingFace 지원 |
| YOLOv5 | 빠름 | 중간 | 쉬움 | 안정적 |
| MediaPipe | 매우 빠름 | 중간 | 쉬움 | 모바일 최적화 |

## YOLOv8 모델 크기별 비교

| 모델 | 파일 크기 | 속도 | 정확도 | 용도 |
|------|----------|------|--------|------|
| `yolov8n.pt` | 6MB | 가장 빠름 | 보통 | 대량 처리 |
| `yolov8s.pt` | 22MB | 빠름 | 좋음 | 일반 사용 |
| `yolov8m.pt` | 52MB | 중간 | 높음 | 균형 |
| `yolov8l.pt` | 87MB | 느림 | 매우 높음 | 정확도 우선 |

## 설치

```bash
pip install ultralytics
```

## 기본 사용법

```python
from ultralytics import YOLO

# 모델 로드 (최초 실행 시 자동 다운로드)
model = YOLO("yolov8n.pt")

# 이미지에서 객체 탐지
results = model("image.jpg")

# 결과 확인
for box in results[0].boxes:
    class_id = int(box.cls)
    confidence = float(box.conf)
    print(f"클래스: {class_id}, 신뢰도: {confidence:.2f}")
```

## 사람 탐지 함수

```python
from ultralytics import YOLO

model = YOLO("yolov8n.pt")

def has_person(image_path: str, confidence_threshold: float = 0.5) -> bool:
    """
    이미지에 사람이 있는지 확인

    Args:
        image_path: 이미지 파일 경로
        confidence_threshold: 신뢰도 임계값 (기본 0.5)

    Returns:
        bool: 사람이 있으면 True
    """
    results = model(image_path, verbose=False)

    for box in results[0].boxes:
        # 0 = person 클래스 (COCO 데이터셋 기준)
        if int(box.cls) == 0 and float(box.conf) >= confidence_threshold:
            return True
    return False
```

## 사람 없는 이미지 필터링 스크립트

```python
from ultralytics import YOLO
from pathlib import Path
import shutil

model = YOLO("yolov8n.pt")

def has_person(image_path: str, confidence_threshold: float = 0.5) -> bool:
    """이미지에 사람이 있는지 확인"""
    results = model(image_path, verbose=False)
    for box in results[0].boxes:
        if int(box.cls) == 0 and float(box.conf) >= confidence_threshold:
            return True
    return False

def filter_images_without_person(input_dir: str, output_dir: str):
    """
    사람이 없는 이미지만 추출하여 복사

    Args:
        input_dir: 원본 이미지 디렉토리
        output_dir: 필터링된 이미지 저장 디렉토리
    """
    input_path = Path(input_dir)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    image_extensions = ["*.jpg", "*.jpeg", "*.png", "*.webp"]

    for ext in image_extensions:
        for img in input_path.glob(ext):
            if not has_person(str(img)):
                shutil.copy(img, output_path / img.name)
                print(f"[복사] {img.name}")
            else:
                print(f"[스킵] {img.name} (사람 감지됨)")

# 실행 예시
if __name__ == "__main__":
    filter_images_without_person(
        input_dir="./crawled_images",
        output_dir="./filtered_images"
    )
```

## 배치 처리 (대량 이미지)

```python
from ultralytics import YOLO
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import json

model = YOLO("yolov8n.pt")

def analyze_image(image_path: str) -> dict:
    """단일 이미지 분석"""
    results = model(image_path, verbose=False)
    has_person = any(
        int(box.cls) == 0 and float(box.conf) >= 0.5
        for box in results[0].boxes
    )
    return {
        "path": image_path,
        "has_person": has_person
    }

def batch_analyze(input_dir: str, max_workers: int = 4) -> list:
    """
    멀티스레드로 대량 이미지 분석

    Args:
        input_dir: 이미지 디렉토리
        max_workers: 동시 처리 스레드 수

    Returns:
        list: 분석 결과 리스트
    """
    image_paths = list(Path(input_dir).glob("*.jpg"))

    with ThreadPoolExecutor(max_workers=max_workers) as executor:
        results = list(executor.map(
            lambda p: analyze_image(str(p)),
            image_paths
        ))

    return results

# 실행
results = batch_analyze("./crawled_images")
no_person_images = [r["path"] for r in results if not r["has_person"]]
print(f"사람 없는 이미지: {len(no_person_images)}개")
```

## COCO 클래스 ID 참고

YOLOv8은 COCO 데이터셋으로 학습되어 80개 클래스를 탐지합니다.

| ID | 클래스 | ID | 클래스 |
|----|--------|----|----|
| 0 | person | 1 | bicycle |
| 2 | car | 3 | motorcycle |
| 24 | backpack | 25 | umbrella |
| 26 | handbag | 27 | tie |

패션 관련 객체도 탐지 가능하므로, 필요에 따라 활용할 수 있습니다.

## 주의사항

1. **마네킹**: 사람으로 인식될 수 있음 (신뢰도 조정 필요)
2. **부분 노출**: 손, 발만 보이는 경우 탐지 안 될 수 있음
3. **GPU 권장**: 대량 처리 시 GPU 사용 시 10배 이상 빠름

## 참고 자료

- [Ultralytics YOLOv8 공식 문서](https://docs.ultralytics.com/)
- [COCO 데이터셋 클래스 목록](https://cocodataset.org/#detection-eval)
