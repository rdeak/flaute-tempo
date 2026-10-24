package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.flute.NoteLength
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.ui.common.OutlineWidth
import hr.bertlab.fluteassistant.ui.common.dashedBorder
import hr.bertlab.fluteassistant.ui.common.noteColor
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.common.noteLabelParts
import hr.bertlab.fluteassistant.ui.common.noteText
import hr.bertlab.fluteassistant.ui.theme.FluteTheme
import hr.bertlab.fluteassistant.ui.theme.InkOnFill
import hr.bertlab.fluteassistant.ui.theme.Sun

/** The line's height: a row of note pills, each with its length's symbol under it. */
val TuneLineHeight = 62.dp

private val PillHeight = 36.dp
private val PillGap = 4.dp

/** A pill's width per beat; a longer note also covers the gaps between its beats. */
private val BeatWidth = 36.dp
private val EighthWidth = 20.dp

/** How wide a note of [length] is drawn: longer pill = longer note. */
private fun pillWidth(length: NoteLength): Dp =
    if (length == NoteLength.EIGHTH) EighthWidth else BeatWidth * length.wholeBeats + PillGap * (length.wholeBeats - 1)

/**
 * The child's tune as a line of pills, one per note in its letter's colour and as wide as the
 * note is long ([pillWidth]), with the length's note symbol faint under each (a hint, not a
 * control); the one at [litIndex] (sounding now, or held and still growing) is ringed in
 * yellow. The line scrolls sideways: it follows the newest note and the playing one, and the
 * child can drag it back to look. While [tune] is empty it is a dashed pill with one hint
 * instead, at the same height, so nothing below moves when the first note comes.
 */
@Composable
fun TuneLine(
    tune: List<TuneNote>,
    litIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = FluteTheme.colors
    if (tune.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(TuneLineHeight), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .dashedBorder(OutlineWidth / 2, colors.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.flute_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.mutedText,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 22.sp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
        return
    }
    val description = pluralStringResource(R.plurals.flute_tune_line, tune.size, tune.size)
    val listState = rememberLazyListState()
    LaunchedEffect(tune.size, litIndex) {
        listState.animateScrollToItem(litIndex ?: tune.lastIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(TuneLineHeight)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(PillGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        itemsIndexed(tune) { index, note -> TuneNotePill(note = note, lit = index == litIndex) }
    }
}

/** One note: its pill, and the length's symbol under it. */
@Composable
private fun TuneNotePill(note: TuneNote, lit: Boolean) {
    val colors = FluteTheme.colors
    val name = noteLabel(note.midi)
    val description = when (note.length) {
        NoteLength.EIGHTH -> stringResource(R.string.flute_note_half_beat, name)
        else -> pluralStringResource(R.plurals.flute_note_beats, note.length.wholeBeats, name, note.length.wholeBeats)
    }
    val shape = RoundedCornerShape(PillHeight / 2)
    val longNote = note.length.halfBeats > NoteLength.QUARTER.halfBeats
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Box(
            contentAlignment = if (longNote) Alignment.CenterStart else Alignment.Center,
            modifier = Modifier
                .size(pillWidth(note.length), PillHeight)
                .drawBehind { if (lit) drawRing() }
                .clip(shape)
                .background(noteColor(note.midi))
                .border(OutlineWidth, colors.outline, shape)
                .padding(horizontal = if (longNote) 9.dp else 0.dp),
        ) {
            val style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp)
            if (note.length == NoteLength.EIGHTH) {
                // Too narrow for the octave figure: the letter alone.
                Text(noteLabelParts(LocalResources.current, note.midi).first, style = style, color = InkOnFill, maxLines = 1)
            } else {
                Text(noteText(note.midi), style = style, color = InkOnFill, maxLines = 1)
            }
        }
        LengthSymbol(note.length, colors.mutedText)
    }
}

/** The yellow ring round the lit pill, a little outside it. */
private fun DrawScope.drawRing() {
    val stroke = 4.dp.toPx()
    val outset = 2.dp.toPx() + stroke / 2
    drawRoundRect(
        color = Sun,
        topLeft = Offset(-outset, -outset),
        size = Size(size.width + outset * 2, size.height + outset * 2),
        cornerRadius = CornerRadius(size.height / 2 + outset),
        style = Stroke(stroke),
    )
}

/**
 * The note symbol for [length], small and faint, drawn in an 18 x 20 box like the canvas's:
 * a filled head with a flag (eighth), a filled head (quarter), an open head (half), an open
 * head with a dot (dotted half), an open head without a stem (whole).
 */
@Composable
private fun LengthSymbol(length: NoteLength, color: Color) {
    Canvas(Modifier.size(16.dp, 18.dp)) {
        scale(size.width / 18f, size.height / 20f, pivot = Offset.Zero) {
            val line = 2f
            if (length == NoteLength.WHOLE) {
                rotate(-20f, pivot = Offset(9f, 12f)) {
                    drawOval(color, topLeft = Offset(3.4f, 8.2f), size = Size(11.2f, 7.6f), style = Stroke(2.2f))
                }
                return@scale
            }
            val filled = length == NoteLength.EIGHTH || length == NoteLength.QUARTER
            rotate(-20f, pivot = Offset(8.5f, 15f)) {
                val head = Offset(3.7f, 11.4f)
                val headSize = Size(9.6f, 7.2f)
                if (filled) drawOval(color, topLeft = head, size = headSize)
                drawOval(color, topLeft = head, size = headSize, style = Stroke(line))
            }
            drawLine(color, Offset(12.5f, 13.5f), Offset(12.5f, 3f), strokeWidth = line, cap = StrokeCap.Round)
            when (length) {
                NoteLength.EIGHTH -> drawPath(
                    Path().apply {
                        moveTo(12.5f, 3f)
                        cubicTo(13f, 6f, 17.5f, 6.5f, 16f, 11f)
                    },
                    color,
                    style = Stroke(line, cap = StrokeCap.Round),
                )
                NoteLength.DOTTED_HALF -> drawCircle(color, radius = 1.7f, center = Offset(16.4f, 16f))
                else -> Unit
            }
        }
    }
}
