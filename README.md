# Pika Player

안드로이드 동영상 플레이어. 기기에 있는 파일을 재생하는 올포맷 플레이어이고,
광고 없이 Pro 인앱결제로만 수익을 냅니다. Kotlin · Jetpack Compose · Media3.

## 지금 상태

Phase 1(로컬 재생 전체)과 Phase 2 일부(결제 뼈대 · PiP)까지 코드가 들어와 있습니다.

**다만 `:app` 모듈은 아직 한 번도 빌드된 적이 없습니다.** Android SDK 와 Google Maven 에
접근할 수 없는 환경에서 작성됐기 때문입니다. Android Studio 로 열어 동기화하는 것이
다음 할 일이고, 무엇이 검증됐고 무엇이 안 됐는지는 [개발 노트](docs/development.md)
맨 앞에 정리돼 있습니다.

## 시작하기

macOS · Windows · Linux 모두 됩니다. Android Studio 최신 안정판이면 충분합니다.

**JDK 는 21(안드로이드 스튜디오 번들 JBR)을 그대로 쓰면 됩니다.** 따로 설치할 필요 없습니다.
Settings → Build Tools → Gradle → *Gradle JDK* 가 번들 JBR 을 가리키면 그대로 두세요.
JDK 21 로 컴파일하되 결과물은 Java 17 바이트코드로 나오도록 맞춰 뒀습니다 — 안드로이드가
요구하는 것은 바이트코드 수준이지 컴파일에 쓴 JDK 가 아닙니다.

```
./gradlew :subtitle:test :entitlement:test :vault:test   # SDK 없이도 도는 테스트
./gradlew :app:assembleDebug                 # Android SDK 필요
python3 tools/check_refs.py                  # 빌드 없이 도는 참조 점검
```

Apple Silicon 맥이면 에뮬레이터 이미지를 **arm64-v8a** 로 받으세요. 결제까지 확인하려면
**Google Play** 가 포함된 이미지여야 합니다.

에뮬레이터에는 영상이 없어 보관함이 비어 보입니다. 창에 파일을 끌어다 놓거나
`adb push 영상.mp4 /sdcard/Movies/` 로 넣으세요. 자막은 **같은 폴더에 같은 이름**이어야
자동으로 붙습니다.

## 구조

| 모듈 | 내용 |
|---|---|
| `:app` | 화면과 안드로이드 연동 전부 |
| `:subtitle` | 자막 파싱·인코딩 판별 (순수 Kotlin, 테스트 23개) |
| `:entitlement` | 결제 등급과 기능 게이팅 (순수 Kotlin, 테스트 12개) |
| `:vault` | 비공개 폴더 PIN·잠금 정책 (순수 Kotlin, 테스트 16개) |

깨지기 쉽거나 틀렸을 때 값이 큰 로직은 안드로이드 의존성 없는 모듈로 빼서
기기 없이 테스트로 못박아 뒀습니다.

## 문서

- [기획서](docs/mobile-video-player-spec.md) — 리서치, 기능 층위, 수익 구조, 개발 범위, 디자인 토큰
- [개발 노트](docs/development.md) — 구조, 설계 판단 근거, 검토 이력, 남은 작업
- `design/` — 화면 시안 아트보드
