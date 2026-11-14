# logcat에서 음성 응답 지연을 뽑는다: 앱이 말하라고 한 때(doSpeak) → 엔진이 첫 소리 데이터를 낸 때(pcmSize)
import sys, re, datetime
t = lambda s: datetime.datetime.strptime(s, "%H:%M:%S.%f")
ask = None; out = []
for line in open(sys.argv[1], encoding="utf-8", errors="ignore"):
    m = re.match(r"\S+ (\d\d:\d\d:\d\d\.\d+)", line)
    if not m: continue
    if "doSpeak(" in line:
        ask = t(m.group(1)); txt = line.split("text=")[-1].split(",")[0][:24]
    elif "pcmSize" in line and ask:
        out.append((txt, int((t(m.group(1)) - ask).total_seconds() * 1000))); ask = None
for x, ms in out: print(f"{ms:6d}ms  {x}")
if out:
    v = sorted(ms for _, ms in out); print("중앙값", v[len(v) // 2], "ms / 건수", len(v))
