import Foundation

/// Remembers which milestones have already been announced today so a milestone is
/// never announced twice — the app, a background refresh and a HealthKit wake-up
/// all consult the same ledger.
///
/// Deliberately keyed by calendar day: at midnight the ledger goes empty and the
/// whole plan is live again.
struct MilestoneLedger {

    private let defaults: UserDefaults
    private let calendar: Calendar
    private static let key = "StepPacer.deliveredMilestones"

    init(defaults: UserDefaults = .standard, calendar: Calendar = .current) {
        self.defaults = defaults
        self.calendar = calendar
    }

    /// Thresholds already announced on `date`'s calendar day.
    func delivered(on date: Date = Date()) -> Set<Int> {
        guard let stored = defaults.dictionary(forKey: Self.key) as? [String: [Int]],
              let day = stored[Self.dayKey(for: date, calendar: calendar)]
        else { return [] }
        return Set(day)
    }

    func record(_ thresholds: [Int], on date: Date = Date()) {
        guard !thresholds.isEmpty else { return }
        var stored = defaults.dictionary(forKey: Self.key) as? [String: [Int]] ?? [:]
        let key = Self.dayKey(for: date, calendar: calendar)
        stored[key] = Array(Set(stored[key] ?? []).union(thresholds)).sorted()
        defaults.set(prune(stored, relativeTo: date), forKey: Self.key)
    }

    /// Drops every milestone recorded today. Used when the user edits their plan
    /// mid-day and wants the new checkpoints to fire.
    func resetToday(_ date: Date = Date()) {
        var stored = defaults.dictionary(forKey: Self.key) as? [String: [Int]] ?? [:]
        stored[Self.dayKey(for: date, calendar: calendar)] = nil
        defaults.set(stored, forKey: Self.key)
    }

    /// Milestones that `steps` has passed but that haven't been announced yet.
    func pending(steps: Int, plan: [Milestone], on date: Date = Date()) -> [Milestone] {
        let already = delivered(on: date)
        return MilestonePlanner.reached(steps, in: plan)
            .filter { !already.contains($0.threshold) }
    }

    /// Keeps a week of history so the dictionary can't grow without bound.
    private func prune(_ stored: [String: [Int]], relativeTo date: Date) -> [String: [Int]] {
        let cutoff = calendar.date(byAdding: .day, value: -7, to: date) ?? date
        let cutoffKey = Self.dayKey(for: cutoff, calendar: calendar)
        return stored.filter { $0.key >= cutoffKey }
    }

    /// Sortable `yyyy-MM-dd` key — string comparison matches date order.
    static func dayKey(for date: Date, calendar: Calendar) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }
}
