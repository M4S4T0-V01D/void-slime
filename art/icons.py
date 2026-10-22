"""The game's icons: UI, tools and toys, bases, mix-ins, glitter, dishes, plate and jelly shapes.

Each entry of ICONS is name -> function(ctx) drawing on the 192 px canvas. gen_art.py renders them.
"""
import math

import cairo

from artlib import *

ICONS = {}


def icon(name):
    def reg(fn):
        ICONS[name] = fn
        return fn
    return reg


# ---- UI ----------------------------------------------------------------------------------------------------

def coin_at(ctx, x, y, r):
    blob(ctx, circle(x, y, r), "#ffc93c", outline="#8a4b00", ow=r * 0.09, gloss=0.5, shade=0.3)
    arc_line(ctx, x, y, r * 0.74, 0, TAU, "#e89a10", r * 0.09)
    # A little slime drop stamped in the middle.
    blob(ctx, drop(x, y + r * 0.06, r * 0.42), "#ffe27a", outline="#c27400", ow=r * 0.06, gloss=0.6, shine=False, shade=0.1)


@icon("coin")
def _coin(ctx):
    coin_at(ctx, 96, 96, 78)
    sparkle(ctx, 150, 40, 18)


@icon("coins")
def _coins(ctx):
    coin_at(ctx, 70, 118, 50)
    coin_at(ctx, 122, 104, 54)
    coin_at(ctx, 98, 62, 46)
    sparkle(ctx, 160, 40, 16)


@icon("lock")
def _lock(ctx):
    ctx.new_path()
    ctx.arc(96, 82, 38, math.pi, 0)
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(34)
    ctx.stroke()
    ctx.arc(96, 82, 38, math.pi, 0)
    ctx.set_source_rgb(*c("#b7b3c9"))
    ctx.set_line_width(16)
    ctx.stroke()
    blob(ctx, rrect(36, 80, 120, 92, 24), "#ffb43c", outline=INK, ow=8)
    blob(ctx, circle(96, 118, 13), "#6b3a00", outline=None, gloss=0, shine=False)
    flat(ctx, capsule(96, 120, 96, 146, 7), "#6b3a00")


@icon("star")
def _star(ctx):
    blob(ctx, star(96, 100, 84, 40, round_=10), "#ffd23f", outline="#a35a00", ow=8)


@icon("star_off")
def _star_off(ctx):
    blob(ctx, star(96, 100, 84, 40, round_=10), "#5d5672", outline="#2a2438", ow=8, gloss=0.25, shine=False)


@icon("level")
def _level(ctx):
    blob(ctx, star(96, 98, 86, 42, round_=10), "#a970ff", outline=INK, ow=8)
    sparkle(ctx, 152, 40, 16)


@icon("gift")
def _gift(ctx):
    blob(ctx, rrect(32, 84, 128, 86, 14), "#ff5fa2", outline=INK, ow=8)
    blob(ctx, rrect(24, 64, 144, 36, 12), "#ff7fb6", outline=INK, ow=8)
    flat(ctx, rrect(84, 64, 24, 106, 4), "#ffe066")
    flat(ctx, rrect(84, 64, 24, 106, 4), "#ffe066")
    # The bow.
    blob(ctx, [ellipse(70, 52, 26, 16, -0.5), ellipse(122, 52, 26, 16, 0.5)], "#ffe066", outline=INK, ow=7, shine=False)
    blob(ctx, circle(96, 58, 13), "#ffd23f", outline=INK, ow=7, shine=False)


@icon("bulb")
def _bulb(ctx):
    glow(ctx, 96, 80, 90, "#fff3a0", 0.55)
    blob(ctx, [circle(96, 78, 52), rrect(72, 90, 48, 50, 10)], "#ffe45c", outline=INK, ow=8)
    blob(ctx, rrect(70, 136, 52, 34, 10), "#9a96b0", outline=INK, ow=7, shine=False)
    line(ctx, [(74, 148), (118, 148)], INK, 5)
    line(ctx, [(76, 159), (116, 159)], INK, 5)
    for a in range(7):
        ang = math.pi + a * math.pi / 6
        line(ctx, [(96 + math.cos(ang) * 70, 78 + math.sin(ang) * 70), (96 + math.cos(ang) * 84, 78 + math.sin(ang) * 84)], "#ffe45c", 7)


@icon("moon")
def _moon(ctx):
    blob(ctx, crescent(92, 96, 72, 0.6, rot=-0.6), "#ffd86b", outline="#7a4a00", ow=7)
    sparkle(ctx, 150, 54, 16, "#fff6c8")
    sparkle(ctx, 136, 142, 10, "#fff6c8")


@icon("ballup")
def _ballup(ctx):
    blob(ctx, circle(96, 104, 56), "#a970ff", outline=INK, ow=8)
    face(ctx, 96, 104, 40)
    for sx in (-1, 1):
        ctx.new_path()
        ctx.arc(96, 104, 80, math.pi * (1.5 + 0.38 * sx) - 0.5, math.pi * (1.5 + 0.38 * sx) + 0.5)
        ctx.set_source_rgb(*c("#22e3ff"))
        ctx.set_line_width(10)
        ctx.stroke()


@icon("zen")
def _zen(ctx):
    # A lotus.
    for a, col in ((-1.0, "#ff9ec7"), (1.0, "#ff9ec7"), (-0.5, "#ffb6d5"), (0.5, "#ffb6d5"), (0, "#ffd1e6")):
        ctx.save()
        ctx.translate(96, 140)
        ctx.rotate(a * 0.85)
        blob(ctx, drop(0, -48, 40), col, outline=INK, ow=7, shine=False)
        ctx.restore()
    blob(ctx, ellipse(96, 148, 66, 16), "#6ef2b4", outline=INK, ow=7, gloss=0.3, shine=False)


@icon("dice")
def _dice(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(-0.18)
    blob(ctx, rrect(-62, -62, 124, 124, 28), "#ffffff", outline=INK, ow=8, shade=0.18)
    for x, y in ((-30, -30), (30, 30), (0, 0), (30, -30), (-30, 30)):
        flat(ctx, circle(x, y, 12), "#ff4d8d")
    ctx.restore()


@icon("jar")
def _jar(ctx):
    jar_at(ctx, 96, 100, 1.0, "#a970ff")


def jar_at(ctx, x, y, s, slime, lid="#ff7eb6"):
    blob(ctx, rrect(x - 56 * s, y - 50 * s, 112 * s, 116 * s, 30 * s), "#cfe9ff", outline=INK, ow=7 * s, gloss=0, shine=False, shade=0.1, alpha=0.9)
    blob(ctx, rrect(x - 46 * s, y - 6 * s, 92 * s, 62 * s, 22 * s), slime, outline=None, gloss=0.4, shine=False)
    face(ctx, x, y + 22 * s, 30 * s)
    # Glass shine.
    flat(ctx, capsule(x - 38 * s, y - 34 * s, x - 38 * s, y + 30 * s, 5 * s), "#ffffff", 0.7)
    blob(ctx, rrect(x - 60 * s, y - 72 * s, 120 * s, 30 * s, 12 * s), lid, outline=INK, ow=7 * s, shine=False)


@icon("trash")
def _trash(ctx):
    blob(ctx, poly([(46, 62), (146, 62), (134, 170), (58, 170)]), "#ff6b81", outline=INK, ow=8)
    for x in (76, 96, 116):
        line(ctx, [(x, 84), (x - (96 - x) * 0.1, 150)], dark("#ff6b81", 0.35), 7)
    blob(ctx, rrect(34, 40, 124, 26, 10), "#ff8fa3", outline=INK, ow=8, shine=False)
    blob(ctx, rrect(78, 24, 36, 22, 8), "#ff8fa3", outline=INK, ow=7, shine=False, gloss=0)


@icon("check")
def _check(ctx):
    blob(ctx, circle(96, 96, 78), "#3ddc84", outline="#0d5a32", ow=8)
    line(ctx, [(56, 98), (84, 126), (138, 68)], "#0d5a32", 30)
    line(ctx, [(56, 98), (84, 126), (138, 68)], "#ffffff", 16)


@icon("timer")
def _timer(ctx):
    blob(ctx, rrect(82, 14, 28, 22, 6), "#ff8a3d", outline=INK, ow=6, shine=False)
    blob(ctx, circle(96, 106, 70), "#ffffff", outline=INK, ow=8, shade=0.15)
    blob(ctx, circle(96, 106, 56), "#e9f7ff", outline=None, gloss=0, shine=False)
    ctx.new_path()
    ctx.move_to(96, 106)
    ctx.arc(96, 106, 50, -math.pi / 2, math.pi * 0.2)
    ctx.close_path()
    ctx.set_source_rgba(*c("#22e3ff"), 0.6)
    ctx.fill()
    line(ctx, [(96, 106), (96, 66)], INK, 9)
    line(ctx, [(96, 106), (124, 118)], INK, 9)
    flat(ctx, circle(96, 106, 9), "#ff4d6d")


@icon("trophy")
def _trophy(ctx):
    for sx in (-1, 1):
        arc_line(ctx, 96 + sx * 54, 70, 22, 0, TAU, INK, 18)
        arc_line(ctx, 96 + sx * 54, 70, 22, 0, TAU, "#ffc93c", 8)
    blob(ctx, smooth([(40, 36), (152, 36), (146, 84), (120, 116), (72, 116), (46, 84)], 0.3), "#ffc93c", outline="#7a4300", ow=8)
    blob(ctx, rrect(84, 112, 24, 30, 4), "#e8a21a", outline="#7a4300", ow=7, shine=False)
    blob(ctx, rrect(52, 138, 88, 34, 10), "#a970ff", outline=INK, ow=8, shine=False)
    blob(ctx, star(96, 72, 22, 10, round_=3), "#fff3b0", outline=None, shine=False, gloss=0)


@icon("target")
def _target(ctx):
    for r, col in ((80, "#ff4d6d"), (60, "#ffffff"), (40, "#ff4d6d"), (20, "#ffffff")):
        blob(ctx, circle(96, 96, r), col, outline=INK if r == 80 else None, ow=8, gloss=0.3 if r == 80 else 0, shine=False)
    # A dart in the middle.
    line(ctx, [(96, 96), (160, 32)], INK, 12)
    line(ctx, [(96, 96), (160, 32)], "#ffe066", 5)
    blob(ctx, poly([(150, 26), (176, 18), (168, 44)]), "#22e3ff", outline=INK, ow=5, shine=False)


@icon("bell")
def _bell(ctx):
    blob(ctx, rrect(18, 140, 156, 22, 10), "#c7c2dd", outline=INK, ow=8, shine=False)
    ctx.new_path()
    ctx.arc(96, 140, 66, math.pi, 0)
    ctx.close_path()
    blob(ctx, lambda cx: (cx.new_sub_path(), cx.arc(96, 138, 66, math.pi, 0), cx.close_path()), "#ffc93c", outline=INK, ow=8)
    blob(ctx, circle(96, 64, 12), "#ffc93c", outline=INK, ow=7, shine=False)


@icon("basket")
def _basket(ctx):
    arc_line(ctx, 96, 88, 56, math.pi, 0, INK, 18)
    arc_line(ctx, 96, 88, 56, math.pi, 0, "#c88a4a", 8)
    # Things peeking out.
    blob(ctx, circle(70, 84, 22), "#ff7eb6", outline=INK, ow=6, shine=False)
    blob(ctx, circle(112, 80, 24), "#22e3ff", outline=INK, ow=6, shine=False)
    blob(ctx, star(92, 74, 22, 10, round_=3), "#ffe066", outline=INK, ow=6, shine=False)
    blob(ctx, poly([(26, 92), (166, 92), (148, 168), (44, 168)]), "#e0a060", outline=INK, ow=8, gloss=0.3)
    for y in (112, 134, 154):
        line(ctx, [(40, y), (152, y)], dark("#e0a060", 0.3), 5)


@icon("ball")
def _ball(ctx):
    blob(ctx, circle(96, 96, 76), "#ffffff", outline=INK, ow=8)
    ctx.save()
    circle(96, 96, 76)(ctx)
    ctx.clip()
    for i, col in enumerate(("#ff4d6d", "#ffe066", "#22e3ff")):
        ctx.save()
        ctx.translate(96, 96)
        ctx.rotate(i * TAU / 3 + 0.3)
        ctx.new_path()
        ctx.move_to(0, 0)
        ctx.arc(0, 0, 80, -0.5, 0.5)
        ctx.close_path()
        ctx.set_source_rgba(*c(col), 0.95)
        ctx.fill()
        ctx.restore()
    ctx.restore()
    blob(ctx, circle(96, 96, 76), "#ffffff", outline=INK, ow=0, alpha=0.0)
    blob(ctx, circle(96, 96, 14), "#ffffff", outline=INK, ow=5, shine=False)
    # Gloss on top of the stripes.
    ctx.save()
    ctx.translate(70, 60)
    ctx.scale(30, 16)
    ctx.arc(0, 0, 1, 0, TAU)
    ctx.restore()
    ctx.set_source_rgba(1, 1, 1, 0.55)
    ctx.fill()


@icon("dish")
def _dish(ctx):
    dish_at(ctx, 96, 112, 1.0, "#cfc8ff")


def dish_at(ctx, x, y, s, col, rim=None, shape=None):
    col = c(col)
    rim = c(rim) if rim else light(col, 0.35)
    blob(ctx, ellipse(x, y + 10 * s, 82 * s, 34 * s), dark(col, 0.25), outline=INK, ow=8 * s, gloss=0, shine=False)
    blob(ctx, ellipse(x, y, 84 * s, 34 * s), rim, outline=INK, ow=8 * s, gloss=0.4, shine=False)
    blob(ctx, ellipse(x, y + 3 * s, 64 * s, 23 * s), col, outline=None, gloss=0, shine=False, shade=0.0)
    ctx.save()
    ctx.translate(x - 30 * s, y - 14 * s)
    ctx.scale(20 * s, 5 * s)
    ctx.arc(0, 0, 1, 0, TAU)
    ctx.restore()
    ctx.set_source_rgba(1, 1, 1, 0.6)
    ctx.fill()


@icon("palette")
def _palette(ctx):
    blob(ctx, smooth([(30, 96), (60, 36), (130, 30), (170, 70), (166, 120), (128, 132), (118, 160), (70, 166), (34, 140)], 0.5), "#ffe2b8", outline=INK, ow=8)
    flat(ctx, circle(124, 112, 13), "#3b2050")
    for (x, y), col in zip(((64, 78), (98, 56), (138, 70), (66, 124), (98, 140)), ("#ff4d6d", "#ffe066", "#22e3ff", "#a970ff", "#6ef2b4")):
        blob(ctx, circle(x, y, 15), col, outline=INK, ow=4, shine=False, gloss=0.5)


@icon("flask")
def _flask(ctx):
    flask_at(ctx, 96, 100, 1.0, "#6ef2b4")


def flask_at(ctx, x, y, s, liquid):
    body = poly([(x - 18 * s, y - 70 * s), (x + 18 * s, y - 70 * s), (x + 18 * s, y - 30 * s), (x + 66 * s, y + 58 * s),
                 (x - 66 * s, y + 58 * s), (x - 18 * s, y - 30 * s)])
    body_r = rounded_poly([(x - 18 * s, y - 70 * s), (x + 18 * s, y - 70 * s), (x + 18 * s, y - 30 * s), (x + 66 * s, y + 58 * s),
                           (x - 66 * s, y + 58 * s), (x - 18 * s, y - 30 * s)], 14 * s)
    blob(ctx, body_r, "#e6f6ff", outline=INK, ow=8 * s, gloss=0, shine=False, shade=0.08, alpha=0.95)
    ctx.save()
    path(ctx, [body_r])
    ctx.clip()
    blob(ctx, smooth([(x - 90 * s, y + 6 * s), (x - 40 * s, y - 2 * s), (x, y + 8 * s), (x + 40 * s, y - 4 * s),
                      (x + 90 * s, y + 4 * s), (x + 90 * s, y + 80 * s), (x - 90 * s, y + 80 * s)], 0.4), liquid, outline=None, gloss=0.2, shine=False)
    ctx.restore()
    face(ctx, x, y + 32 * s, 30 * s, "open")
    for bx, by, br in ((x + 4 * s, y - 28 * s, 7), (x - 8 * s, y - 50 * s, 5), (x + 10 * s, y - 64 * s, 4)):
        blob(ctx, circle(bx, by - 30 * s, br * s), light(liquid, 0.3), outline=INK, ow=3 * s, gloss=0.4, shine=False)
    flat(ctx, rrect(x - 26 * s, y - 80 * s, 52 * s, 14 * s, 6 * s), "#b7b3c9", outline=INK, ow=6 * s)


@icon("shop")
def _shop(ctx):
    blob(ctx, rrect(34, 80, 124, 92, 10), "#fff4e6", outline=INK, ow=8, shine=False)
    blob(ctx, rrect(80, 112, 36, 60, 6), "#c88a4a", outline=INK, ow=6, shine=False)
    blob(ctx, rrect(46, 104, 26, 26, 4), "#9be7ff", outline=INK, ow=5, shine=False)
    blob(ctx, rrect(124, 104, 26, 26, 4), "#9be7ff", outline=INK, ow=5, shine=False)
    # The striped awning with scallops.
    scal = []
    for i in range(6):
        scal.append(circle(38 + i * 23.2, 82, 12))
    blob(ctx, [rrect(26, 42, 140, 40, 8)] + scal, "#ff5fa2", outline=INK, ow=8)
    ctx.save()
    path(ctx, [rrect(26, 42, 140, 40, 8)] + scal)
    ctx.clip()
    for i in range(0, 6, 2):
        flat(ctx, rrect(26 + i * 23.2 + 2, 40, 23.2, 60, 0), "#ffffff", 0.85)
    ctx.restore()
    blob(ctx, rrect(60, 18, 72, 26, 10), "#ffe066", outline=INK, ow=6, shine=False)


@icon("stickers")
def _stickers(ctx):
    sticker_badge(ctx, 96, 96, 76, "#ff9ec7", lambda cx: blob(cx, star(96, 98, 44, 21, round_=6), "#ffe066", outline=INK, ow=6))
    # The peeled corner.
    blob(ctx, poly([(150, 128), (168, 146), (142, 160)]), "#ffffff", outline=INK, ow=5, shine=False, gloss=0)


def sticker_badge(ctx, x, y, r, col, inner, scallops=14):
    pts = []
    n = scallops * 2
    for i in range(n):
        a = i / n * TAU
        rr = r if i % 2 == 0 else r * 0.9
        pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
    edge = smooth(pts, 0.6)
    blob(ctx, edge, "#ffffff", outline=INK, ow=7, gloss=0, shine=False, shade=0.12)
    blob(ctx, circle(x, y, r * 0.8), col, outline=None, gloss=0.35, shine=False, shade=0.2)
    inner(ctx)


@icon("gear")
def _gear(ctx):
    pts = []
    n = 9
    for i in range(n * 4):
        a = i / (n * 4) * TAU
        rr = 78 if (i % 4) in (0, 1) else 60
        pts.append((96 + math.cos(a) * rr, 96 + math.sin(a) * rr))
    blob(ctx, rounded_poly(pts, 6), "#a9b4d0", outline=INK, ow=8)
    blob(ctx, circle(96, 96, 26), "#3b2f5c", outline=INK, ow=6, gloss=0.2, shine=False)


@icon("sparkles")
def _sparkles(ctx):
    for x, y, r, col in ((84, 104, 66, "#ffe066"), (148, 46, 30, "#ff9ec7"), (150, 150, 22, "#22e3ff")):
        ctx.new_path()
        sp = star_pts(x, y, r, r * 0.22, 4, -math.pi / 2)
        blob(ctx, smooth(sp, 0.25), col, outline=INK, ow=6, shine=False)


@icon("party")
def _party(ctx):
    blob(ctx, poly([(34, 172), (66, 70), (136, 140)]), "#a970ff", outline=INK, ow=8)
    ctx.save()
    path(ctx, [poly([(34, 172), (66, 70), (136, 140)])])
    ctx.clip()
    for i in range(4):
        line(ctx, [(20 + i * 30, 180), (90 + i * 30, 60)], "#ffe066", 9)
    ctx.restore()
    for x, y, col in ((120, 40, "#ff4d6d"), (158, 70, "#22e3ff"), (150, 112, "#ffe066"), (96, 30, "#6ef2b4"), (172, 140, "#ff9ec7")):
        blob(ctx, rrect(x - 8, y - 5, 16, 10, 3), col, outline=INK, ow=3, shine=False, gloss=0)
    curve(ctx, (90, 64), (100, 40), (120, 70), (128, 50), "#ff9ec7", 6)


@icon("music")
def _music(ctx):
    line(ctx, [(70, 140), (70, 50), (150, 34), (150, 122)], INK, 22)
    line(ctx, [(70, 140), (70, 50), (150, 34), (150, 122)], "#a970ff", 10)
    line(ctx, [(70, 62), (150, 46)], "#a970ff", 18)
    blob(ctx, ellipse(52, 144, 26, 20, -0.4), "#ff7eb6", outline=INK, ow=7)
    blob(ctx, ellipse(132, 126, 26, 20, -0.4), "#ff7eb6", outline=INK, ow=7)


@icon("sound")
def _sound(ctx):
    blob(ctx, poly([(30, 76), (64, 76), (106, 40), (106, 152), (64, 116), (30, 116)]), "#22e3ff", outline=INK, ow=8)
    for r in (34, 58):
        arc_line(ctx, 106, 96, r, -0.8, 0.8, INK, 16)
        arc_line(ctx, 106, 96, r, -0.8, 0.8, "#ffe066", 7)


@icon("vibe")
def _vibe(ctx):
    blob(ctx, rrect(62, 30, 68, 132, 16), "#a970ff", outline=INK, ow=8)
    flat(ctx, rrect(72, 44, 48, 92, 6), "#2b1f4d")
    flat(ctx, circle(96, 148, 5), "#ffffff")
    for sx in (-1, 1):
        for k in (0, 1):
            x = 96 + sx * (54 + k * 18)
            line(ctx, [(x, 70), (x - sx * 6, 84), (x, 98), (x - sx * 6, 112), (x, 126)], "#ffe066", 6)


@icon("update")
def _update(ctx):
    blob(ctx, [circle(70, 100, 38), circle(108, 80, 48), circle(140, 108, 32), rrect(40, 100, 130, 40, 20)], "#9be7ff", outline=INK, ow=8)
    line(ctx, [(100, 94), (100, 150)], INK, 22)
    line(ctx, [(78, 128), (100, 152), (122, 128)], INK, 22)
    line(ctx, [(100, 94), (100, 150)], "#3ddc84", 10)
    line(ctx, [(78, 128), (100, 152), (122, 128)], "#3ddc84", 10)


@icon("planet")
def _planet(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(-0.35)
    ctx.scale(1, 0.32)
    ctx.new_path()
    ctx.arc(0, 0, 88, math.pi, TAU)
    ctx.restore()
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(20)
    ctx.stroke()
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(-0.35)
    ctx.scale(1, 0.32)
    ctx.new_path()
    ctx.arc(0, 0, 88, math.pi, TAU)
    ctx.restore()
    ctx.set_source_rgb(*c("#ffd23f"))
    ctx.set_line_width(9)
    ctx.stroke()
    blob(ctx, circle(96, 96, 56), "#ff7eb6", outline=INK, ow=8)
    ctx.save()
    circle(96, 96, 56)(ctx)
    ctx.clip()
    for y, col in ((70, "#ff9ec7"), (104, "#e2569b")):
        flat(ctx, ellipse(96, y, 70, 9, -0.35), col, 0.8)
    ctx.restore()
    face(ctx, 96, 100, 36)
    for front in (True,):
        ctx.save()
        ctx.translate(96, 96)
        ctx.rotate(-0.35)
        ctx.scale(1, 0.32)
        ctx.new_path()
        ctx.arc(0, 0, 88, 0, math.pi)
        ctx.restore()
        ctx.set_source_rgb(*INK)
        ctx.set_line_width(20)
        ctx.stroke()
        ctx.save()
        ctx.translate(96, 96)
        ctx.rotate(-0.35)
        ctx.scale(1, 0.32)
        ctx.new_path()
        ctx.arc(0, 0, 88, 0, math.pi)
        ctx.restore()
        ctx.set_source_rgb(*c("#ffd23f"))
        ctx.set_line_width(9)
        ctx.stroke()


@icon("clock")
def _clock(ctx):
    blob(ctx, circle(96, 96, 76), "#9be7ff", outline=INK, ow=8)
    blob(ctx, circle(96, 96, 58), "#ffffff", outline=None, gloss=0, shine=False)
    for i in range(12):
        a = i / 12 * TAU
        flat(ctx, circle(96 + math.cos(a) * 48, 96 + math.sin(a) * 48, 4 if i % 3 else 6), INK)
    line(ctx, [(96, 96), (96, 58)], INK, 9)
    line(ctx, [(96, 96), (124, 108)], INK, 9)
    flat(ctx, circle(96, 96, 8), "#ff4d8d")


@icon("spoon")
def _spoon(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(0.7)
    blob(ctx, [ellipse(0, -46, 32, 42), capsule(0, -10, 0, 82, 11)], "#d7d3ea", outline=INK, ow=8)
    flat(ctx, ellipse(0, -46, 22, 30), "#a970ff", 0.9)
    ctx.restore()
    for x, y, r in ((146, 52, 10), (160, 82, 7), (38, 150, 8)):
        blob(ctx, circle(x, y, r), "#c9a8ff", outline=INK, ow=4, shine=False)


@icon("tag")
def _tag(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(-0.6)
    blob(ctx, rounded_poly([(-70, -34), (40, -34), (78, 0), (40, 34), (-70, 34)], 12), "#ffe066", outline=INK, ow=8)
    flat(ctx, circle(40, 0, 10), "#3b2050")
    for i, w in enumerate((70, 50)):
        line(ctx, [(-54, -10 + i * 22), (-54 + w, -10 + i * 22)], dark("#ffe066", 0.4), 8)
    ctx.restore()


@icon("play")
def _play(ctx):
    blob(ctx, circle(96, 96, 78), "#22e3ff", outline=INK, ow=8)
    blob(ctx, rounded_poly([(74, 52), (142, 96), (74, 140)], 14), "#ffffff", outline=None, gloss=0.2, shine=False)


@icon("bubbles")
def _bubbles(ctx):
    for x, y, r in ((80, 110, 54), (140, 60, 32), (144, 140, 24), (52, 44, 18)):
        bubble_at(ctx, x, y, r)


def bubble_at(ctx, x, y, r, col="#9be7ff"):
    g = cairo.RadialGradient(x - r * 0.3, y - r * 0.3, r * 0.1, x, y, r)
    g.add_color_stop_rgba(0, 1, 1, 1, 0.35)
    g.add_color_stop_rgba(0.75, *c(col), 0.45)
    g.add_color_stop_rgba(1, *light(col, 0.4), 0.9)
    circle(x, y, r)(ctx)
    ctx.set_source_rgba(*INK, 1)
    ctx.set_line_width(max(3, r * 0.12))
    ctx.stroke_preserve()
    ctx.set_source(g)
    ctx.fill()
    # Rainbow film.
    ctx.save()
    circle(x, y, r)(ctx)
    ctx.clip()
    for i, col2 in enumerate(("#ff9ec7", "#ffe066", "#6ef2b4")):
        arc_line(ctx, x, y, r * (0.8 - i * 0.07), 0.2, 1.5, col2, r * 0.07, 0.6)
    ctx.restore()
    flat(ctx, ellipse(x - r * 0.35, y - r * 0.4, r * 0.28, r * 0.16, -0.7), "#ffffff", 0.95)
    flat(ctx, circle(x + r * 0.38, y - r * 0.5, r * 0.07), "#ffffff", 0.9)


@icon("shelf")
def _shelf(ctx):
    flat(ctx, rrect(14, 150, 164, 18, 6), "#c88a4a", outline=INK, ow=6)
    jar_at(ctx, 40, 120, 0.42, "#ff7eb6", "#22e3ff")
    jar_at(ctx, 152, 120, 0.42, "#6ef2b4", "#ffe066")
    jar_at(ctx, 96, 112, 0.5, "#a970ff")


@icon("bolt")
def _bolt(ctx):
    blob(ctx, rounded_poly([(112, 14), (40, 110), (90, 110), (76, 178), (152, 76), (102, 76)], 6), "#ffe066", outline=INK, ow=8)


@icon("calendar")
def _calendar(ctx):
    blob(ctx, rrect(28, 40, 136, 128, 20), "#ffffff", outline=INK, ow=8, shade=0.12)
    blob(ctx, rrect(28, 40, 136, 40, 18), "#ff4d6d", outline=INK, ow=8, shine=False)
    for x in (62, 130):
        flat(ctx, capsule(x, 28, x, 54, 8), "#b7b3c9", outline=INK, ow=4)
    for i in range(9):
        x, y = 54 + (i % 3) * 42, 100 + (i // 3) * 22
        flat(ctx, rrect(x - 12, y - 7, 24, 14, 4), "#a970ff" if i == 4 else "#e6e0f5")


