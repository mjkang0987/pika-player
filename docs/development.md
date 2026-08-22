# 개발 노트 (Phase 1)

## 확인되지 않은 것 — 먼저 읽어주세요

이 골격은 **Android SDK가 없고 Google Maven에 접근할 수 없는 환경에서 작성**됐습니다. 따라서:

- **`:app` 모듈은 빌드해 본 적이 없습니다.** 컴파일 오류가 남아 있을 수 있습니다.
- 반면 **`:subtitle` 모듈은 실제로 컴파일하고 테스트를 돌려 16개 전부 통과했습니다.** Android 의존성이 없는 순수 Kotlin이라 SDK 없이 검증이 가능했습니다.
- **`gradle/libs.versions.toml`의 버전은 검증되지 않았습니다.** Kotlin 버전만 Maven Central에서 확인했고, AGP·Compose·Media3·Room 버전은 추정값입니다. Android Studio에서 열면 동기화 단계에서 바로 걸러집니다.

첫 작업은 Android Studio로 열어 **동기화 → 버전 갱신 → 빌드**입니다.

## 구조

```
subtitle/                 순수 Kotlin 모듈 (Android 의존성 없음)
  EncodingDetector.kt     자막 파일 인코딩 판별
  SmiParser.kt            SAMI(.smi) 파서
  SrtParser.kt            SubRip(.srt) 파서
  SubtitleLoader.kt       바이트 → 판별 → 파싱
  SubtitleTrack.kt        재생 위치로 자막 조회 (이진 탐색 + 싱크 오프셋)

app/src/main/java/com/pikaworks/pikaplayer/
  PikaApp.kt            의존성 조립 (DI 프레임워크 없이 손으로)
  MainActivity.kt       진입점, 권한 요청
  core/
    FeatureGate.kt      Pro 게이팅 지점 — Phase 1은 항상 허용
  data/
    media/              MediaStore 조회, VideoItem 모델
    db/                 Room — 재생 위치(이어보기)
    prefs/              DataStore — 설정 화면 값
  data/subtitle/        영상 옆 자막 파일 찾기 + 읽기 (Android 쪽)
  ui/
    theme/              기획서 7.4 디자인 토큰
    library/            라이브러리 화면(S1)
    player/             플레이어 화면(S3)
    Format.kt           재생시간·용량·남은시간 표기
```

## 설계 메모

**색 토큰은 `ui/theme/Color.kt`가 유일한 출처입니다.** 화면에서 색을 직접 쓰지 말고 `PikaTheme.colors.key` 처럼 참조하세요.

`PikaColors.onMediaKey` / `onMediaText` / `onMediaTrack` 은 **테마와 무관하게 고정**입니다. 썸네일과 영상 위에 얹히는 요소는 항상 어두운 배경 위에 놓이므로, 라이트 테마 값을 쓰면 묻혀서 안 보입니다.

**진행 바 높이는 모서리 반경과 같게 유지하세요.** 바가 반경보다 얇으면 클리핑 곡선이 바를 대각선으로 잘라내 끝이 쐐기 모양이 됩니다.

**`FeatureGate`는 Phase 1에서 쓸 일이 없어 보여도 지금 있어야 합니다.** Phase 2에서 결제를 붙일 때 구현체만 갈아끼우면 되고, 없으면 여러 화면에 흩어진 분기를 일일이 찾아 심어야 합니다.

## 자막 처리 — 왜 직접 만들었나

`.smi`(SAMI)는 국내 자막 파일에서 비중이 큰데 재생 엔진이 기본 지원하지 않을 수 있습니다. 지원 여부 확인을 기다리는 대신, **의존하지 않아도 되도록 파서를 직접 만들었습니다.** 텍스트 파싱이라 분량이 크지 않고, 엔진 지원 여부와 무관하게 동작합니다.

인코딩 판별에서 주의할 점 하나를 테스트로 못박아 뒀습니다: **"유효한 UTF-8인가"만 검사하면 안 됩니다.** CP949 한글 바이트쌍이 우연히 유효한 UTF-8 시퀀스가 되는 경우가 있어서(예: `한` = `0xC7 0xD1`), 검사를 통과했다고 UTF-8이라 단정하면 짧은 자막이 통째로 깨집니다. 그래서 디코딩 결과가 깨진 글자처럼 보이는지까지 확인합니다.

`EncodingDetector.korean`은 플랫폼마다 다른 인코딩 이름을 순서대로 시도합니다. 마지막 후보인 EUC-KR은 CP949의 부분집합이라 확장 한글이 일부 깨질 수 있지만, 앞 후보가 하나라도 있으면 문제되지 않습니다.

테스트 실행: `./gradlew :subtitle:test`

## Phase 1 남은 작업

우선순위 순:

1. **플레이어 컨트롤 아이콘** — 지금은 동작 확인용 텍스트(`−10`, `▶`)다. 시안대로 벡터 아이콘으로 교체
2. **제스처** — 좌우 스와이프(탐색), 상하(밝기/볼륨), 더블탭 10초
3. **전체화면(가로) 전환** — 자동회전 연동 포함
4. **다음 영상 목록** — 플레이어 하단, 같은 폴더
5. **라이브러리 목록의 자막 배지** — `SubtitleMatcher` 를 목록에도 연결해 `LibraryRow.subtitleFormat` 채우기
6. 권한 온보딩(S5) — 현재는 진입 즉시 요청하는 임시 처리. **거부 시 SAF 우회로가 반드시 필요**
5. 폴더 탐색(S2) — SAF 기반
6. 설정 화면(S6) — `SettingsStore`는 이미 있음, 화면만 연결
7. 저장소 사용량 표시 — `StatFs`
8. 자막 파일 자동 매칭 — `LibraryRow.subtitleFormat` 채우기

## 참고

- 화면 시안: `design/` 디렉터리와 캔버스 아트보드
- 기획서: `docs/mobile-video-player-spec.md`
