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
    private let operations: Set<String> = ["GET_STATE", "GET_QUEUE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM"]

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
    private func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        let args = call.arguments as? [String: Any] ?? [:]
        switch call.method {
        case "snapshot": result(snapshot()); return
        case "permissions":
            // CBCentralManager initialization invokes Apple's own permission flow.
            result(CBManager.authorization == .allowedAlways); return
        case "captureTab":
            #if DEBUG
            result(ProcessInfo.processInfo.arguments.first { $0.hasPrefix("--capture-tab=") }?.components(separatedBy: "=").last)
            #else
            result(nil)
            #endif
            return
        case "scan": remote.scan()
        case "connect":
            guard let id = args["id"] as? String, let device = remote.devices.first(where: { $0.identifier.uuidString == id }) else {
                result(FlutterError(code: "DEVICE_EXPIRED", message: "Find DX340 again before connecting.", details: nil)); return
            }
            remote.connect(device)
        case "disconnect": remote.disconnect()
        case "authorize": remote.authorize()
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
