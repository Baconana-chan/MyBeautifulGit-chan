package org.baconana.mybeautifulgit_chan.app

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.baconana.mybeautifulgit_chan.domain.Repository
import org.baconana.mybeautifulgit_chan.domain.RepositoryMetadataException
import org.baconana.mybeautifulgit_chan.domain.GitAuth
import org.baconana.mybeautifulgit_chan.domain.UpdateChannel
import org.baconana.mybeautifulgit_chan.domain.cloneArgs
import org.baconana.mybeautifulgit_chan.domain.initArgs
import org.baconana.mybeautifulgit_chan.domain.validCloneTarget
import org.baconana.mybeautifulgit_chan.domain.validCloneUrl
import org.baconana.mybeautifulgit_chan.domain.validInitBranch
import org.baconana.mybeautifulgit_chan.git.CommandShellPreference
import org.baconana.mybeautifulgit_chan.git.GitCommandException
import org.baconana.mybeautifulgit_chan.git.GitVersion
import org.baconana.mybeautifulgit_chan.git.currentShellFlavor
import org.baconana.mybeautifulgit_chan.git.cli.CliGitBackend
import org.baconana.mybeautifulgit_chan.git.cli.GitDiscovery
import org.baconana.mybeautifulgit_chan.git.cli.GitInstallOption
import org.baconana.mybeautifulgit_chan.git.cli.GitLocator
import org.baconana.mybeautifulgit_chan.git.cli.GitStatus
import org.baconana.mybeautifulgit_chan.i18n.Language
import org.baconana.mybeautifulgit_chan.platform.AppSettings
import org.baconana.mybeautifulgit_chan.ai.AiSettings
import org.baconana.mybeautifulgit_chan.platform.AppPaths
import org.baconana.mybeautifulgit_chan.platform.CredentialStore
import org.baconana.mybeautifulgit_chan.platform.DesktopActions
import org.baconana.mybeautifulgit_chan.platform.DirectoryPickerMode
import org.baconana.mybeautifulgit_chan.platform.FileDialogs
import org.baconana.mybeautifulgit_chan.platform.RecentRepos
import org.baconana.mybeautifulgit_chan.platform.RepositorySessionStore
import org.baconana.mybeautifulgit_chan.ui.mockup.FontChoice
import org.baconana.mybeautifulgit_chan.ui.mockup.A11yFocusIndication
import org.baconana.mybeautifulgit_chan.ui.mockup.Term
import org.baconana.mybeautifulgit_chan.ui.mockup.ThemeMode
import java.io.File

data class GitRuntime(
    val executable: String,
    val version: GitVersion,
    val backend: CliGitBackend,
)

private enum class StartDialog { Clone, Init }

private data class DirectoryPickRequest(
    val title: String,
    val initial: String,
    val confirmLabel: String,
    val pathPlaceholder: String,
    val hint: String?,
    val resolveRepository: Boolean,
    val showRecent: Boolean,
    val onChosen: (String) -> Unit,
)

@Composable
fun DesktopApp(
    zoom: ZoomController,
    commandPalette: CommandPaletteController,
    showCustomTitleBar: Boolean = true,
    titleBar: @Composable (String, (Int) -> Unit) -> Unit,
    onQuit: () -> Unit,
    droppedPath: String? = null,
    onDropConsumed: () -> Unit = {},
    onOpenWorktreeWindow: (GitRuntime, String, Language) -> Unit = { _, _, _ -> },
) {
    val scope = rememberCoroutineScope()
    val recentRepos = remember { RecentRepos() }
    val sessionStore = remember { RepositorySessionStore() }
    val sessionSaveMutex = remember { Mutex() }
    val settings = remember { AppSettings() }
    val credentials = remember { CredentialStore() }
    val aiCredentials = remember {
        CredentialStore(AppPaths.aiCredentialsFile(), AppPaths.aiCredentialKeyFile())
    }
    var discovery by remember { mutableStateOf<GitDiscovery?>(null) }
    var booting by remember { mutableStateOf(true) }
    var manualPath by remember { mutableStateOf(settings.loadGitPath()) }
    var checking by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    var installError by remember { mutableStateOf<String?>(null) }
    var dismissedGitVersion by remember { mutableStateOf(settings.loadDismissedGitVersion()) }
    var updateDialogOpen by remember { mutableStateOf(false) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    var openError by remember { mutableStateOf<String?>(null) }
    var openRepositories by remember { mutableStateOf<List<Repository>>(emptyList()) }
    var activeRepositoryRoot by remember { mutableStateOf<String?>(null) }
    var sessionRestored by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<StartDialog?>(null) }
    val homeDirectory = remember { DirectoryBrowser.homeDirectory() }
    var cloneTarget by remember { mutableStateOf(homeDirectory) }
    var initPath by remember { mutableStateOf(File(homeDirectory, "new-repository").path) }
    var language by remember {
        mutableStateOf(
            settings.loadLanguage(
                Language.fromCode(System.getProperty("user.language")),
            ),
        )
    }
    val strings = language.strings
    var pickerMode by remember { mutableStateOf(settings.loadPickerMode()) }
    var themeMode by remember { mutableStateOf(settings.loadTheme()) }
    var fontChoice by remember { mutableStateOf(settings.loadFont()) }
    var highContrast by remember { mutableStateOf(settings.loadHighContrast()) }
    var gitPath by remember { mutableStateOf(settings.loadGitPath()) }
    var defaultBranch by remember { mutableStateOf(settings.loadDefaultBranch()) }
    var copyShell by remember { mutableStateOf(settings.loadCopyShell()) }
    var closeToTray by remember { mutableStateOf(settings.loadCloseToTray()) }
    var startAtLogin by remember { mutableStateOf(settings.loadStartAtLogin()) }
    var updateChannel by remember { mutableStateOf(settings.loadUpdateChannel()) }
    val effectiveCopyShell = copyShell ?: currentShellFlavor()
    remember(effectiveCopyShell) {
        CommandShellPreference.flavor = effectiveCopyShell
        effectiveCopyShell
    }
    var authUsername by remember { mutableStateOf(settings.loadAuthUsername()) }
    var authToken by remember { mutableStateOf(credentials.loadToken().orEmpty()) }
    var aiSettings by remember { mutableStateOf(settings.loadAiSettings()) }
    var aiApiKey by remember { mutableStateOf(aiCredentials.loadToken().orEmpty()) }
    var credentialHelper by remember { mutableStateOf(settings.loadCredentialHelper()) }
    var sshMode by remember { mutableStateOf(settings.loadSshMode()) }
    var sshKeyPath by remember { mutableStateOf(settings.loadSshKeyPath()) }
    var settingsOpen by remember { mutableStateOf(false) }
    var directoryRequest by remember { mutableStateOf<DirectoryPickRequest?>(null) }
    val focusManager = LocalFocusManager.current
    val paletteRecent = recent
    val effectiveActiveRoot = openRepositories
        .firstOrNull { sameRepositoryRoot(it.root, activeRepositoryRoot) }
        ?.root
        ?: openRepositories.firstOrNull()?.root
    val latestRepositories by rememberUpdatedState(openRepositories)
    val latestActiveRoot by rememberUpdatedState(activeRepositoryRoot)
    val latestSessionRestored by rememberUpdatedState(sessionRestored)

    DisposableEffect(Unit) {
        onDispose {
            if (latestSessionRestored) {
                runCatching {
                    sessionStore.save(latestRepositories.map { it.root }, latestActiveRoot)
                }
            }
        }
    }

    val baseDensity = LocalDensity.current

    val runtime = remember(discovery) {
        discovery
            ?.takeIf { it.status == GitStatus.READY }
            ?.installation
            ?.let {
                val auth = GitAuth(
                    username = authUsername,
                    token = authToken,
                    credentialHelper = credentialHelper,
                    sshMode = sshMode,
                    sshKeyPath = sshKeyPath,
                )
                GitRuntime(it.executable, it.version, CliGitBackend(it.executable, auth))
            }
    }
    val repositoryCommandsEnabled = runtime != null && sessionRestored
    val repositoryCommandsDisabledReason = when {
        runtime == null -> "Git is not ready"
        !sessionRestored -> "Restoring the previous session"
        else -> null
    }
    val installOptions = remember(strings) {
        runCatching { GitLocator(strings = strings).installOptions() }.getOrDefault(emptyList())
    }
    val updateOptions = remember(strings) {
        runCatching { GitLocator(strings = strings).updateOptions() }.getOrDefault(emptyList())
    }

    fun applyDiscovery(result: GitDiscovery) {
        discovery = result
        booting = false
        if (result.status == GitStatus.READY) {
            recent = runCatching { recentRepos.load() }.getOrDefault(emptyList())
        }
    }

    fun activateRepository(repository: Repository) {
        activeRepositoryRoot = repository.root
        openError = null
        commandPalette.close()
        focusManager.clearFocus(force = true)
    }

    fun closeRepository(root: String) {
        val index = openRepositories.indexOfFirst { sameRepositoryRoot(it.root, root) }
        if (index < 0) return
        val wasActive = sameRepositoryRoot(activeRepositoryRoot, root)
        val remaining = openRepositories.filterIndexed { currentIndex, _ -> currentIndex != index }
        openRepositories = remaining
        if (wasActive) {
            activeRepositoryRoot = remaining.getOrNull(index)?.root ?: remaining.lastOrNull()?.root
            commandPalette.close()
            focusManager.clearFocus(force = true)
        }
    }

    suspend fun openPath(path: String): Boolean {
        val current = runtime ?: return false
        val opened = withContext(Dispatchers.IO) {
            runCatching { current.backend.open(path) }
        }
        return opened.fold(
            onSuccess = { repo ->
                val existing = openRepositories.firstOrNull {
                    sameRepositoryRoot(it.root, repo.root)
                }
                if (existing == null) openRepositories = openRepositories + repo
                activeRepositoryRoot = existing?.root ?: repo.root
                openError = null
                recent = runCatching { recentRepos.add(repo.root) }.getOrDefault(recent)
                commandPalette.close()
                focusManager.clearFocus(force = true)
                true
            },
            onFailure = {
                openError = when (it) {
                    is RepositoryMetadataException -> strings.repoOpenMetadataBroken(
                        it.repositoryPath,
                        it.missingMetadata.joinToString(", "),
                    )
                    else -> it.message ?: it.toString()
                }
                false
            },
        )
    }

    fun checkManualGit() {
        if (checking) return
        checking = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    GitLocator(strings = strings).discover(manualPath.trim().ifBlank { null })
                }.getOrElse {
                    GitDiscovery(GitStatus.NOT_FOUND, problem = it.message ?: it.toString())
                }
            }
            applyDiscovery(result)
            checking = false
        }
    }

    fun installGit(option: GitInstallOption, onReady: () -> Unit = {}) {
        if (installing) return
        installing = true
        installError = null
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                GitInstaller.install(option)
            }
            if (!outcome.ok) {
                installError = buildString {
                    append(strings.setupInstallFailed)
                    if (outcome.output.isNotBlank()) {
                        append("\n").append(outcome.output.take(1_600))
                    }
                }
                installing = false
                return@launch
            }

            val discovered = withContext(Dispatchers.IO) {
                runCatching { GitLocator(strings = strings).discover() }.getOrElse {
                    GitDiscovery(GitStatus.NOT_FOUND, problem = it.message ?: it.toString())
                }
            }
            applyDiscovery(discovered)
            if (discovered.status == GitStatus.READY) {
                val installedExecutable = discovered.installation?.executable
                if (installedExecutable != null &&
                    manualPath.isNotBlank() &&
                    manualPath != installedExecutable
                ) {
                    manualPath = installedExecutable
                    gitPath = installedExecutable
                    settings.saveGitPath(installedExecutable)
                }
                onReady()
            } else {
                installError = strings.setupInstallNotDetected
            }
            installing = false
        }
    }

    fun setPickerMode(mode: DirectoryPickerMode) {
        pickerMode = mode
        settings.savePickerMode(mode)
    }

    fun setLanguage(next: Language) {
        if (next == language) return
        language = next
        settings.saveLanguage(next)
    }

    fun saveSettings(values: SettingsValues) {
        val previousGitPath = gitPath
        settings.saveTheme(values.theme)
        themeMode = values.theme
        settings.saveFont(values.font)
        fontChoice = values.font
        settings.saveHighContrast(values.highContrast)
        highContrast = values.highContrast
        settings.saveGitPath(values.gitPath)
        gitPath = values.gitPath
        settings.saveDefaultBranch(values.defaultBranch)
        defaultBranch = values.defaultBranch
        settings.saveCopyShell(values.copyShell)
        copyShell = values.copyShell
        settings.saveCloseToTray(values.closeToTray)
        closeToTray = values.closeToTray
        settings.saveStartAtLogin(values.startAtLogin)
        startAtLogin = values.startAtLogin
        settings.saveUpdateChannel(values.updateChannel)
        updateChannel = values.updateChannel
        val nextAiSettings = AiSettings(
            enabled = values.aiEnabled,
            endpoint = values.aiEndpoint,
            model = values.aiModel,
            messageLanguage = values.aiMessageLanguage,
            style = values.aiStyle,
            maxDiffChars = values.aiMaxDiffChars,
            temperature = values.aiTemperature,
            allowLocalHttp = values.aiAllowLocalHttp,
        )
        settings.saveAiSettings(nextAiSettings)
        aiSettings = nextAiSettings
        runCatching { aiCredentials.saveToken(values.aiApiKey.ifBlank { null }) }
        aiApiKey = values.aiApiKey
        CommandShellPreference.flavor = values.copyShell ?: currentShellFlavor()
        runCatching { credentials.saveToken(values.authToken.ifBlank { null }) }
        settings.saveAuthUsername(values.authUsername)
        settings.saveCredentialHelper(values.credentialHelper)
        settings.saveSshMode(values.sshMode)
        settings.saveSshKeyPath(values.sshKeyPath)
        authUsername = values.authUsername
        authToken = values.authToken
        credentialHelper = values.credentialHelper
        sshMode = values.sshMode
        sshKeyPath = values.sshKeyPath
        runtime?.backend?.auth = GitAuth(
            username = values.authUsername,
            token = values.authToken,
            credentialHelper = values.credentialHelper,
            sshMode = values.sshMode,
            sshKeyPath = values.sshKeyPath,
        )
        setPickerMode(values.pickerMode)
        setLanguage(values.language)
        manualPath = values.gitPath
        settingsOpen = false
        if (previousGitPath != values.gitPath) checkManualGit()
    }

    fun pickerStart(path: String): String {
        val file = File(path)
        val parent = file.parentFile
        return when {
            file.isDirectory -> DirectoryBrowser.normalize(file.path)
            parent?.isDirectory == true -> DirectoryBrowser.normalize(parent.path)
            else -> homeDirectory
        }
    }

    fun openDirectoryPicker(request: DirectoryPickRequest) {
        if (pickerMode == DirectoryPickerMode.Custom) {
            directoryRequest = request
        } else {
            scope.launch {
                FileDialogs.chooseDirectorySystem(request.title, request.initial, strings.open)
                    ?.let(request.onChosen)
            }
        }
    }

    fun chooseRepository() {
        openDirectoryPicker(
            DirectoryPickRequest(
                title = "OPEN GIT REPOSITORY",
                initial = recent.firstOrNull()?.let { File(it).parent } ?: homeDirectory,
                confirmLabel = "OPEN",
                pathPlaceholder = strings.pickerPathRepo,
                hint = strings.pickerRepoNotFound,
                resolveRepository = true,
                showRecent = true,
                onChosen = { path -> scope.launch { openPath(path) } },
            ),
        )
    }

    fun browseCloneTarget() {
        openDirectoryPicker(
            DirectoryPickRequest(
                title = "CLONE TARGET",
                initial = pickerStart(cloneTarget),
                confirmLabel = "SELECT",
                pathPlaceholder = strings.pickerPathTarget,
                hint = null,
                resolveRepository = false,
                showRecent = false,
                onChosen = { path -> cloneTarget = path },
            ),
        )
    }

    fun browseInitPath() {
        openDirectoryPicker(
            DirectoryPickRequest(
                title = "INIT REPOSITORY",
                initial = pickerStart(initPath),
                confirmLabel = "SELECT",
                pathPlaceholder = strings.pickerPathInit,
                hint = null,
                resolveRepository = false,
                showRecent = false,
                onChosen = { path -> initPath = path },
            ),
        )
    }

    LaunchedEffect(openRepositories, activeRepositoryRoot) {
        if (openRepositories.isEmpty()) {
            activeRepositoryRoot = null
            commandPalette.close()
            commandPalette.clearScreenCommands()
        } else if (openRepositories.none { sameRepositoryRoot(it.root, activeRepositoryRoot) }) {
            activeRepositoryRoot = openRepositories.first().root
        }
    }

    SideEffect {
        commandPalette.setBaseCommands(
            buildList {
                add(
                    PaletteCommand(
                        id = "app.settings",
                        title = "Open settings",
                        category = "APPLICATION",
                        description = "Theme, font, Git path, AI, authentication and language",
                        keywords = "preferences configuration настройки",
                    ) { settingsOpen = true },
                )
                add(
                    PaletteCommand(
                        id = "app.open-repository",
                        title = "Open repository",
                        category = "APPLICATION",
                        description = "Choose an existing Git repository and open it in a new tab",
                        keywords = "folder repo open tab",
                        enabled = repositoryCommandsEnabled,
                        disabledReason = repositoryCommandsDisabledReason,
                    ) { chooseRepository() },
                )
                paletteRecent.take(5).forEachIndexed { index, path ->
                    val name = runCatching { File(path).name }.getOrDefault(path)
                    add(
                        PaletteCommand(
                            id = "app.recent.$index",
                            title = "Open recent: $name",
                            category = "RECENT",
                            description = path,
                            command = path,
                            keywords = "recent repository reopen tab",
                            enabled = repositoryCommandsEnabled,
                            disabledReason = repositoryCommandsDisabledReason,
                        ) { scope.launch { openPath(path) } },
                    )
                }
                add(
                    PaletteCommand(
                        id = "app.clone",
                        title = "Clone repository",
                        category = "APPLICATION",
                        description = "Clone a remote repository",
                        command = "git clone <url> <path>",
                        keywords = "remote download",
                        enabled = repositoryCommandsEnabled,
                        disabledReason = repositoryCommandsDisabledReason,
                    ) {
                        cloneTarget = homeDirectory
                        dialog = StartDialog.Clone
                    },
                )
                add(
                    PaletteCommand(
                        id = "app.init",
                        title = "Create repository",
                        category = "APPLICATION",
                        description = "Initialize a new repository with git init",
                        command = "git init -b <branch> <path>",
                        keywords = "new init create",
                        enabled = repositoryCommandsEnabled,
                        disabledReason = repositoryCommandsDisabledReason,
                    ) {
                        initPath = File(homeDirectory, "new-repository").path
                        dialog = StartDialog.Init
                    },
                )
                Language.entries.forEach { next ->
                    if (next == language) return@forEach
                    add(
                        PaletteCommand(
                            id = "app.language.${next.name.lowercase()}",
                            title = "Language: ${next.label}",
                            category = "APPLICATION",
                            description = "Switch the interface language without leaving the workspace",
                            keywords = "locale translation язык",
                        ) { setLanguage(next) },
                    )
                }
                add(
                    PaletteCommand(
                        id = "app.zoom-in",
                        title = "Zoom in",
                        category = "VIEW",
                        description = "Increase the interface scale",
                        shortcut = "Ctrl+=",
                        keywords = "scale larger ui",
                    ) { zoom.step(1) },
                )
                add(
                    PaletteCommand(
                        id = "app.zoom-out",
                        title = "Zoom out",
                        category = "VIEW",
                        description = "Decrease the interface scale",
                        shortcut = "Ctrl+-",
                        keywords = "scale smaller ui",
                    ) { zoom.step(-1) },
                )
                add(
                    PaletteCommand(
                        id = "app.zoom-reset",
                        title = "Reset zoom",
                        category = "VIEW",
                        description = "Return the interface scale to 100%",
                        shortcut = "Ctrl+0",
                        keywords = "scale default ui",
                    ) { zoom.reset() },
                )
                add(
                    PaletteCommand(
                        id = "app.high-contrast",
                        title = if (highContrast) "Disable high contrast" else "Enable high contrast",
                        category = "VIEW",
                        description = "Use stronger text and control-border contrast",
                        keywords = "accessibility contrast low vision",
                    ) {
                        highContrast = !highContrast
                        settings.saveHighContrast(highContrast)
                    },
                )
                add(
                    PaletteCommand(
                        id = "app.quit",
                        title = "Quit application",
                        category = "APPLICATION",
                        description = "Close all windows and exit",
                        danger = true,
                        keywords = "exit close",
                    ) { onQuit() },
                )
            },
        )
    }

    LaunchedEffect(Unit) {
        val result = withContext(Dispatchers.IO) {
            runCatching {
                GitLocator(strings = strings).discover(manualPath.trim().ifBlank { null })
            }.getOrElse {
                GitDiscovery(GitStatus.NOT_FOUND, problem = it.message ?: it.toString())
            }
        }
        applyDiscovery(result)
    }

    LaunchedEffect(runtime) {
        val current = runtime ?: return@LaunchedEffect
        if (sessionRestored) return@LaunchedEffect
        val session = withContext(Dispatchers.IO) {
            runCatching { sessionStore.load() }.getOrNull()
        }
        if (session != null && session.repositories.isNotEmpty() && openRepositories.isEmpty()) {
            val opened = withContext(Dispatchers.IO) {
                session.repositories.mapNotNull { path ->
                    runCatching { current.backend.open(path) }.getOrNull()
                }
            }
            val restored = mutableListOf<Repository>()
            opened.forEach { repository ->
                if (restored.none { sameRepositoryRoot(it.root, repository.root) }) {
                    restored += repository
                }
            }
            if (restored.isNotEmpty()) {
                openRepositories = restored
                activeRepositoryRoot = restored
                    .firstOrNull { sameRepositoryRoot(it.root, session.active) }
                    ?.root
                    ?: restored.first().root
            }
        }
        sessionRestored = true
    }

    LaunchedEffect(openRepositories, activeRepositoryRoot, sessionRestored) {
        if (!sessionRestored) return@LaunchedEffect
        val paths = openRepositories.map { it.root }
        sessionSaveMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching { sessionStore.save(paths, activeRepositoryRoot) }
            }
        }
    }

    LaunchedEffect(themeMode, fontChoice, highContrast) {
        val systemDark = if (themeMode == ThemeMode.System) {
            withContext(Dispatchers.IO) { PlatformTheme.isDark() }
        } else {
            true
        }
        Term.apply(themeMode, fontChoice, systemDark, highContrast)
    }

    // Если язык переключили на экране установки, перечитываем discovery,
    // чтобы problem и подсказки были на выбранном языке.
    LaunchedEffect(language) {
        if (booting || runtime != null) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            runCatching {
                GitLocator(strings = strings).discover(manualPath.trim().ifBlank { null })
            }.getOrElse {
                GitDiscovery(GitStatus.NOT_FOUND, problem = it.message ?: it.toString())
            }
        }
        discovery = result
    }

    LaunchedEffect(droppedPath, runtime, sessionRestored) {
        val path = droppedPath ?: return@LaunchedEffect
        if (runtime != null && sessionRestored) {
            openPath(path)
            onDropConsumed()
        }
    }

    Box(Modifier.fillMaxSize().background(Term.bg)) {
        CompositionLocalProvider(
            LocalStrings provides strings,
            LocalIndication provides A11yFocusIndication,
            LocalDensity provides Density(
                density = baseDensity.density * zoom.scale,
                fontScale = baseDensity.fontScale,
            ),
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    if (showCustomTitleBar) {
                        titleBar(zoom.label) { delta -> zoom.step(delta) }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (settingsOpen) {
                            SettingsScreen(
                                initial = SettingsValues(
                                    theme = themeMode,
                                    font = fontChoice,
                                    highContrast = highContrast,
                                    gitPath = gitPath,
                                    defaultBranch = defaultBranch,
                                    copyShell = copyShell,
                                    closeToTray = closeToTray,
                                    startAtLogin = startAtLogin,
                                    updateChannel = updateChannel,
                                    pickerMode = pickerMode,
                                    language = language,
                                    authUsername = authUsername,
                                    authToken = authToken,
                                    credentialHelper = credentialHelper,
                                    sshMode = sshMode,
                                    sshKeyPath = sshKeyPath,
                                    aiEnabled = aiSettings.enabled,
                                    aiEndpoint = aiSettings.endpoint,
                                    aiModel = aiSettings.model,
                                    aiApiKey = aiApiKey,
                                    aiMessageLanguage = aiSettings.messageLanguage,
                                    aiStyle = aiSettings.style,
                                    aiMaxDiffChars = aiSettings.maxDiffChars,
                                    aiTemperature = aiSettings.temperature,
                                    aiAllowLocalHttp = aiSettings.allowLocalHttp,
                                ),
                                onCancel = { settingsOpen = false },
                                onSave = { saveSettings(it) },
                            )
                        } else when {
                            booting -> BootScreen()

                            runtime == null -> {
                                val current = discovery
                                if (current != null) {
                                    GitSetupScreen(
                                        discovery = current,
                                        installOptions = installOptions,
                                        manualPath = manualPath,
                                        onManualPathChange = { manualPath = it },
                                        onCheck = { checkManualGit() },
                                        checking = checking,
                                        installing = installing,
                                        installError = installError,
                                        onInstall = { installGit(it) },
                                        onDismissInstallError = { installError = null },
                                        onOpenUrl = { DesktopActions.openUrl(it) },
                                        onCopy = { DesktopActions.copy(it) },
                                        language = language,
                                        onLanguageChange = { setLanguage(it) },
                                        onOpenSettings = { settingsOpen = true },
                                    )
                                } else {
                                    BootScreen()
                                }
                            }

                            !sessionRestored -> BootScreen(
                                title = "RESTORING SESSION",
                                detail = "reopening previous repository tabs",
                            )

                            openRepositories.isNotEmpty() -> Column(Modifier.fillMaxSize()) {
                                RepositoryTabsBar(
                                    repositories = openRepositories,
                                    activeRoot = effectiveActiveRoot,
                                    onSelect = { repository -> activateRepository(repository) },
                                    onClose = { repository -> closeRepository(repository.root) },
                                    onOpen = { chooseRepository() },
                                )
                                openError?.let { message ->
                                    ErrorBanner(
                                        text = message,
                                        onDismiss = { openError = null },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    openRepositories.forEach { repository ->
                                        val active = sameRepositoryRoot(
                                            repository.root,
                                            effectiveActiveRoot,
                                        )
                                        key(repository.root) {
                                            val hostModifier = if (active) {
                                                Modifier.fillMaxSize()
                                            } else {
                                                Modifier.size(0.dp).clearAndSetSemantics { }
                                            }
                                            Box(hostModifier) {
                                                RepoHost(
                                                    runtime = runtime,
                                                    repository = repository,
                                                    commandPalette = commandPalette,
                                                    active = active,
                                                    onClose = { closeRepository(repository.root) },
                                                    language = language,
                                                    onLanguageChange = { setLanguage(it) },
                                                    onOpenWorktree = { path ->
                                                        scope.launch { openPath(path) }
                                                    },
                                                    onOpenWorktreeWindow = { path ->
                                                        onOpenWorktreeWindow(runtime, path, language)
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                val current = discovery
                                if (current != null) {
                                    WelcomeScreen(
                                        runtime = runtime,
                                        discovery = current,
                                        recent = recent,
                                        errorText = openError,
                                        onDismissError = { openError = null },
                                        onOpen = { chooseRepository() },
                                        onOpenRecent = { path ->
                                            scope.launch { openPath(path) }
                                        },
                                        onForgetRecent = { path ->
                                            recent = runCatching { recentRepos.remove(path) }
                                                .getOrDefault(recent)
                                        },
                                        onClone = {
                                            cloneTarget = homeDirectory
                                            dialog = StartDialog.Clone
                                        },
                                        onInit = {
                                            initPath = File(homeDirectory, "new-repository").path
                                            dialog = StartDialog.Init
                                        },
                                        pickerMode = pickerMode,
                                        onTogglePicker = {
                                            setPickerMode(
                                                if (pickerMode == DirectoryPickerMode.Custom) {
                                                    DirectoryPickerMode.System
                                                } else {
                                                    DirectoryPickerMode.Custom
                                                },
                                            )
                                        },
                                        language = language,
                                        onLanguageChange = { setLanguage(it) },
                                        onOpenSettings = { settingsOpen = true },
                                        updateReminderDismissed =
                                            dismissedGitVersion == runtime.version.key,
                                        onUpdateGit = {
                                            installError = null
                                            updateDialogOpen = true
                                        },
                                        onDismissUpdateReminder = {
                                            dismissedGitVersion = runtime.version.key
                                            settings.saveDismissedGitVersion(runtime.version.key)
                                        },
                                        onQuit = onQuit,
                                    )
                                } else {
                                    BootScreen()
                                }
                            }
                        }
                    }
                }

                dialog?.let { current ->
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (current) {
                            StartDialog.Clone -> {
                                val rt = runtime
                                if (rt != null) {
                                    CloneDialog(
                                        target = cloneTarget,
                                        onTargetChange = { cloneTarget = it },
                                        onBrowse = { browseCloneTarget() },
                                        onDismiss = { dialog = null },
                                        onClone = { url, target, filter, control ->
                                            withContext(Dispatchers.IO) {
                                                require(validCloneUrl(url) && validCloneTarget(target)) {
                                                    "unsafe clone arguments"
                                                }
                                                rt.backend.runControlled(
                                                    cwd = null,
                                                    args = cloneArgs(url.trim(), target.trim(), filter),
                                                    timeoutSeconds = 900,
                                                    control = control,
                                                )
                                            }
                                        },
                                        onOpened = { path ->
                                            dialog = null
                                            scope.launch { openPath(path) }
                                        },
                                    )
                                }
                            }

                            StartDialog.Init -> {
                                val rt = runtime
                                if (rt != null) {
                                    InitDialog(
                                        path = initPath,
                                        defaultBranch = defaultBranch,
                                        onPathChange = { initPath = it },
                                        onBrowse = { browseInitPath() },
                                        onDismiss = { dialog = null },
                                        onInit = { path ->
                                            withContext(Dispatchers.IO) {
                                                val cleanPath = path.trim()
                                                val branch = defaultBranch.trim().ifBlank { "main" }
                                                if (!validCloneTarget(path) || !validInitBranch(branch)) {
                                                    Result.failure(IllegalArgumentException("unsafe init arguments"))
                                                } else {
                                                    val result = rt.backend.run(
                                                        cwd = null,
                                                        args = initArgs(branch, cleanPath),
                                                        timeoutSeconds = 60,
                                                    )
                                                    if (result.ok) {
                                                        Result.success(cleanPath)
                                                    } else {
                                                        Result.failure(GitCommandException(result))
                                                    }
                                                }
                                            }
                                        },
                                        onOpened = { path ->
                                            dialog = null
                                            scope.launch { openPath(path) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (updateDialogOpen) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        GitUpdateDialog(
                            currentVersion = runtime?.version?.key.orEmpty(),
                            options = updateOptions,
                            installing = installing,
                            installError = installError,
                            onInstall = { option ->
                                installGit(option) { updateDialogOpen = false }
                            },
                            onOpenUrl = { DesktopActions.openUrl(it) },
                            onCopy = { DesktopActions.copy(it) },
                            onDismissInstallError = { installError = null },
                            onDismiss = { if (!installing) updateDialogOpen = false },
                        )
                    }
                }

                val request = directoryRequest
                if (request != null) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.74f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        DirectoryPickerDialog(
                            title = request.title,
                            initial = request.initial,
                            recent = recent,
                            confirmLabel = request.confirmLabel,
                            pathPlaceholder = request.pathPlaceholder,
                            hint = request.hint,
                            resolveRepository = request.resolveRepository,
                            showRecent = request.showRecent,
                            onDismiss = { directoryRequest = null },
                            onChoose = { path ->
                                directoryRequest = null
                                request.onChosen(path)
                            },
                            onUseSystem = { path ->
                                directoryRequest = null
                                setPickerMode(DirectoryPickerMode.System)
                                scope.launch {
                                    FileDialogs.chooseDirectorySystem(
                                        request.title,
                                        path,
                                        strings.open,
                                    )?.let(request.onChosen)
                                }
                            },
                        )
                    }
                }

                CommandPaletteHost(commandPalette)
            }
        }
    }
}

@Composable
private fun BootScreen(
    title: String = "SCANNING FOR GIT",
    detail: String = "PATH → standard locations → git --version",
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                fontSize = 12.sp,
                fontFamily = Term.mono,
                fontWeight = FontWeight.Bold,
                color = Term.green,
                letterSpacing = 2.sp,
            )
            Text(
                detail,
                fontSize = 9.sp,
                fontFamily = Term.mono,
                color = Term.faint,
            )
        }
    }
}
