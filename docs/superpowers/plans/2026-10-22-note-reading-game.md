# Note Reading Game Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A third "Notes" tab where a child is shown a note name, taps its place on a treble staff, gets green-plus-sound or red-plus-the-right-place feedback, and sees a summary after every ten notes.

**Architecture:** A pure-Kotlin `StaffNotes` object maps the fifteen natural notes c1 to c3 to staff positions. `NotesViewModel` (Activity-scoped, like the other two) owns the round in a `NotesUiState` StateFlow and plays the note through the existing `ReferenceTonePlayer`; the chosen notes persist through `SettingsStore`. `NotesContent` is the stateless, tested composable: a Canvas staff with a column of tappable strips, the round dots, the summary and the note picker.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Hilt, SharedPreferences, JUnit 4 unit tests, Compose UI tests on the headless emulator `Medium_Phone_API_37.0` (boot with `-no-audio`).

**Spec:** `docs/superpowers/specs/2026-10-22-note-reading-game-design.md`

## Global Constraints

- No commits: this repository's CLAUDE.md forbids the agent committing; every "commit" step means "leave the change in the working tree".
- Notes offered: MIDI 60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84 (c1 to c3, naturals), fifteen, index = staff position. Default practice set: 60, 62, 64, 65, 67.
- A round is 10 notes (`ROUND_LENGTH`). Feedback after a right tap lasts `FEEDBACK_MS = 1200`; the tone sounds for the same time at `TunerViewModel.REFERENCE_TONE_VOLUME`.
- Staff place pitch 24 dp (48 dp staff space; revised from 18, then 20); staff lines at positions 2, 4, 6, 8, 10; ledger lines at 0, 12, 14.
- Every user-visible string in both `values/strings.xml` and `values-hr/strings.xml`; Croatian in the informal singular; note names via `noteLabel` (C4 / c1).
- No numbers on the game screen besides the note name and the streak count; the summary carries the score, best streak and accuracy.
- No new dependencies, drawables or fonts; the clef and the tab icon are hand-built paths.

## Review Focus

1. A stored practice set containing a sharp, an out-of-range note, or nothing (an old build, corrupt prefs): the game must start with notes it can draw. Test in Task 1 (`sanitize drops foreign notes and never returns empty`) and Task 3 (`stored practice notes are restored and sanitized`).
2. Only one note chosen: the next note must be that note again, not a crash or an endless loop. Test in Task 3 (`a single practice note repeats`).
3. Switching off the note being asked: a new question is drawn from the remaining notes. Test in Task 3 (`switching off the current note draws another`).
4. Taps during the green second: ignored, no double counting. Test in Task 3 (`taps while the note sounds are ignored`).
5. Leaving the tab mid-tone: the sound stops so the tuner's microphone does not hear it. Test in Task 3 (`stopSound silences the tone`).

---

### Task 1: Domain: the notes on the staff

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/domain/notes/StaffNotes.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/domain/notes/StaffNotesTest.kt`

**Interfaces:**
- Produces: `object StaffNotes { val ALL: List<Int>; val DEFAULT_PRACTICE: Set<Int>; const val POSITIONS = 15; val LINE_POSITIONS: List<Int>; fun positionOf(midi: Int): Int /* -1 when not offered */; fun midiAt(position: Int): Int; fun ledgerLinesFor(position: Int): List<Int>; fun sanitize(notes: Set<Int>): Set<Int> }`.

- [ ] **Step 1: Write the failing tests** in `StaffNotesTest`: `all is the fifteen naturals from c1 to c3` (`ALL == listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84)`, `POSITIONS == ALL.size`); `position is the index on the staff` (`positionOf(60) == 0`, `positionOf(64) == 2`, `positionOf(84) == 14`, `positionOf(61) == -1`, `positionOf(96) == -1`, `midiAt(4) == 67`); `lines and ledger lines` (`LINE_POSITIONS == listOf(2, 4, 6, 8, 10)`, `ledgerLinesFor(0) == listOf(0)`, `ledgerLinesFor(1).isEmpty()`, `ledgerLinesFor(6).isEmpty()`, `ledgerLinesFor(12) == listOf(12)`, `ledgerLinesFor(13) == listOf(12)`, `ledgerLinesFor(14) == listOf(12, 14)`); `sanitize drops foreign notes and never returns empty` (`sanitize(setOf(60, 61, 96)) == setOf(60)`, `sanitize(emptySet()) == DEFAULT_PRACTICE`, `sanitize(setOf(61)) == DEFAULT_PRACTICE`, `DEFAULT_PRACTICE == setOf(60, 62, 64, 65, 67)`).
- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest --tests '*StaffNotesTest*'`; expected: compilation failure (unresolved `StaffNotes`).
- [ ] **Step 3: Implement `StaffNotes`** as the Interfaces block; `ALL` is the naturals of 60..84 in order, `positionOf` is `ALL.indexOf`, `ledgerLinesFor` returns the even positions between the note and the staff (below 2: `0 downTo position step 2` reversed; above 10: `12..position step 2`).
- [ ] **Step 4: Run** the same command; expected: PASS.

### Task 2: Settings: remember the chosen notes

**Files:**
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/domain/repository/SettingsStore.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/data/settings/SharedPreferencesSettingsStore.kt`
- Modify: `app/src/test/java/hr/bertlab/fluteassistant/fakes/Fakes.kt`

**Interfaces:**
- Consumes: `StaffNotes.DEFAULT_PRACTICE`.
- Produces: `SettingsStore.loadPracticeNotes(): Set<Int>`, `SettingsStore.savePracticeNotes(notes: Set<Int>)`; `FakeSettingsStore(var practiceNotes: Set<Int> = StaffNotes.DEFAULT_PRACTICE)`.

- [ ] **Step 1: Add the two methods** to the interface (doc: the notes the reading game asks, as MIDI numbers; not sanitised here) and to `SharedPreferencesSettingsStore` under key `"practice_notes"` as a string set of decimal MIDI numbers (`getStringSet` copied into a new set, non-numeric entries dropped), default `StaffNotes.DEFAULT_PRACTICE`.
- [ ] **Step 2: Extend `FakeSettingsStore`** with the field and the two overrides.
- [ ] **Step 3: Run** `./gradlew :app:compileDebugUnitTestKotlin -q`; expected: BUILD SUCCESSFUL.

### Task 3: Round state and ViewModel

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/NotesUiState.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/NotesViewModel.kt`
- Test: `app/src/test/java/hr/bertlab/fluteassistant/ui/notes/NotesUiStateTest.kt`, `app/src/test/java/hr/bertlab/fluteassistant/ui/notes/NotesViewModelTest.kt`

**Interfaces:**
- Consumes: `StaffNotes`, `SettingsStore.loadPracticeNotes/savePracticeNotes`, `ReferenceTonePlayer.play/stop`, `NoteMath.midiToFrequency`, `TunerViewModel.REFERENCE_TONE_VOLUME`.
- Produces: `data class NotesUiState(practiceNotes: Set<Int>, currentMidi: Int, wrongTaps: Set<Int> = emptySet(), solved: Boolean = false, results: List<Boolean> = emptyList(), streak: Int = 0, bestStreak: Int = 0, taps: Int = 0, roundOver: Boolean = false) { val answered: Int; val score: Int; val accuracyPercent: Int? ; companion { const val ROUND_LENGTH = 10 } }`; `class NotesViewModel internal constructor(settings, tonePlayer, random: Random)` with `@Inject constructor(settings, tonePlayer)`, `state: StateFlow<NotesUiState>`, `onNoteTapped(midi: Int)`, `playAgain()`, `togglePracticeNote(midi: Int)`, `stopSound()`, `companion { const val FEEDBACK_MS = 1200 }`.

- [ ] **Step 1: Write the failing state tests** in `NotesUiStateTest`: `score counts first-tap answers` (`results = [true, false, true]` gives `answered == 3`, `score == 2`); `accuracy is correct taps out of all taps` (`results` of size 2 with `taps = 3` gives 67; `taps = 0` gives null; size 10 with `taps = 10` gives 100).
- [ ] **Step 2: Write the failing ViewModel tests** in `NotesViewModelTest` (`StandardTestDispatcher`, `Dispatchers.setMain`, `FakeSettingsStore`, `FakeReferenceTonePlayer`, `Random(1)`): `first note comes from the practice set` (`currentMidi in DEFAULT_PRACTICE`); `stored practice notes are restored and sanitized` (`settings.practiceNotes = setOf(69, 61, 96)` gives `practiceNotes == setOf(69)` and `currentMidi == 69`); `the right tap turns green, sounds the note and moves on` (`onNoteTapped(current)`: `solved`, `results == [true]`, `streak == 1`, `tonePlayer.played.single()` is `Played(midiToFrequency(current), FEEDBACK_MS, REFERENCE_TONE_VOLUME)`; `advanceTimeBy(FEEDBACK_MS + 1)`: `!solved`, `wrongTaps` empty, `tonePlayer.stopCount == 1`, `currentMidi != previous` with the default set); `a wrong tap is marked, resets the streak and the note stays` (tap a note other than current: `wrongTaps == setOf(it)`, `streak == 0`, `taps == 1`, `currentMidi` unchanged, nothing played; then the right tap: `results == [false]`, `solved`); `taps while the note sounds are ignored` (after the right tap, tapping anything before the delay leaves `taps == 1` and `results.size == 1`); `ten notes end the round and play again starts fresh` (answer 10 notes each followed by `advanceTimeBy(FEEDBACK_MS + 1)`: `roundOver`, `answered == 10`; then `onNoteTapped` of anything changes nothing; `playAgain()`: `!roundOver`, `results` empty, `taps == 0`, `streak == 0`, `bestStreak == 0`, `currentMidi in practiceNotes`); `best streak survives a miss` (three first-tap rights, one wrong: `streak == 0`, `bestStreak == 3`); `a single practice note repeats` (toggle off every default note but 60: repeated right taps keep `currentMidi == 60`); `switching off the current note draws another` (`togglePracticeNote(current)`: `current !in practiceNotes`, `currentMidi in practiceNotes`, `settings.practiceNotes == practiceNotes`, `wrongTaps` empty); `the last note cannot be switched off` (leave one, toggle it: still there); `switching on a note is saved` (`togglePracticeNote(84)`: 84 in both the state and `settings.practiceNotes`; `togglePracticeNote(61)` changes nothing); `the next note is never the one just asked` (with two notes chosen, 20 rounds of right-tap-and-advance never repeat consecutively); `stopSound silences the tone` (after a right tap, `stopSound()` gives `tonePlayer.stopCount == 1`).
- [ ] **Step 3: Run** `./gradlew :app:testDebugUnitTest --tests '*Notes*'`; expected: compilation failure.
- [ ] **Step 4: Implement `NotesUiState`** as the Interfaces block; `accuracyPercent` is `(results.size * 100.0 / taps).roundToInt()`.
- [ ] **Step 5: Implement `NotesViewModel`**: initial state from `StaffNotes.sanitize(settings.loadPracticeNotes())` and a drawn note; `onNoteTapped` ignores taps while `solved` or `roundOver`, on the right note sets `solved`, appends `wrongTaps.isEmpty()` to `results`, counts the tap, advances the streak (or zeroes it when not first try), plays the tone and launches a `delay(FEEDBACK_MS)` job that stops the tone and either marks `roundOver` (ten answered) or draws the next note; on a wrong note adds it to `wrongTaps`, counts the tap and zeroes the streak. Drawing: `(practiceNotes - avoid).ifEmpty { practiceNotes }.sorted()[random.nextInt(size)]`. `onCleared` cancels the job and stops the tone.
- [ ] **Step 6: Run** the same command; expected: PASS.

### Task 4: Strings, icon and the third tab

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-hr/strings.xml`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/common/AppIcons.kt`
- Modify: `app/src/main/java/hr/bertlab/fluteassistant/ui/MainScreen.kt`

**Interfaces:**
- Produces: `AppIcons.MusicNote`; `AppTab.NOTES`; string ids `tab_notes`, `notes_where_is`, `notes_correct`, `notes_wrong`, `staff_description`, `staff_position`, `round_progress`, `streak_badge`, `streak_description`, `round_amazing`, `round_great`, `round_good`, `round_keep_going`, `round_score`, `round_score_caption`, `round_best_streak`, `round_accuracy`, `play_again`, `choose_notes`, `choose_notes_done`, `choose_notes_title`, `practice_notes`.

- [ ] **Step 1: Add the strings** (en / hr): `tab_notes` "Notes" / "Note"; `notes_where_is` "👆 Where is %1$s?" / "👆 Gdje je %1$s?"; `notes_correct` "🎉 That's it!" / "🎉 Tako je!"; `notes_wrong` "🙈 Not there, try again" / "🙈 Nije tu, probaj opet"; `staff_description` "Staff, tap where the note goes" / "Crtovlje, dodirni gdje ide nota"; `staff_position` "Staff position %1$d" / "Mjesto %1$d u crtovlju"; `round_progress` (a plurals resource on the round length) "Round progress, %1$d of %2$d notes" / "Napredak, %1$d od %2$d nota"; `streak_badge` "🔥 %1$d" (both); `streak_description` "Streak of %1$d" / "Niz od %1$d"; `round_amazing` "🏆 Amazing!" / "🏆 Nevjerojatno!"; `round_great` "🌟 Great job!" / "🌟 Odlično!"; `round_good` "👍 Good work!" / "👍 Dobro!"; `round_keep_going` "💪 Keep practising!" / "💪 Vježbaj dalje!"; `round_score` "%1$d / %2$d" (both); `round_score_caption` "right first time" / "točno iz prve"; `round_best_streak` "🔥 Best streak: %1$d" / "🔥 Najdulji niz: %1$d"; `round_accuracy` "🎯 Accuracy: %1$d %%" / "🎯 Točnost: %1$d %%"; `play_again` "Play again" / "Igraj opet"; `choose_notes` "🎵 Choose notes" / "🎵 Odaberi note"; `choose_notes_done` "✅ Done" / "✅ Gotovo"; `choose_notes_title` "Which notes do you know?" / "Koje note znaš?"; `practice_notes` "Notes to practise" / "Note za vježbanje".
- [ ] **Step 2: Add `AppIcons.MusicNote`** from the Material `music_note` path `M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z`.
- [ ] **Step 3: Add `AppTab.NOTES(R.string.tab_notes)`** after `TUNER` with that icon; `MainScreen` takes a `notesViewModel: NotesViewModel = hiltViewModel()`, collects its state and shows `NotesScreen(state, viewModel)` for the tab (Task 5 creates it; this step compiles only after Task 5).

### Task 5: The screen

**Files:**
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/NoteStaff.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/RoundProgress.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/RoundSummary.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/PracticeNotePicker.kt`
- Create: `app/src/main/java/hr/bertlab/fluteassistant/ui/notes/NotesScreen.kt`

**Interfaces:**
- Consumes: Task 3's state and ViewModel, Task 4's strings, `noteLabel`, `InTuneGreen`/`OutOfTuneRed`/`SlightlyOffAmber` and their `*Text` variants.
- Produces: `NoteStaff(currentMidi: Int, wrongTaps: Set<Int>, solved: Boolean, onNoteTapped: (Int) -> Unit, modifier)`, `RoundProgress(results: List<Boolean>, streak: Int, modifier)`, `RoundSummary(state: NotesUiState, onPlayAgain: () -> Unit, modifier)`, `PracticeNotePicker(selected: Set<Int>, onToggle: (Int) -> Unit, modifier)`, `NotesScreen(state, viewModel, modifier)`, `NotesContent(state, onNoteTapped, onPlayAgain, onTogglePracticeNote, modifier)`.

- [ ] **Step 1: `NoteStaff`**: a `Box` of height `24.dp * StaffNotes.POSITIONS` holding a `Canvas` (staff lines at `LINE_POSITIONS`, the clef as a stroked `Path` scaled to the staff space at the left, small faint circles at every position in a column at 60 % of the width with faint ledger lines at 0, 12, 14 (a two-column split was tried and rejected by the user); wrong taps as red filled heads with their ledger lines; the current note as a green ring once `wrongTaps` is not empty and as a green filled head once `solved`) under a `Column` of fifteen strips, top strip = position 14, each `fillMaxWidth().height(24.dp).clickable { onNoteTapped(StaffNotes.midiAt(position)) }` with `contentDescription = stringResource(R.string.staff_position, position + 1)` and `Role.Button`; the box is `semantics { contentDescription = staff_description }`.
- [ ] **Step 2: `RoundProgress`**: ten 14 dp circles in a row, `results[i]` green / amber, the index `results.size` ringed in `primary`, the rest faint; `contentDescription = round_progress(results.size, ROUND_LENGTH)`; when `streak >= 2`, a `Text(streak_badge)` with `contentDescription = streak_description` on the right.
- [ ] **Step 3: `RoundSummary`**: headline by `state.score` (>= 9 amazing, >= 7 great, >= 4 good, else keep going), `round_score(score, ROUND_LENGTH)` in `displayLarge`, the caption, best streak, accuracy (`accuracyPercent ?: 0`), and a full-width 80 dp coral `Button` with `AppIcons.PlayArrow` and `play_again`.
- [ ] **Step 4: `PracticeNotePicker`**: a `Text(choose_notes_title, titleLarge)` over a `FlowRow(maxItemsInEachRow = 5)` of `Surface(selected, onClick)` pills (`weight(1f)`, 52 dp, teal when selected, `Role.Checkbox`), texts `noteLabel(midi)`; the row group has `contentDescription = practice_notes`.
- [ ] **Step 5: `NotesScreen` / `NotesContent`**: `NotesScreen` renders `NotesContent` with the ViewModel's intents and a `DisposableEffect(Unit) { onDispose { viewModel.stopSound() } }`. `NotesContent` is a scrolling `Column` with 24 dp side padding: `RoundProgress`; then, by a `rememberSaveable` `choosingNotes` flag and `state.roundOver`, either the picker, the summary, or the note name (`displayLarge`, green while `solved`) over `NoteStaff` over the status line (`notes_where_is(noteLabel)` / `notes_correct` / `notes_wrong`, coloured like the tuner's status, bouncing in on `solved`); and the bottom pill (`choose_notes` / `choose_notes_done`, filled teal while choosing) that flips the flag.
- [ ] **Step 6: Build** `./gradlew :app:compileDebugKotlin -q`; expected: BUILD SUCCESSFUL. Then `./gradlew testDebugUnitTest`; expected: all green.

### Task 6: Instrumented tests and screenshots

**Files:**
- Create: `app/src/androidTest/java/hr/bertlab/fluteassistant/ui/notes/NotesScreenTest.kt`

- [ ] **Step 1: Write the tests** rendering `NotesContent` inside `LocaleProvider` with a `MutableState<NotesUiState>` the callbacks update: `shows_the_note_name_and_asks_where_it_is` ("C4" and "👆 Where is C4?" displayed, "Staff, tap where the note goes" exists); `tapping_a_staff_position_reports_its_note` (click "Staff position 1" reports 60, "Staff position 15" reports 84); `a_right_tap_says_so` (state `solved = true`: "🎉 That's it!"); `a_wrong_tap_says_try_again` (`wrongTaps = setOf(62)`: "🙈 Not there, try again"); `the_summary_shows_the_numbers_and_plays_again` (`roundOver`, `results` eight true two false, `taps = 13`, `bestStreak = 5`: "🌟 Great job!", "8 / 10", "🔥 Best streak: 5", "🎯 Accuracy: 77 %", click "Play again" counts one); `choosing_notes_toggles_them` (click "🎵 Choose notes", "Which notes do you know?" shown, "C4" selected and "C6" not, click "C6" reports 84, click "✅ Done" brings "👆 Where is C4?" back); `streak_badge_appears_from_two` (`streak = 3`: "🔥 3" displayed; `streak = 1`: absent); `croatian_names_and_copy` (`Locale("hr")`: "c1", "👆 Gdje je c1?", "🎵 Odaberi note").
- [ ] **Step 2: Run** `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=hr.bertlab.fluteassistant.ui.notes.NotesScreenTest` on the emulator; expected: all pass. Then `TunerScreenTest`, `MetronomeScreenTest`, `LabelsTest`, `LanguageSwitchTest` one at a time; expected: unchanged, all pass.
- [ ] **Step 3: Screenshots**: `adb install -r` the debug APK, open the Notes tab, capture the question, a wrong tap, the picker and (after ten answers) the summary, in English and Croatian, light and dark; check the clef and heads by eye and adjust the clef path if it looks wrong.

### Task 7: Docs

**Files:**
- Modify: `CLAUDE.md` (Architecture: a "Note reading path" paragraph and the third tab in UI conventions; Testing conventions: `NotesScreenTest`).

- [ ] **Step 1: Document** the new package, the ViewModel contract, the staff geometry constants and where the fifteen-note pool lives, in the style of the tuner paragraph.
