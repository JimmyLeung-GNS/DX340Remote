import SwiftUI

@main struct DXRemoteApp: App {
    @State private var connection = RemoteConnection()
    @Environment(\.scenePhase) private var phase
    var body: some Scene {
        WindowGroup {
            RemoteView(connection: connection)
                .onChange(of: phase) { _, phase in if phase == .active { connection.refreshOnForeground() } }
        }
    }
}
