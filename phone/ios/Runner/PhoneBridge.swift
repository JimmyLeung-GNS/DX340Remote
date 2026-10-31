import Flutter
import UIKit
import Observation
import CoreBluetooth

/// Retained by AppDelegate. CoreBluetooth remains native when Flutter UI is suspended.
@MainActor final class PhoneBridge: NSObject, FlutterStreamHandler {
    private let remote = RemoteConnection()
    private var events: FlutterEventSink?
    private var commands: FlutterMethodChannel?
    private var channel: FlutterEventChannel?
    private var observationGeneration = 0
    private var exportingDiagnostics = false
    private let operations: Set<String> = ["GET_STATE", "GET_QUEUE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME"]

    func attach(_ messenger: FlutterBinaryMessenger) {
        commands = FlutterMethodChannel(name: "org.dx340.remote/commands", binaryMessenger: messenger)
        channel = FlutterEventChannel(name: "org.dx340.remote/events", binaryMessenger: messenger)
        channel?.setStreamHandler(self)
        commands?.setMethodCallHandler { [weak self] call, result in
            MainActor.assumeIsolated { self?.handle(call, result: result) }
        }
    }
    private func snapshot() -> String {
        let value: [String: Any] = [
            "bridgeVersion": 1, "status": remote.status, "authorized": remote.authorized,
            "mutationPending": remote.mutationPending, "state": remote.state,
            "controlRequestAvailable": remote.canRequestControl,
            "connectionSetup": remote.connectionSetup,
            "stateSampleId": remote.stateSampleId, "stateAgeMs": remote.stateAgeMs as Any? ?? NSNull(),
            "sources": remote.sources, "library": remote.library, "queue": remote.queue,
            "queueRevision": remote.queueRevision, "queuePage": remote.queuePage, "queueLoading": remote.queueLoading,
            "devices": remote.devices.map { ["id": $0.identifier.uuidString, "name": $0.name ?? "DX340"] },
            "artworkBase64": remote.artwork?.base64EncodedString() as Any? ?? NSNull()
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: value), let json = String(data: data, encoding: .utf8) else {
            return "{\"bridgeVersion\":1,\"status\":\"Cannot read native connection data. Reopen the remote.\",\"authorized\":false}"
        }
        return json
    }
    private func observe(_ generation: Int) {
        guard generation == observationGeneration, let events else { return }
        let json = withObservationTracking { snapshot() } onChange: { [weak self] in
            // Observation fires before the mutation; emit after it on the native main actor.
            Task { @MainActor [weak self] in self?.observe(generation) }
        }
        events(json)
    }
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        observationGeneration += 1; self.events = events; observe(observationGeneration); return nil
    }
    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        observationGeneration += 1; events = nil; return nil
    }
    private func exportDiagnostics(_ result: @escaping FlutterResult) {
        guard !exportingDiagnostics else { result(FlutterError(code: "BUSY", message: "A connection report is already open.", details: nil)); return }
        let windows = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive }.flatMap { $0.windows }
        guard var presenter = windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            result(FlutterError(code: "REPORT_WINDOW", message: "Return to the app to share a connection report.", details: nil)); return
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        guard !presenter.isBeingDismissed, !(presenter is UIActivityViewController) else {
            result(FlutterError(code: "BUSY", message: "Close the current sharing sheet first.", details: nil)); return
        }
        do {
            guard var report = try JSONSerialization.jsonObject(with: remote.diagnostics()) as? [String: Any] else { throw WireError.malformed }
            report["app"] = ["id": Bundle.main.bundleIdentifier ?? "", "versionName": Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "", "versionCode": Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""]
            report["os"] = ["version": UIDevice.current.systemVersion, "model": UIDevice.current.model]
            let data = try JSONSerialization.data(withJSONObject: report, options: [.prettyPrinted, .sortedKeys])
            guard data.count <= 262144 else { throw CocoaError(.fileWriteOutOfSpace) }
            let directory = FileManager.default.temporaryDirectory.appendingPathComponent("dx340-connection-reports", isDirectory: true)
            try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
            // Keep at most three of our own temporary reports. Never touch destination copies.
            let old = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: [.contentModificationDateKey], options: [.skipsHiddenFiles])
                .filter { $0.lastPathComponent.hasPrefix("connection-") && $0.pathExtension == "json" }
                .sorted { ((try? $0.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast) > ((try? $1.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast) }
            for file in old.dropFirst(2) { try? FileManager.default.removeItem(at: file) }
            let file = directory.appendingPathComponent("connection-\(UUID().uuidString).json")
            try data.write(to: file, options: [.atomic])
            let activity = UIActivityViewController(activityItems: [file], applicationActivities: nil)
            if let popover = activity.popoverPresentationController {
                popover.sourceView = presenter.view
                popover.sourceRect = CGRect(x: presenter.view.bounds.midX, y: presenter.view.bounds.maxY - 1, width: 1, height: 1)
                popover.permittedArrowDirections = []
            }
            exportingDiagnostics = true
            activity.completionWithItemsHandler = { [weak self] _, _, _, _ in
                Task { @MainActor [weak self] in self?.exportingDiagnostics = false }
            }
            presenter.present(activity, animated: true) { result("Choose where to save or share the connection report.") }
        } catch {
            exportingDiagnostics = false
            result(FlutterError(code: "REPORT_SAVE", message: "Could not prepare the connection report. Try again.", details: nil))
        }
    }
    private func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let args = call.arguments as? [String: Any] ?? [:]
        switch call.method {
        case "snapshot": result(snapshot()); return
        case "diagnostics":
            do { result(String(decoding: try remote.diagnostics(), as: UTF8.self)) }
            catch { result(FlutterError(code: "REPORT_READ", message: "Could not read the connection report.", details: nil)) }
            return
        case "exportDiagnostics": exportDiagnostics(result); return
        case "permissions":
            // CBCentralManager initialization invokes Apple's own permission flow.
            result(CBManager.authorization == .allowedAlways || CBManager.authorization == .notDetermined); return
        case "captureTab":
            #if DEBUG
            result(ProcessInfo.processInfo.arguments.first { $0.hasPrefix("--capture-tab=") }?.components(separatedBy: "=").last)
            #else
            result(nil)
            #endif
            return
        case "scan": remote.scan()
        case "connectPlayer": remote.connectPlayer()
        case "searchPlayers": remote.searchPlayers()
        case "bluetoothSettings", "appSettings":
            if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
        case "connect":
            guard let id = args["id"] as? String, let device = remote.devices.first(where: { $0.identifier.uuidString == id }) else {
                result(FlutterError(code: "DEVICE_EXPIRED", message: "Find DX340 again before connecting.", details: nil)); return
            }
            remote.connect(device)
        case "disconnect": remote.disconnect()
        case "authorize": remote.authorize()
        case "requestControl": remote.requestControl()
        case "resume": remote.refreshOnForeground()
        case "browse":
            let page = (args["page"] as? NSNumber)?.intValue ?? 0
            guard (0...100000).contains(page) else { result(FlutterError(code: "BAD_PAGE", message: "Invalid page.", details: nil)); return }
            remote.browse(parent: args["parent"] as? String, page: page)
        case "send":
            guard let type = args["type"] as? String, operations.contains(type) else {
                result(FlutterError(code: "BAD_COMMAND", message: "Unsupported bridge command.", details: nil)); return
            }
            remote.send(type, args: args["args"] as? [String: Any] ?? [:])
        default: result(FlutterMethodNotImplemented); return
        }
        result(nil)
    }
}
