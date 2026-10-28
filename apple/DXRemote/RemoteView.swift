import SwiftUI
import UIKit

struct RemoteView: View {
    let connection: RemoteConnection
    private enum Destination: String { case player, library, queue, setup }
    @State private var destination = Destination.player
    init(connection: RemoteConnection) {
        self.connection = connection
        #if DEBUG
        // Actual native-screen captures only; does not simulate a connection or source.
        if let value = ProcessInfo.processInfo.arguments.first(where: { $0.hasPrefix("--capture-tab=") }),
           let requested = Destination(rawValue: String(value.dropFirst("--capture-tab=".count))) {
            _destination = State(initialValue: requested)
        }
        #endif
    }
    var body: some View {
        TabView(selection: $destination) {
            NavigationStack { PlayerView(connection: connection).navigationTitle("DX340") }.tabItem { Label("Player", systemImage: "play.circle") }.tag(Destination.player)
            NavigationStack { LibraryView(connection: connection, parent: nil, openSetup: { destination = .setup }).navigationTitle("Library") }.id(connection.sourceScope).tabItem { Label("Library", systemImage: "music.note.list") }.tag(Destination.library)
            NavigationStack { QueueView(connection: connection, openSetup: { destination = .setup }).navigationTitle("Queue") }.tabItem { Label("Queue", systemImage: "list.bullet") }.tag(Destination.queue)
            NavigationStack { SetupView(connection: connection).navigationTitle("Setup") }.tabItem { Label("Setup", systemImage: "antenna.radiowaves.left.and.right") }.tag(Destination.setup)
        }
    }
}

private struct ConnectionStatusSection: View {
    let connection: RemoteConnection
    var body: some View {
        Section {
            Text(connection.status).font(.footnote).fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.updatesFrequently)
        }
    }
}

private struct PlayerView: View {
    let connection: RemoteConnection
    var body: some View {
        List {
            ConnectionStatusSection(connection: connection)
            Section {
                if let data = connection.artwork, let image = UIImage(data: data) { Image(uiImage: image).resizable().scaledToFit().frame(maxHeight: 240).accessibilityLabel("Album artwork") }
                Text(connection.title).font(.title2).bold()
                if !connection.artist.isEmpty { Text(connection.artist).foregroundStyle(.secondary) }
                if let album = connection.state["album"] as? String, !album.isEmpty { Text(album).foregroundStyle(.secondary) }
            }
            Section("Playback") {
                HStack(spacing: 20) {
                    action("Previous", icon: "backward.end.fill", capability: "canPrevious") { connection.send("PREVIOUS") }
                    action("Play", icon: "play.fill", capability: "canPlay") { connection.send("SET_PLAYING", args: ["playing": true]) }
                    action("Pause", icon: "pause.fill", capability: "canPause") { connection.send("SET_PLAYING", args: ["playing": false]) }
                    action("Next", icon: "forward.end.fill", capability: "canNext") { connection.send("NEXT") }
                }.buttonStyle(.borderless)
                Text("\((connection.state["position"] as? Int ?? 0) / 1000)s / \((connection.state["duration"] as? Int ?? 0) / 1000)s")
                Button("Back 10 seconds") { connection.send("SEEK", args: ["position": max(0, (connection.state["position"] as? Int ?? 0) - 10000)]) }.disabled(!connection.can("canSeek"))
                Text("Volume control awaits physical output verification.").foregroundStyle(.secondary)
            }
            if !connection.sources.isEmpty { Section("Source") {
                ForEach(connection.sources.indices, id: \.self) { i in
                    let source = connection.sources[i]
                    Button(source["name"] as? String ?? "Source") { connection.send("SELECT_SOURCE", args: ["source": source["id"] ?? ""]) }.disabled(!connection.authorized)
                }
            } }
        }.refreshable { connection.send("GET_STATE"); connection.send("LIST_SOURCES") }
    }
    private func action(_ label: String, icon: String, capability: String, command: @escaping () -> Void) -> some View {
        Button(action: command) { Image(systemName: icon).frame(minWidth: 44, minHeight: 44) }.accessibilityLabel(label).disabled(!connection.can(capability))
    }
}

private struct LibraryView: View {
    let connection: RemoteConnection
    let parent: String?
    let openSetup: () -> Void
    var body: some View {
        List {
            ConnectionStatusSection(connection: connection)
            if !connection.authorized {
                Text("Connect to DX340 in Setup to browse its library.")
                Button("Open Setup", action: openSetup)
            }
            else if connection.assistedBrowsing { Text((connection.state["capabilities"] as? [String: Any])?["reason"] as? String ?? "Apple Music needs DX340 awake and unlocked.").foregroundStyle(.secondary) }
            else if !connection.can("canBrowse") { Text("This source has not exposed a usable browse connection. Open it on DX340 and refresh.").foregroundStyle(.secondary) }
            Button("Refresh library") { connection.browse(parent: parent) }.disabled(!connection.can("canBrowse"))
            if connection.assistedBrowsing, let back = connection.library["backId"] as? String { Button("Back") { connection.browse(parent: back) }.disabled(!connection.can("canBrowse") || !connection.assistedReady) }
            if connection.library["loading"] as? Bool == true { ProgressView("Loading library") }
            let matches = (connection.assistedBrowsing || connection.library["requestedParent"] as? String == (parent ?? "")) && connection.library["scopeSource"] as? String == connection.state["source"] as? String && connection.library["scopeEpoch"] as? String == connection.state["sourceEpoch"] as? String
            let items = matches ? (connection.library["items"] as? [[String: Any]] ?? []) : []
            ForEach(items.indices, id: \.self) { i in
                let item = items[i], id = item["id"] as? String ?? ""
                let uiUnavailable = (connection.assistedBrowsing || item["uiAssisted"] as? Bool == true) && !connection.assistedReady
                if item["browsable"] as? Bool == true {
                    if connection.assistedBrowsing { Button(item["title"] as? String ?? "Folder") { connection.browse(parent: id) }.disabled(!connection.can("canBrowse") || uiUnavailable) }
                    else { NavigationLink(item["title"] as? String ?? "Folder") { LibraryView(connection: connection, parent: id, openSetup: openSetup).navigationTitle(item["title"] as? String ?? "Library") }.disabled(!connection.can("canBrowse") || uiUnavailable) }
                } else {
                    Button(item["title"] as? String ?? "Untitled") { connection.send("PLAY_ITEM", args: ["item": id, "revision": connection.library["revision"] ?? ""]) }
                        .disabled(!connection.can("canPlayItem") || item["playable"] as? Bool != true || uiUnavailable)
                }
            }
            if matches, let page = connection.library["nextPage"] as? Int { Button("Next page") { connection.browse(parent: connection.assistedBrowsing ? connection.library["parent"] as? String : parent, page: page) }.disabled(!connection.can("canBrowse") || (connection.assistedBrowsing && !connection.assistedReady)) }
        }
        .task(id: parent) { if connection.authorized { connection.browse(parent: parent) } }
        .safeAreaInset(edge: .bottom) { Text(connection.title).font(.subheadline).padding().frame(maxWidth: .infinity).background(.bar) }
    }
}

private struct QueueView: View {
    let connection: RemoteConnection
    let openSetup: () -> Void
    var body: some View {
        List {
            ConnectionStatusSection(connection: connection)
            Button("Refresh queue") { connection.readQueue() }.disabled(!connection.can("canReadQueue") || connection.queueLoading)
            if connection.queueLoading { Text("Loading queue…") }
            if let offset = connection.queuePage["offset"] as? Int, let total = connection.queuePage["total"] as? Int, !connection.queue.isEmpty {
                Text("Queue items \(offset + 1)–\(offset + connection.queue.count) of \(total)")
            }
            if !connection.authorized {
                Text("Connect to DX340 in Setup to view its queue.")
                Button("Open Setup", action: openSetup)
            }
            else if !connection.can("canReadQueue") { Text("This source has not exposed a readable queue.") }
            ForEach(connection.queue.indices, id: \.self) { i in
                let item = connection.queue[i]
                let revision = connection.queueRevision
                Button(item["title"] as? String ?? "Untitled") { connection.send("PLAY_QUEUE_ITEM", args: ["item": item["id"] ?? "", "revision": revision]) }.disabled(!connection.can("canPlayQueueItem") || connection.queueLoading)
            }
            if (connection.queuePage["offset"] as? Int ?? 0) > 0 {
                Button("Start of queue") { connection.readQueue() }.disabled(!connection.can("canReadQueue") || connection.queueLoading)
            }
            if let next = connection.queuePage["nextOffset"] as? Int {
                Button("Next queue page") { connection.readQueue(offset: next) }.disabled(!connection.can("canReadQueue") || connection.queueLoading)
            }
        }
    }
}

private struct SetupView: View {
    let connection: RemoteConnection
    var body: some View {
        List {
            ConnectionStatusSection(connection: connection)
            Section { Text("Music stays on DX340. Open pairing on the companion, accept system pairing, then approve this controller on DX340.") }
            Section("Connect") {
                Button("Find DX340") { connection.scan() }
                ForEach(connection.devices, id: \.identifier) { device in Button(device.name ?? "DX340") { connection.connect(device) } }
                Button("Retry authorization after approval") { connection.authorize() }
                Button("Disconnect") { connection.disconnect() }
            }
            Section("Capabilities") { Text("Mango and Apple Music expose different controls. Browsing is available only through a source interface that accepts this companion.") }
        }
    }
}
