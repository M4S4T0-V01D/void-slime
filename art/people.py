"""The shop's customers: round portraits, one per name in model/Orders.kt."""
import math

from artlib import *
from icons import ICONS, bubble_at

SKIN = ("#ffd8b8", "#e8b48a", "#c98c5e", "#8d5a3b")


def portrait(bg, draw):
    def fn(ctx):
        blob(ctx, circle(96, 96, 86), bg, outline=INK, ow=7, gloss=0.25, shine=False, shade=0.15)
        ctx.save()
        circle(96, 96, 86)(ctx)
        ctx.clip()
        # Soft rays in the background.
        for i in range(10):
            a = i / 10 * TAU
            ctx.new_path()
            ctx.move_to(96, 96)
            ctx.arc(96, 96, 120, a, a + 0.3)
            ctx.close_path()
            ctx.set_source_rgba(1, 1, 1, 0.12)
            ctx.fill()
        draw(ctx)
        ctx.restore()
        arc_line(ctx, 96, 96, 86, 0, TAU, INK, 10)
    return fn


def body(ctx, col, collar=None):
    blob(ctx, smooth([(30, 200), (40, 160), (70, 146), (122, 146), (152, 160), (162, 200)], 0.4), col, outline=INK, ow=6, shine=False, gloss=0.2)
    if collar:
        blob(ctx, poly([(80, 146), (96, 166), (112, 146)]), collar, outline=INK, ow=4, shine=False, gloss=0)


def head(ctx, skin, x=96, y=92, r=46):
    blob(ctx, circle(x, y, r), skin, outline=INK, ow=6, gloss=0.25, shine=False, shade=0.15)


def ears_side(ctx, skin, y=96):
    for sx in (-1, 1):
        blob(ctx, circle(96 + sx * 46, y, 10), skin, outline=INK, ow=5, shine=False, gloss=0)


def mia(ctx):
    body(ctx, "#ff7eb6", "#ffffff")
    for sx in (-1, 1):
        blob(ctx, ellipse(96 + sx * 56, 104, 18, 30, sx * -0.3), "#6b3a1f", outline=INK, ow=6, shine=False)
    head(ctx, SKIN[0])
    blob(ctx, smooth([(48, 96), (52, 60), (80, 42), (112, 42), (140, 60), (144, 96), (130, 72), (110, 64), (90, 72), (70, 66), (56, 80)], 0.45), "#7a4424", outline=INK, ow=6, shine=False, gloss=0.4)
    for sx in (-1, 1):
        blob(ctx, [ellipse(96 + sx * 44 - 10, 70, 12, 8, 0.4), ellipse(96 + sx * 44 + 10, 70, 12, 8, -0.4)], "#ff4d8d", outline=INK, ow=4, shine=False, gloss=0)
    face(ctx, 96, 100, 40, "open")


def leo(ctx):
    body(ctx, "#3d7bff", "#ffe066")
    ears_side(ctx, SKIN[1])
    head(ctx, SKIN[1])
    spikes = [(46, 92), (44, 66), (36, 58), (58, 50), (60, 34), (80, 44), (94, 24), (106, 44), (126, 32), (130, 52), (152, 54), (144, 70), (146, 92), (130, 70), (100, 66), (70, 72)]
    blob(ctx, rounded_poly(spikes, 6), "#ff8a3d", outline=INK, ow=6, shine=False, gloss=0.3)
    face(ctx, 96, 102, 40, "happy", blush=False)
    for sx in (-1, 1):
        for k in range(3):
            flat(ctx, circle(96 + sx * (28 + k * 5), 116 + (k % 2) * 4, 2.2), "#c2643a")


def grandma(ctx):
    body(ctx, "#a970ff", "#ffffff")
    blob(ctx, circle(96, 44, 22), "#e8e6f0", outline=INK, ow=6, shine=False)
    head(ctx, SKIN[0])
    blob(ctx, smooth([(50, 94), (54, 62), (80, 48), (112, 48), (138, 62), (142, 94), (128, 70), (96, 64), (64, 70)], 0.45), "#e8e6f0", outline=INK, ow=6, shine=False, gloss=0.3)
    blob(ctx, [circle(138, 58, 9), circle(146, 50, 9), circle(130, 50, 9)], "#ff4d6d", outline=INK, ow=4, shine=False)
    face(ctx, 96, 102, 40, "closed")
    for sx in (-1, 1):
        arc_line(ctx, 96 + sx * 20, 100, 15, 0, TAU, INK, 4)
    line(ctx, [(91, 100), (101, 100)], INK, 4)


def captain(ctx):
    body(ctx, "#f4f1ff", "#ff4d6d")
    blob(ctx, circle(96, 92, 58), "#ffffff", outline=INK, ow=6, gloss=0.4)
    blob(ctx, ellipse(96, 98, 42, 36), "#2a2d6b", outline=INK, ow=5, shine=False, gloss=0)
    ctx.save()
    ellipse(96, 98, 42, 36)(ctx)
    ctx.clip()
    head(ctx, SKIN[2], 96, 104, 34)
    face(ctx, 96, 104, 30, "happy")
    flat(ctx, ellipse(78, 80, 18, 8, -0.4), "#ffffff", 0.45)
    ctx.restore()
    blob(ctx, capsule(140, 52, 152, 30, 4), "#d7d3ea", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, circle(152, 28, 8), "#ff4d6d", outline=INK, ow=4, shine=False)
    blob(ctx, star(60, 160, 12, 6), "#ffe066", outline=INK, ow=3, shine=False, gloss=0)


def pixel(ctx):
    body(ctx, "#8aa0c8")
    blob(ctx, capsule(96, 46, 96, 26, 4), "#b7c3dc", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, circle(96, 24, 9), "#ff4d6d", outline=INK, ow=4, shine=False)
    for sx in (-1, 1):
        blob(ctx, rrect(96 + sx * 56 - 8, 78, 16, 30, 6), "#ffb14d", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, rrect(42, 44, 108, 96, 26), "#b7c3dc", outline=INK, ow=6, gloss=0.4)
    blob(ctx, rrect(54, 58, 84, 64, 16), "#1b2340", outline=INK, ow=4, gloss=0.2, shine=False)
    for sx in (-1, 1):
        glow(ctx, 96 + sx * 20, 84, 18, "#22e3ff", 0.6)
        flat(ctx, rrect(96 + sx * 20 - 7, 76, 14, 18, 5), "#7ff4ff")
    arc_line(ctx, 96, 96, 14, 0.3, math.pi - 0.3, "#7ff4ff", 4)


def nova(ctx):
    for sx in (-1, 1):
        blob(ctx, [ellipse(96 + sx * 64, 94, 30, 44, sx * 0.5), ellipse(96 + sx * 58, 140, 20, 26, sx * -0.4)], "#bff6ff", outline=INK, ow=5, alpha=0.9, shine=False)
    body(ctx, "#6ef2b4")
    head(ctx, SKIN[0])
    blob(ctx, smooth([(46, 110), (48, 62), (78, 42), (114, 42), (144, 62), (146, 110), (134, 80), (110, 66), (82, 72), (60, 80)], 0.45), "#c9a8ff", outline=INK, ow=6, shine=False, gloss=0.4)
    blob(ctx, star(96, 46, 16, 7), "#ffe066", outline=INK, ow=4, shine=False)
    face(ctx, 96, 102, 40, "happy")
    sparkle(ctx, 40, 50, 10)
    sparkle(ctx, 154, 150, 8)


def whiskers(ctx):
    body(ctx, "#ffb14d")
    for sx in (-1, 1):
        blob(ctx, rounded_poly([(96 + sx * 18, 60), (96 + sx * 54, 26), (96 + sx * 56, 76)], 6), "#ffb14d", outline=INK, ow=6, shine=False)
        flat(ctx, poly([(96 + sx * 30, 58), (96 + sx * 50, 38), (96 + sx * 50, 64)]), "#ffb6d5")
    head(ctx, "#ffb14d", r=50)
    for x in (80, 96, 112):
        line(ctx, [(x, 46), (x, 60)], dark("#ffb14d", 0.25), 5)
    blob(ctx, ellipse(96, 116, 26, 16), "#fff1e0", outline=None, gloss=0, shine=False)
    face(ctx, 96, 96, 40, "happy", blush=True)
    flat(ctx, heart(96, 110, 6), "#ff6b8a")
    for sx in (-1, 1):
        for k in (-1, 1):
            line(ctx, [(96 + sx * 30, 116 + k * 4), (96 + sx * 64, 112 + k * 10)], INK, 3)
    blob(ctx, [poly([(96, 152), (74, 140), (74, 164)]), poly([(96, 152), (118, 140), (118, 164)])], "#ff4d8d", outline=INK, ow=4, shine=False, gloss=0)


def bubbles(ctx):
    for i in range(6):
        x = 46 + i * 20
        curve(ctx, (x, 130), (x - 10, 160), (x + 14, 176), (x + 2, 196), INK, 20)
        curve(ctx, (x, 130), (x - 10, 160), (x + 14, 176), (x + 2, 196), "#ff7eb6", 10)
    blob(ctx, smooth([(40, 136), (42, 76), (70, 42), (122, 42), (150, 76), (152, 136), (120, 146), (72, 146)], 0.45), "#ff7eb6", outline=INK, ow=6)
    face(ctx, 96, 104, 44, "o")
    for x, y in ((64, 66), (124, 60), (110, 72)):
        flat(ctx, circle(x, y, 5), "#ffb6d5")
    bubble_at(ctx, 156, 44, 12)
    bubble_at(ctx, 36, 60, 8)


def goo(ctx):
    body(ctx, "#ffffff", "#22e3ff")
    head(ctx, SKIN[1])
    blob(ctx, [circle(54, 64, 20), circle(70, 44, 22), circle(96, 36, 22), circle(122, 44, 22), circle(138, 64, 20), circle(46, 86, 16), circle(146, 86, 16)], "#f4f1ff", outline=INK, ow=6, shine=False, gloss=0.2)
    blob(ctx, rrect(52, 62, 88, 14, 7), "#5a4a6b", outline=INK, ow=4, shine=False, gloss=0)
    for sx in (-1, 1):
        blob(ctx, circle(96 + sx * 22, 66, 14), "#7ff4ff", outline=INK, ow=5, shine=False)
    face(ctx, 96, 102, 38, "open", blush=False)
    blob(ctx, [ellipse(84, 122, 16, 7, 0.2), ellipse(108, 122, 16, 7, -0.2)], "#f4f1ff", outline=INK, ow=4, shine=False, gloss=0)


def luna(ctx):
    body(ctx, "#f4f1ff")
    for i, col in enumerate(("#ff9ec7", "#ffe28a", "#9dffcf", "#9be7ff", "#c9a8ff")):
        blob(ctx, ellipse(132 + i * 5, 64 + i * 18, 20, 16, 0.4), col, outline=INK, ow=4, shine=False, gloss=0)
    head(ctx, "#ffffff", r=48)
    blob(ctx, poly([(84, 52), (96, 6), (108, 52)]), "#ffd36b", outline=INK, ow=5, shine=False)
    for k in range(3):
        line(ctx, [(88 + k * 3, 42 - k * 12), (104 - k * 3, 36 - k * 12)], "#e8a21a", 3)
    for sx in (-1, 1):
        blob(ctx, rounded_poly([(96 + sx * 30, 54), (96 + sx * 46, 30), (96 + sx * 50, 64)], 5), "#ffffff", outline=INK, ow=5, shine=False)
    blob(ctx, [ellipse(70, 54, 22, 14, -0.4), ellipse(56, 70, 16, 14)], "#ff9ec7", outline=INK, ow=4, shine=False, gloss=0)
    face(ctx, 96, 100, 40, "happy")


def bao(ctx):
    body(ctx, "#2a2730")
    for sx in (-1, 1):
        blob(ctx, circle(96 + sx * 42, 54, 18), "#2a2730", outline=INK, ow=6, shine=False)
    head(ctx, "#ffffff", r=50)
    for sx in (-1, 1):
        flat(ctx, ellipse(96 + sx * 20, 96, 14, 18, sx * 0.5), "#2a2730")
        flat(ctx, circle(96 + sx * 20, 94, 7), "#ffffff")
        flat(ctx, circle(96 + sx * 20, 95, 4.5), INK)
    flat(ctx, ellipse(96, 114, 8, 5), INK)
    arc_line(ctx, 96, 116, 8, 0.4, math.pi - 0.4, INK, 3)
    for sx in (-1, 1):
        flat(ctx, ellipse(96 + sx * 34, 118, 9, 5), "#ff9ec7", 0.6)
    blob(ctx, [capsule(140, 140, 156, 90, 6)], "#8be04e", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, ellipse(160, 92, 12, 6, -0.6), "#6ef2b4", outline=INK, ow=3, shine=False, gloss=0)


def ziggy(ctx):
    body(ctx, "#a970ff")
    for sx in (-1, 1):
        curve(ctx, (96 + sx * 20, 52), (96 + sx * 24, 34), (96 + sx * 40, 30), (96 + sx * 44, 22), INK, 10)
        curve(ctx, (96 + sx * 20, 52), (96 + sx * 24, 34), (96 + sx * 40, 30), (96 + sx * 44, 22), "#8be04e", 4)
        blob(ctx, circle(96 + sx * 44, 20, 9), "#ffe066", outline=INK, ow=4, shine=False)
    blob(ctx, smooth([(46, 80), (56, 50), (96, 40), (136, 50), (146, 80), (132, 124), (96, 144), (60, 124)], 0.45), "#8be04e", outline=INK, ow=6, gloss=0.35)
    for sx in (-1, 1):
        blob(ctx, ellipse(96 + sx * 22, 92, 16, 22, sx * -0.5), "#14102a", outline=None, gloss=0, shine=False)
        flat(ctx, ellipse(96 + sx * 22 - 4, 84, 6, 8, sx * -0.5), "#ffffff", 0.9)
    arc_line(ctx, 96, 118, 10, 0.3, math.pi - 0.3, INK, 4)


def kim(ctx):
    body(ctx, "#ff4d6d", "#ffffff")
    curve(ctx, (78, 148), (84, 168), (108, 168), (114, 148), INK, 4)
    blob(ctx, rrect(100, 162, 22, 12, 5), "#d7d3ea", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, ellipse(150, 70, 16, 28, 0.6), "#2a2730", outline=INK, ow=5, shine=False, gloss=0.3)
    head(ctx, SKIN[2])
    blob(ctx, smooth([(48, 92), (52, 58), (80, 42), (112, 42), (140, 58), (144, 92), (130, 70), (96, 62), (62, 70)], 0.45), "#2a2730", outline=INK, ow=6, shine=False, gloss=0.4)
    blob(ctx, rrect(48, 62, 96, 16, 8), "#ff4d6d", outline=INK, ow=4, shine=False, gloss=0)
    face(ctx, 96, 102, 40, "open")


def fizz(ctx):
    for x, y, r in ((40, 60, 10), (150, 50, 13), (160, 110, 8), (30, 120, 7)):
        bubble_at(ctx, x, y, r)
    blob(ctx, rounded_poly([(80, 22), (112, 22), (112, 54), (138, 80), (138, 190), (54, 190), (54, 80), (80, 54)], 14), "#6ef2b4", outline=INK, ow=6, alpha=0.95)
    blob(ctx, rrect(54, 104, 84, 50, 4), "#ff4d8d", outline=INK, ow=4, shine=False, gloss=0.3)
    blob(ctx, rrect(76, 12, 40, 18, 5), "#ff4d6d", outline=INK, ow=5, shine=False)
    face(ctx, 96, 124, 34, "happy")
    for x, y, r in ((70, 84, 4), (116, 74, 3), (106, 92, 5), (80, 172, 4), (118, 168, 3)):
        flat(ctx, circle(x, y, r), "#ffffff", 0.8)


def penny(ctx):
    body(ctx, "#2a2730")
    blob(ctx, circle(96, 96, 54), "#2a2d3c", outline=INK, ow=6, gloss=0.3)
    blob(ctx, smooth([(54, 104), (64, 74), (86, 70), (96, 80), (106, 70), (128, 74), (138, 104), (120, 140), (72, 140)], 0.45), "#ffffff", outline=None, gloss=0, shine=False)
    face(ctx, 96, 96, 40, "happy")
    blob(ctx, rounded_poly([(84, 110), (108, 110), (96, 124)], 4), "#ff9f1c", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, [ellipse(124, 48, 14, 10, 0.5), ellipse(144, 52, 14, 10, -0.4)], "#ff7eb6", outline=INK, ow=4, shine=False, gloss=0)
    blob(ctx, circle(134, 50, 6), "#ff4d8d", outline=INK, ow=3, shine=False, gloss=0)


def rex(ctx):
    body(ctx, "#4cc96a")
    spikes = [poly([(96 + dx - 12, 50 - abs(dx) * 0.15 + 10), (96 + dx, 30 - abs(dx) * 0.2 + 10), (96 + dx + 12, 50 - abs(dx) * 0.15 + 10)]) for dx in (-32, 0, 32)]
    blob(ctx, spikes, "#ff8a3d", outline=INK, ow=5, shine=False)
    blob(ctx, smooth([(42, 96), (52, 56), (96, 46), (140, 56), (156, 96), (152, 130), (96, 146), (44, 130)], 0.45), "#6ee08a", outline=INK, ow=6, gloss=0.35)
    face(ctx, 96, 88, 46, "happy", blush=True)
    ctx.new_path()
    ctx.move_to(60, 118)
    ctx.curve_to(80, 138, 112, 138, 132, 118)
    ctx.set_source_rgb(*INK)
    ctx.set_line_width(5)
    ctx.stroke()
    for x in (74, 92, 110):
        flat(ctx, poly([(x, 126), (x + 6, 136), (x + 12, 126)]), "#ffffff")
    for x, y in ((66, 72), (128, 70), (120, 132)):
        flat(ctx, circle(x, y, 5), "#4cc96a")


CUSTOMERS = {"mia": ("#ffd1e6", mia), "leo": ("#bfe3ff", leo), "rose": ("#ead9ff", grandma), "zed": ("#1d1a4a", captain),
             "pixel": ("#c4f5e6", pixel), "nova": ("#fff0b8", nova), "whiskers": ("#ffe2b8", whiskers),
             "bubbles": ("#9be7ff", bubbles), "goo": ("#d6ffe9", goo), "luna": ("#ffd1f0", luna), "bao": ("#c8f5b0", bao),
             "ziggy": ("#2b1f5c", ziggy), "kim": ("#ffe066", kim), "fizz": ("#ffd1dc", fizz), "penny": ("#bfefff", penny),
             "rex": ("#fff3c4", rex)}

for cid, (bg, fn) in CUSTOMERS.items():
    ICONS["cust_" + cid] = portrait(bg, fn)
