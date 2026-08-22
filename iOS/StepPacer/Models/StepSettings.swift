import Foundation

/// User-tunable pacing settings, persisted in `UserDefaults` so the background
/// task and the UI read the exact same values.
struct StepSettings: Equatable, Codable {

    /// Alert every this many steps.
    var alertInterval: Int

    /// Steps to hit by end of day.
    var dailyGoal: Int

    /// Fold a checkpoint landing just below the goal into the goal alert.
    var mergesFinalMilestone: Bool

    /// Keep alerting on the same rhythm after the goal is met.
    var continuesAfterGoal: Bool

    /// Suppress notifications outside these hours (24h clock, local time).
    var quietHoursEnd: Int
    var quietHoursStart: Int

    static let `default` = StepSettings(
        alertInterval: 3_700,
        dailyGoal: 15_000,
        mergesFinalMilestone: true,
        continuesAfterGoal: false,
        quietHoursEnd: 7,
        quietHoursStart: 22
    )

    /// True when `date` falls inside the allowed alerting window.
    func allowsAlerts(at date: Date, calendar: Calendar = .current) -> Bool {
        let hour = calendar.component(.hour, from: date)
        if quietHoursStart == quietHoursEnd { return true }
        if quietHoursEnd < quietHoursStart {
            // Normal case: awake window sits inside one calendar day (07:00–22:00).
            return hour >= quietHoursEnd && hour < quietHoursStart
        }
        // Window wraps midnight (e.g. 22:00–07:00 allowed).
        return hour >= quietHoursEnd || hour < quietHoursStart
    }

    var isValid: Bool {
        alertInterval > 0 && dailyGoal > 0 && alertInterval <= dailyGoal
    }
}

/// Single storage point for settings, shared by the app and the background task.
enum SettingsStore {
    private static let key = "StepPacer.settings"

    static var defaults: UserDefaults = .standard

    static func load() -> StepSettings {
        guard let data = defaults.data(forKey: key),
              let decoded = try? JSONDecoder().decode(StepSettings.self, from: data),
              decoded.isValid
        else { return .default }
        return decoded
    }

    static func save(_ settings: StepSettings) {
        guard settings.isValid, let data = try? JSONEncoder().encode(settings) else { return }
        defaults.set(data, forKey: key)
    }
}
