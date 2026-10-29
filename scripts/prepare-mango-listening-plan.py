"""Prepare 20 candidates from observed Mango UI labels. Sends no device command."""
import argparse
import json
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("inventory", type=Path)
parser.add_argument("--output", type=Path, help="Optional separate plan under this repository's artifacts directory")
args = parser.parse_args()
repo = Path(__file__).resolve().parent.parent
collections = json.loads((args.inventory / "collections.json").read_text(encoding="utf-8-sig"))
groups = []
for collection in collections:
    candidates = []
    for item in sorted(collection["fileLabels"], key=lambda row: row["fileLabel"]):
        filename = item["fileLabel"]
        if item["formatLabel"] not in ("FLAC", "WAV"):
            continue
        title = re.sub(r"^\d{1,3}[. _-]+", "", filename.rsplit(".", 1)[0])
        if re.match(r"^(intro|encore(?: intro)?|opening|ending|intermission|mc)(\b|\s|\d)", title, re.I):
            continue
        candidates.append({"source": "com.ibasso.music", "mode": "ui-assisted",
                           "artistFolder": collection["artistFolder"], "albumFolder": collection["albumFolder"],
                           "locationLabels": collection["locationLabels"], "fileLabel": filename,
                           "formatLabel": item["formatLabel"], "expectedTitle": title,
                           "expectedTitleBasis": "Filename-derived hypothesis; exact selected metadata and human audio still required",
                           "status": "NOT TESTED playback/audio", "audibleConfirmation": None})
    if len(candidates) >= 2:
        groups.append(candidates)
groups = groups[:5]
if len(groups) < 5:
    raise SystemExit("Need five observed album folders with at least two candidate songs each")
chosen, used = [], set()

def take(candidate):
    identity = tuple(candidate["locationLabels"] + [candidate["fileLabel"]])
    if identity in used or any(row["expectedTitle"] == candidate["expectedTitle"] for row in chosen):
        return False
    used.add(identity)
    chosen.append(candidate)
    return True

for group in groups:
    count = 0
    for candidate in group:
        count += take(candidate)
        if count == 2:
            break
    if count != 2:
        raise SystemExit("Insufficient distinct song labels across observed folders")
for format_label in ("WAV", "FLAC"):
    for group in groups:
        for candidate in group:
            if sum(row["formatLabel"] == format_label for row in chosen) >= 8:
                break
            if candidate["formatLabel"] == format_label:
                take(candidate)
    if sum(row["formatLabel"] == format_label for row in chosen) < 8:
        raise SystemExit(f"Need eight distinct {format_label} candidates; inspect more folders")
while len(chosen) < 20:
    before = len(chosen)
    for group in groups:
        for candidate in group:
            if take(candidate):
                break
        if len(chosen) == 20:
            break
    if len(chosen) == before:
        raise SystemExit("Not enough distinct candidate labels for twenty selections")
if len(chosen) != 20:
    raise SystemExit("Candidate quotas exceed twenty; inspect plan allocation")
# Keep both formats and exercise selections farther down real folder lists.
# Actual page numbers must still be observed during the hardware attempt.
for index in (18, 19):
    current = chosen[index]
    group = next(group for group in groups if group[0]["locationLabels"] == current["locationLabels"])
    for late in reversed(group):
        if late["formatLabel"] != current["formatLabel"]:
            continue
        if any(row["expectedTitle"] == late["expectedTitle"] for i, row in enumerate(chosen) if i != index):
            continue
        late = dict(late)
        late["coverage"] = "Later observed folder row; verify actual UI scrolling/page at selection"
        chosen[index] = late
        break
for sequence, candidate in enumerate(chosen, 1):
    candidate["sequence"] = sequence
plan = {"kind": "pending-Mango-listening-plan-no-playback", "generatedUtc": datetime.now(timezone.utc).isoformat(),
        "sourceCommit": subprocess.check_output(["git", "-C", str(repo), "rev-parse", "HEAD"], text=True).strip(),
        "inventoryDirectory": str(args.inventory.resolve()), "selections": chosen,
        "scope": "Fresh opaque UI IDs/header, real file format, selected metadata and human listening must be verified per attempt; no BLE proof"}
out = args.output.resolve() if args.output else repo / "artifacts/mango-listening-plan.json"
if not out.is_relative_to(repo / "artifacts"):
    raise SystemExit("Generated plan must stay inside this repository's artifacts directory")
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps({"candidates": len(chosen), "albumFolders": len(groups),
                  "WAV": sum(row["formatLabel"] == "WAV" for row in chosen),
                  "FLAC": sum(row["formatLabel"] == "FLAC" for row in chosen), "plan": str(out)}))
