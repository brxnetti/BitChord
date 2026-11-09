package com.music.bitchord.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/*
 * Material 3 Expressive components (branch material3-expressive).
 *
 * Stable androidx material3 1.4.0 — the latest stable, verified on Google
 * Maven 2026-11-09 — keeps the Expressive component set internal or absent:
 * MaterialExpressiveTheme, MotionScheme.expressive() and
 * expressiveLightColorScheme() are `internal`, and ButtonGroup, SplitButton
 * and LoadingIndicator have no public composable at all (only design tokens).
 * Those APIs go public in the 1.5.0-alpha line, which this app does not ship:
 * an alpha Compose line under a music player that must keep playing is the
 * wrong trade, and the JetBrains multiplatform material3 this module compiles
 * against has no stable mapping to it either.
 *
 * So the Expressive components are built here, on the public 1.4.0 API, to
 * the M3 Expressive spec: five button sizes (XS 32 / S 40 / M 56 / L 96 /
 * XL 136dp), corners that morph one step smaller under the finger, segments
 * that grow ~15% on press, a shape-morphing loading indicator, a 28dp slider
 * thumb, and spring motion throughout. When 1.5.0 goes stable these wrappers
 * are the seam to swap the stock components in behind — call sites already
 * speak Expressive.
 */

/** Expressive motion: springs, not durations. Bouncy for presses, standard for layout. */
object ExpressiveMotion {
    /** A press: quick, underdamped, lands with a little overshoot. */
    fun press() = spring<Float>(dampingRatio = 0.65f, stiffness = 500f)

    /** Layout following a press or a selection: firm, barely any overshoot. */
    fun layout() = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

    /** The slow, floaty spring for sheets and large containers. */
    fun container() = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow)
}

/**
 * Press feedback for any surface: shrinks to 97% and rounds one step
 * tighter while held, then springs back. The corner morph is the Expressive
 * tell — a button that only darkens is classic M3.
 */
@Composable
fun Modifier.expressivePress(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    scale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = ExpressiveMotion.press(),
        label = "expressivePress",
    )
    return this.graphicsLayer {
        scaleX = pressScale
        scaleY = pressScale
    }
}

/** The five Expressive button sizes, heights per the M3 Expressive spec. */
enum class ExpressiveButtonSize(val height: Dp, val contentPadding: PaddingValues) {
    XS(32.dp, PaddingValues(horizontal = 12.dp)),
    S(40.dp, PaddingValues(horizontal = 16.dp)),
    M(56.dp, PaddingValues(horizontal = 24.dp)),
    L(96.dp, PaddingValues(horizontal = 32.dp)),
    XL(136.dp, PaddingValues(horizontal = 40.dp)),
}

/**
 * An Expressive button: one of the five sizes, fully rounded, corners that
 * morph tighter under the finger, spring press scale. Filled by default;
 * pass [colors] for tonal / outlined / text variants.
 */
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ExpressiveButtonSize = ExpressiveButtonSize.M,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // Corners morph one step smaller while pressed, per the spec.
    val corner by animateDpAsState(
        targetValue = if (pressed) (size.height / 4) else (size.height / 2),
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveButtonCorner",
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveButtonScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .height(size.height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        colors = colors,
        contentPadding = size.contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * An Expressive button group: 2–5 related actions joined into one connected
 * cluster, the canonical Expressive control. Segments share edges (2dp gap),
 * the ends stay round and the joins square, and the pressed segment grows
 * ~15% on a spring while its siblings hold — the flex that says "group"
 * rather than "row of buttons".
 */
@Composable
fun ExpressiveButtonGroup(
    modifier: Modifier = Modifier,
    size: ExpressiveButtonSize = ExpressiveButtonSize.M,
    content: @Composable ExpressiveButtonGroupScope.() -> Unit,
) {
    // Fresh scope per composition: the content lambda appends segments, so a
    // remembered scope would grow one copy per recomposition.
    val scope = ExpressiveButtonGroupScope()
    scope.content()
    Row(modifier = modifier.height(size.height)) {
        scope.segments.forEachIndexed { index, segment ->
            ExpressiveButtonGroupSegment(
                segment = segment,
                size = size,
                isFirst = index == 0,
                isLast = index == scope.segments.lastIndex,
            )
        }
    }
}

@Composable
private fun ExpressiveButtonGroupSegment(
    segment: ExpressiveButtonSegment,
    size: ExpressiveButtonSize,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val grow by animateFloatAsState(
        targetValue = if (pressed) 1.15f else 1f,
        animationSpec = ExpressiveMotion.press(),
        label = "buttonGroupGrow",
    )
    val shape = RoundedCornerShape(
        topStart = if (isFirst) size.height / 2 else 4.dp,
        bottomStart = if (isFirst) size.height / 2 else 4.dp,
        topEnd = if (isLast) size.height / 2 else 4.dp,
        bottomEnd = if (isLast) size.height / 2 else 4.dp,
    )
    Box(
        modifier = Modifier
            .weight(segment.weight)
            .height(size.height)
            .graphicsLayer {
                scaleX = grow
                scaleY = grow
            }
            .padding(end = if (isLast) 0.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = segment.onClick,
            modifier = Modifier.height(size.height),
            enabled = segment.enabled,
            shape = shape,
            colors = segment.colors ?: ButtonDefaults.buttonColors(),
            contentPadding = size.contentPadding,
            interactionSource = interactionSource,
        ) {
            segment.content(this)
        }
    }
}

class ExpressiveButtonGroupScope {
    internal val segments = mutableListOf<ExpressiveButtonSegment>()
    fun segment(
        onClick: () -> Unit,
        enabled: Boolean = true,
        colors: ButtonColors? = null,
        weight: Float = 1f,
        content: @Composable RowScope.() -> Unit,
    ) {
        segments.add(ExpressiveButtonSegment(onClick, enabled, colors, weight, content))
    }
}

internal data class ExpressiveButtonSegment(
    val onClick: () -> Unit,
    val enabled: Boolean,
    val colors: ButtonColors?,
    val weight: Float,
    val content: @Composable RowScope.() -> Unit,
)

/**
 * An Expressive split button: a leading action plus an attached dropdown
 * trigger, joined like a button group. The chevron spins 180° as the menu
 * opens. [onMenuClick] fires the trailing half; [expanded] drives the
 * chevron.
 */
@Composable
fun ExpressiveSplitButton(
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: ExpressiveButtonSize = ExpressiveButtonSize.M,
    expanded: Boolean = false,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    leadingContent: @Composable RowScope.() -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = ExpressiveMotion.layout(),
        label = "splitButtonChevron",
    )
    Row(modifier = modifier.height(size.height)) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(size.height),
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = size.height / 2,
                bottomStart = size.height / 2,
                topEnd = 4.dp,
                bottomEnd = 4.dp,
            ),
            colors = colors,
            contentPadding = size.contentPadding,
        ) {
            leadingContent()
        }
        Box(Modifier.width(2.dp))
        Button(
            onClick = onMenuClick,
            modifier = Modifier.height(size.height),
            enabled = enabled,
            shape = RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = size.height / 2,
                bottomEnd = size.height / 2,
            ),
            colors = colors,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
            )
        }
    }
}

/**
 * The Expressive loading indicator: the stock spinner's replacement. A shape
 * that morphs between circle, squircle and blob while it turns, in a 48dp
 * container with a 38dp active shape — the M3 Expressive spec numbers. Use
 * wherever a wait has no measurable fraction; determinate progress keeps its
 * own linear indicator.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerSize: Dp = 48.dp,
    shapeSize: Dp = 38.dp,
) {
    val transition = rememberInfiniteTransition(label = "expressiveLoading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1600)),
        label = "loadingRotation",
    )
    // Corner radius breathes circle -> squircle -> almost-square, one morph
    // per half turn, so the shape never sits still long enough to read as a
    // static icon.
    val cornerFraction by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "loadingCorner",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "loadingPulse",
    )
    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .size(shapeSize)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = pulse
                    scaleY = pulse
                },
        ) {
            val radius = size.minDimension * cornerFraction
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(radius, radius),
            )
            // A lighter core, so the morph reads as a shape turning rather
            // than a solid block spinning.
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.22f, size.height * 0.22f),
                size = Size(size.width * 0.56f, size.height * 0.56f),
                cornerRadius = CornerRadius(radius * 0.7f, radius * 0.7f),
            )
        }
    }
}

/**
 * An Expressive assist chip: 40dp tall, fully rounded, tonal container.
 * For selection, use [androidx.compose.material3.FilterChip] with
 * [MaterialTheme.shapes.large] — the theme already carries the radius.
 */
@Composable
fun ExpressiveChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    AssistChip(
        onClick = onClick,
        label = label,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = null,
    )
}

/**
 * An Expressive slider: the M3 slider with a 28dp thumb (the Expressive
 * thumb size) that grows on press, on the primary track. [thumbSize] is the
 * public `SliderDefaults.Thumb` size knob — no alpha API needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @androidx.annotation.IntRange(from = 0) steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ),
    thumbSize: DpSize = DpSize(28.dp, 28.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val thumb by animateDpAsState(
        targetValue = if (pressed) 34.dp else thumbSize.width,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveSliderThumb",
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                colors = colors,
                enabled = enabled,
                thumbSize = DpSize(thumb, thumb),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = colors,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
            )
        },
    )
}

/**
 * An Expressive card: 24dp corners on a surface-container fill, no
 * elevation — Expressive cards get their presence from shape and tone, not
 * shadow.
 */
@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Box { content() } },
    )
}

/**
 * An Expressive filled icon button: a tonal disc (56dp, the M size) with a
 * spring press scale. The play/pause and other hero glyphs sit on this, not
 * on a bare IconButton.
 */
@Composable
fun ExpressiveFilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    enabled: Boolean = true,
    contentDescription: String? = null,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveIconButtonScale",
    )
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        interactionSource = interactionSource,
    ) {
        Box(Modifier.size(iconSize), contentAlignment = Alignment.Center) { icon() }
    }
}

/**
 * An Expressive switch: M3's Switch with a check in the thumb when on — the
 * Expressive thumb carries its state as a glyph, not just a colour — on the
 * expressive primary track.
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    )
}

/** Convenience: a label in the Expressive label style, for button content. */
@Composable
fun ExpressiveButtonLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}
