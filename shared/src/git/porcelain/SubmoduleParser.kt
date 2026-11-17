package org.baconana.mybeautifulgit_chan.git.porcelain

import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.SubmoduleState

/**
 * Разбирает `git submodule status --recursive`.
 *
 * Первый символ строки — состояние: пробел (актуален), `+` (изменён),
 * `-` (не инициализирован), `U` (конфликт). Описание в скобках опционально.
 */
object SubmoduleParser {

    private val LINE = Regex("""^([ +\-U])([0-9a-fA-F]{7,64}) (.+?)(?: \((.*)\))?$""")

    fun parse(raw: String): List<Submodule> = raw.lineSequence().mapNotNull { rawLine ->
        val line = rawLine.trimEnd('\r')
        val match = LINE.matchEntire(line) ?: return@mapNotNull null
        val marker = match.groupValues[1].first()
        val state = when (marker) {
            '+' -> SubmoduleState.Modified
            '-' -> SubmoduleState.Uninitialized
            'U' -> SubmoduleState.Conflict
            else -> SubmoduleState.Current
        }
        Submodule(
            path = match.groupValues[3],
            head = match.groupValues[2],
            state = state,
            describe = match.groupValues[4].ifBlank { null },
        )
    }.toList()
}
