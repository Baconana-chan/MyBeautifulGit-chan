package org.baconana.mybeautifulgit_chan.app

/**
 * Разбирает строку команды без shell: пробелы разделяют аргументы, одинарные
 * и двойные кавычки группируют текст. Никаких подстановок, пайпов и редиректов.
 */
object GitCommandLine {

    sealed interface Parsed {
        data class Args(val values: List<String>) : Parsed
        data object Empty : Parsed
        data object UnclosedQuote : Parsed
    }

    fun parse(raw: String): Parsed {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var started = false

        for (character in raw) {
            if (quote != null) {
                if (character == quote) {
                    quote = null
                } else {
                    current.append(character)
                }
                started = true
                continue
            }

            when {
                character == '\'' || character == '"' -> {
                    quote = character
                    started = true
                }

                character.isWhitespace() -> {
                    if (started) {
                        values += current.toString()
                        current.clear()
                        started = false
                    }
                }

                else -> {
                    current.append(character)
                    started = true
                }
            }
        }

        if (quote != null) return Parsed.UnclosedQuote

        if (started) values += current.toString()
        return if (values.isEmpty()) Parsed.Empty else Parsed.Args(values)
    }
}
