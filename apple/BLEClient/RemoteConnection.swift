import Foundation
import CoreBluetooth
import Observation
import CryptoKit

@MainActor @Observable
// CoreBluetooth delivers these delegates on the explicitly configured .main queue.
// Check that legacy protocol assumption at runtime rather than ignoring actor isolation.
final class RemoteConnection: NSObject, @preconcurrency CBCentralManagerDelegate, @preconcurrency CBPeripheralDelegate {
    var status = "Find your DX340 to connect directly over Bluetooth."
    var devices: [CBPeripheral] = []
    var authorized = false
    private var controlProposal = ControlProposal()
    var canRequestControl: Bool { !authorized && controlProposal.available && !pending.values.contains("REQUEST_CONTROL") }
    var state: [String: Any] = [:]
    var stateSampleId = ""
    private var stateReceivedAt: ContinuousClock.Instant?
    var stateAgeMs: Int64? {
        guard let received = stateReceivedAt else { return nil }
        let age = received.duration(to: ContinuousClock.now).components
        return max(0, age.seconds * 1_000 + age.attoseconds / 1_000_000_000_000_000)
    }
    var sources: [[String: Any]] = []
    var library: [String: Any] = [:]
    var queue: [[String: Any]] = []
    var queueRevision = ""
    var queuePage: [String: Any] = [:]
    var queueLoading: Bool { activeQueueId.map { pending[$0] != nil } ?? false }
    var artwork: Data?
    private var hostEpoch = ""
    private var manager: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var rx: CBCharacteristic?
    private var tx: CBCharacteristic?
    private var serial: UInt16 = 0
    private var writes: [Data] = []
    private var writing = false
    private var wantsConnection = false
    private var reconnectAttempts = 0
    private var connectionTimeout: Task<Void, Never>?
    private var connectionGeneration = 0
    private var connectionPhase = "idle"
    private var servicesInvalidated = false
    private var pendingQuickConnect = false
    private var discoveryGeneration = 0
    private var discoveryTimeout: Task<Void, Never>?
    private var approvalRetry = ApprovalRetry()
    private var approvalCheck: Task<Void, Never>?
    private var setupDeadline = ConnectionSetupDeadline()
    private var assistedPageUse = AssistedPageUse()
    private var setupTimeout: Task<Void, Never>?
    private let setupClockOrigin = ContinuousClock.now
    private var setupExpiryNotice: String?
    private var setupNowMs: Int64 {
        let elapsed = setupClockOrigin.duration(to: ContinuousClock.now).components
        return elapsed.seconds * 1000 + elapsed.attoseconds / 1_000_000_000_000_000
    }
    private var writeTimeout: Task<Void, Never>?
    private var pending: [String: String] = [:]
    private let mutations: Set<String> = ["SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM", "ADJUST_VOLUME"]
    var mutationPending: Bool { pending.values.contains { mutations.contains($0) } }
    private struct BrowseLocation { let source: String; let epoch: String; let parent: String? }
    private var browseLocations: [String: BrowseLocation] = [:]
    private var activeBrowseId: String?
    private struct QueueLocation { let source: String; let epoch: String; let offset: Int; let revision: String? }
    private var queueLocations: [String: QueueLocation] = [:]
    private var activeQueueId: String?
    var assistedBrowsing: Bool { library["uiAssisted"] as? Bool == true || (state["capabilities"] as? [String: Any])?["browseMode"] as? String == "ui-assisted" }
    private func isAssistedReady(_ snapshot: [String: Any]) -> Bool {
        let caps = snapshot["capabilities"] as? [String: Any] ?? [:]
        return caps[caps["browseMode"] as? String == "ui-assisted" ? "canBrowse" : "canBrowseFolders"] as? Bool == true
    }
    var assistedReady: Bool { authorized && isAssistedReady(state) }
    var sourceScope: String { (state["source"] as? String ?? "") + ":" + (state["sourceEpoch"] as? String ?? "") + ":" + ((state["capabilities"] as? [String: Any])?["browseMode"] as? String ?? "direct") }
    private var assembler = Reassembler()
    override init() {
        super.init()
        if let bytes = try? Data(contentsOf: snapshotURL()), bytes.count <= 262144, let saved = try? JSONSerialization.jsonObject(with: bytes) as? [String: Any] {
            state = saved["state"] as? [String: Any] ?? [:]; sources = saved["sources"] as? [[String: Any]] ?? []; library = saved["library"] as? [String: Any] ?? [:]
            status = "Cached metadata is stale. Connect to DX340 before controlling playback."
        }
        manager = CBCentralManager(delegate: self, queue: .main, options: [CBCentralManagerOptionRestoreIdentifierKey: "org.dx340.remote.central"])
    }
    var title: String { state["title"] as? String ?? "No track connected" }
    var artist: String { state["artist"] as? String ?? "" }
    func can(_ name: String) -> Bool { authorized && (state["capabilities"] as? [String: Any])?[name] as? Bool == true }
    var connectionSetup: [String: Any] {
        let phase: String
        if CBManager.authorization == .denied || CBManager.authorization == .restricted { phase = "permission" }
        else if manager?.state == .poweredOff { phase = "bluetoothOff" }
        else if authorized { phase = "connected" }
        else if canRequestControl { phase = "busy" }
        else { phase = connectionPhase }
        return ["phase": phase, "savedName": UserDefaults.standard.string(forKey: "approvedPlayerName") ?? "Player",
                "hasSavedPlayer": UserDefaults.standard.string(forKey: "approvedPlayerId") != nil]
    }
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if central.state == .poweredOn {
            if !authorized && ["bluetoothOff", "permission", "preparing"].contains(connectionPhase) { connectionPhase = "idle" }
            if pendingQuickConnect { pendingQuickConnect = false; connectPlayer(); return }
            status = "Bluetooth ready. Connect to your player."
            if wantsConnection, let peripheral, peripheral.state == .disconnected { beginConnection(peripheral) }
        } else {
            clearConnection()
            if central.state == .unauthorized { pendingQuickConnect = false; connectionPhase = "permission"; status = "Allow Bluetooth access in iPhone Settings." }
            else if central.state == .poweredOff { pendingQuickConnect = false; connectionPhase = "bluetoothOff"; status = "Turn on Bluetooth to connect." }
            else if central.state == .unsupported { pendingQuickConnect = false; wantsConnection = false; connectionPhase = "failed"; status = "Bluetooth is unavailable on this device." }
            else { connectionPhase = "preparing"; status = "Preparing Bluetooth…" }
        }
    }
    private func stopDiscovery() {
        discoveryGeneration += 1; discoveryTimeout?.cancel(); discoveryTimeout = nil; manager?.stopScan()
    }
    func scan() { discoverPlayers(automatic: false) }
    func searchPlayers() { disconnect(); discoverPlayers(automatic: false) }
    func connectPlayer() {
        guard !authorized, !["searching", "connecting", "approval"].contains(connectionPhase) else { return }
        guard manager.state == .poweredOn else {
            pendingQuickConnect = manager.state == .unknown || manager.state == .resetting
            connectionPhase = pendingQuickConnect ? "preparing" : (manager.state == .unauthorized ? "permission" : (manager.state == .unsupported ? "failed" : "bluetoothOff"))
            status = manager.state == .unsupported ? "Bluetooth is unavailable on this device." : "Allow Bluetooth access and turn on Bluetooth to connect."; return
        }
        disconnect()
        if let text = UserDefaults.standard.string(forKey: "approvedPlayerId"), let id = UUID(uuidString: text),
           let device = manager.retrievePeripherals(withIdentifiers: [id]).first {
            devices = [device]; connect(device)
        } else { discoverPlayers(automatic: true) }
    }
    private func discoverPlayers(automatic: Bool) {
        stopDiscovery(); cancelApprovalCheck()
        guard manager.state == .poweredOn else { status = "Enable Bluetooth and allow access to connect."; return }
        let generation = discoveryGeneration
        devices = []; connectionPhase = "searching"; status = "Searching for compatible players…"
        manager.scanForPeripherals(withServices: [CBUUID(string: Wire.service)], options: nil)
        discoveryTimeout = Task {
            try? await Task.sleep(for: .seconds(10))
            guard !Task.isCancelled, discoveryGeneration == generation else { return }
            manager.stopScan(); discoveryTimeout = nil
            let choice = PlayerDiscovery.finish(found: devices.map { $0.identifier.uuidString }, remembered: UserDefaults.standard.string(forKey: "approvedPlayerId"))
            if automatic, let target = choice.target, let device = devices.first(where: { $0.identifier.uuidString == target }) { connect(device) }
            else {
                connectionPhase = devices.isEmpty ? "failed" : "choosing"
                status = devices.isEmpty ? "No compatible player found. Start Companion on your player, keep it nearby, then try again." : "Choose the player you want to control."
            }
        }
    }
    private func rememberApprovedPlayer() {
        guard let peripheral else { return }
        let name = (peripheral.name ?? "Player").unicodeScalars.filter { !CharacterSet.controlCharacters.contains($0) && !(0x202A...0x202E).contains($0.value) && !(0x2066...0x2069).contains($0.value) }
        UserDefaults.standard.set(peripheral.identifier.uuidString, forKey: "approvedPlayerId")
        let label = String(String.UnicodeScalarView(name)).trimmingCharacters(in: .whitespacesAndNewlines)
        UserDefaults.standard.set(label.isEmpty ? "Player" : String(label.prefix(60)), forKey: "approvedPlayerName")
    }
    private func cancelApprovalCheck() {
        approvalRetry.clear(); approvalCheck?.cancel(); approvalCheck = nil
    }
    private func awaitLocalApproval() {
        let ticket = approvalRetry.begin()
        guard approvalCheck == nil else { return }
        approvalCheck = Task {
            while !Task.isCancelled && approvalRetry.current(ticket) {
                try? await Task.sleep(for: .milliseconds(1500), clock: .continuous)
                guard !Task.isCancelled, approvalRetry.current(ticket) else { return }
                if approvalRetry.mayCheck(ticket: ticket, setupActive: setupDeadline.active, authorized: authorized, requestPending: pending.values.contains("AUTHORIZE")) { authorize() }
            }
        }
    }
    func connect(_ device: CBPeripheral) {
        stopDiscovery(); cancelApprovalCheck(); pendingQuickConnect = false
        if let previous = peripheral, previous.identifier != device.identifier { manager.cancelPeripheralConnection(previous) }
        clearConnection(); peripheral = device; device.delegate = self; wantsConnection = true; reconnectAttempts = 0; status = "Connecting…"
        beginConnection(device)
    }
    func disconnect() {
        pendingQuickConnect = false; stopDiscovery(); cancelApprovalCheck(); wantsConnection = false
        if let p = peripheral { manager.cancelPeripheralConnection(p) }
        clearConnection(); status = "Disconnected. Cached lists may be stale."
    }
    private func clearConnection() { stopDiscovery(); cancelApprovalCheck(); servicesInvalidated = false; connectionPhase = "idle"; abandonAssistedPage(); cancelSetupDeadline(); setupExpiryNotice = nil; connectionGeneration += 1; connectionTimeout?.cancel(); connectionTimeout = nil; writeTimeout?.cancel(); writeTimeout = nil; authorized = false; controlProposal.clear(); assistedPageUse.clear(); rx = nil; tx = nil; writing = false; writes = []; pending = [:]; browseLocations = [:]; queueLocations = [:]; activeBrowseId = nil; activeQueueId = nil; hostEpoch = ""; assembler.clear() }
    private func cancelSetupDeadline() { setupTimeout?.cancel(); setupTimeout = nil; setupDeadline.clear() }
    private func beginSetupDeadline() {
        guard !setupDeadline.active else { return }
        let ticket = setupDeadline.begin(at: setupNowMs)
        setupTimeout = Task {
            try? await Task.sleep(until: .now + .milliseconds(ConnectionSetupDeadline.timeoutMs), clock: .continuous)
            guard !Task.isCancelled else { return }
            expireSetupIfNeeded(ticket: ticket)
        }
    }
    @discardableResult private func expireSetupIfNeeded(ticket: UInt64? = nil) -> Bool {
        guard !authorized, setupDeadline.expired(at: setupNowMs, ticket: ticket) else { return false }
        disconnect(); connectionPhase = "failed"; setupExpiryNotice = ConnectionSetupDeadline.message; status = ConnectionSetupDeadline.message
        return true
    }
    private func beginConnection(_ device: CBPeripheral) {
        guard wantsConnection, manager.state == .poweredOn else { return }
        connectionPhase = "connecting"; status = "Connecting…"; armConnectionTimeout(device)
        if device.state == .connected { device.discoverServices([CBUUID(string: Wire.service)]) }
        else if device.state != .disconnecting { manager.connect(device) }
    }
    private func armConnectionTimeout(_ device: CBPeripheral) {
        connectionTimeout?.cancel(); connectionGeneration += 1
        let generation = connectionGeneration
        connectionTimeout = Task {
            try? await Task.sleep(for: .seconds(30))
            guard !Task.isCancelled, connectionGeneration == generation, self.peripheral == device, tx?.isNotifying != true else { return }
            clearConnection(); connectionPhase = "failed"; status = "Connection setup timed out. Try connecting again."
            manager.cancelPeripheralConnection(device)
        }
    }
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String: Any], rssi RSSI: NSNumber) {
        guard manager.isScanning, connectionPhase == "searching" else { return }
        if !devices.contains(where: { $0.identifier == peripheral.identifier }) { devices.append(peripheral) }
    }
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        guard peripheral == self.peripheral, !servicesInvalidated else { return }
        status = "Discovering DX340 service…"; peripheral.delegate = self; peripheral.discoverServices([CBUUID(string: Wire.service)])
        // Remember only after the host confirms authorization, not merely a radio connection.
    }
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        guard peripheral == self.peripheral else { return }
        clearConnection(); connectionPhase = "failed"; status = "Connection failed. Try connecting again."
    }
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        guard peripheral == self.peripheral else { return }
        let expiryNotice = setupExpiryNotice
        clearConnection(); status = expiryNotice ?? (wantsConnection ? "Disconnected; pending commands discarded. Reconnecting…" : "Disconnected. Cached lists may be stale.")
        if expiryNotice != nil { connectionPhase = "failed" }
        if wantsConnection && central.state == .poweredOn && reconnectAttempts < 5 {
            connectionPhase = "connecting"
            let delay = min(15, 1 << reconnectAttempts); reconnectAttempts += 1
            let generation = connectionGeneration
            Task { try? await Task.sleep(for: .seconds(delay)); if connectionGeneration == generation && wantsConnection && self.peripheral == peripheral && peripheral.state == .disconnected { beginConnection(peripheral) } }
        }
    }
    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String: Any]) {
        guard let restored = (dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral])?.first else { return }
        peripheral = restored; restored.delegate = self; wantsConnection = true
        clearConnection()
        if restored.state == .connected { connectionPhase = "connecting"; armConnectionTimeout(restored); restored.discoverServices([CBUUID(string: Wire.service)]) }
    }
    func refreshOnForeground() {
        if expireSetupIfNeeded() { return }
        guard let p = peripheral, p.state == .connected, rx != nil else { return }
        if authorized { send("GET_STATE") }
        else if connectionPhase != "approval" && connectionPhase != "busy" && !pending.values.contains(where: { $0 == "HELLO" || $0 == "AUTHORIZE" }) { send("HELLO", bootstrap: true) }
    }
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        guard peripheral == self.peripheral, peripheral.state == .connected, !servicesInvalidated,
              invalidatedServices.contains(where: { $0.uuid == CBUUID(string: Wire.service) }) else { return }
        // Invalidate handles, authorization and queued mutations immediately.
        // Cancellation uses the existing bounded reconnect/discovery path.
        clearConnection(); servicesInvalidated = true
        connectionPhase = wantsConnection ? "connecting" : "idle"
        status = "Player service changed. Refreshing the Bluetooth connection."
        if wantsConnection { armConnectionTimeout(peripheral) }
        manager.cancelPeripheralConnection(peripheral)
    }
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard peripheral == self.peripheral, peripheral.state == .connected, !servicesInvalidated else { return }
        guard error == nil, let service = peripheral.services?.first(where: { $0.uuid == CBUUID(string: Wire.service) }) else { status = "DX340 service unavailable."; return }
        peripheral.discoverCharacteristics([CBUUID(string: Wire.rx), CBUUID(string: Wire.tx)], for: service)
    }
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard peripheral == self.peripheral, peripheral.state == .connected, !servicesInvalidated else { return }
        guard error == nil else { status = "Service discovery failed."; return }
        rx = service.characteristics?.first(where: { $0.uuid == CBUUID(string: Wire.rx) })
        tx = service.characteristics?.first(where: { $0.uuid == CBUUID(string: Wire.tx) })
        guard rx != nil, let tx else { status = "Missing DX340 message characteristics."; return }
        // The encrypted CCC write invokes system BLE pairing, including MITM authentication on the host.
        peripheral.setNotifyValue(true, for: tx)
    }
    func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
        guard peripheral == self.peripheral, characteristic == tx else { return }
        guard error == nil, characteristic.isNotifying else { status = "Pairing failed. Open pairing on DX340, accept the system prompt, then retry."; return }
        connectionTimeout?.cancel(); connectionTimeout = nil; reconnectAttempts = 0
        beginSetupDeadline()
        send("HELLO", bootstrap: true)
    }
    func send(_ type: String, args: [String: Any] = [:], bootstrap: Bool = false) {
        if expireSetupIfNeeded() { return }
        guard let p = peripheral, let _ = rx, p.state == .connected, bootstrap || authorized else { status = "Connect and authorize before controlling playback."; return }
        if library["invalidated"] as? Bool == true, type == "PLAY_ITEM" || (type == "BROWSE" && args["parent"] is String) {
            status = "This page expired after a song selection. Browse source library again."; return
        }
        guard pending.count < 8 else { status = "Wait for pending replies."; return }
        guard !mutations.contains(type) || !mutationPending else { status = "Wait for the current playback action to finish."; return }
        let id = UUID().uuidString
        let request: [String: Any] = ["v": 1, "id": id, "type": type, "hostEpoch": hostEpoch, "source": state["source"] ?? "", "sourceEpoch": state["sourceEpoch"] ?? "", "args": args]
        do {
            let data = try JSONSerialization.data(withJSONObject: request, options: [.sortedKeys])
            let frames = try Wire.frames(data, capacity: min(512, p.maximumWriteValueLength(for: .withResponse)), id: serial)
            guard writes.count + frames.count <= 12000 else { status = "Too many queued messages."; return }
            serial &+= 1; pending[id] = type
            if type == "PLAY_ITEM" {
                assistedPageUse.begin(id: id, source: state["source"] as? String ?? "", epoch: state["sourceEpoch"] as? String ?? "",
                                      revision: library["revision"] as? String ?? "",
                                      assisted: AssistedPageUse.tracks(source: state["source"] as? String ?? "", item: args["item"] as? String ?? "",
                                        assisted: assistedBrowsing, requestedRevision: args["revision"] as? String ?? "", pageRevision: library["revision"] as? String ?? ""))
            }
            if type == "GET_QUEUE" { queueLocations[id] = QueueLocation(source: state["source"] as? String ?? "", epoch: state["sourceEpoch"] as? String ?? "", offset: args["offset"] as? Int ?? 0, revision: args["revision"] as? String); activeQueueId = id }
            if type == "BROWSE" {
                browseLocations[id] = BrowseLocation(source: state["source"] as? String ?? "", epoch: state["sourceEpoch"] as? String ?? "", parent: args["parent"] as? String)
                activeBrowseId = id; library = ["loading": true]; status = "Loading library…"
            }
            writes.append(contentsOf: frames); pump()
            let replyBudget = ReplyBudget.milliseconds(type, item: args["item"] as? String)
            Task { try? await Task.sleep(for: .milliseconds(replyBudget)); if pending.removeValue(forKey: id) != nil { if type == "AUTHORIZE" { cancelApprovalCheck(); connectionPhase = "failed" }; retireAssistedPage(id); browseLocations[id] = nil; queueLocations[id] = nil; if id == activeBrowseId { library = [:] }; status = "Reply timed out; outcome unknown. Refresh before retrying." } }
        } catch { status = "Cannot encode Bluetooth message." }
    }
    private func pump() {
        guard !writing, !writes.isEmpty, let p = peripheral, let rx else { return }
        writing = true; p.writeValue(writes.removeFirst(), for: rx, type: .withResponse)
        writeTimeout?.cancel()
        writeTimeout = Task { try? await Task.sleep(for: .seconds(10)); guard !Task.isCancelled else { return }; if writing { manager.cancelPeripheralConnection(p); clearConnection(); status = "Bluetooth write timed out; outcome unknown. Pending controls cleared." } }
    }
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        guard peripheral == self.peripheral, characteristic == rx else { return }
        writeTimeout?.cancel(); writeTimeout = nil; writing = false
        if error != nil { clearConnection(); status = "Write failed; outcome unknown. Reconnect before retrying." }
        else { pump() }
    }
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard peripheral == self.peripheral, characteristic == tx else { return }
        guard error == nil, let bytes = characteristic.value else { status = "Bluetooth read failed."; return }
        do { if let value = try assembler.accept(bytes) { try receive(value) } }
        catch { disconnect(); status = "Invalid Bluetooth reply. Reconnect." }
    }
    func authorize() { send("AUTHORIZE", bootstrap: true) }
    func readQueue(offset: Int = 0) {
        send("GET_QUEUE", args: ["offset": offset, "revision": offset == 0 ? NSNull() : queueRevision as Any])
    }
    func requestControl() {
        guard canRequestControl, let revision = controlProposal.consume() else { return }
        status = "Requesting control on this phone…"
        send("REQUEST_CONTROL", args: ["ownershipRevision": revision], bootstrap: true)
    }
    func browse(parent: String? = nil, page: Int = 0) {
        send("BROWSE", args: ["parent": parent.map { $0 as Any } ?? NSNull(), "page": page, "revision": parent == nil ? NSNull() : (library["revision"] ?? state["sourceEpoch"] ?? "")])
    }
    private func abandonAssistedPage() {
        if assistedPageUse.abandon(source: state["source"] as? String ?? "", epoch: state["sourceEpoch"] as? String ?? "", revision: library["revision"] as? String ?? "") {
            library["invalidated"] = true
        }
    }
    private func retireAssistedPage(_ id: String) {
        if assistedPageUse.finish(id: id, source: state["source"] as? String ?? "", epoch: state["sourceEpoch"] as? String ?? "", revision: library["revision"] as? String ?? "") {
            library["invalidated"] = true
        }
    }
    private func receive(_ value: Data) throws {
        if expireSetupIfNeeded() { return }
        guard let reply = try JSONSerialization.jsonObject(with: value) as? [String: Any], reply["v"] as? Int == 1,
              let type = reply["type"] as? String, let data = reply["data"] as? [String: Any] else { throw WireError.malformed }
        let id = reply["id"] as? String ?? "", operation = pending[id], location = browseLocations[id]
        if type == "CONTROL_CHANGED" {
            if reply["hostEpoch"] as? String == hostEpoch, data["code"] as? String == "CONTROL_MOVED" {
                disconnect(); status = "Control moved to another approved controller. Music continues on DX340."
            }
            return
        }
        let route = HostReplyGate.classify(type: type, operation: operation, expectedHost: hostEpoch,
            replyHost: reply["hostEpoch"] as? String ?? "", authorized: authorized)
        if route == .ignore { return }
        if route == .reconnect {
            disconnect(); connectionPhase = "failed"; setupExpiryNotice = HostReplyGate.changedMessage
            status = HostReplyGate.changedMessage; return
        }
        let queueLocation = queueLocations[id]
        if type != "ACCEPTED" { queueLocations[id] = nil }
        if type != "ACCEPTED" { pending[id] = nil }
        if type != "ACCEPTED", reply["hostEpoch"] as? String == hostEpoch { retireAssistedPage(id) }
        if route == .hello { controlProposal.clear(); hostEpoch = reply["hostEpoch"] as? String ?? ""; authorize(); return }
        if type == "ERROR", reply["hostEpoch"] as? String == hostEpoch, operation == "AUTHORIZE", !authorized {
            if data["code"] as? String == "CONTROLLER_BUSY" {
                wantsConnection = false; cancelApprovalCheck(); connectionPhase = "busy"
                controlProposal.observeBusy(replyHost: reply["hostEpoch"] as? String ?? "", expectedHost: hostEpoch, value: data["ownershipRevision"] as? String ?? "")
            } else {
                controlProposal.clear()
                if data["code"] as? String == "LOCAL_APPROVAL_REQUIRED" { wantsConnection = false; connectionPhase = "approval"; awaitLocalApproval() }
                else { cancelApprovalCheck(); connectionPhase = "failed" }
            }
        }
        if type == "ERROR" { if reply["hostEpoch"] as? String == hostEpoch, let fresh = data["state"] as? [String: Any] { applyState(fresh, notifyLibraryChange: false) }; browseLocations[id] = nil; if id == activeBrowseId { library = [:] }; if id == activeQueueId { queue = []; queueRevision = ""; queuePage = [:] }; status = errorMessage(data["code"] as? String ?? "Source error"); return }
        if type == "ACCEPTED" { status = "Waiting for source…"; return }
        guard reply["hostEpoch"] as? String == hostEpoch else { disconnect(); return }
        if ["AUTHORIZE", "REQUEST_CONTROL"].contains(operation ?? ""), data["authorized"] as? Bool == true { controlProposal.clear(); authorized = true; cancelSetupDeadline(); cancelApprovalCheck(); rememberApprovedPlayer(); connectionPhase = "connected"; wantsConnection = true; status = "Connected to player" }
        if let new = data["state"] as? [String: Any] { applyState(new, notifyLibraryChange: !["AUTHORIZE", "REQUEST_CONTROL"].contains(operation ?? "")) }
        if type == "STATE_CHANGED" { applyState(data) }
        if let list = data["sources"] as? [[String: Any]] { sources = list }
        if operation == "BROWSE" {
            browseLocations[id] = nil
            guard id == activeBrowseId, let location, location.source == state["source"] as? String, location.epoch == state["sourceEpoch"] as? String else { return }
            library = data; library["requestedParent"] = location.parent ?? ""; library["scopeSource"] = location.source; library["scopeEpoch"] = location.epoch
            status = "Library loaded. Offline availability depends on the source."
        }
        if operation == "GET_QUEUE", id == activeQueueId, let location = queueLocation, location.source == state["source"] as? String, location.epoch == state["sourceEpoch"] as? String {
            guard let items = data["items"] as? [[String: Any]], let revision = data["revision"] as? String else { throw WireError.malformed }
            let offset = data["offset"] as? Int ?? 0, total = data["total"] as? Int ?? items.count
            let next = data["nextOffset"] as? Int
            guard !revision.isEmpty, location.revision == nil || location.revision == revision,
                  offset == location.offset, (0...10000).contains(offset), items.count <= 50,
                  total >= offset + items.count, total <= 10000,
                  next == nil ? offset + items.count == total : (!items.isEmpty && next == offset + items.count && next! < total) else { throw WireError.malformed }
            queue = items; queueRevision = revision
            queuePage = ["offset": offset, "total": total, "nextOffset": next as Any? ?? NSNull()]
            status = "Queue loaded."
        }
        if operation == "GET_ARTWORK", let key = data["key"] as? String, let encoded = data["base64"] as? String, let bytes = Data(base64Encoded: encoded), bytes.count <= 49152 {
            let digest = SHA256.hash(data: bytes).map { String(format: "%02x", $0) }.joined()
            guard digest == key else { throw WireError.malformed }
            try? bytes.write(to: artworkURL(key), options: .atomic)
            if state["artworkKey"] as? String == key { artwork = bytes }
            trimArtworkCache()
        }
        if operation != "GET_ARTWORK", let key = state["artworkKey"] as? String, key.range(of: "^[a-f0-9]{64}$", options: .regularExpression) != nil {
            if let cached = try? Data(contentsOf: artworkURL(key)) { artwork = cached }
            else {
                artwork = nil
                Task { try? await Task.sleep(for: .milliseconds(300)); if authorized && pending.isEmpty && state["artworkKey"] as? String == key { send("GET_ARTWORK", args: ["key": key]) } }
            }
        } else if state["artworkKey"] is NSNull { artwork = nil }
        if data["outcome"] as? String == "dispatched" { status = "Sent to player; waiting for observed playback."; send("GET_STATE") }
        if data["outcome"] as? String == "observed" { status = "Playback state confirmed by source." }
        if let snapshot = try? JSONSerialization.data(withJSONObject: ["state": state, "sources": sources, "library": library]), snapshot.count <= 262144 { try? snapshot.write(to: snapshotURL(), options: .atomic) }
    }
    private func applyState(_ new: [String: Any], notifyLibraryChange: Bool = true) {
        func context(_ value: [String: Any]) -> BrowseContext {
            BrowseContext(source: value["source"] as? String ?? "", epoch: value["sourceEpoch"] as? String ?? "",
                          mode: (value["capabilities"] as? [String: Any])?["browseMode"] as? String ?? "", assistedReady: isAssistedReady(value))
        }
        let transition = BrowseContextTransition.evaluate(context(state), context(new),
            hasLibrary: !library.isEmpty || activeBrowseId != nil,
            hasQueue: !queue.isEmpty || !queuePage.isEmpty || !queueRevision.isEmpty || activeQueueId != nil,
            assistedLibrary: assistedBrowsing || activeBrowseId.flatMap({ browseLocations[$0]?.parent })?.hasPrefix("mango-ui:") == true,
            notify: notifyLibraryChange && authorized)
        if transition.reset == .all {
            library = [:]; queue = []; queueRevision = ""; queuePage = [:]; artwork = nil; activeBrowseId = nil; activeQueueId = nil; browseLocations = [:]; queueLocations = [:]
        } else if transition.reset == .assisted {
            library = [:]; activeBrowseId = nil; browseLocations = [:]
        }
        if let message = transition.message { status = message }
        state = new
        stateReceivedAt = ContinuousClock.now
        stateSampleId = UUID().uuidString
    }
    private func errorMessage(_ code: String) -> String {
        switch code {
        case "CONTROLLER_BUSY": return "Another approved controller has control. Open Setup to take control on this phone."
        case "STALE_CONTROL", "CONTROL_REQUEST_USED": return "Control changed. Retry authorization to check who has control."
        case "STALE_VOLUME": return "Volume changed on DX340. Refresh before choosing another step."
        case "VOLUME_AT_BOUNDARY": return "This is the edge of the locally confirmed range. Recheck volume on DX340 to use another range."
        case "VOLUME_PROFILE_CHANGED", "VOLUME_VERIFICATION_REQUIRED", "VOLUME_SOURCE_NOT_READY": return "Play the selected source and verify its output volume in the DX340 companion."
        case "UI_PERMISSION_REQUIRED": return "On DX340, enable the selected player's optional browsing service in Accessibility settings."
        case "UI_SCREEN_UNLOCK_REQUIRED": return "Wake and unlock DX340 for assisted browsing."
        case "UI_FOREGROUND_REQUIRED", "UI_SETUP_REQUIRED", "UI_UNEXPECTED_SCREEN", "UI_STORAGE_CONTEXT_REQUIRED": return "Open the selected player's local/downloaded library on DX340, then refresh."
        case "UI_VERSION_UNVERIFIED": return "The selected player changed. Assisted browsing needs revalidation."
        case "UI_ACTIVE_PAGE_UNVERIFIED", "UI_TREE_CHANGED", "UI_ACTION_TIMEOUT", "UI_ACTION_INTERRUPTED": return "The player's screen changed or did not settle. Refresh the library before retrying."
        case "UI_MODE_DISABLED": return "On DX340, enable the selected player's optional browsing mode."
        case "STALE_LIBRARY", "EXPIRED_ITEM": return "The library changed. Refresh before selecting a track."
        case "UI_BUSY", "SOURCE_BUSY": return "Wait for the current action to finish."
        case "LOCAL_APPROVAL_REQUIRED": return "On the player, open pairing and approve this phone. Connection will finish automatically."
        default: return "Action failed (\(code)). Refresh the player to check its state."
        }
    }
    private func snapshotURL() -> URL { FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("dx340-metadata.json") }
    private func artworkURL(_ key: String) -> URL {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("DX340Artwork")
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent(key).appendingPathExtension("jpg")
    }
    private func trimArtworkCache() {
        let directory = artworkURL("unused").deletingLastPathComponent()
        let files = (try? FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: [.contentModificationDateKey])) ?? []
        let sorted = files.sorted { ((try? $0.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast) > ((try? $1.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate) ?? .distantPast) }
        for file in sorted.dropFirst(100) { try? FileManager.default.removeItem(at: file) }
    }
}
