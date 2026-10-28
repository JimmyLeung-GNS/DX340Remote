import Foundation
import Vision

// Evidence readiness only: this does not exercise navigation, Bluetooth or playback.
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
    // A destination label in the tab bar alone is not its navigation title.
    let requiredHeadingCount = ["dx340", "volume up"].contains(heading) ? 1 : 2
    let visibleHeading = lines.filter { $0 == heading }.count >= requiredHeadingCount
    let visibleTabs = ["player", "library", "queue", "setup"].allSatisfy { lines.contains($0) }
    if visibleHeading && visibleTabs { print("Capture ready: \(heading)"); exit(0) }
    print("Capture not ready: heading=\(heading), headingCount=\(lines.filter { $0 == heading }.count), tabs=\(visibleTabs)")
    print("Recognized lines: \(lines)")
    exit(1)
} catch {
    print("Capture recognition failed: \(error)")
    exit(1)
}
