"""The worlds behind the dish (painted backdrops, 1600x800), their shop thumbnails, and the title logo.

The 3D camera looks down at the dish, so the middle-bottom of each painting is mostly hidden by it: the scenery
lives in the top half and along the sides.
"""
import math
import random

import cairo

from artlib import *

W, H = 1600, 800


def sky(ctx, stops, w=W, h=H):
    g = cairo.LinearGradient(0, 0, 0, h)
    for t, col in stops:
        g.add_color_stop_rgb(t, *c(col))
    ctx.set_source(g)
    ctx.paint()


def hills(ctx, base_y, amp, col, col2=None, seed=1, freq=3.0, w=W, h=H, outline=None):
    rnd = random.Random(seed)
    ph = [rnd.random() * TAU for _ in range(3)]
    ctx.new_path()
    ctx.move_to(0, h)
    for i in range(0, w + 21, 20):
        x = i / w
        y = base_y + amp * (0.6 * math.sin(x * freq * TAU / 2 + ph[0]) + 0.3 * math.sin(x * freq * 1.9 * TAU / 2 + ph[1]) + 0.1 * math.sin(x * freq * 5 * TAU / 2 + ph[2]))
        ctx.line_to(i, y)
    ctx.line_to(w, h)
    ctx.close_path()
    g = cairo.LinearGradient(0, base_y - amp, 0, h)
    g.add_color_stop_rgb(0, *c(col))
    g.add_color_stop_rgb(1, *c(col2 or dark(col, 0.25)))
    if outline:
        ctx.set_source_rgba(*c(outline), 0.5)
        ctx.set_line_width(6)
        ctx.stroke_preserve()
    ctx.set_source(g)
    ctx.fill()


def cloud(ctx, x, y, s, col="#ffffff", alpha=0.95):
    shapes = [circle(x - 60 * s, y, 40 * s), circle(x - 10 * s, y - 28 * s, 54 * s), circle(x + 50 * s, y - 6 * s, 44 * s), rrect(x - 100 * s, y - 4 * s, 196 * s, 44 * s, 22 * s)]
    path(ctx, shapes)
    ctx.set_source_rgba(*c(col), alpha)
    ctx.fill()
    # A soft shadow along the bottom.
    ctx.save()
    path(ctx, shapes)
    ctx.clip()
    g = cairo.LinearGradient(0, y - 40 * s, 0, y + 40 * s)
    g.add_color_stop_rgba(0, 1, 1, 1, 0)
    g.add_color_stop_rgba(1, *dark(col, 0.15), 0.6)
    ctx.set_source(g)
    ctx.paint()
    ctx.restore()


def stars_field(ctx, n, seed, w=W, h=H, ymax=1.0, col=(1, 1, 1)):
    rnd = random.Random(seed)
    for _ in range(n):
        x, y = rnd.random() * w, rnd.random() * h * ymax
        r = rnd.random() ** 3 * 3 + 0.8
        ctx.arc(x, y, r, 0, TAU)
        ctx.set_source_rgba(*col, 0.4 + rnd.random() * 0.6)
        ctx.fill()
    for _ in range(n // 12):
        sparkle(ctx, rnd.random() * w, rnd.random() * h * ymax, 6 + rnd.random() * 10, col, 0.9)


def vignette(ctx, w=W, h=H, a=0.35):
    g = cairo.RadialGradient(w / 2, h * 0.55, h * 0.3, w / 2, h * 0.55, w * 0.7)
    g.add_color_stop_rgba(0, 0, 0, 0, 0)
    g.add_color_stop_rgba(1, 0.05, 0.02, 0.12, a)
    ctx.set_source(g)
    ctx.paint()


def tree(ctx, x, y, s, trunk="#8a5a3c", leaf="#ff9ec7"):
    flat(ctx, rrect(x - 10 * s, y - 80 * s, 20 * s, 90 * s, 8 * s), trunk)
    for dx, dy, r in ((-40, -110, 50), (30, -120, 56), (-5, -160, 60), (50, -80, 40), (-55, -70, 38)):
        blob(ctx, circle(x + dx * s, y + dy * s, r * s), leaf, outline=None, gloss=0.25, shine=False, shade=0.2)


def w_void(ctx, w=W, h=H):
    sky(ctx, [(0, "#1a0f3d"), (0.55, "#0b0820"), (1, "#05040d")], w, h)
    for x, y, r, col in ((0.25, 0.3, 0.35, "#a970ff"), (0.78, 0.22, 0.3, "#22e3ff"), (0.6, 0.6, 0.25, "#ff5fd2")):
        glow(ctx, x * w, y * h, r * w, col, 0.18)
    stars_field(ctx, 400, 3, w, h)


def w_meadow(ctx, w=W, h=H):
    sky(ctx, [(0, "#6ec8ff"), (0.55, "#bfeaff"), (1, "#e9fbff")], w, h)
    glow(ctx, w * 0.82, h * 0.18, h * 0.5, "#fff6c0", 0.7)
    blob(ctx, circle(w * 0.82, h * 0.18, h * 0.09), "#ffe066", outline=None, gloss=0.4, shine=False, shade=0.1)
    for x, y, s in ((0.12, 0.16, 1.2), (0.45, 0.1, 0.9), (0.62, 0.3, 0.7), (0.3, 0.34, 0.6)):
        cloud(ctx, x * w, y * h, s * w / 1600)
    hills(ctx, h * 0.52, h * 0.06, "#9be86a", "#5cbf4a", seed=4, freq=2.2, w=w, h=h)
    hills(ctx, h * 0.62, h * 0.05, "#7fd858", "#43a83a", seed=7, freq=3.0, w=w, h=h)
    rnd = random.Random(5)
    for _ in range(90):
        x, y = rnd.random() * w, h * (0.6 + rnd.random() * 0.4)
        col = rnd.choice(("#ffffff", "#ffe066", "#ff9ec7", "#c9a8ff"))
        s = w / 1600 * (0.6 + (y / h - 0.6) * 1.5)
        for k in range(5):
            a = k / 5 * TAU
            ctx.arc(x + math.cos(a) * 6 * s, y + math.sin(a) * 6 * s, 5 * s, 0, TAU)
            ctx.set_source_rgb(*c(col))
            ctx.fill()
        ctx.arc(x, y, 3.5 * s, 0, TAU)
        ctx.set_source_rgb(*c("#ffb14d"))
        ctx.fill()
    vignette(ctx, w, h, 0.2)


def w_candy(ctx, w=W, h=H):
    sky(ctx, [(0, "#ff9ed6"), (0.5, "#ffd1ec"), (1, "#fff0f8")], w, h)
    for x, y, s in ((0.15, 0.14, 1.0), (0.7, 0.1, 1.2), (0.45, 0.28, 0.7)):
        cloud(ctx, x * w, y * h, s * w / 1600, "#fff6fb")
    # Lollipops and candy canes on the hills.
    k = w / 1600
    for x, y, r, cols in ((0.08, 0.42, 70, ("#ff4d8d", "#ffffff")), (0.9, 0.38, 80, ("#22e3ff", "#ffffff")), (0.22, 0.5, 46, ("#a970ff", "#ffe066")), (0.78, 0.5, 50, ("#6ef2b4", "#ffffff"))):
        X, Y, R = x * w, y * h, r * k
        line(ctx, [(X, Y), (X, Y + R * 3.4)], "#ffffff", 14 * k)
        blob(ctx, circle(X, Y, R), cols[0], outline=None, gloss=0.4, shine=False)
        ctx.save()
        circle(X, Y, R)(ctx)
        ctx.clip()
        for i in range(60):
            a = i * 0.35
            rr = R * i / 60
            ctx.arc(X + math.cos(a) * rr, Y + math.sin(a) * rr, R * 0.07, 0, TAU)
            ctx.set_source_rgb(*c(cols[1]))
            ctx.fill()
        ctx.restore()
    hills(ctx, h * 0.6, h * 0.05, "#ffb3e0", "#ff7ec4", seed=2, freq=2.5, w=w, h=h)
    hills(ctx, h * 0.7, h * 0.04, "#d9b8ff", "#b48aff", seed=9, freq=3.5, w=w, h=h)
    rnd = random.Random(8)
    for _ in range(120):
        x, y = rnd.random() * w, h * (0.68 + rnd.random() * 0.32)
        a = rnd.random() * TAU
        line(ctx, [(x, y), (x + math.cos(a) * 14 * k, y + math.sin(a) * 14 * k)], rnd.choice(("#ff4d6d", "#ffe066", "#22e3ff", "#ffffff", "#6ef2b4")), 7 * k)


def w_ocean(ctx, w=W, h=H):
    sky(ctx, [(0, "#6fe0ff"), (0.4, "#1e8fd8"), (1, "#0b3b7a")], w, h)
    for i in range(7):
        x = w * (0.1 + i * 0.14)
        ctx.new_path()
        ctx.move_to(x - 40, 0)
        ctx.line_to(x + 60, 0)
        ctx.line_to(x + 200, h)
        ctx.line_to(x + 20, h)
        ctx.close_path()
        g = cairo.LinearGradient(0, 0, 0, h)
        g.add_color_stop_rgba(0, 1, 1, 1, 0.22)
        g.add_color_stop_rgba(1, 1, 1, 1, 0)
        ctx.set_source(g)
        ctx.fill()
    k = w / 1600
    rnd = random.Random(11)
    for x0 in (0.04, 0.12, 0.86, 0.95, 0.24, 0.75):
        X = x0 * w
        col = rnd.choice(("#3ddc84", "#2bb673", "#6ef2b4"))
        for j in range(2):
            pts = [(X + j * 18 * k + math.sin(t * 0.8 + j) * 22 * k, h - t * 26 * k) for t in range(16 + rnd.randint(0, 8))]
            line(ctx, pts, col, 22 * k)
    hills(ctx, h * 0.78, h * 0.03, "#ffe0a6", "#e8b56a", seed=3, freq=2, w=w, h=h)
    for x, col in ((0.18, "#ff7eb6"), (0.82, "#ff9f1c"), (0.3, "#c9a8ff")):
        X, Y = x * w, h * 0.8
        for a in range(5):
            ang = -math.pi / 2 + (a - 2) * 0.45
            line(ctx, [(X, Y), (X + math.cos(ang) * 70 * k, Y + math.sin(ang) * 70 * k)], col, 20 * k)
    for _ in range(40):
        x, y, r = rnd.random() * w, rnd.random() * h * 0.8, (2 + rnd.random() * 10) * k
        arc_line(ctx, x, y, r, 0, TAU, "#ffffff", 2.5 * k, 0.6)
        ctx.arc(x - r * 0.35, y - r * 0.35, r * 0.25, 0, TAU)
        ctx.set_source_rgba(1, 1, 1, 0.7)
        ctx.fill()
    # A couple of little fish.
    for x, y, col, d in ((0.15, 0.25, "#ffe066", 1), (0.88, 0.32, "#ff7eb6", -1), (0.7, 0.15, "#ff9f1c", -1)):
        X, Y = x * w, y * h
        blob(ctx, [ellipse(X, Y, 34 * k, 22 * k), poly([(X - d * 26 * k, Y), (X - d * 58 * k, Y - 20 * k), (X - d * 58 * k, Y + 20 * k)])], col, outline=INK, ow=4 * k, shine=False)
        ctx.arc(X + d * 16 * k, Y - 4 * k, 5 * k, 0, TAU)
        ctx.set_source_rgb(*INK)
        ctx.fill()


def w_sakura(ctx, w=W, h=H):
    sky(ctx, [(0, "#ffc2dc"), (0.5, "#ffe3ee"), (1, "#fff6f0")], w, h)
    k = w / 1600
    glow(ctx, w * 0.5, h * 0.2, h * 0.5, "#ffffff", 0.6)
    # A far mountain with snow.
    blob(ctx, smooth([(w * 0.3, h * 0.6), (w * 0.47, h * 0.18), (w * 0.53, h * 0.18), (w * 0.72, h * 0.6)], 0.25), "#c9b6e8", outline=None, gloss=0, shine=False, shade=0.1)
    blob(ctx, smooth([(w * 0.43, h * 0.27), (w * 0.47, h * 0.18), (w * 0.53, h * 0.18), (w * 0.57, h * 0.27), (w * 0.53, h * 0.3), (w * 0.5, h * 0.26), (w * 0.47, h * 0.3)], 0.3), "#ffffff", outline=None, gloss=0, shine=False, shade=0)
    hills(ctx, h * 0.6, h * 0.04, "#b9e3a0", "#8fc979", seed=6, freq=2, w=w, h=h)
    for x, y, s in ((0.06, 0.75, 2.0), (0.94, 0.72, 2.2), (0.2, 0.66, 1.2), (0.8, 0.64, 1.1)):
        tree(ctx, x * w, y * h, s * k, "#7a4a3a", "#ffa6c9")
    rnd = random.Random(12)
    for _ in range(70):
        x, y = rnd.random() * w, rnd.random() * h
        blob(ctx, ellipse(x, y, 9 * k, 6 * k, rnd.random() * TAU), "#ffb6d5", outline=None, gloss=0.3, shine=False, shade=0.1, alpha=0.9)


def w_snow(ctx, w=W, h=H):
    sky(ctx, [(0, "#2a3c8f"), (0.5, "#6a7fd0"), (1, "#c7d6ff")], w, h)
    k = w / 1600
    stars_field(ctx, 120, 4, w, h, ymax=0.4)
    blob(ctx, crescent(w * 0.8, h * 0.16, 50 * k, 0.6, rot=-0.6), "#fff6d8", outline=None, gloss=0.2, shine=False, shade=0.05)
    for (x0, x1, top, col) in ((0.0, 0.45, 0.22, "#8fa3e8"), (0.3, 0.8, 0.14, "#a9bbf2"), (0.6, 1.05, 0.26, "#8fa3e8")):
        X0, X1, T = x0 * w, x1 * w, top * h
        mid = (X0 + X1) / 2
        blob(ctx, poly([(X0, h * 0.62), (mid, T), (X1, h * 0.62)]), col, outline=None, gloss=0, shine=False, shade=0.15)
        blob(ctx, poly([(mid - (mid - X0) * 0.3, T + (h * 0.62 - T) * 0.3), (mid, T), (mid + (X1 - mid) * 0.3, T + (h * 0.62 - T) * 0.3),
                        (mid + (X1 - mid) * 0.12, T + (h * 0.62 - T) * 0.38), (mid, T + (h * 0.62 - T) * 0.28), (mid - (mid - X0) * 0.14, T + (h * 0.62 - T) * 0.4)]), "#ffffff", outline=None, gloss=0, shine=False, shade=0)
    hills(ctx, h * 0.62, h * 0.04, "#ffffff", "#dbe6ff", seed=5, freq=2, w=w, h=h)
    for x, y, s in ((0.06, 0.8, 1.6), (0.14, 0.74, 1.1), (0.9, 0.78, 1.7), (0.82, 0.7, 1.0)):
        X, Y, S = x * w, y * h, s * k
        for i in range(3):
            blob(ctx, poly([(X - (70 - i * 15) * S, Y - i * 50 * S), (X, Y - (90 + i * 50) * S), (X + (70 - i * 15) * S, Y - i * 50 * S)]), "#3f8f6a", outline=None, gloss=0.1, shine=False)
            flat(ctx, poly([(X - (30 - i * 6) * S, Y - (60 + i * 50) * S), (X, Y - (90 + i * 50) * S), (X + (30 - i * 6) * S, Y - (60 + i * 50) * S)]), "#ffffff", 0.9)
    rnd = random.Random(13)
    for _ in range(160):
        ctx.arc(rnd.random() * w, rnd.random() * h, (1.5 + rnd.random() * 4) * k, 0, TAU)
        ctx.set_source_rgba(1, 1, 1, 0.8)
        ctx.fill()


def w_beach(ctx, w=W, h=H):
    sky(ctx, [(0, "#5a3da8"), (0.3, "#ff6b9a"), (0.55, "#ffb36b"), (0.62, "#ffe0a0")], w, h)
    k = w / 1600
    glow(ctx, w * 0.5, h * 0.52, h * 0.6, "#ffe9a8", 0.6)
    ctx.save()
    ctx.rectangle(0, 0, w, h * 0.56)
    ctx.clip()
    blob(ctx, circle(w * 0.5, h * 0.52, h * 0.16), "#ffd36b", outline=None, gloss=0.3, shine=False, shade=0.05)
    ctx.restore()
    for x, y, s in ((0.15, 0.2, 0.9), (0.82, 0.14, 1.1)):
        cloud(ctx, x * w, y * h, s * k, "#ffc2d6", 0.8)
    g = cairo.LinearGradient(0, h * 0.55, 0, h)
    g.add_color_stop_rgb(0, *c("#ff8fb0"))
    g.add_color_stop_rgb(0.3, *c("#5a6fd8"))
    g.add_color_stop_rgb(1, *c("#2a3c8f"))
    ctx.rectangle(0, h * 0.55, w, h)
    ctx.set_source(g)
    ctx.fill()
    for i in range(14):
        y = h * (0.57 + i * 0.03)
        x = w * 0.5 + math.sin(i * 1.7) * 40 * k
        line(ctx, [(x - (60 + i * 18) * k, y), (x + (60 + i * 18) * k, y)], "#ffe9a8", 4 * k, 0.6 - i * 0.035)
    hills(ctx, h * 0.84, h * 0.02, "#ffe0a6", "#e8b56a", seed=1, freq=1.5, w=w, h=h)
    for X, lean in ((w * 0.07, 0.25), (w * 0.93, -0.25)):
        pts = [(X + math.sin(t / 10 * 1.2) * lean * 160 * k, h * 0.9 - t * 40 * k) for t in range(11)]
        line(ctx, pts, "#7a4a3a", 24 * k)
        tx, ty = pts[-1]
        for a in (-2.6, -2.0, -1.2, -0.5, 0.1, -1.6):
            ex, ey = tx + math.cos(a) * 150 * k, ty + math.sin(a) * 60 * k + 60 * k
            curve(ctx, (tx, ty), (tx + math.cos(a) * 60 * k, ty - 50 * k), (ex - math.cos(a) * 20 * k, ey - 40 * k), (ex, ey), "#2f8a5a", 26 * k)


def w_space(ctx, w=W, h=H):
    sky(ctx, [(0, "#090620"), (0.6, "#1a0f40"), (1, "#2a1060")], w, h)
    k = w / 1600
    for x, y, r, col in ((0.2, 0.4, 0.3, "#ff5fd2"), (0.7, 0.3, 0.35, "#5a6bff"), (0.5, 0.7, 0.3, "#22e3ff")):
        glow(ctx, x * w, y * h, r * w, col, 0.22)
    stars_field(ctx, 500, 7, w, h)
    # Planets.
    blob(ctx, circle(w * 0.14, h * 0.26, 90 * k), "#ff9f5a", outline=None, gloss=0.3, shine=False, shade=0.45)
    ctx.save()
    circle(w * 0.14, h * 0.26, 90 * k)(ctx)
    ctx.clip()
    for i in range(4):
        flat(ctx, ellipse(w * 0.14, h * 0.26 - 60 * k + i * 36 * k, 120 * k, 9 * k, 0.15), "#ffd0a0", 0.5)
    ctx.restore()
    for front in (False,):
        ctx.save()
        ctx.translate(w * 0.86, h * 0.2)
        ctx.rotate(-0.3)
        ctx.scale(1, 0.3)
        arc_line(ctx, 0, 0, 120 * k, 0, TAU, "#ffe9a8", 14 * k, 0.8)
        ctx.restore()
    blob(ctx, circle(w * 0.86, h * 0.2, 62 * k), "#9f8aff", outline=None, gloss=0.3, shine=False, shade=0.45)
    ctx.save()
    ctx.translate(w * 0.86, h * 0.2)
    ctx.rotate(-0.3)
    ctx.scale(1, 0.3)
    arc_line(ctx, 0, 0, 120 * k, 0, math.pi, "#ffe9a8", 14 * k, 0.9)
    ctx.restore()
    blob(ctx, circle(w * 0.7, h * 0.12, 22 * k), "#6ef2b4", outline=None, gloss=0.4, shine=False, shade=0.4)
    # A tiny rocket.
    ctx.save()
    ctx.translate(w * 0.3, h * 0.14)
    ctx.rotate(0.9)
    ctx.scale(k, k)
    flat(ctx, poly([(-14, 30), (0, 60), (14, 30)]), "#ffb14d")
    blob(ctx, smooth([(0, -46), (18, -10), (16, 30), (-16, 30), (-18, -10)], 0.4), "#ffffff", outline=INK, ow=4, shine=False)
    blob(ctx, circle(0, -6, 8), "#22e3ff", outline=INK, ow=3, shine=False)
    ctx.restore()


def w_neon(ctx, w=W, h=H):
    sky(ctx, [(0, "#12052e"), (0.45, "#3a0a5e"), (0.6, "#ff2bd6"), (0.62, "#1a0630"), (1, "#0a0218")], w, h)
    k = w / 1600
    stars_field(ctx, 160, 9, w, h, ymax=0.45)
    ctx.save()
    ctx.rectangle(0, 0, w, h * 0.6)
    ctx.clip()
    g = cairo.LinearGradient(0, h * 0.3, 0, h * 0.6)
    g.add_color_stop_rgb(0, *c("#ffe066"))
    g.add_color_stop_rgb(1, *c("#ff2bd6"))
    ctx.arc(w * 0.5, h * 0.52, h * 0.2, 0, TAU)
    ctx.set_source(g)
    ctx.fill()
    for i in range(6):
        ctx.rectangle(w * 0.3, h * (0.44 + i * 0.025), w * 0.4, (2 + i * 1.6) * k)
        ctx.set_source_rgb(*c("#3a0a5e"))
        ctx.fill()
    ctx.restore()
    rnd = random.Random(21)
    x = 0
    while x < w:
        bw = (50 + rnd.random() * 90) * k
        bh = h * (0.12 + rnd.random() * 0.22)
        if abs(x + bw / 2 - w / 2) > w * 0.16:
            flat(ctx, rrect(x, h * 0.6 - bh, bw - 6 * k, bh + 2, 4 * k), "#1d0838")
            for wy in range(int(bh // (22 * k))):
                for wx in range(int((bw - 20 * k) // (18 * k))):
                    if rnd.random() < 0.45:
                        flat(ctx, rrect(x + 8 * k + wx * 18 * k, h * 0.6 - bh + 10 * k + wy * 22 * k, 9 * k, 10 * k, 2 * k), rnd.choice(("#22e3ff", "#ff2bd6", "#ffe066")), 0.8)
        x += bw
    for i in range(16):
        t = i / 15
        line(ctx, [(w * 0.5 + (t - 0.5) * w * 0.3, h * 0.62), (w * 0.5 + (t - 0.5) * w * 2.4, h)], "#ff2bd6", 3 * k, 0.8)
    for i in range(8):
        y = h * 0.62 + (h * 0.38) * (i / 8) ** 1.7
        line(ctx, [(0, y), (w, y)], "#22e3ff", 3 * k, 0.7)


WORLDS = {"void": w_void, "meadow": w_meadow, "candy": w_candy, "ocean": w_ocean, "sakura": w_sakura,
          "snow": w_snow, "beach": w_beach, "space": w_space, "neon": w_neon}


def logo():
    """The title: chunky rounded letters, glossy slime colours, a drip or two and a slime friend."""
    w, h = 1120, 500
    surf, ctx = canvas(w, h)
    family = "DejaVu Sans"
    ctx.select_font_face(family, cairo.FONT_SLANT_NORMAL, cairo.FONT_WEIGHT_BOLD)

    def word(s, x, y, size, top, bottom, extra=()):
        ctx.set_font_size(size)
        ctx.new_path()
        ctx.move_to(x, y)
        ctx.text_path(s)
        p = ctx.copy_path()
        ctx.new_path()
        for e in extra:
            e(ctx)
        pe = ctx.copy_path()
        g = cairo.LinearGradient(0, y - size * 0.8, 0, y)
        g.add_color_stop_rgb(0, *c(top))
        g.add_color_stop_rgb(1, *c(bottom))
        for q in (p, pe):
            ctx.new_path()
            ctx.append_path(q)
            ctx.set_source_rgb(*INK)
            ctx.set_line_width(size * 0.26)
            ctx.stroke()
        for q in (pe, p):
            ctx.new_path()
            ctx.append_path(q)
            ctx.set_source(g if q is p else cairo.SolidPattern(*c(bottom)))
            ctx.set_line_width(size * 0.11)
            ctx.stroke_preserve()
            ctx.fill()
        # Gloss across the top half of the letters.
        ctx.save()
        ctx.append_path(p)
        ctx.clip()
        ctx.rectangle(0, y - size * 0.78, w, size * 0.22)
        ctx.set_source_rgba(1, 1, 1, 0.35)
        ctx.fill()
        ctx.restore()
        return ctx.text_extents(s)

    word("VOID", 330, 190, 170, "#c9a8ff", "#7a2bd8")
    # SLIME drips off its bottom edge.
    drips = []
    for x, l in ((388, 58), (566, 38), (706, 66), (930, 44)):
        drips += [capsule(x, 380, x, 384 + l, 10), circle(x, 384 + l, 15)]
    word("SLIME", 300, 400, 220, "#9dffcf", "#14b87a", drips)
    # The slime friend.
    blob(ctx, slime_dome(150, 340, 118, 112), "#ff7eb6", outline=INK, ow=12, gloss=0.6)
    face(ctx, 150, 284, 88, "open")
    sparkle(ctx, 1010, 70, 40, "#ffe066")
    sparkle(ctx, 1070, 160, 22, "#22e3ff")
    sparkle(ctx, 70, 90, 26, "#ffffff")
    return surf


def main():
    for wid, fn in WORLDS.items():
        surf, ctx = canvas(W, H)
        fn(ctx)
        save(surf, "bg_" + wid, shadow=False, quality=84)
        # Thumbnails: the same painting, cropped a little and with rounded corners.
        def thumb(ctx2, fn=fn):
            rrect(0, 0, 384, 216, 30)(ctx2)
            ctx2.clip()
            ctx2.scale(384 / (W * 0.8), 216 / (H * 0.8))
            ctx2.translate(-W * 0.1, -H * 0.05)
            fn(ctx2)
        save_img(pixel(thumb, 384, 216, grid=128, outline=False, shadow=False), "ic_world_" + wid)
    save(logo(), "logo", shadow=True, quality=92)


if __name__ == "__main__":
    main()
