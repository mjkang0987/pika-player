# plan.md

## 진행 중(2026-11-12): Google Play 첫 출시 1.0.0

### 된 것
- 버전 1.0.0(versionCode 3)으로 올리고 dev 에 병합했다. PR #2 로 main 에 머지했다.
- `:app:bundleRelease` 로 서명된 AAB 를 만들었고, 출시용 APK 를 에뮬레이터에서 실행해 첫 화면까지 확인했다.

### 남은 것
- 2026-11-12 Play Console(조직 계정) 프로덕션 1.0.0 제출, 검토 중. 177개 국가, 관리형 게시 꺼짐(승인 즉시 공개).
- 콘솔에서 낸 선언: 사진·동영상 권한(READ_MEDIA_VIDEO, 동영상 플레이어 핵심 기능), 광고 ID 사용 안 함, 타겟층 18세 이상.
- 영어(en-US) 스토어 등록정보는 저장만 해 둠. 1.0.0 승인 뒤 게시 개요에서 제출.
- 다음 업데이트 때 `ndk { debugSymbolLevel = "FULL" }` 추가(디버그 기호 경고).
- 출시용 빌드로 비공개 폴더·PIN·영상 삭제 수동 확인.

---

## 완료(2026-11-03): 앱 언어 선택 (한국어 · 영어 · 일본어 · 중국어 간체)

### 결과
- `feature/i18n-strings` → `feature/i18n-ja-zh` → `feature/language-selector` 순서로 `dev` 에 병합했다.
- 화면 문구 209개를 `res/values*/strings_{main,settings,player,library,vault}.xml` 로 옮겼다. 네 언어의 키 목록과 자리표시자가 같다.
- 언어는 설정 → 화면 → 언어에서 고른다. 안드로이드 13 이상에서는 기기 설정의 앱 언어 목록에도 뜬다.
- 구현하며 바뀐 것:
  - 영어 플레이어 하단 라벨은 칸이 좁아 "Full" 처럼 짧게 썼다.
  - 언어를 바꾸면 화면이 새로 만들어져서, 탭 위치를 `rememberSaveable` 로 바꿨다. 보관함으로 튕기지 않는다.
  - 이름 없는 폴더 묶음("기타")을 null 로 다루면서, 열면 빈 목록이 나오던 버그도 같이 고쳐졌다.
  - 선택 시트(`OptionSheet`) 항목에 라디오 버튼 접근성 정보를 붙였다.

### 남은 것
- 일본어·중국어 문구는 원어민 검수 전 초안이다.
- 안드로이드 12 이하에서 고른 언어가 유지되는지 기기로 확인하지 못했다. API 37 에뮬레이터만 있다.
- 기기 설정에서 언어를 바꾸면 재생 중이던 화면이 닫힌다. 재생 상태가 `remember` 라서 생기는, 원래 있던 동작이다.
- 언어 이름(日本語 등)에 언어 정보를 붙이지 않아, TalkBack 이 지금 화면 언어의 음성으로 읽을 수 있다.
- `:app:lintDebug` 가 `PlayerScreen.kt` 의 media3 `@UnstableApi` 때문에 실패한다. 이번 작업 전부터 있던 문제다.

---

아래는 작업 전에 세운 계획이다.

### 요구사항
- 앱 화면 글자를 4개 언어로 보여준다.
- 앱 설정 화면에서 언어를 고를 수 있다. 안드로이드 13 이상에서는 기기 설정 → 앱 → 언어에서도 고를 수 있고, 두 곳이 같은 값을 쓴다.
- 기본값은 "시스템 언어 따르기"로 한다. 지원하지 않는 언어로 설정된 기기에서는 영어가 나온다.

### 지금 상태
- `res/values/strings.xml` 에는 `app_name` 하나만 있다.
- 화면 글자는 Kotlin 파일 42개에 한국어로 직접 적혀 있다. 주석·로그를 빼면 236곳이고, 서로 다른 문구는 약 191개다. 이 중 약 20개는 중간에 값이 들어간다(`"${n}개"` 같은 문구).
- `MainActivity` 는 `ComponentActivity`, 테마 부모는 `android:Theme.Material.NoActionBar` 다. AppCompat 의존성은 없다.

### 구현 방식
1. **문자열 꺼내기**: 화면 문구를 `strings.xml` 로 옮긴다.
   - `values/` = 영어(기본 언어), `values-ko/` = 한국어, `values-ja/`, `values-zh-rCN/`.
   - 컴포저블에서는 `stringResource`, 값이 들어가는 문구는 `%1$d` · `%1$s` 형식으로 바꾼다. 개수에 따라 형태가 바뀌는 영어 문구는 `plurals` 로 처리한다.
   - ViewModel·data 계층에 있는 문구(`LibraryViewModel` 의 배지 문구 등)는 문자열 대신 `@StringRes` id 를 넘기고, 화면에서 글자로 바꾼다.
   - 로그(`Log.*`)·주석·파일 이름 매칭에 쓰는 문자열은 옮기지 않는다.
2. **언어 전환**: AppCompat 의 `AppCompatDelegate.setApplicationLocales` 를 쓴다.
   - `androidx.appcompat:appcompat` 의존성을 추가하고, `MainActivity` 를 `AppCompatActivity` 로 바꾼다. 테마 부모를 `Theme.AppCompat.NoActionBar` 계열로 바꾸되 기존 항목(창 배경·투명 상태바·내비게이션바)은 그대로 둔다.
   - 안드로이드 12 이하에서도 선택을 기억하도록 매니페스트에 `AppLocalesMetadataHolderService`(`autoStoreLocales=true`)를 추가한다.
   - `android { androidResources { generateLocaleConfig = true } }` 와 `res/resources.properties` 를 추가해서 안드로이드 13 이상 기기 설정의 앱 언어 목록에 나오게 한다.
   - AppCompat 을 쓰는 이유: 안드로이드 12 이하까지 직접 처리하려면 `attachBaseContext` 를 덮어쓰고, 설정 저장과 화면 다시 만들기를 직접 짜야 한다. 이건 공식 지원 경로라 버그가 덜 생긴다.
3. **설정 화면**: `SettingsScreen.kt` 에 "언어" 항목을 하나 추가한다. 기존 선택 시트(`OptionSheet`)를 재사용하고, 선택지는 시스템 언어 · 한국어 · English · 日本語 · 简体中文 으로 한다. 각 언어 이름은 그 언어로 쓴다.
4. **번역**: 영어·일본어·중국어 간체 문구는 Claude 가 초안을 만든다. 일본어·중국어는 원어민 검수 전까지 "초안"으로 둔다.

### 영향받는 파일
- `app/build.gradle.kts`, `gradle/libs.versions.toml` (appcompat 추가, generateLocaleConfig)
- `app/src/main/AndroidManifest.xml` (서비스 추가)
- `app/src/main/res/values*/strings.xml`, `res/values/themes.xml`, `res/resources.properties`
- `MainActivity.kt`, `ui/settings/SettingsScreen.kt`
- 한국어 문구가 들어 있는 `ui/**`, `data/**` 의 Kotlin 파일 약 40개

### 작업 단위 (브랜치 · 커밋)
1. `feature/i18n-strings`: 문자열 꺼내기(영어 기본 + 한국어). 화면 묶음별로 커밋을 나눈다. 이 단계가 끝나도 한국어 기기에서는 화면이 지금과 똑같아야 한다.
2. `feature/i18n-ja-zh`: 일본어·중국어 간체 번역 파일.
3. `feature/language-selector`: AppCompat 전환과 설정 화면 언어 항목.

### 예상 결과
- 기기 언어가 영어·일본어·중국어(간체)면 앱이 그 언어로 나온다. 나머지 언어는 영어로 나온다.
- 설정에서 언어를 바꾸면 화면이 그 자리에서 다시 그려진다. 앱을 다시 켜도 선택이 유지된다.

### 검증
- `./gradlew :app:assembleDebug` 빌드.
- 남은 하드코딩 점검: 주석·로그를 뺀 Kotlin 소스에서 `"…한글…"` 검색 결과가 0개인지 확인한다.
- 에뮬레이터에서 4개 언어로 바꿔 가며 주요 화면(라이브러리·폴더·플레이어·설정·비공개 폴더)이 잘리거나 넘치지 않는지 확인한다.

### 범위 밖
- 스토어 등록 문구, 개인정보처리방침(`docs/privacy*.md`) 번역
- 중국어 번체 및 그 밖의 언어
