package org.baconana.mybeautifulgit_chan.domain

/**
 * Команды интерактивного rebase. Полный набор из git-rebase-todo; сокращения
 * принимаются при чтении, но в UI и в файл всегда пишется полное имя.
 */
enum class RebaseCommand(val token: String, val takesCommit: Boolean = false) {
    Pick("pick", takesCommit = true),
    Reword("reword", takesCommit = true),
    Edit("edit", takesCommit = true),
    Squash("squash", takesCommit = true),
    Fixup("fixup", takesCommit = true),
    Drop("drop", takesCommit = true),
    Exec("exec"),
    Break("break"),
    Label("label"),
    Reset("reset"),
    Merge("merge");

    val label: String
        get() = when (this) {
            Pick -> "pick"
            Reword -> "reword"
            Edit -> "edit"
            Squash -> "squash"
            Fixup -> "fixup"
            Drop -> "drop"
            Exec -> "exec"
            Break -> "break"
            Label -> "label"
            Reset -> "reset"
            Merge -> "merge"
        }

    companion object {
        fun fromToken(value: String): RebaseCommand? = when (value.lowercase()) {
            "p", "pick" -> Pick
            "r", "reword" -> Reword
            "e", "edit" -> Edit
            "s", "squash" -> Squash
            "f", "fixup" -> Fixup
            "d", "drop" -> Drop
            "x", "exec" -> Exec
            "b", "break" -> Break
            "l", "label" -> Label
            "t", "reset" -> Reset
            "m", "merge" -> Merge
            else -> null
        }
    }
}

/**
 * Одна строка todo. [argument] хранит hash, имя label или хвост merge/exec
 * без изменений; [comment] — только предмет коммита после `#`.
 */
data class RebaseTodoEntry(
    val id: Long,
    val command: RebaseCommand,
    val argument: String = "",
    val comment: String = "",
    /** Сообщение для reword; не входит в todo, а пишется в message-editor. */
    val message: String = "",
) {
    val subject: String
        get() = comment.removePrefix("#").trim()

    val hash: String
        get() = if (command.takesCommit) {
            argument.trim().split(Regex("\\s+")).lastOrNull().orEmpty()
        } else {
            ""
        }

    fun todoLine(): String {
        val tail = argument.trim()
        return when (command) {
            RebaseCommand.Break -> "break"
            RebaseCommand.Exec, RebaseCommand.Label, RebaseCommand.Reset, RebaseCommand.Merge ->
                "${command.token} $tail".trimEnd()
            else -> {
                val base = "${command.token} $tail".trimEnd()
                if (comment.isBlank()) base else "$base # ${comment.trim()}"
            }
        }
    }
}

enum class RebaseTodoProblem {
    Empty,
    Newline,
    MissingCommit,
    MissingExec,
    MissingLabel,
    MissingMerge,
    MissingMessage,
}

/** Todo-план, снятый с git перед началом интерактивного rebase. */
data class RebasePlan(
    val base: String?,
    val root: Boolean,
    val entries: List<RebaseTodoEntry>,
    val autosquash: Boolean = false,
)

/**
 * Парсер и сериализатор git-rebase-todo. Строки-комментарии и пустые строки
 * не входят в модель: их всё равно перегенерирует git. Неизвестные команды
 * отбрасываются, чтобы старый формат не ломал редактор.
 */
object RebaseTodo {
    fun parse(text: String, startingId: Long = 0L): List<RebaseTodoEntry> {
        var id = startingId
        return text.lineSequence()
            .map { it.trimEnd('\r', '\n').trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val split = line.split(Regex("\\s+"), limit = 2)
                val command = RebaseCommand.fromToken(split.first()) ?: return@mapNotNull null
                val rest = split.getOrElse(1) { "" }.trim()
                val entry = when {
                    command.takesCommit -> {
                        if (rest.isEmpty()) return@mapNotNull null
                        // fixup -C <commit> и fixup -c <commit> — тоже валидный todo.
                        val parts = rest.split(Regex("\\s+"), limit = 3)
                        val hasOption = parts.first().startsWith("-")
                        if (hasOption && parts.size < 2) return@mapNotNull null
                        val argument = if (hasOption) {
                            "${parts[0]} ${parts[1]}"
                        } else {
                            parts.first()
                        }
                        if (argument.isBlank()) return@mapNotNull null
                        val after = when {
                            hasOption -> parts.getOrElse(2) { "" }
                            else -> rest.substring(argument.length).trim()
                        }.trim()
                        val comment = after.removePrefix("#").trim()
                        RebaseTodoEntry(
                            id = id++,
                            command = command,
                            argument = argument,
                            comment = comment,
                        )
                    }

                    command == RebaseCommand.Break -> RebaseTodoEntry(
                        id = id++,
                        command = command,
                    )

                    else -> RebaseTodoEntry(
                        id = id++,
                        command = command,
                        argument = rest,
                    )
                }
                entry
            }
            .toList()
    }

    fun render(entries: List<RebaseTodoEntry>): String =
        entries.joinToString(separator = "\n", postfix = "\n") { it.todoLine() }

    /** Пустые и многострочные аргументы нельзя безопасно положить в todo. */
    fun validate(entries: List<RebaseTodoEntry>): RebaseTodoProblem? {
        if (entries.isEmpty()) return RebaseTodoProblem.Empty
        entries.forEach { entry ->
            if (entry.argument.any { it == '\n' || it == '\r' || it == '\u0000' }) {
                return RebaseTodoProblem.Newline
            }
            if (entry.comment.any { it == '\n' || it == '\r' || it == '\u0000' }) {
                return RebaseTodoProblem.Newline
            }
            if (entry.command.takesCommit && entry.argument.isBlank()) {
                return RebaseTodoProblem.MissingCommit
            }
            if (entry.command == RebaseCommand.Exec && entry.argument.isBlank()) {
                return RebaseTodoProblem.MissingExec
            }
            if ((entry.command == RebaseCommand.Label || entry.command == RebaseCommand.Reset) &&
                entry.argument.isBlank()
            ) {
                return RebaseTodoProblem.MissingLabel
            }
            if (entry.command == RebaseCommand.Merge && entry.argument.isBlank()) {
                return RebaseTodoProblem.MissingMerge
            }
            if (entry.command == RebaseCommand.Reword && entry.message.isBlank()) {
                return RebaseTodoProblem.MissingMessage
            }
        }
        return null
    }
}
