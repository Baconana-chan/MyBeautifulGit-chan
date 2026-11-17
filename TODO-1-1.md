# TODO 1.1 — GitHub/GitLab adapters и немецкая локализация

Статус: **план, работа не начата**. Это не обещание даты и не замена основному
[TODO.md](./TODO.md): здесь только объём 1.1 и явно отложенные вещи.

База: desktop 1.0 с уже готовым **M11 Level 1 — Custom Git Host** (разбор remote,
`insteadOf`, несколько URL и локальный preflight без API). Android в 1.1 не входит.

## 0. Цель релиза

1.1 должна дать первые **Level 2** адаптеры для GitHub и GitLab и сделать интерфейс
полноценно доступным на немецком, не ломая offline-сценарии и обычные Git-операции.

Главный принцип остаётся прежним: адаптер **показывает проверенные данные и предлагает
явные действия**, а не угадывает права и не делает push/fork/PR за пользователя.

## 1. Что входит и что нет

M11 coverage: берём M11.1 (identity/accounts/cache), M11.2 (GitHub/GitLab API),
M11.3 (state matrix), M11.4 (preflight + compare/create) и M11.5 (security/glossary).
Из M11.6 в 1.1 входят только первые два адаптера; остальные хосты не входят.

### 1.1. Обязательно для 1.1 (P0)

- [ ] Provider-фундамент: общая модель адаптеров, HTTP-клиент, аккаунты, ошибки, кэш,
      rate limit и offline-поведение.
- [ ] GitHub-адаптер: identity, метаданные репозитория, права, contribution state,
      поиск существующего PR, compare URL и создание PR.
- [ ] GitLab-адаптер: то же для merge request, включая вложенные группы.
- [ ] Интеграция в приложение: панель contribution, выбор аккаунта, preflight,
      подтверждение действий и открытие результата в браузере.
- [ ] Немецкая локализация интерфейса: `Language.DE`, полный `GermanStrings`,
      переключатель языка, сохранение `ui.language=de`, немецкий язык AI-сообщений.
- [ ] Unit/contract-тесты, ручная матрица на тестовых аккаунтах, обновление README,
      user guide и release notes.
- [ ] Проверка, что Level 1 Custom Git Host продолжает работать без сети и без аккаунта.

### 1.2. Полезно, но не блокирует 1.1 (P1)

Эти пункты берём только после зелёного P0; иначе переносим в 1.2:

- [ ] Статус PR/MR после создания: open/merged/closed, checks summary, mergeability.
- [ ] Шаблоны title/body и расширенное редактирование после создания.
- [ ] Мини-глоссарий fork/upstream/base/head/PR/MR/checks в панели.
- [ ] ETag/Last-Modified кэш с явным «данные устарели», если сеть недоступна.
- [ ] Немецкий перевод README/user guide (не только UI).

### 1.3. Явно после 1.1

- [ ] Gitea/Forgejo/Codeberg и остальные хосты из M11.6.
- [ ] GitHub Enterprise Server, self-hosted GitLab, настраиваемый API base URL.
- [ ] OAuth/device flow; в 1.1 только PAT.
- [ ] Автоматический fork, автоматическое добавление `upstream`, автоматический push.
- [ ] Reviews, comment threads, resolve conversation, update branch, auto-merge.
- [ ] Android, auto-update install, другие крупные фичи 1.0.
- [ ] Долги/баги 1.0, обнаруженные после релиза; чинить по severity, не расширять ими 1.1.
- [ ] Любой тяжёлый рефакторинг, не нужный для P0.

## 2. Что уже есть и можно переиспользовать

- `RemoteUrlParser` и `RemoteProfile` из Level 1: host, owner/repo, transport,
  несколько fetch/push URL, `insteadOf`, local/unknown.
- `RemoteManagerDialog`: выбранный remote, URL, HEAD, local preflight.
- `CredentialStore`/`AppPaths`: AES-GCM-хранилище и owner-only permissions; сейчас
  отдельные файлы для HTTPS-токена и AI-ключа.
- `DesktopActions.openUrl()`: открытие разрешённого http(s)-URL в браузере.
- `kotlinx.serialization` и `java.net.http.HttpClient` (JDK 25) — можно не добавлять
  новый HTTP-фреймворк в 1.1.
- `Language`/`Strings`: EN/RU и полный интерфейс строк; немецкий добавляется как
  третий язык, а не отдельный слой UI.
- Существующие push/fetch/branch managers: адаптер не должен их подменять.

## 3. Workstream A — provider-фундамент

### A1. Модель и границы

- [ ] Вынести общие модели в `shared/src/domain/forge/`:
      `ForgeProvider`, `ForgeAccount`, `ForgeRepository`, `ContributionState`,
      `ForgeCapabilities`, `PullRequestSummary`, `ForgeError`.
- [ ] `ContributionState` должен содержать `state`, `reason`, `evidence`, `confidence`
      (как в M11.3), а не только enum. Порядок определения:
      `OWN` (канонический репозиторий принадлежит аккаунту) → `COLLABORATOR` (push в чужой
      канонический репозиторий) → `FORK` (origin — форк с parent/upstream) → `READ_ONLY`
      (remote есть, но подтверждённых push-прав и форка нет) → `UNKNOWN` (нет данных/аккаунта,
      несколько аккаунтов, сеть/API недоступны). `READ_ONLY` нельзя выводить из 404 или
      отсутствия токена.
- [ ] Адаптер разрешается только по точному host из `RemoteProfile`:
      `github.com` → GitHub, `gitlab.com` → GitLab. Неизвестный/self-hosted host
      остаётся Level 1 (`CUSTOM`/`UNKNOWN`), без догадок.
- [ ] Capabilities явно описывают, что адаптер умеет: identity, permissions,
      PR/MR lookup, create, checks, reviews. Отсутствующая capability = `UNKNOWN`,
      а не «не поддерживается вообще».
- [ ] Provider-код не должен менять `RemoteProfile` и не должен сам менять Git config.

### A2. HTTP и JSON

- [ ] Ввести `ForgeHttpClient`-абстракцию; desktop-реализация — JDK `HttpClient`.
- [ ] HTTPS-only, фиксированные base URL: `https://api.github.com` и
      `https://gitlab.com/api/v4`. В 1.1 не принимать произвольный base URL.
- [ ] Не отправлять `Authorization`/`PRIVATE-TOKEN` на другой host после redirect;
      cross-host redirect отклонять.
- [ ] Таймауты, отмена корутины, ограничение размера ответа и тела ошибки.
- [ ] JSON через существующий `kotlinx.serialization`; все необязательные поля nullable.
- [ ] Не логировать request/response headers и token; тела ошибок — только после
      redaction и с лимитом.
- [ ] Тесты на fake HTTP server: success, 401, 403, 404, 409/422, 429, 5xx, timeout,
      malformed JSON, redirect.

### A3. Аккаунты и токены

- [ ] Не использовать один общий `credentials.bin` для провайдеров: добавить
      provider credential store с ключом по `(provider, host, accountId)`.
      Формат — такой же AEAD-подход, атомарная запись, owner-only permissions.
- [ ] Хранить token отдельно от `settings.properties`, Git remote URL и command log.
- [ ] Метаданные аккаунта (provider, login, id, scopes, last validated) — без секрета;
      активный аккаунт хранится явно.
- [ ] Поддержать несколько аккаунтов на провайдера; при неоднозначности не выбирать
      автоматически — показать выбор.
- [ ] Удаление аккаунта удаляет token и не затрагивает Git credentials.
- [ ] Добавить тест: token не появляется в settings/log/command preview; после удаления
      аккаунта повторный запрос требует входа.

### A4. Кэш, rate limit и offline

- [ ] TTL-кэш метаданных per account/repository для UI; условные запросы
      ETag/Last-Modified — P1.
- [ ] Обрабатывать `X-RateLimit-Remaining/Reset` (GitHub) и `Retry-After`;
      не делать автоматических retry-циклов.
- [ ] 401 → «нужен вход», 403 → «нет прав или rate limit» с evidence, 404 →
      «репозиторий/PR не найден», 429 → время повтора, 5xx/network → stale/UNKNOWN.
- [ ] Сетевой сбой не блокирует remote manager, status, fetch, push и обычные Git-команды.
- [ ] Не опрашивать API при открытии репозитория или выборе remote без явного действия;
      refresh/открытие provider-панели — явные триггеры.

## 4. Workstream B — GitHub

### B1. Чтение

- [ ] `GET /user`: login/id/name; identity только после успешной проверки token.
- [ ] `GET /repos/{owner}/{repo}`:
      `default_branch`, `html_url`, `fork`, `parent`, `source`, `permissions.push`,
      `permissions.admin` (если поле отсутствует — `UNKNOWN`).
- [ ] Публичный репозиторий без token: только публичные метаданные; permissions и
      contribution state остаются `UNKNOWN`, не `READ_ONLY`.
- [ ] Учитывать канонический репозиторий: `source`/`parent` для форка; не считать
      `origin` каноническим только по имени.
- [ ] GitHub REST headers: `Accept: application/vnd.github+json`,
      `X-GitHub-Api-Version: 2022-11-28`, корректный User-Agent.

### B2. PR-сценарий

- [ ] `GET /repos/{owner}/{repo}/pulls?state=all&head=<owner>:<branch>&base=<base>`
      для поиска существующего PR; не создавать дубликат.
- [ ] Compare URL строить только из проверенных `html_url` (GitHub) или `web_url`
      (GitLab), host/owner/repo/branch; при невозможности — manual URL.
- [ ] `POST /repos/{owner}/{repo}/pulls` с `title`, `body`, `head`, `base`, `draft`.
- [ ] Перед POST показать: репозиторий, base/head, title/body, draft, endpoint и метод;
      создание только после подтверждения.
- [ ] После успеха показать номер/URL PR и обновить панель; никакого auto-merge.
- [ ] Если ветка ещё не на remote — объяснить и открыть существующий push flow;
      не делать push автоматически.
- [ ] Для fork: `origin` — head, upstream/parent — base; base/head можно выбрать вручную.
      Автоматический fork API в 1.1 не обязателен.
- [ ] Для read-only: объяснить fork и открыть страницу fork; не выполнять `POST fork`
      без отдельного дизайна и подтверждения.

### B3. Токены

- [ ] Classic PAT: минимально `repo` для private/PR; `public_repo` для public, если
      достаточно. Fine-grained PAT: Metadata read, Contents read, Pull requests write;
      точный набор проверить на тестовом аккаунте.
- [ ] Не использовать provider PAT как Git push credential; push остаётся задачей
      существующей Git-аутентификации.
- [ ] В UI объяснить разницу между GitHub token и Git remote credentials.

## 5. Workstream C — GitLab

### C1. Чтение

- [ ] `GET /user`: id/username/name.
- [ ] `GET /projects/:id`, где `:id` — URL-encoded `path_with_namespace`
      (включая вложенные группы, например `group/sub/repo` → `group%2Fsub%2Frepo`).
- [ ] Использовать `default_branch`, `web_url`, `path_with_namespace`,
      `forked_from_project`, `permissions.project_access.access_level` (и `group_access`,
      если GitLab его отдаёт). Не выводить права из 404 или отсутствия token; access level
      трактовать только как подтверждённое evidence.
- [ ] Публичный проект без token: только публичные метаданные; права — `UNKNOWN`.
- [ ] Не угадывать host как GitLab по имени папки; self-hosted GitLab остаётся Level 1.

### C2. MR-сценарий

- [ ] `GET /projects/:id/merge_requests?state=all&source_branch=<head>&target_branch=<base>`
      для поиска существующего MR.
- [ ] Compare URL из проверенного `web_url` и веток; fallback — manual URL.
- [ ] `POST /projects/:id/merge_requests` с `source_branch`, `target_branch`, `title`,
      `description`; для fork явно выбирать target project и использовать поддерживаемый
      GitLab API-параметр для target, а не считать source и target одним проектом.
      Draft оформлять через `Draft:` в title, а не несуществующим полем.
- [ ] Показывать project, source/target, title/description, endpoint и метод до POST;
      создание только после подтверждения.
- [ ] После успеха — `iid`, URL, состояние; не делать auto-merge и не удалять
      source branch автоматически.
- [ ] Для fork использовать `forked_from_project`; base/head можно выбрать вручную.
- [ ] Токены: `read_api` для чтения, `api` для создания MR; `read_user` для `/user`.
      Не передавать token query-параметром; использовать `PRIVATE-TOKEN` header.

## 6. Workstream D — приложение и UX

### D1. Contribution-панель

- [ ] Добавить панель **CONTRIBUTION** в remote manager (или отдельную вкладку,
      если панель не помещается), не ломая Level 1 preflight.
- [ ] Состояния панели: signed out, loading, public-only, connected, stale/offline,
      rate limited, UNKNOWN, error.
- [ ] Показывать: аккаунт, canonical repository, default branch, fork/upstream,
      permissions evidence, `ContributionState` с reason/confidence, существующий PR/MR.
- [ ] Network preflight: текущая ветка, dirty state, upstream, ahead/behind, коммиты вне
      base и наличие ветки на remote через provider API; при недоступности — manual URL
      и явное объяснение, без ложного «готово к PR».
- [ ] Кнопки: refresh, open compare, open existing PR/MR, sign in/manage account,
      create PR/MR, open fork page (если включено). Все сетевые действия — явные.
- [ ] Base/head выбираются вручную при `FORK`/`UNKNOWN`; никаких догадок по `origin`.
- [ ] Для `READ_ONLY` объяснить fork до push; не показывать ложную кнопку создания PR.
- [ ] Для `UNKNOWN` показать, каких данных не хватает, и оставить ручной URL.

### D2. Настройки и аккаунты

- [ ] Раздел Providers/Accounts в Settings: GitHub, GitLab, token, scopes hint,
      «проверить подключение», удалить аккаунт, выбрать активный.
- [ ] Пароль/токен вводится как secret field; после сохранения значение не показывается.
- [ ] Явно предупредить, что token даёт доступ к API, а push использует отдельные
      Git credentials.
- [ ] Не хранить token в `settings.properties`, command history, логах или URL.
- [ ] Добавить состояние «токен истёк/отозван» и кнопку повторного входа.

### D3. Действия и безопасность UX

- [ ] Создание PR/MR — только после подтверждения с итоговым payload.
- [ ] Не делать скрытый push, fork, remote add/set-url или merge.
- [ ] При отсутствии upstream показать точную команду и открыть существующий push flow.
- [ ] Все внешние URL проверять: https, ожидаемый provider host; не открывать
      произвольный URL из API без проверки.
- [ ] Добавить i18n-строки для новых панелей/ошибок; строки не должны содержать
      технические токены или сырые response bodies.
- [ ] Обновить command palette: refresh contribution, open PR/MR, manage accounts;
      destructive/опасные действия явно помечены.

## 7. Workstream E — немецкая локализация

### E1. Языковая инфраструктура

- [ ] Добавить `Language.DE("de", "DE", GermanStrings)`.
- [ ] `Language.fromCode` распознаёт `de`, `de-DE`, `de-AT`, `de-CH`; неизвестный
      язык по-прежнему fallback EN.
- [ ] `Language.entries` автоматически даёт DE в переключателе шапки и Settings.
- [ ] Сохранять `ui.language=de` в `settings.properties`; системная локаль `de-*`
      выбирается при первом запуске.
- [ ] Обновить `GitLocator`/install-screen заголовок и все места, где сейчас есть
      отдельные EN/RU ветки.

### E2. Полный GermanStrings

- [ ] Реализовать `GermanStrings : Strings` для **всех** свойств интерфейса; компилятор
      должен ловить пропуски, а не runtime fallback.
- [ ] Перевести новые provider/contribution/account строки одновременно с их добавлением,
      не оставляя английские обрывки в немецком UI.
- [ ] Терминальные заголовки (`START`, `CHANGES`, `HISTORY`, команды Git) остаются
      английскими по существующему визуальному языку; переводить только пояснения.
- [ ] Зафиксировать глоссарий: Branch/Commit/Merge/Rebase/Remote/Stash/PR/MR/Check
      используются единообразно; не переводить имена команд и флагов.
- [ ] Проверить длинные немецкие строки на обрезку в узких панелях, диалогах и тостах;
      при необходимости сокращать, а не ломать layout.
- [ ] Проверить ошибки, loading, empty states, accessibility labels и диалоги
      подтверждения — не только главный экран.

### E3. AI и документация

- [ ] Добавить `AiMessageLanguage.German` (`german`) и prompt-инструкцию «German»;
      Settings показывает немецкий вариант.
- [ ] Проверить `AppSettings` round-trip `ai.commitMessage.language=german`.
- [ ] README/user guide на немецком — P1, не блокирует 1.1; минимум обновить
      английский README списком поддерживаемых языков.
- [ ] Перевод должен пройти ревью носителя/компетентного German-редактора; машинный
      черновик без ревью не считать готовым релизным переводом.
- [ ] Тесты: `Language.fromCode("de-DE")`, сохранение/загрузка языка, AI language,
      сборка `GermanStrings`, ручной smoke всех основных экранов.

## 8. Матрица проверки

### GitHub

- [ ] Public repo, без token: metadata доступна, permissions/state `UNKNOWN`, write-кнопок нет.
- [ ] Private repo, без token: понятный запрос входа, без ложного `READ_ONLY`.
- [ ] Own repo с token: `OWN`, создание PR из текущей ветки.
- [ ] Collaborator с push: `COLLABORATOR`, создание PR без форка.
- [ ] Fork: `FORK`, origin=head, upstream/parent=base, compare/create PR.
- [ ] Read-only: `READ_ONLY`, объяснение fork, без скрытого fork/push.
- [ ] Existing PR: находится, дубликат не создаётся.
- [ ] Expired/revoked token, 403 rate limit, offline, 5xx: stale/UNKNOWN и Git работает.

### GitLab

- [ ] Public/private project без token и с token.
- [ ] Own project, group project, вложенная группа (`group/sub/repo`).
- [ ] Fork: `forked_from_project`, base/head.
- [ ] Existing MR, draft MR, создание MR, результат и URL.
- [ ] Read-only, expired token, rate limit, offline.

### German UI

- [ ] Свежий запуск с `de-DE`: интерфейс на немецком, без английских пояснений
      (кроме намеренных Git-терминов).
- [ ] Переключение EN → DE → RU → EN без перезапуска.
- [ ] Перезапуск сохраняет DE.
- [ ] Все новые contribution/account/error строки переведены.
- [ ] Узкие панели/диалоги/тосты не обрезают критические кнопки.
- [ ] Level 1 remote manager и обычные Git-операции не регрессировали.

### Регрессия 1.0

- [ ] Level 1 Custom Git Host работает без аккаунта и сети.
- [ ] fetch/push/pull/rebase/reset/undo не меняют поведение.
- [ ] Нет сетевых запросов при обычном открытии репозитория.
- [ ] `./kotlin.bat build`, `./kotlin.bat build -m desktopApp` и целевые тесты зелёные.

## 9. Порядок работ и оценка

Оценка ориентировочная и относится к **P0**. P1 идёт за счёт резерва и не является
обязательством; без жёсткой даты.

| Этап | Ориентир |
|---|---:|
| A. Provider-фундамент, HTTP, аккаунты | 10–14 ч |
| B. GitHub adapter + PR | 9–13 ч |
| C. GitLab adapter + MR | 9–13 ч |
| D. UI, настройки, ошибки, i18n | 8–12 ч |
| E. GermanStrings + AI/DE + проверка | 8–12 ч |
| Тесты, ручная матрица, документация | 6–9 ч |
| **Итого (не обязательство)** | **50–73 ч** |

Порядок:

1. A — без него адаптеры снова начнут угадывать права.
2. B — GitHub как первый адаптер и проверка модели на реальном API.
3. C — GitLab; не копировать GitHub-модель без проверки GitLab permissions/fork.
4. D — UI после стабилизации JSON/state; новый текст сразу в EN/RU/DE.
5. E — GermanStrings после фиксации новых строк, чтобы не переводить дважды.
6. Финальная матрица, README/user guide/release notes, сборка desktop-пакетов.

P1 можно делать параллельно только после зелёного P0. Если время заканчивается,
P1 переносится в 1.2, а не сокращает тесты P0.

## 10. Definition of Done для 1.1

- [ ] GitHub и GitLab P0-сценарии проходят на тестовых аккаунтах, включая public,
      private, own, collaborator и fork.
- [ ] Существующий PR/MR находится; дубликаты не создаются.
- [ ] Нет скрытого push/fork/merge; все действия подтверждаются и показывают payload.
- [ ] Токены не попадают в settings, URL, command log, Git journal или error toast.
- [ ] Offline/401/403/429/5xx дают понятное состояние, а не ложные права.
- [ ] Немецкий UI полный; переключение и сохранение языка работают.
- [ ] Level 1 Custom Git Host и обычные Git-операции не регрессировали.
- [ ] Тесты и desktop-сборка зелёные; README, user guide и release notes обновлены.
- [ ] P1 либо готов, либо явно перенесён в TODO 1.2, без незавершённых «полуфич».

## 11. Риски и решения

- **PAT scopes и private repos.** Не угадывать права по 404; показывать «нет доступа
  или репозиторий отсутствует» и давать вход/выбор аккаунта.
- **Fork/upstream неоднозначны.** Сначала API `fork`/`parent`/`source`/`forked_from_project`;
  при неоднозначности — `UNKNOWN` и ручной выбор base/head.
- **Rate limit.** Кэш и `Retry-After`; не блокировать Git и не ретраить бесконечно.
- **Токен в логах.** Централизованная redaction, тест на command/log/settings.
- **Немецкий перевод.** Большой `Strings`-интерфейс и риск обрезки; перевод после
  фиксации строк, ревью и smoke на узких панелях.
- **Scope creep.** Checks/reviews/OAuth/self-hosted/Gitea не входят в P0; они не должны
  задерживать GitHub/GitLab core и немецкий UI.
- **API changes.** Модели терпимы к nullable/новым полям; фиксированная версия GitHub API
  и явные contract-тесты. Ошибки API никогда не превращаются в «read-only».

## 12. Следующий шаг

Начать с A1/A2 и одного GitHub read-only сценария (identity + repository + permissions),
затем добавить state matrix и только после этого — создание PR. Немецкую локализацию
начинать после того, как provider-строки в `Strings` перестанут массово меняться.
