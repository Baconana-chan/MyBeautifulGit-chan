package org.baconana.mybeautifulgit_chan.git.cli

import org.baconana.mybeautifulgit_chan.git.GitVersion

/**
 * Политика поддержки версий git, зашитая в релиз приложения.
 *
 * [hardMinimum] — порог возможностей, а не EOL: ниже него приложение не гарантирует
 * базовые команды. EOL upstream — отдельное, неблокирующее предупреждение: дистрибутивы
 * часто бэкпортируют патчи в старые ветки, и по `git --version` это не отличить.
 *
 * Значения [oldestSecuritySupported] обновлять при каждом релизе приложения.
 */
data class GitSupportPolicy(
    val hardMinimum: GitVersion = GitVersion.MINIMUM_SUPPORTED,
    /** Версия, начиная с которой доступен весь запланированный набор возможностей. */
    val fullToolset: GitVersion = GitFeature.entries.maxOf { it.since },
    /** Самая старая ветка, ещё получающая security-патчи upstream на момент релиза. */
    val oldestSecuritySupported: GitVersion = GitVersion(2, 52, 0, "2.52.0"),
) {
    fun meetsHardMinimum(version: GitVersion): Boolean = version >= hardMinimum

    fun hasFullToolset(version: GitVersion): Boolean = version >= fullToolset

    /**
     * Проверяет только ветку, а не патч-уровень: наличие бэкпортов в дистрибутиве
     * по выводу `git --version` определить нельзя.
     */
    fun hasSecuritySupport(version: GitVersion): Boolean = version >= oldestSecuritySupported
}
