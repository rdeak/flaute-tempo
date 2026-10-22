# Kid-friendly UI: design

Date: 2026-10-22. Status: approved by the author under auto mode (the user asked for the
change and opted out of mid-task questions); the calls marked *my call* are the builder's.

## Intent

Flute Assistant is used by primary-school children (roughly ages 6 to 11) who are learning
the flute. The current UI is a musician's tool: it exposes every option the audio engine
supports. The request is to simplify the UI as much as possible and keep the tone and
styling playful. The child should be able to do the two things a teacher asks for without
help: "set the metronome to 80 and play along" and "check that your note is in tune".

## Goals

- A child can set a tempo, start and stop the beat, and pick 2/4, 3/4 or 4/4 unaided.
- A child can see at a glance which note they are playing and whether it is in tune.
- Everything on screen is big, round, colourful and bouncy. Copy is short, friendly and
  addresses the child directly (informal "ti" form in Croatian).
- English and Croatian stay fully translated.

## Non-goals

- Removing features from the audio engine, domain model, persistence or ViewModels.
  Subdivisions, click sounds, presets, tap tempo, volume and the A4 reference stay in the
  code so they can come back as an "advanced" mode later. Only the UI layer stops showing
  them.
- New art assets, custom fonts or sound effects. Playfulness comes from colour, shape, size,
  motion and emoji in the system font.
- A separate "parent / teacher" mode.

## Anti-goals

- A sterile, grey, Material-default look. If it looks like a settings screen, it failed.
- Numbers and jargon a child does not know: BPM, Hz, cents, Italian tempo markings,
  "subdivision", "reference pitch".
- Hidden traps: an option that is no longer visible must not keep affecting playback.

## What the child sees

### Metronome tab

Top to bottom, centred, on a warm cream background:

1. **Beat dots.** One big dot per beat in the bar (28 to 36 dp). The sounding beat pops
   and lights up; beat one is yellow, the others coral.
2. **Tempo ring.** A thick coral ring (about 250 dp) with the tempo number inside in a
   huge bold figure and, under it, a "speed feel" line: an emoji plus a word that maps
   the tempo to something a child understands. Dragging round the ring changes the
   tempo, as today. No tick marks, no "BPM" label, no Italian marking.
3. **Minus / plus.** Two big round buttons. A tap changes the tempo by one; holding a
   button keeps stepping (first after 400 ms, then about 12 steps per second). *My
   call:* this replaces the four ±1/±5 buttons.
4. **Play button.** One full-width pill, 72 dp tall, coral with a play icon and
   "Let's play!"; while playing it turns to a stop look with "Stop".
5. **Beats in a bar.** Three round toggle buttons labelled "2/4", "3/4", "4/4", the way
   the time signature is written in their sheet music. Selected = filled coral. 5/4 and
   6/8 are not offered. If a stored setting is one of the hidden signatures (a dev
   device), nothing is highlighted until the child picks one, and the dots still show
   the real beat count.

No screen title, no presets icon, no tap tempo, no subdivision row, no sound row, no
volume slider (the phone's volume buttons do that).

Speed feel words (English / Croatian), by tempo:

| Tempo        | Emoji | English    | Croatian   |
|--------------|-------|------------|------------|
| under 60     | 🐌    | Very slow  | Jako sporo |
| 60 to 89     | 🐢    | Slow       | Sporo      |
| 90 to 119    | 🚶    | Medium     | Srednje    |
| 120 to 159   | 🏃    | Fast       | Brzo       |
| 160 and up   | 🚀    | Super fast | Super brzo |

### Tuner tab

1. **Note name.** A huge bold letter (C, D, E… with ♯; Croatian H / B / Cis…) without
   the octave number. Green while in tune. "…" dimmed while nothing is heard.
2. **Gauge.** The semicircle keeps its green / amber / red zones and the springy needle,
   but loses the tick marks and the −50…+50 labels. Thicker stroke, chunkier needle.
3. **Status line.** One of: "🎉 Perfect!", "⬇️ Too low", "⬆️ Too high", or
   "👂 Play a note" while silent. Coloured like the needle zone.

No A4 reference control, no Hz, no cents, no neighbouring notes, no level bar. Status text
uses darker green / amber / red than the gauge so it stays readable on cream.

The microphone permission card keeps its two buttons but speaks to the child: "Can I use
the microphone?" / "I need to hear your flute to help you tune. Nothing is recorded or
sent anywhere." / "Yes, allow" / "Open settings".

### Shared

- Bottom navigation with the same two tabs and icons. Labels stay "Metronome" / "Tuner"
  (the words the teacher uses).
- Snackbar texts are reworded to be friendly ("Oops, the sound is busy right now. Try
  again in a moment.").
- The background-playback notification keeps its technical summary line and its own
  "Stop" string (it is a system surface read by adults, so it does not take the child's
  informal "Stani").

## Styling

- **Palette (my call):** fixed, not wallpaper-based. Dynamic colour is switched off so
  the app looks the same on every child's phone. Light: coral primary, teal secondary,
  sunny yellow tertiary, cream surface, peach surface-variant, dark navy text. Dark:
  lighter coral / teal / yellow on deep navy. Dark mode still follows the system.
- **Shapes:** everything rounded. Material shape tokens small 16 dp, medium 24 dp, large
  32 dp; buttons and toggles are circles or pills.
- **Type:** bigger and bolder than default: display 84 sp bold for tempo and note,
  titles 24 sp bold, body 18 sp. System font (Roboto); a rounded font is a possible
  follow-up.
- **Motion:** the existing beat flash and gauge spring stay. The "Perfect!" line
  bounces in when the note comes into tune.

## Code changes (the how, for the builder)

- `ui/theme`: new colours, `Shapes`, bigger `Typography`, `dynamicColor = false`.
- `ui/metronome/MetronomeScreen.kt`: new layout; `MetronomeContent` loses the
  tap-tempo, subdivision, sound, volume and presets callbacks. Hold-to-repeat tempo
  buttons via the button's `InteractionSource` pressed state.
- `ui/metronome/TempoDial.kt`: no ticks; speed-feel line instead of BPM + marking.
- `ui/metronome/MetronomeUiState.kt`: `tempoMarking()` becomes `tempoFeel()` returning a
  `TempoFeel` enum (emoji + string resource). Unit test updated.
- `ui/metronome/BeatIndicator.kt`: bigger dots.
- `ui/metronome/PresetsSheet.kt`: deleted (UI only; repository, use cases and ViewModel
  methods stay).
- `ui/metronome/MetronomeViewModel.kt`: on load, the options the UI no longer exposes
  (subdivision, sound) are reset to their defaults so a stale stored value cannot keep
  playing. One line plus a comment. The tuner ViewModel likewise starts from A4 = 440 Hz
  instead of the stored reference (a stale 442 or 466 would relabel notes with no visible
  cause); volume keeps its stored value since it only changes loudness.
- `ui/tuner/TunerScreen.kt`: new layout; `TunerContent` loses `onReferenceChange`.
- `ui/tuner/TuningGauge.kt`: no ticks or labels, thicker.
- `ui/common/Labels.kt`: add `noteName(midi)` (pitch class only) next to `noteLabel`.
- `ui/common/AppIcons.kt`: drop the icons nothing uses any more (bookmark, save,
  delete, volume).
- `res/values*/strings.xml`: remove the strings for hidden features, reword the rest,
  add the speed-feel words. Keep `config_summary`, subdivision and sound names (used by
  the notification and `LabelsTest`).
- Instrumented UI tests (`MetronomeScreenTest`, `TunerScreenTest`) updated to the new
  content; `LabelsTest` unchanged.

## Verification

- `./gradlew testDebugUnitTest` green.
- `./gradlew connectedDebugAndroidTest` green on the headless emulator.
- Screenshots of both tabs in English and Croatian, light and dark, checked by eye for
  the layout fitting a 1080×2400 phone without the main controls scrolling off.
