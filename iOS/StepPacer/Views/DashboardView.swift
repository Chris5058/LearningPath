import SwiftUI

struct DashboardView: View {

    @Bindable var model: StepDashboardModel
    @State private var showingSettings = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 28) {
                    ProgressRing(
                        progress: model.goalProgress,
                        steps: model.steps,
                        goal: model.settings.dailyGoal
                    )
                    .frame(height: 260)
                    .padding(.top, 12)

                    nextUpCard

                    MilestoneTimeline(
                        plan: model.plan,
                        reached: model.reachedMilestones,
                        steps: model.steps
                    )

                    if let errorMessage = model.errorMessage {
                        Text(errorMessage)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                }
                .padding(.horizontal)
                .padding(.bottom, 32)
            }
            .navigationTitle("Today")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingSettings = true } label: {
                        Image(systemName: "slider.horizontal.3")
                    }
                    .accessibilityLabel("Settings")
                }
            }
            .refreshable { await model.refresh() }
            .sheet(isPresented: $showingSettings) {
                SettingsView(model: model)
            }
        }
    }

    @ViewBuilder
    private var nextUpCard: some View {
        VStack(spacing: 6) {
            if let next = model.nextMilestone {
                Text(next.kind == .goal ? "Goal in" : "Next alert in")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Text("\(NotificationService.format(model.stepsToNextMilestone)) steps")
                    .font(.system(size: 34, weight: .semibold, design: .rounded))
                    .contentTransition(.numericText())

                ProgressView(value: model.segmentProgress)
                    .tint(.accentColor)
                    .padding(.top, 4)

                Text("at \(NotificationService.format(next.threshold)) steps")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            } else {
                Text("All done for today 🎉")
                    .font(.title3.weight(.semibold))
                Text("Every checkpoint cleared.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}

/// The big progress ring, with a tick for every milestone.
private struct ProgressRing: View {

    let progress: Double
    let steps: Int
    let goal: Int

    var body: some View {
        ZStack {
            Circle()
                .stroke(Color.secondary.opacity(0.18), lineWidth: 22)

            Circle()
                .trim(from: 0, to: max(0.001, progress))
                .stroke(
                    AngularGradient(
                        colors: [.accentColor.opacity(0.65), .accentColor],
                        center: .center
                    ),
                    style: StrokeStyle(lineWidth: 22, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))
                .animation(.smooth, value: progress)

            VStack(spacing: 2) {
                Text(NotificationService.format(steps))
                    .font(.system(size: 52, weight: .bold, design: .rounded))
                    .contentTransition(.numericText())
                Text("of \(NotificationService.format(goal)) steps")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(steps) of \(goal) steps today")
    }
}

/// Vertical list of the day's checkpoints with their state.
private struct MilestoneTimeline: View {

    let plan: [Milestone]
    let reached: Set<Int>
    let steps: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Checkpoints")
                .font(.headline)
                .padding(.bottom, 12)

            ForEach(plan) { milestone in
                let isReached = reached.contains(milestone.threshold)
                let isNext = !isReached && plan.first { !reached.contains($0.threshold) } == milestone

                HStack(spacing: 14) {
                    Image(systemName: symbol(isReached: isReached, kind: milestone.kind))
                        .font(.title3)
                        .foregroundStyle(isReached ? Color.accentColor : Color.secondary.opacity(0.5))
                        .frame(width: 28)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(NotificationService.format(milestone.threshold) + " steps")
                            .font(.body.weight(isNext ? .semibold : .regular))
                        if milestone.kind == .goal {
                            Text("Daily target")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        } else if isNext {
                            Text("\(NotificationService.format(max(0, milestone.threshold - steps))) to go")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }

                    Spacer()
                }
                .padding(.vertical, 10)
                .opacity(isReached || isNext ? 1 : 0.55)

                if milestone != plan.last {
                    Divider().padding(.leading, 42)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private func symbol(isReached: Bool, kind: Milestone.Kind) -> String {
        if kind == .goal { return isReached ? "flag.checkered.circle.fill" : "flag.checkered.circle" }
        return isReached ? "checkmark.circle.fill" : "circle"
    }
}
