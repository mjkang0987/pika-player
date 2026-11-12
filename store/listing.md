# Play Console 등록 자료

Play Console 에 그대로 붙여 넣는 값이다. 글자 수 제한: 앱 이름 30자, 간단한 설명 80자, 자세한 설명 4000자.

## 파일

| 항목 | 파일 | 규격 |
|---|---|---|
| 앱 아이콘 | `store/ic_launcher-playstore.png` | 512×512 |
| 그래픽 이미지 | `store/feature-graphic-{ko,en}.png` | 1024×500 |
| 휴대전화 스크린샷 | `store/screenshots/{ko,en}/1~4-*.png` | 1080×1920, 4장 |

스크린샷의 영상은 ffmpeg 로 만든 그라데이션이다. 실제 사람·저작물이 들어 있지 않다.

---

## 한국어 (ko-KR, 기본 언어)

**앱 이름** (11자)

```
Pika Player
```

**간단한 설명** (45자)

```
광고 없는 동영상 플레이어. 자막, 이어보기, 작은 창, 비공개 폴더까지 무료로.
```

**자세한 설명**

```
Pika Player 는 휴대폰에 있는 동영상을 편하게 보는 플레이어입니다.
광고도, 결제도, 회원가입도 없습니다. 모든 기능이 무료입니다.

■ 보관함
• 기기 안의 영상을 자동으로 모아 보여 줍니다
• 폴더별 보기, 최근 본 영상, 이어보기 목록
• 이름·크기·재생시간·수정일 순 정렬과 검색
• 권한을 주지 않아도 직접 고른 폴더만 열어 볼 수 있습니다

■ 재생
• 마지막으로 본 위치에서 이어보기
• 재생속도 조절, 10초 앞뒤로 넘기기(두 번 탭)
• 화면 왼쪽·오른쪽을 밀어 밝기·볼륨 조절
• 구간 반복(A-B), 한 개 반복·전체 반복·무작위
• 화면 맞춤·채우기·늘리기
• 앱을 나가도 작은 창(PiP)으로 계속 재생
• 재생목록 만들기, 다음 영상 자동 재생

■ 자막
• 같은 폴더의 같은 이름 자막(SRT, SMI)을 자동으로 붙입니다
• 글자가 깨지면 인코딩을 바꿔 볼 수 있습니다
• 자막 크기와 싱크 조절

■ 비공개 폴더 · 어린이 잠금
• 고른 폴더를 PIN 으로 목록에서 감춥니다
• 어린이 잠금을 켜면 PIN 없이는 재생 화면을 나갈 수 없습니다
※ 비공개 폴더는 이 앱의 목록에서 감추는 기능이며, 파일을 암호화하지 않습니다.

■ 개인정보
• 인터넷 권한이 없습니다. 어떤 정보도 밖으로 보내지 않습니다
• 분석 도구, 광고 SDK 를 넣지 않았습니다
• 파일 삭제는 사용자가 확인했을 때만 합니다

■ 언어
한국어, English, 日本語, 简体中文
```

**출시 노트** (1.0.0)

```
Pika Player 첫 버전입니다.
```

---

## English (en-US)

**App name**

```
Pika Player
```

**Short description** (75 chars)

```
Ad-free video player with subtitles, resume, PiP and private folders. Free.
```

**Full description**

```
Pika Player is a simple player for the videos on your phone.
No ads, no in-app purchases, no sign-up. Every feature is free.

LIBRARY
• Finds the videos on your device automatically
• Browse by folder, recent videos and Continue watching
• Sort by name, size, duration or date, and search
• Don't want to grant access? Pick a single folder instead

PLAYBACK
• Resume where you left off
• Playback speed, double-tap to skip 10 seconds
• Swipe on the left or right to change brightness and volume
• A-B repeat, repeat one, repeat all and shuffle
• Fit, fill or stretch the picture
• Keep watching in picture-in-picture when you leave the app
• Playlists and auto-play next video

SUBTITLES
• Loads SRT and SMI files with the same name from the same folder
• Change the encoding if text looks garbled
• Adjust subtitle size and timing

PRIVATE FOLDERS & CHILD LOCK
• Hide chosen folders from the list behind a PIN
• With child lock on, the player can't be left without your PIN
Note: private folders are hidden from this app's lists only. Files are not encrypted.

PRIVACY
• No internet permission. Nothing leaves your phone
• No analytics, no ad SDKs
• Files are deleted only after you confirm

LANGUAGES
English, 한국어, 日本語, 简体中文
```

**Release notes** (1.0.0)

```
First release of Pika Player.
```

---

## 앱 콘텐츠 (설문 답안)

**개인정보처리방침 URL**

```
https://github.com/mjkang0987/pika-player/blob/main/docs/privacy.md
```

**광고**: 광고 없음

**앱 액세스 권한**: 모든 기능을 별도 로그인이나 제한 없이 사용할 수 있음. (비공개 폴더 PIN 은 사용자가 직접 정하는 값이라 심사자용 계정이 필요 없다.)

**데이터 보안**
- 앱이 필수 사용자 데이터 유형을 수집하거나 공유하나요? → **아니요**
- 근거: `INTERNET` 권한이 없어 기기 밖으로 데이터를 보낼 수 없다. 재생 위치·설정·PIN 해시는 기기 안에만 저장된다(저장만 하고 전송하지 않으면 Play 기준 "수집"이 아니다).

**콘텐츠 등급** (IARC 설문)
- 카테고리: 그 외 앱(유틸리티·생산성 등)
- 폭력·성적 콘텐츠·욕설·약물·도박: 모두 **아니요** (앱 자체 콘텐츠가 없다)
- 사용자 간 상호작용·콘텐츠 공유: **아니요**
- 사용자 위치 공유: **아니요**
- 디지털 상품 구매: **아니요**

**타겟층**: 18세 이상 권장. 13세 미만을 넣으면 가족 정책 심사가 추가된다. 어린이 잠금은 부모가 쓰는 기능이라 아동 대상 앱이 아니다.

**뉴스 앱**: 아니요 · **정부 앱**: 아니요 · **금융 기능**: 없음 · **건강 앱**: 아니요

**앱 카테고리**: 동영상 플레이어 및 편집기 (Video Players & Editors)

**연락처 이메일**: `docs/privacy.md` 9항과 같은 주소
