# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Flute Assistant: a single-module Android app (metronome, chromatic tuner and a note reading
game) for primary-school children learning the flute. Package and applicationId are `hr.bertlab.fluteassistant`; the
project folder keeps the old name FluteTempo. Kotlin, Jetpack Compose + Material 3, Hilt, Room,
coroutines/Flow. Shipped languages: English (default resources) and Croatian (`values-hr`).

## Git rules

Do not commit, push, or create git worktrees in this repository. Leave changes in the working
tree for the user to review and commit.

## Commands

Everything goes through the wrapper (Gradle 9.8.2, sha256-pinned in
`gradle-wrapper.properties`; the daemon runs on JDK 25 via the foojay toolchain). The
configuration cache is on.

```sh
./gradlew assembleDebug                                   # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest                               # JVM unit tests (app/src/test)
./gradlew :app:testDebugUnitTest --tests '*NoteMathTest*' # one test class
./gradlew :app:testDebugUnitTest --tests '*MetronomeViewModelTest.tempo feel follows the bpm'  # one test
./gradlew lintRelease
./gradlew :app:connectedDebugAndroidTest                  # Compose UI + resource tests (app/src/androidTest)
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.tuner.TunerScreenTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.notes.NotesScreenTest
./gradlew testDebugUnitTest lintRelease bundleRelease     # pre-upload check (docs/release/README.md)
```

Instrumented-test notes: pass one class at a time (a comma-separated list only ran the first
class here); the task uninstalls the app when it finishes, so `adb install -r` again before any
adb-driven checks; uninstall a release-signed build first or the debug install fails.

### Emulator

A physical phone is often attached over USB, so pin adb to the emulator with
`ANDROID_SERIAL=emulator-5554`. The only AVD is `Medium_Phone_API_37.0`. `-no-audio` is
required: with host audio on, the emulator crashed about 30 s after the metronome started. That
also means the audio output itself can only be judged on a real device.

```sh
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_37.0 -no-window -no-audio -gpu swiftshader_indirect -no-boot-anim &
adb shell pm grant hr.bertlab.fluteassistant android.permission.RECORD_AUDIO
adb shell cmd locale set-app-locales hr.bertlab.fluteassistant --user 0 --locales hr   # switch the app to Croatian (the in-app flags do the same)
adb shell cmd uimode night yes                                                         # dark mode
adb exec-out screencap -p > shot.png
```

The notification-permission dialog appears on the first metronome start (API 33+);
`adb shell input keyevent 4` dismisses it.

### Release

Versions live in `gradle.properties` (`fluteAssistant.versionCode` must increase on every Play
upload; CI can pass `-PfluteAssistant.versionCode=N`). Release signing reads the
`FLUTE_ASSISTANT_STORE_FILE / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD` env vars, then the
gitignored `keystore.properties`; without either the release build is produced unsigned. Never
read or print the keystore password. Release builds use whole-app R8 (a package-scoped
optimisation crashed at startup inside androidx.startup). Process, Play Console answers and the
Secrets Manager backup recipe are in `docs/release/`. Launcher PNGs and store graphics come from
`java -Djava.awt.headless=true tools/IconGen.java /tmp/icons`.

## Build wiring (AGP 9)

AGP 9.4.1 uses built-in Kotlin: there is no `kotlin-android` plugin and no kapt. The root
`build.gradle.kts` puts the Kotlin Gradle plugin on the buildscript classpath to raise AGP's
bundled Kotlin to the catalog's `kotlin` version, and the Compose compiler plugin shares that
same version ref; keep the two equal. KSP is the standalone 2.3.x line; Hilt must stay >= 2.59.
Room exports schemas to `app/schemas` (database version 1, destructive fallback migration for
now). `localeFilters` strips every library locale except en and hr.

`androidx.appcompat` is in the build only for the in-app language switch:
`AppCompatDelegate.setApplicationLocales` is the per-app-language backport below Android 13, so
`MainActivity` is an `AppCompatActivity`, the window theme must stay an AppCompat theme
(`Theme.AppCompat.DayNight.NoActionBar`) and the manifest declares
`AppLocalesMetadataHolderService` with `autoStoreLocales`. The choice is stored by the platform
(system on 13+, AppCompat below), not by `SettingsStore`.

## Architecture

Layered by package under `app/src/main/java/hr/bertlab/fluteassistant/`, with the Hilt modules
in `di/` binding each interface to its Android implementation:

- `domain/` is pure Kotlin: `MetronomeConfig` (bpm, `TimeSignature` with per-beat accents,
  `Subdivision` offsets, `SoundProfile`), `NoteMath` (frequency to MIDI/cents),
  `TapTempoCalculator`, `StaffNotes` (the note reading game's fifteen notes and their staff
  places), the `PresetRepository` and `SettingsStore` interfaces, preset use cases.
- `data/` implements them: Room (`PresetEntity`, `PresetDao`, enum `Converters` that fall back
  to defaults on unknown names) and `SharedPreferencesSettingsStore` for last-used settings.
- `audio/` holds the metronome and tuner engines; `core/MonotonicClock` is a fake-able clock.
- `playback/` keeps the metronome alive off screen (foreground service).
- `ui/` is Compose + MVVM: `StateFlow` UI state plus intent methods on the ViewModels.

### Metronome audio path

`MetronomeConfig` -> `ClickScheduler` (sample-accurate click timeline; positions accumulate as
doubles so there is no drift, and `retime` moves the already-scheduled click when only the tempo
changes) -> `MetronomeRenderer` (mixes active click "voices" into PCM blocks, caches samples
synthesised by `ClickSynth`, passes everything through `SoftLimiter`) ->
`AudioTrackMetronomePlayer` (a dedicated URGENT_AUDIO thread doing blocking float-PCM writes to a
low-latency streaming `AudioTrack`; stops on audio-focus loss and ACTION_AUDIO_BECOMING_NOISY).
Scheduler, renderer and synth have no Android dependencies and are unit-tested on the JVM.

The player is an app-wide singleton that outlives any screen. Its contract (`MetronomePlayer`):
`start()` returns a session id (or `NO_SESSION` when focus is refused); every `BeatEvent` carries
that id plus `audibleAtNanos` derived from `AudioTrack.getTimestamp`, so the UI delays the beat
flash until the click is actually heard and ignores events from stale sessions; `playback` is a
StateFlow that a newly created ViewModel or the service adopts; `sessionEnded` is an event flow
rather than state because a session can start and fail within the same frame.

### Background playback

`MetronomeViewModel` calls `BackgroundPlayback.start()` after a successful start and never stops
playback in `onCleared`. `MetronomeService` owns nothing: it promotes the process with a
media-playback notification, mirrors `player.playback` into that notification, stops itself when
the flow goes null, and calls `player.stop()` from the notification's Stop action and from
`onTaskRemoved`. It returns START_NOT_STICKY on purpose.

### Tuner path

`AudioRecordTunerEngine` exposes a cold `Flow<PitchFrame>`: collecting opens the microphone
(UNPROCESSED source when available, 4096-sample window advanced by 2048, conflated on
Dispatchers.IO) and cancelling releases it; it throws `SecurityException` without RECORD_AUDIO.
`YinPitchDetector` (pure Kotlin: YIN plus a silence gate and period refinement for high notes)
produces the estimate; the engine uses `YinPitchDetector.forFlute`, which searches only one
semitone beyond `FluteRange` (so voices and mains hum below the flute cannot register as low
notes) and gates at `QUIET_ROOM_RMS` (about -60 dBFS, a value only a real phone can confirm).
`TunerViewModel` gates on probability (`MIN_PROBABILITY` 0.85 on top of YIN's own 0.2
threshold: two stacked confidence gates whose combined cliff sits at about 9 dB breath-noise
signal-to-noise, right where a beginner's airy tone lives; the KDoc on `MIN_PROBABILITY`
explains how to read `tuner.summary` to confirm it on a phone and what the evidence-backed
relaxation is), publishes a reading only once the last `LOCK_FRAMES` (3,
about 130 ms) agree within `LOCK_CENTS` (30) so a syllable or a bump never shows a note and a
disagreeing frame freezes the reading instead of moving it, median-smooths a 5-frame window and
holds the last reading for about 0.9 s of silence before clearing; `NoteMath.frequencyToNote`
turns the median into a `NoteReading`. Two modes (`TunerMode`, remembered through
`SettingsStore.saveTunerMode`, default `IDENTIFY`): `IDENTIFY` names the nearest note and
measures against it; `TARGET` lets the child pick the note to play in a `NoteWheel`
(`FluteRange`: c1 to c4, MIDI 60 to 96, default a1 / A4, remembered through
`SettingsStore.saveTargetNote` and clamped on load) and measures against it with
`TunerUiState.centsOffTarget` (`NoteMath.centsBetween`), so a wrong note pegs the needle
instead of being relabelled. `TunerUiState.gaugeCents` picks by mode and is what the gauge
and status line show; `TunerUiState.status` judges it against `IN_TUNE_CENTS` (10, a child's
wobble, deliberately wider than a professional tuner's 5) and `SLIGHTLY_OFF_CENTS` (20), the one
place those thresholds live (the gauge draws its zones from them). There is no tolerance
setting on purpose: one good default beats a control. `noteLabel` follows the locale's octave convention
(`bool/note_octaves_helmholtz`: scientific C4 in English, music-school c1 in Croatian).
`TunerViewModel.playTargetNote` sounds the chosen note for two seconds through
`ReferenceTonePlayer` (`ReferenceToneSynth` renders it, `AudioTrackReferenceTonePlayer` plays
it) and ignores microphone frames meanwhile. `FluteFingerings.keysFor(midi)` is the C-foot
Boehm chart (Woodwind Fingering Guide basic fingerings; no entry for MIDI 94 and 95) that
`FingeringDiagram` draws.

### Note reading path

`StaffNotes` (`domain/notes/`) is the pool: the naturals c1 to c3 (MIDI 60 to 84), fifteen
notes whose index in `ALL` is their place on the treble staff (0 = the ledger line below,
`LINE_POSITIONS` 2, 4, 6, 8, 10 are the lines, 14 = the second ledger line above);
`ledgerLinesFor` and `sanitize` (unknown or empty stored sets fall back to `DEFAULT_PRACTICE`,
c1 to g1) live there too. `NotesViewModel` (Activity-scoped; spec:
`docs/superpowers/specs/2026-10-22-note-reading-game-design.md`) runs one round of
`NotesUiState.ROUND_LENGTH` (10) notes: `onNoteTapped(midi)` judges a tap (ignored while
`solved` or `roundOver`), a right tap sets `solved`, appends to `results` (true = first tap),
plays the note through the tuner's `ReferenceTonePlayer` at `REFERENCE_TONE_VOLUME` and after
`FEEDBACK_MS` (1200) draws the next note (never the same twice in a row unless only one is
chosen) or ends the round; a wrong tap joins `wrongTaps` and zeroes the streak. `taps` counts
every tap so `accuracyPercent` (right taps over all taps) differs from `score`; `roundMillis`
is the time from the round's first tap to the tenth note found (`MonotonicClock`, so tests use
`FakeClock`), shown in the summary as m:ss by `formatRoundTime`. The chosen
notes persist through `SettingsStore.savePracticeNotes` (`practice_notes`, a string set), the last one cannot be switched off, and any change to them starts a fresh round; the sound of
a found note can be switched off (`toggleSound`, `SettingsStore.saveNoteSound`, a pill under
the picker's grid); `resetRound()` (also behind `playAgain`) throws the round away and is
called by the tab bar when the tab is left, and `stopSound()` when the screen is disposed, so
the tuner's microphone does not hear the tone. A `Random` is passed through an internal
secondary constructor for deterministic tests. `NoteStaff` draws the staff on a Canvas
(it takes all the height left in the column and sizes the places from it; it shows the staff
and its two neighbouring spaces, d1 to g2, plus the ledger-line places c1, a2, h2, c3 only as
far as a chosen note needs them, `StaffNotes.visiblePositions`; a hand-built stroked clef
path in staff spaces; small faint circles in one column at 60 % of the width, after the user
rejected a two-column split) under a column of fifteen full-width tap strips whose accessibility names
are "Staff position N" counted from the bottom, never the note, so a screen reader does not
give the answer away. The name above the staff is 64 sp (not the tuner's 84) to make room.

### UI conventions

- One Activity, which sets `volumeControlStream` to the media stream so the volume keys adjust
  the app's sound even while nothing is playing. `MainScreen` hosts a top app bar (app name plus the `LanguageSwitch`: two flag
  buttons over `ui/common/AppLanguage`; selecting one calls
  `AppCompatDelegate.setApplicationLocales`, which re-creates the Activity) and a bottom bar
  switching between the three tabs (`AppTab`: metronome, tuner, notes). All ViewModels are
  Activity-scoped so state and playback survive tab switches, rotation and that re-creation;
  the one exception is the game's round, which the tab bar resets (`NotesViewModel.resetRound`)
  when the notes tab is left, so the child always comes back to a zero score.
- Each screen is split into a stateful `MetronomeScreen` / `TunerScreen` / `NotesScreen`
  (ViewModel, permissions, lifecycle) and a stateless `MetronomeContent` / `TunerContent` /
  `NotesContent` taking state plus callbacks. The `*Content` composables are what the Compose
  UI tests render, wrapped in the test-only `LocaleProvider` to pin the language.
- Every user-visible string is a resource in both `values/strings.xml` and
  `values-hr/strings.xml`. Domain enums stay language-free; `ui/common/Labels.kt` maps them to
  string resources and builds note names from the `note_names` array (Croatian uses the German
  H / B convention). Croatian copy addresses the child in the informal singular.
- Fixed coral / teal / yellow palette with dynamic colour off. Icons are hand-built
  `ImageVector`s in `AppIcons` so the extended Material icon set is not a dependency.
- `ui/common/ValueWheel` is the snapping three-row drum behind both the metronome's
  `TempoWheel` and the tuner's `NoteWheel`. Its content description stays on the `LazyColumn`
  node: the UI tests scroll it with `performScrollToIndex`.

### Kid-friendly UI constraint

The UI was deliberately reduced (spec: `docs/superpowers/specs/2026-10-22-kid-friendly-ui-design.md`,
follow-up: `docs/superpowers/specs/2026-10-22-metronome-controls-and-language-design.md`)
to beat dots, a tempo ring (drag it, or tap the number to get an in-place three-value
`TempoWheel`; there are no −/+ buttons), four "level" pills (`SkillLevel` in
`MetronomeUiState.kt`: Beginner 60, Learner 80, Star 100, Master 120; emoji plus a word, never
the number, filled only when the tempo matches exactly), one beats-in-a-bar stepper pill
(◀ 4/4 ▶, wrapping through every `TimeSignature` in enum order; the beat dots shrink above
six beats) and one play button (no volume control: the phone's buttons set loudness); the tuner shows either the huge name of the
heard note or the note wheel (the note the child chose to play, on a peach band with faded
neighbours and a speaker button that sounds it; spec:
`docs/superpowers/specs/2026-10-22-tuner-target-note-design.md`), the fingering diagram
for that note, a numberless gauge, one status line and, at the bottom, the two mode pills
("Which note?" / "Tune to a note"); the notes tab shows one row with the huge note name (the whole
question; the score once the round is over) and a small round checklist button at its end
that swaps the staff for a grid of fifteen note pills plus the sound toggle in place (no
dialog; the button becomes a tick to close), the staff, one feedback line, then ten round
dots at the bottom (score and accuracy without a number, plus a "🔥 3" streak badge from
two in a row), and the numbers (score out of 10, best streak, accuracy) only in the
end-of-round summary. The user rejected a wider layout (−/+ beside the ring,
a ten-button signature scroller, flags in the content area) as too packed: keep the screen
this sparse. Tap tempo, subdivisions, click sounds, volume, presets and the A4 reference still
exist in the engine, data layer and ViewModels but are not exposed: `MetronomeViewModel.initialState()`
resets subdivision, sound and volume to their defaults and `TunerViewModel` starts from 440 Hz
and plays the reference tone at a fixed `REFERENCE_TONE_VOLUME`, so a stale stored value cannot
silently affect playback. Do not reintroduce numbers or jargon
(BPM, Hz, cents, Italian markings) or extra controls on the main screens; an "advanced mode"
is the intended home for those features.

## Logging and observability

`core/log/Logger` (Hilt-provided, `AndroidLogger` in production, `FakeLogger` in tests) is the
one way to log. Every line is a dotted event name plus `key=value` fields built by `LogLine`
(two decimals for floating point, `none` for null, quoted text), under one tag per subsystem
(`LogTags`: `FA.Tuner`, `FA.Metronome`, `FA.Playback`), so lines are greppable and a script
can split them on spaces. Levels: `v` per-frame detail, off unless switched on per tag at
runtime; `d` periodic summaries and state changes, on in debug builds; `i` lifecycle; `w`/`e`
failures with the exception. Guard costly `v` calls with `logger.isVerbose(tag)`. Never log
through `android.util.Log` directly, never log user-identifying data, and keep event names
stable: they are the interface a logcat session is read with.

Tuner events: `mic.open` (source, rate, buffer), `mic.close`, `tuner.permission`,
`tuner.frame` (verbose: f, p, level, `FrameDecision`), `tuner.summary` every
`TunerViewModel.SUMMARY_FRAMES` frames (counts per decision, pMin/pMax/pMean against `gate`,
levelMean, reading, target, mode), `tuner.reading` when the shown note changes,
`tuner.target`, `tuner.mode`, `tuner.reference`, `tuner.error`, `reference.error`. Metronome:
`metronome.start` / `stop` / `ended` / `focus_lost` / `noisy` / `track_open` / `track_closed`
(underruns) / `error`; playback: `playback.error`.

```sh
adb logcat -s FA.Tuner FA.Metronome FA.Playback         # everything structured, debug build
adb shell setprop log.tag.FA.Tuner VERBOSE               # per-frame tuner lines (any build)
adb logcat -v time -s FA.Tuner | grep tuner.summary     # one line a second: did frames pass the gate?
```

## Testing conventions

Unit tests use hand-written fakes from `app/src/test/.../fakes/Fakes.kt` (`FakeMetronomePlayer`,
`FakeTunerEngine`, `FakeSettingsStore`, `FakePresetRepository`, `FakeClock`,
`FakeBackgroundPlayback`, `FakeReferenceTonePlayer`); there is no mocking library. ViewModel tests run under
`StandardTestDispatcher` with `Dispatchers.setMain` and drive time with `advanceTimeBy` /
`advanceUntilIdle`. Instrumented tests cover the `*Content` composables (`MetronomeScreenTest`, `TunerScreenTest`,
`NotesScreenTest`) and locale-dependent resources (`LabelsTest`). The `config_summary`, `subdivision_*` and `sound_*` strings must stay
even though the screens no longer show them: the playback notification and `LabelsTest` use them.
