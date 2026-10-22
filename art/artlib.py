"""Drawing helpers for the game art: glossy, outlined "sticker" shapes in cairo, saved as webp with a soft shadow.

Everything is drawn on a 192 px canvas (icons) or bigger (backdrops, logo). A shape is a function that adds
subpaths to the context; several subpaths drawn together merge into one outlined blob.
"""
import math
import os

import cairo
from PIL import Image, ImageFilter

TAU = math.pi * 2
S = 192
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app/src/main/res/drawable-nodpi")
PREVIEW = os.path.join(ROOT, "art/out")
INK = (0.13, 0.07, 0.24)  # the dark outline most icons share


def hexc(h, a=1.0):
    h = h.lstrip("#")
    return (int(h[0:2], 16) / 255, int(h[2:4], 16) / 255, int(h[4:6], 16) / 255) if a == 1.0 else \
        (int(h[0:2], 16) / 255, int(h[2:4], 16) / 255, int(h[4:6], 16) / 255, a)


def c(x):
    return hexc(x) if isinstance(x, str) else x


def mix(a, b, t):
    a, b = c(a), c(b)
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def light(col, t):
    return mix(col, (1, 1, 1), t)


def dark(col, t):
    return mix(col, (0, 0, 0), t)


def canvas(w=S, h=S):
    surf = cairo.ImageSurface(cairo.FORMAT_ARGB32, w, h)
    ctx = cairo.Context(surf)
    ctx.set_line_join(cairo.LINE_JOIN_ROUND)
    ctx.set_line_cap(cairo.LINE_CAP_ROUND)
    return surf, ctx


def to_image(surf):
    """cairo's premultiplied BGRA to a PIL RGBA image."""
    surf.flush()
    w, h = surf.get_width(), surf.get_height()
    return Image.frombuffer("RGBa", (w, h), bytes(surf.get_data()), "raw", "BGRa", surf.get_stride(), 1).convert("RGBA")


def with_shadow(img, blur=5, dy=5, alpha=0.38):
    """A soft drop shadow under the drawing, from its own silhouette."""
    a = img.getchannel("A").point(lambda v: int(v * alpha))
    sh = Image.new("RGBA", img.size, (14, 6, 30, 0))
    sh.putalpha(a)
    sh = sh.filter(ImageFilter.GaussianBlur(blur))
    base = Image.new("RGBA", img.size, (0, 0, 0, 0))
    base.alpha_composite(sh, (0, dy))
    base.alpha_composite(img)
    return base


def save(surf, name, shadow=True, quality=92, lossless=False, size=None):
    os.makedirs(RES, exist_ok=True)
    os.makedirs(PREVIEW, exist_ok=True)
    img = to_image(surf)
    if shadow:
        img = with_shadow(img, blur=max(2, img.width // 40), dy=max(2, img.width // 38))
    if size:
        img = img.resize(size, Image.LANCZOS)
    img.save(os.path.join(PREVIEW, name + ".png"))
    img.save(os.path.join(RES, name + ".webp"), "WEBP", quality=quality, lossless=lossless, method=6)
    return img


# ---- paths -------------------------------------------------------------------------------------------------

def circle(x, y, r):
    def p(ctx):
        ctx.new_sub_path()
        ctx.arc(x, y, r, 0, TAU)
    return p


def ellipse(x, y, rx, ry, rot=0.0):
    def p(ctx):
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(rot)
        ctx.scale(rx, ry)
        ctx.new_sub_path()
        ctx.arc(0, 0, 1, 0, TAU)
        ctx.restore()
    return p


def rrect(x, y, w, h, r):
    def p(ctx):
        rr = min(r, w / 2, h / 2)
        ctx.new_sub_path()
        ctx.arc(x + w - rr, y + rr, rr, -math.pi / 2, 0)
        ctx.arc(x + w - rr, y + h - rr, rr, 0, math.pi / 2)
        ctx.arc(x + rr, y + h - rr, rr, math.pi / 2, math.pi)
        ctx.arc(x + rr, y + rr, rr, math.pi, 1.5 * math.pi)
        ctx.close_path()
    return p


def capsule(x1, y1, x2, y2, r):
    """A rounded bar from (x1,y1) to (x2,y2), r thick on each side."""
    def p(ctx):
        a = math.atan2(y2 - y1, x2 - x1)
        ctx.new_sub_path()
        ctx.arc(x2, y2, r, a - math.pi / 2, a + math.pi / 2)
        ctx.arc(x1, y1, r, a + math.pi / 2, a + 1.5 * math.pi)
        ctx.close_path()
    return p


def poly(pts):
    def p(ctx):
        ctx.new_sub_path()
        ctx.move_to(*pts[0])
        for q in pts[1:]:
            ctx.line_to(*q)
        ctx.close_path()
    return p


def smooth(pts, tension=0.5):
    """A closed smooth curve through the points (Catmull-Rom as Béziers)."""
    def p(ctx):
        n = len(pts)
        ctx.new_sub_path()
        ctx.move_to(*pts[0])
        for i in range(n):
            p0, p1, p2, p3 = pts[i - 1], pts[i], pts[(i + 1) % n], pts[(i + 2) % n]
            k = tension / 3
            ctx.curve_to(p1[0] + (p2[0] - p0[0]) * k, p1[1] + (p2[1] - p0[1]) * k,
                         p2[0] - (p3[0] - p1[0]) * k, p2[1] - (p3[1] - p1[1]) * k, p2[0], p2[1])
        ctx.close_path()
    return p


def star_pts(x, y, r1, r2, n=5, rot=-math.pi / 2):
    return [(x + (r1 if i % 2 == 0 else r2) * math.cos(rot + i * math.pi / n),
             y + (r1 if i % 2 == 0 else r2) * math.sin(rot + i * math.pi / n)) for i in range(n * 2)]


def star(x, y, r1, r2=None, n=5, rot=-math.pi / 2, round_=0.0):
    r2 = r2 if r2 is not None else r1 * 0.48
    pts = star_pts(x, y, r1, r2, n, rot)
    if round_ <= 0:
        return poly(pts)
    return rounded_poly(pts, round_)


def rounded_poly(pts, rad):
    def p(ctx):
        n = len(pts)
        ctx.new_sub_path()
        for i in range(n):
            a, b, cc = pts[i - 1], pts[i], pts[(i + 1) % n]
            v1 = (a[0] - b[0], a[1] - b[1])
            v2 = (cc[0] - b[0], cc[1] - b[1])
            l1, l2 = math.hypot(*v1), math.hypot(*v2)
            r = min(rad, l1 / 2, l2 / 2)
            s1 = (b[0] + v1[0] / l1 * r, b[1] + v1[1] / l1 * r)
            s2 = (b[0] + v2[0] / l2 * r, b[1] + v2[1] / l2 * r)
            if i == 0:
                ctx.move_to(*s1)
            else:
                ctx.line_to(*s1)
            ctx.curve_to(b[0], b[1], b[0], b[1], *s2)
        ctx.close_path()
    return p


def heart(x, y, s):
    """A plump heart centred near (x, y), about 2s wide."""
    def p(ctx):
        ctx.new_sub_path()
        ctx.move_to(x, y + s * 0.95)
        ctx.curve_to(x - s * 0.35, y + s * 0.62, x - s * 1.05, y + s * 0.22, x - s * 1.0, y - s * 0.3)
        ctx.curve_to(x - s * 0.96, y - s * 0.86, x - s * 0.22, y - s * 1.0, x, y - s * 0.48)
        ctx.curve_to(x + s * 0.22, y - s * 1.0, x + s * 0.96, y - s * 0.86, x + s * 1.0, y - s * 0.3)
        ctx.curve_to(x + s * 1.05, y + s * 0.22, x + s * 0.35, y + s * 0.62, x, y + s * 0.95)
        ctx.close_path()
    return p


def drop(x, y, s):
    """A teardrop with its point up."""
    def p(ctx):
        ctx.new_sub_path()
        ctx.move_to(x, y - s * 1.1)
        ctx.curve_to(x + s * 0.25, y - s * 0.6, x + s * 0.85, y - s * 0.1, x + s * 0.8, y + s * 0.38)
        ctx.curve_to(x + s * 0.75, y + s * 0.95, x - s * 0.75, y + s * 0.95, x - s * 0.8, y + s * 0.38)
        ctx.curve_to(x - s * 0.85, y - s * 0.1, x - s * 0.25, y - s * 0.6, x, y - s * 1.1)
        ctx.close_path()
    return p


def crescent(x, y, r, cut=0.62, rot=0.0):
    """A moon: a circle with a smaller offset circle taken out of it."""
    def p(ctx):
        ctx.save()
        ctx.translate(x, y)
        ctx.rotate(rot)
        # Where the two circles cross.
        d = r * cut
        r2 = r * 0.86
        xx = (d * d + r * r - r2 * r2) / (2 * d)
        yy = math.sqrt(max(0.0, r * r - xx * xx))
        a1 = math.atan2(yy, xx)
        b1 = math.atan2(yy, xx - d)
        ctx.new_sub_path()
        ctx.arc(0, 0, r, a1, TAU - a1)
        ctx.arc_negative(d, 0, r2, -b1, b1)
        ctx.close_path()
        ctx.restore()
    return p


def slime_dome(x, y, w, h, drips=True, wobble=0.0):
    """The classic slime blob: a soft dome with a puffy, lumpy bottom edge."""
    pts = []
    n = 28
    for i in range(n):
        t = i / n
        a = math.pi + t * math.pi  # top half, left to right
        px = x + math.cos(a) * w
        py = y + math.sin(a) * h * (1 + wobble * math.sin(t * 9))
        pts.append((px, py))
    bottom = []
    k = 7
    for i in range(k + 1):
        t = i / k
        px = x + w - t * 2 * w
        bump = (math.sin(t * math.pi * k) ** 2) * h * 0.09 if drips else 0
        bottom.append((px, y + h * 0.26 + bump))
    return smooth(pts + bottom, 0.5)


# ---- painting ----------------------------------------------------------------------------------------------

def path(ctx, shapes):
    ctx.new_path()
    for s in shapes:
        s(ctx)


def bounds(ctx, shapes):
    path(ctx, shapes)
    x1, y1, x2, y2 = ctx.fill_extents()
    ctx.new_path()
    return x1, y1, x2, y2


def blob(ctx, shapes, col, outline=INK, ow=9.0, gloss=0.55, shade=0.28, top=0.32, shine=True, grad=None,
         alpha=1.0):
    """Fill the union of the shapes with a glossy gradient, outlined, with a wet highlight on top."""
    if callable(shapes):
        shapes = [shapes]
    col = c(col)
    x1, y1, x2, y2 = bounds(ctx, shapes)
    w, h = x2 - x1, y2 - y1
    if outline is not None and ow > 0:
        path(ctx, shapes)
        ctx.set_source_rgba(*c(outline), alpha)
        ctx.set_line_width(ow * 2)
        ctx.stroke()
    path(ctx, shapes)
    if grad is not None:
        g = grad(x1, y1, x2, y2)
    else:
        g = cairo.LinearGradient(0, y1, 0, y2)
        g.add_color_stop_rgba(0, *light(col, top), alpha)
        g.add_color_stop_rgba(0.55, *col, alpha)
        g.add_color_stop_rgba(1, *dark(col, shade * 0.6), alpha)
    ctx.set_source(g)
    ctx.fill_preserve()
    # Shade toward the lower right, like light from the upper left.
    ctx.save()
    ctx.clip()
    rg = cairo.RadialGradient(x1 + w * 0.32, y1 + h * 0.3, min(w, h) * 0.2, x1 + w * 0.45, y1 + h * 0.45, max(w, h) * 0.85)
    rg.add_color_stop_rgba(0, 0, 0, 0, 0)
    rg.add_color_stop_rgba(0.7, 0, 0, 0, 0)
    rg.add_color_stop_rgba(1, *dark(col, 0.6), shade * alpha)
    ctx.set_source(rg)
    ctx.paint()
    if gloss > 0:
        ctx.save()
        ctx.translate(x1 + w * 0.36, y1 + h * 0.24)
        ctx.scale(w * 0.28, h * 0.14)
        ctx.arc(0, 0, 1, 0, TAU)
        ctx.restore()
        hg = cairo.LinearGradient(0, y1 + h * 0.1, 0, y1 + h * 0.38)
        hg.add_color_stop_rgba(0, 1, 1, 1, gloss * alpha)
        hg.add_color_stop_rgba(1, 1, 1, 1, 0.04 * alpha)
        ctx.set_source(hg)
        ctx.fill()
    if shine:
        ctx.arc(x1 + w * 0.72, y1 + h * 0.2, max(2.0, min(w, h) * 0.045), 0, TAU)
        ctx.set_source_rgba(1, 1, 1, 0.85 * alpha)
        ctx.fill()
    ctx.restore()


def flat(ctx, shapes, col, alpha=1.0, outline=None, ow=0.0):
    if callable(shapes):
        shapes = [shapes]
    if outline is not None:
        path(ctx, shapes)
        ctx.set_source_rgba(*c(outline), alpha)
        ctx.set_line_width(ow * 2)
        ctx.stroke()
    path(ctx, shapes)
    ctx.set_source_rgba(*c(col), alpha)
    ctx.fill()


def line(ctx, pts, col, w, alpha=1.0, close=False):
    ctx.new_path()
    ctx.move_to(*pts[0])
    for q in pts[1:]:
        ctx.line_to(*q)
    if close:
        ctx.close_path()
    ctx.set_source_rgba(*c(col), alpha)
    ctx.set_line_width(w)
    ctx.stroke()


def curve(ctx, p0, p1, p2, p3, col, w, alpha=1.0):
    ctx.new_path()
    ctx.move_to(*p0)
    ctx.curve_to(*p1, *p2, *p3)
    ctx.set_source_rgba(*c(col), alpha)
    ctx.set_line_width(w)
    ctx.stroke()


def arc_line(ctx, x, y, r, a0, a1, col, w, alpha=1.0):
    ctx.new_path()
    ctx.arc(x, y, r, a0, a1)
    ctx.set_source_rgba(*c(col), alpha)
    ctx.set_line_width(w)
    ctx.stroke()


def glow(ctx, x, y, r, col, alpha=0.6):
    g = cairo.RadialGradient(x, y, 0, x, y, r)
    g.add_color_stop_rgba(0, *c(col), alpha)
    g.add_color_stop_rgba(1, *c(col), 0)
    ctx.set_source(g)
    ctx.arc(x, y, r, 0, TAU)
    ctx.fill()


def sparkle(ctx, x, y, r, col=(1, 1, 1), alpha=1.0):
    """A four-point twinkle."""
    ctx.new_path()
    ctx.move_to(x, y - r)
    for i in range(4):
        a = -math.pi / 2 + i * math.pi / 2
        b = a + math.pi / 4
        ctx.curve_to(x + math.cos(b) * r * 0.16, y + math.sin(b) * r * 0.16, x + math.cos(b) * r * 0.16,
                     y + math.sin(b) * r * 0.16, x + math.cos(a + math.pi / 2) * r, y + math.sin(a + math.pi / 2) * r)
    ctx.close_path()
    ctx.set_source_rgba(*c(col), alpha)
    ctx.fill()


def face(ctx, x, y, s, mood="happy", blush=True, eye_col=INK):
    """Two shiny eyes and a little mouth, s is roughly the eye spacing."""
    ex = s * 0.5
    er = s * 0.17
    for sx in (-1, 1):
        if mood == "closed":
            arc_line(ctx, x + sx * ex, y + er * 0.2, er * 0.9, math.pi * 1.1, math.pi * 1.9, eye_col, er * 0.55)
            continue
        ctx.new_path()
        ctx.save()
        ctx.translate(x + sx * ex, y)
        ctx.scale(1, 1.18)
        ctx.arc(0, 0, er, 0, TAU)
        ctx.restore()
        ctx.set_source_rgb(*c(eye_col))
        ctx.fill()
        ctx.arc(x + sx * ex - er * 0.32, y - er * 0.42, er * 0.42, 0, TAU)
        ctx.set_source_rgb(1, 1, 1)
        ctx.fill()
        ctx.arc(x + sx * ex + er * 0.35, y + er * 0.4, er * 0.18, 0, TAU)
        ctx.fill()
    if blush:
        for sx in (-1, 1):
            ctx.save()
            ctx.translate(x + sx * ex * 1.55, y + er * 1.5)
            ctx.scale(er * 1.25, er * 0.7)
            ctx.arc(0, 0, 1, 0, TAU)
            ctx.restore()
            ctx.set_source_rgba(1, 0.42, 0.6, 0.55)
            ctx.fill()
    my = y + er * 1.45
    if mood in ("happy", "closed"):
        arc_line(ctx, x, my - er * 0.45, er * 0.6, math.pi * 0.15, math.pi * 0.85, eye_col, er * 0.42)
    elif mood == "open":
        ctx.new_path()
        ctx.arc(x, my - er * 0.2, er * 0.55, 0, math.pi)
        ctx.close_path()
        ctx.set_source_rgb(*c(eye_col))
        ctx.fill()
        ctx.arc(x, my + er * 0.12, er * 0.25, 0, math.pi)
        ctx.set_source_rgb(1, 0.45, 0.55)
        ctx.fill()
    elif mood == "o":
        ctx.new_path()
        ctx.save()
        ctx.translate(x, my)
        ctx.scale(er * 0.42, er * 0.5)
        ctx.arc(0, 0, 1, 0, TAU)
        ctx.restore()
        ctx.set_source_rgb(*c(eye_col))
        ctx.fill()


def text(ctx, s, x, y, size, col, outline=INK, ow=0.0, weight=cairo.FONT_WEIGHT_BOLD, family="DejaVu Sans",
         anchor="middle", grad=None):
    ctx.select_font_face(family, cairo.FONT_SLANT_NORMAL, weight)
    ctx.set_font_size(size)
    xb, yb, w, h, xa, ya = ctx.text_extents(s)
    tx = x - (xb + w / 2) if anchor == "middle" else x
    ctx.new_path()
    ctx.move_to(tx, y)
    ctx.text_path(s)
    if ow > 0:
        ctx.set_source_rgb(*c(outline))
        ctx.set_line_width(ow * 2)
        ctx.stroke_preserve()
    ctx.set_source(grad) if grad is not None else ctx.set_source_rgb(*c(col))
    ctx.fill()
