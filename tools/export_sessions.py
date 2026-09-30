#!/usr/bin/env python3
"""Exporta sesiones de Claude Code (~/.claude/projects/*/*.jsonl) a Markdown.

Uso: export_sessions.py [--out DIR] [--src DIR] [--force]
Idempotente: salta sesiones cuyo .md es más reciente que el .jsonl.
Por defecto --out = /mnt/c/Users/<usuario Windows>/ClaudeSessions (WSL).
"""
import argparse, glob, json, os, sys
from datetime import datetime

def default_out():
    users = [d for d in glob.glob("/mnt/c/Users/*")
             if os.path.basename(d) not in ("Public", "Default", "Default User", "All Users")
             and os.path.isdir(d)]
    if len(users) == 1:
        return os.path.join(users[0], "ClaudeSessions")
    sys.exit("No pude deducir tu usuario de Windows; usa --out /mnt/c/Users/<tu_usuario>/ClaudeSessions")

def text_of(content):
    """Devuelve (texto, [resumen de tool calls]) de un message.content."""
    if isinstance(content, str):
        return content, []
    parts, tools = [], []
    for b in content or []:
        t = b.get("type")
        if t == "text":
            parts.append(b.get("text", ""))
        elif t == "tool_use":
            arg = json.dumps(b.get("input", {}), ensure_ascii=False)
            tools.append(f"`{b.get('name')}` {arg[:200]}")
        elif t == "tool_result":
            pass  # ruido; se omite
    return "\n".join(parts).strip(), tools

def convert(path):
    msgs, title, cwd, branch, first, last = [], None, None, None, None, None
    for line in open(path, encoding="utf-8"):
        try:
            d = json.loads(line)
        except ValueError:
            continue
        ty = d.get("type")
        if ty == "ai-title":
            title = d.get("aiTitle") or title
        if ty not in ("user", "assistant") or d.get("isSidechain"):
            continue
        cwd = cwd or d.get("cwd"); branch = branch or d.get("gitBranch")
        ts = d.get("timestamp")
        if ts:
            first = first or ts; last = ts
        txt, tools = text_of(d.get("message", {}).get("content"))
        if txt or tools:
            msgs.append((ty, ts, txt, tools))
    return title, cwd, branch, first, last, msgs

def render(sid, project, title, cwd, branch, first, last, msgs):
    o = [f"# {title or sid}", "",
         f"- **Sesión:** `{sid}`", f"- **Proyecto:** {project}",
         f"- **Directorio:** {cwd}", f"- **Rama:** {branch}",
         f"- **Inicio:** {first}", f"- **Fin:** {last}", "", "---", ""]
    for ty, ts, txt, tools in msgs:
        o.append(f"## {'🧑 Usuario' if ty == 'user' else '🤖 Claude'} — {ts}\n")
        if txt: o.append(txt + "\n")
        for t in tools: o.append(f"> 🔧 {t}\n")
    return "\n".join(o)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--src", default=os.path.expanduser("~/.claude/projects"))
    ap.add_argument("--out"); ap.add_argument("--force", action="store_true")
    a = ap.parse_args()
    out = a.out or default_out()
    done = skipped = 0
    for f in sorted(glob.glob(os.path.join(a.src, "*", "*.jsonl"))):
        project = os.path.basename(os.path.dirname(f))
        sid = os.path.splitext(os.path.basename(f))[0]
        title, cwd, branch, first, last, msgs = convert(f)
        if not msgs:
            continue
        date = (first or datetime.fromtimestamp(os.path.getmtime(f)).isoformat())[:10]
        dest = os.path.join(out, project, f"{date}_{sid[:8]}.md")
        if not a.force and os.path.exists(dest) and os.path.getmtime(dest) >= os.path.getmtime(f):
            skipped += 1; continue
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, "w", encoding="utf-8") as fh:
            fh.write(render(sid, project, title, cwd, branch, first, last, msgs))
        done += 1
    print(f"exportadas: {done}, sin cambios: {skipped}, destino: {out}")

if __name__ == "__main__":
    main()
