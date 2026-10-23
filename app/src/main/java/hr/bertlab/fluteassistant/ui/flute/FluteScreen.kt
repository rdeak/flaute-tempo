package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleStartEffect
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.FluteFingerings
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.tuner.FingeringDiagram
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Stateful entry point: opens the flute's sound while the screen is started (visible) and
 * closes it when it stops, the way the tuner holds the microphone, then renders the stateless
 * [FluteContent].
 */
@Composable
fun FluteScreen(
    state: FluteUiState,
    viewModel: FluteViewModel,
    modifier: Modifier = Modifier,
) {
    LifecycleStartEffect(Unit) {
        viewModel.enterTab()
        onStopOrDispose { viewModel.leaveTab() }
    }
    FluteContent(
        state = state,
        onKeyPressed = viewModel::onKeyPressed,
        onKeyReleased = viewModel::onKeyReleased,
        onKeyActivated = viewModel::onKeyActivated,
        onTogglePlayback = viewModel::togglePlayback,
        onEraseLast = viewModel::eraseLast,
        modifier = modifier,
    )
}

/** The name row's height: the 64 sp name's line. */
private val NameRowHeight = 72.dp
private val FluteHeight = 56.dp
private val PlayRowHeight = 64.dp

/**
 * Everything below this height cannot hold the keys at their minimum: the name row, the
 * flute, the tune line, three key rows and the Play row, plus the spacers between them.
 */
private val FitsHeight = NameRowHeight + 8.dp + FluteHeight + 12.dp + TuneLineHeight + 12.dp +
    FluteKeysMinHeight + 12.dp + PlayRowHeight + 12.dp

/**
 * The whole flute tab, top to bottom: the big name of the note shown (held, last played, or
 * playing now; a faint "…" before the first tap), the tuner's flute drawing with that note's
 * fingering, the scrolling [TuneLine], the octave rows of [FluteKeys] taking all the height
 * left, and the Play row: one big coral pill ("Play my tune", "Stop" while the tune plays) and
 * a round erase button, both faded while the tune is empty. On an ordinary phone held upright
 * nothing scrolls; when the height is short of [FitsHeight] the keys keep their minimum and
 * the whole screen scrolls, like the metronome and tuner tabs.
 */
@Composable
fun FluteContent(
    state: FluteUiState,
    onKeyPressed: (Int) -> Unit,
    onKeyReleased: (Int) -> Unit,
    onKeyActivated: (Int) -> Unit,
    onTogglePlayback: () -> Unit,
    onEraseLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fits = maxHeight >= FitsHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (fits) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val shown = state.shownMidi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NameRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shown?.let { noteLabel(it) } ?: "…",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 72.sp),
                    color = if (shown == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            FingeringDiagram(
                keys = shown?.let(FluteFingerings::keysFor),
                noteLabel = shown?.let { noteLabel(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FluteHeight),
            )
            Spacer(Modifier.height(12.dp))
            TuneLine(tune = state.tune, litIndex = state.litIndex)
            Spacer(Modifier.height(12.dp))
            FluteKeys(
                litMidi = state.litMidi,
                onPress = onKeyPressed,
                onRelease = onKeyReleased,
                onActivate = onKeyActivated,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fits) Modifier.weight(1f) else Modifier.height(FluteKeysMinHeight)),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayRowHeight),
            ) {
                Button(
                    onClick = onTogglePlayback,
                    enabled = !state.isEmpty,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) AppIcons.Stop else AppIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    // The Croatian label is long: shrink it to fit a narrow phone rather than cut it short.
                    Text(
                        text = stringResource(if (state.isPlaying) R.string.stop else R.string.flute_play),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = PlayLabelMinSize,
                            maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
                            stepSize = 1.sp,
                        ),
                    )
                }
                Spacer(Modifier.width(10.dp))
                EraseButton(enabled = !state.isEmpty, onErase = onEraseLast, modifier = Modifier.size(PlayRowHeight))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The smallest the Play label shrinks to on a narrow phone. */
private val PlayLabelMinSize = 14.sp

/** After this long held, the erase button starts repeating, then every [EraseRepeatMs]: a phone keyboard's backspace. */
private const val EraseRepeatDelayMs = 400L
private const val EraseRepeatMs = 120L

/**
 * One round soft-teal button with a backspace icon that erases the last note on a tap and
 * keeps erasing while held, so a long line is cleared by holding; it fills teal while a finger
 * is on it, like the keys fill coral. Faded and inert while [enabled] is false.
 */
@Composable
private fun EraseButton(
    enabled: Boolean,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.flute_erase)
    val scope = rememberCoroutineScope()
    val currentOnErase by rememberUpdatedState(onErase)
    var pressed by remember { mutableStateOf(false) }
    Surface(
        shape = CircleShape,
        color = if (pressed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (pressed) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (!enabled) disabled()
                onClick {
                    if (enabled) currentOnErase()
                    enabled
                }
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                currentOnErase()
                                val repeat = scope.launch {
                                    delay(EraseRepeatDelayMs)
                                    while (isActive) {
                                        currentOnErase()
                                        delay(EraseRepeatMs)
                                    }
                                }
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    pressed = false
                                    repeat.cancel()
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(AppIcons.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
        }
    }
}
