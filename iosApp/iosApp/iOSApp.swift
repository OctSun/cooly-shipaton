import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Register the native Google Map factory (no-op when the SDK/key is absent).
        GoogleMapsSetup.activateIfConfigured()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
