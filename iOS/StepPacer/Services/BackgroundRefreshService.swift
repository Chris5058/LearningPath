import Foundation
import BackgroundTasks

/// Supplements HealthKit's background delivery with periodic `BGAppRefreshTask` wake-ups.
///
/// Neither mechanism is guaranteed on its own — iOS decides when to run background work
/// based on battery, usage patterns and Low Power Mode — so the app registers for both
/// and lets the ledger sort out duplicates.
enum BackgroundRefreshService {

    static let taskIdentifier = "com.steppacer.refresh"

    /// Must be called before the app finishes launching.
    static func register() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: taskIdentifier,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            handle(refreshTask)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 30 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("[StepPacer] could not schedule refresh: \(error.localizedDescription)")
        }
    }

    private static func handle(_ task: BGAppRefreshTask) {
        // Always queue the next one first — if this run is cut short we still get another.
        schedule()

        let work = Task {
            await MilestoneCoordinator.shared.evaluate()
            task.setTaskCompleted(success: true)
        }

        task.expirationHandler = {
            work.cancel()
            task.setTaskCompleted(success: false)
        }
    }
}
