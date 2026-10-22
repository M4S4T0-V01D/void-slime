package dev.m4s4t0.voidslime.gl

/** The scene's GLSL (ES 1.00, so it runs on every GPU). */
object Shaders {
    private const val PRECISION = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
"""

    private const val NOISE = """
float hash(vec3 p) { p = fract(p * 0.3183099 + 0.1); p *= 17.0; return fract(p.x * p.y * p.z * (p.x + p.y + p.z)); }
float noise(vec3 x) {
    vec3 i = floor(x); vec3 f = fract(x); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1.0, 0.0, 0.0)), f.x), mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x), mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0)), f.x), f.y), f.z);
}
vec3 hue(float h) { return clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0); }
"""

    /** A studio around the dish: theme-coloured sky and floor, a big softbox up left and a strip light right. */
    private const val ENV = """
uniform vec3 uEnvTop;
uniform vec3 uEnvBot;
uniform float uRoom;
vec3 env(vec3 r) {
    vec3 c = mix(uEnvBot, uEnvTop, smoothstep(-0.3, 0.8, r.y));
    float box1 = smoothstep(0.78, 0.9, dot(r, normalize(vec3(-0.5, 0.8, 0.45))));
    float box2 = smoothstep(0.93, 0.97, dot(r, normalize(vec3(0.75, 0.45, -0.2))));
    return c * (0.35 + 0.65 * uRoom) + vec3(box1 * 1.5 + box2 * 0.9) * uRoom;
}
"""

    const val SLIME_VS = """
uniform mat4 uVP;
attribute vec3 aPos;
attribute vec3 aNor;
attribute vec3 aPat;
varying vec3 vPos;
varying vec3 vNor;
varying vec3 vPat;
void main() {
    vPos = aPos; vNor = aNor; vPat = aPat;
    gl_Position = uVP * vec4(aPos, 1.0);
}
"""

    const val SLIME_FS = PRECISION + NOISE + ENV + """
varying vec3 vPos;
varying vec3 vNor;
varying vec3 vPat;
uniform vec3 uEye;
uniform vec3 uLight;
uniform vec3 uC0;
uniform vec3 uC1;
uniform vec3 uC2;
uniform float uCount;
uniform float uMix;
uniform float uAlpha;
uniform float uGloss;
uniform float uFluff;
uniform float uMetal;
uniform float uHolo;
uniform float uGlow;
uniform float uFrost;
uniform float uGalaxy;
uniform float uThermo;
uniform float uHeat;
uniform vec4 uGlitter;
uniform float uTime;
uniform float uBack;
// Air blisters blown into the skin: centre (pattern space) and how blown up (0..MAX). Their skin goes thin.
uniform vec4 uAir[3];
void main() {
    vec3 n = normalize(vNor) * (uBack > 0.5 ? -1.0 : 1.0);
    vec3 v = normalize(uEye - vPos);
    vec3 p = vPat;
    float thin = 0.0;
    for (int i = 0; i < 3; i++) {
        float d = length(p - uAir[i].xyz) / 0.62;
        thin = max(thin, smoothstep(1.0, 0.25, d) * smoothstep(0.05, 0.6, uAir[i].w));
    }
    if (uFluff > 0.0) {
        vec3 q = p * 22.0;
        n = normalize(n + (vec3(noise(q), noise(q + 5.1), noise(q + 9.7)) - 0.5) * 0.6 * uFluff);
    }
    if (uFrost > 0.0) {
        vec3 q = p * 46.0;
        n = normalize(n + (vec3(noise(q), noise(q + 3.3), noise(q + 7.7)) - 0.5) * 0.4 * uFrost);
    }

    // Swirled dye: domain-warped noise in the slime's own coordinates, so the swirls stretch and squash with it.
    vec3 w = p * 1.7 + vec3(noise(p * 2.3 + 1.7), noise(p * 2.3 + 8.1), noise(p * 2.3 + 4.4)) * 2.2;
    float s1 = noise(w * 1.3);
    float s2 = noise(w * 1.1 + 13.0);
    float sharp = mix(14.0, 1.0, uMix);
    vec3 base = uC0;
    if (uCount > 1.5) base = mix(base, uC1, clamp((s1 - 0.5) * sharp + 0.5, 0.0, 1.0));
    if (uCount > 2.5) base = mix(base, uC2, clamp((s2 - 0.55) * sharp + 0.5, 0.0, 1.0));
    vec3 avg = uCount > 2.5 ? (uC0 + uC1 + uC2) / 3.0 : (uCount > 1.5 ? (uC0 + uC1) * 0.5 : uC0);
    base = mix(base, avg, uMix * uMix * 0.9);
    if (uThermo > 0.0) {
        // Warms in patches as it's played with: the dye turns to its "hot" colour, then cools back.
        vec3 warm = mix(base.gbr, vec3(1.0, 0.45, 0.7), 0.3);
        float t = clamp((uHeat * 1.4 - noise(p * 2.6 + 7.0)) * 3.0, 0.0, 1.0);
        base = mix(base, warm, t * uThermo);
    }
    if (uGalaxy > 0.0) {
        float neb = smoothstep(0.35, 0.85, noise(w * 0.9 + 3.0));
        vec3 cloud = mix(base, hue(fract(noise(p * 3.0) * 0.6 + 0.7)), 0.35);
        base = mix(base, base * 0.16 + vec3(0.02, 0.0, 0.05) + cloud * neb * 0.65, uGalaxy);
    }

    float ndl = dot(n, uLight);
    float diff = clamp((ndl + 0.35) / 1.35, 0.0, 1.0);
    float ndv = max(dot(n, v), 0.0);
    float fres = 0.04 + 0.96 * pow(1.0 - ndv, 5.0);
    vec3 h = normalize(uLight + v);
    float nh = max(dot(n, h), 0.0);
    float spec = pow(nh, mix(10.0, 260.0, uGloss)) * mix(0.12, 2.6, uGloss) + pow(nh, 22.0) * 0.12 * uGloss;
    vec3 e = env(reflect(-v, n));
    vec3 amb = mix(uEnvBot, uEnvTop, n.y * 0.5 + 0.5);
    // Light leaking through thin edges, richer than the surface colour.
    vec3 sss = base * base * 1.3 * pow(1.0 - ndv, 1.5) * 0.6;
    vec3 col = base * (0.5 * amb + 0.78 * diff * uRoom) + sss * uRoom;
    if (uFluff > 0.0) col += vec3(0.06) * uFluff * noise(p * 70.0) * uRoom;
    col += e * fres * uGloss * 0.9;
    col += vec3(spec) * uRoom;
    if (uMetal > 0.0) {
        vec3 m = base * (e * 1.4 + 0.06) + vec3(spec) * mix(base, vec3(1.0), 0.5) * 1.5 + fres * e * 0.6;
        col = mix(col, m, uMetal);
    }
    if (uHolo > 0.0) {
        vec3 rainbow = hue(fract(ndv * 1.6 + p.y * 0.7 + p.x * 0.4 + uTime * 0.04));
        col = mix(col, col * 0.6 + rainbow * 0.65, (0.25 + 0.6 * fres) * uHolo * uRoom);
    }
    if (uGlitter.a > 0.0) {
        vec3 gq = p * 58.0;
        vec3 id = floor(gq);
        float hr = hash(id);
        if (hr < uGlitter.a * 0.45) {
            float d = length(fract(gq) - 0.5);
            vec3 rn = normalize(n + (vec3(hash(id + 1.3), hash(id + 2.7), hash(id + 5.1)) - 0.5) * 1.6);
            float tw = pow(max(dot(reflect(-uLight, rn), v), 0.0), 18.0) * (0.6 + 0.4 * sin(uTime * 4.0 + hr * 80.0));
            vec3 gc = uGlitter.r < 0.0 ? hue(hash(id + 9.1)) : uGlitter.rgb;
            col += gc * smoothstep(0.38, 0.05, d) * (tw * 5.0 + 0.25) * max(uRoom, 0.35);
        }
    }
    if (uGalaxy > 0.0) {
        vec3 sq = p * 34.0;
        vec3 sid = floor(sq);
        float hs = hash(sid + 4.2);
        if (hs > 0.9) {
            float tw = 0.55 + 0.45 * sin(uTime * 3.0 + hs * 90.0);
            col += vec3(1.0, 0.95, 0.9) * smoothstep(0.24, 0.0, length(fract(sq) - 0.5)) * tw * 1.5 * uGalaxy;
        }
    }
    float alpha = 1.0;
    if (uAlpha < 0.999) alpha = clamp(mix(uAlpha, 1.0, fres) + spec * 0.5, 0.0, 1.0);
    if (thin > 0.0) {
        // A stretched, see-through film with a soap-bubble shimmer.
        vec3 film = hue(fract(ndv * 1.3 + p.y * 0.5 + uTime * 0.05)) * 0.5 + base * 0.5;
        col = mix(col, col * 0.55 + film * (0.3 + fres) * max(uRoom, 0.4) + vec3(spec), thin * 0.8);
        alpha = mix(alpha, clamp(0.35 + fres * 0.8 + spec, 0.0, 1.0), thin);
    }
    if (uFrost > 0.0) col = mix(col, col * 0.8 + vec3(0.24, 0.3, 0.36) * uRoom, 0.35 * uFrost);
    if (uGlow > 0.0) {
        float pulse = 0.78 + 0.22 * sin(uTime * 1.7);
        vec3 em = base * 1.3 * pulse * uGlow;
        col = col * mix(1.0, 0.25 + 0.75 * uRoom, uGlow) + em * (1.15 - uRoom * 0.8);
        alpha = mix(alpha, 0.94, (1.0 - uRoom) * uGlow);
    }
    gl_FragColor = vec4(col, alpha);
}
"""

    const val SOLID_VS = """
uniform mat4 uVP;
uniform mat4 uModel;
attribute vec3 aPos;
attribute vec3 aNor;
varying vec3 vW;
varying vec3 vN;
void main() {
    vec4 w = uModel * vec4(aPos, 1.0);
    vW = w.xyz;
    vN = (uModel * vec4(aNor, 0.0)).xyz;
    gl_Position = uVP * w;
}
"""

    const val SOLID_FS = PRECISION + ENV + """
varying vec3 vW;
varying vec3 vN;
uniform vec3 uEye;
uniform vec3 uLight;
uniform vec4 uColor;
uniform float uGloss;
uniform vec3 uEmit;
// 1 for the glass jars: mostly see-through, with bright edges and reflections.
uniform float uGlass;
void main() {
    vec3 n = normalize(vN);
    vec3 v = normalize(uEye - vW);
    if (uGlass > 0.5 && dot(n, v) < 0.0) n = -n;
    float diff = clamp((dot(n, uLight) + 0.25) / 1.25, 0.0, 1.0);
    float ndv = max(dot(n, v), 0.0);
    float fres = 0.04 + 0.96 * pow(1.0 - ndv, 5.0);
    // Tight enough that a flat dish floor shows a sheen, not a blown-out patch, when the camera lines up with the light.
    float spec = pow(max(dot(n, normalize(uLight + v)), 0.0), mix(60.0, 220.0, uGloss)) * uGloss * 0.9;
    vec3 amb = mix(uEnvBot, uEnvTop, n.y * 0.5 + 0.5);
    vec3 col = uColor.rgb * (0.45 * amb + 0.7 * diff * uRoom) + env(reflect(-v, n)) * fres * uGloss * 0.8 + vec3(spec) * uRoom;
    // Neon tray: a glowing rim.
    col += uEmit * smoothstep(0.1, 0.2, vW.y);
    float alpha = uColor.a;
    if (uGlass > 0.5) {
        col = uColor.rgb * 0.25 * uRoom + env(reflect(-v, n)) * (0.15 + fres * 1.6) + vec3(spec) * 2.0 * uRoom;
        alpha = clamp(0.1 + fres * 0.85 + spec, 0.0, 1.0);
    }
    gl_FragColor = vec4(col, alpha);
}
"""

    const val SPRITE_VS = """
uniform mat4 uVP;
uniform float uScale;
attribute vec3 aPos;
attribute vec4 aCol;
attribute vec4 aMeta;
varying vec4 vCol;
varying float vShape;
varying float vAngle;
varying float vPx;
varying float vLit;
void main() {
    gl_Position = uVP * vec4(aPos, 1.0);
    float px = aMeta.x * uScale / gl_Position.w;
    gl_PointSize = px;
    vPx = max(px, 1.0);
    vCol = aCol; vShape = aMeta.y; vAngle = aMeta.z; vLit = aMeta.w;
}
"""

    const val SPRITE_FS = PRECISION + """
varying vec4 vCol;
varying float vShape;
varying float vAngle;
varying float vPx;
varying float vLit;
float sdStar(vec2 p, float r, float rf) {
    vec2 k1 = vec2(0.809016994, -0.587785252);
    vec2 k2 = vec2(-k1.x, k1.y);
    p.x = abs(p.x);
    p -= 2.0 * max(dot(k1, p), 0.0) * k1;
    p -= 2.0 * max(dot(k2, p), 0.0) * k2;
    p.x = abs(p.x);
    p.y -= r;
    vec2 ba = rf * vec2(-k1.y, k1.x) - vec2(0.0, 1.0);
    float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, r);
    return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y);
}
float sdSeg(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a; vec2 ba = b - a;
    return length(pa - ba * clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0));
}
float sdHeart(vec2 p) {
    p.x = abs(p.x);
    if (p.y + p.x > 1.0) return length(p - vec2(0.25, 0.75)) - 0.35355;
    vec2 q = p - 0.5 * max(p.x + p.y, 0.0);
    return sqrt(min(dot(p - vec2(0.0, 1.0), p - vec2(0.0, 1.0)), dot(q, q))) * sign(p.x - p.y);
}
void main() {
    vec2 p = gl_PointCoord * 2.0 - 1.0;
    p.y = -p.y;
    float ca = cos(vAngle); float sa = sin(vAngle);
    vec2 rp = vec2(ca * p.x - sa * p.y, sa * p.x + ca * p.y);
    float aa = 2.5 / vPx;
    vec3 col = vCol.rgb;
    float d;
    float a = 1.0;
    vec3 L = normalize(vec3(-0.4, 0.6, 0.7));
    if (vShape < 0.5) {            // bead
        d = length(p) - 0.9;
        vec2 q = p / 0.9;
        vec3 nrm = vec3(q, sqrt(max(0.0, 1.0 - dot(q, q))));
        col = col * (0.5 + 0.6 * clamp(dot(nrm, L), 0.0, 1.0)) + vec3(pow(max(dot(reflect(-L, nrm), vec3(0.0, 0.0, 1.0)), 0.0), 24.0)) * 0.9;
    } else if (vShape < 1.5) {     // star
        d = sdStar(rp, 0.92, 0.42);
        col = col * (0.85 + 0.35 * (1.0 - length(p))) + vec3(0.25) * smoothstep(0.3, 0.0, length(rp - vec2(-0.15, 0.2)));
    } else if (vShape < 2.5) {     // heart
        vec2 q = rp * 1.25 + vec2(0.0, 0.6);
        d = sdHeart(q) / 1.25;
        col = col * (0.8 + 0.4 * (1.0 - length(p))) + vec3(0.35) * smoothstep(0.25, 0.0, length(rp - vec2(-0.3, 0.25)));
    } else if (vShape < 3.5) {     // sprinkle
        vec2 q = rp; q.x = max(abs(q.x) - 0.55, 0.0);
        d = length(q) - 0.26;
        col = col * (0.75 + 0.5 * clamp(0.5 + rp.y * 2.0, 0.0, 1.0));
    } else if (vShape < 4.5) {     // fruit slice
        float r = length(p);
        d = r - 0.9;
        float ang = atan(rp.y, rp.x);
        float seg = abs(fract(ang / 6.2831853 * 8.0) - 0.5);
        vec3 flesh = mix(col, vec3(1.0), 0.25);
        col = r > 0.74 ? col * 0.75 : (r > 0.66 || seg > 0.46 || r < 0.1 ? vec3(1.0, 0.97, 0.9) : flesh);
    } else if (vShape < 5.5) {     // snowflake
        float ang = atan(rp.y, rp.x);
        float sector = floor(ang / 1.0471976 + 0.5) * 1.0471976;
        vec2 q = vec2(cos(-sector) * rp.x - sin(-sector) * rp.y, sin(-sector) * rp.x + cos(-sector) * rp.y);
        float arm = length(vec2(max(q.x - 0.85, 0.0) + max(-q.x, 0.0), q.y)) - 0.1;
        vec2 b = vec2(q.x - 0.55, abs(q.y));
        float twig = length(b - vec2(0.15, 0.15) * clamp(dot(b, vec2(0.15, 0.15)) / 0.045, 0.0, 1.0)) - 0.07;
        d = min(arm, twig);
    } else if (vShape < 6.5) {     // crystal
        d = (abs(rp.x) * 1.15 + abs(rp.y)) * 0.75 - 0.68;
        float facet = rp.x > 0.0 ? (rp.y > 0.0 ? 1.0 : 0.7) : (rp.y > 0.0 ? 0.85 : 0.55);
        col = col * facet + vec3(0.3) * smoothstep(0.2, 0.0, abs(rp.x + rp.y * 0.3));
        a = 0.85;
    } else if (vShape < 7.5) {     // fishbowl bead: clear glass
        float r = length(p);
        d = r - 0.9;
        a = 0.3 + 0.6 * smoothstep(0.55, 0.88, r);
        col = mix(col, vec3(1.0), 0.35) + vec3(1.0) * smoothstep(0.22, 0.0, length(p - vec2(-0.32, 0.36))) * 1.2;
    } else if (vShape < 8.5) {     // confetti disc
        vec2 q = rp * vec2(1.0, 2.0);
        d = (length(q) - 0.85) / 2.0;
        col = col * (0.8 + 0.3 * rp.y);
    } else if (vShape < 9.5) {     // spark (particles)
        float r = length(p);
        d = r - 0.95;
        a = exp(-r * r * 4.0);
        col = col + vec3(0.4) * exp(-r * r * 16.0);
    } else if (vShape < 10.5) {    // flower: five round petals and a yellow middle
        float r = length(rp);
        float ang = atan(rp.y, rp.x);
        d = r - (0.62 + 0.3 * abs(cos(ang * 2.5)));
        col = r < 0.26 ? vec3(1.0, 0.82, 0.25) : col * (0.8 + 0.3 * r);
    } else if (vShape < 11.5) {    // crescent moon
        d = max(length(rp) - 0.85, -(length(rp - vec2(0.38, 0.22)) - 0.68));
        col = col * (0.85 + 0.3 * (1.0 - length(p)));
    } else if (vShape < 12.5) {    // face: an eye, closing as vAngle goes to 1 (a blink, a happy squint)
        float open = max(0.1, 1.0 - vAngle);
        vec2 q = vec2(p.x, p.y / open);
        d = (length(q) - 0.86) * open;
        col = vec3(0.1, 0.06, 0.18) + vec3(1.0) * smoothstep(0.3, 0.2, length(q - vec2(-0.28, 0.32))) * step(0.4, open)
            + vec3(0.9) * smoothstep(0.15, 0.08, length(q - vec2(0.26, -0.24))) * step(0.4, open);
    } else if (vShape < 13.5) {    // face: a soft rosy cheek
        vec2 q = p * vec2(1.0, 1.7);
        d = length(q) / 1.7 - 0.5;
        a = 0.6 * smoothstep(0.9, 0.2, length(q));
    } else if (vShape < 14.5) {    // face: a little smile
        float r = length(p - vec2(0.0, 0.5));
        d = max(abs(r - 0.78) - 0.13, p.y - 0.12);
        col = vec3(0.1, 0.06, 0.18);
    } else if (vShape < 45.0) {    // mood eyes: 20 + variant * 2 + side (0 the left eye); vAngle is the blink
        float id = floor(vShape - 20.0 + 0.5);
        float v = floor(id * 0.5 + 0.01);
        vec2 u = p * 1.5;
        vec2 q = vec2(id - v * 2.0 < 0.5 ? u.x : -u.x, u.y);   // +x points at the nose
        vec3 ink = vec3(0.1, 0.06, 0.18);
        float open = max(0.1, 1.0 - vAngle);
        vec3 shine = vec3(1.0) * smoothstep(0.3, 0.2, length(u - vec2(-0.28, 0.32))) + vec3(0.9) * smoothstep(0.15, 0.08, length(u - vec2(0.26, -0.24)));
        col = ink;
        if (v < 0.5) {             // round
            vec2 e = vec2(u.x, u.y / open);
            d = (length(e) - 0.86) * open;
            col = ink + shine * step(0.4, open);
        } else if (v < 1.5) {      // ^ squeezed shut
            d = max(abs(length(q - vec2(0.0, -0.45)) - 0.6) - 0.17, -0.3 - q.y);
        } else if (v < 2.5) {      // angry: a lid slanting down to the nose, and a cross brow
            vec2 e = vec2(q.x, (q.y + 0.15) / open);
            float eye = max((length(e) - 0.7) * open, dot(q - vec2(0.0, 0.0), normalize(vec2(0.5, 1.0))));
            d = min(eye, sdSeg(q, vec2(-0.8, 0.8), vec2(0.65, 0.3)) - 0.15);
            col = ink + shine * step(eye, 0.0) * step(0.4, open) * 0.8;
        } else if (v < 3.5) {      // sad: big wet eye, brows up at the middle
            vec2 e = vec2(q.x, (q.y + 0.15) / open);
            float eye = (length(e) - 0.72) * open;
            d = min(eye, sdSeg(q, vec2(-0.75, 0.62), vec2(0.6, 0.98)) - 0.12);
            col = mix(ink, vec3(0.35, 0.6, 1.0), smoothstep(-0.3, -0.75, q.y) * step(eye, 0.0)) + shine * step(eye, 0.0) * step(0.4, open);
        } else if (v < 4.5) {      // heart
            d = sdHeart(u * 1.0 + vec2(0.0, 0.55));
            col = vec3(1.0, 0.25, 0.42) + vec3(0.7) * smoothstep(0.22, 0.1, length(u - vec2(-0.42, 0.25)));
        } else if (v < 5.5) {      // wide: white with a tiny pupil
            float r = length(vec2(u.x, u.y / open));
            d = (r - 0.95) * open;
            col = r > 0.78 ? ink : (length(u - vec2(0.0, -0.05)) < 0.3 ? ink : vec3(1.0));
        } else if (v < 6.5) {      // droopy closed
            d = max(abs(length(q - vec2(0.0, 0.5)) - 0.65) - 0.15, q.y + 0.05);
        } else if (v < 7.5) {      // swirl
            float r = length(u);
            float t = r * 2.4 - atan(u.y, u.x) / 6.2831853 + vAngle;   // vAngle spins it
            d = max((abs(fract(t) - 0.5) - 0.2) / 2.4, r - 0.9);
        } else if (v < 8.5) {      // shades
            vec2 b = abs(q - vec2(0.1, 0.05)) - vec2(0.75, 0.45);
            float lens = length(max(b, 0.0)) + min(max(b.x, b.y), 0.0) - 0.3;
            d = min(lens, sdSeg(q, vec2(0.8, 0.35), vec2(1.6, 0.35)) - 0.1);
            col = ink + vec3(0.55, 0.45, 0.75) * step(abs(u.x + u.y * 0.8 + 0.2), 0.13) * step(lens, 0.0);
        } else if (v < 9.5) {      // star
            d = sdStar(u, 0.98, 0.48);
            col = vec3(1.0, 0.84, 0.2) + vec3(0.5) * smoothstep(0.25, 0.1, length(u - vec2(-0.2, 0.25)));
        } else {                   // half-closed, smug
            vec2 e = vec2(q.x, (q.y + 0.1) / open);
            float eye = max((length(e) - 0.75) * open, q.y + 0.05 - q.x * 0.12);
            d = min(eye, sdSeg(q, vec2(-0.75, 0.05), vec2(0.7, 0.13)) - 0.11);
            col = ink + vec3(0.9) * smoothstep(0.15, 0.08, length(u - vec2(-0.25, -0.3))) * step(eye, 0.0);
        }
    } else if (vShape < 65.0) {    // mood mouths: 50 + variant
        float v = floor(vShape - 50.0 + 0.5);
        vec2 q = p * 1.3;
        vec3 ink = vec3(0.1, 0.06, 0.18);
        vec3 pink = vec3(1.0, 0.42, 0.55);
        col = ink;
        if (v < 0.5) {             // smile
            d = max(abs(length(q - vec2(0.0, 0.5)) - 0.78) - 0.14, q.y - 0.12);
        } else if (v < 1.5) {      // open laugh
            d = max(length(q - vec2(0.0, 0.25)) - 0.75, q.y - 0.25);
            col = length(q - vec2(0.0, -0.5)) < 0.33 ? pink : ink;
        } else if (v < 2.5) {      // frown
            d = max(abs(length(q - vec2(0.0, -0.75)) - 0.62) - 0.14, -0.4 - q.y);
        } else if (v < 3.5) {      // o
            float r = length(q * vec2(1.3, 1.0));
            d = (r - 0.5) / 1.3;
            col = r < 0.3 ? pink * 0.6 : ink;
        } else if (v < 4.5) {      // tongue out
            float smile = max(abs(length(q - vec2(0.0, 0.5)) - 0.78) - 0.14, q.y - 0.12);
            float tongue = max(length(q - vec2(0.2, -0.38)) - 0.3, q.y + 0.2);
            d = min(smile, tongue);
            col = tongue < smile ? pink : ink;
        } else if (v < 5.5) {      // wavy
            d = max(abs(q.y + 0.05 - 0.13 * sin(q.x * 7.0)) - 0.11, abs(q.x) - 0.65);
        } else if (v < 6.5) {      // smirk
            d = max(abs(length(q - vec2(-0.35, 0.55)) - 0.75) - 0.13, max(q.y - 0.05, -0.75 - q.x));
        } else if (v < 7.5) {      // gritted teeth
            vec2 b = abs(q - vec2(0.0, -0.1)) - vec2(0.62, 0.28);
            float box = length(max(b, 0.0)) + min(max(b.x, b.y), 0.0) - 0.1;
            d = box;
            col = (box > -0.11 || abs(q.y + 0.1) < 0.05 || abs(fract(q.x * 2.2 + 0.5) - 0.5) < 0.07) ? ink : vec3(1.0);
        } else {                   // cat :3
            vec2 c = vec2(abs(q.x) - 0.3, q.y - 0.15);
            d = max(abs(length(c) - 0.3) - 0.12, q.y - 0.15);
        }
    } else {                       // mood cheeks: 70 + variant
        float v = floor(vShape - 70.0 + 0.5);
        if (v < 0.5) {             // blush
            vec2 q = p * vec2(1.0, 1.7);
            d = length(q) / 1.7 - 0.5;
            a = 0.6 * smoothstep(0.9, 0.2, length(q));
        } else if (v < 1.5) {      // a tear / a bead of sweat
            vec2 q = p * 1.1;
            float drop = min(length(q - vec2(0.0, -0.25)) - 0.48, max(dot(vec2(abs(q.x), q.y - 0.85), normalize(vec2(0.9, 0.4))), -0.25 - q.y));
            d = drop / 1.1;
            col = vec3(0.45, 0.78, 1.0) + vec3(0.6) * smoothstep(0.18, 0.06, length(q - vec2(-0.16, -0.12)));
            a = 0.92;
        } else if (v < 2.5) {      // a cross mark
            vec2 q = abs(p);
            d = max(max(abs(length(q - vec2(0.85)) - 0.52) - 0.14, max(q.x, q.y) - 0.85), 0.14 - min(q.x, q.y));
            col = vec3(1.0, 0.22, 0.3);
        } else if (v < 3.5) {      // little heart
            d = sdHeart(p * 1.2 + vec2(0.0, 0.6)) / 1.2;
            col = vec3(1.0, 0.4, 0.6);
        } else {                   // Z
            d = min(min(sdSeg(p, vec2(-0.5, 0.55), vec2(0.5, 0.55)), sdSeg(p, vec2(0.5, 0.55), vec2(-0.5, -0.55))), sdSeg(p, vec2(-0.5, -0.55), vec2(0.5, -0.55))) - 0.14;
            col = vec3(0.85, 0.9, 1.0);
        }
    }
    float cover = clamp(0.5 - d / aa, 0.0, 1.0);
    if (cover * a * vCol.a < 0.01) discard;
    gl_FragColor = vec4(col * vLit, cover * a * vCol.a);
}
"""

    const val BUBBLE_VS = """
uniform mat4 uVP;
uniform vec3 uCenter;
uniform vec3 uScale3;
attribute vec3 aPos;
varying vec3 vW;
varying vec3 vN;
void main() {
    vW = uCenter + aPos * uScale3;
    vN = normalize(aPos / uScale3);
    gl_Position = uVP * vec4(vW, 1.0);
}
"""

    const val BUBBLE_FS = PRECISION + """
varying vec3 vW;
varying vec3 vN;
uniform vec3 uEye;
uniform vec3 uLight;
uniform float uTime;
uniform float uSeed;
uniform float uFade;
uniform float uGold;
uniform float uRoom;
vec3 hue(float h) { return clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0); }
void main() {
    vec3 n = normalize(vN);
    vec3 v = normalize(uEye - vW);
    float ndv = abs(dot(n, v));
    float fres = pow(1.0 - ndv, 2.5);
    vec3 film = hue(fract(ndv * 1.8 + n.y * 0.35 + uTime * 0.08 + uSeed));
    film = mix(film, vec3(1.0, 0.8, 0.3), uGold * 0.75);
    float spec = pow(max(dot(reflect(-v, n), uLight), 0.0), 90.0);
    float win = smoothstep(0.85, 0.95, dot(reflect(-v, n), normalize(vec3(-0.5, 0.8, 0.45))));
    vec3 col = film * (0.3 + fres) * (0.4 + 0.6 * uRoom) + vec3(spec * 1.6 + win * 0.9) * uRoom;
    float alpha = clamp(0.05 + fres * 0.8 + spec + win * 0.5 + uGold * 0.15, 0.0, 1.0) * uFade;
    gl_FragColor = vec4(col, alpha);
}
"""

    const val SHADOW_VS = """
uniform mat4 uVP;
uniform vec3 uCenter;
uniform float uRadius;
attribute vec2 aPos;
varying vec2 vUV;
void main() {
    vUV = aPos;
    gl_Position = uVP * vec4(uCenter + vec3(aPos.x, 0.0, aPos.y) * uRadius, 1.0);
}
"""

    const val SHADOW_FS = PRECISION + """
varying vec2 vUV;
uniform vec4 uColor;
void main() {
    float d = length(vUV);
    gl_FragColor = vec4(uColor.rgb, uColor.a * pow(clamp(1.0 - d, 0.0, 1.0), 1.6));
}
"""

    const val BACK_VS = """
attribute vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.999, 1.0); }
"""

    const val BACK_FS = PRECISION + """
uniform vec3 uTop;
uniform vec3 uBot;
uniform vec3 uGlow;
uniform float uRoom;
uniform float uTime;
uniform vec2 uRes;
// A painted world: uTex covers the screen (uScale, uOff in texture space); uFx is its weather (model.Fx), -1 none.
uniform sampler2D uTex;
uniform float uHasTex;
uniform vec2 uScale;
uniform vec2 uOff;
uniform float uFx;
float hash2(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }

// One layer of drifting things in cells of [cell] pixels moving at [vel] px/s; returns coverage and a random value.
vec2 field(vec2 px, float cell, vec2 vel, float density, float size, float sway, float shape) {
    vec2 sp = (px - vel * uTime) / cell;
    vec2 id = floor(sp);
    float h = hash2(id);
    if (h > density) return vec2(0.0);
    float h2 = hash2(id + 17.3);
    vec2 c = vec2(0.3 + 0.4 * h2, 0.3 + 0.4 * fract(h * 7.1));
    c.x += sin(uTime * (0.6 + h2) + h * 40.0) * sway;
    vec2 d = fract(sp) - c;
    float a = 0.0;
    if (shape < 0.5) {
        // Soft round dot (snow, pollen).
        a = smoothstep(size, size * 0.35, length(d));
    } else if (shape < 1.5) {
        // A petal: a turning ellipse.
        float r = uTime * (0.8 + h2) + h * 30.0;
        vec2 q = mat2(cos(r), -sin(r), sin(r), cos(r)) * d;
        a = smoothstep(1.0, 0.6, length(q / vec2(size, size * 0.55)));
    } else if (shape < 2.5) {
        // A bubble: a ring with a shine.
        float l = length(d);
        a = smoothstep(size, size * 0.8, l) * (0.35 + 0.65 * smoothstep(size * 0.6, size * 0.95, l));
        a += smoothstep(size * 0.3, 0.0, length(d - vec2(-size * 0.4, size * 0.4))) * 0.8;
    } else {
        // A twinkling four-point sparkle.
        float tw = 0.5 + 0.5 * sin(uTime * (2.0 + h2 * 3.0) + h * 90.0);
        vec2 ad = abs(d);
        a = (smoothstep(size * 0.18, 0.0, ad.x) * smoothstep(size, 0.0, ad.y) + smoothstep(size * 0.18, 0.0, ad.y) * smoothstep(size, 0.0, ad.x)) * tw;
        a += smoothstep(size * 0.35, 0.0, length(d)) * tw;
    }
    return vec2(clamp(a, 0.0, 1.0), h2);
}

void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    vec2 px = gl_FragCoord.xy;
    float u = uRes.y / 800.0;
    vec3 c;
    if (uHasTex > 0.5) {
        vec2 t = (vec2(uv.x, 1.0 - uv.y) - 0.5) * uScale + 0.5 + uOff;
        c = texture2D(uTex, t).rgb;
    } else {
        c = mix(uBot, uTop, uv.y);
        vec2 g = (uv - vec2(0.5, 0.5)) * vec2(uRes.x / uRes.y, 1.0);
        c += uGlow * exp(-dot(g, g) * 5.0) * 0.28;
    }
    if (uFx < -0.5) {
    } else if (uFx < 0.5) {
        // Twinkling stars.
        vec2 sp = px / 4.0;
        vec2 id = floor(sp);
        float h = hash2(id);
        if (h > 0.996) {
            float tw = 0.5 + 0.5 * sin(uTime * (1.0 + h * 2.0) + h * 500.0);
            c += vec3(0.7) * tw * smoothstep(0.5, 0.0, length(fract(sp) - 0.5)) * (1.2 - uRoom * 0.6);
        }
    } else if (uFx < 1.5) {
        // Snow, near and far.
        vec2 a = field(px, 46.0 * u, vec2(8.0, -38.0) * u, 0.5, 0.11, 0.15, 0.0);
        vec2 b = field(px + 300.0, 80.0 * u, vec2(14.0, -70.0) * u, 0.4, 0.09, 0.12, 0.0);
        c = mix(c, vec3(1.0), max(a.x * 0.7, b.x * 0.95));
    } else if (uFx < 2.5) {
        // Cherry petals.
        vec2 a = field(px, 70.0 * u, vec2(30.0, -40.0) * u, 0.32, 0.16, 0.12, 1.0);
        vec2 b = field(px + 500.0, 110.0 * u, vec2(46.0, -64.0) * u, 0.28, 0.12, 0.14, 1.0);
        c = mix(c, mix(vec3(1.0, 0.72, 0.84), vec3(1.0, 0.86, 0.92), a.y), a.x * 0.85);
        c = mix(c, mix(vec3(1.0, 0.62, 0.78), vec3(1.0, 0.9, 0.95), b.y), b.x * 0.95);
    } else if (uFx < 3.5) {
        // Bubbles rising.
        vec2 a = field(px, 60.0 * u, vec2(0.0, 34.0) * u, 0.3, 0.16, 0.12, 2.0);
        vec2 b = field(px + 200.0, 100.0 * u, vec2(0.0, 58.0) * u, 0.25, 0.14, 0.1, 2.0);
        c = mix(c, vec3(0.85, 0.97, 1.0), max(a.x * 0.45, b.x * 0.6));
    } else if (uFx < 4.5) {
        // Sparkles.
        vec2 a = field(px, 64.0 * u, vec2(0.0, 0.0), 0.22, 0.32, 0.0, 3.0);
        vec2 b = field(px + 77.0, 38.0 * u, vec2(0.0, 0.0), 0.2, 0.26, 0.0, 3.0);
        c += mix(vec3(1.0, 0.95, 0.75), vec3(0.95, 0.85, 1.0), a.y) * a.x * 0.8 + vec3(1.0) * b.x * 0.45;
    } else {
        // Pollen and fluff drifting in the breeze.
        vec2 a = field(px, 56.0 * u, vec2(22.0, 6.0) * u, 0.28, 0.07, 0.14, 0.0);
        vec2 b = field(px + 400.0, 90.0 * u, vec2(36.0, 10.0) * u, 0.2, 0.06, 0.14, 0.0);
        c = mix(c, vec3(1.0, 0.97, 0.75), max(a.x, b.x) * 0.75);
    }
    c *= mix(0.22, 1.0, uRoom);
    gl_FragColor = vec4(c, 1.0);
}
"""
}
