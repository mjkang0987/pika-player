#!/usr/bin/env bash
# Google Maven 에 있는 의존성의 실제 최신 버전을 뽑는다.
#
# 이 저장소는 Google Maven 에 접근할 수 없는 환경에서 작성돼 AGP·Compose·Media3·
# Room·Billing 버전이 전부 추정값이다. 동기화 오류를 하나씩 만나는 대신
# 한 번에 확인하려고 둔다. 네트워크가 되는 곳(개발 기계)에서 실행할 것.
set -u
BASE=https://dl.google.com/dl/android/maven2

show() { # group-path  artifact  현재값
  local versions
  versions=$(curl -sS --max-time 20 "$BASE/$1/group-index.xml" \
    | tr '>' '\n' | grep -o "^ *<$2 versions=\"[^\"]*\"" | sed 's/.*versions="//')
  if [ -z "$versions" ]; then
    printf '%-46s %-14s (조회 실패)\n' "$2" "$3"
    return
  fi
  printf '%-46s %-14s 최신: %s\n' "$2" "$3" "$(echo "$versions" | tr ',' '\n' | tail -1)"
}

echo "아티팩트                                       현재(추정)     실제"
echo "-------------------------------------------------------------------------"
show com/android/tools/build          gradle          8.7.3
show androidx/compose                 compose-bom     2024.12.01
show androidx/media3                  media3-exoplayer 1.5.1
show androidx/room                    room-runtime    2.6.1
show androidx/datastore               datastore-preferences 1.1.1
show androidx/core                    core-ktx        1.15.0
show androidx/lifecycle               lifecycle-runtime-ktx 2.8.7
show androidx/activity                activity-compose 1.9.3
show com/android/billingclient        billing-ktx     7.1.1
echo
echo "Kotlin 2.4.10 · KSP 2.3.11 은 Maven Central 에서 확인 완료."
