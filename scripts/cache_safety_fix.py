from pathlib import Path

path = Path('app/src/main/java/com/windroid/xp/MainActivity.kt')
text = path.read_text(encoding='utf-8')
old = '''    assetImageCache[key] = image
    return image
}'''
new = '''    if (image != null) assetImageCache[key] = image
    return image
}'''
if old not in text:
    raise SystemExit('asset cache assignment not found')
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('Fixed nullable ConcurrentHashMap cache write')
