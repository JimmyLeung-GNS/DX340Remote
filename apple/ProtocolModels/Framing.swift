import Foundation

public enum HostReplyDecision: String { case ignore, hello, current, reconnect }

/// Run before ERROR/ACCEPTED dispatch or publishing reply state.
public enum HostReplyGate {
    public static let changedMessage = "Player connection changed. Connect again to refresh."
    public static func classify(type: String, operation: String?, expectedHost: String, replyHost: String, authorized: Bool) -> HostReplyDecision {
        if type == "STATE_CHANGED" {
            if !authorized { return .ignore }
        } else if operation == nil { return .ignore }
        if type == "HELLO" {
            return operation == "HELLO" && !replyHost.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? .hello : .reconnect
        }
        if expectedHost.isEmpty { return operation == "HELLO" && type == "ERROR" ? .current : .reconnect }
        return replyHost == expectedHost ? .current : .reconnect
    }
}

public struct PlayerChoice: Equatable {
    public let target: String?
    public let choose: Bool
}
public enum PlayerDiscovery {
    public static func finish(found: [String], remembered: String?) -> PlayerChoice {
        let ids = Array(Set(found.filter { !$0.isEmpty }))
        let saved = remembered.flatMap { $0.isEmpty ? nil : $0 }
        if let saved, ids.contains(saved) { return PlayerChoice(target: saved, choose: false) }
        if saved == nil, ids.count == 1 { return PlayerChoice(target: ids[0], choose: false) }
        return PlayerChoice(target: nil, choose: !ids.isEmpty)
    }
}
/// This gate never extends the host/client setup deadline or replays a source command.
public struct ApprovalRetry {
    private var generation: UInt64 = 0
    public private(set) var active = false
    public init() {}
    public mutating func begin() -> UInt64 {
        if !active { generation &+= 1; active = true }; return generation
    }
    public mutating func clear() { generation &+= 1; active = false }
    public func current(_ ticket: UInt64) -> Bool { active && ticket == generation }
    public func mayCheck(ticket: UInt64, setupActive: Bool, authorized: Bool, requestPending: Bool) -> Bool {
        current(ticket) && setupActive && !authorized && !requestPending
    }
}

/// An assisted selection consumes source UI action IDs, not the controller grant.
public struct AssistedPageUse {
    public static func tracks(source: String, item: String, assisted: Bool, requestedRevision: String, pageRevision: String) -> Bool {
        let sourceItem = (source == "com.ibasso.music" && item.hasPrefix("mango-ui:")) ||
            (source == "com.apple.android.music" && item.hasPrefix("ui:"))
        return sourceItem && assisted && !pageRevision.isEmpty && requestedRevision == pageRevision
    }
    private struct Use { let id: String, source: String, epoch: String, revision: String }
    private var pending: Use?
    public init() {}
    public mutating func begin(id: String, source: String, epoch: String, revision: String, assisted: Bool) {
        pending = assisted && !id.isEmpty && !revision.isEmpty ? Use(id: id, source: source, epoch: epoch, revision: revision) : nil
    }
    public mutating func clear() { pending = nil }
    public mutating func abandon(source: String, epoch: String, revision: String) -> Bool {
        guard let use = pending else { return false }
        return finish(id: use.id, source: source, epoch: epoch, revision: revision)
    }
    public mutating func finish(id: String, source: String, epoch: String, revision: String) -> Bool {
        guard let use = pending, id == use.id else { return false }
        pending = nil
        return source == use.source && epoch == use.epoch && revision == use.revision
    }
}

public struct BrowseContext: Equatable {
    public let source: String
    public let epoch: String
    public let mode: String
    public let assistedReady: Bool
    public init(source: String, epoch: String, mode: String, assistedReady: Bool) {
        self.source = source; self.epoch = epoch; self.mode = mode; self.assistedReady = assistedReady
    }
}
public enum BrowseReset: String { case none = "NONE", all = "ALL", assisted = "ASSISTED" }
public struct BrowseInvalidation {
    public let reset: BrowseReset
    public let message: String?
}
/// Presentation invalidation only; never sends a request or retries playback.
public enum BrowseContextTransition {
    public static func evaluate(_ old: BrowseContext, _ new: BrowseContext, hasLibrary: Bool, hasQueue: Bool,
                                assistedLibrary: Bool, notify: Bool) -> BrowseInvalidation {
        if old.source != new.source || old.epoch != new.epoch || old.mode != new.mode {
            return BrowseInvalidation(reset: .all, message: notify && (hasLibrary || hasQueue) ? "Library changed. Browse again or refresh Queue." : nil)
        }
        if old.assistedReady != new.assistedReady && assistedLibrary {
            return BrowseInvalidation(reset: .assisted, message: notify && hasLibrary ? "Assisted browsing changed. Browse again; wake/unlock DX340 and check browsing access if needed." : nil)
        }
        return BrowseInvalidation(reset: .none, message: nil)
    }
}

public enum WireError: Error { case malformed, incompatible, outOfOrder, limit }
/// Client recovery only. This never extends the host's pairing/authorization windows.
public struct ConnectionSetupDeadline {
    public static let timeoutMs: Int64 = 60000
    public static let message = "Connection setup expired. In Setup, find and connect to DX340 again. Open pairing on DX340 if approval is needed."
    private var generation: UInt64 = 0
    private var startedAt: Int64?
    public init() {}
    public var active: Bool { startedAt != nil }
    public mutating func begin(at nowMs: Int64) -> UInt64 {
        precondition(nowMs >= 0)
        generation &+= 1; startedAt = nowMs
        return generation
    }
    public mutating func clear() { generation &+= 1; startedAt = nil }
    public func expired(at nowMs: Int64, ticket: UInt64? = nil) -> Bool {
        guard let start = startedAt, ticket == nil || ticket == generation else { return false }
        return nowMs < start || nowMs - start >= Self.timeoutMs
    }
}
/// Pending observation only; host authorization, source deadlines and no-replay rules stay separate.
public enum ReplyBudget {
    public static func milliseconds(_ operation: String, item: String? = nil) -> Int64 {
        if operation == "GET_ARTWORK" { return 75000 }
        if operation == "PLAY_ITEM", item?.hasPrefix("mango-ui:") == true { return 30000 }
        return 12000
    }
}
public enum Wire {
    public static let service = "e7e01000-4a76-4b53-9c8f-c7a12b00d340"
    public static let info = "e7e01001-4a76-4b53-9c8f-c7a12b00d340"
    public static let rx = "e7e01002-4a76-4b53-9c8f-c7a12b00d340"
    public static let tx = "e7e01003-4a76-4b53-9c8f-c7a12b00d340"
    public static func frames(_ data: Data, capacity: Int, id: UInt16) throws -> [Data] {
        guard !data.isEmpty, data.count <= 65536, (9...512).contains(capacity) else { throw WireError.limit }
        let width = capacity - 8
        let count = (data.count + width - 1) / width
        guard count <= 65535 else { throw WireError.limit }
        return (0..<count).map { index in
            let header: [UInt8] = [0x44, 1, UInt8(id >> 8), UInt8(id & 255), UInt8(index >> 8), UInt8(index & 255), UInt8(count >> 8), UInt8(count & 255)]
            return Data(header) + data.subdata(in: (index * width)..<min((index + 1) * width, data.count))
        }
    }
}

/// A handoff proposal is bound to the observed host and consumed only by a user action.
public struct ControlProposal {
    private var revision: String?
    public init() {}
    public var available: Bool { revision != nil }
    public mutating func observeBusy(replyHost: String, expectedHost: String, value: String) {
        let bytes = Array(value.utf8)
        let valid = (1...64).contains(bytes.count) && bytes.allSatisfy { (48...57).contains($0) || (65...90).contains($0) || (97...122).contains($0) || $0 == 45 }
        revision = !expectedHost.isEmpty && replyHost == expectedHost && valid ? value : nil
    }
    public mutating func consume() -> String? { let value = revision; revision = nil; return value }
    public mutating func clear() { revision = nil }
}

public final class Reassembler {
    private struct Partial { let count: Int; let start: TimeInterval; var lastProgress: TimeInterval; var pieces: Int; var bytes: Data }
    private var partials: [Int: Partial] = [:]
    private let now: () -> TimeInterval
    public init(now: @escaping () -> TimeInterval = { ProcessInfo.processInfo.systemUptime }) { self.now = now }
    public func clear() { partials.removeAll() }
    public func accept(_ data: Data) throws -> Data? {
        let time = now(); partials = partials.filter { time >= $0.value.lastProgress && time - $0.value.lastProgress < 10 && time - $0.value.start < 60 }
        guard (9...512).contains(data.count) else { throw WireError.malformed }
        let b = [UInt8](data)
        guard b[0] == 0x44, b[1] == 1 else { throw WireError.incompatible }
        let id = Int(b[2]) * 256 + Int(b[3]), index = Int(b[4]) * 256 + Int(b[5]), count = Int(b[6]) * 256 + Int(b[7])
        guard count > 0, index < count else { throw WireError.malformed }
        if partials[id] == nil {
            guard index == 0, partials.count < 4 else { throw WireError.outOfOrder }
            partials[id] = Partial(count: count, start: time, lastProgress: time, pieces: 0, bytes: Data())
        }
        var p = partials[id]!
        guard p.count == count, p.pieces == index, p.bytes.count + b.count - 8 <= 65536 else {
            partials[id] = nil; throw WireError.malformed
        }
        p.bytes.append(contentsOf: b.dropFirst(8)); p.pieces += 1
        p.lastProgress = time
        if p.pieces == count { partials[id] = nil; return p.bytes }
        partials[id] = p; return nil
    }
}

/// Bounded observations only. Never authorizes, sends or retries a command.
public final class RequestJournal {
    private let capacity: Int
    private let now: () -> Int64
    private let origin: Int64
    private struct Entry {
        let id: String, operation: String, source: String
        let start: Int64
        var accepted = false
        var acceptedAfter: Int64?, finishedAfter: Int64?
        var result = "pending", outcome = "none"
        var terminalApplied = false
        var error: String?
    }
    private var entries: [String: Entry] = [:]
    private var order: [String] = []
    private var connections: [[String: Any]] = []
    private var evictedRequests: Int64 = 0, evictedConnections: Int64 = 0
    private static let clockOrigin = ContinuousClock.now
    public static func monotonicMilliseconds() -> Int64 {
        let elapsed = clockOrigin.duration(to: ContinuousClock.now).components
        return elapsed.seconds * 1_000 + elapsed.attoseconds / 1_000_000_000_000_000
    }
    public init(capacity: Int = 256, now: @escaping () -> Int64 = RequestJournal.monotonicMilliseconds) {
        precondition((1...512).contains(capacity)); self.capacity = capacity; self.now = now; origin = now()
    }
    private func elapsed(_ end: Int64, _ start: Int64) -> Int64? { end >= start && start >= 0 ? end - start : nil }
    public func begin(id: String, operation: String, source: String) {
        guard !id.isEmpty, id.utf8.count <= 64, id.utf8.allSatisfy({ (48...57).contains($0) || (65...90).contains($0) || (97...122).contains($0) || $0 == 45 || $0 == 95 }), entries[id] == nil else { return }
        if order.count == capacity { entries.removeValue(forKey: order.removeFirst()); evictedRequests += 1 }
        order.append(id); entries[id] = Entry(id: id, operation: Self.operations.contains(operation) ? operation : "OTHER", source: Self.sourceCategory(source), start: now())
    }
    public func phase(_ value: String) {
        let phase = Self.phases.contains(value) ? value : "other"
        guard connections.last?["phase"] as? String != phase else { return }
        if connections.count == capacity { connections.removeFirst(); evictedConnections += 1 }
        connections.append(["atMs": elapsed(now(), origin) as Any? ?? NSNull(), "phase": phase])
    }
    @discardableResult public func reply(id: String, type: String, outcome: String = "", error: String = "") -> [String: Any]? {
        guard var entry = entries[id], entry.result == "pending" else { return nil }
        if type == "ACCEPTED" {
            guard !entry.accepted else { return nil }
            entry.accepted = true; entry.acceptedAfter = elapsed(now(), entry.start)
        } else {
            entry.finishedAfter = elapsed(now(), entry.start)
            entry.result = ["HELLO", "RESULT"].contains(type) ? "result" : type == "ERROR" ? "error" : "other_reply"
            entry.outcome = ["observed", "dispatched"].contains(outcome) ? outcome : "none"
            entry.error = type == "ERROR" ? (Self.errors.contains(error) ? error : "OTHER") : nil
        }
        entries[id] = entry; return snapshot(entry)
    }
    public func markTerminalApplied(id: String) {
        guard var entry = entries[id], ["result", "error", "other_reply"].contains(entry.result) else { return }
        entry.terminalApplied = true; entries[id] = entry
    }
    @discardableResult public func finish(id: String, reason: String) -> [String: Any]? {
        guard var entry = entries[id], entry.result == "pending" else { return nil }
        entry.result = Self.endings.contains(reason) ? reason : "other"; entry.finishedAfter = elapsed(now(), entry.start)
        entries[id] = entry; return snapshot(entry)
    }
    @discardableResult public func abandon(reason: String = "connection_lost") -> [[String: Any]] { order.compactMap { finish(id: $0, reason: reason) } }
    private func snapshot(_ e: Entry) -> [String: Any] {
        ["id": e.id, "operation": e.operation, "source": e.source, "startedMs": elapsed(e.start, origin) as Any? ?? NSNull(),
         "accepted": e.accepted, "acceptedAfterMs": e.acceptedAfter as Any? ?? NSNull(), "finishedAfterMs": e.finishedAfter as Any? ?? NSNull(),
         "result": e.result, "terminalApplied": e.terminalApplied, "hostOutcome": e.outcome, "error": e.error as Any? ?? NSNull()]
    }
    public func report() -> [String: Any] {
        ["formatVersion": 1, "clock": "monotonic_ms_relative_to_model", "capacity": capacity,
         "evictedRequests": evictedRequests, "evictedConnections": evictedConnections,
         "requests": order.compactMap { entries[$0].map(snapshot) }, "connections": connections]
    }
    private static let operations: Set<String> = ["HELLO", "AUTHORIZE", "REQUEST_CONTROL", "GET_STATE", "GET_QUEUE", "GET_ARTWORK", "GET_CAPABILITIES", "LIST_SOURCES", "SUBSCRIBE_STATE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME", "BROWSE", "SEARCH", "SET_SHUFFLE", "SET_REPEAT"]
    // Wire source IDs are package names. Export only these fixed categories.
    private static func sourceCategory(_ source: String) -> String {
        switch source {
        case "com.ibasso.music", "mango": return "mango"
        case "com.apple.android.music", "apple": return "apple"
        case "none", "": return source
        default: return "other"
        }
    }
    private static let errors: Set<String> = ["AUTHORIZATION_DENIED", "LOCAL_APPROVAL_REQUIRED", "CONTROLLER_BUSY", "STALE_HOST", "STALE_CONTROL", "CONTROL_REQUEST_USED", "STALE_SOURCE", "SOURCE_UNAVAILABLE", "SOURCE_NOT_READY", "SOURCE_BUSY", "UNSUPPORTED", "STALE_LIBRARY", "EXPIRED_ITEM", "STALE_QUEUE", "STALE_ARTWORK", "INVALID_REQUEST", "BAD_REQUEST", "MALFORMED_REQUEST", "PROTOCOL_MISMATCH", "STALE_VOLUME", "VOLUME_AT_BOUNDARY", "VOLUME_PROFILE_CHANGED", "VOLUME_VERIFICATION_REQUIRED", "VOLUME_SOURCE_NOT_READY", "UI_PERMISSION_REQUIRED", "UI_SCREEN_UNLOCK_REQUIRED", "UI_FOREGROUND_REQUIRED", "UI_SETUP_REQUIRED", "UI_UNEXPECTED_SCREEN", "UI_STORAGE_CONTEXT_REQUIRED", "UI_VERSION_UNVERIFIED", "UI_ACTIVE_PAGE_UNVERIFIED", "UI_TREE_CHANGED", "UI_ACTION_TIMEOUT", "UI_ACTION_INTERRUPTED", "UI_MODE_DISABLED", "UI_BUSY", "RESPONSE_TOO_LARGE"]
    private static let phases: Set<String> = ["idle", "preparing", "searching", "choosing", "connecting", "approval", "busy", "connected", "permission", "bluetoothOff", "failed"]
    private static let endings: Set<String> = ["timeout", "send_failed", "connection_lost", "session_reset"]
}
