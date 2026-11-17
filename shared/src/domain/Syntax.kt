package org.baconana.mybeautifulgit_chan.domain

/** Категории подсветки; UI сам выбирает цвета терминальной палитры. */
enum class SyntaxKind {
    Keyword,
    String,
    Comment,
    Number,
    Type,
    Function,
    Constant,
    Annotation,
    Tag,
    Attribute,
}

/** Диапазон подсветки внутри одной строки. */
data class SyntaxSpan(
    val start: Int,
    val end: Int,
    val kind: SyntaxKind,
)

private data class SyntaxSpec(
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    val quotes: Set<Char> = setOf('"', '\''),
    val backtickStrings: Boolean = false,
    val keywords: Set<String> = COMMON_KEYWORDS,
    val constants: Set<String> = COMMON_CONSTANTS,
    val annotations: Boolean = false,
    val markup: Boolean = false,
    val caseInsensitiveKeywords: Boolean = false,
)

private val COMMON_KEYWORDS = setOf(
    "abstract", "actual", "and", "as", "assert", "async", "await", "break", "by",
    "case", "catch", "class", "const", "constructor", "continue", "data", "default",
    "delegate", "do", "dynamic", "elif", "else", "enum", "except", "expect", "export",
    "extends", "external", "final", "finally", "for", "from", "fun", "func", "function",
    "get", "global", "if", "impl", "implements", "import", "in", "init", "inline",
    "instanceof", "interface", "internal", "is", "lambda", "lateinit", "let", "local",
    "match", "mod", "module", "mut", "namespace", "new", "noinline", "nonlocal", "not",
    "object", "of", "open", "operator", "or", "out", "override", "package", "pass",
    "private", "protected", "pub", "public", "raise", "reified", "return", "sealed",
    "select", "self", "set", "static", "struct", "super", "suspend", "tailrec", "then",
    "this", "throw", "throws", "trait", "try", "type", "typealias", "typeof", "unsafe",
    "use", "val", "var", "vararg", "virtual", "void", "when", "where", "while", "with",
    "yield", "echo",
)

private val COMMON_CONSTANTS = setOf(
    "true", "false", "null", "nil", "None", "undefined", "NaN", "Infinity",
)

private val MARKUP_SPEC = SyntaxSpec(
    blockComments = listOf("<!--" to "-->"),
    keywords = emptySet(),
    markup = true,
)

private val SHELL_SPEC = SyntaxSpec(
    lineComments = listOf("#"),
    keywords = COMMON_KEYWORDS + setOf("fi", "done", "esac", "source", "readonly"),
)

private val PYTHON_SPEC = SyntaxSpec(
    lineComments = listOf("#"),
    quotes = setOf('"', '\''),
)

private val JSON_SPEC = SyntaxSpec(
    keywords = emptySet(),
    constants = COMMON_CONSTANTS,
)

private val MARKDOWN_SPEC = SyntaxSpec(
    blockComments = listOf("<!--" to "-->"),
    keywords = emptySet(),
    backtickStrings = true,
)

private val CSS_SPEC = SyntaxSpec(
    lineComments = emptyList(),
    blockComments = listOf("/*" to "*/"),
    quotes = setOf('"', '\''),
    keywords = emptySet(),
)

private val SQL_SPEC = SyntaxSpec(
    lineComments = listOf("--"),
    blockComments = listOf("/*" to "*/"),
    quotes = setOf('\''),
    keywords = COMMON_KEYWORDS + setOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
        "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "JOIN", "LEFT", "RIGHT",
        "INNER", "OUTER", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET",
        "AS", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY", "FOREIGN", "REFERENCES",
    ),
    caseInsensitiveKeywords = true,
)

private fun syntaxSpec(path: String?): SyntaxSpec? {
    val extension = path
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return when (extension) {
        "kt", "kts", "gradle" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
            annotations = true,
        )

        "java", "scala", "groovy" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
            annotations = true,
        )

        "js", "mjs", "cjs", "jsx", "ts", "tsx" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
            backtickStrings = true,
            constants = COMMON_CONSTANTS,
        )

        "c", "h", "cc", "cpp", "cxx", "hpp", "cs", "swift", "dart" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
            annotations = extension == "cs",
        )

        "rs" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
        )

        "go" -> SyntaxSpec(
            lineComments = listOf("//"),
            blockComments = listOf("/*" to "*/"),
            backtickStrings = true,
        )

        "py" -> PYTHON_SPEC
        "rb" -> SyntaxSpec(lineComments = listOf("#"))
        "sh", "bash", "zsh", "fish" -> SHELL_SPEC
        "json" -> JSON_SPEC
        "yaml", "yml", "toml" -> SyntaxSpec(lineComments = listOf("#"))
        "xml", "html", "htm", "svg", "xhtml" -> MARKUP_SPEC
        "css", "scss", "less" -> CSS_SPEC
        "md", "markdown" -> MARKDOWN_SPEC
        "sql" -> SQL_SPEC
        "php" -> SyntaxSpec(
            lineComments = listOf("//", "#"),
            blockComments = listOf("/*" to "*/"),
        )

        "lua" -> SyntaxSpec(
            lineComments = listOf("--"),
            blockComments = listOf("--[[" to "]]"),
        )

        "pl", "pm", "r" -> SyntaxSpec(lineComments = listOf("#"))
        else -> null
    }
}

/**
 * Лёгкая строчная подсветка без внешних зависимостей. Она не претендует на
 * полноценный parser: цель — различить комментарии, строки, числа, типы,
 * функции, ключевые слова и разметку, не меняя исходный текст.
 */
fun highlightSyntax(path: String?, text: String): List<SyntaxSpan> {
    val spec = syntaxSpec(path) ?: return emptyList()
    val spans = ArrayList<SyntaxSpan>()
    var index = 0

    while (index < text.length) {
        val blockComment = spec.blockComments.firstOrNull { (start, _) ->
            text.startsWith(start, index)
        }
        if (blockComment != null) {
            val start = index
            val endMarker = blockComment.second
            val closing = text.indexOf(endMarker, index + blockComment.first.length)
            index = if (closing < 0) text.length else closing + endMarker.length
            spans += SyntaxSpan(start, index, SyntaxKind.Comment)
            continue
        }

        val lineComment = spec.lineComments.firstOrNull { text.startsWith(it, index) }
        if (lineComment != null) {
            spans += SyntaxSpan(index, text.length, SyntaxKind.Comment)
            break
        }

        if (spec.markup && text[index] == '<') {
            index = scanMarkupTag(text, index, spans)
            continue
        }

        val char = text[index]
        if (char == '"' || char == '\'' || (char == '`' && spec.backtickStrings)) {
            val start = index
            index = scanString(text, index, char)
            spans += SyntaxSpan(start, index, SyntaxKind.String)
            continue
        }

        if (spec.annotations && char == '@') {
            val start = index
            index += 1
            while (index < text.length && isIdentifierPart(text[index])) index += 1
            if (index > start + 1) {
                spans += SyntaxSpan(start, index, SyntaxKind.Annotation)
            }
            continue
        }

        if (char.isDigit()) {
            val start = index
            index = scanNumber(text, index)
            spans += SyntaxSpan(start, index, SyntaxKind.Number)
            continue
        }

        if (isIdentifierStart(char)) {
            val start = index
            index += 1
            while (index < text.length && isIdentifierPart(text[index])) index += 1
            val word = text.substring(start, index)
            val kind = identifierKind(text, word, start, index, spec)
            if (kind != null) spans += SyntaxSpan(start, index, kind)
            continue
        }

        index += 1
    }

    return spans
}

private fun identifierKind(
    text: String,
    word: String,
    start: Int,
    end: Int,
    spec: SyntaxSpec,
): SyntaxKind? {
    val comparison = if (spec.caseInsensitiveKeywords) word.lowercase() else word
    val keywords = if (spec.caseInsensitiveKeywords) {
        spec.keywords.mapTo(HashSet()) { it.lowercase() }
    } else {
        spec.keywords
    }
    val constants = if (spec.caseInsensitiveKeywords) {
        spec.constants.mapTo(HashSet()) { it.lowercase() }
    } else {
        spec.constants
    }

    if (comparison in constants) return SyntaxKind.Constant
    if (comparison in keywords) return SyntaxKind.Keyword
    if (word.firstOrNull()?.isUpperCase() == true) return SyntaxKind.Type

    var next = end
    while (next < text.length && text[next].isWhitespace()) next += 1
    if (next < text.length && text[next] == '(') return SyntaxKind.Function
    return null
}

private fun scanString(text: String, start: Int, quote: Char): Int {
    val triple = start + 2 < text.length &&
        text[start + 1] == quote &&
        text[start + 2] == quote
    val delimiterLength = if (triple) 3 else 1
    var index = start + delimiterLength
    while (index < text.length) {
        if (
            triple &&
            index + 2 < text.length &&
            text[index] == quote &&
            text[index + 1] == quote &&
            text[index + 2] == quote
        ) {
            return index + 3
        }
        if (!triple && text[index] == quote) return index + 1
        if (text[index] == '\\' && index + 1 < text.length) {
            index += 2
        } else {
            index += 1
        }
    }
    return text.length
}

private fun scanNumber(text: String, start: Int): Int {
    var index = start + 1
    if (text[start] == '0' && index < text.length &&
        (text[index] == 'x' || text[index] == 'X' || text[index] == 'b' || text[index] == 'B')
    ) {
        index += 1
        while (
            index < text.length &&
            (text[index].isLetterOrDigit() || text[index] == '_')
        ) {
            index += 1
        }
        return index
    }
    while (index < text.length) {
        val char = text[index]
        if (char.isDigit() || char == '_' || char == '.') {
            index += 1
        } else if (char == 'e' || char == 'E') {
            index += 1
            if (index < text.length && (text[index] == '+' || text[index] == '-')) index += 1
        } else if (char.isLetter()) {
            index += 1
        } else {
            break
        }
    }
    return index
}

private fun scanMarkupTag(text: String, start: Int, spans: MutableList<SyntaxSpan>): Int {
    val end = text.indexOf('>', start)
    val stop = if (end < 0) text.length else end
    var index = start + 1
    if (index < stop && (text[index] == '/' || text[index] == '?' || text[index] == '!')) {
        index += 1
    }
    val nameStart = index
    while (index < stop && isIdentifierPart(text[index])) index += 1
    if (index > nameStart) {
        spans += SyntaxSpan(start, index, SyntaxKind.Tag)
    }

    while (index < stop) {
        if (text[index] == '"' || text[index] == '\'') {
            val quote = text[index]
            val stringStart = index
            index = scanString(text, index, quote).coerceAtMost(stop)
            spans += SyntaxSpan(stringStart, index, SyntaxKind.String)
            continue
        }
        if (isIdentifierStart(text[index])) {
            val attributeStart = index
            index += 1
            while (index < stop && isIdentifierPart(text[index])) index += 1
            var next = index
            while (next < stop && text[next].isWhitespace()) next += 1
            if (next < stop && text[next] == '=') {
                spans += SyntaxSpan(attributeStart, index, SyntaxKind.Attribute)
            }
            continue
        }
        index += 1
    }

    return if (end < 0) text.length else end + 1
}

private fun isIdentifierStart(char: Char): Boolean = char.isLetter() || char == '_' || char == '$'

private fun isIdentifierPart(char: Char): Boolean =
    char.isLetterOrDigit() || char == '_' || char == '$' || char == '-'
