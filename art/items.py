"""Icons for the things in the game: tools and toys, slime bases, mix-ins, glitter, dishes, plate and jelly shapes."""
import math

import cairo

from artlib import *
from icons import ICONS, bubble_at, dish_at, icon


# ---- hands -------------------------------------------------------------------------------------------------

SKIN = "#ffcfae"


def hand_open(ctx, x, y, s, rot=0.0, col=SKIN):
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(rot)
    ctx.scale(s, s)
    fingers = [capsule(fx, -6, fx + lean, -top, 11) for fx, top, lean in ((-27, 62, -6), (-9, 74, -2), (9, 74, 2), (27, 62, 6))]
    blob(ctx, [rrect(-40, -24, 80, 72, 30), capsule(-34, 22, -62, -8, 12)] + fingers, col, outline=INK, ow=7, gloss=0.35)
    for fx in (-18, 0, 18):
        line(ctx, [(fx, -10), (fx, -24)], dark(col, 0.25), 4)
    ctx.restore()


def hand_point(ctx, x, y, s, rot=0.0, col=SKIN):
    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(rot)
    ctx.scale(s, s)
    blob(ctx, [rrect(-36, -26, 74, 70, 28), capsule(-14, -16, -14, -86, 12)], col, outline=INK, ow=7, gloss=0.35)
    for fy in (-6, 12):
        curve(ctx, (4, fy), (16, fy - 6), (28, fy - 2), (34, fy + 6), dark(col, 0.3), 4)
    blob(ctx, capsule(-30, 18, 8, 4, 10), col, outline=INK, ow=5, gloss=0, shine=False)
    ctx.restore()


def arrow(ctx, x1, y1, x2, y2, col="#22e3ff", w=10):
    a = math.atan2(y2 - y1, x2 - x1)
    head = [(x2 + math.cos(a + k) * 22, y2 + math.sin(a + k) * 22) for k in (2.5, -2.5)]
    for cc, ww in ((INK, w + 9), (col, w)):
        line(ctx, [(x1, y1), (x2, y2)], cc, ww)
        line(ctx, [head[0], (x2, y2), head[1]], cc, ww)


def slime(ctx, x, y, w, h, col="#a970ff", mood="happy", faces=True, **kw):
    blob(ctx, slime_dome(x, y, w, h), col, outline=INK, ow=7, **kw)
    if faces:
        face(ctx, x, y - h * 0.35, w * 0.5, mood)


# ---- tools and toys ----------------------------------------------------------------------------------------

@icon("tool_poke")
def _poke(ctx):
    slime(ctx, 96, 150, 76, 54, "#a970ff", "o")
    arc_line(ctx, 112, 112, 16, 0.2, 2.9, "#22e3ff", 5)
    hand_point(ctx, 124, 82, 0.66, rot=-0.35)


@icon("tool_stretch")
def _stretch(ctx):
    blob(ctx, [ellipse(40, 112, 34, 30), ellipse(152, 112, 34, 30), capsule(40, 112, 152, 112, 13)], "#ff7eb6", outline=INK, ow=7)
    face(ctx, 96, 106, 26, "open", blush=True)
    arrow(ctx, 36, 52, 6, 52)
    arrow(ctx, 156, 52, 186, 52)


@icon("tool_squish")
def _squish(ctx):
    blob(ctx, slime_dome(96, 160, 88, 30), "#6ef2b4", outline=INK, ow=7)
    face(ctx, 96, 150, 30, "closed")
    hand_open(ctx, 96, 74, 0.75, rot=math.pi)


@icon("tool_roll")
def _roll(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(-0.5)
    blob(ctx, [capsule(-88, 0, -58, 0, 11), capsule(58, 0, 88, 0, 11)], "#b07a4a", outline=INK, ow=7, shine=False)
    blob(ctx, rrect(-60, -30, 120, 60, 26), "#e8b27a", outline=INK, ow=8)
    for x in (-30, 0, 30):
        line(ctx, [(x, -22), (x, 22)], dark("#e8b27a", 0.18), 4)
    ctx.restore()


@icon("tool_bubble")
def _bubble(ctx):
    ICONS["bubbles"](ctx)


@icon("tool_slap")
def _slap(ctx):
    hand_open(ctx, 102, 104, 1.0, rot=0.3)
    for i in range(3):
        a = math.pi * 0.95 + i * 0.28
        line(ctx, [(96 + math.cos(a) * 76, 104 + math.sin(a) * 76), (96 + math.cos(a) * 92, 104 + math.sin(a) * 92)], "#ffe066", 8)


@icon("tool_rake")
def _rake(ctx):
    ctx.save()
    ctx.translate(96, 96)
    ctx.rotate(0.6)
    tines = [capsule(x, -84, x, -40, 7) for x in (-36, -12, 12, 36)]
    blob(ctx, [rrect(-46, -48, 92, 22, 10), capsule(0, -30, 0, 86, 10)] + tines, "#d7d3ea", outline=INK, ow=7)
    blob(ctx, capsule(0, 30, 0, 86, 12), "#ff7eb6", outline=INK, ow=6, shine=False)
    ctx.restore()


@icon("tool_stamp")
def _stamp(ctx):
    blob(ctx, star(96, 168, 30, 14, round_=4), "#ffe066", outline=INK, ow=5, shine=False, gloss=0)
    blob(ctx, rrect(46, 112, 100, 34, 10), "#ff7eb6", outline=INK, ow=8)
    blob(ctx, rrect(80, 62, 32, 54, 10), "#c88a4a", outline=INK, ow=7, shine=False)
    blob(ctx, circle(96, 50, 30), "#a970ff", outline=INK, ow=8)


@icon("tool_shaker")
def _shaker(ctx):
    ctx.save()
    ctx.translate(108, 82)
    ctx.rotate(0.6)
    blob(ctx, rrect(-34, -40, 68, 104, 26), "#e9f7ff", outline=INK, ow=8, alpha=0.95)
    ctx.save()
    rrect(-34, -40, 68, 104, 26)(ctx)
    ctx.clip()
    for i in range(14):
        x = -26 + (i * 37) % 52
        y = 8 + (i * 23) % 52
        flat(ctx, capsule(x, y, x + 6, y + 6, 3.4), ("#ff4d6d", "#ffe066", "#22e3ff", "#6ef2b4", "#a970ff")[i % 5])
    ctx.restore()
    blob(ctx, rrect(-36, -66, 72, 32, 14), "#ff7eb6", outline=INK, ow=7, shine=False)
    for x in (-14, 0, 14):
        flat(ctx, circle(x, -56, 3.5), INK)
    ctx.restore()
    for i, (x, y) in enumerate(((30, 104), (48, 130), (24, 148), (62, 160), (40, 176))):
        flat(ctx, capsule(x, y, x + 8, y + 5, 4), ("#ff4d6d", "#ffe066", "#22e3ff", "#6ef2b4", "#a970ff")[i], outline=INK, ow=2)


@icon("tool_magnet")
def _magnet(ctx):
    ctx.save()
    ctx.translate(96, 90)
    ctx.rotate(0.0)
    ctx.new_path()
    ctx.arc_negative(0, 0, 54, math.pi, 0)
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(56)
    ctx.set_line_cap(cairo.LINE_CAP_BUTT)
    ctx.new_path()
    ctx.arc(0, -10, 50, math.pi, 0)
    ctx.line_to(50, 40)
    ctx.move_to(-50, -10)
    ctx.line_to(-50, 40)
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(52)
    ctx.stroke()
    ctx.new_path()
    ctx.arc(0, -10, 50, math.pi, 0)
    ctx.line_to(50, 22)
    ctx.move_to(-50, -10)
    ctx.line_to(-50, 22)
    ctx.set_source_rgb(*c("#ff4d6d"))
    ctx.set_line_width(36)
    ctx.stroke()
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    for sx in (-1, 1):
        flat(ctx, rrect(sx * 50 - 18, 22, 36, 26, 4), "#e6e3f2")
    ctx.new_path()
    ctx.arc(-12, -40, 22, math.pi * 1.1, math.pi * 1.5)
    ctx.set_source_rgba(1, 1, 1, 0.6)
    ctx.set_line_width(8)
    ctx.stroke()
    ctx.restore()
    for sx in (-1, 1):
        line(ctx, [(96 + sx * 50, 150), (96 + sx * 40, 162), (96 + sx * 50, 174)], "#ffe066", 6)


@icon("tool_wand")
def _wand(ctx):
    blob(ctx, capsule(46, 170, 104, 98, 9), "#ff7eb6", outline=INK, ow=7, shine=False)
    ctx.new_path()
    ctx.arc(128, 68, 40, 0, TAU)
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(24)
    ctx.stroke()
    g = cairo.RadialGradient(118, 58, 4, 128, 68, 40)
    g.add_color_stop_rgba(0, 1, 1, 1, 0.5)
    g.add_color_stop_rgba(1, 0.6, 0.9, 1, 0.35)
    ctx.arc(128, 68, 40, 0, TAU)
    ctx.set_source(g)
    ctx.fill()
    ctx.arc(128, 68, 40, 0, TAU)
    ctx.set_source_rgb(*c("#a970ff"))
    ctx.set_line_width(11)
    ctx.stroke()
    flat(ctx, ellipse(114, 54, 12, 6, -0.7), "#ffffff", 0.9)
    bubble_at(ctx, 46, 52, 18)
    bubble_at(ctx, 70, 24, 11)
    bubble_at(ctx, 172, 140, 14, "#ffe066")


# ---- slime bases -------------------------------------------------------------------------------------------

def base_shape():
    return slime_dome(96, 128, 80, 70)


def galaxy_grad(x1, y1, x2, y2):
    g = cairo.LinearGradient(x1, y1, x2, y2)
    g.add_color_stop_rgb(0, *c("#3a1c80"))
    g.add_color_stop_rgb(0.5, *c("#1b0d40"))
    g.add_color_stop_rgb(1, *c("#5a1d6b"))
    return g


def rainbow(x1, y1, x2, y2):
    g = cairo.LinearGradient(x1, y1, x2, y2)
    for i, col in enumerate(("#ff9ec7", "#ffe28a", "#9dffcf", "#9be7ff", "#c9a8ff")):
        g.add_color_stop_rgb(i / 4, *c(col))
    return g


def chrome(x1, y1, x2, y2):
    g = cairo.LinearGradient(0, y1, 0, y2)
    for t, col in ((0, "#ffffff"), (0.3, "#b8bfd6"), (0.48, "#5d6380"), (0.55, "#e9ecf8"), (0.8, "#8b91ab"), (1, "#4a4f68")):
        g.add_color_stop_rgb(t, *c(col))
    return g


def thermo(x1, y1, x2, y2):
    g = cairo.LinearGradient(x1, 0, x2, 0)
    g.add_color_stop_rgb(0, *c("#ff4d8d"))
    g.add_color_stop_rgb(0.5, *c("#ffb14d"))
    g.add_color_stop_rgb(1, *c("#3d7bff"))
    return g


def pearl(x1, y1, x2, y2):
    g = cairo.LinearGradient(x1, y1, x2, y2)
    for t, col in ((0, "#ffffff"), (0.4, "#ffe6f2"), (0.7, "#e6f0ff"), (1, "#f3e6ff")):
        g.add_color_stop_rgb(t, *c(col))
    return g


@icon("base_glossy")
def _glossy(ctx):
    slime(ctx, 96, 128, 80, 70, "#a970ff", gloss=0.8)
    flat(ctx, ellipse(60, 92, 10, 5, -0.6), "#ffffff", 0.9)


@icon("base_clear")
def _clear(ctx):
    blob(ctx, base_shape(), "#c4f1ff", outline=INK, ow=7, alpha=0.8, gloss=0.8)
    for x, y, r in ((70, 110, 8), (118, 96, 6), (104, 130, 10), (84, 138, 5)):
        bubble_at(ctx, x, y, r)
    face(ctx, 96, 104, 40)


@icon("base_butter")
def _butter(ctx):
    slime(ctx, 96, 128, 80, 70, "#ffe9a8", gloss=0.2, shine=False, shade=0.2)
    blob(ctx, rrect(70, 46, 44, 26, 8), "#ffd65c", outline=INK, ow=5, shine=False, gloss=0.4)


@icon("base_cloud")
def _cloud(ctx):
    blob(ctx, [circle(56, 116, 36), circle(96, 92, 48), circle(138, 112, 38), rrect(30, 112, 132, 46, 22)], "#f4f1ff", outline=INK, ow=7, gloss=0.25, shine=False)
    face(ctx, 96, 118, 40, "closed")
    for x, y in ((56, 176), (96, 182), (136, 176)):
        blob(ctx, drop(x, y, 7), "#9be7ff", outline=INK, ow=3, shine=False)


@icon("base_jelly")
def _jelly(ctx):
    dish_at(ctx, 96, 160, 0.95, "#ffffff")
    blob(ctx, smooth([(40, 152), (46, 80), (66, 52), (126, 52), (146, 80), (152, 152)], 0.45), "#ff4d6d", outline=INK, ow=7, alpha=0.85, gloss=0.8)
    for y in (84, 114):
        line(ctx, [(52, y), (140, y)], light("#ff4d6d", 0.35), 5, 0.7)
    face(ctx, 96, 104, 36)


@icon("base_icee")
def _icee(ctx):
    blob(ctx, base_shape(), "#d4f5ff", outline=INK, ow=7, gloss=0.7)
    for x, y, r in ((62, 104, 10), (128, 96, 12), (110, 138, 8), (72, 140, 7)):
        blob(ctx, rounded_poly([(x, y - r), (x + r, y), (x, y + r), (x - r, y)], 2), "#ffffff", outline="#5bb7e0", ow=2, shine=False, gloss=0)
    face(ctx, 96, 112, 40, "o")
    sparkle(ctx, 152, 52, 14)


@icon("base_chrome")
def _chrome(ctx):
    blob(ctx, base_shape(), "#b8bfd6", outline=INK, ow=7, grad=chrome, gloss=0.6)
    face(ctx, 96, 112, 40)


@icon("base_holo")
def _holo(ctx):
    blob(ctx, base_shape(), "#c9a8ff", outline=INK, ow=7, grad=rainbow, gloss=0.6)
    face(ctx, 96, 112, 40)
    sparkle(ctx, 150, 50, 14)


@icon("base_glow")
def _glow(ctx):
    glow(ctx, 96, 110, 96, "#7cff6b", 0.5)
    blob(ctx, base_shape(), "#9dff6b", outline="#1d4d10", ow=7, gloss=0.5)
    face(ctx, 96, 112, 40, eye_col="#1d4d10")
    for x, y in ((40, 40), (160, 60), (150, 170)):
        sparkle(ctx, x, y, 10, "#d8ffc0")


@icon("base_floam")
def _floam(ctx):
    slime(ctx, 96, 128, 80, 70, "#ff9ec7", faces=False, gloss=0.3)
    ctx.save()
    path(ctx, [base_shape()])
    ctx.clip()
    for i in range(46):
        x = 20 + (i * 47) % 156
        y = 54 + (i * 31) % 110
        flat(ctx, circle(x, y, 6), ("#ffffff", "#fff3b0", "#d8f4ff", "#ffd6ec")[i % 4], outline=dark("#ff9ec7", 0.3), ow=1.2)
    ctx.restore()
    face(ctx, 96, 112, 40)


@icon("base_pearl")
def _pearl(ctx):
    blob(ctx, base_shape(), "#fff0f6", outline=INK, ow=7, grad=pearl, gloss=0.7)
    face(ctx, 96, 112, 40)
    blob(ctx, circle(150, 52, 14), "#ffffff", outline=INK, ow=4, grad=pearl)


@icon("base_magnetic")
def _magnetic(ctx):
    spikes = [poly([(96 + dx - 9, 70 + abs(dx) * 0.4), (96 + dx, 30 + abs(dx) * 0.5), (96 + dx + 9, 70 + abs(dx) * 0.4)]) for dx in (-40, -14, 14, 40)]
    blob(ctx, [base_shape()] + spikes, "#4a4d5e", outline=INK, ow=7, gloss=0.5)
    face(ctx, 96, 116, 40, eye_col="#ffffff")


@icon("base_thermo")
def _thermo(ctx):
    blob(ctx, base_shape(), "#ffb14d", outline=INK, ow=7, grad=thermo, gloss=0.6)
    face(ctx, 96, 112, 40)
    blob(ctx, [capsule(160, 30, 160, 80, 10), circle(160, 88, 15)], "#ffffff", outline=INK, ow=5, shine=False, gloss=0)
    flat(ctx, [capsule(160, 54, 160, 84, 4.5), circle(160, 88, 9)], "#ff4d6d")


@icon("base_galaxy")
def _galaxy(ctx):
    blob(ctx, base_shape(), "#2a1660", outline=INK, ow=7, grad=galaxy_grad, gloss=0.4)
    ctx.save()
    path(ctx, [base_shape()])
    ctx.clip()
    glow(ctx, 70, 120, 40, "#ff5fd2", 0.45)
    glow(ctx, 126, 100, 44, "#22e3ff", 0.35)
    for i in range(22):
        x = 26 + (i * 53) % 140
        y = 66 + (i * 37) % 96
        sparkle(ctx, x, y, 3 + (i % 3) * 2.5)
    ctx.restore()
    face(ctx, 96, 112, 40, eye_col="#ffffff")


# ---- mix-ins -----------------------------------------------------------------------------------------------

BRIGHT = ("#ff4d8d", "#ffe066", "#22e3ff", "#6ef2b4", "#a970ff", "#ff8a3d")


def bead(ctx, x, y, r, col, ow=4):
    blob(ctx, circle(x, y, r), col, outline=INK, ow=ow, shine=False, gloss=0.6)


@icon("addin_foam")
def _foam(ctx):
    for i, (x, y, r) in enumerate(((60, 120, 30), (110, 136, 28), (140, 90, 26), (90, 84, 32), (52, 64, 20), (146, 150, 18))):
        bead(ctx, x, y, r, ("#ffffff", "#ffd6ec", "#d8f4ff", "#fff3b0")[i % 4])


@icon("addin_sprinkles")
def _sprinkles(ctx):
    for i in range(14):
        a = i * 2.39
        r = 22 + (i * 13) % 60
        x, y = 96 + math.cos(a) * r, 96 + math.sin(a) * r
        rot = i * 0.9
        blob(ctx, capsule(x - math.cos(rot) * 13, y - math.sin(rot) * 13, x + math.cos(rot) * 13, y + math.sin(rot) * 13, 6.5), BRIGHT[i % 6], outline=INK, ow=3.5, shine=False, gloss=0.4)


@icon("addin_stars")
def _stars(ctx):
    for x, y, r, col in ((76, 100, 52, "#ffe066"), (140, 58, 28, "#ffffff"), (142, 144, 24, "#8fe9ff")):
        blob(ctx, star(x, y, r, r * 0.5, round_=r * 0.12), col, outline=INK, ow=6)


@icon("addin_hearts")
def _hearts(ctx):
    for x, y, s, col in ((84, 104, 50, "#ff4d8d"), (148, 54, 24, "#ff9ec7"), (150, 146, 20, "#ff2b55")):
        blob(ctx, heart(x, y, s), col, outline=INK, ow=6)


@icon("addin_pearls")
def _pearls(ctx):
    for i in range(7):
        a = i / 7 * TAU
        blob(ctx, circle(96 + math.cos(a) * 52, 96 + math.sin(a) * 52, 22), "#fff8f0", outline=INK, ow=4, grad=pearl, gloss=0.6)
    blob(ctx, circle(96, 96, 26), "#fff8f0", outline=INK, ow=4, grad=pearl, gloss=0.6)


@icon("addin_fishbowl")
def _fishbowl(ctx):
    for i, (x, y, r) in enumerate(((66, 114, 38), (124, 126, 32), (112, 64, 30), (156, 70, 16))):
        bubble_at(ctx, x, y, r, BRIGHT[i])


@icon("addin_sugar")
def _sugar(ctx):
    for x, y, s, rot in ((70, 110, 34, 0.3), (126, 120, 28, -0.4), (104, 62, 24, 0.8)):
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(rot)
        blob(ctx, rrect(-s, -s, 2 * s, 2 * s, 6), "#ffffff", outline=INK, ow=5, shade=0.25)
        line(ctx, [(-s * 0.6, -s * 0.2), (-s * 0.2, -s * 0.6)], "#cdeeff", 4)
        ctx.restore()
    sparkle(ctx, 160, 46, 12, "#ffffff")


@icon("addin_fruit")
def _fruit(ctx):
    blob(ctx, circle(96, 96, 76), "#ff9f1c", outline=INK, ow=7, gloss=0.3, shine=False)
    blob(ctx, circle(96, 96, 62), "#fff1cf", outline=None, gloss=0, shine=False)
    for i in range(8):
        a = i / 8 * TAU
        ctx.new_path()
        ctx.move_to(96 + math.cos(a + 0.33) * 6, 96 + math.sin(a + 0.33) * 6)
        ctx.arc(96, 96, 54, a + 0.08, a + TAU / 8 - 0.08)
        ctx.close_path()
        ctx.set_source_rgb(*c("#ffb53d"))
        ctx.fill()
    flat(ctx, ellipse(70, 66, 16, 8, -0.7), "#ffffff", 0.6)


@icon("addin_snow")
def _snow(ctx):
    for k in range(3):
        a = k * math.pi / 3
        dx, dy = math.cos(a) * 74, math.sin(a) * 74
        for cc, w in ((INK, 24), ("#e8f7ff", 11)):
            line(ctx, [(96 - dx, 96 - dy), (96 + dx, 96 + dy)], cc, w)
    for k in range(6):
        a = k * math.pi / 3
        bx, by = 96 + math.cos(a) * 50, 96 + math.sin(a) * 50
        for cc, w in ((INK, 20), ("#e8f7ff", 8)):
            for side in (-0.7, 0.7):
                line(ctx, [(bx, by), (bx + math.cos(a + side) * 22, by + math.sin(a + side) * 22)], cc, w)
    blob(ctx, rounded_poly([(96 + math.cos(k * math.pi / 3) * 20, 96 + math.sin(k * math.pi / 3) * 20) for k in range(6)], 4), "#bfefff", outline=INK, ow=5, shine=False)


@icon("addin_confetti")
def _confetti(ctx):
    for i in range(16):
        a = i * 2.39
        r = 14 + (i * 17) % 66
        x, y = 96 + math.cos(a) * r, 96 + math.sin(a) * r
        if i % 3 == 0:
            blob(ctx, circle(x, y, 10), BRIGHT[i % 6], outline=INK, ow=3.5, shine=False, gloss=0.4)
        else:
            ctx.save()
            ctx.translate(x, y)
            ctx.rotate(i)
            blob(ctx, rrect(-12, -6, 24, 12, 3), BRIGHT[i % 6], outline=INK, ow=3.5, shine=False, gloss=0.4)
            ctx.restore()


@icon("addin_micro")
def _micro(ctx):
    for i in range(34):
        a = i * 2.39
        r = 8 + (i * 29) % 72
        bead(ctx, 96 + math.cos(a) * r, 96 + math.sin(a) * r, 9, ("#ffffff", "#e8f6ff", "#ffeaf6")[i % 3], ow=2.5)


def daisy(ctx, x, y, r, col="#ffffff", mid="#ffd23f"):
    petals = []
    for i in range(8):
        a = i / 8 * TAU
        petals.append(ellipse(x + math.cos(a) * r * 0.55, y + math.sin(a) * r * 0.55, r * 0.42, r * 0.24, a))
    blob(ctx, petals, col, outline=INK, ow=max(3, r * 0.08), shine=False, gloss=0.3)
    blob(ctx, circle(x, y, r * 0.3), mid, outline=INK, ow=max(2.5, r * 0.06), shine=False)


@icon("addin_daisies")
def _daisies(ctx):
    daisy(ctx, 80, 104, 66)
    daisy(ctx, 150, 56, 32, "#ff9ec7")
    daisy(ctx, 152, 150, 26, "#c9a8ff")


@icon("addin_moons")
def _moons(ctx):
    blob(ctx, crescent(86, 100, 66, 0.6, rot=-0.5), "#ffd36b", outline=INK, ow=7)
    blob(ctx, crescent(152, 52, 26, 0.6, rot=-0.5), "#fff6d8", outline=INK, ow=5, shine=False)
    sparkle(ctx, 150, 140, 14, "#fff6d8")


@icon("addin_goldleaf")
def _goldleaf(ctx):
    for i, (x, y, s, rot) in enumerate(((80, 100, 48, 0.4), (140, 62, 26, -0.6), (138, 140, 22, 1.2), (50, 50, 16, 2.0))):
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(rot)
        pts = [(math.cos(k / 7 * TAU) * s * (0.75 + 0.3 * ((k * 5) % 3) / 2), math.sin(k / 7 * TAU) * s * (0.75 + 0.3 * ((k * 3) % 3) / 2)) for k in range(7)]
        blob(ctx, rounded_poly(pts, 4), ("#ffd36b", "#ffe9a8", "#e8b04b")[i % 3], outline="#7a4a00", ow=5)
        ctx.restore()


@icon("addin_gems")
def _gems(ctx):
    def gem(x, y, s, col):
        pts = [(x - s, y - s * 0.4), (x - s * 0.5, y - s * 0.9), (x + s * 0.5, y - s * 0.9), (x + s, y - s * 0.4), (x, y + s)]
        blob(ctx, poly(pts), col, outline=INK, ow=max(3, s * 0.1), shine=False)
        line(ctx, [(x - s, y - s * 0.4), (x + s, y - s * 0.4)], light(col, 0.5), max(2, s * 0.06))
        line(ctx, [(x - s * 0.5, y - s * 0.4), (x, y + s), (x + s * 0.5, y - s * 0.4)], light(col, 0.4), max(2, s * 0.05))
        line(ctx, [(x - s * 0.5, y - s * 0.9), (x - s * 0.2, y - s * 0.4)], light(col, 0.5), max(2, s * 0.05))
        line(ctx, [(x + s * 0.5, y - s * 0.9), (x + s * 0.2, y - s * 0.4)], light(col, 0.5), max(2, s * 0.05))
    gem(84, 106, 64, "#22e3ff")
    gem(152, 56, 26, "#ff7eb6")
    gem(154, 150, 22, "#a970ff")


@icon("addin_gummies")
def _gummies(ctx):
    for k, (y0, cols) in enumerate(((64, ("#ff4d6d", "#ffe14d")), (124, ("#8be04e", "#3da5ff")))):
        pts = [(28 + i * 17, y0 + math.sin(i * 0.9 + k) * 16) for i in range(9)]
        for cc, w in ((INK, 40),):
            line(ctx, pts, cc, w)
        for i in range(len(pts) - 1):
            line(ctx, [pts[i], pts[i + 1]], cols[0] if i < 4 else cols[1], 26)
        for i in range(1, len(pts) - 1):
            line(ctx, [(pts[i][0], pts[i][1] - 10), (pts[i][0], pts[i][1] + 10)], (1, 1, 1), 2, 0.35)
        line(ctx, [(pts[0][0] + 4, pts[0][1] - 7), (pts[3][0], pts[3][1] - 7)], (1, 1, 1), 4, 0.6)


# ---- glitter -----------------------------------------------------------------------------------------------

GLITTERS = {"none": None, "silver": "#e8ecff", "gold": "#ffd36b", "rose": "#ff9ecf", "rainbow": "rainbow",
            "aqua": "#7df9ff", "violet": "#b98cff", "emerald": "#5bf5a0", "copper": "#e08a4f"}


def glitter_icon(col):
    def draw(ctx):
        if col is None:
            arc_line(ctx, 96, 96, 62, 0, TAU, INK, 22)
            arc_line(ctx, 96, 96, 62, 0, TAU, "#8a84a3", 10)
            line(ctx, [(52, 140), (140, 52)], INK, 22)
            line(ctx, [(52, 140), (140, 52)], "#8a84a3", 10)
            return
        cols = BRIGHT if col == "rainbow" else (col, light(col, 0.5), dark(col, 0.15))
        # A little heap of glitter with big twinkles.
        blob(ctx, smooth([(26, 160), (60, 118), (96, 104), (134, 116), (168, 160)], 0.5), cols[0] if col != "rainbow" else "#c9a8ff",
             outline=INK, ow=7, gloss=0.3, shine=False, grad=rainbow if col == "rainbow" else None)
        for i in range(40):
            x = 34 + (i * 41) % 126
            y = 120 + (i * 17) % 40
            if y < 160 - abs(x - 96) * 0.5 + 0 and y > 104 + abs(x - 96) * 0.35:
                flat(ctx, circle(x, y, 2.6), "#ffffff" if i % 3 == 0 else cols[(i // 3) % len(cols)])
        for x, y, r, k in ((80, 62, 30, 0), (138, 46, 18, 1), (40, 92, 14, 2), (150, 100, 12, 0)):
            sp = star_pts(x, y, r, r * 0.22, 4, -math.pi / 2)
            blob(ctx, smooth(sp, 0.25), cols[k % len(cols)] if col != "rainbow" else BRIGHT[k + 1], outline=INK, ow=4, shine=False)
    return draw


for gid, gcol in GLITTERS.items():
    ICONS["glitter_" + gid] = glitter_icon(gcol)


# ---- dishes (the tray colours) -----------------------------------------------------------------------------

TRAYS = {"void": "#3a2f6e", "marble": "#edeaf2", "bubblegum": "#ffb3d9", "mint": "#a8f0d0", "wood": "#b07a4a",
         "neon": "#1d1530", "candy": "#d9b8ff", "sunset": "#ff9a6b", "ice": "#cfefff", "starry": "#151036", "gold": "#e8c46a"}


def tray_icon(tid, col):
    def draw(ctx):
        if tid == "neon":
            glow(ctx, 96, 112, 100, "#ff2bd6", 0.35)
        dish_at(ctx, 96, 112, 1.0, col, rim="#ff5fe0" if tid == "neon" else None)
        ctx.save()
        ellipse(96, 115, 64, 23)(ctx)
        ctx.clip()
        if tid == "marble":
            curve(ctx, (40, 104), (70, 120), (100, 96), (150, 124), "#b9b2cc", 3)
            curve(ctx, (60, 130), (90, 110), (120, 132), (140, 100), "#cfc8dd", 2)
        elif tid == "wood":
            for i in range(5):
                curve(ctx, (30, 96 + i * 8), (70, 92 + i * 9), (120, 104 + i * 7), (162, 98 + i * 8), dark(col, 0.25), 3)
        elif tid in ("starry", "void"):
            for i in range(14):
                sparkle(ctx, 40 + (i * 37) % 112, 98 + (i * 13) % 32, 2 + i % 3 * 1.5, "#ffffff", 0.9)
        elif tid == "candy":
            for i in range(6):
                curve(ctx, (30 + i * 24, 140), (40 + i * 24, 110), (60 + i * 24, 110), (70 + i * 24, 90), "#ff9ec7", 6, 0.8)
        elif tid == "sunset":
            g = cairo.LinearGradient(0, 92, 0, 138)
            g.add_color_stop_rgb(0, *c("#ff6b9a"))
            g.add_color_stop_rgb(1, *c("#ffd36b"))
            ctx.set_source(g)
            ctx.paint()
        elif tid == "ice":
            line(ctx, [(60, 100), (80, 116), (76, 132)], "#ffffff", 3)
            line(ctx, [(80, 116), (110, 110), (130, 126)], "#ffffff", 3)
        elif tid == "neon":
            for r in (40, 22):
                ctx.save()
                ctx.translate(96, 115)
                ctx.scale(1, 23 / 64)
                arc_line(ctx, 0, 0, r * 1.5, 0, TAU, "#22e3ff", 4)
                ctx.restore()
        elif tid == "gold":
            sparkle(ctx, 120, 112, 10)
        elif tid in ("bubblegum", "mint"):
            for i in range(8):
                flat(ctx, circle(44 + (i * 29) % 104, 102 + (i * 11) % 28, 5), "#ffffff", 0.6)
        ctx.restore()
        if tid == "gold":
            sparkle(ctx, 160, 64, 14)
            sparkle(ctx, 30, 80, 10)
    return draw


for tid, tcol in TRAYS.items():
    ICONS["tray_" + tid] = tray_icon(tid, tcol)


# ---- plate shapes (top view), the same outlines as model/Shapes.kt ------------------------------------------

def plate_r(kind, a):
    if kind == "round":
        return 1.0
    if kind == "square":
        p = 5.0
        return 0.96 * (abs(math.cos(a)) ** p + abs(math.sin(a)) ** p) ** (-1 / p)
    if kind == "hexagon":
        k = (a + math.pi / 6) % (math.pi / 3) - math.pi / 6
        return 1.06 * math.cos(math.pi / 6) / math.cos(k) * 0.97 + 0.03
    if kind == "flower":
        return 1.0 + 0.11 * math.cos(6 * a)
    if kind == "cloud":
        return 0.94 + 0.12 * abs(math.cos(3.5 * a + math.pi / 2)) ** 0.6
    if kind == "star":
        return 1.0 + 0.17 * math.cos(5 * (a + math.pi / 2))
    if kind == "heart":
        return heart_r(a)
    return 1.0


_HEART = []


def heart_r(a):
    """Polar radius of a softened heart (point at the front, toward +y on screen)."""
    if not _HEART:
        pts = []
        for i in range(720):
            t = i / 720 * TAU
            x = 16 * math.sin(t) ** 3
            y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
            pts.append((x / 16.5, -(y - 2.5) / 16.5))
        for k in range(360):
            th = k / 360 * TAU
            best = 1.0
            bd = 9.0
            for (x, y) in pts:
                ang = math.atan2(y, x)
                d = abs((ang - th + math.pi) % TAU - math.pi)
                if d < bd:
                    bd, best = d, math.hypot(x, y)
            _HEART.append(best)
        m = max(_HEART)
        for k in range(360):
            _HEART[k] = 0.5 * _HEART[k] / m * 1.18 + 0.5 * 0.96
    k = int(((a % TAU) / TAU) * 360) % 360
    return _HEART[k]


PLATES = ["round", "square", "hexagon", "flower", "cloud", "star", "heart"]


def plate_icon(kind):
    def draw(ctx):
        n = 180
        outer = [(96 + math.cos(i / n * TAU) * 74 * plate_r(kind, i / n * TAU), 96 + math.sin(i / n * TAU) * 74 * plate_r(kind, i / n * TAU)) for i in range(n)]
        inner = [(96 + (x - 96) * 0.78, 96 + (y - 96) * 0.78) for x, y in outer]
        blob(ctx, poly(outer), "#cfc8ff", outline=INK, ow=7, gloss=0.3, shine=False)
        blob(ctx, poly(inner), "#e9e4ff", outline=None, gloss=0.2, shine=False, shade=0.0)
        arc_line(ctx, 74, 74, 18, math.pi * 1.05, math.pi * 1.45, "#ffffff", 6, 0.8)
    return draw


for pk in PLATES:
    ICONS["plate_" + pk] = plate_icon(pk)


# ---- jelly shapes (molds), seen from the front ---------------------------------------------------------------

MOLD_COL = "#ff7eb6"


def mold(fn):
    def draw(ctx):
        flat(ctx, ellipse(96, 172, 70, 10), "#000000", 0.18)
        fn(ctx)
    return draw


def m_dome(ctx):
    slime(ctx, 96, 140, 82, 76, MOLD_COL)


def m_heart(ctx):
    blob(ctx, heart(96, 100, 74), "#ff4d8d", outline=INK, ow=7)
    face(ctx, 96, 96, 40)


def m_star(ctx):
    blob(ctx, star(96, 104, 84, 44, round_=14), "#ffd23f", outline=INK, ow=7)
    face(ctx, 96, 106, 34)


def m_cube(ctx):
    blob(ctx, poly([(40, 66), (96, 40), (152, 66), (96, 92)]), light("#22e3ff", 0.35), outline=INK, ow=7, gloss=0.2, shine=False)
    blob(ctx, rounded_poly([(40, 66), (96, 92), (96, 168), (40, 142)], 8), "#22e3ff", outline=INK, ow=7, gloss=0.2, shine=False)
    blob(ctx, rounded_poly([(96, 92), (152, 66), (152, 142), (96, 168)], 8), dark("#22e3ff", 0.18), outline=INK, ow=7, gloss=0.1, shine=False)
    ctx.save()
    ctx.translate(68, 116)
    ctx.transform(cairo.Matrix(1, 0.46, 0, 1, 0, 0))
    face(ctx, 0, 0, 28)
    ctx.restore()


def m_flower(ctx):
    petals = [circle(96 + math.cos(a) * 46, 104 + math.sin(a) * 46, 36) for a in [i / 6 * TAU - math.pi / 2 for i in range(6)]]
    blob(ctx, petals + [circle(96, 104, 50)], "#c9a8ff", outline=INK, ow=7)
    blob(ctx, circle(96, 104, 34), "#ffe066", outline=INK, ow=5, shine=False)
    face(ctx, 96, 102, 26)


def ears(ctx, kind, col):
    if kind == "bear":
        for sx in (-1, 1):
            blob(ctx, circle(96 + sx * 50, 62, 22), col, outline=INK, ow=7, shine=False)
            flat(ctx, circle(96 + sx * 50, 62, 11), "#ffd1dc")
    elif kind == "bunny":
        for sx in (-1, 1):
            blob(ctx, ellipse(96 + sx * 28, 42, 15, 40, sx * 0.18), col, outline=INK, ow=7, shine=False)
            flat(ctx, ellipse(96 + sx * 28, 46, 7, 26, sx * 0.18), "#ffb6d5")
    elif kind == "cat":
        for sx in (-1, 1):
            blob(ctx, rounded_poly([(96 + sx * 22, 76), (96 + sx * 64, 34), (96 + sx * 72, 92)], 8), col, outline=INK, ow=7, shine=False)
            flat(ctx, poly([(96 + sx * 38, 74), (96 + sx * 60, 50), (96 + sx * 62, 82)]), "#ffb6d5")


def m_animal(kind, col):
    def draw(ctx):
        ears(ctx, kind, col)
        slime(ctx, 96, 146, 80, 74, col, faces=False)
        face(ctx, 96, 112, 42)
        if kind == "cat":
            for sx in (-1, 1):
                for k in (-1, 1):
                    line(ctx, [(96 + sx * 46, 124 + k * 6), (96 + sx * 72, 120 + k * 10)], INK, 3)
        if kind == "bear":
            blob(ctx, ellipse(96, 132, 18, 12), light(col, 0.4), outline=None, gloss=0, shine=False)
            flat(ctx, ellipse(96, 128, 7, 5), INK)
    return draw


def m_pudding(ctx):
    blob(ctx, smooth([(44, 160), (54, 70), (70, 52), (122, 52), (138, 70), (148, 160)], 0.4), "#ffd88a", outline=INK, ow=7, gloss=0.5)
    blob(ctx, smooth([(54, 70), (70, 48), (122, 48), (138, 70), (130, 86), (116, 78), (104, 92), (88, 80), (74, 90), (60, 82)], 0.45), "#8a4a1c", outline=None, gloss=0.5, shine=True)
    face(ctx, 96, 118, 36)


def m_drop(ctx):
    blob(ctx, drop(96, 112, 64), "#22e3ff", outline=INK, ow=7)
    face(ctx, 96, 126, 36)


MOLDS = {"dome": m_dome, "heart": m_heart, "star": m_star, "cube": m_cube, "flower": m_flower,
         "bear": m_animal("bear", "#c8946a"), "bunny": m_animal("bunny", "#f4f1ff"), "cat": m_animal("cat", "#ffb14d"),
         "pudding": m_pudding, "drop": m_drop}

for mk, mf in MOLDS.items():
    ICONS["mold_" + mk] = mold(mf)


@icon("face")
def _face(ctx):
    blob(ctx, circle(96, 96, 76), "#ffe066", outline=INK, ow=8)
    face(ctx, 96, 88, 64, "open")
