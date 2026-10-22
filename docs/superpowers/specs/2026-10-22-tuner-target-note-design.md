# Tuner target note: design

Date: 2026-10-22. Status: built under auto mode (the user asked for the change in one line
and the session ran without questions); the calls marked *my call* are the builder's.

## Intent

The request: "add tone to tuner eg c1, d1, e1 so tuner shows how far is played tone against
note, like anchor, note should be spinner so user can chose different notes". It is a change
to the tuner tab only.

Today the tuner names the nearest note to whatever it hears and shows the distance to that
note. For a child this sets a trap: play a1 sixty cents flat and the screen says "G♯, too
high", so the child lowers the note further. With a chosen note as the anchor, the gauge
always measures against the note the child means to play, and the advice is always right.

## Goals

- The child picks the note they are about to play, in a spinner; the gauge and the status
  line measure against that note only.
- Note names the way the child's teacher writes them: c1, d1, e1 in Croatian, C4, D4, E4 in
  English.
- Choosing is one scroll, and the chosen note stays as big as the letter was.
- The choice survives leaving the tab, rotating and restarting the app.

## Non-goals

- (Superseded by the revision below.) An automatic (nearest-note) mode in the UI.
- Showing the heard note's name when it is a different note from the target. A possible
  follow-up if children turn out to need it.
- Transposition, other temperaments, or exposing the A4 reference.

## Anti-goals

- Numbers that are not part of the note name; no cents, no Hz.
- A second row of controls on the tuner. The wheel takes the place of the big letter, so the
  screen stays three things tall: note, gauge, status.
- Wheel entries the flute cannot play.

## What the child sees (tuner tab)

### The note wheel

Where the big letter was: a vertical drum with three rows. The chosen note sits huge in the
middle on a peach band (the same colour as the beats pill on the metronome), the neighbours
above and below are small and faded. Scrolling snaps one note per notch with a haptic tick,
the wheel reports the note as soon as it is centred, and tapping a neighbour scrolls to it.
It is the same drum as the metronome's tempo wheel, so the two tabs feel the same.

Range: c1 to c4 (C4 to C7, MIDI 60 to 96), every semitone, 37 entries. Default a1 (A4), the
note every orchestra tunes to. *My call* on range and default; both are constants in one place.

Names follow the language:

| Language | Convention                 | Examples                         |
|----------|----------------------------|----------------------------------|
| Croatian | Croatian music-school (Helmholtz-style) octaves, lowercase | c1, cis1, b1 (B flat), h1 (B), c2, c3, c4 |
| English  | scientific pitch notation  | C4, C♯4, A♯4, B4, C5, C6, C7     |

*My call:* scientific notation for English, since that is what English-language tuners show.
The Croatian names are lowercase because that is how the one-lined octave and above are
written; the full rule (great octave uppercase, small octave lowercase without a number,
contra octave C1) is implemented so the helper is correct outside the flute range too.

The middle note turns green while the played note is in tune, as the big letter did.

### Gauge and status line

The gauge is unchanged to look at. Its needle now shows the distance from the chosen note,
still clamped at ±50 cents, so playing a different note pegs the needle in the red at one end.
The status line keeps "🎉 Perfect!", "⬇️ Too low" and "⬆️ Too high" with the same thresholds
(5 cents in tune, 15 cents slightly off). In silence it now says "👂 Play a1", naming the
chosen note, instead of "Play a note": that is how the child learns the wheel is the note to
play. *My call*.

### Accessibility

The wheel's content description is "Note to tune, scroll to choose another" / "Ton koji
ugađaš, zavrti za drugi"; the row texts are ordinary text nodes.

## Code changes (the how, for the builder)

- `domain/tuner/NoteMath.kt`: `centsBetween(frequency, targetMidi, a4)`, the signed distance
  in cents from the target note's frequency (not clamped).
- `domain/tuner/FluteRange.kt`: `LOWEST_MIDI = 60`, `HIGHEST_MIDI = 96`, `DEFAULT_TARGET = 69`
  and `clamp(midi)`.
- `domain/repository/SettingsStore.kt`: `loadTargetNote()` / `saveTargetNote(midi)`;
  `SharedPreferencesSettingsStore` stores the MIDI number; `FakeSettingsStore` gains the field.
- `ui/tuner/TunerUiState.kt`: `targetMidi` plus a derived `centsOffTarget` (null in silence).
- `ui/tuner/TunerViewModel.kt`: loads the stored target clamped into the range (it is visible
  on screen, so unlike the A4 reference a stored value is safe to honour); `setTargetNote`
  clamps, saves and updates the state.
- `ui/common/ValueWheel.kt`: the snapping drum lifted out of `TempoWheel`, parameterised by
  item count, selected index, label, text styles and row height. `TempoWheel` becomes a thin
  wrapper with its public API, semantics and tests unchanged.
- `ui/tuner/NoteWheel.kt`: `ValueWheel` over `FluteRange` with `noteLabel`, a peach band
  behind the middle row, green centre text when in tune.
- `ui/common/Labels.kt`: `noteLabel` follows the locale's octave convention through the new
  `bool/note_octaves_helmholtz` resource (false by default, true in `values-hr`).
- `ui/tuner/TunerScreen.kt`: the wheel replaces the big letter; `TunerContent` gains
  `onTargetNoteChange`; status and colours read `centsOffTarget`.
- Strings: `target_note`, `play_note_prompt` (with the note name; replaces `play_a_note`).
- Tests: `NoteMathTest`, `FluteRangeTest`, `TunerUiStateTest`, `TunerViewModelTest` (JVM);
  `LabelsTest`, `TunerScreenTest`, `MetronomeScreenTest` (instrumented, the last one to prove
  the wheel refactor changed nothing).

## Verification

- `./gradlew testDebugUnitTest` green.
- `./gradlew :app:connectedDebugAndroidTest` green on the headless emulator, one class at a time.
- Screenshots of the tuner tab in English and Croatian, light and dark.

## Revision, later the same day: keep both modes

After the first build the user asked to keep both behaviours: one mode that names whatever
note is played, and one that measures against the chosen note.

### What the child sees

Two pills at the bottom of the tuner, under the status line (the user asked for them there
after seeing them at the top), an emoji and a few words each, the chosen one filled teal like
the level pills on the metronome: "🔍 Which note?" / "🔍 Koji je ton?" and "🎯 Tune to a
note" / "🎯 Ugodi ton". The accessibility name of the pair is "Tuner mode" / "Način štimanja".

- **Which note?** (`TunerMode.IDENTIFY`): the huge note name of whatever is heard, now with
  its octave ("A4", "h1") so the two flute octaves are told apart, "…" dimmed in silence; the
  gauge and status measure against that nearest note, and silence says "👂 Play a note".
  *My call:* the octave is shown here too, for consistency with the wheel.
- **Tune to a note** (`TunerMode.TARGET`): the wheel and everything in the first part of this
  document.

The letter takes exactly the wheel's height, so the gauge does not jump when switching. The
mode is remembered like the note; the first launch starts in "Which note?", which needs no
setup. *My call* on the default.

### Code changes

- `domain/tuner/TunerMode.kt`: `IDENTIFY`, `TARGET` (persisted by name).
- `SettingsStore.loadTunerMode()` / `saveTunerMode()`, key `tuner_mode`, default `IDENTIFY`
  with the usual unknown-name fallback; `FakeSettingsStore.tunerMode`.
- `TunerUiState.mode` and `gaugeCents` (the nearest note's cents in IDENTIFY, `centsOffTarget`
  in TARGET); `TunerViewModel.setMode`.
- `TunerContent` gains `onModeChange`; `TunerModeChoice` pills; `NoteWheelHeight` is public so
  the letter box matches it; `TunerMode.labelRes()` in `Labels.kt`.
- Strings: `tuner_mode`, `mode_identify`, `mode_target`; `play_a_note` is back.
- Tests: `TunerUiStateTest.gauge cents follow the mode`, two `TunerViewModelTest` cases, five
  `TunerScreenTest` cases for the identify mode and the pills.

## Revision 3: hear the note and see the fingering

The user asked for a way to hear the note that needs to be played, and for the flute
fingering that produces it.

### What the child sees

- **Speaker button** at the right end of the note band in "Tune to a note": a coral round
  button with a speaker. A tap sounds the chosen note for two seconds as a soft flute-like
  tone (a sine with a little second and third harmonic, faded in and out) at the app's volume.
  While it sounds the button is dimmed, the status line says "🔊 Listen" / "🔊 Slušaj", and the
  microphone reading is paused so the speaker is not measured; readings start afresh when it
  ends. Leaving the tab stops the tone. *My call* on the two-second one-shot rather than a
  toggle: it avoids the microphone and speaker fighting each other.
- **Fingering diagram** under the note (in both modes; in "Which note?" it shows the fingering
  of whatever is being played, faintly empty in silence): the flute from above, head joint on
  the left; thumb key, three left-hand keys, the G sharp lever, three right-hand keys, the E
  flat key and the two foot-joint rollers. Pressed keys are filled coral, the rest outlines.
  Fingerings follow the Woodwind Fingering Guide's basic chart for a C-foot Boehm flute; B flat
  is the "one and one" fingering. a3 sharp and h3 need trill or sliver keys the diagram does not
  draw, so they show no diagram. Accessibility name: "Fingering for A4" / "Prstomet za a1".

### Code changes

- `domain/tuner/FluteFingerings.kt`: `FluteKey` enum and `keysFor(midi)` (null when uncharted).
- `audio/tuner/ReferenceTonePlayer.kt` (interface), `ReferenceToneSynth` (pure rendering),
  `AudioTrackReferenceTonePlayer` (static-mode AudioTrack), bound in `AudioModule`.
- `TunerUiState.isPlayingReference`; `TunerViewModel.playTargetNote()` (`REFERENCE_TONE_MS`),
  frames ignored while playing, `stopListening` and `onCleared` stop the tone.
- `ui/tuner/FingeringDiagram.kt`; `NoteWheel` gains `onPlay` / `isPlaying`; `TunerContent`
  gains `onPlayNote`.
- Strings `hear_note`, `listen_status`, `fingering_for`.
- Tests: `FluteFingeringsTest`, `ReferenceToneSynthTest`, two `TunerViewModelTest` cases,
  three `TunerScreenTest` cases.
