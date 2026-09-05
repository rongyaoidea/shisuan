#!/usr/bin/env python3
"""把 ShisuanIcons.kt 的 ImageVector builder 调用解析为 SVG，生成图标总览预览页。"""
import re, html

SRC = "/tmp/shisuan/app/src/main/java/com/example/shisuan/ui/icons/ShisuanIcons.kt"
OUT = "/tmp/shisuan/icon_preview.html"

text = open(SRC, encoding="utf-8").read()

# 提取每个 public 图标：val Name: ImageVector ... private var _name
icon_re = re.compile(r"^val (\w+): ImageVector", re.M)
matches = list(icon_re.finditer(text))
icons = []
for i, m in enumerate(matches):
    name = m.group(1)
    end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
    block = text[m.start():end]
    # 仅取第一个 path{} 内的绘制调用
    pstart = block.find("path(")
    pend = block.find("}.build()")
    body = block[pstart:pend]
    d_parts = []
    tok = re.finditer(
        r'moveTo\(([\d.f]+)f,\s*([\d.f]+)f\)|lineTo\(([\d.f]+)f,\s*([\d.f]+)f\)'
        r'|horizontalLineTo\(([\d.f]+)f\)|verticalLineTo\(([\d.f]+)f\)'
        r'|curveTo\(([\d.f]+)f,\s*([\d.f]+)f,\s*([\d.f]+)f,\s*([\d.f]+)f,\s*([\d.f]+)f,\s*([\d.f]+)f\)'
        r'|close\(\)', body)
    for t in tok:
        if t.group(1) is not None:
            d_parts.append(f"M {t.group(1)} {t.group(2)}")
        elif t.group(3) is not None:
            d_parts.append(f"L {t.group(3)} {t.group(4)}")
        elif t.group(5) is not None:
            d_parts.append(f"H {t.group(5)}")
        elif t.group(6) is not None:
            d_parts.append(f"V {t.group(6)}")
        elif t.group(7) is not None:
            d_parts.append("C " + " ".join(t.group(k) for k in range(7, 13)))
        elif t.group(0).startswith("close"):
            d_parts.append("Z")
    icons.append((name, " ".join(d_parts)))

def svg(name, d, stroke="#222222"):
    return (f'<svg viewBox="0 0 24 24" width="48" height="48" fill="none" '
            f'stroke="{stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">'
            f'<path d="{html.escape(d)}"/></svg>')

cards_dark = "".join(
    f'<div class="cell"><div class="art">{svg(n, d, "#FAF9F5")}</div>'
    f'<span>{n}</span></div>'
    for n, d in icons)
cards_light = "".join(
    f'<div class="cell"><div class="art">{svg(n, d, "#222222")}</div>'
    f'<span>{n}</span></div>'
    for n, d in icons)

page = f"""<!DOCTYPE html><html><head><meta charset="utf-8"><style>
body{{margin:0;font-family:system-ui,sans-serif;background:#F5F4ED;color:#141413}}
h1{{font-size:15px;padding:14px 20px 4px;margin:0}}
p{{font-size:11px;color:#767676;padding:0 20px;margin:2px 0 10px}}
.grid{{display:grid;grid-template-columns:repeat(8,1fr);gap:6px;padding:0 16px 16px}}
.cell{{display:flex;flex-direction:column;align-items:center;gap:4px;padding:10px 2px;
  background:#FFFFFF;border:1px solid #E8E6DC;border-radius:10px}}
.cell span{{font-size:10px;color:#767676}}
.dark .cell{{background:#1F1E1D;border-color:#30302E}}
.dark .cell span{{color:#B8B6AE}}
</style></head><body>
<h1>ShisuanIcons 图标总览（{len(icons)} 个 · 24×24 / stroke 2 / round）</h1>
<p>由 ShisuanIcons.kt 自动转换生成，与 App 内实现一致。新增食品品类：Jam/Sauce/Seasoning/Can/Soda/Milk/Candy/Chocolate/Apple/Carrot/Egg/Grain/Fish/Frozen/Cup</p>
<div class="grid">{cards_light}</div>
<div class="dark"><h1>深色背景检查（对比度 / 负空间）</h1><div class="grid">{cards_dark}</div></div>
</body></html>"""

open(OUT, "w", encoding="utf-8").write(page)
print(f"OK: {len(icons)} icons -> {OUT}")
for n, d in icons:
    print(f"  {n:12s} {len(d):4d} chars")
