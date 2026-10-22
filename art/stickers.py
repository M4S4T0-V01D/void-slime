"""Sticker book art: a scalloped sticker with a picture in the middle and, for the big goals, a ribbon with the number."""
import math

from artlib import *
from icons import ICONS, sticker_badge
from items import arrow

# id (as in model/Stickers.kt), background, picture (an icon name), ribbon text
STICKERS = [
    ("first-poke", "#9be7ff", "tool_poke", None),
    ("poke-500", "#c9a8ff", "tool_poke", "500"),
    ("poke-5000", "#ffd36b", "tool_poke", "5000"),
    ("stretch-50", "#ffb6d5", "tool_stretch", "50"),
    ("snap-10", "#ff9a6b", "snap", "10"),
    ("squish-100", "#a8f0d0", "tool_squish", "100"),
    ("roll-30", "#ffe2b8", "tool_roll", "30"),
    ("pop-100", "#9be7ff", "bubbles", "100"),
    ("pop-1000", "#ffd36b", "balloon", "1000"),
    ("crunch-300", "#ffe2b8", "pretzel", "300"),
    ("made-1", "#a8f0d0", "flask", None),
    ("made-10", "#c9a8ff", "jar", "10"),
    ("made-30", "#ffd36b", "museum", "30"),
    ("order-1", "#ffe2b8", "bell", None),
    ("order-25", "#ff9ec7", "shop", "25"),
    ("perfect-10", "#ffd36b", "stars3", "10"),
    ("rush-25", "#9be7ff", "timer", "25"),
    ("rush-60", "#ffe066", "bolt", "60"),
    ("streak-3", "#ffb6d5", "calendar", "3"),
    ("streak-7", "#c9a8ff", "calendar", "7"),
    ("time-30m", "#a8f0d0", "zen", None),
    ("lights-off", "#3a2f6e", "moon", None),
    ("shopper-5", "#ffb6d5", "bag", "5"),
    ("shopper-25", "#ffd36b", "coins", "25"),
    ("slap-50", "#ffe2b8", "tool_slap", "50"),
    ("rake-30", "#a8f0d0", "tool_rake", "30"),
    ("stamp-25", "#ff9ec7", "tool_stamp", "25"),
    ("shake-60", "#9be7ff", "tool_shaker", None),
    ("magnet-60", "#d7d3ea", "tool_magnet", None),
    ("wand-30", "#c9a8ff", "tool_wand", "30"),
    ("gold-25", "#ffe066", "coin", "25"),
    ("quest-10", "#ffb6d5", "target", "10"),
    ("quest-days-5", "#ffd36b", "trophy", "5"),
    ("splat-50", "#ffb6d5", "splat", "50"),
    ("merge-100", "#a8f0d0", "merge", "100"),
    ("blister-25", "#9be7ff", "blister", "25"),
    ("squeeze-30", "#ff9ec7", "tool_squeeze", "30"),
    ("scoop-25", "#9be7ff", "tool_scoop", "25"),
    ("bonk-50", "#ffe066", "tool_hammer", "50"),
    ("cut-20", "#ffb6d5", "tool_cutter", "20"),
    ("whisk-60", "#a8f0d0", "tool_whisk", None),
    ("dry-60", "#d7d3ea", "tool_dryer", None),
    ("race-1", "#a8f0d0", "flag", None),
    ("race-win-10", "#ffd36b", "race", "10"),
    ("vip-5", "#c9a8ff", "crown", "5"),
    ("combo-5", "#ff9a6b", "bolt", "5"),
]


# A 3 x 5 pixel font for the ribbon numbers, so they stay sharp on the pixel grid.
DIGITS = {
    "0": ("111", "101", "101", "101", "111"), "1": ("010", "110", "010", "010", "111"),
    "2": ("111", "001", "111", "100", "111"), "3": ("111", "001", "011", "001", "111"),
    "4": ("101", "101", "111", "001", "001"), "5": ("111", "100", "111", "001", "111"),
    "6": ("111", "100", "111", "101", "111"), "7": ("111", "001", "010", "010", "010"),
    "8": ("111", "101", "111", "101", "111"), "9": ("111", "101", "111", "001", "111"),
}


def pixel_digits(ctx, label, x, y):
    """Draws label centred at (x, y) in whole pixels of the 64-pixel icon grid (3 canvas units each)."""
    u = S / PIXEL
    w = (len(label) * 4 - 1) * u
    x0 = round((x - w / 2) / u) * u
    y0 = round((y - 2.5 * u) / u) * u
    for i, ch in enumerate(label):
        for row, bits in enumerate(DIGITS[ch]):
            for col, b in enumerate(bits):
                if b == "1":
                    ctx.rectangle(x0 + (i * 4 + col) * u, y0 + row * u, u, u)
    ctx.set_source_rgb(1, 1, 1)
    ctx.fill()


def ribbon(ctx, x, y, label):
    w = 30 + len(label) * 15
    for sx in (-1, 1):
        blob(ctx, poly([(x + sx * (w / 2 - 6), y - 12), (x + sx * (w / 2 + 22), y - 8), (x + sx * (w / 2 + 12), y + 6),
                        (x + sx * (w / 2 + 22), y + 20), (x + sx * (w / 2 - 6), y + 18)]), "#c2125b", outline=INK, ow=5, shine=False, gloss=0)
    blob(ctx, rrect(x - w / 2, y - 18, w, 36, 8), "#ff4d8d", outline=INK, ow=5, shine=False, gloss=0.3)
    pixel_digits(ctx, label, x, y)


def sticker(bg, pic, label):
    def draw(ctx):
        def inner(cx):
            cx.save()
            cx.translate(96, 90 if label else 96)
            cx.scale(0.6, 0.6)
            cx.translate(-96, -96)
            ICONS[pic](cx)
            cx.restore()
        sticker_badge(ctx, 96, 92, 86, bg, inner)
        if label:
            ribbon(ctx, 96, 160, label)
    return draw


# Pictures only stickers use.
def _snap(ctx):
    blob(ctx, star(96, 96, 84, 46, n=9, round_=6), "#ffe066", outline=INK, ow=7)
    blob(ctx, star(96, 96, 50, 28, n=9, round_=4), "#ff8a3d", outline=None, shine=False)


def _balloon(ctx):
    curve(ctx, (96, 140), (80, 160), (112, 170), (96, 190), INK, 4)
    blob(ctx, [ellipse(96, 80, 56, 66), poly([(86, 150), (106, 150), (96, 138)])], "#ff4d6d", outline=INK, ow=7)


def _pretzel(ctx):
    for cc, w in ((INK, 40), ("#c27a3a", 26)):
        ctx.new_path()
        ctx.move_to(60, 156)
        ctx.curve_to(150, 100, 170, 40, 120, 40)
        ctx.curve_to(96, 40, 90, 80, 96, 96)
        ctx.curve_to(102, 80, 96, 40, 72, 40)
        ctx.curve_to(22, 40, 42, 100, 132, 156)
        ctx.set_source_rgb(*c(cc))
        ctx.set_line_width(w)
        ctx.stroke()
    for x, y in ((70, 60), (124, 56), (150, 90), (44, 92), (96, 120)):
        flat(ctx, rrect(x - 4, y - 2, 8, 5, 2), "#ffffff")


def _museum(ctx):
    blob(ctx, poly([(20, 70), (96, 24), (172, 70)]), "#fff4e6", outline=INK, ow=7, shine=False)
    for x in (44, 80, 112, 148):
        blob(ctx, rrect(x - 10, 74, 20, 70, 4), "#fff4e6", outline=INK, ow=6, shine=False, gloss=0)
    blob(ctx, rrect(20, 144, 152, 24, 6), "#fff4e6", outline=INK, ow=7, shine=False)
    blob(ctx, slime_dome(96, 62, 18, 16), "#a970ff", outline=INK, ow=4, shine=False)


def _stars3(ctx):
    for x, y, r in ((48, 116, 36), (144, 116, 36), (96, 80, 48)):
        blob(ctx, star(x, y, r, r * 0.5, round_=5), "#ffd23f", outline="#a35a00", ow=6)


def _bag(ctx):
    arc_line(ctx, 96, 70, 30, math.pi, 0, INK, 18)
    arc_line(ctx, 96, 70, 30, math.pi, 0, "#ff7eb6", 8)
    blob(ctx, rounded_poly([(36, 66), (156, 66), (168, 170), (24, 170)], 14), "#a970ff", outline=INK, ow=8)
    blob(ctx, heart(96, 118, 22), "#ffe066", outline=INK, ow=4, shine=False)


def _splat(ctx):
    blob(ctx, slime_dome(96, 168, 70, 50), "#ff7eb6", outline=INK, ow=7)
    face(ctx, 96, 146, 28, "o")
    for x, y, r in ((36, 70, 18), (150, 56, 22), (100, 34, 13), (166, 120, 12)):
        blob(ctx, circle(x, y, r), "#ff7eb6", outline=INK, ow=5)


def _merge(ctx):
    blob(ctx, slime_dome(110, 170, 66, 70), "#6ef2b4", outline=INK, ow=7)
    face(ctx, 116, 136, 28, "happy")
    for x, y, r in ((28, 160, 16), (44, 120, 11)):
        blob(ctx, circle(x, y, r), "#6ef2b4", outline=INK, ow=5)
    arrow(ctx, 22, 84, 52, 100, "#ffe066", 7)


def _blister(ctx):
    blob(ctx, slime_dome(96, 172, 84, 54), "#a970ff", outline=INK, ow=7)
    face(ctx, 70, 150, 22, "o")
    blob(ctx, circle(122, 96, 44), "#e0c8ff", outline=INK, ow=7, alpha=0.85)
    sparkle(ctx, 108, 80, 12)


def _crown(ctx):
    blob(ctx, poly([(30, 150), (24, 60), (64, 98), (96, 40), (128, 98), (168, 60), (162, 150)]), "#ffc93c", outline="#7a4300", ow=8)
    for x, col in ((60, "#ff4d6d"), (96, "#22e3ff"), (132, "#6ef2b4")):
        blob(ctx, circle(x, 128, 11), col, outline=INK, ow=4, shine=False)


for name, fn in (("splat", _splat), ("droplet", _splat), ("merge", _merge), ("blister", _blister), ("crown", _crown), ("snap", _snap), ("balloon", _balloon), ("pretzel", _pretzel), ("museum", _museum), ("stars3", _stars3), ("bag", _bag)):
    ICONS["pic_" + name] = fn

for sid, bg, pic, label in STICKERS:
    pic = pic if pic in ICONS else "pic_" + pic
    ICONS["st_" + sid.replace("-", "_")] = sticker(bg, pic, label)
