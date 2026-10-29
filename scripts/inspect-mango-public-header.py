"""Read at most 256 bytes of one observed public Music file. No private app data."""
import argparse
import base64
import json
import shlex
import subprocess
from datetime import datetime, timezone
from pathlib import Path, PurePosixPath
from mango_header_format import inspect_header

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--candidate", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--adb", required=True)
parser.add_argument("--serial", default="d")
args = parser.parse_args()
candidate = json.loads(args.candidate.read_text(encoding="utf-8-sig"))
labels = candidate["locationLabels"] + [candidate["fileLabel"]]
if labels[:2] != ["Internal Storage", "Music"] or len(labels) > 9:
    raise SystemExit("Only observed Internal Storage/Music candidate labels are accepted")
if any(not label or label in (".", "..") or "/" in label or any(ord(c) < 32 for c in label) for label in labels):
    raise SystemExit("Invalid single-component source labels")
public_path = str(PurePosixPath("/sdcard", *labels[1:]))
command = f"dd if={shlex.quote(public_path)} bs=256 count=1 2>/dev/null | base64"
encoded = subprocess.check_output([args.adb, "-s", args.serial, "shell", command], timeout=10)
data = base64.b64decode(b"".join(encoded.split()), validate=True)
result = {"kind": "bounded-public-Mango-candidate-header-no-playback", "observedUtc": datetime.now(timezone.utc).isoformat(),
          "publicPath": public_path, "bytesRead": len(data), "containerPrefixHex": data[:12].hex()}
try:
    result.update(inspect_header(data, candidate["formatLabel"]))
except ValueError as error:
    raise SystemExit(str(error))
result["scope"] = "Container/declared header metadata only; not decoded audio, output sample rate, bit-perfect or quality proof. No audio bytes retained."
args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({key: result[key] for key in ("format", "sampleRateHz", "bitsPerSample", "channels")}))
