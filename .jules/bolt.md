## 2026-09-18 - Decoupling High-Frequency Timers in Flow Combine & Compose Stability
**Learning:** Combining a high-frequency ticker flow (like a 1s live duration timer) with list statistics recalculations in `combine` causes O(N) list operations on every tick. Additionally, data classes with `List<T>` properties are inferred as unstable by Compose unless marked `@Immutable`.
**Action:** Extract list transforms into a separate `map` flow before `combine` so operations run only when database emissions occur, and annotate state data classes with `@Immutable`.
