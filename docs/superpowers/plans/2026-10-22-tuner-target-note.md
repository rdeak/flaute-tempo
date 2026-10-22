# Tuner Target Note Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the child pick the note they are about to play on the tuner tab, in a scroll wheel, and measure the played pitch against that note instead of the nearest one.

**Architecture:** The domain gains a cents-from-target function and the flute's note range; the settings store remembers the chosen note; `TunerUiState` carries the target and derives the deviation. The metronome's `TempoWheel` is generalised into `ui/common/ValueWheel`, which the new `NoteWheel` reuses. `TunerContent` swaps the big letter for the wheel and reads the deviation from the state.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Hilt, SharedPreferences, JUnit 4 unit tests, Compose UI tests on the headless emulator `Medium_Phone_API_37.0` (boot with `-no-audio`).

**Spec:** `docs/superpowers/specs/2026-10-22-tuner-target-note-design.md`

## Global Constraints

- No commits: this repository's CLAUDE.md forbids the agent committing; every "commit" step below means "leave the change in the working tree".
- Range c1 to c4: MIDI 60 to 96 inclusive, 37 entries; default target MIDI 69 (A4 / a1).
- Croatian note labels: lowercase Helmholtz-style with the octave number for MIDI >= 60 (c1 = MIDI 60, c4 = MIDI 96); English: scientific pitch notation (C4 = MIDI 60).
- Thresholds unchanged: in tune at |cents| <= 5, slightly off up to 15, gauge clamped at ±50.
- Every user-visible string in both `values/strings.xml` and `values-hr/strings.xml`; Croatian in the informal singular.
- `TempoWheel`'s public API, content description ("Tempo wheel, scroll to change the speed") and `MetronomeScreenTest` stay unchanged.
- No numbers on the tuner besides the octave digit in the note name; no "cents", no "Hz".

## Review Focus

1. A stored target note outside the range (dev device, corrupt prefs): the wheel must open on a note it contains. Test in Task 3 (`stored target note is restored and clamped`).
2. A note an octave away from the target (a beginner overblowing): the needle pins at the edge and the status still says low/high, no crash. Test in Task 6 (`a_note_far_from_the_target_still_says_which_way`).
3. The A4 reference changing (advanced mode) must move the deviation from the target too, not just the reading. Test in Task 3 (`reference pitch moves the deviation from the target`).
4. The top of the range in Croatian must read "c4", not "c7" or "C7". Test in Task 4 (`croatian_octaves_follow_the_music_school_convention`).
5. The tempo wheel must behave exactly as before after the refactor: neighbours, scroll-to-notch, following a pill. Existing `MetronomeScreenTest` run in Task 5.

---

### Task 1: Domain: cents from a target note and the flute range

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/domain/tuner/NoteMath.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/domain/tuner/FluteRange.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/domain/tuner/NoteMathTest.kt`, `app/src/test/java/hr/bertlab/fluteassistant/domain/tuner/FluteRangeTest.kt`

**Interfaces:**
- Produces: `NoteMath.centsBetween(frequency: Double, targetMidi: Int, a4: Double = DEFAULT_A4): Double` (signed, unclamped; positive when sharp); `object FluteRange { const val LOWEST_MIDI = 60; const val HIGHEST_MIDI = 96; const val DEFAULT_TARGET = 69; val midiRange: IntRange; fun clamp(midi: Int): Int }`.

- [ ] **Step 1: Write the failing tests.** In `NoteMathTest`, `cents between a frequency and a target note`: 440 Hz vs 69 is 0.0 (±0.01); 466.16 Hz vs 69 is 100.0 (±0.05); 220 Hz vs 69 is -1200.0 (±0.01); 440 Hz vs 60 is 900.0 (±0.01); 442 Hz vs 69 with `a4 = 442.0` is 0.0. New `FluteRangeTest`: `range covers c1 to c4` (`midiRange` is 60..96, 37 entries, contains `DEFAULT_TARGET`); `clamp keeps a note inside the range` (clamp(10) = 60, clamp(200) = 96, clamp(69) = 69).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*NoteMathTest*' --tests '*FluteRangeTest*'`; expected: compilation failure (unresolved `centsBetween`, `FluteRange`).
- [ ] **Step 3: Implement** `centsBetween` as `1200 * log2(frequency / midiToFrequency(targetMidi, a4))` with the same positivity `require`s as `frequencyToNote`; create `FluteRange`.
- [ ] **Step 4: Run** the same command; expected: PASS.

### Task 2: Settings: remember the chosen note

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/domain/repository/SettingsStore.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/data/settings/SharedPreferencesSettingsStore.kt`
- Modify: `app/src/test/java/hr/bertlab/fluteassistant/fakes/Fakes.kt`

**Interfaces:**
- Consumes: `FluteRange.DEFAULT_TARGET`.
- Produces: `SettingsStore.loadTargetNote(): Int`, `SettingsStore.saveTargetNote(midi: Int)`; `FakeSettingsStore(var targetNote: Int = FluteRange.DEFAULT_TARGET)`.

- [ ] **Step 1: Add the two methods** to the interface (doc comment: the tuner's chosen note as a MIDI number) and to `SharedPreferencesSettingsStore` under key `"target_note"` with `FluteRange.DEFAULT_TARGET` as the default; the store does not clamp (the ViewModel does).
- [ ] **Step 2: Extend `FakeSettingsStore`** with the `targetNote` field and the two overrides.
- [ ] **Step 3: Run** `./gradlew :app:compileDebugUnitTestKotlin -q`; expected: BUILD SUCCESSFUL (Task 3 adds the behaviour tests).

### Task 3: Tuner state and ViewModel

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerUiState.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerViewModel.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/ui/tuner/TunerUiStateTest.kt` (new), `app/src/test/java/hr/bertlab/fluteassistant/ui/tuner/TunerViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 and Task 2.
- Produces: `TunerUiState.targetMidi: Int = FluteRange.DEFAULT_TARGET`; `val TunerUiState.centsOffTarget: Double?` (null while `reading` is null, else `NoteMath.centsBetween(reading.frequency, targetMidi, referenceA4)`); `TunerViewModel.setTargetNote(midi: Int)`.

- [ ] **Step 1: Write the failing tests.** `TunerUiStateTest.cents off target follow the chosen note`: reading from 440 Hz with target 69 gives 0.0; same reading with target 67 gives 200.0 (±0.01); no reading gives null. In `TunerViewModelTest`: `stored target note is restored and clamped` (settings.targetNote = 62 gives state 62; settings.targetNote = 10 gives 60); `choosing a target note is saved and clamped` (setTargetNote(72) gives state 72 and settings 72; setTargetNote(999) gives 96); `deviation is measured from the chosen note` (listening, frames at 440 Hz, setTargetNote(67): `centsOffTarget` 200.0 ±0.05 while `reading.label` stays "A4"); `reference pitch moves the deviation from the target` (frames at 440 Hz, target 69, setReferenceA4(442.0): `centsOffTarget` -7.85 ±0.05).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*Tuner*'`; expected: compilation failure.
- [ ] **Step 3: Implement.** Initial state `TunerUiState(targetMidi = FluteRange.clamp(settings.loadTargetNote()))` (keep the A4 comment: the target is visible on screen, so a stored value is safe); `setTargetNote` clamps, calls `settings.saveTargetNote`, updates the state.
- [ ] **Step 4: Run** the same command; expected: PASS.

### Task 4: Localised octave names

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-hr/strings.xml`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/common/Labels.kt:45-59`
- Test: `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/common/LabelsTest.kt`

**Interfaces:**
- Produces: `bool/note_octaves_helmholtz` (false in `values`, true in `values-hr`); `noteLabel(resources, midi)` and the composable `noteLabel(midi)` now follow it.

- [ ] **Step 1: Rewrite `note_names_follow_the_locale`** and add `croatian_octaves_follow_the_music_school_convention`. English: 71 "B4", 70 "A♯4", 60 "C4", 96 "C7". Croatian: 71 "h1", 70 "b1", 60 "c1", 61 "cis1", 84 "c3", 96 "c4", 59 "h", 48 "c", 47 "H", 36 "C", 24 "C1".
- [ ] **Step 2: Add the bool** next to the `note_names` arrays (comment in `values-hr`: Croatian music schools name octaves c1, c2 … with the one-lined octave at middle C).
- [ ] **Step 3: Implement the rule** in `noteLabel`: scientific `octave = floorDiv(midi, 12) - 1`; when the bool is false return `name + octave`; otherwise octave >= 4 gives `name.lowercase() + (octave - 3)`, octave 3 gives `name.lowercase()`, octave 2 gives `name`, lower gives `name + (2 - octave)`.
- [ ] **Step 4: Run** `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.common.LabelsTest`; expected: PASS.

### Task 5: Shared ValueWheel

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/common/ValueWheel.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/metronome/TempoWheel.kt`
- Test: `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/metronome/MetronomeScreenTest.kt` (unchanged)

**Interfaces:**
- Produces: `@Composable fun ValueWheel(itemCount: Int, selectedIndex: Int, onSelectedIndexChange: (Int) -> Unit, label: (Int) -> String, contentDescription: String, modifier: Modifier = Modifier, onCentreClick: () -> Unit = {}, itemHeight: Dp = 48.dp, centreStyle: TextStyle = displayLarge at 64.sp / 76.sp line height, neighbourStyle: TextStyle = titleLarge, centreColor: Color = onSurface)`. The content description stays on the `LazyColumn` node (tests scroll it with `performScrollToIndex`), items keep index keys.

- [ ] **Step 1: Move the drum** (list state, centred-index derivation, both `LaunchedEffect`s, haptics, snapping, tap-to-scroll) from `TempoWheel` into `ValueWheel`, indices instead of BPM values.
- [ ] **Step 2: Reduce `TempoWheel`** to a call of `ValueWheel` with `itemCount = maxBpm - minBpm + 1`, `selectedIndex = bpm - minBpm`, label `(minBpm + it).toString()`, `onCentreClick = onClose`; same signature and KDoc as before.
- [ ] **Step 3: Run** `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.metronome.MetronomeScreenTest`; expected: PASS, all wheel tests included.

### Task 6: NoteWheel and the tuner layout

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/NoteWheel.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-hr/strings.xml`
- Test: `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/tuner/TunerScreenTest.kt`

**Interfaces:**
- Consumes: Tasks 3, 4, 5.
- Produces: `@Composable fun NoteWheel(targetMidi: Int, onTargetChange: (Int) -> Unit, inTune: Boolean, modifier: Modifier = Modifier)`; `TunerContent(state, onTargetNoteChange: (Int) -> Unit, onRequestPermission, onOpenSettings, modifier)`; strings `target_note` ("Note to tune, scroll to choose another" / "Ton koji ugađaš, zavrti za drugi") and `play_note_prompt` ("👂 Play %1$s" / "👂 Odsviraj %1$s"); `play_a_note` removed.

- [ ] **Step 1: Rewrite `TunerScreenTest`.** `show` takes `targetMidi` and records `chosenNote`. Tests: `asks_for_the_microphone_until_permission_is_granted` (as is); `shows_the_target_note_with_its_neighbours_and_too_high` (443 Hz, target 69: "A4", "G♯4", "A♯4" displayed, "⬆️ Too high", no "+12 ¢"); `measures_against_the_chosen_note` (440 Hz with target 67 says "⬆️ Too high"; with target 69 says "🎉 Perfect!"); `a_note_far_from_the_target_still_says_which_way` (220 Hz, target 69: "⬇️ Too low"); `silence_names_the_note_to_play` ("👂 Play A4"); `scrolling_the_wheel_reports_the_new_note` (scroll the node with content description "Note to tune, scroll to choose another" to index 72 - 60, then `chosenNote == 72`); `croatian_names_the_target_note_the_school_way` (493.88 Hz, target 71, locale hr: "h1" displayed, "🎉 Savršeno!"; silence with target 69 shows "👂 Odsviraj a1").
- [ ] **Step 2: Run** `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.tuner.TunerScreenTest`; expected: compilation failure (`onTargetNoteChange`).
- [ ] **Step 3: Strings:** add the two, delete `play_a_note`, in both files.
- [ ] **Step 4: `NoteWheel`:** a `Box` with a `surfaceVariant` band (shape `MaterialTheme.shapes.large`, full width, `itemHeight` tall, centred) under a `ValueWheel` over `FluteRange.midiRange` labelled with `noteLabel(midi)`, `itemHeight = 56.dp`, centre style `displayLarge` at 72.sp / 84.sp line height, `centreColor` = in-tune green (dark/light variants as the status text) when `inTune`, else `onSurface`.
- [ ] **Step 5: `TunerContent`:** replace the big `Text` with `NoteWheel(state.targetMidi, onTargetNoteChange, inTune = status == IN_TUNE)`; compute `status` from `state.centsOffTarget`; the NONE line is `stringResource(R.string.play_note_prompt, noteLabel(state.targetMidi))`; `TunerScreen` passes `viewModel::setTargetNote`.
- [ ] **Step 6: Run** the Task 6 Step 2 command; expected: PASS.

### Task 7: Docs and verification

**Files:**
- Modify: `CLAUDE.md` (Tuner path, kid-friendly constraint paragraph)

- [ ] **Step 1: Update CLAUDE.md:** the tuner shows a note wheel (target), the gauge measures `centsOffTarget`, `noteLabel` follows the locale's octave convention, `ValueWheel` is shared.
- [ ] **Step 2: Run** `./gradlew testDebugUnitTest lintRelease`; expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Screenshots** on the emulator: tuner tab in English and Croatian, light and dark (`adb shell cmd locale set-app-locales …`, `cmd uimode night yes`), checked by eye for the wheel, band and status fitting without scrolling.

### Task 8: Keep both modes (spec revision)

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/domain/tuner/TunerMode.kt`
- Modify: `SettingsStore.kt`, `SharedPreferencesSettingsStore.kt`, `Fakes.kt`, `TunerUiState.kt`, `TunerViewModel.kt`, `Labels.kt`, `NoteWheel.kt`, `TunerScreen.kt`, both `strings.xml`, `CLAUDE.md`
- Test: `TunerUiStateTest.kt`, `TunerViewModelTest.kt`, `TunerScreenTest.kt`

**Interfaces:**
- Produces: `enum class TunerMode { IDENTIFY, TARGET }`; `SettingsStore.loadTunerMode(): TunerMode` / `saveTunerMode(mode)`; `TunerUiState.mode` (default `IDENTIFY`) and `gaugeCents: Double?`; `TunerViewModel.setMode(mode)`; `TunerContent(..., onModeChange: (TunerMode) -> Unit, ...)`; `val NoteWheelHeight: Dp`; strings `tuner_mode`, `mode_identify`, `mode_target`, `play_a_note`.

- [x] **Step 1: Failing tests.** `gauge cents follow the mode` (443 Hz, target 67: IDENTIFY 11.8, TARGET 211.8, null in silence); `stored tuner mode is restored`; `choosing a mode is saved`; `identify_mode_names_the_heard_note_without_the_wheel`; `identify_mode_measures_from_the_nearest_note`; `identify_mode_silence_asks_for_any_note`; `tapping_a_mode_pill_reports_the_mode`; `croatian_identify_mode_uses_the_school_names`.
- [x] **Step 2: Run** the tuner unit tests; expected: compilation failure on `TunerMode`.
- [x] **Step 3: Implement** as the spec revision's "Code changes" lists.
- [x] **Step 4: Run** `./gradlew :app:testDebugUnitTest --tests '*Tuner*'` then `TunerScreenTest` on the emulator; expected: PASS.
