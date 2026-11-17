package org.baconana.mybeautifulgit_chan.domain

/** Одна строка `git blame --line-porcelain`; [text] хранится без ведущего служебного таба. */
data class BlameLine(
    val hash: String,
    val lineNumber: Int,
    val authorName: String,
    val authorEmail: String,
    val authorTime: Long,
    val summary: String,
    val text: String,
)

data class BlameResult(
    val path: String,
    val lines: List<BlameLine>,
    val truncated: Boolean = false,
)

/** Одно совпадение `git grep`; путь и номер строки нужны для перехода к файлу. */
data class GrepMatch(
    val path: String,
    val lineNumber: Int,
    val text: String,
)

data class GrepResult(
    val matches: List<GrepMatch>,
    val truncated: Boolean = false,
)
