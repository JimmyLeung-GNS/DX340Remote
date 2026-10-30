import Foundation

public enum WireError: Error { case malformed, incompatible, outOfOrder, limit }
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
