import Testing
import Foundation
@testable import StepPacer

struct MilestonePlannerTests {

    @Test("Default plan: 3,700-step checkpoints toward a 15,000 goal")
    func defaultPlan() {
        let plan = MilestonePlanner.milestones(for: .default)

        // 14,800 sits only 200 steps below the goal, so it folds into the goal alert.
        #expect(plan.map(\.threshold) == [3_700, 7_400, 11_100, 15_000])
        #expect(plan.last?.kind == .goal)
        #expect(plan.dropLast().allSatisfy { $0.kind == .interval })
    }

    @Test("Merging off keeps the checkpoint that lands just below the goal")
    func mergingDisabled() {
        var settings = StepSettings.default
        settings.mergesFinalMilestone = false

        let plan = MilestonePlanner.milestones(for: settings)
        #expect(plan.map(\.threshold) == [3_700, 7_400, 11_100, 14_800, 15_000])
    }

    @Test("An interval that divides the goal exactly produces no duplicate")
    func exactDivision() {
        var settings = StepSettings.default
        settings.alertInterval = 5_000
        settings.dailyGoal = 15_000

        let plan = MilestonePlanner.milestones(for: settings)
        #expect(plan.map(\.threshold) == [5_000, 10_000, 15_000])
        #expect(Set(plan.map(\.threshold)).count == plan.count)
    }

    @Test("Continuing past the goal extends the same rhythm")
    func continuesPastGoal() {
        var settings = StepSettings.default
        settings.continuesAfterGoal = true

        let plan = MilestonePlanner.milestones(for: settings)
        #expect(plan.prefix(4).map(\.threshold) == [3_700, 7_400, 11_100, 15_000])
        #expect(plan.count > 4)
        #expect(plan.allSatisfy { $0.threshold > 0 })
        // Everything past the goal stays on the 3,700 grid.
        for milestone in plan where milestone.threshold > 15_000 {
            #expect(milestone.threshold % 3_700 == 0)
        }
    }

    @Test("Invalid settings produce no milestones")
    func invalidSettings() {
        var settings = StepSettings.default
        settings.alertInterval = 0
        #expect(MilestonePlanner.milestones(for: settings).isEmpty)
    }

    @Test("Reached and next track the walker correctly")
    func reachedAndNext() {
        let plan = MilestonePlanner.milestones(for: .default)

        #expect(MilestonePlanner.reached(0, in: plan).isEmpty)
        #expect(MilestonePlanner.next(after: 0, in: plan)?.threshold == 3_700)

        #expect(MilestonePlanner.reached(3_700, in: plan).map(\.threshold) == [3_700])
        #expect(MilestonePlanner.next(after: 3_700, in: plan)?.threshold == 7_400)

        #expect(MilestonePlanner.reached(12_000, in: plan).count == 3)
        #expect(MilestonePlanner.next(after: 12_000, in: plan)?.kind == .goal)

        #expect(MilestonePlanner.next(after: 20_000, in: plan) == nil)
    }

    @Test("Segment progress is measured from the previous checkpoint")
    func segmentProgress() {
        let plan = MilestonePlanner.milestones(for: .default)
        let next = try! #require(MilestonePlanner.next(after: 5_550, in: plan))

        // Half way between 3,700 and 7,400.
        let progress = MilestonePlanner.progress(steps: 5_550, toward: next, in: plan)
        #expect(abs(progress - 0.5) < 0.001)
    }
}

struct StepSettingsTests {

    @Test("Quiet hours block alerts overnight")
    func quietHours() {
        let settings = StepSettings.default // alerts 07:00–22:00
        let calendar = Calendar(identifier: .gregorian)

        #expect(settings.allowsAlerts(at: date(hour: 9), calendar: calendar))
        #expect(settings.allowsAlerts(at: date(hour: 21), calendar: calendar))
        #expect(!settings.allowsAlerts(at: date(hour: 3), calendar: calendar))
        #expect(!settings.allowsAlerts(at: date(hour: 23), calendar: calendar))
    }

    @Test("Matching start and end means always on")
    func alwaysOn() {
        var settings = StepSettings.default
        settings.quietHoursStart = 0
        settings.quietHoursEnd = 0
        #expect(settings.allowsAlerts(at: date(hour: 3)))
    }

    @Test("Validation rejects an interval larger than the goal")
    func validation() {
        var settings = StepSettings.default
        #expect(settings.isValid)
        settings.alertInterval = 20_000
        #expect(!settings.isValid)
    }

    private func date(hour: Int) -> Date {
        var components = DateComponents()
        components.year = 2026
        components.month = 8
        components.day = 22
        components.hour = hour
        return Calendar.current.date(from: components) ?? Date()
    }
}
