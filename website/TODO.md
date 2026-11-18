# Сайт: TODO и план после 1.0

Документ про `website/` — сайт и его серверную часть. Продуктовые этапы и оценки живут в
[../TODO.md](../TODO.md), ближайший релиз — [../TODO-1-1.md](../TODO-1-1.md). Сайт не входит в
Amper-сборку приложения и не должен её блокировать: упавший сайт не мешает `./kotlin.bat build`
и не влияет на работу десктопа без сети.

Даты и часы здесь не дублируются: ориентир берём из таблицы этапов в `TODO.md`
(аккаунты/OAuth и облачный прокси — 1.2, Bitbucket Cloud — 1.3).

## 0. Что уже есть

- [x] Nuxt 4 в `website/`: лендинг `/`, гайд `/docs` (Nuxt Content, якоря, кнопки copy),
      `/download`, `/api/health`.
- [x] Терминальная палитра из `shared/src/ui/mockup/Theme.kt`, своя Shiki-тема, favicon.
- [x] `/download`: патчи ассетов по ОС, версия/дата/размер из GitHub API, фолбэк на
      `/releases/latest`, честное сообщение при недоступном API.
- [x] Прод-конфиги `deploy/Caddyfile` и `deploy/ecosystem.config.cjs` (Caddy + pm2, 127.0.0.1:3000).
- [x] Job `release` в `.github/workflows/desktop-packages.yml` — пакеты и `SHA256SUMS` в релиз по тегу.

## 1. Что сайт должен дать продукту

| Этап продукта (`TODO.md`) | Что нужно от сайта | Статус |
|---|---|---|
| 1.2: accounts/OAuth | аккаунт сайта, сессии, OAuth-брокер для хостов без device flow | не начато |
| 1.2: облачный прокси ИИ | `/api/ai/*`, квоты, лимиты, биллинг, отключение по ключу пользователя | не начато |
| 1.3: Bitbucket Cloud | брокер авторизации: у Bitbucket нет device flow | не начато |
| 1.1–1.9: остальные хосты | выбор способа входа по хосту (device flow / брокер / PAT) | ревью проведено, см. 2.2 |

Ключевая мысль: приложению **нельзя** раздавать `client_secret`. Там, где провайдер умеет
device flow — авторизация идёт напрямую из приложения, сайт не участвует. Там, где не умеет —
сайт работает брокером: приложение открывает системный браузер на сайт, сайт делает обмен кода
своим секретом и отдаёт приложению одноразовый код. Это и есть основная работа по аккаунтам.

## 2. Задачи

### 2.1 Прокси ИИ-генерации коммитов (продукт: 1.2)

Пользовательский режим «свой API key» (M9) уже работает и остаётся: прокси — опция, а не
замена. Если прокси недоступен, приложение обязано молча откатиться на свой ключ.

- [ ] Контракт: OpenAI-compatible (`/v1/chat/completions`) или свой узкий endpoint
      `POST /api/ai/commit-message`. Совместимость с OpenAI даёт минимум правок в клиенте,
      но публично открытый OpenAI-shape приглашает абьюз — закрывать токеном приложения.
- [ ] Авторизация: только аккаунт сайта (см. 2.2), per-user токен, а не общий ключ.
- [ ] Квоты: N запросов в сутки и/или token budget на пользователя, отдельный лимит по IP,
      мягкое предупреждение при приближении к лимиту.
- [ ] Полезная нагрузка: принимаем только `git diff --cached` в пределах лимита размера,
      фильтруем `.env`/ключи/бинарники ещё раз на сервере (не доверяем клиенту).
- [ ] Логи: diff, prompt и ответ **не** пишем. Только счётчики, latency, код ошибки, id пользователя.
- [ ] Ответ санитизируется до одной строки и на сервере тоже, не только в приложении.
- [ ] Ошибки: `401` (нет/просрочен токен), `402`/`429` (квота), `413` (слишком большой diff),
      `502` (провайдер) — приложение различает их и предлагает свой ключ.
- [ ] Таймауты и отмена: если пользователь закрыл диалог, запрос к провайдеру обрывается.
- [ ] Ключ провайдера — только в env на VPS, никогда в репозитории и не в клиенте.
- [ ] Kill switch: выключить прокси одной переменной окружения, без деплоя кода.
- [ ] Стоимость: потолок расходов в сутки, алерт при превышении, счётчик по моделям.
- [ ] Тесты: 401/413/429, таймаут, 5xx провайдера, отмена, отсутствие diff в логах.
- [ ] Тексты: что отправляется, куда, сколько хранится, как удалить — на `/privacy` и в гайде.

Биллинг (подписка/пакеты) — отдельным решением после квот; до него прокси бесплатный с жёстким
потолком. Пока аккаунтов нет, прокси не включаем вообще.

### 2.2 Аккаунты и OAuth (продукт: 1.2, Bitbucket — 1.3)

Ревью основного списка из `TODO.md` M11.6 проведено: у каждого хоста проверено, может ли
распределённое десктоп-приложение войти без утечки секрета. Получилось четыре стратегии:

1. **Device flow — приложение входит само, сайт не нужен:** GitHub, GitLab (17.9+),
   Azure DevOps (MSAL device code).
2. **Брокер нужен — провайдер выдаёт секрет только вместе с кодом:** Bitbucket Cloud, Gitee,
   Assembla, Backlog, SourceHut.
3. **Только PAT — OAuth для desktop не документирован вообще:** Gogs, Gerrit, OneDev, Beanstalk,
   Launchpad, Phorge, CodeCommit.
4. **Не OAuth по природе:** Radicle (ключи, P2P), Hugging Face (токены; PR устроены как discussions).

Значит брокер — не «интеграция с Bitbucket», а общий компонент, который окупается на пяти хостах.

#### Аккаунт сайта

- [ ] Модель: пользователь, сессия, привязанные провайдеры, лимиты.
- [ ] Вход: magic link на email или OAuth-вход через GitHub (нужен почтовый провайдер —
      это отдельная зависимость, см. 2.6).
- [ ] Сессии: httpOnly + Secure + SameSite=Lax cookie, CSRF-токен на мутирующих запросах,
      отзыв всех сессий, срок жизни и ротация.
- [ ] Удаление аккаунта и всех привязанных данных одной кнопкой; экспорт данных.
- [ ] Ограничение попыток входа, блокировка перебора, лог подозрительных входов без секретов.

#### Брокер авторизации для приложения

- [ ] `POST /api/oauth/start` — сайт генерирует `state` + PKCE, редиректит на провайдера.
- [ ] `/api/oauth/callback` — обмен кода на токен серверным секретом, привязка аккаунта.
- [ ] Выдача приложению: одноразовый код, TTL ≤ 60 с, привязка к `state`, обмен на токен
      приложения; повторный обмен запрещён, старый код инвалидируется.
- [ ] Возврат в приложение: loopback `127.0.0.1:<port>` (RFC 8252) как основной путь,
      `git-chan://` deep link — только как запасной, с явным подтверждением.
- [ ] Никогда не отдавать провайдерский `refresh_token` в приложение, если сайт не проксирует
      API этого провайдера; иначе хранить зашифрованным и per-user.
- [ ] Отзыв: разрыв привязки на сайте, отзыв токена у провайдера, уведомление приложения.
- [ ] Ручной ввод PAT остаётся всегда — брокер это удобство, а не обязательный путь.

#### Матрица провайдеров (основной список M11.6)

`Секрет` — нужен ли `client_secret` на стороне приложения; `—` — OAuth не применяется вовсе.
Способ входа и особенности сверены по официальной документации, ссылки — в заметках ниже.

| Провайдер | PR/MR | Вход для desktop | Секрет | Доступ и особенности |
|---|---|---|---|---|
| GitHub | PR | device flow + OAuth App | нет | стабилен; device flow включается в настройках приложения |
| GitLab | MR | device flow (17.9+), иначе code + PKCE + loopback | нет | старые self-hosted без device flow |
| Bitbucket Cloud | PR | только authorization code | **да** → брокер | app passwords отключены, нужны API tokens |
| Azure DevOps | PR | Entra ID: device code (MSAL) | нет | legacy OAuth для новых закрыт с апреля 2025; MSA-аккаунты — отдельный случай |
| Gitea | PR | code + PKCE, public client | нет | self-hosted; OIDC |
| Forgejo / Codeberg | PR | code + PKCE, loopback `127.0.0.1` | нет | общий core с Gitea |
| Gitee | PR | code + secret; PAT как обход | **да** для OAuth | вход через GitHub проверен; API 1.5 с, git 2–8 с |
| Gogs | issue с `pull_request` | только token | — | OAuth2-провайдера нет; создание PR через API не документировано |
| OneDev | PR | token (Basic/Bearer); OIDC code + PKCE | нет | только self-hosted, API `/~api` |
| Assembla | MR | OAuth2 code + PIN-код | **да** → брокер | альтернатива: API key/secret пользователя |
| Beanstalk | code review | Basic: логин + API token | нет | PR-endpoint не подтверждён; API не менялся с 2015 |
| Backlog | PR | OAuth2 code + secret; либо API key | **да** для OAuth | база per-space: `<space>.backlog.com/api/v2` |
| Gerrit | changes | HTTP token (`/a/`), OAuth живёт на сервере | нет | модель changes/patch sets, не branch→branch |
| SourceHut | email-патчи | OAuth только confidential clients; PAT | **да** для OAuth | GraphQL по сервисам, не единый API |
| Launchpad | merge proposals | OAuth 1.0a, consumer key можно не прятать | нет | legacy-подпись, OAuth2 не подтверждён |
| Phorge / Phabricator | Differential revisions | Conduit token | нет | часть методов запрещает OAuth-клиентов; upstream мёртв с 2021 |
| Radicle | патчи в репозитории | нет OAuth, ключи Ed25519 | — | P2P, hosted API не унифицирован |
| CodeCommit | PR | SigV4 / credential helper | — | не OAuth; снова открыт для новых с 25.11.2025 |
| Hugging Face | discussions | токены; OAuth/OIDC с public app | нет | PR — это discussions, не GitHub-модель |

#### Заметки и источники

- **Bitbucket.** Только authorization code grant, implicit и password grants удалены; device
  flow не документирован. App passwords запрещены к созданию с 2025-09-09, существующие живут
  до 2026-06-09, замена — API tokens (Basic: email + token).
  <https://developer.atlassian.com/cloud/bitbucket/oauth-2>,
  <https://www.atlassian.com/blog/bitbucket/bitbucket-cloud-enters-phase-2-of-app-password-deprecation>.
- **GitLab.** PKCE рекомендуется, device grant появился в 17.2, по умолчанию в 17.3, GA в 17.9 —
  <https://docs.gitlab.com/api/oauth2>.
- **Gitea / Forgejo.** Только authorization code + PKCE/OIDC; public client с loopback-редиректом
  `127.0.0.1` и любым портом — <https://docs.gitea.com/development/oauth2-provider/>,
  <https://forgejo.org/docs/latest/user/oauth2-provider>.
- **GitHub.** Device flow для desktop/headless, `client_secret` не нужен, flow включается в
  настройках OAuth App — <https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps>.
- **Azure DevOps.** Entra ID + MSAL: authorization code для desktop и device code для public
  client; legacy Azure DevOps OAuth закрыт для новых регистраций с апреля 2025; на on-prem —
  PAT/Windows auth. <https://learn.microsoft.com/en-us/azure/devops/integrate/get-started/authentication/entra>.
- **OneDev.** Access tokens в Basic/Bearer, API `/~api`; сам может быть OIDC-провайдером с
  code + S256 PKCE и public clients, device flow нет —
  <https://docs.onedev.io/restful-api>, <https://docs.onedev.io/tutorials/security/openid-connect-provider>.
- **Gogs.** `/api/v1`, токен в `Authorization: token`; в документации нет OAuth2-провайдера, а PR
  отдаются как issues с ключом `pull_request` — <https://gogs.io/api-reference/introduction>.
- **Gerrit.** REST через `/a/` с HTTP-паролем/токеном (пароли устаревают в пользу токенов); OAuth —
  серверный плагин с PKCE; модель changes — <https://gerrit-review.googlesource.com/Documentation/rest-api.html>.
- **SourceHut.** GraphQL по каждому сервису, OAuth документирован только для confidential clients,
  то есть публичный desktop-клиент без секрета не проходит — <https://man.sr.ht/meta.sr.ht/oauth.md>.
- **Launchpad.** REST `api.launchpad.net/1.0`, OAuth 1.0a; по документации consumer key можно
  хардкодить, поэтому секрет в приложении не обязателен —
  <https://documentation.ubuntu.com/launchpad/user/how-to/launchpad-api/launchpad-web-signing/>.
- **Phorge.** Conduit + пользовательские API-токены; часть Differential-методов явно запрещает
  OAuth-клиентов; Phabricator без поддержки с 2021-06-01 — <https://phorge.it/>.
- **Assembla.** `api.assembla.com/v1`, OAuth2 authorization code и PIN-код для desktop, оба требуют
  client secret; есть MR-endpoints — <https://api-docs.assembla.cc/content/authentication.html>.
- **Beanstalk.** `<account>.beanstalkapp.com/api`, Basic с API token, OAuth нет; changelog API
  заканчивается 2015 — <https://api.beanstalkapp.com/introduction>.
- **Backlog** — продукт Nulab Inc. (Фукуока, Япония), а не отдельный проект: у Nulab ещё Cacoo,
  Nulab Pass и Nulab Flowbase. Backlog вышел в 2005-м, английская версия — в 2011-м. Встроены
  Git и SVN с pull request'ами и комментариями к коду, то есть в таблице он законно в строке «PR».
  База API зависит от домена пространства: `<space>.backlog.com/api/v2` для глобального,
  `<space>.backlog.jp/api/v2` для японского, старый `backlogtool.com` — тот же сервис.
  Аутентификация — API key или OAuth2 с секретом — <https://developer.nulab.com/docs/backlog/auth/>.
- **CodeCommit.** SigV4 и `git-remote-codecommit`; объявление о возврате к general availability
  от 25.11.2025 противоречит старой плашке на странице продукта — проверять в консоли AWS:
  <https://aws.amazon.com/blogs/devops/aws-codecommit-returns-to-general-availability>.
- **Hugging Face.** Токены для git; OAuth/OIDC-приложения, включая public app без секрета, scope
  `write-repos`/`write-discussions`; PR — discussions —
  <https://huggingface.co/docs/hub/en/oauth>, <https://huggingface.co/docs/hub/en/repositories-pull-requests-discussions>.
- **Radicle.** Идентичность на ключах, транспорт P2P, hosted API у каждого seed свой —
  <https://docs.radicle.xyz/guides/protocol>.

#### Gitee: доступность (измерено 2026-11-17)

Короткий ответ: **Gitee — не «только для Китая», но и не нормальный глобальный хостинг.**
Проверено с этой машины:

- `gitee.com` и `api/v5` доступны: после одного сорванного TLS-хендшейка 5 из 5 запросов
  вернули `200`. Блокировки нет, DNS отдаёт `180.76.19x.x` (Baidu CDN).
- Git по HTTPS работает: `git ls-remote` 2.4–3.6 с (3 из 3), поверхностный clone репозитория
  на 2.2 МБ — 7.7 с; почти всё время уходит на коннект, а не на данные.
- Веб-интерфейс медленный (страница 7.6–10.4 с), API ~1.5 с; первый коннект может упасть и
  требует retry. Сам Gitee советует зарубежным пользователям другой DNS и «ускоренный узел» —
  <https://help.gitee.com/questions/海外用户如何通过加速节点访问Gitee>.
- Анонимный API отдаёт `X-RateLimit-Limit: 60` — как у GitHub без токена.
- Вход из-за рубежа **проверен вручную 2026-11-17**: аккаунт создан и вход выполнен через
  GitHub, без телефона и без +86; на странице входа есть также Google и email, интерфейс
  переключается на английский. Регистрационный барьер снят — остаются латентность и стабильность.
- На ноябрь 2025 есть открытый issue «海外IP访问Gitee显示403», то есть фильтрация зарубежных IP
  встречается — <https://gitee.com/oschina/git-osc/issues/ID51Z0>.
- OAuth: только authorization code, `client_secret` обязателен, access token живёт сутки,
  PKCE и device flow не документированы. Значит для приложения — брокер либо PAT
  (`access_token` в query, есть в 236 из 264 операций) — <https://gitee.com/api/v5/oauth_doc>.
- Регистрация приложения — self-service, `gitee.com/oauth/applications/new`: Name, Description,
  Homepage URL, **одно** поле Callback, обязательный логотип (JPG/PNG ≤ 2 МБ — подойдёт готовый
  `packaging/icon.png`) и чекбоксы scopes. Ручного ревью или ожидания одобрения на форме нет —
  это подтверждает, что брокер можно поднять без переговоров с Gitee.
- Scopes у Gitee грубые («Full control of user pull requests»): клиенту достаточно `user_info`,
  `projects` и `pull_requests`; `notes` — только если делаем комментарии к review. Остальное
  (`keys`, `hook`, `groups`, `gists`, `enterprises`, `emails`) не просим — форма сама
  предупреждает, что от длинного списка пользователи отказываются.
- Callback — один точный адрес приложения, а не шаблон; по документации код привязан к паре
  `client_id` + `redirect_uri`. Значит случайный порт loopback (`127.0.0.1:54321`) так не
  зарегистрировать и это второй аргумент за брокер: адрес сайта фиксирован, а приложение
  получает код уже через одноразовый обмен. Поддержку custom scheme и порт-агностичного
  loopback надо проверить при реализации — в форме этого не видно.
- Self-hosted (Private Deploy) использует свой base URL `https://<domain>/api/v5`.

Вывод для плана: доступ и вход проверены, регистрация приложения self-service, API ~1.5 с,
git 2–8 с — адаптер делать можно. Позже — не по доступности, а по спросу и потому, что OAuth
требует брокера; медленный веб-интерфейс на адаптер не влияет. Уровень 1 (обычный git по URL)
работает и без адаптера.

- [ ] Проверить на живой форме: принимает ли Callback custom scheme (`git-chan://…`) или
      порт-агностичный loopback, или допускается только точный `https://` сайта.
- [ ] Создать приложение от имени сайта и прогнать code → token → `/user` → `/repos`: это
      единая проверка и брокера, и адаптера, без неё API-адаптер не начинать.

#### Bitbucket (продукт: 1.3)

- [ ] Не строить план на app passwords: для новых пользователей их больше нет.
- [ ] Основной путь — API token (Basic, email + token) в существующем encrypted store; это
      работает уже сейчас и не требует сайта.
- [ ] OAuth — второй путь через брокер; перед кодом выяснить, поддерживает ли Bitbucket PKCE:
      если нет, секрет всё равно остаётся на сервере, а приложение получает только токен.
- [ ] Проверить refresh token, срок жизни access token и что происходит при отзыве доступа
      в настройках Atlassian.
- [ ] Учесть, что Bitbucket Cloud и Bitbucket Data Center — разные API: Data Center в этот
      этап не входит.

### 2.3 Контент и документация

- [ ] `/download`: после публикации репозитория проверить прямые ссылки и размеры на живом
      релизе; до этого страница честно объясняет, что данных нет.
- [ ] Предупреждение про неподписанный macOS DMG и `xattr` — отдельным блоком на `/download`,
      а не только в гайде.
- [ ] `/changelog`: список релизов из GitHub API с датами и ссылками; без ручного дублирования.
- [ ] `SHA256SUMS`: показывать хэши и команду проверки на `/download`.
- [ ] Поиск по гайду (Pagefind или аналог) — 1000 строк уже тяжело листать.
- [ ] Синхронизация гайда: правило «гайд живёт только в `content/docs`», проверка в CI, что
      в репозитории нет второй копии.
- [ ] Страница `/install` с требованиями (JDK/Git), если гайд начнёт дублировать `/download`.

### 2.4 Деплой и эксплуатация

- [ ] Первый реальный деплой на VPS: Caddy + pm2, `pm2 startup`/`save`, проверка после ребута.
- [ ] Секреты только в env VPS (файл с правами 600, не в репозитории); список переменных —
      в `deploy/README` рядом с конфигами.
- [ ] Обновление: `git pull` → `npm ci` → `npm run build` → `pm2 reload`; одной командой/скриптом.
- [ ] Деплой из CI по тегу, а не руками, когда появится первый стабильный релиз сайта.
- [ ] Мониторинг: внешний пинг `/api/health`, алерт при падении, ротация логов pm2.
- [ ] Бэкап: база аккаунтов и квот (когда появится), дамп по расписанию + проверка восстановления.
- [ ] Заголовки: CSP без `unsafe-inline` после того, как уйдут инлайновые стили, HSTS уже есть.
- [ ] Staging-окружение на отдельном порту/поддомене, чтобы не проверять на проде.

### 2.5 Мелочи

- [ ] `sitemap.xml`, `robots.txt`, канонические URL.
- [ ] OG-картинки для лендинга, `/docs` и `/download`.
- [ ] Своя 404 с возвратом на главную.
- [ ] EN/RU для сайта (гайд пока один язык; решить, нужен ли перевод).
- [ ] Проверка Lighthouse на лендинге после появления картинок.

### 2.6 Внешние зависимости

- [ ] Почтовый провайдер для magic link и уведомлений (без него вход только через OAuth).
- [ ] Платёжный провайдер — только если появится биллинг прокси.
- [ ] Решение по хранению пользовательских данных: своя SQLite на VPS или managed-база.

## 3. Решения, которые надо принять до кода

1. **Аккаунт сайта = аккаунт приложения?** Если да, приложение хранит только токен сайта и не
   знает провайдерских секретов. Если нет, брокер выдаёт токен провайдера, а сайт остаётся
   только посредником на время входа.
2. **Хранит ли сайт провайдерские токены?** Нужно только если сайт проксирует API провайдера
   (например, создание PR). Для прокси ИИ — не нужно.
3. **Публичность репозитория.** Пока `Baconana-chan/MyBeautifulGit-chan` приватный, `/download`
   и ссылки на GitHub отдают 404 анонимно, а API не возвращает релиз.
4. **Платим ли мы за прокси из своего кармана** и до какого потолка.

## 4. Риски

- Обещали OAuth там, где провайдер его фактически не даёт для desktop (Gogs, Launchpad,
  SourceHut, Gerrit) — формулировать как «ревью/PAT», а не как план.
- Секрет OAuth-приложения, попавший в клиент, — утечка на весь продукт; никогда не класть его
  в приложение, даже «временно». Единственное исключение — Launchpad с OAuth 1.0a и хардкодом
  consumer key.
- Прокси ИИ без квот — счёт за чужое любопытство; квоты и потолок раньше публичного запуска.
- Приватный репозиторий + публичный сайт = мёртвые ссылки; сначала видимость, потом промо.
- pm2 без `startup`/`save` тихо не переживает ребут VPS.
- Gitee: доступ работает (проверено), API и git в норме, но бывают сорванные первые коннекты и
  403 для части зарубежных IP — предусмотреть retry и не строить на Gitee демо.

## 5. Порядок

1. Публичность репозитория и живой релиз с пакетами — проверить `/download` на реальных файлах.
2. Аккаунт сайта + сессии (без OAuth) — фундамент для всего остального.
3. Device flow напрямую в приложении: GitHub, затем GitLab (с fallback на PKCE + loopback).
4. Брокер: сначала GitHub/GitLab как проверка механики, затем Gitea/Forgejo (PKCE напрямую,
   брокер им не нужен — проверить, что он не мешает).
5. Bitbucket: API token (уже возможен) → брокер OAuth после ревью PKCE.
6. Прокси ИИ после аккаунтов и квот; только тогда, когда есть чем считать и что выключать.
7. Остальные хосты — по спросу, по одному, с ревью flow до кода; Gitee и legacy-хосты
   (Assembla, Beanstalk, Launchpad) — в конце.
