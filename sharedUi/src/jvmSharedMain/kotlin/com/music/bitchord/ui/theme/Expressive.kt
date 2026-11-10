@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.music.bitchord.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ButtonGroupMenuState
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Material 3 Expressive components (branch material3-expressive, ALPHA por
 * orden de Alejandro 2026-11-09).
 *
 * This module compiles against JetBrains material3 1.11.0-alpha07, which
 * wraps androidx material3 1.5.0-alpha17 on Android (the newest JetBrains
 * line that still wraps androidx — 1.12+ ships its own copy of the package).
 * In alpha17 the full Expressive set is already public (verified against
 * the published 1.5.0-alpha17 sources jar and the JetBrains 1.11.0-alpha07
 * sources jar — same file, same line numbers): MaterialExpressiveTheme,
 * ButtonGroup, SplitButtonLayout, LoadingIndicator, MotionScheme.expressive()
 * and expressiveLightColorScheme(). They are @ExperimentalMaterial3ExpressiveApi
 * in alpha17 (in 1.5.0-rc01, which the phone app pins, the theme and the
 * groups are already stabilised), hence the file-level @OptIn above.
 *
 * So the four components that were hand-built on stable 1.4.0 are now the
 * STOCK ones, behind the same wrapper names — call sites do not move:
 * ExpressiveButton rides ButtonDefaults.shapesFor/contentPaddingFor (the
 * stock size-aware morphing shapes; XS 32dp is now a real stock size),
 * ExpressiveButtonGroup is the stock ButtonGroup, ExpressiveSplitButton is
 * the stock SplitButtonLayout, ExpressiveLoadingIndicator is the stock
 * LoadingIndicator (48dp container, 38dp morphing shape — the spec numbers
 * the hand-drawn version used). The rest was already stock-based and stays:
 * AssistChip, Slider (+ SliderDefaults.Thumb), Card, FilledIconButton,
 * Switch. ExpressiveMotion / expressivePress stay as app-level motion glue
 * (there is no stock equivalent for the press-scale modifier); the stock
 * motion scheme itself comes from the theme (MotionScheme.expressive()).
 */

/** Expressive motion: springs, not durations. Bouncy for presses, standard for layout. */
object ExpressiveMotion {
    /** A press: quick, underdamped, lands with a little overshoot. */
    // Generic on purpose (run 42671163263): the same press spring drives
    // Float animations (press scale) and Dp ones (slider thumb), and a
    // SpringSpec<Float> does not satisfy AnimationSpec<Dp>.
    fun <T> press() = spring<T>(dampingRatio = 0.65f, stiffness = 500f)

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

/**
 * The five Expressive button sizes, heights per the M3 Expressive spec —
 * and per the stock tokens (ButtonXSmallTokens 32 / Small 40 / Medium 56 /
 * Large 96 / XLarge 136dp). Padding and pressed shapes now come from the
 * stock [ButtonDefaults.contentPaddingFor] / [ButtonDefaults.shapesFor].
 */
enum class ExpressiveButtonSize(val height: Dp) {
    XS(32.dp),
    S(40.dp),
    M(56.dp),
    L(96.dp),
    XL(136.dp),
}

/**
 * An Expressive button: one of the five sizes, on the STOCK Button with the
 * stock size-aware shapes ([ButtonDefaults.shapesFor] morphs shape to
 * pressed shape on its own) and size-aware padding
 * ([ButtonDefaults.contentPaddingFor]). Filled by default; pass [colors]
 * for tonal / outlined / text variants. Keeps the app's spring press scale
 * on top of the stock shape morph.
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
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveButtonScale",
    )
    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapesFor(size.height),
        modifier = modifier
            .height(size.height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
        enabled = enabled,
        colors = colors,
        contentPadding = ButtonDefaults.contentPaddingFor(size.height),
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * An Expressive button group: the STOCK [ButtonGroup], thinly wrapped so
 * call sites keep speaking Expressive. Segments register with
 * [ButtonGroupScope.clickableItem] / [toggleableItem] / [customItem] and
 * grow on press via [ButtonGroupScope.animateWidth]; overflow goes through
 * [ButtonGroupDefaults.OverflowIndicator] unless the caller supplies its own.
 * Previously a hand-built Row of buttons (stable 1.4.0 had no public
 * ButtonGroup); the stock group now owns the connected shapes, the
 * press-grow and the overflow menu.
 */
@Composable
fun ExpressiveButtonGroup(
    modifier: Modifier = Modifier,
    overflowIndicator: @Composable (ButtonGroupMenuState) -> Unit =
        { ButtonGroupDefaults.OverflowIndicator(it) },
    content: ButtonGroupScope.() -> Unit,
) {
    ButtonGroup(
        overflowIndicator = overflowIndicator,
        modifier = modifier,
        content = content,
    )
}

/**
 * An Expressive split button: the STOCK [SplitButtonLayout] with
 * [SplitButtonDefaults.LeadingButton] / [TrailingButton], shapes and padding
 * sized from [size] ([SplitButtonDefaults.leadingButtonShapesFor] and
 * friends). The chevron spins 180° as the menu opens. [onMenuClick] fires
 * the trailing half; [expanded] drives the chevron.
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
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = onClick,
                enabled = enabled,
                shapes = SplitButtonDefaults.leadingButtonShapesFor(size.height),
                colors = colors,
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(size.height),
            ) { leadingContent() }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = onMenuClick,
                enabled = enabled,
                shapes = SplitButtonDefaults.trailingButtonShapesFor(size.height),
                colors = colors,
                contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(size.height),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                )
            }
        },
        modifier = modifier.height(size.height),
    )
}

/**
 * The Expressive loading indicator: the STOCK [LoadingIndicator]. It morphs
 * circle -> squircle -> blob on its own, in its 48dp container with a 38dp
 * active shape — the spec numbers the hand-drawn Canvas version used. The
 * stock indicator always lays out at 48dp (its ContainerWidth/Height
 * tokens), so the smaller call sites (24/28/32dp) are kept by scaling the
 * drawing inside a box of the requested size. [shapeSize] is kept for
 * call-site compatibility and matches the stock 38dp active shape.
 * Use wherever a wait has no measurable fraction; determinate progress keeps
 * its own linear indicator.
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    containerSize: Dp = 48.dp,
    shapeSize: Dp = 38.dp,
) {
    // Stock lays out 48dp no matter the incoming modifier; scale the drawing
    // for the smaller call sites instead of fighting its size. Vector path,
    // so the scale stays crisp.
    val scale = (containerSize / 48.dp).coerceIn(0.25f, 1.5f)
    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center,
    ) {
        LoadingIndicator(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            color = color,
        )
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
 * An Expressive slider: the M3 slider on the primary track.
 *
 * Why the plain value-based overload, not the SliderState one with a custom
 * thumb/track: this module compiles against androidx material3 1.5.0-alpha17
 * (via JetBrains material3 1.11.0-alpha07) but the phone ships 1.5.0-rc01,
 * and rc01 REMOVED the value-based Slider overload that takes thumb/track —
 * calling it linked a method the dex does not have (NoSuchMethodError when
 * opening Settings). The SliderState-based overload that replaces it in
 * rc01 does not exist in alpha17, so it cannot be called from code that
 * must also compile against alpha17 — and this file is shared with the
 * desktop target, which resolves the alpha17-level API too. The plain
 * value-based overload is byte-identical in alpha17 and rc01 (same
 * parameters, same order), so it compiles on both targets and links on the
 * phone. Cost: the thumb is the stock M3 size instead of the 28dp
 * Expressive one that grows on press, and the track draws its default stop
 * indicator — the Expressive colours (primary thumb/track) are kept.
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
) {
    val interactionSource = remember { MutableInteractionSource() }
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

/**
 * The stock pressed corner radius for a button of [size], mirroring
 * [ButtonDefaults.shapesFor]: rest is CornerFull (a circle, radius size/2),
 * pressed is CornerSmall 8dp up to 48dp, CornerMedium 12dp up to 76dp,
 * CornerLarge 16dp up to 116dp, CornerExtraLarge 28dp above that — the same
 * buckets and values the stock tokens use (ButtonXSmall/Small/Medium/Large/
 * XLargeTokens.PressedContainerShape, verified in 1.5.0-alpha17 and rc01).
 *
 * There is no public shape-animation API in either version to animate a
 * [androidx.compose.ui.graphics.Shape] between its rest and pressed forms:
 * `rememberAnimatedShape` / `shapeByInteraction` are internal to material3
 * and no `animateShapeAsState` exists. So the morph below animates the corner
 * radius in Dp instead — the same numbers the stock morph would land on.
 */
internal fun expressivePressedCorner(size: Dp): Dp = when {
    size <= 36.dp -> 8.dp
    size <= 48.dp -> 8.dp
    size <= 76.dp -> 12.dp
    size <= 116.dp -> 16.dp
    else -> 28.dp
}

/**
 * The Expressive press morph for a custom disc: its clip rounds from a full
 * circle at rest to the stock pressed corner ([expressivePressedCorner]) while
 * held, on the press spring. For discs that are drawn by hand (transport
 * glyphs over artwork, lyrics toggles) where the stock Button/IconButton
 * cannot do the morph — their containers would cover the artwork.
 */
@Composable
fun Modifier.expressiveMorphClip(
    interactionSource: MutableInteractionSource,
    size: Dp,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val radius by animateDpAsState(
        targetValue = if (pressed) expressivePressedCorner(size) else size / 2,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveMorphClip",
    )
    return this.clip(RoundedCornerShape(radius))
}

/**
 * An Expressive transport button: a bare glyph over artwork that answers a
 * press with a tonal disc morphing circle -> squircle behind it, plus the
 * 0.97 spring press scale. The play/pause hero (96dp), prev/next (53dp) and
 * any other glyph that sits on the sleeve without a container at rest.
 *
 * The glyph itself is never clipped — clipping the icon shaves the corners
 * off wide glyphs like fast-forward — only the disc morphs. With
 * [pressDiscColor] null there is no disc at all and the morph is the scale
 * alone, so pass a disc colour for the full effect.
 */
@Composable
fun ExpressiveTransportButton(
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
    touchSize: Dp = size,
    enabled: Boolean = true,
    /** Tonal disc shown only while pressed; null keeps the glyph bare. */
    pressDiscColor: Color? = null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val discAlpha by animateFloatAsState(
        targetValue = if (pressed && pressDiscColor != null) 1f else 0f,
        animationSpec = ExpressiveMotion.press(),
        label = "expressiveTransportDisc",
    )
    Box(
        modifier = modifier
            .size(touchSize)
            .expressivePress(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        if (pressDiscColor != null && discAlpha > 0f) {
            Box(
                Modifier
                    .size(size)
                    .expressiveMorphClip(interactionSource, size)
                    .background(pressDiscColor.copy(alpha = discAlpha)),
            )
        }
        Box(
            Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/**
 * An Expressive icon button: the stock [IconButton] with the theme's
 * morphing shapes ([IconButtonDefaults.shapes)]). The plain IconButton
 * overload without `shapes` is HIDDEN-deprecated in 1.5.0-rc01 and paints a
 * fixed standard shape with no morph — no new call sites should use it.
 * Note the stock 48dp minimum touch target: a smaller [modifier] still lays
 * out at 48dp.
 */
@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
        enabled = enabled,
        colors = colors,
        content = content,
    )
}

/**
 * The Expressive determinate wavy progress, in the theme primary. The stock
 * [LinearWavyProgressIndicator] lays out at a fixed 240dp width by design, so
 * it does not fit a full-width bar — for those, keep the thin bar tinted with
 * the expressive primary instead.
 */
@Composable
fun ExpressiveLinearWavyProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    LinearWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}

/** The circular sibling of [ExpressiveLinearWavyProgress]. */
@Composable
fun ExpressiveCircularWavyProgress(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    CircularWavyProgressIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}

/** Convenience: a label in the Expressive label style, for button content. */
@Composable
fun ExpressiveButtonLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge)
}
