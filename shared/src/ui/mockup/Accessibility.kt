package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A two-tone focus ring that stays visible on both the dark and light palettes
 * (and on the bright active-tab/primary-button fill).
 */
@Composable
fun Modifier.a11yFocusRing(enabled: Boolean = true): Modifier {
    var focused by remember { mutableStateOf(false) }
    val drawRing = focused && enabled
    return onFocusChanged { focused = it.isFocused }
        .then(
            if (drawRing) {
                Modifier.drawWithContent {
                    drawContent()
                    drawFocusRing()
                }
            } else {
                Modifier
            },
        )
}

internal fun ContentDrawScope.drawFocusRing() {
    drawRect(color = Color.Black, style = Stroke(width = 4.dp.toPx()))
    drawRect(color = Color.White, style = Stroke(width = 2.dp.toPx()))
}

/**
 * Default indication for every clickable in the app. Unlike a ripple, it only
 * paints keyboard focus, so a focused list row is never invisible.
 */
object A11yFocusIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        A11yFocusNode(interactionSource)

    override fun hashCode(): Int = 0x0A11F0C5

    override fun equals(other: Any?): Boolean = other === this
}

/**
 * Node-based indication: Compose 1.12 requires [IndicationNodeFactory] for
 * the default `clickable` overload. Drawing from an indication node also avoids
 * re-composing the button or row when only hover/press/focus changes.
 */
private class A11yFocusNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(), DrawModifierNode {
    private var focused by mutableStateOf(false)
    private var pressed by mutableStateOf(false)
    private var hovered by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus -> focused = true
                    is FocusInteraction.Unfocus -> focused = false
                    is PressInteraction.Press -> pressed = true
                    is PressInteraction.Release -> pressed = false
                    is PressInteraction.Cancel -> pressed = false
                    is HoverInteraction.Enter -> hovered = true
                    is HoverInteraction.Exit -> hovered = false
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) {
            drawRect(Color.White.copy(alpha = 0.16f))
        } else if (hovered) {
            drawRect(Color.White.copy(alpha = 0.05f))
        }
        if (focused) drawFocusRing()
    }
}

/** Adds a button role, an announced label and an optional selected state. */
fun Modifier.a11yButton(
    label: String? = null,
    selected: Boolean? = null,
    role: Role = Role.Button,
    enabled: Boolean = true,
): Modifier = semantics(mergeDescendants = true) {
    this.role = role
    label?.let { contentDescription = it }
    selected?.let { this.selected = it }
    if (!enabled) disabled()
}

fun Modifier.a11yState(label: String): Modifier = semantics { stateDescription = label }

fun Modifier.a11yLiveRegion(assertive: Boolean = false): Modifier = semantics {
    liveRegion = if (assertive) LiveRegionMode.Assertive else LiveRegionMode.Polite
}

fun Modifier.a11yHeading(): Modifier = semantics { heading() }

fun Modifier.a11yFocusGroup(): Modifier = focusGroup()
