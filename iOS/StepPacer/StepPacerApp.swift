import SwiftUI
import UIKit
import UserNotifications

@main
struct StepPacerApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @State private var model = StepDashboardModel()

    var body: some Scene {
        WindowGroup {
            DashboardView(model: model)
                .task { await model.onAppear() }
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .active:
                Task { await model.refresh() }
                model.startLiveUpdates()
            case .background:
                model.stopLiveUpdates()
                BackgroundRefreshService.schedule()
            default:
                break
            }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Registration has to happen before launch finishes, so it lives here rather
        // than in a `.task` modifier.
        BackgroundRefreshService.register()
        BackgroundRefreshService.schedule()

        // Ask HealthKit to wake us when new step data arrives, then run the same
        // evaluation the foreground uses.
        HealthKitService.shared.startBackgroundDelivery {
            _ = await MilestoneCoordinator.shared.evaluate()
        }

        UNUserNotificationCenter.current().delegate = ForegroundNotificationPresenter.shared
        return true
    }
}

/// Shows milestone banners even when the user is staring at the app.
final class ForegroundNotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = ForegroundNotificationPresenter()

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }
}
