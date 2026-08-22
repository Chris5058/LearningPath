import Foundation
import HealthKit

/// Reads today's step total from HealthKit and — crucially — asks HealthKit to wake
/// the app when new step data lands, which is what lets milestone alerts fire while
/// the app is closed.
final class HealthKitService {

    static let shared = HealthKitService()

    private let store = HKHealthStore()
    private var observer: HKObserverQuery?

    private var stepType: HKQuantityType { HKQuantityType(.stepCount) }

    var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    func requestAuthorization() async throws {
        guard isAvailable else { throw StepPacerError.healthDataUnavailable }
        try await store.requestAuthorization(toShare: [], read: [stepType])
    }

    /// Total steps recorded between the start of `date`'s day and now.
    ///
    /// Uses `.separateBySource` + `.strictStartDate` so an iPhone and a Watch both
    /// logging the same walk don't double-count: HealthKit de-duplicates overlapping
    /// samples from different sources for us.
    func stepsToday(_ date: Date = Date(), calendar: Calendar = .current) async throws -> Int {
        guard isAvailable else { throw StepPacerError.healthDataUnavailable }

        let start = calendar.startOfDay(for: date)
        let predicate = HKQuery.predicateForSamples(withStart: start, end: date, options: .strictStartDate)

        return try await withCheckedThrowingContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: stepType,
                quantitySamplePredicate: predicate,
                options: [.cumulativeSum, .separateBySource]
            ) { _, statistics, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                let steps = statistics?.sumQuantity()?.doubleValue(for: .count()) ?? 0
                continuation.resume(returning: Int(steps.rounded()))
            }
            store.execute(query)
        }
    }

    /// Registers for background delivery of step updates.
    ///
    /// iOS throttles this — `.hourly` is the floor HealthKit honours for cumulative
    /// types — so treat it as "the app gets a chance to check in regularly", not as a
    /// real-time stream. The foreground pedometer covers the live case.
    func startBackgroundDelivery(onUpdate: @escaping () async -> Void) {
        guard isAvailable, observer == nil else { return }

        let query = HKObserverQuery(sampleType: stepType, predicate: nil) { _, completionHandler, error in
            guard error == nil else {
                completionHandler()
                return
            }
            Task {
                await onUpdate()
                // Must be called or HealthKit stops waking us.
                completionHandler()
            }
        }

        observer = query
        store.execute(query)

        store.enableBackgroundDelivery(for: stepType, frequency: .hourly) { success, error in
            if let error {
                print("[StepPacer] background delivery failed: \(error.localizedDescription)")
            } else if !success {
                print("[StepPacer] background delivery was refused")
            }
        }
    }
}

enum StepPacerError: LocalizedError {
    case healthDataUnavailable
    case motionUnavailable

    var errorDescription: String? {
        switch self {
        case .healthDataUnavailable:
            return "Health data isn't available on this device."
        case .motionUnavailable:
            return "Step counting isn't available on this device."
        }
    }
}
