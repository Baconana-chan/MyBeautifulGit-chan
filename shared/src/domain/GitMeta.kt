package org.baconana.mybeautifulgit_chan.domain

/** Files that configure Git itself and live in the worktree root. */
enum class GitMetaFile(val path: String) {
    GitIgnore(".gitignore"),
    GitAttributes(".gitattributes"),
    Mailmap(".mailmap"),
}

/** A parsed editor file. Missing files are represented by an empty [text]. */
data class GitMetaFileContent(
    val file: GitMetaFile,
    val text: String,
    val exists: Boolean,
)

enum class GitMetaIssueSeverity { Error, Warning }

enum class GitMetaIssueCode {
    IgnoreEmptyNegation,
    IgnoreTrailingSpace,
    TrailingEscape,
    UnclosedCharacterClass,
    AttributesMissing,
    AttributesNameInvalid,
    AttributesDuplicate,
    AttributesConflict,
    MailmapEmailRequired,
    MailmapEmailInvalid,
    MailmapTooManyEmails,
    MailmapUnexpectedText,
    MailmapDuplicateMapping,
    MailmapUnclosedEmail,
}

data class GitMetaIssue(
    /** One-based line number. */
    val line: Int,
    val severity: GitMetaIssueSeverity,
    val code: GitMetaIssueCode,
)

data class GitMetaValidation(val issues: List<GitMetaIssue> = emptyList()) {
    val errors: List<GitMetaIssue>
        get() = issues.filter { it.severity == GitMetaIssueSeverity.Error }

    val warnings: List<GitMetaIssue>
        get() = issues.filter { it.severity == GitMetaIssueSeverity.Warning }

    val ok: Boolean
        get() = errors.isEmpty()
}

enum class GitMetaTokenKind {
    Comment,
    Pattern,
    Negation,
    Attribute,
    Value,
    Email,
    Name,
    Invalid,
}

data class GitMetaSpan(
    val start: Int,
    val end: Int,
    val kind: GitMetaTokenKind,
)

fun validateGitMeta(file: GitMetaFile, text: String): GitMetaValidation = when (file) {
    GitMetaFile.GitIgnore -> GitMetaValidation(validateGitIgnore(text))
    GitMetaFile.GitAttributes -> GitMetaValidation(validateGitAttributes(text))
    GitMetaFile.Mailmap -> GitMetaValidation(validateMailmap(text))
}

fun highlightGitMeta(file: GitMetaFile, text: String): List<GitMetaSpan> {
    val spans = mutableListOf<GitMetaSpan>()
    var offset = 0
    text.split('\n').forEach { rawLine ->
        val line = rawLine.removeSuffix("\r")
        if (line.isNotEmpty()) {
            if (line.startsWith("#")) {
                spans += GitMetaSpan(offset, offset + line.length, GitMetaTokenKind.Comment)
            } else {
                when (file) {
                    GitMetaFile.GitIgnore -> highlightIgnoreLine(line, offset, spans)
                    GitMetaFile.GitAttributes -> highlightAttributesLine(line, offset, spans)
                    GitMetaFile.Mailmap -> highlightMailmapLine(line, offset, spans)
                }
            }
        }
        offset += rawLine.length + 1
    }
    return spans
}

private fun validateGitIgnore(text: String): List<GitMetaIssue> {
    val issues = mutableListOf<GitMetaIssue>()
    text.split('\n').forEachIndexed { index, rawLine ->
        val line = rawLine.removeSuffix("\r")
        if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
        val lineNumber = index + 1
        val negated = line.startsWith("!")
        val pattern = if (negated) line.drop(1) else line
        if (negated && pattern.isEmpty()) {
            issues += error(lineNumber, GitMetaIssueCode.IgnoreEmptyNegation)
            return@forEachIndexed
        }
        if (hasTrailingEscape(pattern)) {
            issues += error(lineNumber, GitMetaIssueCode.TrailingEscape)
        }
        if (hasUnclosedCharacterClass(pattern)) {
            issues += warning(lineNumber, GitMetaIssueCode.UnclosedCharacterClass)
        }
        if (line.lastOrNull() == ' ' && !isEscapedAt(line, line.lastIndex)) {
            issues += warning(lineNumber, GitMetaIssueCode.IgnoreTrailingSpace)
        }
    }
    return issues
}

private fun validateGitAttributes(text: String): List<GitMetaIssue> {
    val issues = mutableListOf<GitMetaIssue>()
    text.split('\n').forEachIndexed { index, rawLine ->
        val line = rawLine.removeSuffix("\r")
        if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
        val lineNumber = index + 1
        if (hasTrailingEscape(line)) {
            issues += error(lineNumber, GitMetaIssueCode.TrailingEscape)
        }
        val tokens = tokenizeGitAttributes(line)
        if (tokens.isEmpty()) return@forEachIndexed
        val pattern = tokens.first().text
        if (hasUnclosedCharacterClass(pattern)) {
            issues += warning(lineNumber, GitMetaIssueCode.UnclosedCharacterClass)
        }
        if (tokens.size == 1) {
            issues += warning(lineNumber, GitMetaIssueCode.AttributesMissing)
            return@forEachIndexed
        }

        val states = mutableMapOf<String, AttributeState>()
        tokens.drop(1).forEach { token ->
            val parsed = parseAttribute(token.text)
            if (parsed == null) {
                issues += error(lineNumber, GitMetaIssueCode.AttributesNameInvalid)
                return@forEach
            }
            val previous = states[parsed.name]
            if (previous != null) {
                issues += error(lineNumber, GitMetaIssueCode.AttributesDuplicate)
                if (previous != AttributeState.Unspecified &&
                    parsed.state != AttributeState.Unspecified &&
                    previous != parsed.state
                ) {
                    issues += warning(lineNumber, GitMetaIssueCode.AttributesConflict)
                }
            } else {
                states[parsed.name] = parsed.state
            }
        }
    }
    return issues
}

private fun validateMailmap(text: String): List<GitMetaIssue> {
    val issues = mutableListOf<GitMetaIssue>()
    val mappings = mutableSetOf<String>()
    text.split('\n').forEachIndexed { index, rawLine ->
        val line = rawLine.removeSuffix("\r")
        if (line.isBlank() || line.startsWith("#")) return@forEachIndexed
        val lineNumber = index + 1
        val parsed = parseMailmapLine(line)
        if (parsed.unclosed) {
            issues += error(lineNumber, GitMetaIssueCode.MailmapUnclosedEmail)
        }
        if (parsed.unexpectedAngle) {
            issues += warning(lineNumber, GitMetaIssueCode.MailmapUnexpectedText)
        }
        if (parsed.emails.isEmpty() && !parsed.unclosed) {
            issues += error(lineNumber, GitMetaIssueCode.MailmapEmailRequired)
        }
        if (parsed.emails.size > 2) {
            issues += error(lineNumber, GitMetaIssueCode.MailmapTooManyEmails)
        }
        parsed.emails.forEach { email ->
            when {
                email.isBlank() -> error(lineNumber, GitMetaIssueCode.MailmapEmailRequired)
                email.any { it.isWhitespace() || it.isISOControl() || it == '<' || it == '>' } ->
                    error(lineNumber, GitMetaIssueCode.MailmapEmailInvalid)
                !email.contains('@') -> warning(lineNumber, GitMetaIssueCode.MailmapEmailInvalid)
                else -> null
            }?.let { issues += it }
        }
        if (parsed.emails.isNotEmpty() && parsed.trailingText.isNotBlank()) {
            issues += warning(lineNumber, GitMetaIssueCode.MailmapUnexpectedText)
        }
        parsed.emails.firstOrNull()?.lowercase()?.let { key ->
            if (!mappings.add(key)) {
                issues += warning(lineNumber, GitMetaIssueCode.MailmapDuplicateMapping)
            }
        }
    }
    return issues
}

private fun highlightIgnoreLine(line: String, offset: Int, spans: MutableList<GitMetaSpan>) {
    if (line.startsWith("!")) {
        spans += GitMetaSpan(offset, offset + 1, GitMetaTokenKind.Negation)
        if (line.length == 1) {
            spans += GitMetaSpan(offset, offset + 1, GitMetaTokenKind.Invalid)
        } else {
            spans += GitMetaSpan(offset + 1, offset + line.length, GitMetaTokenKind.Pattern)
        }
        return
    }
    spans += GitMetaSpan(offset, offset + line.length, GitMetaTokenKind.Pattern)
    if (hasTrailingEscape(line)) {
        spans += GitMetaSpan(offset + line.lastIndex, offset + line.length, GitMetaTokenKind.Invalid)
    }
}

private fun highlightAttributesLine(line: String, offset: Int, spans: MutableList<GitMetaSpan>) {
    val tokens = tokenizeGitAttributes(line)
    if (tokens.isEmpty()) return
    tokens.first().let { pattern ->
        val start = offset + pattern.start
        val end = offset + pattern.end
        spans += GitMetaSpan(start, end, GitMetaTokenKind.Pattern)
        if (hasUnclosedCharacterClass(pattern.text)) {
            spans += GitMetaSpan(start, end, GitMetaTokenKind.Invalid)
        }
    }
    tokens.drop(1).forEach { token ->
        val absoluteStart = offset + token.start
        val absoluteEnd = offset + token.end
        val equal = token.text.indexOf('=')
        if (equal >= 0) {
            spans += GitMetaSpan(absoluteStart, absoluteStart + equal, GitMetaTokenKind.Attribute)
            spans += GitMetaSpan(
                absoluteStart + equal + 1,
                absoluteEnd,
                GitMetaTokenKind.Value,
            )
        } else {
            spans += GitMetaSpan(absoluteStart, absoluteEnd, GitMetaTokenKind.Attribute)
        }
        if (parseAttribute(token.text) == null) {
            spans += GitMetaSpan(absoluteStart, absoluteEnd, GitMetaTokenKind.Invalid)
        }
    }
}

private fun highlightMailmapLine(line: String, offset: Int, spans: MutableList<GitMetaSpan>) {
    var cursor = 0
    var emailCount = 0
    while (cursor < line.length) {
        val open = line.indexOf('<', cursor)
        if (open < 0) break
        val close = line.indexOf('>', open + 1)
        if (close < 0) break
        val contentStart = offset + open + 1
        val contentEnd = offset + close
        spans += GitMetaSpan(contentStart, contentEnd, GitMetaTokenKind.Email)
        emailCount += 1
        cursor = close + 1
    }
    if (emailCount == 0) {
        spans += GitMetaSpan(offset, offset + line.length, GitMetaTokenKind.Name)
        return
    }

    var nameCursor = 0
    var search = 0
    while (search < line.length) {
        val open = line.indexOf('<', search)
        if (open < 0) break
        val close = line.indexOf('>', open + 1)
        if (close < 0) break
        if (nameCursor < open) {
            spans += GitMetaSpan(offset + nameCursor, offset + open, GitMetaTokenKind.Name)
        }
        nameCursor = close + 1
        search = close + 1
    }
    if (nameCursor < line.length) {
        spans += GitMetaSpan(offset + nameCursor, offset + line.length, GitMetaTokenKind.Name)
    }
}

private data class AttributeToken(
    val text: String,
    val start: Int,
    val end: Int,
)

private enum class AttributeState { Set, Unset, Unspecified }

private data class ParsedAttribute(
    val name: String,
    val state: AttributeState,
)

private fun parseAttribute(text: String): ParsedAttribute? {
    if (text.isEmpty()) return null
    val state = when (text.first()) {
        '-' -> AttributeState.Unset
        '!' -> AttributeState.Unspecified
        else -> AttributeState.Set
    }
    if (state != AttributeState.Set && text.length == 1) return null
    val body = if (state == AttributeState.Set) text else text.drop(1)
    if (state != AttributeState.Set && body.contains('=')) return null
    val name = body.substringBefore('=')
    if (name.isEmpty() || name.any {
            it.isWhitespace() || it == '=' || it == '!' || it.isISOControl()
        }
    ) {
        return null
    }
    return ParsedAttribute(name, state)
}

private fun tokenizeGitAttributes(line: String): List<AttributeToken> {
    val tokens = mutableListOf<AttributeToken>()
    var index = 0
    while (index < line.length) {
        while (index < line.length && line[index].isWhitespace()) index += 1
        if (index >= line.length) break
        val start = index
        val text = StringBuilder()
        while (index < line.length && !line[index].isWhitespace()) {
            if (line[index] == '\\' && index + 1 < line.length) {
                text.append(line[index])
                text.append(line[index + 1])
                index += 2
            } else {
                text.append(line[index])
                index += 1
            }
        }
        tokens += AttributeToken(text.toString(), start, index)
    }
    return tokens
}

private data class ParsedMailmap(
    val emails: List<String>,
    val trailingText: String,
    val unclosed: Boolean,
    val unexpectedAngle: Boolean,
)

private fun parseMailmapLine(line: String): ParsedMailmap {
    val emails = mutableListOf<String>()
    var cursor = 0
    var unclosed = false
    var unexpectedAngle = false
    var lastClose = -1
    while (cursor < line.length) {
        when (line[cursor]) {
            '<' -> {
                val close = line.indexOf('>', cursor + 1)
                if (close < 0) {
                    unclosed = true
                    cursor = line.length
                } else {
                    emails += line.substring(cursor + 1, close)
                    lastClose = close
                    cursor = close + 1
                }
            }

            '>' -> {
                unexpectedAngle = true
                cursor += 1
            }

            else -> cursor += 1
        }
    }
    return ParsedMailmap(
        emails = emails,
        trailingText = if (lastClose >= 0) line.substring(lastClose + 1) else "",
        unclosed = unclosed,
        unexpectedAngle = unexpectedAngle,
    )
}

private fun hasTrailingEscape(value: String): Boolean {
    var count = 0
    var index = value.lastIndex
    while (index >= 0 && value[index] == '\\') {
        count += 1
        index -= 1
    }
    return count % 2 == 1
}

private fun isEscapedAt(value: String, index: Int): Boolean {
    var count = 0
    var cursor = index - 1
    while (cursor >= 0 && value[cursor] == '\\') {
        count += 1
        cursor -= 1
    }
    return count % 2 == 1
}

private fun hasUnclosedCharacterClass(value: String): Boolean {
    var index = 0
    while (index < value.length) {
        when (value[index]) {
            '\\' -> index += 2
            '[' -> {
                var close = index + 1
                while (close < value.length) {
                    when (value[close]) {
                        '\\' -> close += 2
                        ']' -> break
                        else -> close += 1
                    }
                }
                if (close >= value.length) return true
                index = close + 1
            }

            else -> index += 1
        }
    }
    return false
}

private fun error(line: Int, code: GitMetaIssueCode): GitMetaIssue =
    GitMetaIssue(line, GitMetaIssueSeverity.Error, code)

private fun warning(line: Int, code: GitMetaIssueCode): GitMetaIssue =
    GitMetaIssue(line, GitMetaIssueSeverity.Warning, code)
