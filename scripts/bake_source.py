from pathlib import Path

workflow = Path('.github/workflows/build-apk.yml')
lines = workflow.read_text(encoding='utf-8').splitlines()
remove_names = {
    'Apply launcher runtime cleanup',
    'Unify built-in desktop shortcuts',
    'Bake transformed launcher source',
}

out = []
i = 0
while i < len(lines):
    line = lines[i]
    stripped = line.strip()
    if stripped.startswith('- name:'):
        name = stripped.split(':', 1)[1].strip()
        if name in remove_names:
            indent = len(line) - len(line.lstrip())
            i += 1
            while i < len(lines):
                nxt = lines[i]
                nxt_stripped = nxt.strip()
                nxt_indent = len(nxt) - len(nxt.lstrip()) if nxt.strip() else 999
                if nxt_stripped.startswith('- name:') and nxt_indent == indent:
                    break
                i += 1
            while out and out[-1] == '':
                out.pop()
            out.append('')
            continue
    out.append(line)
    i += 1

workflow.write_text('\n'.join(out).rstrip() + '\n', encoding='utf-8')

for script in ['scripts/runtime_cleanup.py', 'scripts/unify_desktop.py', 'scripts/bake_source.py']:
    p = Path(script)
    if p.exists():
        p.unlink()

print('Baked transformed Kotlin into source and removed temporary build mutation scripts')
