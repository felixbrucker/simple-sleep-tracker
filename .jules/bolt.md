## 2026-09-18 - Decoupling High-Frequency Timers in Flow Combine & Compose Stability
**Learning:** Combining a high-frequency ticker flow (like a 1s live duration timer) with list statistics recalculations in `combine` causes O(N) list operations on every tick. Additionally, data classes with `List<T>` properties are inferred as unstable by Compose unless marked `@Immutable`.
**Action:** Extract list transforms into a separate `map` flow before `combine` so operations run only when database emissions occur, and annotate state data classes with `@Immutable`.

## 2026-09-18 - Reactive Ticker Loops & Draw-Phase Scale Modifiers
**Learning:** Infinite ticker loops (`while (isActive) { delay(1s) }`) running regardless of state cause idle CPU wakeups every second. Furthermore, reading animated scale state in layout modifiers (`Modifier.scale`) causes per-frame recomposition.
**Action:** Gate ticker loops using `distinctUntilChanged()` and `collectLatest` on state flows so the coroutine suspends when inactive, and use `graphicsLayer { scaleX = ... }` to defer animated state reads to the draw phase.

## 2026-09-19 - Pure State Mappers in High-Frequency Ticker Flow Combines
**Learning:** Returning constant values (e.g. `0L`) from a ticker flow during initial phases prevents `StateFlow` emissions (`0L == 0L`), freezing state updates like live countdown timers in downstream `combine` operators. Additionally, calling `System.currentTimeMillis()` inside `combine` lambda blocks causes impure, non-deterministic state derivations.
**Action:** Always emit true monotonic/elapsed delta values from ticker flows so `StateFlow` distinct checks trigger updates, and pass pure `elapsedMillis` into `combine` blocks instead of calling `System.currentTimeMillis()`.
