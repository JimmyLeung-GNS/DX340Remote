import XCTest
@testable import DXProtocol

final class FramingTests: XCTestCase {
    func testOnlyActualMangoAssistedSelectionsConsumeThePage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let cases = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/assisted-selection-eligibility.json"))) as! [[String: Any]]
        for item in cases {
            XCTAssertEqual(AssistedPageUse.tracks(source: item["source"] as! String, item: item["item"] as! String, assisted: item["assisted"] as! Bool,
                requestedRevision: item["requestedRevision"] as! String, pageRevision: item["pageRevision"] as! String), item["tracks"] as! Bool, item["name"] as! String)
        }
    }
    func testSharedSelectionCompletionRetiresOnlyItsCurrentAssistedPage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let cases = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/assisted-page-use.json"))) as! [[String: Any]]
        for item in cases {
            var use = AssistedPageUse()
            for (index, step) in (item["steps"] as! [[String: Any]]).enumerated() {
                switch step["action"] as! String {
                case "begin": use.begin(id: step["id"] as! String, source: step["source"] as! String, epoch: step["epoch"] as! String, revision: step["revision"] as! String, assisted: step["assisted"] as! Bool)
                case "clear": use.clear()
                case "abandon": XCTAssertEqual(use.abandon(source: step["source"] as! String, epoch: step["epoch"] as! String, revision: step["revision"] as! String), step["retire"] as! Bool, item["name"] as! String)
                case "finish": XCTAssertEqual(use.finish(id: step["id"] as! String, source: step["source"] as! String, epoch: step["epoch"] as! String, revision: step["revision"] as! String), step["retire"] as! Bool, "\(item["name"]!):\(index)")
                default: XCTFail("Unknown fixture action")
                }
            }
        }
    }
    func testSharedConnectionSetupDeadlineRetiresOnlyCurrentUnfinishedAttempt() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let fixture = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/connection-setup-deadline.json"))) as! [String: Any]
        XCTAssertEqual(ConnectionSetupDeadline.timeoutMs, (fixture["timeoutMs"] as! NSNumber).int64Value)
        XCTAssertEqual(ConnectionSetupDeadline.message, fixture["message"] as! String)
        for item in fixture["cases"] as! [[String: Any]] {
            var deadline = ConnectionSetupDeadline(), tickets: [String: UInt64] = [:]
            for (index, step) in (item["steps"] as! [[String: Any]]).enumerated() {
                switch step["action"] as! String {
                case "begin": tickets[step["ticket"] as! String] = deadline.begin(at: (step["at"] as! NSNumber).int64Value)
                case "clear": deadline.clear()
                case "check":
                    let ticket = (step["ticket"] as? String).map { tickets[$0]! }
                    XCTAssertEqual(deadline.expired(at: (step["at"] as! NSNumber).int64Value, ticket: ticket), step["expired"] as! Bool, "\(item["name"]!):\(index)")
                default: XCTFail("Unknown fixture action")
                }
            }
        }
    }
    func testConnectionSetupDeadlineAvoidsOverflowAndClearsAfterApproval() {
        var deadline = ConnectionSetupDeadline()
        let ticket = deadline.begin(at: Int64.max - ConnectionSetupDeadline.timeoutMs)
        XCTAssertFalse(deadline.expired(at: Int64.max - 1, ticket: ticket))
        XCTAssertTrue(deadline.expired(at: Int64.max, ticket: ticket))
        deadline.clear(); XCTAssertFalse(deadline.expired(at: Int64.max, ticket: ticket)); XCTAssertFalse(deadline.active)
    }
    func testSharedBrowseContextTransitionsKeepNativePagesAndAuthorizationStatus() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let cases = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/browse-invalidation.json"))) as! [[String: Any]]
        func context(_ value: Any) -> BrowseContext {
            let fields = value as! [String: Any]
            return BrowseContext(source: fields["source"] as! String, epoch: fields["epoch"] as! String,
                                 mode: fields["mode"] as! String, assistedReady: fields["assistedReady"] as! Bool)
        }
        for item in cases {
            let result = BrowseContextTransition.evaluate(context(item["old"]!), context(item["new"]!),
                hasLibrary: item["hasLibrary"] as! Bool, hasQueue: item["hasQueue"] as! Bool,
                assistedLibrary: item["assistedLibrary"] as! Bool, notify: item["notify"] as! Bool)
            XCTAssertEqual(result.reset.rawValue, item["reset"] as! String, item["name"] as! String)
            XCTAssertEqual(result.message, item["message"] as? String, item["name"] as! String)
        }
    }
    func testReplyBudgetsMatchSharedContractAndRetainObservedAssistedSelection() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        let cases = try JSONSerialization.jsonObject(with: Data(contentsOf: root.appendingPathComponent("protocol/fixtures/reply-budgets.json"))) as! [[String: Any]]
        for item in cases {
            let budget = ReplyBudget.milliseconds(item["operation"] as! String, item: item["item"] as? String)
            XCTAssertEqual(budget, (item["milliseconds"] as! NSNumber).int64Value)
            if let observed = item["observedPreparationMs"] as? NSNumber {
                XCTAssertGreaterThan(observed.int64Value, 12000)
                XCTAssertGreaterThan(budget, observed.int64Value)
                XCTAssertGreaterThan(budget, 20000 + 3500)
            }
        }
    }
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
    func testLargeReplyMayProgressForMoreThanTenSeconds() throws {
        var time: TimeInterval = 0
        let a = Reassembler(now: { time }), bytes = Data((0..<(44 * 1024)).map { UInt8($0 % 251) })
        let frames = try Wire.frames(bytes, capacity: 512, id: 9)
        for (index, frame) in frames.enumerated() {
            time = Double(index) * 0.5
            if index == frames.count - 1 { XCTAssertEqual(try a.accept(frame), bytes) }
            else { XCTAssertNil(try a.accept(frame)) }
        }
        XCTAssertGreaterThan(time, 10); XCTAssertLessThan(time, 60)
    }
    func testOtherMessagesCannotExtendIdlePartial() throws {
        var time: TimeInterval = 0
        let a = Reassembler(now: { time }), frames = try Wire.frames(Data(repeating: 1, count: 40), capacity: 20, id: 8)
        _ = try a.accept(frames[0]); time = 9.999
        XCTAssertEqual(try a.accept(try Wire.frames(Data([1]), capacity: 20, id: 9)[0]), Data([1]))
        time = 10; XCTAssertThrowsError(try a.accept(frames[1]))
    }
    func testTotalLifetimeAndClockRollbackFailDespiteProgress() throws {
        var time: TimeInterval = 0
        let a = Reassembler(now: { time }), frames = try Wire.frames(Data(repeating: 1, count: 120), capacity: 20, id: 7)
        for index in 0...6 { time = Double(index) * 9; XCTAssertNil(try a.accept(frames[index])) }
        time = 60; XCTAssertThrowsError(try a.accept(frames[7]))
        _ = try a.accept(frames[0]); time = 59.999; XCTAssertThrowsError(try a.accept(frames[1]))
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
