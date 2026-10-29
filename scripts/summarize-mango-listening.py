"""Aggregate existing human reports without sending commands or inferring audibility."""
import json
from pathlib import Path

repo = Path(__file__).resolve().parent.parent
attempts = []
for path in sorted((repo / "artifacts/mango-listening").glob("*/summary.json")):
    report = json.loads(path.read_text(encoding="utf-8-sig"))
    if report.get("kind") != "actual-offline-Mango-own-adapter-selection-no-BLE":
        raise SystemExit(f"Unexpected report kind: {path}")
    answer = report.get("audibleConfirmation") or {}
    confirmed = report.get("status") == "VERIFIED_LOCAL_SELECTION_AND_USER_REPORTED_AUDIO"
    if confirmed and not (answer.get("result") == "Confirmed" and report.get("selectionSent")
                          and report.get("observedCorrectPlayingState") and report.get("fileHeader", {}).get("format") == report["candidate"]["formatLabel"]):
        raise SystemExit(f"Inconsistent confirmation evidence: {path}")
    attempts.append(report)
confirmed = [row for row in attempts if row.get("status") == "VERIFIED_LOCAL_SELECTION_AND_USER_REPORTED_AUDIO"]
failed = [row for row in attempts if row.get("failure") or row.get("status", "").startswith("FAILED")]
pending = [row for row in attempts if row.get("status") == "AWAITING_HUMAN_LISTENING_CONFIRMATION"]
identities = {tuple(row["candidate"]["locationLabels"] + [row["candidate"]["fileLabel"]]) for row in confirmed}
sequences = {row["sequence"] for row in confirmed}
summary = {"confirmedSelections": len(confirmed), "distinctConfirmedFileLocations": len(identities),
           "albumFolders": len({tuple(row["candidate"]["locationLabels"]) for row in confirmed}),
           "WAV": sum(row["fileHeader"]["format"] == "WAV" for row in confirmed),
           "FLAC": sum(row["fileHeader"]["format"] == "FLAC" for row in confirmed),
           "failedAttempts": len(failed), "pendingListening": len(pending), "totalAttempts": len(attempts),
           "confirmedSelectionsWithExplicitLaterPage": sum(row.get("selectionUiPage", -1) > 0 for row in confirmed),
           "laterPageEvidence": [{"sequence": row["sequence"], "page": row["selectionUiPage"], "file": row["candidate"]["fileLabel"]}
                                 for row in confirmed if row.get("selectionUiPage", -1) > 0],
           "installedCompanionHashes": sorted({row.get("installedCompanionSha256", "missing") for row in attempts}),
           "twentyUniqueConfirmedSequences": sequences == set(range(1, 21)) and len(confirmed) == 20,
           "cleanTwentyAttemptSample": len(attempts) == 20 and len(confirmed) == 20 and not failed,
           "scope": "Source-local exact UI file selection, declared container metadata and delayed human reports; not full T08, phone BLE, SD/rescan, native album/playlist playback or audio quality acceptance"}
(repo / "artifacts/mango-listening-results.json").write_text(json.dumps(attempts, ensure_ascii=False, indent=2), encoding="utf-8")
(repo / "artifacts/mango-listening-summary.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
table = ["| # | Album folder | Selected file | Container | Header rate / bits | Explicit UI page | User audio |",
         "|---:|---|---|---|---|---|---|"]
for row in attempts:
    candidate, header = row["candidate"], row.get("fileHeader", {})
    status = (row.get("audibleConfirmation") or {}).get("result", row.get("status", "Unknown"))
    values = [str(row["sequence"]), candidate["albumFolder"], candidate["fileLabel"], header.get("format", "Not verified"),
              f"{header.get('sampleRateHz', '?')} Hz / {header.get('bitsPerSample', '?')} container bits", str(row.get("selectionUiPage", "—")), status]
    table.append("|" + "|".join(value.replace("|", "\\|").replace("\n", " ") for value in values) + "|")
(repo / "artifacts/mango-listening-table.md").write_text("\n".join(table) + "\n", encoding="utf-8")
print(json.dumps(summary, ensure_ascii=True))
