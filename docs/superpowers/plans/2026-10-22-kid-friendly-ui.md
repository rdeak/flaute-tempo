# Kid-friendly UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Strip the metronome and tuner screens down to what a primary-school child needs and restyle them to be big, round, colourful and friendly, without touching the audio engine or domain.

**Architecture:** All changes are in `app/src/main/java/hr/bertlab/fluteassistant/ui/**` and `res/values*/strings.xml`. The stateless `MetronomeContent` / `TunerContent` composables keep their role as the tested surface; they lose the callbacks for hidden features. The ViewModels keep every method; `MetronomeViewModel` only resets the now-invisible options on load.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 (BOM 2026.09.00), JUnit 4 unit tests, Compose UI tests on the headless emulator `Medium_Phone_API_37.0` (boot with `-no-audio`).

**Spec:** `docs/superpowers/specs/2026-10-22-kid-friendly-ui-design.md`

## Global Constraints

- No changes under `audio/`, `domain/`, `data/`, `playback/`, `di/`; the ViewModel change is the one line the spec names.
- Every user-visible string exists in `values/strings.xml` and `values-hr/strings.xml`; Croatian uses the informal singular ("Odsviraj", not "Odsvirajte").
- No new dependencies, fonts or drawables. Emoji are plain text in the system font.
- `config_summary`, `subdivision_*`, `sound_*` strings and `Labels.summary()` stay (notification + `LabelsTest`).
- Dynamic colour off: `FluteAssistantTheme(dynamicColor = false)` is the default.
- Hold-to-repeat: first repeat after 400 ms, then every 80 ms.
- Tempo feel bands: <60 🐌, 60–89 🐢, 90–119 🚶, 120–159 🏃, ≥160 🚀.

## Review Focus

1. A stored time signature of 5/4 or 6/8 (dev device): nothing selected, dots show 5 or 6, choosing 3/4 recovers. Test in Task 3 (`hidden_time_signature_shows_no_selection`).
2. Holding + at 300 BPM or − at 30: the ViewModel clamps; the button must not crash or spin. Covered by the existing `MetronomeConfig.withBpm` clamp; no UI test (timing-based), checked by hand on the emulator in Task 5.
3. A stored subdivision or sound from before the change keeps clicking with no way to switch it off. Test in Task 3 (`hidden options are reset on load` in `MetronomeViewModelTest`).
4. Tuner with a reading more than 50 cents off: needle pins at the edge, status still "Too low"/"Too high". Existing gauge clamps; status test in Task 4 (`far_off_note_shows_too_low`).
5. Croatian note names without octave: B natural must read "H", B flat "B". Test in Task 2 (`LabelsTest.note_names_without_octave_follow_the_locale`).

---

### Task 1: Playful theme

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/theme/Color.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/theme/Theme.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/theme/Type.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/theme/Shape.kt`

**Interfaces:**
- Produces: `FluteAssistantTheme(darkTheme, dynamicColor = false, content)`; `InTuneGreen`, `SlightlyOffAmber`, `OutOfTuneRed` unchanged; `AppShapes: Shapes`.

- [ ] **Step 1: Replace the palette in `Color.kt`** with the light set (coral `0xFFF4543A` primary / white on it, teal `0xFF1FA8A0` secondary, yellow `0xFFFFC43D` tertiary / `0xFF3D2A00` on it, cream `0xFFFFF9F0` surface and background, navy `0xFF2B2D42` on-surface, peach `0xFFFFDFC4` surface-variant, `0xFF6B5A4E` on it, `0xFFFFDCD1` primary container / `0xFF5A1A08` on it, `0xFFD3F3F1` secondary container / `0xFF00403D`, `0xFFFFF0C2` tertiary container / `0xFF3D2A00`) and the dark set (coral `0xFFFF8F73` / `0xFF4A1507`, teal `0xFF5ED3CB` / `0xFF00403D`, yellow `0xFFFFD66B` / `0xFF3D2A00`, surface `0xFF1E1B2E`, on-surface `0xFFF4EFEA`, surface-variant `0xFF3A3550` / `0xFFCFC6DA`, containers `0xFF7A2A14`/`0xFFFFDCD1`, `0xFF0E5A56`/`0xFFD3F3F1`, `0xFF5C4300`/`0xFFFFF0C2`). Keep the three semantic tuning colours.
- [ ] **Step 2: Build the colour schemes in `Theme.kt`** with `lightColorScheme(...)` / `darkColorScheme(...)` using every value above, default `dynamicColor = false`, and pass `shapes = AppShapes`. Remove the dynamic-colour branch entirely (the Build check goes with it).
- [ ] **Step 3: Create `Shape.kt`** with `val AppShapes = Shapes(extraSmall = RoundedCornerShape(12.dp), small = RoundedCornerShape(16.dp), medium = RoundedCornerShape(24.dp), large = RoundedCornerShape(32.dp), extraLarge = RoundedCornerShape(40.dp))`.
- [ ] **Step 4: Enlarge `Type.kt`**: `displayLarge` 84 sp bold / 92 line height, `titleLarge` 24 sp bold, `titleMedium` 20 sp semi-bold, `bodyLarge` 18 sp / 26 line height, `labelLarge` 16 sp semi-bold.
- [ ] **Step 5: Build** — Run: `./gradlew :app:compileDebugKotlin -q`; expected: BUILD SUCCESSFUL.
- [ ] **Step 6: Commit** — `git add app/src/main/java/hr/bertlab/fluteassistant/ui/theme && git commit -m "feat(ui): playful fixed palette, rounded shapes, bigger type"`.

### Task 2: Strings, tempo feel and note names

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-hr/strings.xml`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/metronome/MetronomeUiState.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/common/Labels.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/ui/metronome/MetronomeViewModelTest.kt`, `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/common/LabelsTest.kt`

**Interfaces:**
- Produces: `enum class TempoFeel(val emoji: String, @StringRes val labelRes: Int) { VERY_SLOW, SLOW, MEDIUM, FAST, SUPER_FAST }`, `fun tempoFeel(bpm: Int): TempoFeel`, `fun noteName(resources: Resources, midi: Int): String` + `@Composable fun noteName(midi: Int): String`.
- String ids used by later tasks: `tab_metronome`, `tab_tuner`, `play`, `stop`, `decrease_tempo`, `increase_tempo`, `tempo_dial`, `time_signature`, `feel_very_slow`, `feel_slow`, `feel_medium`, `feel_fast`, `feel_super_fast`, `msg_audio_unavailable`, `msg_save_failed`, `msg_delete_failed`, `play_a_note`, `in_tune`, `too_low`, `too_high`, `mic_permission_title`, `mic_permission_body`, `allow_microphone`, `open_settings`, plus the kept `config_summary`, `subdivision_*`, `sound_*`, `notification_*`, `note_names`.

- [ ] **Step 1: Write the failing unit test** replacing `tempo marking follows the bpm` in `MetronomeViewModelTest`:
  ```kotlin
  @Test
  fun `tempo feel follows the bpm`() {
      assertEquals(TempoFeel.VERY_SLOW, tempoFeel(50))
      assertEquals(TempoFeel.SLOW, tempoFeel(60))
      assertEquals(TempoFeel.MEDIUM, tempoFeel(90))
      assertEquals(TempoFeel.FAST, tempoFeel(120))
      assertEquals(TempoFeel.SUPER_FAST, tempoFeel(160))
      assertEquals("🐢", TempoFeel.SLOW.emoji)
      assertNull(runCatching { tempoFeel(-1) }.exceptionOrNull())
  }
  ```
- [ ] **Step 2: Run it** — `./gradlew :app:testDebugUnitTest --tests '*MetronomeViewModelTest*' -q`; expected: compilation failure (`TempoFeel` unresolved).
- [ ] **Step 3: Implement `TempoFeel` and `tempoFeel(bpm)`** in `MetronomeUiState.kt`, deleting `tempoMarking`.
- [ ] **Step 4: Rewrite both `strings.xml` files.** Remove: `bpm`, `tap`, `tap_hint`, `start`, `subdivision`, `sound`, `volume`, `presets`, `current_settings`, `preset_name`, `preset_name_hint`, `save`, `no_presets`, `delete_preset`, `delete_preset_title`, `delete_preset_body`, `delete`, `cancel`, `reference_pitch`, `lower_reference`, `raise_reference`, `listening`. Add `play` ("Let's play!" / "Idemo!") and the five `feel_*` strings from the spec table. Reword (en / hr): `stop` "Stop" / "Stani"; `play_a_note` "👂 Play a note" / "👂 Odsviraj ton"; `in_tune` "🎉 Perfect!" / "🎉 Savršeno!"; `too_low` "⬇️ Too low" / "⬇️ Prenisko"; `too_high` "⬆️ Too high" / "⬆️ Previsoko"; `mic_permission_title` "Can I use the microphone? 🎤" / "Smijem li koristiti mikrofon? 🎤"; `mic_permission_body` "I need to hear your flute to help you tune. Nothing is recorded or sent anywhere." / "Trebam čuti tvoju flautu da ti pomognem ugoditi. Ništa se ne snima niti šalje."; `allow_microphone` "Yes, allow" / "Da, dopusti"; `msg_audio_unavailable` "Oops, the sound is busy right now. Try again in a moment." / "Ups, zvuk je trenutno zauzet. Pokušaj opet za trenutak."; `msg_save_failed` "Oops, that could not be saved." / "Ups, to se nije moglo spremiti."; `msg_delete_failed` "Oops, that could not be deleted." / "Ups, to se nije moglo izbrisati."; `tempo_dial` "Tempo ring, drag around it to change the speed" / "Prsten tempa, povuci po njemu za promjenu brzine"; `time_signature` "Beats in a bar" / "Dobe u taktu".
- [ ] **Step 5: Add `noteName`** to `Labels.kt` (pitch class only, same `note_names` array) and the instrumented test in `LabelsTest`:
  ```kotlin
  @Test
  fun note_names_without_octave_follow_the_locale() {
      assertEquals("B", noteName(resources(Locale.ENGLISH), 71))
      assertEquals("H", noteName(resources(Locale("hr")), 71))
      assertEquals("A♯", noteName(resources(Locale.ENGLISH), 70))
      assertEquals("B", noteName(resources(Locale("hr")), 70))
  }
  ```
- [ ] **Step 6: Run unit tests** — `./gradlew :app:testDebugUnitTest -q`; expected: BUILD SUCCESSFUL (the screens still compile because Task 3 and 4 have not removed their callbacks yet; if a removed string breaks compilation, that reference belongs to a later task and is replaced there — do Tasks 2–4 before building if needed).
- [ ] **Step 7: Commit** — `git commit -am "feat(ui): kid-friendly copy, tempo feel words, octave-free note names"`.

### Task 3: Metronome screen

**Files:**
- Modify: `ui/metronome/MetronomeScreen.kt`, `ui/metronome/TempoDial.kt`, `ui/metronome/BeatIndicator.kt`, `ui/metronome/MetronomeViewModel.kt` (`initialState()` only), `ui/common/AppIcons.kt`
- Delete: `ui/metronome/PresetsSheet.kt`
- Test: `app/src/androidTest/.../ui/metronome/MetronomeScreenTest.kt`, `app/src/test/.../ui/metronome/MetronomeViewModelTest.kt`

**Interfaces:**
- Produces: `MetronomeContent(state, onTogglePlayback: () -> Unit, onBpmChange: (Int) -> Unit, onAdjustBpm: (Int) -> Unit, onTimeSignatureChange: (TimeSignature) -> Unit, modifier)`; `TempoDial(bpm, onBpmChange, flash, flashColor, modifier, minBpm, maxBpm)` (unchanged signature); `BeatIndicator` unchanged; private `HoldToRepeatButton(icon, description, onStep)`; private `TimeSignatureChoice(selected, onSelect)` showing `listOf(TWO_FOUR, THREE_FOUR, FOUR_FOUR)`.

- [ ] **Step 1: Rewrite `MetronomeScreenTest`** to the new `MetronomeContent` signature with these tests: `start_button_toggles_playback_and_reflects_the_playing_state` (click "Let's play!", expect "Stop" after `isPlaying = true`); `step_buttons_adjust_the_tempo` (click content descriptions "Increase tempo" then "Decrease tempo", expect `listOf(1, -1)`); `time_signature_choice_reaches_the_callback` (click "3/4" → `THREE_FOUR`); `hidden_time_signature_shows_no_selection` (state with `SIX_EIGHT`: `onNodeWithText("6/8").assertDoesNotExist()`, `onAllNodes(isSelected()).assertCountEquals(0)`); `croatian_system_language_shows_croatian_labels` ("Idemo!", "Dobe u taktu" content description present via `onNodeWithContentDescription`, one "Povećaj tempo").
- [ ] **Step 2: Add the ViewModel test** in `MetronomeViewModelTest`:
  ```kotlin
  @Test
  fun `hidden options are reset on load`() = runTest(dispatcher) {
      settings.config = MetronomeConfig(bpm = 96, subdivision = Subdivision.TRIPLET, sound = SoundProfile.BELL)
      val vm = createViewModel()
      assertEquals(96, vm.state.value.config.bpm)
      assertEquals(Subdivision.NONE, vm.state.value.config.subdivision)
      assertEquals(SoundProfile.WOODBLOCK, vm.state.value.config.sound)
  }
  ```
- [ ] **Step 3: Run the unit test** — expected: FAIL (`TRIPLET != NONE`).
- [ ] **Step 4: Change `initialState()`** so the settings branch returns `settings.loadMetronomeConfig().copy(subdivision = Subdivision.NONE, sound = SoundProfile.WOODBLOCK)` with a comment naming the spec reason. Run the unit tests; expected: PASS.
- [ ] **Step 5: Rewrite `MetronomeScreen.kt`** per the spec layout: no header; `BeatIndicator`; `TempoDial` 250 dp; row of two `HoldToRepeatButton`s (72 dp, `FilledTonalIconButton`, `MutableInteractionSource` + `collectIsPressedAsState()` + `LaunchedEffect(pressed)` loop: `delay(400)` then `while (true) { onStep(); delay(80) }`; `onClick = onStep`); full-width play pill 72 dp (`Button`, `CircleShape`, play icon 36 dp + `play`/`stop` text in `titleLarge`; playing = `errorContainer`/`onErrorContainer` as today); `TimeSignatureChoice` as three 64 dp circles via `Surface(selected, onClick, shape = CircleShape, role = Role.RadioButton)` wrapped in `Row(Modifier.selectableGroup().semantics { contentDescription = time_signature })`. Drop the presets sheet state, notification-permission logic stays.
- [ ] **Step 6: Simplify `TempoDial.kt`**: remove the tick loop and `tickColor`; stroke 22 dp; centre column = bpm in `displayLarge` then `Text("${feel.emoji} ${stringResource(feel.labelRes)}", titleMedium, primary)` where `feel = tempoFeel(bpm)`.
- [ ] **Step 7: `BeatIndicator.kt`**: diameters 36 / 30 / 24 dp, spacing 18 dp.
- [ ] **Step 8: Delete `PresetsSheet.kt`; prune `AppIcons`** to Add, Remove, PlayArrow, Stop, Mic, Metronome, TuningFork.
- [ ] **Step 9: Build and run unit tests** — `./gradlew :app:testDebugUnitTest -q`; expected: BUILD SUCCESSFUL.
- [ ] **Step 10: Commit** — `git add -A app && git commit -m "feat(ui): simplified, playful metronome screen"`.

### Task 4: Tuner screen

**Files:**
- Modify: `ui/tuner/TunerScreen.kt`, `ui/tuner/TuningGauge.kt`
- Test: `app/src/androidTest/.../ui/tuner/TunerScreenTest.kt`

**Interfaces:**
- Produces: `TunerContent(state, onRequestPermission, onOpenSettings, modifier)`; `TuningGauge(cents, modifier, inTuneCents)` unchanged signature.

- [ ] **Step 1: Rewrite `TunerScreenTest`**: `asks_for_the_microphone_until_permission_is_granted` (click "Yes, allow"); `shows_the_note_and_too_high` (443 Hz → "A" displayed, "⬆️ Too high" displayed, "+12 ¢" does not exist); `far_off_note_shows_too_low` (`NoteMath.frequencyToNote(430.0)` → "⬇️ Too low"); `in_tune_note_celebrates` (440.0 → "🎉 Perfect!"); `croatian_system_language_uses_croatian_note_names` (493.88 Hz, hr → "H" and "🎉 Savršeno!").
- [ ] **Step 2: Rewrite `TunerContent`** per spec: no header row, no reference control, no Hz/cents row, no neighbours, no level bar. Note text = `noteName(it.midi)` or "…" at 0.4 alpha; status text in `titleLarge` with `AnimatedContent`-free bounce: `animateFloatAsState` scale 1.15→1 keyed on `status == IN_TUNE` via `Modifier.graphicsLayer`. `PermissionCard` keeps its structure with the new strings, icon 64 dp, `large` shape.
- [ ] **Step 3: Simplify `TuningGauge.kt`**: delete ticks, labels, `textMeasurer`; stroke 26 dp; needle 6 dp with 12 dp hub.
- [ ] **Step 4: Build** — `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin -q`; expected: BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** — `git commit -am "feat(ui): simplified, playful tuner screen"`.

### Task 5: Verify on the emulator

- [ ] **Step 1: Unit tests** — `./gradlew :app:testDebugUnitTest -q`; expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Instrumented tests** — with the emulator booted (`ANDROID_SERIAL=emulator-5554`), `./gradlew :app:connectedDebugAndroidTest -q`; expected: BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 3: Screenshots** — install the debug APK, grant RECORD_AUDIO, capture both tabs in en and hr (`cmd locale set-app-locales`), light and dark (`cmd uimode night yes`); check nothing essential is below the fold on the 1080×2400 AVD and hold-to-repeat steps the tempo (`uiautomator` long press via `input swipe x y x y 1500`).
- [ ] **Step 4: Commit** any fixes and update `docs/release/play-listing` notes if copy changed (none expected).
