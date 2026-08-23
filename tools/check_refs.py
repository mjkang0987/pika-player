#!/usr/bin/env python3
"""app 모듈 정적 점검.

이 저장소는 안드로이드 SDK 와 Google Maven 이 없는 환경에서 작성되어 :app 을
컴파일할 수 없다. 컴파일러가 잡아줬을 실수 중 실제로 났던 세 가지만 확인한다.

  1. `xxxVm::method` 가 해당 ViewModel 에 없는 경우
  2. 이름붙은 인자가 선언에 없거나, 필수 인자가 빠진 경우
  3. 대문자로 시작하는 호출 대상(주로 컴포저블)이 선언도 임포트도 되지 않은 경우

정적 문자열 검사라 타입은 못 본다. 컴파일 대체재가 아니라 자주 나는 실수를
거르는 그물이다. `python3 tools/check_refs.py`, 문제가 있으면 종료 코드 1.
"""
import re, glob, sys

ROOT = 'app/src/main/java/com/pikaworks/pikaplayer'


def strip_comments(s):
    """주석과 문자열 내용을 지운다.

    한 번에 훑어야 한다. 주석 안에 따옴표가, 문자열 안에 `//`(URL) 가 들어 있어서
    둘 중 하나를 먼저 통째로 지우면 나머지가 어긋난다.
    """
    out, i, n = [], 0, len(s)
    while i < n:
        two, three = s[i:i + 2], s[i:i + 3]
        if three == '"""':
            end = s.find('"""', i + 3)
            i = n if end < 0 else end + 3
            out.append('""')
        elif two == '/*':
            end = s.find('*/', i + 2)
            i = n if end < 0 else end + 2
        elif two == '//':
            end = s.find('\n', i)
            i = n if end < 0 else end
        elif s[i] == '"':
            j = i + 1
            while j < n and s[j] != '"':
                j += 2 if s[j] == '\\' else 1
            i = j + 1
            out.append('""')
        else:
            out.append(s[i])
            i += 1
    return ''.join(out)


files = {p: strip_comments(open(p, encoding='utf-8').read())
         for p in sorted(glob.glob(ROOT + '/**/*.kt', recursive=True))}


def match_paren(s, i):
    """s[i] == '(' 일 때 짝이 되는 ')' 의 인덱스."""
    depth = 0
    for j in range(i, len(s)):
        if s[j] == '(':
            depth += 1
        elif s[j] == ')':
            depth -= 1
            if depth == 0:
                return j
    return len(s) - 1


def split_args(body):
    """최상위 쉼표로만 나눈다. 람다 타입의 `->` 때문에 <> 는 세지 않는다."""
    out, cur, depth = [], '', 0
    for ch in body:
        if ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
        else:
            cur += ch
    out.append(cur)
    return [c.strip() for c in out if c.strip()]


bad = []

# 1) ViewModel 메서드 참조
vms = {}
for s in files.values():
    for m in re.finditer(r'class (\w+ViewModel)', s):
        vms[m.group(1)] = s
ALIAS = {'playerVm': 'PlayerViewModel', 'libraryVm': 'LibraryViewModel', 'folderVm': 'FolderViewModel'}
for p, s in files.items():
    for m in re.finditer(r'\b(\w+Vm)::(\w+)', s):
        vm = ALIAS.get(m.group(1))
        if vm and not re.search(r'\bfun ' + m.group(2) + r'\b', vms.get(vm, '')):
            bad.append(f'{p}: {m.group(1)}::{m.group(2)} — {vm} 에 없음')

# 2) 함수 선언 수집. 같은 이름이 파일마다 따로 있을 수 있어(예: private Chip)
#    파일별로 나눠 둔다.
DECL_RE = r'\bfun\s+(?:<[^>]*>\s*)?(\w+)\('
by_file = {}
for p, s in files.items():
    for m in re.finditer(DECL_RE, s):
        body = s[m.end():match_paren(s, m.end() - 1)]
        params = []
        for c in split_args(body):
            mm = re.match(r'^(?:(?:@\w+|vararg|crossinline|noinline)\s+)*(\w+)\s*:', c)
            if mm:
                params.append((mm.group(1), '=' in c.split(':', 1)[1]))
        by_file.setdefault(p, {}).setdefault(m.group(1), params)

# 파일 밖에서 보이는 선언. 이름이 겹치고 시그니처가 다르면 어느 쪽인지 알 수
# 없으므로 검사에서 뺀다.
shared = {}
for p, d in by_file.items():
    for name, sig in d.items():
        if name in shared and shared[name] != sig:
            shared[name] = None
        else:
            shared[name] = sig


def resolve(p, name):
    return by_file.get(p, {}).get(name, shared.get(name))


for p, s in files.items():
    for m in re.finditer(r'(?<![\w.@])([A-Za-z_]\w*)\s*\(', s):
        name = m.group(1)
        sig = resolve(p, name)
        if not sig:
            continue
        if re.search(r'\bfun\s+(?:<[^>]*>\s*)?' + name + r'\s*\($', s[:m.end()]):
            continue  # 선언 자신
        close = match_paren(s, m.end() - 1)
        args = split_args(s[m.end():close])
        given = {a.split('=')[0].strip() for a in args if re.match(r'^\w+\s*=(?!=)', a)}
        positional = len(args) - len(given)
        declared = {n for n, _ in sig}
        for g in sorted(given - declared):
            bad.append(f'{p}: {name}(...) 없는 인자 `{g}`')
        # 위치 인자가 섞이면 무엇이 채워졌는지 이름으로 알 수 없다.
        if positional:
            continue
        if s[close + 1:].lstrip().startswith('{') and sig:
            given.add(sig[-1][0])  # 후행 람다
        for r in sorted({n for n, d in sig if not d} - given):
            bad.append(f'{p}: {name}(...) 필수 인자 누락 `{r}`')

# 3) 해석되지 않는 대문자 호출 — 컴포저블을 쓰기만 하고 만들지 않은 경우
module_names = set(shared)
for s in files.values():
    for m in re.finditer(r'\b(?:class|object|interface)\s+(\w+)', s):
        module_names.add(m.group(1))
for p, s in files.items():
    # 같은 파일의 enum 상수는 `LIBRARY("보관함")` 처럼 호출로 보인다.
    local = set(re.findall(r'\b([A-Z][A-Z0-9_]*)\s*\(', s))
    known = module_names | set(re.findall(r'^import\s+(?:[\w.]+\.)?(\w+)', s, flags=re.M)) | local
    for m in re.finditer(r'(?<![\w.@])([A-Z]\w+)\s*\(', s):
        if m.group(1) not in known:
            bad.append(f'{p}: `{m.group(1)}(...)` — 선언도 임포트도 없음')

print('\n'.join(sorted(set(bad))) if bad else '참조 일관성 OK')
sys.exit(1 if bad else 0)
