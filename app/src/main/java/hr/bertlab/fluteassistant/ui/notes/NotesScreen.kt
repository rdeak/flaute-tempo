package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.InTuneGreenText
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRed
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRedText

/**
 * Stateful entry point: wires the ViewModel to the stateless [NotesContent] and silences
 * the note when the tab goes away mid-feedback (the tuner's microphone would hear it).
 */
@Composable
fun NotesScreen(
    state: NotesUiState,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.stopSound() }
    }
    NotesContent(
        state = state,
        onNoteTapped = viewModel::onNoteTapped,
        onPlayAgain = viewModel::playAgain,
        onTogglePracticeNote = viewModel::togglePracticeNote,
        onToggleSound = viewModel::toggleSound,
        modifier = modifier,
    )
}

/**
 * The whole game, top to bottom: one row holding the hero of the moment (the note's name,
 * which is the whole question; the score once the round is over; "Which notes?" while
 * choosing) with the single round button at its end that opens the note picker; then the
 * staff, which takes all the height left, with its feedback line, or the round summary, or
 * the picker, whose own big OK button closes it (the round button hides meanwhile, so there
 * is one way out); and at the bottom the round's dots. Choosing notes pauses the question
 * in place; it comes back untouched.
 */
@Composable
fun NotesContent(
    state: NotesUiState,
    onNoteTapped: (Int) -> Unit,
    onPlayAgain: () -> Unit,
    onTogglePracticeNote: (Int) -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosingNotes by rememberSaveable { mutableStateOf(false) }
    val dark = isSystemInDarkTheme()
    val right = if (dark) InTuneGreen else InTuneGreenText
    val wrong = if (dark) OutOfTuneRed else OutOfTuneRedText

    // No scrolling: the staff takes whatever height is left, so the tab always fits the screen.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        HeroRow(
            text = when {
                choosingNotes -> stringResource(R.string.choose_notes_title)
                state.roundOver -> stringResource(R.string.round_score, state.score, NotesUiState.ROUND_LENGTH)
                else -> noteLabel(state.currentMidi)
            },
            // A notch under the tuner's letter: the staff below needs the height more than the name does.
            style = if (choosingNotes) MaterialTheme.typography.titleLarge
            else MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 72.sp),
            color = when {
                choosingNotes -> MaterialTheme.colorScheme.onSurface
                state.roundOver -> MaterialTheme.colorScheme.primary
                state.solved -> right
                else -> MaterialTheme.colorScheme.onSurface
            },
            showChooseButton = !choosingNotes,
            onChoose = { choosingNotes = true },
        )
        when {
            choosingNotes -> {
                PracticeNotePicker(
                    selected = state.practiceNotes,
                    onToggle = onTogglePracticeNote,
                    soundOn = state.soundOn,
                    onToggleSound = onToggleSound,
                    onDone = { choosingNotes = false },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.weight(1f))
            }
            state.roundOver -> {
                RoundSummary(state = state, onPlayAgain = onPlayAgain, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
            }
            else -> Question(state = state, onNoteTapped = onNoteTapped, right = right, wrong = wrong)
        }
        // The round's dots last, under whatever is on screen: how far the round has come and
        // how many notes are left, read after the tap rather than before it.
        Spacer(Modifier.height(12.dp))
        RoundProgress(results = state.results, streak = state.streak)
        Spacer(Modifier.height(16.dp))
    }
}

/** The row's height: the 64 sp name's line, which also fits the button with air around it. */
private val HeroRowHeight = 72.dp
private val HeroButtonSize = 48.dp

/**
 * The hero [text] centred across the width and, while [showChooseButton], a soft checklist
 * button at the end of the row that opens the picker ([onChoose]). While the picker is open
 * the row is text alone: the picker's OK button is the way back. The text keeps clear of
 * the button's end of the row either way, so it never shifts; the longest hero, "10 / 10",
 * still fits a 360 dp phone.
 */
@Composable
private fun HeroRow(
    text: String,
    style: TextStyle,
    color: Color,
    showChooseButton: Boolean,
    onChoose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HeroRowHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier.padding(horizontal = HeroButtonSize + 8.dp),
        )
        if (showChooseButton) {
            FilledTonalIconButton(
                onClick = onChoose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(HeroButtonSize),
            ) {
                Icon(AppIcons.Checklist, contentDescription = stringResource(R.string.choose_notes))
            }
        }
    }
}

/**
 * The staff, taking all the height the column has left, and the feedback line; the name
 * above, in the hero row, is the question itself.
 */
@Composable
private fun ColumnScope.Question(state: NotesUiState, onNoteTapped: (Int) -> Unit, right: Color, wrong: Color) {
    NoteStaff(
        currentMidi = state.currentMidi,
        wrongTaps = state.wrongTaps,
        solved = state.solved,
        practiceNotes = state.practiceNotes,
        onNoteTapped = onNoteTapped,
        modifier = Modifier.weight(1f),
    )
    Spacer(Modifier.height(12.dp))

    // The feedback line: "That's it!" bounces in, as "Perfect!" does on the tuner. While
    // waiting the line is empty but keeps its height, so nothing below jumps.
    val scale = remember { Animatable(1f) }
    LaunchedEffect(state.solved) {
        if (state.solved) {
            scale.snapTo(1.3f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    val feedback = when {
        state.solved -> stringResource(R.string.notes_correct) to right
        state.wrongTaps.isNotEmpty() -> stringResource(R.string.notes_wrong) to wrong
        else -> null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FeedbackLineHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (feedback != null) {
            Text(
                text = feedback.first,
                style = MaterialTheme.typography.titleLarge,
                color = feedback.second,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            )
        }
    }
}

/** The feedback line's reserved height: one line of titleLarge. */
private val FeedbackLineHeight = 30.dp
