package org.baconana.mybeautifulgit_chan.ui.mockup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.pow

enum class ThemeMode(val label: String) {
    Dark("dark"),
    Light("light"),
    System("system"),
    ;

    companion object {
        fun parse(value: String?): ThemeMode =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) } ?: Dark
    }
}

enum class FontChoice(val label: String, val family: FontFamily) {
    Mono("mono", FontFamily.Monospace),
    Sans("sans", FontFamily.SansSerif),
    Serif("serif", FontFamily.Serif),
    ;

    companion object {
        fun parse(value: String?): FontChoice =
            entries.firstOrNull { it.label.equals(value?.trim(), ignoreCase = true) } ?: Mono
    }
}

private data class Palette(
    val bg: Color,
    val panel: Color,
    val panelAlt: Color,
    val raised: Color,
    val border: Color,
    val borderHi: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val green: Color,
    val greenDim: Color,
    val amber: Color,
    val red: Color,
    val cyan: Color,
    val magenta: Color,
    val white: Color,
    val black: Color,
    val focus: Color,
)

private val DarkPalette = Palette(
    bg = Color(0xFF050705),
    panel = Color(0xFF0A0E09),
    panelAlt = Color(0xFF0D120C),
    raised = Color(0xFF12190F),
    border = Color(0xFF263323),
    borderHi = Color(0xFF55704E),
    text = Color(0xFFC7D9C1),
    dim = Color(0xFF7E9678),
    faint = Color(0xFF758B6F),
    green = Color(0xFF8CFF9B),
    greenDim = Color(0xFF57A868),
    amber = Color(0xFFFFC857),
    red = Color(0xFFFF6B5E),
    cyan = Color(0xFF6CD3FF),
    magenta = Color(0xFFFF7AD9),
    white = Color(0xFFEAF6E4),
    black = Color(0xFF020402),
    focus = Color(0xFFEAF6E4),
)

private val LightPalette = Palette(
    bg = Color(0xFFF3F0E6),
    panel = Color(0xFFEAE6D9),
    panelAlt = Color(0xFFE2DDCE),
    raised = Color(0xFFD8D2C1),
    border = Color(0xFFB9B29C),
    borderHi = Color(0xFF6F654F),
    text = Color(0xFF20261D),
    dim = Color(0xFF4C5746),
    faint = Color(0xFF525C47),
    green = Color(0xFF1E6B35),
    greenDim = Color(0xFF3B864A),
    amber = Color(0xFF8A5D00),
    red = Color(0xFFA93B32),
    cyan = Color(0xFF1B657F),
    magenta = Color(0xFF8E3A6D),
    white = Color(0xFF10140D),
    black = Color(0xFFFBF8EE),
    focus = Color(0xFF10140D),
)

/**
 * A denser palette for low-vision users. Muted text and control borders are
 * pushed above the WCAG AA contrast thresholds without changing layout.
 */
private val DarkHighContrastPalette = DarkPalette.copy(
    bg = Color(0xFF000000),
    panel = Color(0xFF070A06),
    panelAlt = Color(0xFF0C110A),
    raised = Color(0xFF151D12),
    border = Color(0xFF5D6B55),
    borderHi = Color(0xFF8BA582),
    text = Color(0xFFF0FBEB),
    dim = Color(0xFFB8CDB2),
    faint = Color(0xFFA6BDA0),
    green = Color(0xFFA4FFB0),
    amber = Color(0xFFFFD479),
    red = Color(0xFFFF8A7E),
    cyan = Color(0xFF8BE0FF),
    focus = Color(0xFFFFFFFF),
)

private val LightHighContrastPalette = LightPalette.copy(
    bg = Color(0xFFFFFDF6),
    panel = Color(0xFFF6F2E7),
    panelAlt = Color(0xFFEDE8DA),
    raised = Color(0xFFE3DDCD),
    border = Color(0xFF6F654F),
    borderHi = Color(0xFF4F4738),
    text = Color(0xFF000000),
    dim = Color(0xFF2F382B),
    faint = Color(0xFF3F4737),
    green = Color(0xFF0E5C26),
    greenDim = Color(0xFF256D34),
    amber = Color(0xFF6B4700),
    red = Color(0xFF8E241C),
    cyan = Color(0xFF0D4E66),
    magenta = Color(0xFF6F2452),
    focus = Color(0xFF000000),
)

/**
 * Терминальная палитра. В отличие от старой константной темы, цвета читаются
 * из Compose-состояния, поэтому смена темы и шрифта перерисовывает весь UI.
 *
 * black/white здесь — роли: black используется как фон полей и как текст на
 * акцентной заливке, white — как акцентный текст. В светлой теме они меняются
 * местами, чтобы контраст сохранялся.
 */
object Term {
    private var palette by mutableStateOf(DarkPalette)
    private var monoFamily: FontFamily by mutableStateOf(FontFamily.Monospace)

    val bg get() = palette.bg
    val panel get() = palette.panel
    val panelAlt get() = palette.panelAlt
    val raised get() = palette.raised
    val border get() = palette.border
    val borderHi get() = palette.borderHi

    val text get() = palette.text
    val dim get() = palette.dim
    val faint get() = palette.faint

    val green get() = palette.green
    val greenDim get() = palette.greenDim
    val amber get() = palette.amber
    val red get() = palette.red
    val cyan get() = palette.cyan
    val magenta get() = palette.magenta
    val white get() = palette.white
    val black get() = palette.black
    val focus get() = palette.focus

    val mono get() = monoFamily

    fun apply(
        theme: ThemeMode,
        font: FontChoice,
        systemDark: Boolean = true,
        highContrast: Boolean = false,
    ) {
        val dark = theme == ThemeMode.Dark || (theme == ThemeMode.System && systemDark)
        palette = when {
            highContrast && dark -> DarkHighContrastPalette
            highContrast -> LightHighContrastPalette
            dark -> DarkPalette
            else -> LightPalette
        }
        monoFamily = font.family
    }
}

/** WCAG 2.2 relative contrast, exposed for palette regression tests. */
internal fun contrastRatio(foreground: Color, background: Color): Double {
    fun channel(value: Float): Double {
        val c = value.toDouble()
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    val lighter = maxOf(luminance(foreground), luminance(background))
    val darker = minOf(luminance(foreground), luminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}
