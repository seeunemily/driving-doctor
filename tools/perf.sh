#!/bin/bash
# 앱 성능 재기: perf.sh <pkg> <apk> <rail y값들...>  → 콜드 스타트(7회 중앙값), 화면 전환 프레임, 대기 CPU, 메모리, APK 크기
export MSYS_NO_PATHCONV=1
A=${ADB:-adb}  # adb 경로가 다르면 ADB=경로 로 지정
P=$1; APK=$2; shift 2; YS="$@"
echo "APK $(python -c "import os;print(round(os.path.getsize(r'$(cygpath -w "$APK")')/1048576,1))")MB"
$A install -r "$APK" >/dev/null
# 사용자가 스토어에서 막 받은 상태처럼: 첫 실행으로 프로필 설치 → 기기가 쉬는 동안 하는 컴파일(speed-profile)
$A shell am start -W -n $P/.MainActivity >/dev/null; sleep 5
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $P/androidx.profileinstaller.ProfileInstallReceiver >/dev/null 2>&1; sleep 2
$A shell am force-stop $P; $A shell cmd package compile -f -m speed-profile $P >/dev/null
ts=""; for i in 1 2 3 4 5 6 7; do $A shell am force-stop $P; sleep 1.5; t=$($A shell am start -W -n $P/.MainActivity | grep TotalTime | tr -dc 0-9); ts="$ts $t"; sleep 2.5; done
echo "cold start ms:$ts → median $(echo $ts | tr ' ' '\n' | sort -n | sed -n 4p)"
sleep 3; $A shell dumpsys gfxinfo $P reset >/dev/null
for y in $YS $YS; do $A shell input tap 960 $y; sleep 1.6; done
$A shell dumpsys gfxinfo $P | grep -E "Total frames|Janky frames|50th|90th|99th" | head -5 | tr -s ' ' | tr '\n' ';'; echo
PID=$($A shell pidof $P | tr -d '\r'); $A shell input tap 960 ${YS%% *}; sleep 2
c1=$($A shell cat /proc/$PID/stat | awk '{print $14+$15}'); sleep 30; c2=$($A shell cat /proc/$PID/stat | awk '{print $14+$15}')
echo "idle CPU 30s: $(python -c "print(round(($c2-$c1)/100/30*100,2))")%"
$A shell dumpsys meminfo $P | grep -E "TOTAL PSS|TOTAL:" | head -1 | tr -s ' '
