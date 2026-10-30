# Phone asset origins

The iOS AppIcon PNGs are original project artwork, extending the repository's device-and-signal mark into a shared Remote/Companion family. Editable geometry and SVG masters live in `assets/icons/`; `scripts/generate-app-icons.py` exports the native Android vectors, adaptive/monochrome resources and opaque iOS PNGs. Each PNG embeds its role, geometry SHA256, generator/Pillow version and origin. `assets/icons/exports.json` binds every exported asset to its hash. The native iPhone reference uses the same Remote exports. No third-party artwork or AI-generated image is included in these icons. Platform render/build evidence is recorded separately; asset generation alone is not a launcher-validation pass.

The transparent LaunchImage PNGs remain unmodified pixel assets from the official Flutter3.41.4 app template, flutter/flutter revision ff37bef603469fb030f2b72995ab929ccfc227f0, under packages/flutter_tools/templates/app/ios.tmpl/Runner/Assets.xcassets. Each PNG embeds that origin in text metadata. License: third_party/FLUTTER_LICENSE.

Material icons are supplied by Flutter, and Cupertino glyphs by pinned cupertino_icons1.0.8; Flutter packages their font license information. No new raster illustration was introduced.
