import XCTest
@testable import DXProtocol

final class FramingTests: XCTestCase {
    func testControlProposalNeedsCurrentHostAndExplicitSingleConsumption() {
        var proposal = ControlProposal()
        proposal.observeBusy(replyHost: "other-host", expectedHost: "host-A", value: "revision-1")
        XCTAssertFalse(proposal.available)
        proposal.observeBusy(replyHost: "host-A", expectedHost: "host-A", value: "revision-1")
        XCTAssertEqual(proposal.consume(), "revision-1"); XCTAssertNil(proposal.consume())
        proposal.observeBusy(replyHost: "host-A", expectedHost: "host-A", value: "revision-2")
        proposal.clear(); XCTAssertNil(proposal.consume())
    }
    func testControlProposalRejectsMissingHostAndMalformedRevision() {
        var proposal = ControlProposal()
        for revision in ["", "unsafe:value", String(repeating: "a", count: 65), "字"] {
            proposal.observeBusy(replyHost: "host-A", expectedHost: "host-A", value: revision)
            XCTAssertFalse(proposal.available)
        }
        proposal.observeBusy(replyHost: "", expectedHost: "", value: "revision-1")
        XCTAssertFalse(proposal.available)
    }
    func testFullLimitAndAttBoundaries() throws {
        for capacity in [20, 61, 244, 512] {
            for size in [1, 12, 13, 500, 65536] {
                let bytes = Data((0..<size).map { UInt8($0 % 251) })
                let frames = try Wire.frames(bytes, capacity: capacity, id: 65535), assembler = Reassembler()
                for frame in frames.dropLast() { XCTAssertNil(try assembler.accept(frame)) }
                XCTAssertEqual(try assembler.accept(frames.last!), bytes)
            }
        }
    }
    func testExpiredFragmentsAndDisconnect() throws {
        var time: TimeInterval = 0
        let a = Reassembler(now: { time }), frames = try Wire.frames(Data(repeating: 3, count: 30), capacity: 20, id: 7)
        _ = try a.accept(frames[0]); time = 11
        XCTAssertThrowsError(try a.accept(frames[1]))
        _ = try a.accept(frames[0]); a.clear(); XCTAssertThrowsError(try a.accept(frames[1]))
    }
    func testSharedFixtures() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let fixtures = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/framing.json"))) as! [[String: Any]]
        for fixture in fixtures {
            let bytes = Data((fixture["text"] as! String).utf8)
            let expected = fixture["hex"] as! [String]
            let frames = try Wire.frames(bytes, capacity: fixture["capacity"] as! Int, id: UInt16(fixture["id"] as! Int))
            XCTAssertEqual(frames.map { $0.map { String(format: "%02x", $0) }.joined() }, expected)
        }
    }
}
