package org.baconana.mybeautifulgit_chan.git

import org.baconana.mybeautifulgit_chan.domain.BlameResult
import org.baconana.mybeautifulgit_chan.domain.BranchComparison
import org.baconana.mybeautifulgit_chan.domain.Commit
import org.baconana.mybeautifulgit_chan.domain.CommitInspection
import org.baconana.mybeautifulgit_chan.domain.GitConfigEntry
import org.baconana.mybeautifulgit_chan.domain.GitConfigScope
import org.baconana.mybeautifulgit_chan.domain.GitRef
import org.baconana.mybeautifulgit_chan.domain.GrepResult
import org.baconana.mybeautifulgit_chan.domain.LogFilter
import org.baconana.mybeautifulgit_chan.domain.ReflogEntry
import org.baconana.mybeautifulgit_chan.domain.RepoStatus
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.RepositoryRuntime
import org.baconana.mybeautifulgit_chan.domain.ResetPreview
import org.baconana.mybeautifulgit_chan.domain.StashDetail
import org.baconana.mybeautifulgit_chan.domain.StashEntry
import org.baconana.mybeautifulgit_chan.domain.Submodule
import org.baconana.mybeautifulgit_chan.domain.UnreachablePlan
import org.baconana.mybeautifulgit_chan.domain.Worktree

/**
 * Единственная точка доступа к git.
 *
 * [run] намеренно принимает произвольные аргументы: это гарантия, что UI не упрётся
 * в потолок возможностей бэкенда. Чтение состояния идёт через типизированные методы.
 */
interface GitBackend {
    fun version(): GitVersion

    /** Открывает репозиторий по любому пути внутри рабочего дерева (или сам bare-репозиторий). */
    fun open(path: String): Repository

    fun run(
        cwd: String?,
        args: List<String>,
        stdin: String? = null,
        timeoutSeconds: Long = 120,
        environment: Map<String, String> = emptyMap(),
    ): GitResult

    /**
     * Запуск с управлением отменой и progress-событиями. Бэкенды без поддержки
     * могут использовать обычный [run]; production CLI переопределяет метод.
     */
    fun runControlled(
        cwd: String?,
        args: List<String>,
        stdin: String? = null,
        timeoutSeconds: Long = 120,
        environment: Map<String, String> = emptyMap(),
        control: GitRunControl,
    ): GitResult = run(cwd, args, stdin, timeoutSeconds, environment)

    fun status(repository: Repository): RepoStatus

    /** Полное сообщение коммита, включая body, без завершающего перевода строки. */
    fun commitMessage(repository: Repository, hash: String): String

    /**
     * Собирает данные для подтверждения reset: коммиты, которые покинут ветку
     * (с учётом остальных refs), уникальные объекты, fsck-сводку, локальные
     * изменения и пересечения с target.
     */
    fun resetPreview(
        repository: Repository,
        hash: String,
        maxCommitSubjects: Int = 5,
    ): ResetPreview

    /**
     * Что станет недостижимым, если HEAD/текущая ветка переедет на [targets].
     * Для reset это один target, для reword/amend/split — parents заменяемого
     * коммита (новая версия сохранит их), для rewrite корня — пустой список.
     * Учитываются остальные ветки, теги, remote refs, stash и прочие refs;
     * reflog и backup-refs остаются отдельным путём восстановления.
     */
    fun unreachablePlan(
        repository: Repository,
        targets: List<String>,
        maxCommits: Int = 10,
    ): UnreachablePlan

    /**
     * История с серверными фильтрами. Для [LogFilter.onlyMine] контроллер
     * заранее подставляет email текущего пользователя в [LogFilter.author].
     */
    fun log(
        repository: Repository,
        maxCount: Int = 100,
        rev: String = "HEAD",
        filter: LogFilter = LogFilter(),
        skip: Int = 0,
    ): List<Commit>

    /**
     * Полный diff и статистика коммита относительно первого parent, плюс
     * проверка подписи. Для merge это diff с mainline, а не combined diff.
     */
    fun commitInspection(
        repository: Repository,
        hash: String,
        maxDiffBytes: Long = 2L * 1024 * 1024,
    ): CommitInspection

    /** История одного пути с `--follow` от HEAD; переименования отслеживаются git. */
    fun fileHistory(repository: Repository, path: String, maxCount: Int = 200): List<Commit>

    /** Построчный blame версии файла из HEAD с ограничением числа строк. */
    fun blame(repository: Repository, path: String, maxLines: Int = 5000): BlameResult

    /** Фиксированный поиск `git grep` в рабочем дереве; путь опционален. */
    fun grep(
        repository: Repository,
        query: String,
        path: String = "",
        maxMatches: Int = 500,
    ): GrepResult

    /** Записи reflog по всем refs, новые сверху; нужны для навигации к коммиту. */
    fun reflog(repository: Repository, maxCount: Int = 500): List<ReflogEntry>

    fun refs(repository: Repository): List<GitRef>

    /** Настроенные remotes без обращения к сети. */
    fun remotes(repository: Repository): List<String>

    /** URL для fetch по каждому настроенному remote; без обращения к сети. */
    fun remoteUrls(repository: Repository): Map<String, String>

    /** Сравнение двух refs: ahead/behind и уникальные коммиты с обеих сторон. */
    fun compareBranches(
        repository: Repository,
        first: String,
        second: String,
        maxCommits: Int = 100,
    ): BranchComparison

    /** Все worktree, включая основной; для bare-репозитория — его единственную запись. */
    fun worktrees(repository: Repository): List<Worktree>

    /** Подмодули суперпроекта; для bare-репозитория возвращает пустой список. */
    fun submodules(repository: Repository): List<Submodule>

    /** Количество stash-записей; для bare-репозитория — 0. */
    fun stashCount(repository: Repository): Int

    /** Список stash-записей в порядке применения (новые сверху). */
    fun stashList(repository: Repository): List<StashEntry>

    /** Показывает файлы и patch выбранного stash без изменения рабочего дерева. */
    fun stashShow(
        repository: Repository,
        selector: String,
        maxBytes: Long = 512 * 1024,
    ): StashDetail

    /** Все ключи из system/global/local/worktree, включая неизвестные приложению. */
    fun configEntries(repository: Repository): List<GitConfigEntry>

    /**
     * Записывает значение в выбранный scope. Если [oldValue] задан, заменяется только
     * точное совпадение, а остальные значения multivalue-ключа сохраняются.
     */
    fun setConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        value: String,
        oldValue: String? = null,
    ): GitResult

    /** Добавляет ещё одно значение ключа, не затрагивая уже существующие. */
    fun addConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        value: String,
    ): GitResult

    /** Удаляет одно точное значение [oldValue] или все значения, если он равен null. */
    fun unsetConfig(
        repository: Repository,
        scope: GitConfigScope,
        key: String,
        oldValue: String? = null,
    ): GitResult

    /** Незавершённая операция и оставленный index.lock; читается без запуска git. */
    fun runtimeState(repository: Repository): RepositoryRuntime

    /** Удаляет index.lock, только если он существует; true — lock больше нет. */
    fun removeIndexLock(repository: Repository): Boolean

    /** Читает blob (`HEAD:path`, `:path` и т.п.) с ограничением размера. */
    fun readBlob(repository: Repository, spec: String, maxBytes: Long): GitBlob
}
