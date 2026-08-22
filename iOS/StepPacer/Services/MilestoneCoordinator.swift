import Foundation

/// The one place where "how many steps" turns into "send an alert".
///
/// Every entry point — app launch, foreground pedometer tick, background refresh,
/// HealthKit wake-up — calls `evaluate()`. Because the ledger dedupes, calling it
/// often is harmless; calling it from several places is what makes alerts reliable.
actor MilestoneCoordinator {

    static let shared = MilestoneCoordinator()

    private let health = HealthKitService.shared
    private let pedometer = PedometerService()
    private let notifications = NotificationService.shared
    private let ledger = MilestoneLedger()

    /// Reads the best available step count and announces anything newly passed.
    /// - Returns: today's step count, so callers can update their UI.
    @discardableResult
    func evaluate(now: Date = Date()) async -> Int {
        let steps = await currentSteps(now: now)
        await evaluate(steps: steps, now: now)
        return steps
    }

    /// Same as above but for a count the caller already has (the live pedometer stream).
    func evaluate(steps: Int, now: Date = Date()) async {
        let settings = SettingsStore.load()
        let plan = MilestonePlanner.milestones(for: settings)
        let pending = ledger.pending(steps: steps, plan: plan, on: now)
        guard !pending.isEmpty else { return }

        guard settings.allowsAlerts(at: now) else {
            // Inside quiet hours: bank them so the user isn't ambushed at 3am with a
            // backlog when they wake up and open the app.
            ledger.record(pending.map(\.threshold), on: now)
            return
        }

        // If several checkpoints were passed while the app was asleep, only announce
        // the highest one — a burst of stale banners helps nobody.
        if let latest = pending.last {
            await notifications.announce(latest, steps: steps, settings: settings, plan: plan)
        }
        ledger.record(pending.map(\.threshold), on: now)
    }

    /// Prefers the pedometer (instant, no permission round-trip) and falls back to
    /// HealthKit, which also picks up steps recorded by an Apple Watch.
    func currentSteps(now: Date = Date()) async -> Int {
        let healthSteps = (try? await health.stepsToday(now)) ?? 0
        let motionSteps = (try? await pedometer.stepsToday(now)) ?? 0
        return max(healthSteps, motionSteps)
    }

    /// Called when the user edits their plan so the new checkpoints can fire today.
    /// Anything already passed is marked delivered rather than announced, so changing
    /// the interval at 9,000 steps doesn't dump three banners at once.
    func replanForToday(now: Date = Date()) async {
        let settings = SettingsStore.load()
        let plan = MilestonePlanner.milestones(for: settings)
        let steps = await currentSteps(now: now)
        ledger.resetToday(now)
        ledger.record(MilestonePlanner.reached(steps, in: plan).map(\.threshold), on: now)
    }
}
