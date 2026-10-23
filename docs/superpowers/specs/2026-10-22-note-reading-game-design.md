# Note reading game: design

Date: 2026-10-22. Status: built under auto mode from seven user stories (the session ran
without questions); the calls marked *my call* are the builder's.

## Intent

The request, as user stories: open the app and immediately see a note name and a staff with
empty note positions; tap the place on the staff where that note is written; a green
highlight and the note's flute sound when right; the correct place marked, and another try,
when wrong; choose which notes are included (c1 and d1 at first, more later); see score,
streak and accuracy; a summary after every 10 notes with "Play again".

Today the app helps a child *play* a note (tuner) and keep time (metronome). This adds the
third thing a beginner struggles with: *reading* the note on the staff. It is a game, not
a lesson: one question at a time, instant feedback, short rounds.

## Goals

- A child can play a round unaided: the question, the staff and the one thing to do are
  on screen from the first launch, with no setup.
- Right and wrong are unmistakable (colour, a sound, a short line of text), and a wrong
  tap never blocks: the right place is shown and the child taps again.
- A parent or teacher can limit the notes to the ones the child has learned, once, and
  the choice sticks.
- Progress within a round is visible at a glance; the round ends in a summary with the
  numbers the story asks for and one button to go again.
- English and Croatian, note names the way the child's teacher writes them.

## Non-goals

- Sharps, flats, key signatures, rhythm, bass clef, or anything beyond "which line or
  space is this note on" in the treble clef.
- A history across sessions. Score, streak and accuracy are per round; the round survives
  rotation and the language change because the ViewModel is Activity-scoped, but not an app
  restart. *My call:* "improvement over time" is served by the summary numbers the child
  compares round to round; a stored history is a possible follow-up. (Revised below: leaving
  the tab resets the round.)
- Hearing the wrong note, or hearing the note before answering.
- A difficulty setting, a timer, or lives.

## Anti-goals

- Numbers or jargon on the game screen beyond the note name and the streak. Score and
  accuracy are shown as dots during the round and as numbers only in the summary.
- A crowded screen: the note picker is behind one pill, not a permanent row of fifteen
  chips.
- A screen reader hearing the answer in the tap targets.

## What the child sees: the "Notes" tab

A third tab in the bottom bar, after Metronome and Tuner: a music-note icon, labelled
"Notes" / "Note". *My call* on a tab: it is a separate activity, like the other two, and
the bar has room for three.

Top to bottom, centred, on the cream background:

1. **Round dots.** Ten small dots, one per note of the round: faint until answered, green
   when found on the first tap, amber when it took more than one, the current one ringed
   in coral. To the right, when the child has two or more first-tap answers in a row, a
   flame with the count: "🔥 3". This is the score, accuracy and progress without a number
   besides the streak. *My call.*
2. **The note name**, huge, as on the tuner: "C4" in English, "c1" in Croatian
   (`noteLabel`).
3. **The staff.** Five lines across the width with a treble clef at the left. To the right
   of the clef, a faint empty note head at every one of the fifteen places a natural note
   from c1 to c3 can sit (the ledger line below the staff, every line and space, and up to
   the second ledger line above), with faint ledger lines where they belong. The whole
   staff is tappable: a tap lands on the nearest place. Each place is 24 dp tall (a 48 dp
   staff space), so the staff is 360 dp high (revised twice below: 20 dp, then two columns,
   then one column at 24 dp).
4. **Status line.** "👆 Where is c1?" while waiting; "🎉 That's it!" after the right tap;
   "🙈 Not there, try again" after a wrong one.
5. **One pill** at the bottom: "🎵 Choose notes" / "🎵 Odaberi note".

### Playing

- **Right tap:** the head at that place fills green, the note sounds for about a second
  (the same soft flute-like tone the tuner plays), the status line bounces in with
  "That's it!", and after 1.2 s the next note appears. Taps during that second are
  ignored.
- **Wrong tap:** the tapped place fills red and stays red, the right place gets a green
  ring, the status says "Not there, try again", the flame goes out. The child taps again
  until the right place is hit; then the right-tap feedback plays. The note counts as "not
  first time" for the round (an amber dot).
- The next note is drawn at random from the chosen notes, never the same note twice in a
  row unless only one note is chosen.

### Round and summary

A round is 10 notes. After the tenth note's feedback, the name, staff and status are
replaced in place (no dialog) by the summary:

- A headline by score (right first time, out of 10): 9 or 10 "🏆 Amazing!", 7 or 8
  "🌟 Great job!", 4 to 6 "👍 Good work!", below that "💪 Keep practising!". *My call.*
- The score, big: "8 / 10", captioned "right first time" / "točno iz prve".
- "🔥 Best streak: 5" / "🔥 Najdulji niz: 5".
- "🎯 Accuracy: 80 %" / "🎯 Točnost: 80 %": correct taps out of all taps in the round, so
  a note that took three tries costs more than one that took two. *My call* on the
  definition, which makes accuracy and score different numbers.
- A full-width coral "Play again" / "Igraj opet" button, like the metronome's play
  button. It starts a fresh round at once.

The round dots stay above the summary, all ten filled.

### Choosing notes

Tapping "🎵 Choose notes" swaps the name, staff and status for a grid of fifteen pills,
three rows of five, "c1" to "c3", under the title "Which notes do you know?" / "Koje note
znaš?"; the chosen ones are filled teal like the level pills. The bottom pill now reads
"✅ Done" / "✅ Gotovo" and brings the game back. The last chosen note cannot be switched
off. Any change to the chosen notes starts a fresh round (revised below; at first only a
switched-off current note was redrawn). The choice is stored, like the tuner's note and
mode.

Default: c1, d1, e1, f1, g1. *My call:* the story's example starts at c1, and five notes
is a first-lesson range; everything else is one tap away.

Notes offered: the naturals from c1 to c3 (C4 to C6, MIDI 60, 62, 64, 65, 67, 69, 71, 72,
74, 76, 77, 79, 81, 83, 84), fifteen notes, exactly the fifteen places on the staff. *My
call:* the flute goes higher, but beyond c3 the ledger lines stack past what a primary-
school child reads, and sharps and flats need accidentals the game does not teach.

### Accessibility

The staff is "Staff, tap where the note goes" / "Crtovlje, dodirni gdje ide nota"; each
tappable place is "Staff position N" / "Mjesto N u crtovlju", counted from the bottom, so
the answer is not read out. The dots row is "Round progress, 3 of 10 notes"; the flame is
"Streak of 3". The picker grid is "Notes to practise" / "Note za vježbanje" with the note
names as the pill texts.

## Code changes (the how, for the builder)

- `domain/notes/StaffNotes.kt`: `ALL` (the fifteen naturals, in order; the index is the
  staff position), `DEFAULT_PRACTICE`, `positionOf(midi)` / `midiAt(position)`,
  `LINE_POSITIONS` (2, 4, 6, 8, 10), `ledgerLinesFor(position)`, `sanitize(set)`.
- `domain/repository/SettingsStore.kt`: `loadPracticeNotes()` / `savePracticeNotes(set)`;
  `SharedPreferencesSettingsStore` stores a string set of MIDI numbers under
  `practice_notes`; `FakeSettingsStore` gains the field.
- `ui/notes/NotesUiState.kt`: `practiceNotes`, `currentMidi`, `wrongTaps`, `solved`,
  `results`, `streak`, `bestStreak`, `taps`, `roundOver`; derived `answered`, `score`,
  `accuracyPercent`; `ROUND_LENGTH = 10`.
- `ui/notes/NotesViewModel.kt`: Activity-scoped like the others; `onNoteTapped(midi)`,
  `playAgain()`, `togglePracticeNote(midi)`, `stopSound()`; plays through the existing
  `ReferenceTonePlayer`; `FEEDBACK_MS = 1200`; a `Random` is passed through a secondary
  constructor so tests are deterministic.
- `ui/notes/NotesScreen.kt`: `NotesScreen` (ViewModel wiring, stops the sound when the
  tab is left) and the stateless `NotesContent`; `NoteStaff.kt` (Canvas plus a column of
  tappable strips, hand-built clef path), `RoundProgress.kt`, `RoundSummary.kt`,
  `PracticeNotePicker.kt`.
- `ui/MainScreen.kt`: `AppTab.NOTES`; `ui/common/AppIcons.kt`: `MusicNote`.
- Strings in both languages (listed in the plan).
- Tests: `StaffNotesTest`, `NotesUiStateTest`, `NotesViewModelTest` (JVM);
  `NotesScreenTest` (instrumented).

## Verification

- `./gradlew testDebugUnitTest` green.
- `./gradlew :app:connectedDebugAndroidTest` green on the headless emulator, one class at a time.
- Screenshots of the Notes tab in English and Croatian, light and dark: a question, a wrong
  tap, the summary, the picker.

## Revision, later the same day: leaving the tab resets the score

The user asked: "when user navigate to different view reset score".

Switching from the Notes tab to the metronome or the tuner throws the round away: the dots,
score, streak, taps and any feedback in progress go, a new note is drawn, and the chosen
notes stay. Coming back always starts from zero. Rotation and the language switch, which
re-create the Activity but are not navigation, still keep the round. *My call* on the
boundary: the reset is wired to the tab bar's click, not to the screen leaving composition,
so re-creation cannot trigger it.

Code: `NotesViewModel.resetRound()` (the body `playAgain` now delegates to), called from
`MainScreen`'s `NavigationBarItem.onClick` when the selected tab was Notes and the new one is
not. Test: `NotesViewModelTest.leaving the tab resets the round but keeps the chosen notes`.

## Revision: changing the chosen notes resets the score too

The user asked: "also reset when note selection is changed".

Switching a note on or off in the picker starts a fresh round from the new set at once: the
dots above the picker empty, score, streak and taps go, any feedback in progress is cut, and a
new note is drawn. Toggles that change nothing (the last note, a note the staff does not
offer) leave everything alone. So a score always belongs to one set of notes.

Code: `NotesViewModel.togglePracticeNote` now calls the same private `startRound(notes)` as
`resetRound`. Tests: `changing the chosen notes resets the round`, `changing the chosen notes
mid-feedback resets at once`, `the last note cannot be switched off and nothing resets`.

## Revision: two note columns, so the right place is easy to hit

The user found the places too close to select with confidence: fifteen heads in one column
at 20 dp pitch, with a finger covering two or three of them.

The heads now sit in two columns: every note on a line (ledger lines included) at 52 % of
the width, every note in a space at 76 %. Neighbouring pitches are therefore about 75 dp
apart on screen, and the nearest head in the same column is a whole staff space (40 dp)
away. Each place's tap target is its column's half of the width and the band reaching
halfway to the next place in that column, so a tap anywhere on the staff lands on the
nearest head and no two targets overlap. The ledger lines a place needs are drawn under its
own column. The heads, with room around them now, are drawn at 0.85 of a real head. *My
call* on two columns rather than a taller staff: the staff already takes 300 dp of the
screen, and line-versus-space is itself something a beginner learns.

Code: `NoteStaff.kt` (`LINE_COLUMN`, `SPACE_COLUMN`, a `BoxWithConstraints` holding the
Canvas and fifteen offset tap targets). Test:
`NotesScreenTest.neighbouring_places_sit_in_different_columns_with_big_targets`.

## Revision: one column again, with more room between the places

The user asked: "add bigger gap between placeholder columns and keep everything in one
column". Read as: one column, and more air between the placeholders.

The heads are back in one column at 60 % of the width, and the staff grew instead: each
place is 24 dp (a 48 dp staff space), the staff 360 dp, which is as much as a 360 x 800 dp
phone with three-button navigation fits once the rest is trimmed: the note name drops to
64 sp (from the tuner's 84), the spacers around the dots, the status line and the pill lose
a few dp each. The empty places are small faint circles (0.3 of a place in radius), so
there is clear air between neighbours; a tapped place still fills as a real note head. Tap
targets are full-width strips of 24 dp, one per place. *My call* on the trade: the user
wants one column, so the only way to more room is a taller staff, and the name can spare
the height.

Code: `NoteStaff.kt` (`StaffPlaceHeight = 24.dp`, `NOTE_COLUMN`, circles, strips),
`NotesScreen.kt` (64 sp name, tighter spacers). Test:
`NotesScreenTest.places_stack_in_one_column_with_room_between_them`.

## Revision: the note sound can be switched off; volume keys

The user asked for a toggle so the note replay can be turned off, and noticed that the
phone's volume buttons did not control the app's volume.

- **Sound toggle.** Under the grid in "Choose notes", one wide pill: "🔊 Hear the note" /
  "🔊 Poslušaj ton", filled teal while on; off it reads "🔇 Hear the note" and is unfilled.
  Off, a found place still turns green for the same 1.2 s, only silently. The choice is
  remembered (`SettingsStore.saveNoteSound`, default on). *My call* on the place: it is a
  setting, so it lives with the only other setting, behind the one pill, and the game
  screen stays as sparse as before.
- **Volume keys.** The Activity now declares the media stream as its volume-control stream.
  Everything the app plays is media, but while nothing was sounding the keys adjusted the
  ringer, so pressing them between notes seemed to do nothing. With the declaration they
  adjust the media volume whenever the app is in front.

Code: `NotesUiState.soundOn`, `NotesViewModel.toggleSound`, `PracticeNotePicker`'s toggle,
`MainActivity.volumeControlStream`. Tests: `NotesViewModelTest` (two cases),
`NotesScreenTest.the_sound_toggle_lives_with_the_note_picker`.

## Revision: no "Where is …?" line

The user: "collapse target note with label where is the note, they are redundant". The huge
name is the question; the line under the staff now carries only the feedback ("🎉 That's
it!", "🙈 Not there, try again") and stays empty, at its height, while waiting, so the pill
below does not jump. The `notes_where_is` string is gone.

## Revision: the note name and the picker button share one row

The user: "compose target note and pick note button in one row, watch out to UX, cognitive
load, and make it as much as possible intuitive".

One row under the dots carries the hero of the moment, centred, and a single small round
button at its end, the same arrangement as the tuner's speaker button on the note band, so
nothing new has to be learned:

| State | Row text | Button |
|---|---|---|
| playing | the note's name, 64 sp (green once found) | soft teal checklist: opens the picker |
| round over | the score, "9 / 10", in coral | the same checklist button |
| choosing | "Which notes?" / "Koje note?" | filled teal tick: closes the picker |

The picker below the row is the grid plus the sound toggle; the summary below it is the
caption "right first time", the headline, best streak, accuracy and "Play again". The
bottom "🎵 Choose notes" pill is gone, and its 88 dp went to the staff: 26 dp per place
(a 52 dp staff space, 390 dp high), which still fits a 360 x 800 dp phone with
three-button navigation. The button's accessibility names are "Choose notes" / "Odaberi
note" and "Done" / "Gotovo". *My call* on a checklist icon rather than a gear: the panel is
first of all a list of notes to tick, and the tick that closes it rhymes with it.

Code: `NotesScreen.kt` (`HeroRow`), `PracticeNotePicker` (no title), `RoundSummary` (no
score line), `AppIcons.Checklist` / `Check`, `StaffPlaceHeight = 26.dp`. Test:
`NotesScreenTest.the_choose_button_sits_beside_the_note_name`.

## Revision: the round dots move to the bottom

The user: "move score indicator below notes (dots indicating how many attempts pending)".
The dots row (with the streak flame) now sits last, under the staff and its feedback line,
and likewise under the summary or the picker, so the order is name row, staff, feedback,
dots. The question stays at the top, where the eye starts; progress is read after the tap.
Test: `NotesScreenTest.the_round_dots_sit_below_the_staff_and_its_feedback`.

## Revision: how long the round took

The user: "add also timer how much time is needed for completing whole session".

The summary gains "⏱️ Time: 1:23" / "⏱️ Vrijeme: 1:23" under the accuracy: minutes and
whole seconds from the round's first tap (right or wrong) to the tenth note found. The
1.2 s feedback pauses are part of it, as they are part of playing; time spent idle on the
tab before the first tap is not, so a child who looks at the first note for a while, or
comes back to the tab later, is not charged for it. *My call:* no running clock on the game
screen. A ticking number while a child is still finding the note is pressure, and the
screen is meant to hold only the question and the staff; the time in the summary is
enough to compare rounds. A live clock can be added later if wanted.

Code: `NotesUiState.roundMillis`, `formatRoundTime`, `NotesViewModel` with a
`MonotonicClock`, `RoundSummary`. Tests: `NotesViewModelTest.the round's time runs from
the first tap to the last note found`, `NotesUiStateTest.round time reads as minutes and
seconds`, the summary case in `NotesScreenTest`.

## Revision: the staff fills the screen and hides unneeded ledger lines

The user: "use all available space on screen for drawing notes so placeholders can be
bigger, if notes are not selected dont show lines outside regular notes".

- **Height.** The staff takes all the height left between the name row and the dots (the
  tab no longer scrolls), and the size of a place follows from that height, so the places
  are as big as the phone allows. On the emulator the staff grew from 390 dp to about
  620 dp.
- **Only the places that are needed.** The staff always shows the five lines with their
  spaces and the two neighbouring spaces, d1 to g2 (eleven places, no ledger lines). The
  ledger-line places (c1 below; a2, h2, c3 above) appear only as far as a chosen note
  needs them: with the default c1 to g1, c1's ledger line shows and nothing above the
  staff; choose c3 and the three places above appear. Fewer places also means bigger
  places. One place of margin is kept above and below so the clef is never cut; the end
  places' tap strips cover it.

Code: `StaffNotes.STAFF_POSITIONS` (1..11) and `visiblePositions(practice)`; `NoteStaff`
on a `BoxWithConstraints`, with `practiceNotes`, given `weight(1f)` by `NotesContent`.
Tests: `StaffNotesTest.visible places stretch from the staff to the chosen ledger notes`;
`NotesScreenTest.ledger_places_outside_the_staff_appear_only_for_chosen_notes` and
`the_staff_takes_the_height_left_on_the_screen`.

## Revision: an OK button closes the picker; the sound setting looks like a setting

The user: "settings in note view does not have great UX, user can't find how to confirm
notes and what play the note means, improve UX, maybe it's good idea to replace checkmark
icon with ok button and somehow signal play the note is not related to selected notes".

Two readings went wrong for the same reason: the way out was a 48 dp tick at the end of
the title row, and the sound toggle was a pill with the same shape and teal fill as a chosen
note, straight under the grid, labelled "Hear the note", so it passed for a sixteenth note
or for a button that plays something now.

- **OK button.** Under the picker, one big full-width coral button, "✓ OK" / "✓ U redu",
  the same shape as the summary's "Play again", closes the picker. Choices still apply as
  they are tapped, so OK only brings the game back. While the picker is open the title row
  is "Which notes?" alone: the checklist button hides, so there is one way out, not two.
- **Sound setting.** Under the grid, past a divider, one settings row of the shape every
  phone uses: "🔊 Play the note you find" / "🔊 Odsviraj pogođenu notu" with a
  switch at its end (🔇 while off). A different control in its own section says "this is a
  setting, not a note", and the label now says what it does. *My call* on a switch rather
  than another pill: the pill was the problem.

Code: `PracticeNotePicker` (`SoundSettingRow`, `onDone`), `NotesScreen.kt` (`HeroRow`
without the tick), strings `choose_notes_done`, `note_sound`. Tests:
`NotesScreenTest.choosing_notes_toggles_them`,
`the_ok_button_is_big_and_sits_under_the_sound_switch`,
`the_sound_toggle_lives_with_the_note_picker`, `croatian_names_and_copy`.

## Revision: marks fit their place (2026-10-23)

The user: "circles that shows miss or hit of note is bigger than placeholder and if user pick
adjacent note second pick is hard to hit because red circle overlap placeholder, analyze how
to improve UX and make placeholders easier to precisely hit without any overlap".

The tap strips were never the problem (full width, one pitch tall, a tap anywhere in a strip
counts); what the child could see was. The red and green marks were real-size note heads,
1.7 pitches tall, and the hint ring 2 pitches, so after a miss directly under the asked note
the red head and the green ring overlapped by a third and buried the placeholder to aim at.

Of four options (marks that fit the place; oval marks that fit; keep the heads and fade the
miss after a moment; keep the heads and show the strips as bands) the user chose the first:

- Every mark is the place's own circle. The placeholder grew from 0.3 to `MARK_RADIUS` 0.4
  of the pitch (80 % of the strip), a miss fills it red, the found note fills it green, the
  hint rings it green with the ring's outer edge at the same radius. Nothing crosses a strip
  boundary; 20 % of the pitch stays clear between neighbours. Ledger lines shrank to match
  (`LEDGER_HALF` 0.9 pitches). The marks no longer look like oval note heads; the big name
  above is the question and the staff is the answer board, so "tap a circle, it turns red or
  green" is the whole model.

Code: `NoteStaff.kt` (`MARK_RADIUS`, `LEDGER_HALF`, `mark()`). Test:
`NotesScreenTest.marks_stay_inside_their_own_place` reads the rendered pixels and checks that
the red rows stay inside the missed place's strip and the green rows inside the asked one's.
