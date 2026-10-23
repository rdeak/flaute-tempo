# Digital flute: design

Date: 2026-10-23. Status: brainstormed with the user over mockups (`.superpowers/brainstorm/`);
the calls marked *my call* are the designer's and the user has not confirmed them.

## Intent

The request: "create digital flute as new view, goal is allow child play songs and explore
song compositions, view should have flute similar or same as tuner has and below should have
notes button similar like notes". Then, in conversation: tapping a note button plays the
tone at once and keeps playing until the finger lifts, and the flute drawing shows how that
tone is made; every note tapped is also drawn into a line of notes, with a button to play
the line back, and the line scrolls sideways once it is full.

Today the app helps a child play a note in tune (tuner), keep time (metronome) and read a
note on the staff (notes game). This adds the thing a child wants to do before any of that:
make sounds, see what fingers make them, and string them into a tune of their own and hear
it back. It is an instrument and a scratchpad, not a lesson: no song to follow, no score,
no right or wrong.

The user rejected a guided-song version (built-in songs, the next note's button glowing) in
favour of this: the child is in charge, and the reward is hearing their own tune.

## Goals

- The flute sounds the instant a key is pressed and stops when the finger lifts, like a real
  key, with no click at either end.
- Every press shows, on the same flute drawing the tuner uses, which keys make that note,
  and names the note big above it.
- Every press is recorded into a line of notes the child can read, fix and play back with
  one button, and the playback shows each note on the flute as it sounds.
- The tune survives switching tabs, rotating and closing the app, so nothing a child made is
  lost by accident.
- English and Croatian, note names the way the child's teacher writes them.

## Non-goals

- Sharps and flats, or notes outside c1 to c3. The flute goes higher, but the fifteen
  naturals are what the notes game teaches and all a beginner's songs need.
- Saving more than one tune, naming it, sharing it, or exporting it.
- Built-in songs, a demo tune, a follow-the-lights mode.
- A tempo, a metronome tick during playback, note lengths as a control, a staff view of the
  tune. (The tune's data, a list of notes with held lengths, would support a staff view
  later.)
- Two notes at once. A flute plays one note.

## Anti-goals

- Numbers or jargon on the screen: no Hz, no note lengths, no count of notes.
- Extra controls. The screen has the keys, one Play button and one erase button, nothing
  else. No clear-all, no record button, no settings.
- Recording that has to be switched on. The line simply keeps everything the child plays.
- A packed screen. The keys take the height the phone has; nothing else is added to fill it.

## What the child sees: the "Flute" tab

A fourth tab in the bottom bar, after Notes: a flute icon, labelled "Flute" / "Flauta".
*My call* on the Croatian label and on the icon (a hand-built `ImageVector` in `AppIcons`,
a slanted tube with a few holes, like the other three icons).

Top to bottom, centred, on the cream background, no scrolling of the screen itself:

1. **The note name**, big (the notes tab's 64 sp): the note being held, or the last one
   played, or during playback the note sounding now. Before the first tap, a faint "…" as
   on the tuner. *My call* on the "…".
2. **The flute**, the tuner's `FingeringDiagram`, a little taller than on the tuner (about
   56 dp against 40) since nothing else competes for the space. It shows the fingering of
   the note the name shows; before the first tap every key is drawn faintly open, as the
   tuner does with no note.
3. **The tune line.** One rounded strip across the width holding the notes played so far,
   each as a small pill with its name, in the colour of its octave row below (peach, teal
   or yellow) so a child can find the key it came from. The newest note is coral while it
   sounds; during playback the note sounding now is coral. The line scrolls sideways: it
   follows the newest note and the playing note, and the child can drag it back to look.
   While empty it shows one faint line of text, "Tap a note to start your tune" / "Dodirni
   notu i složi pjesmicu", which goes with the first note. *My call* on the hint and on
   the octave colours in the line.
4. **The keys.** Fifteen buttons in three rows, one row per octave: c1 d1 e1 f1 g1 a1 h1 in
   peach, c2 d2 e2 f2 g2 a2 h2 in teal, c3 alone at the start of a yellow row (the English
   names, C4 to C6, follow the locale as everywhere). Rounded rectangles, not pills, seven
   across, with the names in the same style as the notes picker. The rows take all the
   height left between the line and the Play row, so the keys are as big as the phone
   allows, and never less than 64 dp. A pressed key fills coral. The user chose this
   octave-row layout over the notes picker's three-by-five grid and over coloured
   xylophone bars.
   *My call* on what gives when there is not enough height (landscape, a very short
   phone): the keys stay at their 64 dp minimum and the whole screen scrolls, as the
   metronome and tuner tabs already do. On an ordinary phone held upright nothing
   scrolls. The app stays unlocked in orientation.
5. **The Play row**, at the bottom: a big full-width coral pill, "▶ Play my tune" /
   "▶ Odsviraj moju pjesmicu", which reads "■ Stop" / "■ Stani" while the tune plays, and
   to its right one round soft-teal erase button (a backspace icon). Both are faded, and
   do nothing, while the line is empty. The user chose the big pill over a small Play at
   the end of the line. *My call* on the copy.

### Playing a key

- **Press:** the note sounds at once and keeps sounding; the key fills coral; the name and
  the flute switch to it; it is appended to the tune line, coral, and the line scrolls to it.
- **Release:** the sound fades out over about 120 ms, the key and the pill return to their
  octave colour. The name and the fingering stay, so the child can still look at how the
  note was made.
- **How long it was held** is stored with the note in the line, clamped to between 300 ms
  and 2 s. Playback uses it (below). *My call*: rhythm for free, without a control. If it
  sounds wrong on a real phone, equal lengths of 500 ms are the fallback and the line looks
  the same either way.
- **The pauses between notes are not recorded.** A child hunting for the next key leaves a
  gap before every note; played back, those gaps would make the tune halting, like the
  search, not like a song. Without them a slowly built tune plays back fluently, which is
  the reward. A breath between phrases is lost, but holding the last note of a phrase
  longer marks it. *My call*, the one the reviewer flagged as costly to change later; the
  stored format therefore tolerates an optional third field per note (a pause before it)
  so pauses can be added without a migration.
- **A second finger** while one key is held: the flute is a one-note instrument, so the
  newest press takes over, sounds, and is appended; lifting the newest press silences the
  flute; lifting the older key, which is no longer sounding, does nothing. *My call.*
- **Sliding** the finger off a key counts as a release; sliding onto another key does not
  press it. Each note in the line is a deliberate tap. *My call*, so a wandering finger
  cannot fill the line.
- **A press during playback** stops the playback first, then behaves as above.
- **The line has a cap**, 200 notes. At the cap a press still sounds and shows the note, but
  nothing is appended. *My call*: far beyond what a child makes on purpose, and silent
  rather than a message.

### Playing the tune back

- Play starts from the first note. Each note sounds for its held length, then a short gap
  (about 80 ms) so a repeated note is heard twice, then the next. While a note sounds it is
  coral in the line, its key fills coral, the flute shows its fingering and the name shows
  it: the child watches their own tune being fingered.
- The pill reads "■ Stop" meanwhile; tapping it stops at once, as does pressing a key or
  erase. When the last note ends, Play comes back, the last note stays named and fingered,
  nothing in the line is coral.
- Leaving the tab stops playback and any held note. Coming back shows the line as it was.

### Erasing

- The erase button removes the last note of the line. Held, it keeps removing, one note
  about every 120 ms after a 400 ms wait, the way a phone keyboard's backspace repeats, so a
  long line is cleared by holding. *My call* on repeat rather than a clear-all.
- Erasing during playback stops the playback first.

### Remembering

The line is kept while the app runs (the ViewModel is Activity-scoped, like the others, so
tabs, rotation and the language switch keep it) and also across app restarts, saved
through `SettingsStore` on every change and loaded when the ViewModel is created. On load,
notes the keys do not offer are dropped and held lengths are clamped. *My call* on
restarts: losing something you made is upsetting, and one string in the preferences is
cheap.

### Sound

- The same flute-like timbre as the tuner's reference tone (`ReferenceToneSynth`'s sine with
  soft second and third harmonics), at the fixed `REFERENCE_TONE_VOLUME`; the phone's
  volume buttons set loudness, as everywhere in the app.
- A held note needs a sound that does not know its length in advance, which the tuner's
  static player cannot do. A new streaming player fades a note in over about 30 ms, holds
  it, and fades it out over about 120 ms on release; a new note taken over while one sounds
  fades the old one down over about 10 ms before the new attack, so nothing clicks. Latency
  from press to sound should be what the metronome gets (low-latency streaming track,
  native rate, float PCM).
- Media attributes, no audio focus request, as the reference tone: a metronome left running
  keeps ticking under the flute. *My call.*
- The track opens, writing silence, when the tab comes on screen, and closes when the tab
  stops being visible (leaving the tab, Home, screen off), the way the tuner holds the
  microphone. Opening a track costs tens of milliseconds, so opening it on the first press
  would make the first note of every visit late; opening it on entry keeps every press
  immediate, and closing it on stop means the tuner's microphone never hears the flute and
  nothing drains the battery in the background. *The reviewer's call, adopted.*

### Accessibility

Each key is a button named after its note ("c1", "C4"), with the role of a button; with a
screen reader, activating a key sounds it for 500 ms and appends it. The tune line is
"Your tune, 7 notes" / "Tvoja pjesmica, 7 nota" and each pill reads its note name. The
Play pill reads its label; the erase button is "Erase the last note" / "Obriši zadnju
notu". The flute drawing keeps the tuner's "Fingering for g1" description.

## Code changes (the how, for the builder)

- `domain/flute/Tune.kt`: `TuneNote(midi, heldMs)`; `Tune` helpers: `MAX_NOTES = 200`,
  `MIN_NOTE_MS = 300`, `MAX_NOTE_MS = 2000`, `GAP_MS = 80`, `clampHeld`, `encode` / `decode`
  (a string such as `60:450,67:300`; the decoder accepts and ignores an optional third
  field per note, a pause in ms, so pauses can be stored later without a migration),
  `sanitize` (keeps only `StaffNotes.ALL`, clamps).
  The keys offered are `StaffNotes.ALL`; fingerings come from `FluteFingerings.keysFor`.
- `domain/repository/SettingsStore.kt`: `loadTune(): List<TuneNote>` / `saveTune(notes)`;
  `SharedPreferencesSettingsStore` stores the encoded string under `tune`;
  `FakeSettingsStore` gains the field.
- `audio/flute/FluteTonePlayer.kt` (interface): `open()` (starts the thread and the track,
  silent), `noteOn(frequencyHz, volume)`, `noteOff()`, `close()` (stops the thread and
  releases the track; `noteOn` on a closed player opens it first, so a late `open` is never
  fatal). `audio/flute/SustainedToneSynth.kt`: pure
  Kotlin, phase-continuous oscillator with an attack / sustain / release envelope and a
  retrigger that fades the old note first; renders PCM blocks on demand; unit-tested for
  continuity (no sample step larger than the envelope allows at note on, note off and
  retrigger) and for reaching silence after release. `audio/flute/AudioTrackFluteTonePlayer.kt`:
  a streaming `AudioTrack` on a dedicated URGENT_AUDIO thread doing blocking float writes,
  after `AudioTrackMetronomePlayer`; the thread runs from `open()` to `close()`, writing
  silence while no note is held; logs under a new `LogTags.FLUTE` (`FA.Flute`): `tone.track_open`,
  `tone.track_closed` (underruns), `tone.error`. Bound in `di/AudioModule.kt`.
- `ui/flute/FluteUiState.kt`: `tune: List<TuneNote>`, `heldMidi: Int?`, `shownMidi: Int?`
  (what the name and flute show), `playingIndex: Int?`; derived `isPlaying`, `isEmpty`,
  `atCap`.
- `ui/flute/FluteViewModel.kt`: Activity-scoped; `onKeyPressed(midi)`, `onKeyReleased(midi)`,
  `onKeyActivated(midi)` (the screen-reader path), `togglePlayback()`, `eraseLast()`,
  `enterTab()` (opens the player) and `leaveTab()` (stops sound and playback, closes the
  player, keeps the tune); uses
  `MonotonicClock` for held lengths and `viewModelScope` with `delay` for playback, so
  tests drive it with `advanceTimeBy`; logs `flute.note` (midi, heldMs), `flute.play`
  (count), `flute.play_done`, `flute.stop`, `flute.erase`.
- `ui/flute/FluteScreen.kt`: `FluteScreen` (ViewModel wiring; a `LifecycleStartEffect`
  calls `enterTab()` on start and `leaveTab()` in `onStopOrDispose`, as `TunerScreen` does
  with the microphone) and the stateless `FluteContent`; `FluteKeys.kt` (the three rows; press and
  release through `detectTapGestures(onPress)` with `tryAwaitRelease`, plus a click action
  for accessibility); `TuneLine.kt` (a `LazyRow` of pills, auto-scroll to the newest or
  playing note); the Play row in `FluteScreen.kt`.
- `ui/MainScreen.kt`: `AppTab.FLUTE`; `ui/common/AppIcons.kt`: `Flute`, `Backspace`.
- Strings in both languages: `tab_flute`, `flute_hint`, `flute_play`, `flute_stop`,
  `flute_erase`, `flute_tune_line` (with a count).
- Tests: `TuneTest`, `SustainedToneSynthTest`, `FluteViewModelTest` (press sounds and
  appends; release records the held length, clamped; newest press wins; release of a
  non-sounding key is ignored; erase, with nothing to erase; playback sequence and
  timings; stop by pill, by key, by erase; the cap; the tune round-trips through the
  store; leaving the tab silences but keeps the tune) on the JVM with a
  `FakeFluteTonePlayer`; `FluteScreenTest` (fifteen keys in octave rows; a press shows the
  name, the fingering and a pill in the line; Play and erase faded when empty; the pill
  reads Stop while playing; Croatian copy) instrumented.

## Verification

- `./gradlew testDebugUnitTest` and `./gradlew lintRelease` green.
- `./gradlew :app:connectedDebugAndroidTest` green on the headless emulator, one class at a
  time.
- Screenshots of the Flute tab in English and Croatian, light and dark: empty, a held key,
  a line of seven notes, playback in progress.
- On a real phone (the emulator has no audio): press-to-sound latency feels immediate, no
  click on press, release or takeover, repeated notes are heard as two, the held-length
  playback sounds like what was played.
