#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""具名实参 ↔ 函数签名 一致性自检（上游删/改形参时最容易漏的一类编译错）。

背景：上游改签名（删掉 `section` / `countdownNotificationId` 这类形参）时，Git 只在
**定义处**冲突；**调用点的具名实参**不冲突，于是留到编译才炸。
2026-11-20 跟 1.7.3 时，这类错靠 CI 连炸 3 批（每批 5 分钟）才收敛 —— 所以本地先扫一遍。

用法：  python3 tools/check_named_args.py <源码根目录>

判定（刻意保守，只报「确凿」的）：
  只在**该函数名全仓唯一定义**、且签名**完整解析成功**时才检查它的调用点。
  任何一处解析不确定（函数类型形参 / vararg / 同名多定义 / 定义在未扫描的模块）就整个跳过 ——
  宁可漏报也不要误报，否则门禁会变成噪音。
退出码：有可疑项 → 1
"""
import os
import re
import sys

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
SKIP_DIRS = {".git", "build", ".gradle"}

FUN_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public |internal |private |expect |actual |open |override |suspend |inline |operator |tailrec |external )*"
    r"fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\("
)

# 构造函数也要算：`data class DayBlock(week = …)` 这类具名实参同样会因改字段名编译失败
CTOR_RE = re.compile(
    r"^\s*(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public |internal |private |expect |actual |open |abstract |sealed |data |value |enum )*"
    r"class\s+(\w+)\s*\("
)


def kotlin_files():
    for base, dirs, files in os.walk(ROOT):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for f in files:
            if f.endswith(".kt"):
                yield os.path.join(base, f)


def parse_signature(lines, i, m):
    """返回 (形参名集合, 是否解析完整)。解析不确定返回 (set(), False)"""
    buf = lines[i][m.end() - 1:]
    depth = 0
    j = i
    while j < len(lines):
        add = buf if j == i else lines[j]
        depth += add.count("(") - add.count(")")
        if j > i:
            buf += "\n" + add
        if depth <= 0:
            break
        j += 1
        if j - i > 60:
            return set(), False
    if ")" not in buf:
        return set(), False
    inner = buf[1:buf.rfind(")")]
    if "->" in inner or "vararg" in inner:
        return set(), False
    params = set()
    for part in re.split(r",(?![^(]*\))", inner):
        part = part.strip()
        if not part:
            continue
        pm = re.match(r"(?:@\w+(?:\([^)]*\))?\s*)*(?:noinline\s+|crossinline\s+|private\s+|internal\s+)?(\w+)\s*:", part)
        if pm:
            params.add(pm.group(1))
        else:
            return set(), False
    return params, True


def collect(src):
    """name -> {'params': set, 'ambiguous': bool}"""
    out = {}
    lines = src.split("\n")
    for i, line in enumerate(lines):
        m = FUN_RE.match(line) or CTOR_RE.match(line)
        if not m:
            continue
        name = m.group(1)
        params, ok = parse_signature(lines, i, m)
        rec = out.setdefault(name, {"params": set(), "ambiguous": False, "count": 0})
        rec["count"] += 1
        if not ok or rec["count"] > 1:
            rec["ambiguous"] = True
        rec["params"] |= params
    return out


CALL_RE = re.compile(r"(?<![\w.])(\w+)\s*\(\s*\n")


def top_level_args(args):
    """取出调用实参里**顶层**的具名参数名（跳过嵌套括号里的）"""
    names = []
    depth = 0
    token = ""
    for ch in args:
        if ch in "([{":
            depth += 1
            token += ch
        elif ch in ")]}":
            depth -= 1
            token += ch
        elif depth == 0 and ch == ",":
            m = re.match(r"\s*(\w+)\s*=(?!=)", token)
            if m:
                names.append(m.group(1))
            token = ""
        else:
            token += ch
    m = re.match(r"\s*(\w+)\s*=(?!=)", token)
    if m:
        names.append(m.group(1))
    return names


def check(path, src, known):
    bad = []
    for m in CALL_RE.finditer(src):
        name = m.group(1)
        rec = known.get(name)
        if not rec or rec["ambiguous"] or not rec["params"]:
            continue
        start = m.end()
        depth = 1
        i = start
        while i < len(src) and depth > 0:
            c = src[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            i += 1
        args = src[start:i - 1]
        # ★ 只取**外层调用的第 1 层**实参：嵌套调用（base.copy(name = …)）的具名实参
        #   属于内层函数，混进来就是误报（saveTimeConfig 里套 copy 那类）。
        outer = top_level_args(args)
        for arg in outer:
            if arg not in rec["params"]:
                bad.append((path, src[:m.start()].count("\n") + 1, name, arg))
    return bad


def main():
    known = {}
    sources = {}
    for p in kotlin_files():
        try:
            src = open(p, encoding="utf-8", errors="replace").read()
        except OSError:
            continue
        sources[p] = src
        for name, rec in collect(src).items():
            agg = known.setdefault(name, {"params": set(), "ambiguous": False, "count": 0})
            agg["params"] |= rec["params"]
            agg["count"] += rec["count"]
            if rec["ambiguous"] or agg["count"] > 1:
                agg["ambiguous"] = True
    problems = []
    for p, src in sources.items():
        problems.extend(check(p, src, known))
    safe = sum(1 for r in known.values() if not r["ambiguous"] and r["params"])
    if problems:
        print("❌ 具名实参对不上函数签名（上游可能删/改了形参）：")
        for p, line, fn, arg in problems:
            print("   %s:%d  调用 %s(...) 传了 %s = …，但签名里没有这个形参" % (p, line, fn, arg))
        return 1
    print("✅ 具名实参与函数签名一致（检查了 %d 个唯一定义的函数）" % safe)
    return 0


if __name__ == "__main__":
    sys.exit(main())
