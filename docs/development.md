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
    media/              MediaStore 조회, SAF 폴더 조회, VideoItem 모델
    db/                 Room — 재생 위치(이어보기)
    prefs/              DataStore — 설정 화면 값
  data/subtitle/        영상 옆 자막 파일 찾기 + 읽기 (Android 쪽)
  ui/
    theme/              기획서 7.4 디자인 토큰
    library/            라이브러리 화면(S1)
    folder/             폴더 탐색(S2)
    permission/         권한 온보딩(S5)
    recent/             최근 탭
    settings/           설정 화면(S6)
    player/             플레이어 화면(S3)
      PlayerIcons.kt      시안 SVG 패스를 옮긴 벡터 아이콘
      PlayerGestures.kt   스와이프 탐색 / 밝기 · 볼륨 / 더블탭
      SystemControls.kt   밝기 · 볼륨 · 화면 켜둠
      PlayerOrientation.kt 방향 정책 · 몰입 모드
    Format.kt           재생시간·용량·남은시간 표기
    VideoListRow.kt     목록 한 줄 (보관함·폴더·최근 공용)
    BottomNav.kt        하단 네비게이션
    AppIcons.kt         시안 SVG 패스를 옮긴 벡터 아이콘
```

## 설계 메모

**색 토큰은 `ui/theme/Color.kt`가 유일한 출처입니다.** 화면에서 색을 직접 쓰지 말고 `PikaTheme.colors.key` 처럼 참조하세요.

`PikaColors.onMediaKey` / `onMediaText` / `onMediaTrack` 은 **테마와 무관하게 고정**입니다. 썸네일과 영상 위에 얹히는 요소는 항상 어두운 배경 위에 놓이므로, 라이트 테마 값을 쓰면 묻혀서 안 보입니다.

**진행 바 높이는 모서리 반경과 같게 유지하세요.** 바가 반경보다 얇으면 클리핑 곡선이 바를 대각선으로 잘라내 끝이 쐐기 모양이 됩니다.

**권한을 거부해도 앱을 쓸 수 있어야 합니다.** 저장소 권한은 거절률이 높아서, 거부 = 막다른 길이면 그 자리에서 이탈합니다. SAF 로 폴더를 직접 고르는 경로(`SafFolderSource`)가 그래서 있습니다.

**제스처 방향은 드래그 시작 직후 한 번만 정합니다.** 매 프레임 다시 판단하면 대각선으로 움직일 때 탐색과 볼륨이 번갈아 걸립니다.

**전체화면은 별도 상태가 아니라 가로 방향 그 자체입니다.** 따로 관리하면 기기를 돌렸을 때와 버튼을 눌렀을 때가 어긋납니다. `Configuration.orientation` 에서 읽습니다.

**앱 내 잠금이 시스템 자동회전보다 우선입니다.** 누워서 보는 사용자는 자동회전이 켜져 있어도 화면이 도는 걸 싫어합니다.

**영상 표면은 Media3 `PlayerView` 를 씁니다** (`useController = false`). 컨트롤과 자막은 우리가 그리지만, 화면비 처리와 표면 생명주기까지 직접 다루면 실수하기 쉽습니다.

**밝기는 창에만 적용합니다.** 기기 전체 밝기를 바꾸면 앱을 나간 뒤에도 어두운 채로 남습니다.

**`FeatureGate`는 Phase 1에서 쓸 일이 없어 보여도 지금 있어야 합니다.** Phase 2에서 결제를 붙일 때 구현체만 갈아끼우면 되고, 없으면 여러 화면에 흩어진 분기를 일일이 찾아 심어야 합니다.

## 검토 이력

`:app` 은 빌드할 수 없어 눈으로 훑는 것 외에 검증 수단이 없다. 아래는 그렇게 찾아 고친 것들이다. 같은 종류의 실수를 반복하지 않도록 남겨둔다.

| 문제 | 증상 |
|---|---|
| 영상마다 `viewModel(key = uri)` 로 PlayerViewModel 생성 | ExoPlayer 인스턴스가 쌓임. 코덱은 기기당 개수 제한이 있어 몇 개만 새도 재생 실패 |
| Coil ImageLoader 를 등록한 적이 없음 | 썸네일이 하나도 뜨지 않음. 기본 ImageLoader 는 동영상 프레임을 못 뽑는다 |
| 권한 상태를 진입 시 한 번만 확인 | 시스템 설정에서 권한을 켜고 돌아와도 계속 막힌 화면 |
| `PlayerIcons` 를 설정·권한 화면이 참조 | 화면 계층이 player 패키지에 의존. `ui/AppIcons` 로 이동 |
| 권한 화면 대표 아이콘이 자막 아이콘 | 화면 의미와 다른 그림 |
| 전체화면 시크바 이중 패딩 | 가로에서 좌우 여백이 44dp 로 벌어짐 |
| 보관함·폴더 화면이 목록 행을 각자 구현 | 같은 모양을 두 번 유지해야 함 → `ui/VideoListRow` 로 통합 |

## 자막 처리 — 왜 직접 만들었나

`.smi`(SAMI)는 국내 자막 파일에서 비중이 큰데 재생 엔진이 기본 지원하지 않을 수 있습니다. 지원 여부 확인을 기다리는 대신, **의존하지 않아도 되도록 파서를 직접 만들었습니다.** 텍스트 파싱이라 분량이 크지 않고, 엔진 지원 여부와 무관하게 동작합니다.

인코딩 판별에서 주의할 점 하나를 테스트로 못박아 뒀습니다: **"유효한 UTF-8인가"만 검사하면 안 됩니다.** CP949 한글 바이트쌍이 우연히 유효한 UTF-8 시퀀스가 되는 경우가 있어서(예: `한` = `0xC7 0xD1`), 검사를 통과했다고 UTF-8이라 단정하면 짧은 자막이 통째로 깨집니다. 그래서 디코딩 결과가 깨진 글자처럼 보이는지까지 확인합니다.

`EncodingDetector.korean`은 플랫폼마다 다른 인코딩 이름을 순서대로 시도합니다. 마지막 후보인 EUC-KR은 CP949의 부분집합이라 확장 한글이 일부 깨질 수 있지만, 앞 후보가 하나라도 있으면 문제되지 않습니다.

테스트 실행: `./gradlew :subtitle:test`

## Phase 1 남은 작업

우선순위 순:

1. **다음 영상 목록** — 플레이어 하단, 같은 폴더
2. **라이브러리 목록의 자막 배지** — `SubtitleMatcher` 를 목록에도 연결해 `LibraryRow.subtitleFormat` 채우기
3. **자막 설정 시트(S4)** — 인코딩 강제 지정을 화면에 연결. `SubtitleMatcher.load(forcedCharsetName)` 은 이미 받는다
4. **설정 화면의 미완 항목** — 재생속도 · 인코딩 · 글자 크기 · 표시 위치 · 테마 선택 시트
5. **SAF 폴더 탐색** — 폴더 화면은 MediaStore 기준으로만 동작한다. SAF 로 고른 폴더를 쓰는 사용자는 폴더 탭이 비어 보인다
6. **SAF 메타데이터 캐시** — 폴더를 열 때마다 파일마다 `MediaMetadataRetriever` 를 돌린다. Room 에 캐시할 것
7. **검색(S7)** — P2 — 현재는 진입 즉시 요청하는 임시 처리. **거부 시 SAF 우회로가 반드시 필요**
5. 폴더 탐색(S2) — SAF 기반
6. 설정 화면(S6) — `SettingsStore`는 이미 있음, 화면만 연결
7. 저장소 사용량 표시 — `StatFs`
8. 자막 파일 자동 매칭 — `LibraryRow.subtitleFormat` 채우기

## 참고

- 화면 시안: `design/` 디렉터리와 캔버스 아트보드
- 기획서: `docs/mobile-video-player-spec.md`
