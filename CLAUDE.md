# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Flute Assistant: a single-module Android app (metronome, chromatic tuner, a note reading
game and a digital flute) for primary-school children learning the flute. Package and applicationId are `hr.bertlab.fluteassistant`; the
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
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.flute.FluteScreenTest
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

Versions live in `gradle.properties`. Pushing a new `fluteAssistant.versionName` to `main` makes
`.github/workflows/release.yml` tag `v<name>` and publish a GitHub Release with the signed bundle,
APK and mapping file; every push to `main` runs the full check (`testDebugUnitTest lintRelease
bundleRelease assembleRelease`, signed; no instrumented tests). Three jobs (build, then play,
then release; only a release run keeps a 7-day artifact for the later two) so "Re-run failed
jobs" repeats only what failed. `tools/release.sh` derives the versionCode from the name (major * 10000 +
minor * 100 + patch), checks both outputs carry the upload certificate (a build without the
secrets comes out unsigned on purpose, so the check is what stops it being published), writes
the notes from the git log, and `tools/release.sh secrets` sets the four signing secrets from
`keystore.properties` without showing them. A release also uploads the bundle to the Play
internal testing track through Workload Identity Federation (no key file; repository variables
`PLAY_WORKLOAD_IDENTITY_PROVIDER` and `PLAY_SERVICE_ACCOUNT`, both required); closed test and
production stay manual. Release signing reads the
`FLUTE_ASSISTANT_STORE_FILE / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD` env vars, then the
gitignored `keystore.properties`; without either the release build is produced unsigned. Never
read or print the keystore password. Release builds use whole-app R8 (a package-scoped
optimisation crashed at startup inside androidx.startup). Process, Play Console answers and the
Secrets Manager backup recipe are in `docs/release/`. Launcher PNGs and store graphics come from
`java -Djava.awt.headless=true tools/IconGen.java /tmp/icons`. Store screenshots (one per tab, en and hr,
padded to 9:16) come from `tools/play-screenshots.sh` with the emulator up and a debug APK built.

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
  `TapTempoCalculator`, `TunerMode`, `FluteRange` and `FluteFingerings` (`domain/tuner/`),
  `StaffNotes` (the note reading game's fifteen notes and their staff places), `Tune` (the
  digital flute's tune), the `PresetRepository` and `SettingsStore` interfaces, preset use cases.
- `data/` implements them: Room (`PresetEntity`, `PresetDao`, enum `Converters` that fall back
  to defaults on unknown names) and `SharedPreferencesSettingsStore` for last-used settings.
- `audio/` holds the metronome, tuner and flute-tone engines; `core/MonotonicClock` is a
  fake-able clock and `core/log/` the logger.
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
holds the last reading for about 0.9 s of silence or unsteadiness before clearing;
`NoteMath.frequencyToNote` turns the median into a `NoteReading`. Two modes (`TunerMode`,
remembered through `SettingsStore.saveTunerMode`, default `IDENTIFY`): `IDENTIFY` names the
nearest note (`TunerUiState.identifiedMidi`: the reading's note, but a drift just past the halfway
point to a neighbour, still within `NEAR_TARGET_CENTS` of the old note, moves it only after
`TunerViewModel.SWITCH_FRAMES`, about 0.5 s, so it never flickers; the first note after silence
and a clear step to another note move it at once) and measures against it; `TARGET` lets the
child pick the note to play with the
‹ › half-step buttons of `TargetNote` (`FluteRange`: c1 to c4, MIDI 60 to 96, default a1 / A4, remembered through
`SettingsStore.saveTargetNote` and clamped on load) and measures against it with
`TunerUiState.centsOffTarget` (`NoteMath.centsBetween`), so a wrong note is pinned at an end of
the pitch window instead of being relabelled. `TunerUiState.homeMidi` (the chosen or identified
note) is the pitch window's home ring, `heardMidi` its sound bubble (the home note, or the other
note or octave played in target mode) and `gaugeCents` (cents from `homeMidi`) what
`PitchWindow` and the status line show; `TunerUiState.status` judges it against
`IN_TUNE_CENTS` (10, a child's wobble, deliberately wider than a professional tuner's 5), the one
place that threshold lives (the bubble sits in its ring exactly when the status says
"Perfect!" and jumps clear of it as soon as it does not). There is no tolerance
setting on purpose: one good default beats a control. In target mode a reading within
`NEAR_TARGET_CENTS` (70) of the chosen note or of its octaves is still that note or that octave
(`TargetMismatch.NONE`, `OCTAVE_UP` / `OCTAVE_DOWN`), so a1 played sixty cents flat reads
"Too low", not "That's gis1". `HearingTracker` turns level plus
`FrameDecision` into `Hearing` (sound from `SOUND_LEVEL`, quiet or loud around `CLOSE_LEVEL`,
reported after `UNCLEAR_FRAMES` sound frames, a gap of `UNCLEAR_GAP_FRAMES` tolerated);
`TunerUiState.advice` (`TunerAdvice`) is derived from status, mismatch and hearing and shown on
one advice line under the status (spec: revision 5 of
`docs/superpowers/specs/2026-10-22-tuner-target-note-design.md`). Unsteady frames count towards
`HOLD_FRAMES` like silent ones. `noteLabel` follows the locale's octave convention
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
`NotesUiState.ROUND_LENGTH` (10) notes: `onNoteTapped(midi)` judges an answer (ignored while
`solved` or `roundOver`), a right tap sets `solved`, appends to `results` (true = first tap),
plays the note through the tuner's `ReferenceTonePlayer` at `REFERENCE_TONE_VOLUME` and after
`FEEDBACK_MS` (1200) draws the next note (never the same twice in a row unless only one is
chosen) or ends the round; a wrong tap joins `wrongTaps`, becomes `lastWrongTap` (the cross that
gets the arrow) and zeroes the streak. `taps` counts
every tap so `accuracyPercent` (right taps over all taps) differs from `score`; `roundMillis`
is the time from the round's first tap to the tenth note found (`MonotonicClock`, so tests use
`FakeClock`), shown in the summary only as one of the metronome's animals (`NotesUiState.speed`,
the time per note: cheetah up to 2 s, rabbit 3.5 s, duck 6 s, turtle 10 s, then the snail), as the
accuracy is only one to three stars (`aimStars`: 90 % and 70 %). The chosen
notes persist through `SettingsStore.savePracticeNotes` (`practice_notes`, a string set), the last one cannot be switched off, and any change to them starts a fresh round; the sound of
a found note can be switched off (`toggleSound`, `SettingsStore.saveNoteSound`, a switch row
under the picker's rows); `resetRound()` (also behind `playAgain`) throws the round away and is
called by the tab bar when the tab is left, and `stopSound()` when the screen is disposed, so
the tuner's microphone does not hear the tone. A `Random` is passed through an internal
secondary constructor for deterministic tests. `NoteStaff` draws the staff on a Canvas
(it takes all the height left in the stage and sizes the places from it; it shows the staff
and its two neighbouring spaces, d1 to g2, plus the ledger-line places c1, a2, h2, c3 only as
far as a chosen note needs them, `StaffNotes.visiblePositions`, their ledger lines dashed; every
place a full-width band, the spaces tinted, the lines thick; a small, faint hand-built clef whose
curl sits on the G line; one note column in the middle of the width, clear of a right-handed
finger sliding on the right, after the user rejected a two-column split). The child answers by touch and slide: a finger down shows a see-through note
on the nearest place and turns its band yellow (`placing`, hoisted to `NotesContent` for the
"Slide, let go" line), it follows the finger with a haptic tick per place, lifting it answers,
sliding off the staff cancels. A miss is a cross inside its own place (`CROSS_RADIUS` 0.45 of the
pitch, so it never hides the place next to it), the latest with a ⬆️/⬇️ arrow towards the right
place; the found note is a head in its letter's colour with sparkles. Over the canvas sits a
column of fifteen full-width places that take no touches but are buttons for accessibility,
named "Staff position N" counted from the bottom, never the note, so a screen reader does not
give the answer away; their semantic click answers at once.

### Digital flute path

`Tune` (`domain/flute/`) is the child's tune: `TuneNote(midi, heldMs)`, at most `MAX_NOTES`
(200), each held length clamped to `MIN_NOTE_MS`..`MAX_NOTE_MS` (300 to 2000 ms) and played
back with `GAP_MS` (80) of silence between notes so a repeated note is heard twice. Pauses
between presses are not recorded (a child hunting for the next key would make the tune
halting), but the stored string (`"60:450,67:300"`; `decode` is lenient and `sanitize` keeps
only `StaffNotes.ALL`) tolerates an optional third field per note so pauses can be added
without a migration; `SettingsStore.saveTune` keeps it under `tune` across restarts. Sound:
`FluteTimbre` (`audio/flute/`, the harmonics the tuner's `ReferenceToneSynth` now shares)
under `SustainedToneSynth`, a phase-continuous oscillator with an attack (30 ms) / sustain /
release (120 ms) envelope and a retrigger fade (10 ms), so a note on, a note off or a takeover
never clicks; `AudioTrackFluteTonePlayer` (`FluteTonePlayer`) streams it from an URGENT_AUDIO
thread like the metronome (float PCM, low latency, media attributes, no audio focus), writing
silence while nothing is held and every block written in full (`writeFully`, retried through
a route rebuild so a sustained tone never skips samples); on `close()` the thread
(`ToneRenderLoop`, the loop body kept pure Kotlin so the close path is unit-tested) lets a
sounding note release and plays the track buffer out before it gives the track back, never
touches the command queue once retired (after a rotation the successor thread is already
serving the keys), and a new `open()` meanwhile starts its own track at once, so two tracks
overlap for the fade rather than the first press after a rotation waiting for it. `FluteViewModel` (Activity-scoped; spec:
`docs/superpowers/specs/2026-10-23-digital-flute-design.md`) opens the player in `enterTab()`
and closes it in `leaveTab()` (`FluteScreen`'s `LifecycleStartEffect`, so the first press is
never late and nothing plays off screen); `onKeyPressed(midi)` sounds the key at once, shows
it (`shownMidi`) and appends it with `MIN_NOTE_MS` (not at the cap, where it only sounds);
`onKeyReleased(midi)` silences it and records the held length from `MonotonicClock`, ignored
unless that key is the one held (the newest press wins; sliding off a key releases it);
`onKeyActivated(midi)` is the screen-reader path (`ACTIVATED_MS` 500); `togglePlayback()`
plays the tune from the start, each note for its held length then the gap, lighting it in the
line, on its key and on the fingering diagram, and stops at once on the pill, on a key press
or on `eraseLast()`, which drops the last note (the erase button repeats while held).

### UI conventions

- One Activity, which sets `volumeControlStream` to the media stream so the volume keys adjust
  the app's sound even while nothing is playing. `MainScreen` hosts a top bar (the app icon, `AppLogo`, and the open tab's name plus the `LanguageSwitch`: two flag
  buttons over `ui/common/AppLanguage`; selecting one calls
  `AppCompatDelegate.setApplicationLocales`, which re-creates the Activity) and a bottom bar
  switching between the four tabs (`AppTab`: metronome, tuner, notes, flute). All ViewModels are
  Activity-scoped so state and playback survive tab switches, rotation and that re-creation;
  the one exception is the game's round, which the tab bar resets (`NotesViewModel.resetRound`)
  when the notes tab is left, so the child always comes back to a zero score. `MainScreen` also
  keeps the screen on (`ui/common/KeepScreenOn`) while the metronome plays, the tuner listens or
  the flute plays the tune back.
- Each screen is split into a stateful `MetronomeScreen` / `TunerScreen` / `NotesScreen` /
  `FluteScreen` (ViewModel, permissions, lifecycle) and a stateless `MetronomeContent` /
  `TunerContent` / `NotesContent` / `FluteContent` taking state plus callbacks. The `*Content` composables are what the Compose
  UI tests render, wrapped in the test-only `LocaleProvider` to pin the language.
- Every user-visible string is a resource in both `values/strings.xml` and
  `values-hr/strings.xml`. Domain enums stay language-free; `ui/common/Labels.kt` maps them to
  string resources and builds note names from the `note_names` array (Croatian uses the German
  H / B convention). Croatian copy addresses the child in the informal singular.
- The look comes from the Claude Design canvas (see `.superpowers/design/flute-assistant-screens/`):
  white cards (`Stage`) on sky blue, or navy in dark mode, with dynamic colour off; the tokens that
  change with the theme are `FluteColors` (read through `FluteTheme.colors`), the bright fills that
  never change are `Sun` (yellow = chosen), `Go`, `Stop` and `NoteLetterColors`, one per letter,
  which `noteColor(midi)` applies wherever a note appears (a sharp takes the letter below). Text on a
  bright fill is always `InkOnFill`. Everything pressable is a `ChunkyButton` / `Modifier.chunky`
  (thick ink outline, solid shadow, sinks 3 dp when pressed); big actions are `BigActionButton`.
  The font is Nunito (three static weights in `res/font`, licence in `docs/licenses/`); the canvas
  used Fredoka, which has no č, ć or đ. Icons are emoji (`Emoji`, hidden from accessibility), plus
  the chevrons, arrows and tick in `AppIcons`. The app icon's picture is one vector, `@drawable/ic_app_art` (a robot
  over a flute, from the canvas's "App icon" board): the launcher insets it into the adaptive icon's
  safe zone, `AppLogo` puts it on the golden rounded square in the top bar, and `tools/IconGen.java`
  draws the same shapes for the PNGs and the Play listing. Note names are shown with the octave raised (`noteText`, built from
  `noteLabelParts`) in a round `NoteBubble`; English keeps scientific octaves (C⁴), Croatian the
  school ones (c¹), and accessibility and tests still read the plain `noteLabel`.
- On a short screen (360 x 640 dp, large text) the ring and the tuner's pitch window shrink to
  fit, so Play and the status line stay in view; only below a tab's minimum height
  does `Modifier.fitOrScroll` make it scroll (a fixed-height column, so weights still work). The
  notes tab scrolls a little on such phones rather than shrink the staff's places, and the flute
  scrolls below its own `FitsHeight` as before.
- `ui/common/ValueWheel` is the snapping three-row drum behind the metronome's `TempoWheel`. Its
  content description stays on the `LazyColumn` node: the UI tests scroll it with
  `performScrollToIndex`.

### Kid-friendly UI constraint

The UI was deliberately reduced (spec: `docs/superpowers/specs/2026-10-22-kid-friendly-ui-design.md`,
follow-up: `docs/superpowers/specs/2026-10-22-metronome-controls-and-language-design.md`) and on
2026-10-24 redrawn from the user's Claude Design canvas. The top bar shows the app icon and names
the open tab; the tab bar is four emoji-and-word buttons. The metronome is one stage with the beat bubbles
(the first bigger; the sounding one yellow with the speed's animal hopping on it) between a − and
a + that step the bar from two to six beats (`fewerBeats` / `moreBeats` over 2/4, 3/4, 4/4, 5/4,
6/8; the other signatures are not offered), and the tempo ring from 40 to 200: five animal bands
(`TempoBand`: snail 40, turtle 60, duck 80, rabbit 100, cheetah 130), each a fifth of the ring and
every animal mirrored to face right (`facingRight` in `ui/common`, also used for the summary's
speed animal: the emoji sets draw them facing left), a
yellow knob with the current animal that is dragged round, a tap on an animal jumps to its band
(the rabbit again to 120), and the tempo figure alone in the middle (the band's name above it was
dropped on 2026-10-24; a screen reader hears it as the ring's state; tap the figure for the
in-place `TempoWheel`; there are no tempo −/+ buttons: the user turned them down again on
2026-10-24); then one Play / Stop pill (no volume control: the phone's buttons set loudness). The
tuner has the two mode pills on top ("Which?" / "Tune"), then a stage with the pitch window (since
2026-10-24, replacing a face meter): a dashed home ring holding the note to play on a dotted
up-down guide, and the child's sound as a bubble floating above it when too high and below when
too low, with an arrow pointing home; another note shows as that note filled in its colour, in
tune the bubble fills, sits in the ring and gets a tick and sparkles, and in "Which?" silence the
ring is grey with "…". In "Tune" the window sits between ‹ › with a "Hear" pill under it (spec:
`docs/superpowers/specs/2026-10-22-tuner-target-note-design.md`); then the fingering for the
ring's note, one status line and, under it, one quieter advice line that says what to change
(roll the flute, air, fingers, come closer, a clear note), never a number. The notes tab has one
hero row (the streak as up to five flames, "Find" over the note's bubble, and a "Pick" button with
a dot per chosen letter) over a stage with the staff and one feedback line, and ten round places
at the bottom (a star for first time, a faint star for a retry, yellow for the current note);
"Pick" swaps the staff for the picker in place (no dialog): the fifteen notes in octave rows,
highest on top, the "Sound" switch row and a big OK, the only way back. The end-of-round summary
has no numbers on screen either: a trophy, ten stars, and tiles for the best streak (flames), the
aim (one to three stars) and the speed (an animal). The flute tab has a stage with the note's
bubble, the fingering and one scrolling line of the notes played (circles in their letter's
colour), the fifteen keys in three octave rows highest on top (each in its letter's colour; a key
sounds for as long as it is held) and, at the bottom, one big Play pill ("Play" / "Stop") with a
round erase button beside it (it sinks while held and repeats like a keyboard's backspace): no
record button, no clear-all, no tempo, no note lengths as a control. The user rejected a wider
layout (−/+ beside the ring, a ten-button signature scroller, flags in the content area) as too
packed: keep the screens this sparse. Tap tempo, subdivisions, click sounds, volume, presets and the A4 reference still
exist in the engine, data layer and ViewModels but are not exposed: `MetronomeViewModel.initialState()`
resets subdivision, sound and volume to their defaults (and brings a stored tempo onto the
ring's 40 to 200) and `TunerViewModel` starts from 440 Hz
and plays the reference tone at a fixed `REFERENCE_TONE_VOLUME`, so a stale stored value cannot
silently affect playback. Do not reintroduce numbers or jargon
(BPM, Hz, cents, Italian markings) or extra controls on the main screens; an "advanced mode"
is the intended home for those features.

## Logging and observability

`core/log/Logger` (Hilt-provided, `AndroidLogger` in production, `FakeLogger` in tests) is the
one way to log. Every line is a dotted event name plus `key=value` fields built by `LogLine`
(two decimals for floating point, `none` for null, quoted text), under one tag per subsystem
(`LogTags`: `FA.Tuner`, `FA.Metronome`, `FA.Playback`, `FA.Flute`), so lines are greppable and a script
can split them on spaces. Levels: `v` per-frame detail, off unless switched on per tag at
runtime; `d` periodic summaries and state changes, on in debug builds; `i` lifecycle; `w`/`e`
failures with the exception. Guard costly `v` calls with `logger.isVerbose(tag)`. Never log
through `android.util.Log` directly, never log user-identifying data, and keep event names
stable: they are the interface a logcat session is read with.

Tuner events: `mic.open` (source, rate, buffer), `mic.close`, `tuner.permission`,
`tuner.frame` (verbose: f, p, level, `FrameDecision`), `tuner.summary` every
`TunerViewModel.SUMMARY_FRAMES` frames (counts per decision, pMin/pMax/pMean against `gate`,
levelMean, reading, target, mode, hearing), `tuner.reading` when the shown note changes,
`tuner.hearing` (hearing, levelMean) when it changes, `tuner.target`, `tuner.mode`,
`tuner.reference`, `tuner.error`, `reference.error`. Metronome: `metronome.start` / `stop` / `ended` / `focus_lost` / `noisy` / `track_open` / `track_closed`
(underruns) / `error`; playback: `playback.error`. Flute: `tone.track_open` (rate, block and
buffer frames) / `tone.track_closed` (underruns) / `tone.error`, `flute.note` (midi, heldMs,
added: false at the cap, per note ended), `flute.play` (notes), `flute.play_done`, `flute.stop`
(reason: pill, key, erase or leave, so every `flute.play` has a closing line), `flute.erase`
(left).

```sh
adb logcat -s FA.Tuner FA.Metronome FA.Playback FA.Flute   # everything structured, debug build
adb shell setprop log.tag.FA.Tuner VERBOSE               # per-frame tuner lines (any build)
adb logcat -v time -s FA.Tuner | grep tuner.summary     # one line a second: did frames pass the gate?
```

## Testing conventions

Unit tests use hand-written fakes from `app/src/test/.../fakes/Fakes.kt` (`FakeMetronomePlayer`,
`FakeTunerEngine`, `FakeSettingsStore`, `FakePresetRepository`, `FakeClock`,
`FakeBackgroundPlayback`, `FakeReferenceTonePlayer`, `FakeFluteTonePlayer`, `FakeLogger`); there is no mocking library. ViewModel tests run under
`StandardTestDispatcher` with `Dispatchers.setMain` and drive time with `advanceTimeBy` /
`advanceUntilIdle`. Instrumented tests cover the `*Content` composables (`MetronomeScreenTest`, `TunerScreenTest`,
`NotesScreenTest`, `FluteScreenTest`), the flag buttons (`LanguageSwitchTest`) and
locale-dependent resources (`LabelsTest`). The `config_summary`, `subdivision_*` and `sound_*` strings must stay
even though the screens no longer show them: the playback notification and `LabelsTest` use them.
