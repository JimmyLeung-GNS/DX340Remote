// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "DXProtocol", platforms: [.macOS(.v13), .iOS(.v17), .watchOS(.v10)], products: [.library(name: "DXProtocol", targets: ["DXProtocol"])], targets: [
    .target(name: "DXProtocol", path: "ProtocolModels"),
    .testTarget(name: "DXProtocolTests", dependencies: ["DXProtocol"], path: "Tests")
])
