package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.ui.common.noteLabel

/** A key is never shorter than this: a sure target for a small finger. */
private val KeyMinHeight = 64.dp
private val KeyRowGap = 6.dp
private val KeyGap = 4.dp

/** The keys' height when the screen is too short to give them more: three rows at their minimum. */
val FluteKeysMinHeight: Dp = KeyMinHeight * 3 + KeyRowGap * 2

/**
 * The fifteen keys ([StaffNotes.ALL]) in three rows, one per octave, each row in its own
 * colour ([octaveColors]) so a child can tell c1 from c2 at a glance; the key of [litMidi]
 * fills coral. A finger down calls [onPress] at once and the finger lifting, or sliding off,
 * calls [onRelease]; sliding onto another key does not press it, so every note in the tune
 * is a deliberate tap. A screen reader activates a key through [onActivate]. The rows share
 * the height they are given, never under 64 dp each.
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
    val rows = remember { StaffNotes.ALL.groupBy { Math.floorDiv(it, 12) }.toSortedMap().values.toList() }
    val keysPerRow = remember(rows) { rows.maxOf { it.size } }
    Column(
        modifier = modifier.semantics { contentDescription = groupLabel },
        verticalArrangement = Arrangement.spacedBy(KeyRowGap),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = KeyMinHeight),
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
    val (container, content) = if (lit) {
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    } else {
        octaveColors(midi)
    }
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
            .background(container, RoundedCornerShape(14.dp))
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
        Text(label, style = MaterialTheme.typography.titleMedium, color = content)
    }
}

/** Container and content colours of a note's octave row: peach for c1 to h1, teal for c2 to h2, yellow for c3. */
@Composable
internal fun octaveColors(midi: Int): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (Math.floorDiv(midi, 12) - Math.floorDiv(StaffNotes.ALL.first(), 12)) {
        0 -> scheme.surfaceVariant to scheme.onSurfaceVariant
        1 -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
}
