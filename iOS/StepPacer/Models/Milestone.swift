import Foundation

/// A step count at which the user should be alerted.
struct Milestone: Identifiable, Equatable, Hashable, Codable {
    enum Kind: String, Codable {
        /// One of the repeating "every N steps" checkpoints.
        case interval
        /// The daily target itself.
        case goal
    }

    let threshold: Int
    let kind: Kind

    var id: Int { threshold }
}

/// Pure, deterministic milestone maths. No HealthKit, no notifications, no clock —
/// everything here is trivially testable and is the single source of truth for
/// "when should we alert?".
enum MilestonePlanner {

    /// Builds the ordered list of thresholds for a day.
    ///
    /// With the default settings (stride 3700, goal 15000, merging on) this yields
    /// 3700, 7400, 11100, 15000 — the 14800 checkpoint is folded into the goal alert
    /// because firing two notifications 200 steps apart is just noise.
    static func milestones(for settings: StepSettings) -> [Milestone] {
        let stride = settings.alertInterval
        let goal = settings.dailyGoal
        guard stride > 0, goal > 0 else { return [] }

        var result: [Milestone] = []

        var threshold = stride
        while threshold < goal {
            result.append(Milestone(threshold: threshold, kind: .interval))
            threshold += stride
        }

        // Fold a checkpoint that lands just shy of the goal into the goal alert.
        if settings.mergesFinalMilestone,
           let last = result.last,
           goal - last.threshold < mergeWindow(for: stride) {
            result.removeLast()
        }

        result.append(Milestone(threshold: goal, kind: .goal))

        if settings.continuesAfterGoal {
            // Keep the rhythm going past the target, but cap it so a marathon day
            // doesn't turn into an endless stream of banners.
            var stretch = ((goal / stride) + 1) * stride
            var added = 0
            while added < Self.maxStretchMilestones {
                if stretch > goal {
                    result.append(Milestone(threshold: stretch, kind: .interval))
                    added += 1
                }
                stretch += stride
            }
        }

        return result
    }

    /// Milestones already passed at `steps`, oldest first.
    static func reached(_ steps: Int, in milestones: [Milestone]) -> [Milestone] {
        milestones.filter { steps >= $0.threshold }
    }

    /// The next milestone the user is walking toward, if any remain.
    static func next(after steps: Int, in milestones: [Milestone]) -> Milestone? {
        milestones.first { steps < $0.threshold }
    }

    /// How close the walker is to `milestone`, as 0...1.
    static func progress(steps: Int, toward milestone: Milestone, in milestones: [Milestone]) -> Double {
        let previous = milestones.last { $0.threshold <= steps && $0.threshold < milestone.threshold }?.threshold ?? 0
        let span = milestone.threshold - previous
        guard span > 0 else { return 1 }
        return min(1, max(0, Double(steps - previous) / Double(span)))
    }

    /// A checkpoint closer than this to the goal gets merged into the goal alert.
    static func mergeWindow(for stride: Int) -> Int {
        max(1, stride / 4)
    }

    private static let maxStretchMilestones = 6
}
