"""Deterministic native icon exports. Requires Pillow; no network or image service.

Geometry is editable in assets/icons/family.json. The same quadratic paths feed
SVG masters, Android vectors and antialiased iOS PNGs. No pre-rounded iOS corners.
"""
from pathlib import Path
import hashlib
import json
import math
import re
from PIL import Image, ImageChops, ImageDraw, PngImagePlugin

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'assets/icons/family.json'
FAMILY = json.loads(SOURCE.read_text())
DESTINATIONS = []


def save(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data.encode('utf-8') if isinstance(data, str) else data)
    DESTINATIONS.append(path)


def contours(path):
    tokens = re.findall(r'[MLQZ]|-?\d+(?:\.\d+)?', path)
    result, points, at = [], [], (0, 0)
    i = 0
    while i < len(tokens):
        command = tokens[i]
        i += 1
        if command in ('M', 'L'):
            at = tuple(float(x) for x in tokens[i:i + 2])
            i += 2
            if command == 'M' and points:
                result.append(points)
                points = []
            points.append(at)
        elif command == 'Q':
            control = tuple(float(x) for x in tokens[i:i + 2])
            end = tuple(float(x) for x in tokens[i + 2:i + 4])
            i += 4
            start = at
            for step in range(1, 65):
                t = step / 64
                points.append(tuple((1-t)**2 * start[k] + 2*(1-t)*t*control[k] + t*t*end[k] for k in (0, 1)))
            at = end
        elif command == 'Z':
            result.append(points)
            points = []
        else:
            raise ValueError(command)
    if points:
        result.append(points)
    return result


def mask(role, size):
    scale = size / 108
    result = Image.new('1', (size, size))
    for polygon in contours(FAMILY['roles'][role]['fillPath']):
        part = Image.new('1', (size, size))
        ImageDraw.Draw(part).polygon([(x*scale, y*scale) for x, y in polygon], fill=1)
        result = ImageChops.logical_xor(result, part)
    result = result.convert('L')
    draw = ImageDraw.Draw(result)
    width = FAMILY['strokeWidth'] * scale
    for signal in FAMILY['signals']:
        points = contours(signal)[0]
        # A filled offset contour avoids Pillow's thick-polyline joint seams.
        left, right = [], []
        for index, (x, y) in enumerate(points):
            before, after = points[max(0, index-1)], points[min(len(points)-1, index+1)]
            dx, dy = after[0]-before[0], after[1]-before[1]
            length = math.hypot(dx, dy)
            nx, ny = -dy/length*width/2, dx/length*width/2
            left.append((x*scale+nx, y*scale+ny))
            right.append((x*scale-nx, y*scale-ny))
        draw.polygon(left + right[::-1], fill=255)
        for x, y in (points[0], points[-1]):
            x, y, r = x*scale, y*scale, width/2
            draw.ellipse((x-r, y-r, x+r, y+r), fill=255)
    return result


def png(role, size, path):
    large = size * 4
    image = Image.new('RGB', (large, large), FAMILY['background'])
    image.paste(FAMILY['foreground'], mask=mask(role, large))
    image = image.resize((size, size), Image.Resampling.LANCZOS)
    metadata = PngImagePlugin.PngInfo()
    metadata.add_text('Origin', FAMILY['origin'])
    metadata.add_text('Role', role)
    metadata.add_text('Geometry-SHA256', hashlib.sha256(SOURCE.read_bytes()).hexdigest())
    metadata.add_text('Generator', 'scripts/generate-app-icons.py v1; Pillow ' + Image.__version__)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, pnginfo=metadata, optimize=True)
    DESTINATIONS.append(path)


def vector(role, background=False):
    fill = FAMILY['roles'][role]['fillPath']
    elements = []
    if background:
        elements.append(f'    <path android:fillColor="{FAMILY["background"]}" android:pathData="M0,0 L108,0 L108,108 L0,108 Z"/>')
    elements.append(f'    <path android:fillColor="#FFFFFF" android:fillType="evenOdd" android:pathData="{fill}"/>')
    for signal in FAMILY['signals']:
        elements.append(f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF" android:strokeWidth="4" android:strokeLineCap="round" android:pathData="{signal}"/>')
    return '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n' + '\n'.join(elements) + '\n</vector>\n'


def main():
    for role, geometry in FAMILY['roles'].items():
        svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108"><title>DX340 {role.title()}</title><rect width="108" height="108" fill="{FAMILY["background"]}"/><path fill="#fff" fill-rule="evenodd" d="{geometry["fillPath"]}"/>'
        svg += ''.join(f'<path fill="none" stroke="#fff" stroke-width="4" stroke-linecap="round" d="{p}"/>' for p in FAMILY['signals']) + '</svg>\n'
        save(ROOT / f'assets/icons/{role}.svg', svg)
    for module, role in [('android/host-app', 'companion'), ('android/remote-app', 'remote'), ('phone/android/app', 'remote')]:
        res = ROOT / module / 'src/main/res'
        save(res / 'drawable/ic_dx340.xml', vector(role, background=True))
        save(res / 'drawable/ic_launcher_foreground.xml', vector(role))
        save(res / 'drawable/ic_launcher_background.xml', '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle"><solid android:color="#263238"/></shape>\n')
        for folder, monochrome in [('mipmap-anydpi-v26', ''), ('mipmap-anydpi-v33', '\n    <monochrome android:drawable="@drawable/ic_launcher_foreground"/>')]:
            save(res / folder / 'ic_launcher.xml', '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/ic_launcher_background"/>\n    <foreground android:drawable="@drawable/ic_launcher_foreground"/>' + monochrome + '\n</adaptive-icon>\n')
    catalog = ROOT / 'phone/ios/Runner/Assets.xcassets/AppIcon.appiconset'
    contents = json.loads((catalog / 'Contents.json').read_text())
    for entry in contents['images']:
        size = round(float(entry['size'].split('x')[0]) * float(entry['scale'].rstrip('x')))
        png('remote', size, catalog / entry['filename'])
    native = ROOT / 'apple/DXRemote/Assets.xcassets/AppIcon.appiconset'
    save(native / 'Contents.json', json.dumps(contents, indent=2) + '\n')
    save(native.parent / 'Contents.json', '{"info":{"author":"xcode","version":1}}\n')
    for entry in contents['images']:
        size = round(float(entry['size'].split('x')[0]) * float(entry['scale'].rstrip('x')))
        png('remote', size, native / entry['filename'])
    manifest = {'geometrySha256': hashlib.sha256(SOURCE.read_bytes()).hexdigest(), 'generator': 'scripts/generate-app-icons.py v1', 'pillow': Image.__version__, 'files': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(set(DESTINATIONS))}}
    save(ROOT / 'assets/icons/exports.json', json.dumps(manifest, indent=2) + '\n')
    print(f'Generated {len(set(DESTINATIONS))} icon files with source provenance.')


if __name__ == '__main__':
    main()
