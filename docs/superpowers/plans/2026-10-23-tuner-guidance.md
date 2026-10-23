# Tuner Guidance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Under the tuner's status line, one quieter line tells the child what to change (roll the flute, air, fingers, come closer, play a clear note), and a note played well flat or sharp is still treated as the chosen note.

**Architecture:** `TunerUiState` grows a `hearing` input and derives one `advice` enum from status, mismatch and hearing, so the composable only maps an enum to a string; the mismatch rule gains a 70-cent near-target band. A new pure-Kotlin `HearingTracker` (tuner package) turns the per-frame level and `FrameDecision` into `Hearing` (clear / quiet unclear / loud unclear) with a tolerated breath gap and a sliding level window; `TunerViewModel` feeds it, mirrors it into the state and logs changes, and lets unsteady frames count towards the existing hold. `TunerScreen` adds an `AdviceLine` under `StatusLine`; strings change in both languages.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Hilt, JUnit 4 with `kotlinx-coroutines-test` (hand-written fakes in `app/src/test/.../fakes/Fakes.kt`), Compose UI tests on the headless emulator `Medium_Phone_API_37.0` booted with `-no-audio`.

**Spec:** `docs/superpowers/specs/2026-10-22-tuner-target-note-design.md`, section "Revision 5: say what to change" (the earlier sections describe what already exists).

## Global Constraints

- No commits: this repository's CLAUDE.md forbids the agent committing; every change stays in the working tree for the user.
- Constants, exact: `TunerUiState.NEAR_TARGET_CENTS = 70.0`; `HearingTracker.SOUND_LEVEL = 0.25f`, `CLOSE_LEVEL = 0.5f`, `UNCLEAR_FRAMES = 25`, `UNCLEAR_GAP_FRAMES = 10`. Unchanged: the detector, `MIN_PROBABILITY`, `LOCK_FRAMES`, `LOCK_CENTS`, `HOLD_FRAMES` (only what counts towards it changes).
- Copy, exact, English / Croatian (informal singular): `advice_come_closer` "Come closer to the phone" / "Dođi bliže mobitelu"; `advice_clear_note` "Play one long, clear note" / "Odsviraj jedan dug, čist ton"; `advice_roll_out` "Roll the flute out a little" / "Zakreni flautu malo od sebe"; `advice_roll_in` "Roll the flute in a little" / "Zakreni flautu malo k sebi"; `advice_faster_air` "Faster air" / "Brži zrak"; `advice_gentler_air` "Gentler air" / "Nježniji zrak"; `advice_check_fingers` "Check your fingers" / "Provjeri prste"; `heard_other_note` "🖐️ That\'s %1$s" / "🖐️ To je %1$s"; `octave_too_low` "⬇️ An octave too low" / "⬇️ Oktavu prenisko"; `octave_too_high` "⬆️ An octave too high" / "⬆️ Oktavu previsoko". `too_low_heard` and `too_high_heard` are deleted from both files.
- The advice line is plain text: `bodyLarge`, `onSurfaceVariant`, centred, exactly one line high even when empty. No meter, no number, no new control, no marker on the fingering diagram.
- Logging only through `Logger` under `LogTags.TUNER`: new event `tuner.hearing` (fields `hearing`, `levelMean`), new field `hearing` in `tuner.summary`; existing event names unchanged.
- Instrumented tests run one class at a time; `adb install -r` again after `connectedDebugAndroidTest` before any adb-driven check.

## Review Focus

1. a1 played sixty cents flat (nearest note g♯1) must read "⬇️ Too low" with "Roll the flute out a little", never "That's gis1 / Check your fingers". Tests: Task 1 `the chosen note played past the halfway point is still the chosen note`, Task 3 `a_note_played_well_flat_is_still_the_chosen_note`.
2. Speech near the phone will trigger the unclear-sound advice; it must vanish within about 0.4 s of silence and the instant a note is read. Tests: Task 2 `a breath inside the run is tolerated, a longer gap ends it`, `a shown note or a reset ends the run at once`.
3. The reference tone from the speaker must neither count as unclear sound nor leave a stale "Come closer" behind. Test: Task 2 `the reference tone neither counts as unclear sound nor leaves a stale hearing`.
4. The boundary: 69 cents off is still the chosen note, 71 is not, and the semitone neighbour in tune (100 cents) is "Check your fingers". Test: Task 1 `the chosen note played past the halfway point is still the chosen note`.
5. A wandering pitch (glissando, speech vowels after a note) must not pin the old note and its advice on screen. Test: Task 2 `a wandering pitch clears the reading after the hold`.

---

### Task 1: UI state: the near-target band, `Hearing` and `TunerAdvice`

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerUiState.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/ui/tuner/TunerUiStateTest.kt`

**Interfaces:**
- Consumes: `NoteMath.centsBetween`, `TuningStatus`, `TargetMismatch`, `TunerMode`.
- Produces: `enum class Hearing { CLEAR, QUIET_UNCLEAR, LOUD_UNCLEAR }`; `enum class TunerAdvice { NONE, COME_CLOSER, CLEAR_NOTE, ROLL_OUT, ROLL_IN, FASTER_AIR, GENTLER_AIR, CHECK_FINGERS }`; `TunerUiState.hearing: Hearing = Hearing.CLEAR` (constructor parameter, after `isPlayingReference`); `val TunerUiState.advice: TunerAdvice`; `TunerUiState.NEAR_TARGET_CENTS = 70.0`.

- [ ] **Step 1: Write the failing tests.** In `TunerUiStateMismatchTest` add a file-level helper `private fun offA4(cents: Double) = NoteMath.frequencyToNote(440.0 * 2.0.pow(cents / 1200.0))` and:

```kotlin
@Test
fun `the chosen note played past the halfway point is still the chosen note`() {
    fun mismatchAt(cents: Double) = TunerUiState(reading = offA4(cents), targetMidi = 69, mode = TunerMode.TARGET).mismatch
    assertEquals(TargetMismatch.NONE, mismatchAt(-60.0)) // nearest note is G♯4, but it is a flat A4
    assertEquals(TargetMismatch.NONE, mismatchAt(69.0))
    assertEquals(TargetMismatch.OTHER_NOTE, mismatchAt(-71.0))
    assertEquals(TargetMismatch.OTHER_NOTE, mismatchAt(-100.0)) // G♯4 in tune: the fingers
    assertEquals(TuningStatus.LOW, TunerUiState(reading = offA4(-60.0), targetMidi = 69, mode = TunerMode.TARGET).status)
}
```

Add a new class `TunerUiStateAdviceTest` in the same file, with the same `offA4` and `private fun target(reading: NoteReading?, targetMidi: Int = 69, hearing: Hearing = Hearing.CLEAR) = TunerUiState(reading = reading, targetMidi = targetMidi, mode = TunerMode.TARGET, hearing = hearing)`:

```kotlin
@Test fun `silence gives no advice unless the sound is unclear`() {
    assertEquals(TunerAdvice.NONE, target(null).advice)
    assertEquals(TunerAdvice.COME_CLOSER, target(null, hearing = Hearing.QUIET_UNCLEAR).advice)
    assertEquals(TunerAdvice.CLEAR_NOTE, target(null, hearing = Hearing.LOUD_UNCLEAR).advice)
    assertEquals(TunerAdvice.COME_CLOSER, TunerUiState(mode = TunerMode.IDENTIFY, hearing = Hearing.QUIET_UNCLEAR).advice)
}
@Test fun `the reference tone silences the advice`() {
    assertEquals(TunerAdvice.NONE, target(null, hearing = Hearing.LOUD_UNCLEAR).copy(isPlayingReference = true).advice)
}
@Test fun `flat and sharp advise rolling, in both modes`() {
    assertEquals(TunerAdvice.ROLL_OUT, target(offA4(-30.0)).advice)
    assertEquals(TunerAdvice.ROLL_IN, target(offA4(30.0)).advice)
    assertEquals(TunerAdvice.ROLL_OUT, target(offA4(-60.0)).advice) // still the chosen note
    assertEquals(TunerAdvice.ROLL_OUT, TunerUiState(reading = offA4(-30.0), mode = TunerMode.IDENTIFY).advice)
    assertEquals(TunerAdvice.NONE, target(offA4(5.0)).advice) // in tune
}
@Test fun `a reading wins over unclear sound`() {
    assertEquals(TunerAdvice.ROLL_IN, target(offA4(30.0), hearing = Hearing.LOUD_UNCLEAR).advice)
}
@Test fun `octaves advise air and any other note the fingers, in target mode only`() {
    assertEquals(TunerAdvice.FASTER_AIR, target(offA4(0.0), targetMidi = 81).advice)
    assertEquals(TunerAdvice.GENTLER_AIR, target(offA4(0.0), targetMidi = 57).advice)
    assertEquals(TunerAdvice.CHECK_FINGERS, target(offA4(0.0), targetMidi = 67).advice)
    assertEquals(TunerAdvice.NONE, TunerUiState(reading = offA4(0.0), targetMidi = 67, mode = TunerMode.IDENTIFY).advice)
}
```

- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*TunerUiState*'`; expected: compilation failure (unresolved `Hearing`, `TunerAdvice`, `hearing`).
- [ ] **Step 3: Implement in `TunerUiState.kt`.** Add `NEAR_TARGET_CENTS = 70.0` to the companion beside `IN_TUNE_CENTS`. Change `mismatch`: in target mode with a reading, `NONE` when `abs(centsOffTarget!!) <= NEAR_TARGET_CENTS`, else the existing `+12` / `-12` / other test. Add the two enums at the bottom of the file next to `TuningStatus` and `TargetMismatch`, each with a one-paragraph KDoc from the spec table. Add `hearing` to the data class and the derived `advice` with this precedence: `isPlayingReference` → `NONE`; `status == NONE` → by `hearing` (`CLEAR` → `NONE`, `QUIET_UNCLEAR` → `COME_CLOSER`, `LOUD_UNCLEAR` → `CLEAR_NOTE`); `IN_TUNE` → `NONE`; `mismatch` `OCTAVE_DOWN` → `FASTER_AIR`, `OCTAVE_UP` → `GENTLER_AIR`, `OTHER_NOTE` → `CHECK_FINGERS`; then `LOW` → `ROLL_OUT`, `HIGH` → `ROLL_IN`.
- [ ] **Step 4: Run** the same command; expected: PASS, including the untouched revision-4 cases (`452.0` Hz stays `NONE`; target 70 against A4, 100 cents, stays `OTHER_NOTE`).

### Task 2: `HearingTracker`, the ViewModel and the hold

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/HearingTracker.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerViewModel.kt` (`process`, `onFrame`, `stopListening`, `playTargetNote`, `logSummary`)
- Test: `app/src/test/java/hr/bertlab/fluteassistant/ui/tuner/HearingTrackerTest.kt` (new), `TunerViewModelTest.kt`, `TunerViewModelLoggingTest.kt`

**Interfaces:**
- Consumes: `Hearing` (Task 1), `FrameDecision`, `PitchFrame.level`.
- Produces: `class HearingTracker(soundLevel: Float = SOUND_LEVEL, closeLevel: Float = CLOSE_LEVEL, unclearFrames: Int = UNCLEAR_FRAMES, gapFrames: Int = UNCLEAR_GAP_FRAMES)` with `val hearing: Hearing` (private set), `val levelMean: Double` (mean of the sound frames in the window, `0.0` when empty), `fun onFrame(level: Float, decision: FrameDecision): Hearing`, `fun reset()`, and the four constants in its companion. `TunerUiState.hearing` is kept in step by the ViewModel.

- [ ] **Step 1: Write the failing `HearingTrackerTest`** (plain JUnit, no dispatcher), with `private val tracker = HearingTracker()` and `private fun sound(level: Float, n: Int, decision: FrameDecision = FrameDecision.SILENT) = repeat(n) { tracker.onFrame(level, decision) }`:

```kotlin
@Test fun `quiet sound without a note is noticed after about a second`() {
    sound(0.3f, HearingTracker.UNCLEAR_FRAMES - 1)
    assertEquals(Hearing.CLEAR, tracker.hearing)
    sound(0.3f, 1)
    assertEquals(Hearing.QUIET_UNCLEAR, tracker.hearing)
    assertEquals(0.3, tracker.levelMean, 1e-6)
}
@Test fun `loud unclear sound is told apart from quiet`() {
    sound(0.6f, HearingTracker.UNCLEAR_FRAMES, FrameDecision.LOW_PROBABILITY)
    assertEquals(Hearing.LOUD_UNCLEAR, tracker.hearing)
}
@Test fun `a breath inside the run is tolerated, a longer gap ends it`() {
    sound(0.3f, 20)
    sound(0f, HearingTracker.UNCLEAR_GAP_FRAMES - 1)
    sound(0.3f, 5)
    assertEquals(Hearing.QUIET_UNCLEAR, tracker.hearing)
    sound(0f, HearingTracker.UNCLEAR_GAP_FRAMES)
    assertEquals(Hearing.CLEAR, tracker.hearing)
}
@Test fun `the level is judged on the last second, so moving closer is noticed`() {
    sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
    sound(0.7f, HearingTracker.UNCLEAR_FRAMES)
    assertEquals(Hearing.LOUD_UNCLEAR, tracker.hearing)
    assertEquals(0.7, tracker.levelMean, 1e-6)
}
@Test fun `a shown note or a reset ends the run at once`() {
    sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
    assertEquals(Hearing.CLEAR, tracker.onFrame(0.4f, FrameDecision.SHOWN))
    sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
    tracker.reset()
    assertEquals(Hearing.CLEAR, tracker.hearing)
    assertEquals(0.0, tracker.levelMean, 0.0)
}
@Test fun `ignored frames and sound under the floor never start a run`() {
    sound(0.6f, HearingTracker.UNCLEAR_FRAMES, FrameDecision.IGNORED)
    sound(0.1f, 100)
    assertEquals(Hearing.CLEAR, tracker.hearing)
}
```

- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*HearingTrackerTest*'`; expected: compilation failure.
- [ ] **Step 3: Implement `HearingTracker`** as the Interfaces block. State: a count of sound frames in the run, a count of consecutive under-floor frames inside the run, and an `ArrayDeque<Float>` of the last `unclearFrames` sound-frame levels. `onFrame`: `IGNORED` → no change; `SHOWN` → `reset()`; `level >= soundLevel` → gap count to 0, push the level (drop the oldest past `unclearFrames`), count it, and once the count reaches `unclearFrames` set `hearing` to `QUIET_UNCLEAR` if `levelMean < closeLevel` else `LOUD_UNCLEAR`; otherwise (under the floor) → if a run is active and the gap count reaches `gapFrames`, `reset()`. Return `hearing`. KDoc: the spec's "unclear sound" paragraph, including that the two levels are to be confirmed from `levelMean` in `tuner.summary` on a phone.
- [ ] **Step 4: Run** the same command; expected: PASS.
- [ ] **Step 5: Write the failing ViewModel tests.** In `TunerViewModelTest` (helpers `pitched`, `silence`, `steady`, `listeningViewModel` already exist; `private fun quiet(level: Float = 0.3f) = PitchFrame(null, 0.0, level)`):

```kotlin
@Test fun `unclear sound reaches the state and clears when listening stops`() = runTest(dispatcher) {
    val vm = listeningViewModel(); advanceUntilIdle()
    repeat(HearingTracker.UNCLEAR_FRAMES) { engine.frames.tryEmit(quiet()) }
    advanceUntilIdle()
    assertEquals(Hearing.QUIET_UNCLEAR, vm.state.value.hearing)
    assertEquals(TunerAdvice.COME_CLOSER, vm.state.value.advice)
    vm.stopListening()
    assertEquals(Hearing.CLEAR, vm.state.value.hearing)
}
@Test fun `the reference tone neither counts as unclear sound nor leaves a stale hearing`() = runTest(dispatcher) {
    val vm = listeningViewModel(); advanceUntilIdle()
    repeat(HearingTracker.UNCLEAR_FRAMES) { engine.frames.tryEmit(quiet()) }
    advanceUntilIdle()
    vm.playTargetNote()
    assertEquals(Hearing.CLEAR, vm.state.value.hearing)
    repeat(HearingTracker.UNCLEAR_FRAMES) { engine.frames.tryEmit(quiet(0.6f)) } // the speaker, ignored
    advanceTimeBy(TunerViewModel.REFERENCE_TONE_MS + 1L)
    assertEquals(Hearing.CLEAR, vm.state.value.hearing)
}
@Test fun `a wandering pitch clears the reading after the hold`() = runTest(dispatcher) {
    val vm = listeningViewModel(); advanceUntilIdle()
    steady(440.0)
    repeat(TunerViewModel.HOLD_FRAMES - 1) { engine.frames.tryEmit(pitched(300.0 + it * 7)) } // a glissando: no three frames agree
    advanceUntilIdle()
    assertNotNull(vm.state.value.reading)
    engine.frames.tryEmit(pitched(300.0 + (TunerViewModel.HOLD_FRAMES - 1) * 7))
    advanceUntilIdle()
    assertNull(vm.state.value.reading)
}
```

In `TunerViewModelLoggingTest`:

```kotlin
@Test fun `the hearing is logged when it changes, with the level that decided it`() = runTest(dispatcher) {
    listeningViewModel(); advanceUntilIdle()
    repeat(HearingTracker.UNCLEAR_FRAMES) { engine.frames.tryEmit(PitchFrame(null, 0.0, level = 0.3f)) }
    advanceUntilIdle()
    val event = logger.events("tuner.hearing").single()
    assertEquals(Hearing.QUIET_UNCLEAR, event["hearing"])
    assertEquals(0.3, event["levelMean"] as Double, 1e-6)
    assertEquals(Hearing.QUIET_UNCLEAR, logger.events("tuner.summary").single()["hearing"])
}
```

- [ ] **Step 6: Run** `./gradlew :app:testDebugUnitTest --tests '*TunerViewModel*'`; expected: the four new tests FAIL (hearing stays `CLEAR`, the reading survives the glissando, no `tuner.hearing` event), everything else PASS.
- [ ] **Step 7: Implement in `TunerViewModel`.** A `private val hearingTracker = HearingTracker()`. A `private fun updateHearing(hearing: Hearing)` that, only when it differs from `_state.value.hearing`, updates the state and logs `tuner.hearing` with `"hearing" to hearing, "levelMean" to hearingTracker.levelMean` at level `d`. In `onFrame`, after `process` and the verbose line and before `summary.add`, call `updateHearing(hearingTracker.onFrame(frame.level, decision))`, so a summary logged on that frame already carries it. `logSummary` adds `"hearing" to s.hearing` after `"mode"`. `stopListening`, the start of `playTargetNote` and its completion block call `hearingTracker.reset()` then `updateHearing(Hearing.CLEAR)`. The hold: in `process`, move `silentFrames = 0` after the steadiness check and in the unsteady branch do `silentFrames++` and clear `reading` when `silentFrames >= HOLD_FRAMES` (leave `recentFrequencies` alone so a lock can still form), keeping `level` updated and returning `UNSTEADY`. Update the class KDoc's bullet list (the hearing, and that unsteadiness expires the reading like silence) and the `HOLD_FRAMES` KDoc.
- [ ] **Step 8: Run** `./gradlew :app:testDebugUnitTest --tests '*Tuner*'`; expected: PASS (the revision-4 tests `frames that disagree leave the reading where it was` and `median smoothing rejects a single outlier` still pass: three and four unsteady frames are far under `HOLD_FRAMES`).

### Task 3: Strings, labels, the advice line, the instrumented tests and the docs

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-hr/strings.xml`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/common/Labels.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/tuner/TunerScreen.kt` (`TunerContent`, `StatusLine`)
- Modify: `CLAUDE.md`
- Test: `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/tuner/TunerScreenTest.kt`

**Interfaces:**
- Consumes: `TunerAdvice`, `TunerUiState.advice`, `TunerUiState.hearing` (Task 1).
- Produces: `@StringRes fun TunerAdvice.labelRes(): Int?` in `Labels.kt` (`NONE` → `null`); `private fun AdviceLine(advice: TunerAdvice)` in `TunerScreen.kt`.

- [ ] **Step 1: Rewrite the revision-4 screen tests and add the new ones** in `TunerScreenTest` (helpers `show`, `listening`, `identifying` exist):
  - `measures_against_the_chosen_note`: expects `"🖐️ That's A4"` and `"Check your fingers"` displayed.
  - Rename `a_lower_note_than_the_chosen_one_is_named_with_the_direction` to `a_different_note_is_named_and_sent_to_the_fingers`: `listening(329.63)` shows `"🖐️ That's E4"` and `"Check your fingers"`; delete the `..._higher_note_...` twin.
  - `the_octave_above_the_chosen_note_asks_for_gentler_air`: `"⬆️ An octave too high"` and `"Gentler air"`; `..._below_...`: `"⬇️ An octave too low"` and `"Faster air"`.
  - `a_different_note_is_named_in_croatian_with_its_croatian_name`: `"🖐️ To je h1"` and `"Provjeri prste"`.
  - New `too_low_advises_rolling_out`: `listening(430.0)` (A4 about 40 cents flat) shows `"⬇️ Too low"` and `"Roll the flute out a little"`.
  - New `too_high_advises_rolling_in`: `listening(443.0)` shows `"Roll the flute in a little"`.
  - New `a_note_played_well_flat_is_still_the_chosen_note`: `listening(425.0)` (A4 sixty cents flat, nearest G♯4) shows `"⬇️ Too low"` and `"Roll the flute out a little"`, and `"🖐️ That's G♯4"` does not exist.
  - New `quiet_unclear_sound_asks_to_come_closer`: `listening(null).copy(hearing = Hearing.QUIET_UNCLEAR)` shows `"👂 Play A4"` and `"Come closer to the phone"`.
  - New `loud_unclear_sound_asks_for_a_clear_note`: `identifying(null).copy(hearing = Hearing.LOUD_UNCLEAR)` shows `"👂 Play a note"` and `"Play one long, clear note"`.
  - New `in_tune_gives_no_advice`: `listening(440.0)` shows `"🎉 Perfect!"`, and `"Roll the flute out a little"`, `"Roll the flute in a little"`, `"Check your fingers"` do not exist.
  - New `croatian_advice_uses_the_informal_form`: `listening(430.0)` with `Locale("hr")` shows `"⬇️ Prenisko"` and `"Zakreni flautu malo od sebe"`.
- [ ] **Step 2: Add the strings** from Global Constraints to both files (the advice strings after `octave_too_high`, with a comment "Advice line under the status (ui/tuner/TunerScreen.kt)"), change `octave_too_low` / `octave_too_high`, add `heard_other_note`, delete `too_low_heard` / `too_high_heard`. Mind the apostrophe escape in `"🖐️ That\'s %1$s"`.
- [ ] **Step 3: Add `TunerAdvice.labelRes()`** to `Labels.kt` after `TunerMode.labelRes()`, mapping each value to its `R.string.advice_*` and `NONE` to `null`.
- [ ] **Step 4: Change `TunerScreen.kt`.** `StatusLine`: `LOW` → `OCTAVE_DOWN` gives `octave_too_low`, `OTHER_NOTE` gives `heard_other_note(heardNote)`, else `too_low`; `HIGH` likewise with `octave_too_high`, `heard_other_note`, `too_high`. Add `AdviceLine(advice)`: a `Text` of `advice.labelRes()?.let { stringResource(it) } ?: ""`, `bodyLarge`, `onSurfaceVariant`, `TextAlign.Center`, `minLines = 1`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`, `fillMaxWidth`. In `TunerContent` place `Spacer(4.dp)` then `AdviceLine(state.advice)` right after `StatusLine`, and shrink the spacer before `TunerModeChoice` from 32 dp to 20 dp. Update the `TunerContent` and `StatusLine` KDocs (one status line plus one advice line; the octave and other-note wording moved to the advice line).
- [ ] **Step 5: Build** `./gradlew assembleDebug` and `./gradlew :app:compileDebugAndroidTestKotlin`; expected: success, no unresolved string references.
- [ ] **Step 6: Run the instrumented class** on the emulator (`$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_37.0 -no-window -no-audio -gpu swiftshader_indirect -no-boot-anim &`, wait for boot, `ANDROID_SERIAL=emulator-5554`): `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.tuner.TunerScreenTest`; expected: all pass. Then `...class=hr.bertlab.fluteassistant.ui.common.LabelsTest`; expected: pass (it does not use the changed strings, this proves it).
- [ ] **Step 7: Update `CLAUDE.md`** in three places: in "Tuner path", after the sentence on `IN_TUNE_CENTS` / `SLIGHTLY_OFF_CENTS`, add that in target mode a reading within `NEAR_TARGET_CENTS` (70) of the chosen note is still that note (`TargetMismatch.NONE`), that `HearingTracker` turns level plus `FrameDecision` into `Hearing` (sound floor `SOUND_LEVEL`, quiet/loud at `CLOSE_LEVEL`, after `UNCLEAR_FRAMES` sound frames, a gap of `UNCLEAR_GAP_FRAMES` tolerated), that `TunerUiState.advice` (`TunerAdvice`) is derived from status, mismatch and hearing and shown on one advice line under the status, and that unsteady frames now count towards `HOLD_FRAMES`; in "Kid-friendly UI constraint", change "one status line" to "one status line and, under it, one quieter advice line that says what to change (roll the flute, air, fingers, come closer, a clear note), never a number"; in "Logging and observability", add `tuner.hearing` (hearing, levelMean) to the tuner events and `hearing` to the `tuner.summary` fields.

### Task 4: Whole-feature verification

**Files:** none changed.

- [ ] **Step 1: Run** `./gradlew testDebugUnitTest`; expected: BUILD SUCCESSFUL, no failures.
- [ ] **Step 2: Run** `./gradlew lintRelease`; expected: BUILD SUCCESSFUL (no unused-resource error for the deleted strings, no missing translation).
- [ ] **Step 3: Screenshots.** `adb install -r app/build/outputs/apk/debug/app-debug.apk`, grant `RECORD_AUDIO`, open the tuner tab, `adb exec-out screencap -p > shot.png` in English and (after `cmd locale set-app-locales ... --locales hr`) Croatian, light and dark (`cmd uimode night yes`). Expected: the advice line's empty height sits between the status line and the pills in silence, nothing overlaps, and the pills are still fully visible without scrolling on the emulator's screen. The emulator has no microphone, so the non-silent states are proven by the instrumented tests, not by screenshots.
- [ ] **Step 4: Report** the test and lint output and the four screenshots to the user; leave all changes uncommitted.
