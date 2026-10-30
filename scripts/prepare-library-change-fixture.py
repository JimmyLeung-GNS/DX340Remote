"""Generate an owned silent public-storage fixture; never touches a device or user music."""
import argparse
import hashlib
import json
from pathlib import Path
import uuid
import wave


def prepare(parent: Path) -> Path:
    identity = uuid.uuid4().hex
    folder = parent / f"library-change-{identity}"
    folder.mkdir(parents=True, exist_ok=False)
    wav = folder / "probe-A.wav"
    with wav.open("xb") as output:
        with wave.open(output, "wb") as sound:
            sound.setnchannels(2)
            sound.setsampwidth(3)
            sound.setframerate(48_000)
            sound.writeframes(bytes(48_000 * 2 * 3))
    marker = folder / "ownership.txt"
    marker.write_text(f"DX340 Remote T11 owned fixture\n{identity}\n", encoding="utf-8")
    manifest = {
        "kind": "generated-silent-library-change-fixture-not-audio-acceptance",
        "identity": identity,
        "deviceDirectory": f"/sdcard/Music/DX340RemoteVerification-{identity}",
        "initialName": "probe-A.wav",
        "renamedName": "probe-B.wav",
        "markerName": marker.name,
        "files": {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (wav, marker)},
        "format": {"sampleRate": 48_000, "channels": 2, "bits": 24, "frames": 48_000, "signal": "silence"},
        "state": "LOCAL_ONLY_NOT_UPLOADED",
        "cleanup": "Verify ownership marker/hash, remove only listed owned files, then rmdir the empty exact directory. Never recursively delete or touch other music.",
    }
    (folder / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return folder


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-parent", type=Path, default=Path("artifacts"))
    print(prepare(parser.parse_args().output_parent).resolve())
