package org.baconana.mybeautifulgit_chan.app

/** Последняя строка прогресса, которую Git напечатал в stderr. */
data class OperationProgress(
    val stage: String? = null,
    val percent: Int? = null,
    val detail: String? = null,
)

private val gitProgressPattern = Regex(
    """(?im)^\s*(?:remote:\s*)?([A-Za-z][A-Za-z0-9 _-]{2,40}):\s*(\d{1,3})%(?:\s*\(([^)]*)\))?""",
)

/**
 * Git пишет progress через `\r`, иногда с префиксом `remote:`.
 * Берём последнюю законченную строку из накопленного хвоста stderr.
 */
internal fun parseOperationProgress(text: String): OperationProgress? {
    if (text.isBlank()) return null
    val normalized = text.replace('\r', '\n')
    val match = gitProgressPattern.findAll(normalized).lastOrNull() ?: return null
    val stage = match.groupValues[1].trim().ifBlank { null }
    val percent = match.groupValues[2].toIntOrNull()?.coerceIn(0, 100)
    if (percent == null) return null
    return OperationProgress(
        stage = stage,
        percent = percent,
        detail = match.groupValues[3].trim().ifBlank { null },
    )
}