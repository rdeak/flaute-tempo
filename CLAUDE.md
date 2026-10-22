# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Flute Assistant: a single-module Android app (metronome + chromatic tuner) for primary-school
children learning the flute. Package and applicationId are `hr.bertlab.fluteassistant`; the
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
  `TapTempoCalculator`, the `PresetRepository` and `SettingsStore` interfaces, preset use cases.
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
produces the estimate. `TunerViewModel` gates on probability, median-smooths a 5-frame window and
holds the last reading for about 0.9 s of silence before clearing; `NoteMath.frequencyToNote`
turns the median into a `NoteReading`.

### UI conventions

- One Activity. `MainScreen` hosts a top app bar (app name plus the `LanguageSwitch`: two flag
  buttons over `ui/common/AppLanguage`; selecting one calls
  `AppCompatDelegate.setApplicationLocales`, which re-creates the Activity) and a bottom bar
  switching between the two tabs. Both ViewModels are Activity-scoped so state and playback
  survive tab switches, rotation and that re-creation.
- Each screen is split into a stateful `MetronomeScreen` / `TunerScreen` (ViewModel, permissions,
  lifecycle) and a stateless `MetronomeContent` / `TunerContent` taking state plus callbacks. The
  `*Content` composables are what the Compose UI tests render, wrapped in the test-only
  `LocaleProvider` to pin the language.
- Every user-visible string is a resource in both `values/strings.xml` and
  `values-hr/strings.xml`. Domain enums stay language-free; `ui/common/Labels.kt` maps them to
  string resources and builds note names from the `note_names` array (Croatian uses the German
  H / B convention). Croatian copy addresses the child in the informal singular.
- Fixed coral / teal / yellow palette with dynamic colour off. Icons are hand-built
  `ImageVector`s in `AppIcons` so the extended Material icon set is not a dependency.

### Kid-friendly UI constraint

The UI was deliberately reduced (spec: `docs/superpowers/specs/2026-10-22-kid-friendly-ui-design.md`,
follow-up: `docs/superpowers/specs/2026-10-22-metronome-controls-and-language-design.md`)
to beat dots, a tempo ring (drag it, or tap the number to get an in-place three-value
`TempoWheel`; there are no −/+ buttons), four "level" pills (`SkillLevel` in
`MetronomeUiState.kt`: Beginner 60, Learner 80, Star 100, Master 120; emoji plus a word, never
the number, filled only when the tempo matches exactly), one beats-in-a-bar stepper pill
(◀ 4/4 ▶, wrapping through every `TimeSignature` in enum order; the beat dots shrink above
six beats), one play button and a numberless volume slider; the tuner shows a big note name, a
numberless gauge and one status line. The user rejected a wider layout (−/+ beside the ring,
a ten-button signature scroller, flags in the content area) as too packed: keep the screen
this sparse. Tap tempo, subdivisions, click sounds, presets and the A4 reference still exist
in the engine, data layer and ViewModels but are not exposed: `MetronomeViewModel.initialState()`
resets subdivision and sound to their defaults and `TunerViewModel` starts from 440 Hz so a
stale stored value cannot silently affect playback. Do not reintroduce numbers or jargon
(BPM, Hz, cents, Italian markings) or extra controls on the main screens; an "advanced mode"
is the intended home for those features.

## Testing conventions

Unit tests use hand-written fakes from `app/src/test/.../fakes/Fakes.kt` (`FakeMetronomePlayer`,
`FakeTunerEngine`, `FakeSettingsStore`, `FakePresetRepository`, `FakeClock`,
`FakeBackgroundPlayback`); there is no mocking library. ViewModel tests run under
`StandardTestDispatcher` with `Dispatchers.setMain` and drive time with `advanceTimeBy` /
`advanceUntilIdle`. Instrumented tests cover the `*Content` composables and locale-dependent
resources (`LabelsTest`). The `config_summary`, `subdivision_*` and `sound_*` strings must stay
even though the screens no longer show them: the playback notification and `LabelsTest` use them.
