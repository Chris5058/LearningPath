# StepPacer

An iPhone step counter that alerts you at every **3,700 steps** on the way to a
**15,000-step** daily target.

With the default settings you get alerts at:

| Steps  | Alert |
|--------|-------|
| 3,700  | Checkpoint |
| 7,400  | Checkpoint |
| 11,100 | Checkpoint |
| 15,000 | Daily goal 🎉 |

> **Why isn't there a 14,800 alert?** 3,700 × 4 = 14,800, which is only 200 steps
> short of the goal — you'd get two banners a minute apart. The app folds that
> checkpoint into the goal alert. Turn **Settings → Merge final checkpoint into
> goal** off if you'd rather have all five.

Everything is adjustable in-app: the interval, the goal, quiet hours, and whether
alerts keep going past the goal.

---

## Building it

**You need a Mac with Xcode 16 or newer, and a physical iPhone.** The Simulator
has no pedometer and no real Health data, so the app will just sit at 0 steps
there.

1. Open `iOS/StepPacer.xcodeproj`.
2. Select the **StepPacer** target → **Signing & Capabilities**.
3. Change the **Bundle Identifier** from `com.example.StepPacer` to something
   unique to you (e.g. `com.yourname.StepPacer`).
4. Pick your **Team**. A free Apple ID works; the app will need re-signing every
   7 days, which is normal for free accounts.
5. Plug in your iPhone, select it as the run destination, and hit **⌘R**.
6. On first launch, grant **Health** (steps) and **Notifications** access. Both
   are required — without notification permission you'll see the count but get
   no alerts.

If signing complains about the HealthKit background-delivery entitlement on a
free account, delete the `com.apple.developer.healthkit.background-delivery` key
from `iOS/Config/StepPacer.entitlements`. You'll lose background alerts but the
app still works while open, and background refresh still runs.

Run the tests with **⌘U** — they cover the milestone maths and the
no-duplicate-alerts ledger.

---

## How alerts actually reach you

This is the part worth understanding before you rely on it, because iOS does not
let an app count steps continuously in the background.

The app pulls on three ropes at once, and a shared ledger makes sure a checkpoint
is only ever announced once no matter which one fires:

| Source | When it runs | Latency |
|---|---|---|
| CoreMotion pedometer | App on screen | Seconds — the ring updates live |
| HealthKit background delivery | iOS wakes the app when new step data is written | Typically within the hour |
| `BGAppRefreshTask` | iOS schedules it when it feels like it | 30 min at best, often longer |

**In practice:** if the app is open you get an alert essentially the moment you
cross 3,700. If your phone is in your pocket and the app is closed, expect the
alert to arrive **within roughly an hour** of actually crossing the line, and
sometimes later — iOS throttles background work based on battery level, Low Power
Mode, and how often you use the app.

That's a platform limit, not something the code can tune away. No iOS app can
promise a to-the-step notification while backgrounded. If an alert lands late,
opening the app reconciles immediately.

Two behaviours that follow from this:

- **Passed several checkpoints while the app was asleep?** Only the highest one is
  announced. A burst of three stale banners helps nobody.
- **Quiet hours (default 22:00–07:00):** checkpoints crossed overnight are recorded
  silently rather than banked up and dumped on you at 7am.

---

## Layout

```
iOS/
├── StepPacer.xcodeproj
├── Config/
│   ├── Info.plist                    Usage strings, background modes
│   └── StepPacer.entitlements        HealthKit + background delivery
├── StepPacer/
│   ├── StepPacerApp.swift            Entry point, background registration
│   ├── Models/
│   │   ├── Milestone.swift           MilestonePlanner — the "every N steps" maths
│   │   ├── StepSettings.swift        Persisted settings + quiet hours
│   │   └── MilestoneLedger.swift     Per-day dedupe, so nothing fires twice
│   ├── Services/
│   │   ├── HealthKitService.swift    Daily total + background delivery
│   │   ├── PedometerService.swift    Live foreground counting
│   │   ├── NotificationService.swift Builds and fires the banners
│   │   ├── MilestoneCoordinator.swift steps → alerts, the single choke point
│   │   └── BackgroundRefreshService.swift  BGTaskScheduler wiring
│   ├── ViewModels/StepDashboardModel.swift
│   └── Views/
│       ├── DashboardView.swift       Ring, next-up card, checkpoint timeline
│       └── SettingsView.swift
└── StepPacerTests/
```

The design rule: `MilestonePlanner` and `MilestoneLedger` are pure and fully
tested; every entry point (launch, foreground tick, background wake) funnels into
`MilestoneCoordinator.evaluate()`, so there's exactly one place where a step count
turns into an alert.
