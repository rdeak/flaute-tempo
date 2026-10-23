package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.InTuneGreenText
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRed
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRedText

/** Where the note column sits, as a fraction of the width; the clef takes the left. */
private const val NOTE_COLUMN = 0.6f

/**
 * Room kept above the highest place and below the lowest, in places, so the clef's loop and
 * tail (a space beyond the staff each way) are never cut; the end places' tap strips extend
 * over it.
 */
private const val END_MARGIN_PLACES = 1

/**
 * A place's circle, as a fraction of the pitch: the placeholder and every mark drawn on it
 * (red for a wrong tap, green for the found note, a green ring as the hint). At 0.4 the
 * circle fills 80 % of the place's tap strip and never reaches the strip next to it, so a
 * wrong tap can never hide the place a child needs to aim at next. A real note head is as
 * tall as a whole space, 1.7 pitches, and was found to bury its neighbours.
 */
private const val MARK_RADIUS = 0.4f

/** A ledger line's half length, in pitches: a little past the circle each side. */
private const val LEDGER_HALF = 0.9f

/**
 * A treble staff that fills whatever height it is given: the places shown are
 * [StaffNotes.visiblePositions] of [practiceNotes] (the staff and its two neighbouring spaces,
 * plus the ledger-line notes only as far as a chosen note needs them), and the size of a place
 * follows from the height, so the fewer places the bigger they are. Each place has a faint
 * circle in one column ([MARK_RADIUS] of the pitch, inside its own strip), with the ledger
 * lines it needs. The whole height is tappable, one full-width strip per place, and a tap
 * reports that place's note through [onNoteTapped]. Every mark is that same circle: the
 * places in [wrongTaps] are filled red; the place of [currentMidi] is ringed green once a
 * wrong tap has been made and filled green once [solved]. So tap a circle and it turns red
 * or green, and the circles beside it stay clear. The strips are named by position for
 * accessibility, never by note, so a screen reader does not give the answer away.
 */
@Composable
fun NoteStaff(
    currentMidi: Int,
    wrongTaps: Set<Int>,
    solved: Boolean,
    practiceNotes: Set<Int>,
    onNoteTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val right = if (dark) InTuneGreen else InTuneGreenText
    val wrong = if (dark) OutOfTuneRed else OutOfTuneRedText
    val staffDescription = stringResource(R.string.staff_description)
    val visible = StaffNotes.visiblePositions(practiceNotes)
    val currentPosition = StaffNotes.positionOf(currentMidi)
    val wrongPositions = wrongTaps.map(StaffNotes::positionOf).filter { it in visible }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = staffDescription },
    ) {
        val placeHeight = maxHeight / (visible.count() + 2 * END_MARGIN_PLACES)

        Canvas(Modifier.fillMaxSize()) {
            val pitch = size.height / (visible.count() + 2 * END_MARGIN_PLACES)
            val space = pitch * 2f
            val lineWidth = 2.dp.toPx()
            fun yOf(position: Int) = (visible.last - position + END_MARGIN_PLACES + 0.5f) * pitch
            val x = size.width * NOTE_COLUMN

            for (position in StaffNotes.LINE_POSITIONS) {
                drawLine(ink, Offset(0f, yOf(position)), Offset(size.width, yOf(position)), lineWidth)
            }
            drawTrebleClef(space, Offset(space * 0.35f, yOf(StaffNotes.LINE_POSITIONS.last())), ink)

            val markRadius = pitch * MARK_RADIUS
            val ledgerHalf = pitch * LEDGER_HALF
            fun ledger(position: Int, color: Color) =
                drawLine(color, Offset(x - ledgerHalf, yOf(position)), Offset(x + ledgerHalf, yOf(position)), lineWidth)
            /** The place's circle, filled, or as a ring whose outer edge still sits at [markRadius]. */
            fun mark(position: Int, color: Color, stroke: Float? = null) = drawCircle(
                color,
                radius = if (stroke == null) markRadius else markRadius - stroke / 2,
                center = Offset(x, yOf(position)),
                style = if (stroke == null) Fill else Stroke(stroke),
            )
            fun ledgersFor(position: Int, color: Color) =
                StaffNotes.ledgerLinesFor(position).forEach { ledger(it, color) }

            // Every visible place, empty: the faint ledger lines the far places need and the
            // faint circle itself, with clear air to the circles above and below it.
            for (position in visible) {
                if (position % 2 == 0 && position !in StaffNotes.LINE_POSITIONS) ledger(position, faint)
                mark(position, faint, stroke = 1.5.dp.toPx())
            }
            for (position in wrongPositions) {
                ledgersFor(position, wrong)
                mark(position, wrong)
            }
            if (currentPosition in visible && solved) {
                ledgersFor(currentPosition, right)
                mark(currentPosition, right)
            } else if (currentPosition in visible && wrongPositions.isNotEmpty()) {
                ledgersFor(currentPosition, right)
                mark(currentPosition, right, stroke = 3.dp.toPx())
            }
        }
        // One full-width tap strip per place, the highest at the top; the end strips also take
        // the margin beyond them, so every point of the staff belongs to a place.
        Column(Modifier.fillMaxSize()) {
            for (position in visible.last downTo visible.first) {
                val description = stringResource(R.string.staff_position, position + 1)
                val margin = if (position == visible.last || position == visible.first) END_MARGIN_PLACES else 0
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(placeHeight * (1 + margin))
                        .clickable(interactionSource = null, indication = null, role = Role.Button) {
                            onNoteTapped(StaffNotes.midiAt(position))
                        }
                        .semantics { contentDescription = description },
                )
            }
        }
    }
}

/**
 * A stroked treble clef, drawn in staff spaces from [origin], the left end of the top line:
 * the hook and dot below the staff, the stem up to the loop above it, the sweep back down
 * across the staff and the curl around the G line (three spaces below the top).
 */
private fun DrawScope.drawTrebleClef(space: Float, origin: Offset, color: Color) {
    fun x(u: Float) = origin.x + u * space
    fun y(u: Float) = origin.y + u * space
    val path = Path().apply {
        moveTo(x(0.6f), y(4.7f))
        cubicTo(x(0.65f), y(5.1f), x(1.1f), y(5.12f), x(1.05f), y(4.65f))
        cubicTo(x(1.0f), y(3.8f), x(1.1f), y(2.0f), x(1.3f), y(0.5f))
        cubicTo(x(1.35f), y(-0.2f), x(1.5f), y(-0.8f), x(1.55f), y(-1.0f))
        cubicTo(x(1.7f), y(-0.9f), x(1.85f), y(-0.3f), x(1.75f), y(0.3f))
        cubicTo(x(1.65f), y(1.2f), x(0.9f), y(1.9f), x(0.5f), y(2.4f))
        cubicTo(x(0.1f), y(2.9f), x(0.1f), y(3.5f), x(0.5f), y(3.9f))
        cubicTo(x(0.9f), y(4.25f), x(1.55f), y(4.0f), x(1.6f), y(3.3f))
        cubicTo(x(1.65f), y(2.7f), x(1.1f), y(2.4f), x(0.8f), y(2.75f))
        cubicTo(x(0.55f), y(3.0f), x(0.7f), y(3.4f), x(1.0f), y(3.3f))
    }
    drawPath(path, color, style = Stroke(width = space * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(color, radius = space * 0.17f, center = Offset(x(0.6f), y(4.7f)))
}
