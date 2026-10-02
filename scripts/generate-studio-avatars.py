"""Original, editable 2D artwork. One path source produces SVG and Android vectors.

The neutral face is included in Figma; Android draws the animated expression on
the same empty face so home, carousel and live overlay share the same body art.
No downloaded or reference-image pixels are used.
"""
from pathlib import Path
from xml.sax.saxutils import escape
import json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'design/phase47/avatars'
RES = ROOT / 'android/app/src/main/res/drawable'
OUT.mkdir(parents=True, exist_ok=True)
RES.mkdir(parents=True, exist_ok=True)
CATALOG = [
    ('studio_nova', 'Nova', 'Calm and observant', '#e8dce8', '#986479', '#ffdDCF', True),
    ('kai', 'Kai', 'Focused and adaptive', '#292334', '#9175eb', '#ffd4bc', False),
    ('astra', 'Astra', 'Thoughtful and grounded', '#638e8b', '#68bcae', '#ffddc9', False),
    ('mira', 'Mira', 'Analytical and intuitive', '#30232f', '#d97390', '#ffd6c7', True),
    ('volt', 'Volt', 'Energetic and expressive', '#4c80c9', '#71b5ea', '#ffdac2', False),
]

def ellipse(x, y, rx, ry):
    return f'M{x-rx},{y} a{rx},{ry} 0 1,0 {rx*2},0 a{rx},{ry} 0 1,0 {-rx*2},0'

def make(ident, name, subtitle, hair, accent, skin, long):
    shapes = []
    def p(d, fill, stroke=None, width=1):
        shapes.append((d, fill, stroke, width))
    # Hair silhouette; layered locks remain vector paths at all display sizes.
    if long:
        p('M29,43 C17,10 46,2 66,7 C95,2 110,28 105,58 L110,124 Q99,143 83,119 L43,119 Q19,141 19,115 Z', hair, '#201d2a', 2)
        p('M27,58 Q18,101 26,122 L37,116 L38,61 Z', '#000000', None)
        p('M93,47 Q111,82 101,128 L89,116 Z', '#000000', None)
    else:
        p('M30,44 L21,34 L29,29 L24,20 L40,22 L42,10 L52,15 Q61,3 75,12 L88,9 L87,19 L102,23 L95,31 L105,40 L96,49 L32,54 Z', hair, '#201d2a', 2)
    # Feet, tailored trousers, jacket and collar.
    p('M43,118 L61,118 L60,149 L41,149 Z M66,118 L85,118 L88,149 L68,149 Z', '#262633', '#191924', 1)
    p('M41,141 Q48,146 59,143 L61,154 Q47,159 36,153 L37,146 Z M69,143 Q78,146 86,142 L94,151 Q88,159 70,155 Z', '#323340', '#191924', 1)
    p('M37,151 L59,152 L59,155 L37,155 Z M70,153 L91,151 L92,154 L71,157 Z', '#d9d8e4')
    p('M37,88 Q45,77 55,77 L75,77 Q84,78 93,89 L91,124 Q63,138 35,124 Z', '#2b2b3b', '#1b1b27', 1.5)
    p('M55,76 L74,76 L75,87 Q64,96 54,87 Z', skin)
    p('M55,85 L64,91 L74,85 L81,93 L73,104 L55,102 L47,91 Z', '#171822')
    p('M36,88 L52,87 L58,126 L35,124 Z M76,87 L93,89 L92,124 L72,128 Z', accent, '#252334', 1)
    p('M40,94 L48,93 L51,122 L40,121 Z M81,93 L89,96 L87,121 L77,125 Z', '#343443')
    p('M39,85 Q32,84 27,97 L23,120 Q23,130 33,129 L41,112 Z M91,85 Q99,84 103,99 L107,119 Q109,129 99,130 L89,111 Z', '#2c2c3b', '#20202c', 1.5)
    p('M25,121 Q22,130 30,133 Q36,132 36,124 Z M99,123 Q96,132 102,134 Q110,133 107,124 Z', skin, '#b78476', .6)
    p('M28,100 L34,89 L38,92 L32,114 Z M96,91 L100,98 L102,114 L96,106 Z', accent)
    p('M57,101 L58,126 L61,126 L60,101 Z', '#bdb5d2')
    p(ellipse(60,109,1,1), '#edebf4')
    # Face and ears, softer secondary tones, no gradients needed on tiny overlays.
    p(ellipse(32,55,7,10), skin, '#ba897b', .7)
    p(ellipse(95,55,7,10), skin, '#ba897b', .7)
    p('M34,34 Q61,19 91,34 L94,52 Q95,73 79,82 Q64,91 49,81 Q33,71 32,53 Z', skin, '#bf9083', .8)
    p('M35,52 Q38,76 55,81 Q45,69 46,54 Z', '#eeb4a2')
    p(ellipse(43,65,5,2), '#f2ada8')
    p(ellipse(85,65,5,2), '#f2ada8')
    p('M63,57 L61,64 L65,64', skin, '#ce9384', .8)
    # Neutral expression is placed behind bangs. Runtime substitutes it for all 8 emotions.
    face = []
    def f(d, fill, stroke=None, width=1): face.append((d, fill, stroke, width))
    for x in (49,78):
        f(ellipse(x,55,7,6), '#fff8f3', '#352c3b', 1.1)
        f(ellipse(x,55,3.9,5.5), accent)
        f(ellipse(x,55.5,2.4,4.8), '#252434')
        f(ellipse(x-1.4,53,1.3,1.4), '#ffffff')
    f('M42,44 Q49,41 56,44 M71,44 Q78,41 85,44', 'none', '#433443', 1.8)
    f('M58,72 Q64,75 70,72', 'none', '#a4636d', 1.2)
    # Distinct hairstyles and accessories, with restrained reflected light.
    if long:
        p('M29,40 Q23,14 48,12 Q68,3 88,16 Q103,24 100,49 L91,63 L88,32 Q77,21 70,24 Q65,44 53,43 L56,29 Q46,48 32,52 Z', hair, '#201d2a', 1.5)
        p('M35,39 Q35,20 52,17 Q46,29 39,39 Z M64,17 Q86,14 91,34 Q82,22 72,23 Z', accent)
        p('M94,54 Q94,101 98,116 L91,109 L88,57 Z M29,59 Q32,105 26,117 L36,107 L39,62 Z', hair)
        p('M92,33 L97,38 L93,43 L89,38 Z', accent, '#eee2ef', .7)
    else:
        p('M28,40 L29,25 L37,24 L35,18 L47,22 L53,13 Q61,23 70,16 L83,15 L87,24 L97,26 L93,39 L87,50 L81,31 Q72,45 58,40 L63,28 Q50,44 39,43 L38,53 Z', hair, '#201d2a', 1.5)
        p('M37,28 L49,25 L47,31 L34,37 Z M67,22 L80,21 L81,26 L67,33 Z', accent)
        p('M90,38 L96,33 L94,51 L89,57 Z', hair)
    if ident in ('kai','volt'):
        p('M40,81 Q45,104 64,103 Q85,103 88,82 L84,81 Q78,96 65,96 Q50,95 45,80 Z', '#151621', '#444352', 1)
        p('M38,81 Q31,85 36,97 L43,95 L43,83 Z M88,82 L94,86 L91,98 L84,95 Z', '#262634', accent, 2)
        p(ellipse(39,89,3,4), accent)
        p(ellipse(89,90,3,4), accent)
    else:
        p('M64,99 L68,106 L64,113 L60,106 Z', accent, '#eee6e8', 1)
    # Android body excludes expression; insertion before foreground hair keeps identical anatomy.
    insert = next(i for i, s in enumerate(shapes) if s[0].startswith('M29,40') or s[0].startswith('M28,40'))
    all_shapes = shapes[:insert] + face + shapes[insert:]
    def svg(paths):
        return '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="320" viewBox="0 0 128 160">' + ''.join(f'<path d="{escape(d)}" fill="{fill}"' + (f' stroke="{stroke}" stroke-width="{width}" stroke-linecap="round" stroke-linejoin="round"' if stroke else '') + '/>' for d, fill, stroke, width in paths) + '</svg>'
    (OUT / f'{ident}.svg').write_text(svg(all_shapes), encoding='utf-8')
    def color(c): return '#00000000' if c=='none' else c
    vector='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="128dp" android:height="160dp" android:viewportWidth="128" android:viewportHeight="160">'
    vector+=''.join(f'<path android:pathData="{escape(d)}" android:fillColor="{color(fill)}"'+(f' android:strokeColor="{stroke}" android:strokeWidth="{width}" android:strokeLineCap="round" android:strokeLineJoin="round"' if stroke else '')+'/>' for d,fill,stroke,width in shapes)
    (RES/f'avatar_{ident}.xml').write_text(vector+'</vector>',encoding='utf-8')

for a in CATALOG: make(*a)
(OUT.parent/'catalog.json').write_text(json.dumps([dict(id=a[0],name=a[1],subtitle=a[2],accent=a[4],asset=f'avatars/{a[0]}.svg') for a in CATALOG],indent=2)+'\n',encoding='utf-8')
