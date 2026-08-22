import Foundation
import UserNotifications

/// Builds and delivers the milestone alerts.
final class NotificationService {

    static let shared = NotificationService()

    private let center = UNUserNotificationCenter.current()

    @discardableResult
    func requestAuthorization() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    func authorizationStatus() async -> UNAuthorizationStatus {
        await center.notificationSettings().authorizationStatus
    }

    /// Fires one banner per milestone, immediately.
    func announce(_ milestone: Milestone, steps: Int, settings: StepSettings, plan: [Milestone]) async {
        let content = UNMutableNotificationContent()
        content.sound = .default
        content.interruptionLevel = .active

        switch milestone.kind {
        case .goal:
            content.title = "Daily goal hit 🎉"
            content.body = "\(Self.format(steps)) steps — you're past your \(Self.format(settings.dailyGoal)) target for today."
        case .interval:
            content.title = "\(Self.format(milestone.threshold)) steps 👟"
            if let next = MilestonePlanner.next(after: steps, in: plan) {
                let remaining = max(0, next.threshold - steps)
                let suffix = next.kind == .goal ? "to hit your goal" : "to your next checkpoint"
                content.body = "\(Self.format(steps)) so far. \(Self.format(remaining)) more \(suffix)."
            } else {
                content.body = "\(Self.format(steps)) steps so far today."
            }
        }

        // Deliver now: a nil trigger means "as soon as this is submitted".
        let request = UNNotificationRequest(
            identifier: "milestone-\(MilestoneLedger.dayKey(for: Date(), calendar: .current))-\(milestone.threshold)",
            content: content,
            trigger: nil
        )

        try? await center.add(request)
    }

    /// A gentle end-of-day nudge if the goal is still out of reach.
    func scheduleEveningReminder(at hour: Int, settings: StepSettings) async {
        let identifier = "evening-reminder"
        center.removePendingNotificationRequests(withIdentifiers: [identifier])

        let content = UNMutableNotificationContent()
        content.title = "Still time to close the ring"
        content.body = "Check how far you are from \(Self.format(settings.dailyGoal)) steps."
        content.sound = .default

        var components = DateComponents()
        components.hour = hour
        components.minute = 0

        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
        )
        try? await center.add(request)
    }

    static func format(_ value: Int) -> String {
        value.formatted(.number.grouping(.automatic))
    }
}
