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

## Revision 4: name the heard note, and a log a session can be read back from

The user reported that the tuner did not recognise a beginner's note played from the drawn
fingering, and that in "Tune to a note" nothing says which note was actually heard. An
analysis against real transverse-flute recordings (University of Iowa samples, with breath
noise added) showed the detector finds the note but the ViewModel's confidence gate
(`MIN_PROBABILITY` 0.85 on top of YIN's 0.2 threshold) discards a breathy tone below about
9 dB signal-to-noise; that gate is deliberately left unchanged until a phone log confirms it.
The user chose, from three options, to keep the wheel and put the heard note in the status
line, plus structured logging so the next problem can be diagnosed from one logcat capture.

### What the child sees

In "Tune to a note", when the heard note is not the chosen one the status line says which
note it was, in the direction's colour: "⬆️ Too high, that's h1" / "⬇️ Too low, that's g1"
("⬆️ Previsoko, to je h1"). The octave above or below the chosen note is a beginner's
overblown or underblown note, cured by air speed rather than fingers, so it gets its own
line: "⬆️ An octave too high: gentler air" / "⬇️ An octave too low: faster air" ("⬆️ Oktavu
previsoko: nježniji zrak" / "⬇️ Oktavu prenisko: brži zrak"). The chosen note itself, however
sharp or flat, keeps "Too high" / "Too low"; "Which note?" mode is unchanged. The wheel is
unchanged: a heard-note marker on it is the possible next step.

### Code changes

- `TunerUiState.mismatch` (`TargetMismatch`: NONE, OCTAVE_UP, OCTAVE_DOWN, OTHER_NOTE) from
  the reading's nearest note versus the target, in target mode only.
- `StatusLine` takes `mismatch` and the heard note's label; strings `too_low_heard`,
  `too_high_heard`, `octave_too_low`, `octave_too_high`.
- `core/log`: `Logger` interface, `LogLine` formatter, `AndroidLogger`, `LogTags`; `LogModule`
  provides it. `TunerViewModel` logs per-frame decisions (`FrameDecision`, verbose), a
  `tuner.summary` every `SUMMARY_FRAMES` frames, `tuner.reading` on note changes, choices and
  failures; `AudioRecordTunerEngine` logs `mic.open` / `mic.close`; the metronome player, the
  reference tone player and the playback service starter log through the same facility.
  Conventions and adb commands are in CLAUDE.md ("Logging and observability").
- Tests: `LogLineTest`, `TunerUiStateMismatchTest`, `TunerViewModelLoggingTest` (JVM, with
  `FakeLogger`); four `TunerScreenTest` cases (instrumented).

## Revision 5: say what to change

Date: 2026-10-23. The user reported that a child played h1 and the tuner showed a1, then
played a1 and it showed b1, with the phone placed at several distances, and asked that the
tuner show "how far the flute is from the microphone and what to change to produce the target
tone", simply, for a young student. From three mockups (one status line carrying everything;
the same plus a three-bar hearing meter; a short status with the advice on a second, quieter
line) the user chose the two-line layout, and words only for a wrong note (no marker on the
fingering diagram). The user does not know what the teacher says for a flat note and asked
for a recommendation; the advice words below are the builder's, from standard flute
pedagogy, and are the first thing to change if a teacher disagrees.

This revision does not make the detector more accurate. In "Tune to a note" a wrong reading
at least appears as "That's a1" with "Check your fingers" rather than as a renamed note.
Why the readings went wrong that morning is unknown; the structured log (`tuner.summary`,
`tuner.reading`) is what a next occurrence should be captured with, and the new
`tuner.hearing` event adds the input level to that picture.

### What the child sees

Under the status line, one more line of text: smaller, in the muted text colour, centred,
and always occupying its height (empty when there is nothing to say) so the gauge and the
mode pills never move. The status line keeps its colour and bounce. Together they read:

| Moment | Status line | Advice line |
|--------|-------------|-------------|
| Nothing heard | "👂 Play h1" (target) / "👂 Play a note" | (empty) |
| Sound for about a second, no readable note, quiet | same as nothing heard | "Come closer to the phone" |
| Sound for about a second, no readable note, loud enough | same as nothing heard | "Play one long, clear note" |
| Reference tone sounding | "🔊 Listen" | (empty) |
| In tune | "🎉 Perfect!" | (empty) |
| Chosen note (or nearest note), flat, even past the halfway point to the neighbour | "⬇️ Too low" | "Roll the flute out a little" |
| Chosen note (or nearest note), sharp, likewise | "⬆️ Too high" | "Roll the flute in a little" |
| Target mode, octave below | "⬇️ An octave too low" | "Faster air" |
| Target mode, octave above | "⬆️ An octave too high" | "Gentler air" |
| Target mode, any other note (further than `NEAR_TARGET_CENTS` from the chosen one) | "🖐️ That's a1" (the heard note), in red | "Check your fingers" |

Croatian, informal singular as everywhere: "Dođi bliže mobitelu", "Odsviraj jedan dug, čist
ton", "Zakreni flautu malo od sebe", "Zakreni flautu malo k sebi", "Brži zrak", "Nježniji
zrak", "Provjeri prste"; status "⬇️ Oktavu prenisko", "⬆️ Oktavu previsoko", "🖐️ To je a1".

Why rolling and not air for a flat or sharp note (*builder's call*): a beginner is flat
mostly because the embouchure hole is covered too much and the air is slow; rolling the
flute out uncovers the hole and raises the pitch a little, which is the adjustment teachers
give first and the one a child can do by small degrees. "Faster air" is kept for the octave,
where it is the actual cure; told to blow faster for twenty cents, a child overblows into the
octave. Sharp is the mirror.

Why "long, clear" for unreadable sound at a good level (*builder's call*): the common case is
a breathy but steady tone that the detector finds and the confidence gate discards
(revision 4), so "steady" would tell the child to keep doing what they are doing; "clear"
points at the embouchure, "long" at the wobble, and one wording serves every cause.

Far-off but still the chosen note (*builder's call, the one open question put to the user*):
a child playing a1 sixty cents flat has g♯1 as the nearest note, and calling that "That's
gis1, check your fingers" is the trap the first part of this document set out to remove,
since the fingers are right. So in "Tune to a note" a heard pitch within `NEAR_TARGET_CENTS`
(70) of the chosen note counts as the chosen note, flat or sharp, even when its nearest note
is the semitone neighbour; beyond that it is another note. Seventy because an embouchure
drift stays under half a semitone while a wrong fingering lands near a whole other note, and
a neighbour played in tune (100 cents away) still gets "Check your fingers". The same band
applies around the octave above and below, so an underblown or overblown note that is also
flat or sharp still gets the air advice rather than "Check your fingers" (*builder's call*,
made at the final review). The gauge is unchanged: it pegs at fifty.

"Which note?" mode shows the same advice for flat, sharp, quiet and unclear sound. The octave
and wrong-note pairs need a chosen note and so appear only in "Tune to a note", as today.

The "come closer" / "steady note" advice can also appear when a child talks near the phone
(a voice is sound without a readable note). Accepted: the status line still says what to
play, and the advice clears as soon as a note is read or the sound stops.

### Behaviour (the how, for the builder)

- `TunerUiState.mismatch` becomes `NONE` whenever `abs(centsOffTarget) <= NEAR_TARGET_CENTS`
  (70, a constant beside `IN_TUNE_CENTS`), then the same band around ±1200 cents for the
  octaves, then `OTHER_NOTE`; `status` is unchanged, so such a note reads "Too low" / "Too
  high" with the needle pegged.
- `TunerUiState` gains `hearing: Hearing` (`CLEAR`, `QUIET_UNCLEAR`, `LOUD_UNCLEAR`;
  default `CLEAR`) and a derived `advice: TunerAdvice` (`NONE`, `COME_CLOSER`, `CLEAR_NOTE`,
  `ROLL_OUT`, `ROLL_IN`, `FASTER_AIR`, `GENTLER_AIR`, `CHECK_FINGERS`) computed from `status`,
  `mismatch`, `hearing` and `isPlayingReference`, so the composable only maps an enum to a
  string. Precedence: a reading (status not `NONE`) wins over `hearing`; the reference tone
  wins over both.
- `TunerViewModel` detects unclear sound from the frames it already receives: a frame that
  is not `SHOWN` and not `IGNORED` and whose `level` is at least `SOUND_LEVEL` is a sound
  frame and joins the run; a `SHOWN` frame ends the run at once and sets `hearing` back to
  `CLEAR`. Frames under `SOUND_LEVEL` are tolerated inside the run (a beginner breathes
  between short notes) until `UNCLEAR_GAP_FRAMES` (10, about 0.4 s) of them come in a row,
  which also ends the run. Once the run holds `UNCLEAR_FRAMES` (25) sound frames, about a
  second, `hearing` becomes `QUIET_UNCLEAR` when the mean level of the last `UNCLEAR_FRAMES`
  sound frames (a sliding window, not the whole run, so moving closer is noticed within a
  second) is under `CLOSE_LEVEL`, else `LOUD_UNCLEAR`, re-evaluated on every sound frame.
  Starting values, *builder's call* and to be confirmed from `levelMean` in `tuner.summary`
  on a real phone: `SOUND_LEVEL` 0.25 (about -45 dBFS, above a quiet room) and `CLOSE_LEVEL`
  0.5 (about -30 dBFS). The run resets with `recentFrequencies` in `stopListening` and when
  the reference tone ends.
- The hold: today a confident but `UNSTEADY` frame resets `silentFrames`, so a wandering
  pitch (a glissando, speech vowels after a note) keeps the last reading, and with it stale
  advice, on screen for as long as it lasts. `UNSTEADY` frames now count towards
  `HOLD_FRAMES` like silent ones (`silentFrames++` instead of the reset), so the reading
  clears after 0.9 s of unsteadiness and the hearing advice can take over. A note change
  passes through one or two unsteady frames and then a `SHOWN` one, which still resets the
  count, so steady playing is unaffected. *Builder's call*, the one change to the hold.
- Logging: `tuner.hearing` (`d`) when `hearing` changes, with the new value and the run's
  mean level, and a `hearing` field in `tuner.summary`, so the two thresholds can be judged
  from one logcat capture.
- `TunerScreen`: `StatusLine` keeps its signature minus the strings it no longer builds; a
  new `AdviceLine(advice)` under it, `bodyLarge` in `onSurfaceVariant`, with a fixed height
  of one line, 4 dp below the status; the 32 dp spacer above the pills shrinks so the pills
  stay roughly where they are. Each `TunerAdvice` maps to a string through `Labels.kt`
  (`TunerAdvice.labelRes()`), `NONE` to an empty string.
- Strings: `advice_come_closer`, `advice_clear_note`, `advice_roll_out`, `advice_roll_in`,
  `advice_faster_air`, `advice_gentler_air`, `advice_check_fingers`; `octave_too_low` and
  `octave_too_high` lose their ": faster air" / ": gentler air" tails; `too_low_heard` and
  `too_high_heard` are replaced by one `heard_other_note` ("🖐️ That's %1$s"). Both languages.
- Tests: `TunerUiStateTest` (advice for every row of the table, in both modes; a1 played
  sixty cents flat is `NONE` / "Too low", g♯1 in tune is `OTHER_NOTE`);
  `TunerViewModelTest` (quiet and loud unclear runs, a breath inside a run tolerated and a
  longer gap ending it, the window following a move closer, the run ending on a shown frame,
  nothing during the reference tone, an unsteady stretch clearing the reading after the
  hold, the `tuner.hearing` log line through `FakeLogger`); `TunerScreenTest` (the advice line's text in three or four states, the
  revision-4 cases updated to the new status strings).
- Docs: CLAUDE.md's tuner path, kid-friendly UI paragraph and logging list.

### Non-goals of this revision

- Any change to the detector, `MIN_PROBABILITY` or the lock (the hold changes only as
  described above).
- A level meter, a number, or a marker on the fingering diagram.
- Advice about the head joint (an instrument-setup matter for the teacher).
