import Foundation
import Observation

@MainActor
@Observable
final class StepDashboardModel {

    private(set) var steps = 0
    private(set) var plan: [Milestone] = []
    private(set) var isAuthorized = false
    private(set) var errorMessage: String?

    var settings: StepSettings {
        didSet {
            guard settings != oldValue, settings.isValid else { return }
            SettingsStore.save(settings)
            plan = MilestonePlanner.milestones(for: settings)
            Task { await MilestoneCoordinator.shared.replanForToday() }
        }
    }

    private let pedometer = PedometerService()
    private let health = HealthKitService.shared
    private let notifications = NotificationService.shared

    init() {
        let loaded = SettingsStore.load()
        self.settings = loaded
        self.plan = MilestonePlanner.milestones(for: loaded)
    }

    var nextMilestone: Milestone? {
        MilestonePlanner.next(after: steps, in: plan)
    }

    var stepsToNextMilestone: Int {
        guard let next = nextMilestone else { return 0 }
        return max(0, next.threshold - steps)
    }

    /// Progress toward the next checkpoint, 0...1.
    var segmentProgress: Double {
        guard let next = nextMilestone else { return 1 }
        return MilestonePlanner.progress(steps: steps, toward: next, in: plan)
    }

    /// Progress toward the whole daily goal, 0...1.
    var goalProgress: Double {
        guard settings.dailyGoal > 0 else { return 0 }
        return min(1, Double(steps) / Double(settings.dailyGoal))
    }

    var reachedMilestones: Set<Int> {
        Set(MilestonePlanner.reached(steps, in: plan).map(\.threshold))
    }

    func onAppear() async {
        await requestPermissions()
        await refresh()
        startLiveUpdates()
    }

    func requestPermissions() async {
        do {
            try await health.requestAuthorization()
            isAuthorized = true
        } catch {
            errorMessage = error.localizedDescription
        }
        await notifications.requestAuthorization()
    }

    /// Pulls a fresh count and lets the coordinator fire anything newly passed.
    func refresh() async {
        steps = await MilestoneCoordinator.shared.evaluate()
    }

    func startLiveUpdates() {
        pedometer.startStreaming { [weak self] liveSteps in
            guard let self else { return }
            // The pedometer can lag HealthKit right after launch; never walk backwards.
            self.steps = max(self.steps, liveSteps)
            Task { await MilestoneCoordinator.shared.evaluate(steps: self.steps) }
        }
    }

    func stopLiveUpdates() {
        pedometer.stopStreaming()
    }
}
