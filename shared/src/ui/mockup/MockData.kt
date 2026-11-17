package org.baconana.mybeautifulgit_chan.ui.mockup

enum class RefKind { Head, Branch, Remote, Tag }

data class MockRef(val label: String, val kind: RefKind)

data class MockCommit(
    val shortHash: String,
    val message: String,
    val author: String,
    val email: String,
    val age: String,
    val date: String,
    val lane: Int,
    val lanes: List<Int>,
    val mergeLanes: List<Int> = emptyList(),
    val refs: List<MockRef> = emptyList(),
    val parents: String,
    val additions: Int,
    val deletions: Int,
    val files: Int,
)

data class MockAction(
    val key: String,
    val label: String,
    val command: String,
    val badge: String? = null,
)

data class MockFile(
    val path: String,
    val code: String,
    val added: Int,
    val removed: Int,
    val staged: Boolean,
)

data class DetailFile(val path: String, val code: String, val added: Int, val removed: Int)

data class ReflogEntry(
    val index: Int,
    val action: String,
    val hash: String,
    val time: String,
    val danger: Boolean = false,
)

enum class ConsoleKind { Prompt, Comment, Output, Success, Hint, Error }

data class ConsoleLine(val text: String, val kind: ConsoleKind = ConsoleKind.Output)

data class DiffLine(val text: String, val kind: DiffKind)

enum class DiffKind { Context, Added, Removed, Hunk }

data class MockRepo(val name: String)

data class MockBranch(
    val name: String,
    val current: Boolean = false,
    val ahead: Int = 0,
    val behind: Int = 0,
)

val mockRepos = listOf(
    MockRepo("mybeautifulgit-chan"),
    MockRepo("dotfiles"),
    MockRepo("notes"),
)

val mockBranches = listOf(
    MockBranch("main", current = true, ahead = 2, behind = 3),
    MockBranch("ui/command-palette"),
    MockBranch("feat/rebase-editor"),
    MockBranch("fix/porcelain-v2"),
)

val mockActions = listOf(
    MockAction("f", "fetch", "git fetch --all --prune", "3"),
    MockAction("l", "pull", "git pull --rebase --autostash"),
    MockAction("p", "push", "git push origin main", "2"),
    MockAction("b", "branch", "git switch -c feature/ui-polish"),
    MockAction("s", "stash", "git stash push -u -m \"wip: ui\""),
    MockAction("r", "rebase", "git rebase -i origin/main"),
    MockAction("u", "undo", "git reset --hard HEAD@{1}"),
    MockAction("`", "terminal", "git status --porcelain=v2 -z"),
)

val mockCommits = listOf(
    MockCommit(
        shortHash = "9c2f1ab",
        message = "feat: interactive rebase with drag&drop",
        author = "anna",
        email = "anna@git-chan.dev",
        age = "12m",
        date = "2026-11-12 23:47 +03:00",
        lane = 0,
        lanes = listOf(0, 1),
        refs = listOf(
            MockRef("HEAD", RefKind.Head),
            MockRef("main", RefKind.Branch),
            MockRef("origin/main", RefKind.Remote),
        ),
        parents = "4f2a1c9",
        additions = 248,
        deletions = 31,
        files = 14,
    ),
    MockCommit(
        shortHash = "4f2a1c9",
        message = "fix: keep backup-ref when canceling",
        author = "anna",
        email = "anna@git-chan.dev",
        age = "27m",
        date = "2026-11-12 23:32 +03:00",
        lane = 1,
        lanes = listOf(0, 1),
        parents = "d41e7b2",
        additions = 18,
        deletions = 6,
        files = 3,
    ),
    MockCommit(
        shortHash = "d41e7b2",
        message = "merge: ui/command-palette",
        author = "ilya",
        email = "ilya@git-chan.dev",
        age = "1h",
        date = "2026-11-12 22:58 +03:00",
        lane = 0,
        lanes = listOf(0, 2),
        mergeLanes = listOf(2),
        refs = listOf(MockRef("v0.4.0-rc1", RefKind.Tag)),
        parents = "b83c05e 7ad9f31",
        additions = 64,
        deletions = 12,
        files = 5,
    ),
    MockCommit(
        shortHash = "b83c05e",
        message = "feat: command palette with every action",
        author = "ilya",
        email = "ilya@git-chan.dev",
        age = "1h",
        date = "2026-11-12 22:41 +03:00",
        lane = 2,
        lanes = listOf(0, 2),
        parents = "7ad9f31",
        additions = 312,
        deletions = 44,
        files = 9,
    ),
    MockCommit(
        shortHash = "7ad9f31",
        message = "refactor: extract GitProcess layer",
        author = "anna",
        email = "anna@git-chan.dev",
        age = "3h",
        date = "2026-11-12 20:15 +03:00",
        lane = 0,
        lanes = listOf(0),
        parents = "e5b27d0",
        additions = 96,
        deletions = 120,
        files = 11,
    ),
    MockCommit(
        shortHash = "e5b27d0",
        message = "feat: line-level staging",
        author = "vera",
        email = "vera@git-chan.dev",
        age = "1d",
        date = "2026-11-11 19:02 +03:00",
        lane = 1,
        lanes = listOf(0, 1),
        parents = "2c6a8f4",
        additions = 187,
        deletions = 22,
        files = 7,
    ),
    MockCommit(
        shortHash = "2c6a8f4",
        message = "test: fixtures for porcelain=v2",
        author = "vera",
        email = "vera@git-chan.dev",
        age = "1d",
        date = "2026-11-11 16:30 +03:00",
        lane = 0,
        lanes = listOf(0, 1),
        parents = "a09d3c1",
        additions = 402,
        deletions = 8,
        files = 12,
    ),
    MockCommit(
        shortHash = "a09d3c1",
        message = "chore: bump kotlin toolchain",
        author = "ilya",
        email = "ilya@git-chan.dev",
        age = "2d",
        date = "2026-11-10 14:11 +03:00",
        lane = 1,
        lanes = listOf(0, 1),
        refs = listOf(MockRef("v0.3.0", RefKind.Tag)),
        parents = "6b1f7e8",
        additions = 4,
        deletions = 4,
        files = 2,
    ),
    MockCommit(
        shortHash = "6b1f7e8",
        message = "feat: history graph without ascii parsing",
        author = "anna",
        email = "anna@git-chan.dev",
        age = "2d",
        date = "2026-11-10 12:45 +03:00",
        lane = 0,
        lanes = listOf(0),
        parents = "c8e40a5",
        additions = 221,
        deletions = 63,
        files = 6,
    ),
    MockCommit(
        shortHash = "c8e40a5",
        message = "fix: index.lock left after cancel",
        author = "vera",
        email = "vera@git-chan.dev",
        age = "3d",
        date = "2026-11-09 21:07 +03:00",
        lane = 2,
        lanes = listOf(0, 2),
        parents = "1d7b93f",
        additions = 27,
        deletions = 11,
        files = 3,
    ),
    MockCommit(
        shortHash = "1d7b93f",
        message = "docs: nothing-hidden manifesto",
        author = "ilya",
        email = "ilya@git-chan.dev",
        age = "3d",
        date = "2026-11-09 18:20 +03:00",
        lane = 0,
        lanes = listOf(0, 2),
        parents = "0a5c2e6",
        additions = 58,
        deletions = 0,
        files = 1,
    ),
    MockCommit(
        shortHash = "0a5c2e6",
        message = "init: compose multiplatform scaffold",
        author = "anna",
        email = "anna@git-chan.dev",
        age = "1w",
        date = "2026-11-05 10:00 +03:00",
        lane = 0,
        lanes = listOf(0),
        refs = listOf(MockRef("v0.1.0", RefKind.Tag)),
        parents = "-",
        additions = 1024,
        deletions = 0,
        files = 24,
    ),
)

val mockDetailFiles = listOf(
    DetailFile("src/ui/RebaseEditor.kt", "A", 186, 0),
    DetailFile("src/ops/InteractiveRebase.kt", "M", 42, 18),
    DetailFile("src/git/GitBackend.kt", "M", 12, 6),
    DetailFile("TODO.md", "M", 8, 7),
    DetailFile("shared/module.yaml", "M", 2, 1),
)

val mockFiles = listOf(
    MockFile("src/git/CliGitBackend.kt", "M", 42, 8, staged = false),
    MockFile("src/ui/HistoryScreen.kt", "M", 18, 3, staged = false),
    MockFile("TODO.md", "M", 6, 0, staged = false),
    MockFile("assets/graph-icon.svg", "A", 12, 0, staged = false),
    MockFile("src/ops/InteractiveRebase.kt", "A", 128, 0, staged = true),
    MockFile("src/git/porcelain/StatusParser.kt", "M", 64, 12, staged = true),
    MockFile("shared/module.yaml", "M", 2, 1, staged = true),
)

val mockDiff = listOf(
    DiffLine("--- a/src/git/CliGitBackend.kt", DiffKind.Hunk),
    DiffLine("+++ b/src/git/CliGitBackend.kt", DiffKind.Hunk),
    DiffLine("@@ -38,9 +38,14 @@ class CliGitBackend(", DiffKind.Hunk),
    DiffLine("     fun status(): RepoStatus {", DiffKind.Context),
    DiffLine("-        val raw = git(\"status\", \"--porcelain\")", DiffKind.Removed),
    DiffLine("+        val raw = git(", DiffKind.Added),
    DiffLine("+            \"status\", \"--porcelain=v2\", \"--branch\",", DiffKind.Added),
    DiffLine("+            \"--untracked-files=all\", \"-z\",", DiffKind.Added),
    DiffLine("+        )", DiffKind.Added),
    DiffLine("         return StatusParser.parse(raw)", DiffKind.Context),
    DiffLine("     }", DiffKind.Context),
    DiffLine("+", DiffKind.Added),
    DiffLine("+    fun fetchProgress(onLine: (String) -> Unit) {", DiffKind.Added),
    DiffLine("+        gitStream(\"fetch\", \"--all\", \"--prune\", onLine)", DiffKind.Added),
    DiffLine("+    }", DiffKind.Added),
    DiffLine(" }", DiffKind.Context),
)

val mockReflog = listOf(
    ReflogEntry(0, "reset: moving to HEAD~1", "9c2f1ab", "2m", danger = true),
    ReflogEntry(1, "commit: feat: interactive rebase", "9c2f1ab", "12m"),
    ReflogEntry(2, "rebase (finish): returning to main", "4f2a1c9", "27m"),
    ReflogEntry(3, "rebase (pick): command palette", "b83c05e", "1h"),
    ReflogEntry(4, "checkout: main -> ui/command-palette", "7ad9f31", "2h"),
    ReflogEntry(5, "pull: Fast-forward", "6b1f7e8", "3h"),
    ReflogEntry(6, "commit (amend): keep backup-ref", "a09d3c1", "1d"),
    ReflogEntry(7, "clone: github.com/baconana/git-chan", "0a5c2e6", "1w"),
)

val mockConsole = listOf(
    ConsoleLine("git-chan v0.4.0  (c) 2009 never forget", ConsoleKind.Comment),
    ConsoleLine("$ git status --porcelain=v2 --branch", ConsoleKind.Prompt),
    ConsoleLine("# branch.oid 9c2f1ab7c4e2", ConsoleKind.Comment),
    ConsoleLine("# branch.head main", ConsoleKind.Comment),
    ConsoleLine("# branch.ab +2 -3", ConsoleKind.Comment),
    ConsoleLine("$ git fetch --all --prune", ConsoleKind.Prompt),
    ConsoleLine("From github.com:baconana/git-chan", ConsoleKind.Output),
    ConsoleLine("   a1b2c3d..d4e5f6a  main -> origin/main", ConsoleKind.Output),
    ConsoleLine("$ git log -z --pretty=format:%H%x00%P%x00%s -n 50", ConsoleKind.Prompt),
    ConsoleLine("[ok] 50 commits parsed in 41 ms", ConsoleKind.Success),
    ConsoleLine("$ git rebase -i 4f2a1c9~1", ConsoleKind.Prompt),
    ConsoleLine("hint: backup-ref refs/git-chan/backups/2026-11-12_main", ConsoleKind.Hint),
    ConsoleLine("Successfully rebased and updated refs/heads/main.", ConsoleKind.Success),
)
