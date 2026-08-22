import SwiftUI

struct SettingsView: View {

    @Bindable var model: StepDashboardModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Stepper(
                        value: $model.settings.dailyGoal,
                        in: 1_000...50_000,
                        step: 500
                    ) {
                        LabeledContent("Daily goal", value: "\(NotificationService.format(model.settings.dailyGoal))")
                    }

                    Stepper(
                        value: $model.settings.alertInterval,
                        in: 250...model.settings.dailyGoal,
                        step: 100
                    ) {
                        LabeledContent("Alert every", value: "\(NotificationService.format(model.settings.alertInterval))")
                    }
                } header: {
                    Text("Pacing")
                } footer: {
                    Text(planSummary)
                }

                Section {
                    Toggle("Merge final checkpoint into goal", isOn: $model.settings.mergesFinalMilestone)
                    Toggle("Keep alerting past the goal", isOn: $model.settings.continuesAfterGoal)
                } footer: {
                    Text("Merging avoids two alerts landing a few hundred steps apart near the end of the day.")
                }

                Section {
                    Picker("Alerts start", selection: $model.settings.quietHoursEnd) {
                        ForEach(0..<24, id: \.self) { Text(Self.hourLabel($0)).tag($0) }
                    }
                    Picker("Alerts stop", selection: $model.settings.quietHoursStart) {
                        ForEach(0..<24, id: \.self) { Text(Self.hourLabel($0)).tag($0) }
                    }
                } header: {
                    Text("Quiet hours")
                } footer: {
                    Text("Checkpoints passed outside these hours are recorded silently — you won't get a backlog of banners later.")
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    private var planSummary: String {
        let thresholds = model.plan.map { NotificationService.format($0.threshold) }
        return "You'll be alerted at: " + thresholds.joined(separator: ", ") + "."
    }

    private static func hourLabel(_ hour: Int) -> String {
        var components = DateComponents()
        components.hour = hour
        components.minute = 0
        let date = Calendar.current.date(from: components) ?? Date()
        return date.formatted(date: .omitted, time: .shortened)
    }
}
