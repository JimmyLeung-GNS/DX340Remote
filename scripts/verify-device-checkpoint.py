#!/usr/bin/env python3
"""Verify a local DX340 checkpoint; optionally clone its source into a new directory."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess


def verify(root):
    root = root.resolve()
    manifest = json.loads((root / 'checkpoint.json').read_text(encoding='utf-8-sig'))
    if manifest.get('formatVersion') != 1 or manifest.get('sourceBranch') != 'codex/source-recovery-checkpoint' or not re.fullmatch(r'[a-f0-9]{40}', manifest.get('sourceCommit', '')):
        raise ValueError('Unrecognized checkpoint format/branch/commit')
    names = set()
    for item in manifest['files']:
        relative = Path(item['file'])
        path = (root / relative).resolve()
        if relative.is_absolute() or not path.is_relative_to(root) or item['file'] in names or not path.is_file():
            raise ValueError('Invalid, missing or duplicate checkpoint file')
        names.add(item['file'])
        digest = hashlib.sha256()
        with path.open('rb') as file:
            for chunk in iter(lambda: file.read(1024 * 1024), b''):
                digest.update(chunk)
        if digest.hexdigest() != item['sha256']:
            raise ValueError('SHA256 mismatch: ' + item['file'])
    if 'dx340-checkpoint.bundle' not in names:
        raise ValueError('Source bundle is missing from the integrity manifest')
    return manifest


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--folder', default=str(Path(__file__).resolve().parent))
    parser.add_argument('--clone-to')
    args = parser.parse_args()
    root = Path(args.folder).resolve()
    manifest = verify(root)
    if args.clone_to:
        target = Path(args.clone_to).resolve()
        if target.exists():
            parser.error('Clone target already exists; nothing was overwritten')
        subprocess.run(['git', '-c', 'core.autocrlf=false', 'clone', '--branch', manifest['sourceBranch'], str(root / 'dx340-checkpoint.bundle'), str(target)], check=True)
        head = subprocess.check_output(['git', '-C', str(target), 'rev-parse', 'HEAD'], text=True).strip()
        if head != manifest['sourceCommit']:
            raise ValueError('Cloned source differs from the integrity manifest')
        subprocess.run(['git', '-C', str(target), 'bundle', 'verify', str(root / 'dx340-checkpoint.bundle')], check=True)
    print('Verified checkpoint ' + manifest['sourceCommit'])
