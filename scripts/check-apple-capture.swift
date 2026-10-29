import Foundation
import Vision

// Evidence readiness only: this does not exercise navigation, Bluetooth or playback.
func readiness(_ rawLines: [String], heading rawHeading: String) -> (ready: Bool, count: Int, tabs: Bool) {
    let heading = rawHeading.lowercased()
    let lines = rawLines.map { $0.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) }
    // Vision reads the adjacent volume icon as a bullet in the observed fixture.
    // Accept only those specific control-label spellings; navigation titles stay exact.
    let allowed = heading == "volume up" ? [heading, "• " + heading, "· " + heading] : [heading]
    let count = lines.filter { allowed.contains($0) }.count
    let requiredCount = ["dx340", "volume up"].contains(heading) ? 1 : 2
    let tabs = ["player", "library", "queue", "setup"].allSatisfy { lines.contains($0) }
    return (count >= requiredCount && tabs, count, tabs)
}

if CommandLine.arguments.count == 2 && CommandLine.arguments[1] == "--self-test" {
    let tabs = ["Player", "Library", "Queue", "Setup"]
    let cases: [(String, [String], String, Bool)] = [
        ("observed icon prefix", ["DX340", "Synthetic volume UI", "• volume up"] + tabs, "volume up", true),
        ("plain control", ["Volume up"] + tabs, "volume up", true),
        ("middle dot control", ["· Volume up"] + tabs, "volume up", true),
        ("arbitrary text prefix", ["Not volume up"] + tabs, "volume up", false),
        ("wrong control", ["Volume down"] + tabs, "volume up", false),
        ("incomplete screen", ["• volume up", "Player", "Library", "Queue"], "volume up", false),
        ("destination tab alone", tabs, "library", false),
        ("navigation and destination", ["Library"] + tabs, "library", true),
        ("navigation prefix stays exact", ["• Library"] + tabs, "library", false),
        ("splash is not ready", ["DX340"], "dx340", false)
    ]
    for (name, lines, heading, expected) in cases {
        guard readiness(lines, heading: heading).ready == expected else {
            print("Capture readiness regression failed: \(name)"); exit(1)
        }
    }
    print("\(cases.count) capture readiness regression cases passed"); exit(0)
}

guard CommandLine.arguments.count == 3 else { exit(2) }
let image = URL(fileURLWithPath: CommandLine.arguments[1])
let heading = CommandLine.arguments[2].lowercased()
do {
    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    // Vision's default 1/32 image height would discard native tab labels.
    request.minimumTextHeight = 0.005
    request.recognitionLanguages = ["en-US"]
    request.usesLanguageCorrection = false
    try VNImageRequestHandler(url: image, options: [:]).perform([request])
    let lines = (request.results ?? []).compactMap { $0.topCandidates(1).first?.string.lowercased().trimmingCharacters(in: .whitespacesAndNewlines) }
    let result = readiness(lines, heading: heading)
    if result.ready { print("Capture ready: \(heading)"); exit(0) }
    print("Capture not ready: heading=\(heading), headingCount=\(result.count), tabs=\(result.tabs)")
    print("Recognized lines: \(lines)")
    exit(1)
} catch {
    print("Capture recognition failed: \(error)")
    exit(1)
}
