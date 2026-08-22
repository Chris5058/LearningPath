import Testing
import Foundation
@testable import StepPacer

struct MilestoneLedgerTests {

    /// Each test gets an isolated defaults domain so nothing leaks between runs.
    private func makeLedger(name: String = UUID().uuidString) -> (MilestoneLedger, UserDefaults) {
        let defaults = UserDefaults(suiteName: name)!
        defaults.removePersistentDomain(forName: name)
        return (MilestoneLedger(defaults: defaults, calendar: .current), defaults)
    }

    @Test("A milestone is only pending until it's recorded")
    func dedupe() {
        let (ledger, _) = makeLedger()
        let plan = MilestonePlanner.milestones(for: .default)

        let first = ledger.pending(steps: 3_800, plan: plan)
        #expect(first.map(\.threshold) == [3_700])

        ledger.record(first.map(\.threshold))
        #expect(ledger.pending(steps: 3_800, plan: plan).isEmpty)

        // Walking further surfaces only the newly passed checkpoint.
        #expect(ledger.pending(steps: 7_500, plan: plan).map(\.threshold) == [7_400])
    }

    @Test("Several checkpoints passed while asleep all come back pending")
    func batchedPending() {
        let (ledger, _) = makeLedger()
        let plan = MilestonePlanner.milestones(for: .default)

        let pending = ledger.pending(steps: 12_000, plan: plan)
        #expect(pending.map(\.threshold) == [3_700, 7_400, 11_100])
    }

    @Test("Yesterday's records don't suppress today's alerts")
    func newDayResets() {
        let (ledger, _) = makeLedger()
        let plan = MilestonePlanner.milestones(for: .default)

        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: Date())!
        ledger.record([3_700, 7_400], on: yesterday)

        #expect(ledger.delivered(on: yesterday) == [3_700, 7_400])
        #expect(ledger.delivered(on: Date()).isEmpty)
        #expect(ledger.pending(steps: 7_500, plan: plan).map(\.threshold) == [3_700, 7_400])
    }

    @Test("Resetting today clears only today")
    func resetToday() {
        let (ledger, _) = makeLedger()
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: Date())!

        ledger.record([3_700], on: yesterday)
        ledger.record([3_700, 7_400], on: Date())

        ledger.resetToday()
        #expect(ledger.delivered(on: Date()).isEmpty)
        #expect(ledger.delivered(on: yesterday) == [3_700])
    }

    @Test("History older than a week is pruned")
    func pruning() {
        let (ledger, defaults) = makeLedger()
        let old = Calendar.current.date(byAdding: .day, value: -30, to: Date())!

        ledger.record([3_700], on: old)
        // Recording today triggers the prune.
        ledger.record([3_700], on: Date())

        let stored = defaults.dictionary(forKey: "StepPacer.deliveredMilestones") as? [String: [Int]] ?? [:]
        #expect(stored.count == 1)
        #expect(ledger.delivered(on: old).isEmpty)
    }

    @Test("Day keys sort in chronological order")
    func dayKeyOrdering() {
        let calendar = Calendar(identifier: .gregorian)
        let earlier = MilestoneLedger.dayKey(for: Date(timeIntervalSince1970: 0), calendar: calendar)
        let later = MilestoneLedger.dayKey(for: Date(timeIntervalSince1970: 10_000_000), calendar: calendar)
        #expect(earlier < later)
    }
}
