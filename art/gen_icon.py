#!/usr/bin/env python3
"""Void Slime logo: one geometry, written as the SVG master and the Android icon layers, once per theme.

A glossy dome of slime sitting on deep space, with the VOID family emblem (two galaxy arms spiralling out
of a white-hot spark) suspended inside it like a drop of void stirred into the slime, a few flecks of
glitter around it and a wet shine on top. Themes come from art/themes.json (the VOID desktop themes,
shared with VOID Portal).

Run from the project root:  python3 art/gen_icon.py
"""
import json
import math
import os
import re

CX, CY = 54.0, 59.0  # the emblem's centre, a little below the blob's middle where the slime is deepest
# The blob: a dome with a puffy, slightly lumpy bottom edge, inside the 33 dp safe zone.
BLOB = ("M27,70 C24,55 35,35 54,34 C73,33 84,54 81,69 C80,75 76,77 71,76 C65,78 60,75 54,77 "
        "C48,79 43,75 37,77 C31,78 28,75 27,70 Z")
SHINE = "M35,52 C35,44 41,39 48,38 C51,38 51,41 48,42 C43,44 40,48 39,53 C38,56 35,56 35,52 Z"
SHINE_DOT = (56.5, 39.5, 1.7)
SHADOW = (54, 79, 26, 3.4)  # an ellipse under the blob
GLITTER = [(40, 64, 0.9), (66, 49, 0.8), (70, 66, 1.0), (47, 47, 0.6), (61, 71, 0.7), (33, 62, 0.6), (75, 58, 0.6)]
THETA_MAX = 1.7 * math.pi
ARM_K = 0.5  # arm size relative to VOID Portal's
R0, R_GROW = 2.5 * ARM_K, 36.0 * ARM_K
S = 5.5 / 9.5  # spark scale


def arm_points(phase, n=64, length=1.0, width_scale=1.0, t0=0.0):
    """Outline of one tapered arm (or the part t0..length of it): thin at the centre, widest early, a fine tail."""
    pts = []
    for i in range(n + 1):
        t = t0 + i / n * (length - t0)
        th = t * THETA_MAX
        r = R0 + R_GROW * t ** 1.25
        pts.append((CX + r * math.cos(th + phase), CY + r * math.sin(th + phase), t))
    left, right = [], []
    for i, (x, y, t) in enumerate(pts):
        x2, y2, _ = pts[min(i + 1, n)] if i < n else pts[i]
        x1, y1, _ = pts[max(i - 1, 0)]
        dx, dy = x2 - x1, y2 - y1
        d = math.hypot(dx, dy) or 1
        nx, ny = -dy / d, dx / d
        w = (0.3 + 2.5 * math.sin(math.pi * min(t, 0.999) ** 0.55)) * width_scale * 0.75 / 2
        left.append((x + nx * w, y + ny * w))
        right.append((x - nx * w, y - ny * w))
    poly = left + right[::-1]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in poly) + " Z", pts[-1][:2]


def circle(cx, cy, r):
    return f"M{cx - r:g},{cy:g} a{r:g},{r:g} 0 1,0 {2 * r:g},0 a{r:g},{r:g} 0 1,0 {-2 * r:g},0"


def ellipse(cx, cy, rx, ry):
    return f"M{cx - rx:g},{cy:g} a{rx:g},{ry:g} 0 1,0 {2 * rx:g},0 a{rx:g},{ry:g} 0 1,0 {-2 * rx:g},0"


def sparkle(cx, cy, r):
    """A four-pointed glint."""
    k = r * 0.28
    return (f"M{cx:.2f},{cy - r:.2f} Q{cx + k:.2f},{cy - k:.2f} {cx + r:.2f},{cy:.2f} Q{cx + k:.2f},{cy + k:.2f} {cx:.2f},{cy + r:.2f} "
            f"Q{cx - k:.2f},{cy + k:.2f} {cx - r:.2f},{cy:.2f} Q{cx - k:.2f},{cy - k:.2f} {cx:.2f},{cy - r:.2f} Z")


SPARK = sparkle(CX, CY, 9.5 * S)
STARS = [(20, 24, 0.9, 0.8), (88, 22, 0.7, 0.6), (91, 70, 1.0, 0.7), (15, 80, 0.7, 0.5), (74, 96, 0.6, 0.6), (64, 12, 0.5, 0.5), (34, 95, 0.5, 0.45)]
PHASES = [-math.pi / 2, math.pi / 2]
SEGMENTS = 14
BLOB_GRAD = (54, 34, 54, 79)  # top to bottom


def vd_lin(x1, y1, x2, y2, stops):
    items = "".join(f'<item android:offset="{o}" android:color="{c}"/>' for o, c in stops)
    return f'<gradient android:type="linear" android:startX="{x1:.2f}" android:startY="{y1:.2f}" android:endX="{x2:.2f}" android:endY="{y2:.2f}">{items}</gradient>'


def vd_rad(cx, cy, r, stops):
    items = "".join(f'<item android:offset="{o}" android:color="{c}"/>' for o, c in stops)
    return f'<gradient android:type="radial" android:centerX="{cx}" android:centerY="{cy}" android:gradientRadius="{r}">{items}</gradient>'


def argb(hex_rgb, a):
    return f"#{int(a * 255):02X}{hex_rgb[1:]}"


def mix(a, b, f):
    pa = [int(a[i:i + 2], 16) for i in (1, 3, 5)]
    pb = [int(b[i:i + 2], 16) for i in (1, 3, 5)]
    return "#" + "".join(f"{round(x + (y - x) * f):02X}" for x, y in zip(pa, pb))


def palette(theme):
    """Icon colours from a theme; the same background mapping as VOID Portal's, so the icons sit together."""
    c = theme["colors"]
    light = theme["mode"] == "light"
    a, a2 = c["accent"].upper(), c["accent2"].upper()
    return {
        # The slime runs from a pale, lit top through the accent to a deep, saturated bottom.
        "slime": [(0, mix(a2, "#FFFFFF", 0.55)), (0.45, mix(a, a2, 0.35)), (1, mix(a, "#05040C", 0.45))],
        "rim": mix(a2, "#FFFFFF", 0.6),
        "arms": [a2, mix(a, "#FFFFFF", 0.25)],
        "hot": mix("#FFFFFF", a2, 0.05),
        "bg": (mix(c["panelAlt"], a, 0.08), c["bg"].upper()) if light else (mix(c["bg"], a, 0.12), mix(c["bg"], "#000000", 0.35)),
        "nebula": (mix(c["bg"], a, 0.35), 0.45) if light else (mix(c["bg"], a, 0.45), 0.85),
        "star": (a, 0.45) if light else ("#FFFFFF", 1.0),
        "core": "#0B0820",
        "glow": a2,
        "spark_end": mix("#FFFFFF", a2, 0.35),
        "glitter": mix(c["warning"].upper(), "#FFFFFF", 0.35),
    }


def arm_segments(phase, color, hot):
    """The arm in short overlapping pieces, coloured along its length: white-hot core, arm colour, fading tail."""
    out = []
    for k in range(SEGMENTS):
        t0, t1 = k / SEGMENTS, min(1.0, (k + 1.35) / SEGMENTS)
        tm = (t0 + t1) / 2
        c = mix(hot, color, min(1.0, tm / 0.22))
        alpha = 1.0 if tm < 0.55 else 1.0 - 0.6 * (tm - 0.55) / 0.45
        d, _ = arm_points(phase, n=10, length=t1, t0=t0)
        out.append((d, c, alpha))
    return out


HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" '
        'xmlns:aapt="http://schemas.android.com/aapt" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n')


def background_vd(P):
    out = HEAD
    top, bottom = P["bg"]
    neb, neb_a = P["nebula"]
    star, star_a = P["star"]
    out += f'  <path android:pathData="M0,0h108v108h-108z"><aapt:attr name="android:fillColor">{vd_lin(54, 0, 54, 108, [(0, argb(top, 1)), (1, argb(bottom, 1))])}</aapt:attr></path>\n'
    out += f'  <path android:pathData="M0,0h108v108h-108z"><aapt:attr name="android:fillColor">{vd_rad(54, 54, 50, [(0, argb(neb, neb_a)), (1, argb(neb, 0))])}</aapt:attr></path>\n'
    for cx, cy, r, a in STARS:
        out += f'  <path android:pathData="{circle(cx, cy, r)}" android:fillColor="{argb(star, 1)}" android:fillAlpha="{a * star_a:.2f}"/>\n'
    return out + "</vector>\n"


def foreground_vd(P):
    out = HEAD
    out += f'  <path android:pathData="{ellipse(*SHADOW)}" android:fillColor="#59000000"/>\n'
    out += f'  <path android:pathData="{BLOB}" android:strokeWidth="5" android:strokeAlpha="0.18" android:strokeLineJoin="round" android:strokeColor="{argb(P["glow"], 1)}"/>\n'
    slime = [(o, argb(c, 0.96)) for o, c in P["slime"]]
    out += f'  <path android:pathData="{BLOB}"><aapt:attr name="android:fillColor">{vd_lin(*BLOB_GRAD, slime)}</aapt:attr></path>\n'
    out += f'  <path android:pathData="{circle(CX, CY, 15)}"><aapt:attr name="android:fillColor">{vd_rad(CX, CY, 15, [(0, argb(P["core"], 0.92)), (0.6, argb(P["core"], 0.55)), (1, argb(P["core"], 0))])}</aapt:attr></path>\n'
    for phase, color in zip(PHASES, P["arms"]):
        for scale, alpha in [(4.0, 0.1), (2.3, 0.28)]:  # glow
            d, _ = arm_points(phase, width_scale=scale)
            out += f'  <path android:pathData="{d}" android:fillColor="{argb(color, alpha)}"/>\n'
        for d, c, a in arm_segments(phase, color, P["hot"]):
            out += f'  <path android:pathData="{d}" android:fillColor="{argb(c, a)}"/>\n'
    out += f'  <path android:pathData="{circle(CX, CY, 8)}"><aapt:attr name="android:fillColor">{vd_rad(CX, CY, 8, [(0, argb(P["glow"], 0.6)), (1, argb(P["glow"], 0))])}</aapt:attr></path>\n'
    out += f'  <path android:pathData="{SPARK}"><aapt:attr name="android:fillColor">{vd_rad(CX, CY, 9.5 * S, [(0, "#FFFFFFFF"), (1, argb(P["spark_end"], 1))])}</aapt:attr></path>\n'
    for x, y, r in GLITTER:
        out += f'  <path android:pathData="{sparkle(x, y, r * 2.2)}" android:fillColor="{argb(P["glitter"], 0.95)}"/>\n'
    out += f'  <path android:pathData="{BLOB}" android:strokeWidth="1.2" android:strokeAlpha="0.55" android:strokeColor="{argb(P["rim"], 1)}"/>\n'
    out += f'  <path android:pathData="{SHINE}" android:fillColor="#C8FFFFFF"/>\n'
    out += f'  <path android:pathData="{circle(*SHINE_DOT)}" android:fillColor="#C8FFFFFF"/>\n'
    return out + "</vector>\n"


def mono_vd(color="#FF000000"):
    """The blob as a solid shape with the emblem and shine cut out, so it reads at any size."""
    cut = "".join(arm_points(p, width_scale=1.3)[0] for p in PHASES) + SPARK + SHINE
    return (HEAD + f'  <path android:pathData="{BLOB}{circle(CX, CY, 13)}" android:fillColor="{color}" android:fillType="evenOdd"/>\n'
            + f'  <path android:pathData="{cut}" android:fillColor="{color}"/>\n' + "</vector>\n")


def svg(P, size=432):
    top, bottom = P["bg"]
    neb, neb_a = P["nebula"]
    star, star_a = P["star"]
    stops = lambda st, op=1: "".join(f'<stop offset="{o}" stop-color="{c}" stop-opacity="{op}"/>' for o, c in st)
    o = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="{size}" height="{size}">', "<defs>",
         f'<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{top}"/><stop offset="1" stop-color="{bottom}"/></linearGradient>',
         f'<radialGradient id="neb" cx="54" cy="54" r="50" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{neb}" stop-opacity="{neb_a}"/><stop offset="1" stop-color="{neb}" stop-opacity="0"/></radialGradient>',
         f'<linearGradient id="slime" x1="{BLOB_GRAD[0]}" y1="{BLOB_GRAD[1]}" x2="{BLOB_GRAD[2]}" y2="{BLOB_GRAD[3]}" gradientUnits="userSpaceOnUse">{stops(P["slime"], 0.96)}</linearGradient>',
         f'<radialGradient id="core" cx="{CX}" cy="{CY}" r="15" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{P["core"]}" stop-opacity=".92"/>'
         f'<stop offset=".6" stop-color="{P["core"]}" stop-opacity=".55"/><stop offset="1" stop-color="{P["core"]}" stop-opacity="0"/></radialGradient>',
         f'<radialGradient id="glow" cx="{CX}" cy="{CY}" r="8" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="{P["glow"]}" stop-opacity=".6"/><stop offset="1" stop-color="{P["glow"]}" stop-opacity="0"/></radialGradient>',
         f'<radialGradient id="spark" cx="{CX}" cy="{CY}" r="{9.5 * S:.2f}" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#fff"/><stop offset="1" stop-color="{P["spark_end"]}"/></radialGradient>',
         "</defs>"]
    o += ['<rect width="108" height="108" fill="url(#bg)"/>', '<rect width="108" height="108" fill="url(#neb)"/>']
    o += [f'<circle cx="{x}" cy="{y}" r="{r}" fill="{star}" opacity="{a * star_a:.2f}"/>' for x, y, r, a in STARS]
    o += [f'<path d="{ellipse(*SHADOW)}" fill="#000" fill-opacity=".35"/>',
          f'<path d="{BLOB}" fill="none" stroke="{P["glow"]}" stroke-opacity=".18" stroke-width="5" stroke-linejoin="round"/>',
          f'<path d="{BLOB}" fill="url(#slime)"/>', f'<circle cx="{CX}" cy="{CY}" r="15" fill="url(#core)"/>']
    for phase, color in zip(PHASES, P["arms"]):
        for scale, alpha in [(4.0, 0.1), (2.3, 0.28)]:
            o.append(f'<path d="{arm_points(phase, width_scale=scale)[0]}" fill="{color}" fill-opacity="{alpha}"/>')
        o += [f'<path d="{d}" fill="{c}" fill-opacity="{a:.2f}"/>' for d, c, a in arm_segments(phase, color, P["hot"])]
    o += [f'<circle cx="{CX}" cy="{CY}" r="8" fill="url(#glow)"/>', f'<path d="{SPARK}" fill="url(#spark)"/>']
    o += [f'<path d="{sparkle(x, y, r * 2.2)}" fill="{P["glitter"]}" fill-opacity=".95"/>' for x, y, r in GLITTER]
    o += [f'<path d="{BLOB}" fill="none" stroke="{P["rim"]}" stroke-opacity=".55" stroke-width="1.2"/>',
          f'<path d="{SHINE}" fill="#fff" fill-opacity=".78"/>', f'<circle cx="{SHINE_DOT[0]}" cy="{SHINE_DOT[1]}" r="{SHINE_DOT[2]}" fill="#fff" fill-opacity=".78"/>', "</svg>"]
    return "\n".join(o) + "\n"


def alias_block(themes):
    """One launcher entry per theme; the app enables the one matching its theme (see Themes.kt)."""
    out = ["        <!-- ICON ALIASES (generated by art/gen_icon.py) -->"]
    for i, t in enumerate(themes):
        key = t["id"].replace("-", "_")
        out += [
            "        <activity-alias",
            f'            android:name=".Icon_{key}"',
            f'            android:enabled="{"true" if i == 0 else "false"}"',
            '            android:exported="true"',
            f'            android:icon="@mipmap/ic_launcher_{key}"',
            f'            android:roundIcon="@mipmap/ic_launcher_{key}"',
            '            android:targetActivity=".MainActivity">',
            "            <intent-filter>",
            '                <action android:name="android.intent.action.MAIN" />',
            '                <category android:name="android.intent.category.LAUNCHER" />',
            "            </intent-filter>",
            "        </activity-alias>",
        ]
    out.append("        <!-- END ICON ALIASES -->")
    return "\n".join(out)


def adaptive(key):
    return ('<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            f'    <background android:drawable="@drawable/ic_launcher_background{key}"/>\n'
            f'    <foreground android:drawable="@drawable/ic_launcher_foreground{key}"/>\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n</adaptive-icon>\n')


if __name__ == "__main__":
    themes = json.load(open("art/themes.json"))
    res = "app/src/main/res/"
    os.makedirs("art/themes", exist_ok=True)
    os.makedirs(res + "drawable", exist_ok=True)
    os.makedirs(res + "mipmap-anydpi-v26", exist_ok=True)
    for i, t in enumerate(themes):
        P = palette(t)
        key = t["id"].replace("-", "_")
        open(f"art/themes/{key}.svg", "w").write(svg(P))
        open(f"{res}drawable/ic_launcher_background_{key}.xml", "w").write(background_vd(P))
        open(f"{res}drawable/ic_launcher_foreground_{key}.xml", "w").write(foreground_vd(P))
        open(f"{res}mipmap-anydpi-v26/ic_launcher_{key}.xml", "w").write(adaptive(f"_{key}"))
        if i == 0:  # the first (default) theme is also the app's own icon
            open("art/slime-icon.svg", "w").write(svg(P))
            open(res + "drawable/ic_launcher_background.xml", "w").write(background_vd(P))
            open(res + "drawable/ic_launcher_foreground.xml", "w").write(foreground_vd(P))
            open(res + "mipmap-anydpi-v26/ic_launcher.xml", "w").write(adaptive(""))
            open(res + "mipmap-anydpi-v26/ic_launcher_round.xml", "w").write(adaptive(""))
    open(res + "drawable/ic_launcher_monochrome.xml", "w").write(mono_vd())
    # Status bar icon: the same shape, white, enlarged to fill the 24dp square.
    note = mono_vd("#FFFFFFFF").replace('android:width="108dp" android:height="108dp"', 'android:width="24dp" android:height="24dp"')
    note = note.replace("  <path", '  <group android:pivotX="54" android:pivotY="56" android:scaleX="1.6" android:scaleY="1.6">\n  <path', 1)
    open(res + "drawable/ic_notification.xml", "w").write(note.replace("</vector>", "  </group>\n</vector>"))
    manifest = "app/src/main/AndroidManifest.xml"
    if os.path.exists(manifest):
        m = open(manifest).read()
        m = re.sub(r"        <!-- ICON ALIASES.*?<!-- END ICON ALIASES -->", lambda _: alias_block(themes), m, flags=re.S)
        open(manifest, "w").write(m)
    print(f"logo written for {len(themes)} themes")
