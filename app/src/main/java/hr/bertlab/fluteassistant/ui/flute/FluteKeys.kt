package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.ui.common.chunky
import hr.bertlab.fluteassistant.ui.common.noteColor
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.common.noteText
import hr.bertlab.fluteassistant.ui.theme.FluteTheme
import hr.bertlab.fluteassistant.ui.theme.InkOnFill
import hr.bertlab.fluteassistant.ui.theme.Sun

/** A key is never shorter than this: a sure target for a small finger. */
private val KeyMinHeight = 58.dp
private val KeyMaxHeight = 72.dp
private val KeyRowGap = 12.dp
private val KeyGap = 6.dp
private val KeyShape = RoundedCornerShape(21.dp)
private val KeyDepth = 5.dp

/** The keys' height when the screen is too short to give them more: three rows at their minimum. */
val FluteKeysMinHeight: Dp = (KeyMinHeight + KeyDepth) * 3 + KeyRowGap * 2


/**
 * The fifteen keys ([StaffNotes.ALL]) in three rows, one per octave, highest on top as on
 * the staff (c3 alone, then c2 to h2, then c1 to h1), each key in its letter's colour with
 * its name; the key of [litMidi] sinks onto its shadow inside a yellow ring. A finger down
 * calls [onPress] at once and the finger lifting, or sliding off, calls [onRelease]; sliding
 * onto another key does not press it, so every note in the tune is a deliberate tap. A
 * screen reader activates a key through [onActivate]. The rows share the height they are
 * given, between 58 and 72 dp each, centred.
 */
@Composable
fun FluteKeys(
    litMidi: Int?,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(R.string.flute_keys)
    val rows = remember { StaffNotes.ALL.groupBy { Math.floorDiv(it, 12) }.toSortedMap().values.reversed() }
    val keysPerRow = remember(rows) { rows.maxOf { it.size } }
    Column(
        modifier = modifier.semantics { contentDescription = groupLabel },
        verticalArrangement = Arrangement.spacedBy(KeyRowGap, Alignment.CenterVertically),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(min = KeyMinHeight + KeyDepth, max = KeyMaxHeight + KeyDepth)
                    .padding(bottom = KeyDepth),
                horizontalArrangement = Arrangement.spacedBy(KeyGap),
            ) {
                row.forEach { midi ->
                    FluteKey(
                        midi = midi,
                        lit = midi == litMidi,
                        onPress = onPress,
                        onRelease = onRelease,
                        onActivate = onActivate,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                // A short row (c3 alone) keeps its keys as wide as the others.
                repeat(keysPerRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FluteKey(
    midi: Int,
    lit: Boolean,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    onActivate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = noteLabel(midi)
    val colors = FluteTheme.colors
    // The gesture block is keyed on the note alone, so it reads the latest callbacks through state.
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                onClick {
                    onActivate(midi)
                    true
                }
            }
            .drawBehind {
                // The yellow ring around the lit key, a little outside its outline.
                if (lit) {
                    val gap = 2.dp.toPx() + 2.5.dp.toPx()
                    drawRoundRect(
                        color = Sun,
                        topLeft = Offset(-gap, -gap + 3.dp.toPx()),
                        size = Size(size.width + gap * 2, size.height + gap * 2),
                        cornerRadius = CornerRadius(21.dp.toPx() + gap),
                        style = Stroke(5.dp.toPx()),
                    )
                }
            }
            .chunky(KeyShape, noteColor(midi), colors.outline, KeyDepth, pressed = lit)
            .pointerInput(midi) {
                detectTapGestures(
                    onPress = {
                        currentOnPress(midi)
                        tryAwaitRelease() // up, or the finger slid off: both let go of the key
                        currentOnRelease(midi)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(noteText(midi), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = InkOnFill, maxLines = 1)
    }
}
