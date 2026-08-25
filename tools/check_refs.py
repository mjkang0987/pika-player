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

# 4) padding 오버로드 — horizontal/vertical 과 start/top/end/bottom 은 섞을 수 없다.
#    Modifier.padding 에는 (all) (horizontal, vertical) (start, top, end, bottom)
#    세 가지뿐이라 `padding(horizontal = .., bottom = ..)` 같은 조합은 없다.
for p, s in files.items():
    for i, line in enumerate(s.split('\n'), 1):
        for m in re.finditer(r'\.padding\(([^)]*)\)', line):
            names = set(re.findall(r'(\w+)\s*=', m.group(1)))
            if names & {'horizontal', 'vertical'} and names & {'start', 'top', 'end', 'bottom'}:
                bad.append(f'{p}:{i}: padding({m.group(1)}) — 없는 조합')

# 5) 딸린 접근자만 남은 프로퍼티
#    `private set` 은 바로 위의 var 선언에 붙는 것이다. 프로퍼티를 지우면서
#    이 줄을 남기면 위에 있던 다른 프로퍼티에 조용히 옮겨 붙는다. val 위로
#    옮겨 붙으면 컴파일이 깨지고("A 'val' property cannot have a setter"),
#    var 위로 옮겨 붙으면 컴파일은 되면서 원래 프로퍼티의 setter 만 열린다.
#    뒤쪽이 더 위험하다 — 아무 신호 없이 캡슐화가 풀린다. 실제로 둘 다 났다.
for p, s in files.items():
    lines = s.split('\n')
    for i, line in enumerate(lines):
        if not re.fullmatch(r'\s*(?:private|internal|protected)\s+set\s*', line):
            continue
        # 위로 올라가며 이 접근자가 딸린 선언을 찾는다. 초기화식이 여러 줄에
        # 걸치는 프로퍼티가 있어서 바로 윗줄만 봐서는 안 된다.
        owner = ''
        for j in range(i - 1, -1, -1):
            t = lines[j].strip()
            if not t or t.startswith('//'):
                continue
            if re.search(r'\b(?:val|var|fun|class|object|interface)\b', t):
                owner = t
                break
        if owner and not re.search(r'\bvar\b', owner):
            bad.append(f'{p}:{i + 1}: `{line.strip()}` — 딸린 선언이 var 가 아니다 ({owner[:50]})')

# 6) 함수형이 아닌 자리에 넘긴 콜백
#    파라미터를 중간에 끼워 넣으면 그 뒤의 위치 인자가 한 칸씩 밀린다. 밀린 자리가
#    Boolean 이나 String 인데 콜백을 넘기고 있으면 컴파일러가 잡아 주지만, 그때는
#    이미 빌드를 한 번 버린 뒤다. ValueRow 와 DialogAction 에서 같은 실수를 두 번 했다.
def _split_args(text):
    """괄호·중괄호 깊이를 세며 최상위 쉼표로만 자른다."""
    out, depth, cur = [], 0, ''
    for ch in text:
        if ch in '([{':
            depth += 1
        elif ch in ')]}':
            depth -= 1
        if ch == ',' and depth == 0:
            out.append(cur); cur = ''
        else:
            cur += ch
    if cur.strip():
        out.append(cur)
    return [a.strip() for a in out]

# 선언에서 파라미터 이름과 형을 뽑는다.
decl_params = {}
for src in files.values():
    for m in re.finditer(r'\bfun\s+(\w+)\s*\(', src):
        i = m.end() - 1
        depth, j = 0, i
        while j < len(src):
            if src[j] == '(':
                depth += 1
            elif src[j] == ')':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        params = []
        for part in _split_args(src[i + 1:j]):
            pm = re.match(r'(?:@\w+\s+)*(?:\w+\s+)*?(\w+)\s*:\s*(.+)', part.replace('\n', ' '))
            if pm:
                params.append((pm.group(1), pm.group(2)))
        # 같은 이름이 여러 번이면 판단할 수 없다. 오탐을 내느니 건너뛴다.
        decl_params[m.group(1)] = None if m.group(1) in decl_params else params

CALLBACKISH = re.compile(r'^(::\w+|\w+::\w+|on[A-Z]\w*)$')
for p, src in files.items():
    for m in re.finditer(r'(?<![\w.])([A-Z]\w+)\s*\(', src):
        name = m.group(1)
        params = decl_params.get(name)
        if not params:
            continue
        i = m.end() - 1
        depth, j = 0, i
        while j < len(src):
            if src[j] == '(':
                depth += 1
            elif src[j] == ')':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        if j >= len(src):
            continue
        for idx, arg in enumerate(_split_args(src[i + 1:j])):
            if '=' in arg.split('(')[0]:
                break  # 이름 붙인 인자부터는 자리와 무관하다
            if idx >= len(params):
                break
            pname, ptype = params[idx]
            if CALLBACKISH.match(arg) and '->' not in ptype:
                line = src[:i].count('\n') + 1
                bad.append(
                    f'{p}:{line}: {name}(...) {idx + 1}번째 자리는 `{pname}: {ptype.strip()}` '
                    f'인데 `{arg}` 를 넘김'
                )

# 선언보다 먼저 쓴 지역 변수(실제로 libraryVm·vaultVm 에서 두 번 났다)도 잡고 싶었지만
# 넣지 않았다. 함수마다 같은 이름(colors 등)이 반복돼 정규식으로는 어느 함수의 것인지
# 가릴 수 없고, 시험 삼아 넣었더니 오탐이 70개 났다. 늘 틀리는 검사는 무시하게 되고,
# 그러면 진짜를 놓친다. 이건 컴파일러가 정확히 잡아주는 종류이기도 하다.

print('\n'.join(sorted(set(bad))) if bad else '참조 일관성 OK')
sys.exit(1 if bad else 0)
