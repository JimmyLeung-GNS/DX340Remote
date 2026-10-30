"""Prepare native row candidates from a completed actual source inventory; no device I/O."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess


def canonical_title(item):
    match = re.fullmatch(r'(?:__ALL_MUSIC__|__NOW_PLAY_MUSIC__)([1-9][0-9]*)|__MUSIC__(?:0|[1-9][0-9]*)_(?:0|[1-9][0-9]*)_[1-9][0-9]*_([1-9][0-9]*)', item['id'])
    if not match:
        raise ValueError('Unsupported native candidate ID')
    ordinal = int(match.group(1) or match.group(2))
    suffix = f' {ordinal}'
    if not item['title'].endswith(suffix):
        raise ValueError('Native title does not carry its exact source ordinal')
    return item['title'][:-len(suffix)], ordinal


def build_plan(albums, all_music):
    selections = []

    def add(parent, page, item, album_label=None, reported_count=None):
        title, ordinal = canonical_title(item)
        if not item.get('playable') or item.get('browsable') or item.get('uiAssisted'):
            raise ValueError('Expected currently playable native candidate')
        selections.append({'sequence': len(selections) + 1, 'source': 'com.ibasso.music', 'mode': 'native',
                           'parent': parent, 'page': page, 'id': item['id'], 'displayTitle': item['title'],
                           'expectedTitle': title, 'expectedArtist': item.get('subtitle', ''),
                           'albumLabel': album_label, 'parentReportedCount': reported_count,
                           'ordinal': ordinal, 'status': 'NOT TESTED', 'audibleConfirmation': None,
                           'format': 'UNKNOWN; native metadata does not prove container/file identity'})

    music = all_music['items']
    if len(music) < 2 or not all_music['complete']:
        raise ValueError('Need completed observed All Music category')
    for index in (0, len(music) - 1):
        add('__ALL_MUSIC__', index // 25, music[index], reported_count=len(music))
    # Observed album positions from this source/version; no synthesized rows.
    # Four/three/four/three/four rows cover first, last and later pages.
    for album_id, count in (('__ARTIST_GENRE_ALBUM__0_0_1', 4), ('__ARTIST_GENRE_ALBUM__0_0_2', 3),
                            ('__ARTIST_GENRE_ALBUM__0_0_3', 4), ('__ARTIST_GENRE_ALBUM__0_0_5', 3),
                            ('__ARTIST_GENRE_ALBUM__0_0_10', 4)):
        matching = [album for album in albums if album['id'] == album_id]
        if len(matching) != 1:
            raise ValueError('Required observed native album missing or ambiguous')
        album = matching[0]
        items = album['items']
        if len(items) != album['sourceReportedTracks'] or len(items) < count:
            raise ValueError('Album count differs from observed source')
        indices = [0, 1, len(items) - 1]
        if count == 4:
            indices.insert(2, 25 if len(items) > 26 else len(items) // 2)
        for index in indices:
            add(album_id, index // 25, items[index], album['title'], len(items))
    if len(selections) != 20 or len({(r['parent'], r['id']) for r in selections}) != 20:
        raise ValueError('Need20 distinct native row locations')
    return selections


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('inventory', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    if not args.output.resolve().is_relative_to((repo / 'artifacts').resolve()) or args.output.exists():
        raise SystemExit('Use a new output file under artifacts; preserve earlier plans')
    summary = json.loads((args.inventory / 'summary.json').read_text(encoding='utf-8-sig'))
    if summary['status'] != 'VERIFIED_SOURCE_LOCAL_NATIVE_METADATA_ONLY':
        raise SystemExit('Inventory must have completed without truncation')
    albums = json.loads((args.inventory / 'albums.json').read_text(encoding='utf-8-sig'))
    all_music = json.loads((args.inventory / 'all-music.json').read_text(encoding='utf-8-sig'))
    plan = {'kind': 'observed-native-Mango-listening-plan', 'createdUtc': datetime.now(timezone.utc).isoformat(),
            'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip(),
            'inventory': str(args.inventory.resolve()), 'selections': build_plan(albums, all_music),
            'scope': '20 native locations, not20 unique files or codec/phone/BLE/audio evidence'}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'Prepared20 NOT TESTED native candidates: {args.output}')


if __name__ == '__main__':
    main()
