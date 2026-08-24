#!/usr/bin/env bash
# Google Maven 에 있는 의존성의 실제 버전을 뽑는다.
#
# 이 저장소는 Google Maven 에 접근할 수 없는 환경에서 작성돼 AGP·Compose·Media3·
# Room·Billing 버전이 전부 추정값이다. 동기화 오류를 하나씩 만나는 대신 한 번에
# 확인하려고 둔다. 네트워크가 되는 곳(개발 기계)에서 실행할 것.
#
# 안정 버전과 프리릴리스를 나눠서 보여준다. alpha/beta/rc 를 그냥 쓰면 안 된다 —
# 특히 AGP 는 메이저 알파에 DSL 이 깨져 있는 경우가 있다.
set -u
BASE=https://dl.google.com/dl/android/maven2

show() { # group-path  artifact  현재값
  local all stable latest
  all=$(curl -sS --max-time 20 "$BASE/$1/group-index.xml" \
    | tr '>' '\n' | grep -o "^ *<$2 versions=\"[^\"]*\"" | sed 's/.*versions="//' | tr ',' '\n')
  if [ -z "$all" ]; then
    printf '%-24s %-14s (조회 실패)\n' "$2" "$3"
    return
  fi
  stable=$(echo "$all" | grep -viE 'alpha|beta|-rc|dev|snapshot' | tail -1)
  latest=$(echo "$all" | tail -1)
  if [ "$stable" = "$latest" ]; then
    printf '%-24s %-14s 안정: %s\n' "$2" "$3" "$stable"
  else
    printf '%-24s %-14s 안정: %-16s (프리릴리스: %s)\n' "$2" "$3" "$stable" "$latest"
  fi
}

echo "아티팩트                 현재            실제"
echo "---------------------------------------------------------------------------"
show com/android/tools/build   gradle                8.7.3
show androidx/compose          compose-bom           2024.12.01
show androidx/media3           media3-exoplayer      1.5.1
show androidx/room             room-runtime          2.6.1
show androidx/datastore        datastore-preferences 1.1.1
show androidx/core             core-ktx              1.15.0
show androidx/lifecycle        lifecycle-runtime-ktx 2.8.7
show androidx/activity         activity-compose      1.9.3
show com/android/billingclient billing-ktx           7.1.1

# AGP 는 메이저마다 호환 범위가 달라(특히 KSP·Gradle) 최신 하나만으로는 못 정한다.
echo
echo "AGP: 메이저별 최신 안정 버전"
echo "---------------------------------------------------------------------------"
curl -sS --max-time 20 "$BASE/com/android/tools/build/group-index.xml" \
  | tr '>' '\n' | grep -o '^ *<gradle versions="[^"]*"' | sed 's/.*versions="//' | tr ',' '\n' \
  | grep -viE 'alpha|beta|-rc|dev' \
  | awk -F. '{print $1"\t"$0}' | sort -k1,1n -k2,2V | awk -F'\t' '{last[$1]=$2} END {for (m in last) print "  AGP " m ".x  ->  " last[m]}' | sort

echo
echo "Kotlin 2.4.10 · KSP 2.3.11 은 Maven Central 에서 확인 완료."
echo "KSP 최신은 2.3.11(2026-08-03) 이고 AGP 9 를 아직 지원하지 않는다."
