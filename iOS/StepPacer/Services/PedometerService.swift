import Foundation
import CoreMotion

/// Live step updates while the app is on screen. CoreMotion's pedometer pushes a new
/// count every few seconds, so the ring animates in real time instead of waiting on
/// HealthKit's batched writes.
final class PedometerService {

    private let pedometer = CMPedometer()
    private var isStreaming = false

    var isAvailable: Bool { CMPedometer.isStepCountingAvailable() }

    var authorizationStatus: CMAuthorizationStatus { CMPedometer.authorizationStatus() }

    /// Steps counted since the start of today, straight from the motion coprocessor.
    func stepsToday(_ date: Date = Date(), calendar: Calendar = .current) async throws -> Int {
        guard isAvailable else { throw StepPacerError.motionUnavailable }
        let start = calendar.startOfDay(for: date)

        return try await withCheckedThrowingContinuation { continuation in
            pedometer.queryPedometerData(from: start, to: date) { data, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: data?.numberOfSteps.intValue ?? 0)
                }
            }
        }
    }

    /// Streams today's running total. Only meaningful in the foreground — iOS suspends
    /// these callbacks once the app leaves the screen.
    func startStreaming(onUpdate: @escaping (Int) -> Void) {
        guard isAvailable, !isStreaming else { return }
        isStreaming = true

        pedometer.startUpdates(from: Calendar.current.startOfDay(for: Date())) { data, error in
            guard error == nil, let data else { return }
            let steps = data.numberOfSteps.intValue
            Task { @MainActor in onUpdate(steps) }
        }
    }

    func stopStreaming() {
        guard isStreaming else { return }
        pedometer.stopUpdates()
        isStreaming = false
    }
}
