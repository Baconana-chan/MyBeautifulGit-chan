package org.baconana.mybeautifulgit_chan.domain

/**
 * Открытый git-репозиторий.
 *
 * [root] — корень рабочего дерева, а для bare-репозитория — его каталог.
 * [workTree] равен null только у bare-репозитория.
 */
data class Repository(
    val root: String,
    val gitDir: String,
    val commonDir: String,
    val name: String,
    val isBare: Boolean,
    val isWorktree: Boolean,
    val workTree: String?,
)

/**
 * Папка выглядит как Git-репозиторий, но Git отказался его открыть из-за
 * неполных или недоступных служебных данных. UI переводит это исключение в
 * понятное действие, а [cause] сохраняет исходный ответ Git для диагностики.
 *
 * [missingMetadata] содержит относительные метки, например `.git/objects`.
 */
class RepositoryMetadataException(
    val repositoryPath: String,
    val missingMetadata: List<String>,
    cause: Throwable? = null,
) : IllegalStateException(
    "Repository metadata is incomplete at $repositoryPath: ${missingMetadata.joinToString()}",
    cause,
)
