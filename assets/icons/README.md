# App icon sources

`family.json` owns the shared108-unit geometry and slate/white palette. `remote.svg` and `companion.svg` are editable, generated vector masters; change the JSON and regenerate to keep every platform consistent. The Remote has a narrow controller body/buttons; the Companion has a wider player/play symbol. These extend the repository's original device-and-signal mark.

From the repository root, with Python and Pillow12.2.0 available:

```powershell
python scripts/generate-app-icons.py
```

This writes native Android vector/adaptive/monochrome resources and both shared/native iPhone AppIcon PNG catalogs. It does not build, install, sign, connect or publish an app. PNGs are opaque square RGB with no baked launcher corner mask. Every PNG embeds origin, role, geometry hash and generator/version metadata. `exports.json` records generated file hashes; LF attributes keep text exports stable across Windows/macOS checkouts. Preserve the declared Pillow version when reproducing raster hashes.

Android foreground artwork stays within the central33-unit safe circle; monochrome uses the same transparent foreground geometry. Circle/rounded/theme software previews are not a substitute for OS render checks. Native Android notifications retain their existing system Bluetooth small icon.

See `docs/test-results/2026-10-31-icon-family.md` for exact build, emulator and pending-platform scope. `phone/ASSET_ORIGINS.md` separately attributes the unchanged Flutter-template launch images. No external artwork, AI image generation, official platform logo or universal-DAP compatibility claim is involved.
