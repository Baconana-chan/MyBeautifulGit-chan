---
title: User Guide
description: Task-oriented workflows for MyBeautifulGit-chan: open a repository, commit, synchronize, rewrite history and recover from it.
---

This guide answers “how do I …?” for MyBeautifulGit-chan. It is task-oriented: open a
repository, make a commit, synchronize, rewrite history, and — most importantly — understand what
is at risk and how to get back.

For installation, build commands and the feature overview see the repository README. Product
milestones are tracked in `TODO.md`; security notes live in `SECURITY_TODO.md`. Both files sit in
the repository root.

Commands in this guide assume Git 2.30+ and a terminal in the repository root. The app uses the
system Git executable, so the equivalent command is always the source of truth. Replace
`<remote>`, `<branch>` and `<sha>` with real values.

## Contents

- [Before you start](#before-you-start)
- [Open, clone or initialize a repository](#open-clone-or-initialize-a-repository)
- [Stage and commit](#stage-and-commit)
- [Branches and synchronization](#branches-and-synchronization)
- [Merge, rebase and conflicts](#merge-rebase-and-conflicts)
- [Stash, tags, worktrees and submodules](#stash-tags-worktrees-and-submodules)
- [Search, reflog and bisect](#search-reflog-and-bisect)
- [Undo and the recovery toolkit](#undo-and-the-recovery-toolkit)
- [Dangerous operations and rollback](#dangerous-operations-and-rollback)
- [Custom Git Host](#custom-git-host)
- [AI commit messages](#ai-commit-messages)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)

## Before you start

- Open a repository with **Open**, clone one with **Clone**, or create one with **Init** on the
  welcome screen. Drag-and-drop a folder onto the window to open it as a tab.
- The main workspace exposes status, history, branches, changes, remotes, tags, worktrees,
  submodules and unfinished operations. The **HISTORY** tab contains the operation journal and
  undo/redo controls; **ADVANCED** contains repository maintenance and escape hatches.
- The command palette (`Ctrl/Cmd+K`) finds app actions, Git commands, managers and navigation.
  Destructive actions are marked in red.
- The app shows the exact Git command and its consequences before destructive work. Some operations
  ask for confirmation; the command console is intentionally unguarded and can run raw Git.
- A Git command may still fail because of hooks, permissions, authentication, an index lock or
  repository state. Read the stderr panel; it is more useful than a generic error toast.

## Open, clone or initialize a repository

### Open an existing repository

1. Choose **Open** and select the repository folder (or drag it onto the window).
2. The app discovers the system Git, reads status, refs, remotes and the initial history page.
3. Use **Refresh** if another tool changed the repository while the watcher was inactive.

Opening another repository creates another tab. Closing a tab returns to its neighbour; the active
tab and recent folders are restored on the next start when they still exist.

### Clone

1. Choose **Clone**, enter the URL and a destination folder.
2. Choose the clone mode: full clone, `--filter=blob:none`, `--filter=blob:limit=1m` or
   `--filter=tree:0`.
3. Watch progress in the clone dialog. A long clone can be cancelled from the same dialog.

Equivalent full clone:

```bash
git clone -- <url> <destination>
```

The filter is passed as a separate `--filter` argument. A partial clone still needs network access
later when missing blobs are requested; it is not an offline copy.

### Initialize

1. Choose **Init**, select an empty/non-repository folder and the initial branch name.
2. The app runs `git init -b <branch>`.
3. Add a remote and make the first commit before using history-rewrite or pull workflows.

### If Git is missing

The setup screen offers platform-specific installation options and a manual path picker. Run the
app again after installing Git, or set the Git path in **Settings**. The app does not bundle Git.

## Stage and commit

### Stage changes

1. Open the changes view and review the diff.
2. Stage a whole file, a hunk, or selected added/removed lines. Partial staging builds a patch from
   the current index and applies it with `git apply --cached`.
3. Unstage the same selection, or discard it only after reading the warning (see
   [Discard](#discard-and-file-tracking)).

Useful terminal equivalents:

```bash
git add -- <path>
git add -p -- <path>
git restore --staged -- <path>
```

### Commit

1. Enter a subject and optional body.
2. Review staged files and the command preview.
3. Choose **Commit**. The command is equivalent to `git commit -m "<message>"`.

Options include amend, sign-off, signed commits, author/committer overrides, `--no-verify`, commit
templates, and `--fixup`/`--squash` targeting a recent commit. If a hook or Git rejects the commit,
the exact command and stderr remain available; **RETRY** reruns the same operation.

The optional **AI** action only fills the message field. It never commits. See
[AI commit messages](#ai-commit-messages).

### Amend the last commit

Use **Amend** in the commit area. This changes the current commit in place:

```bash
git commit --amend
```

Amending is a history rewrite even though it feels local. The old commit remains recoverable from
the app backup ref or reflog until Git expires the objects. Do not amend a commit that other people
already pulled without coordinating a force-push.

### Empty and failed commits

An empty commit is rejected unless the app explicitly offers an empty-commit action. A failed commit
does not intentionally discard the index; verify the staged state, fix the hook/identity problem and
retry. The failed command and stderr remain available.

## Branches and synchronization

### Create and switch

Use the branch manager to create a branch from the current commit or another ref and switch to it:

```bash
git switch -c <branch>
git switch <branch>
```

Branch names are validated with `check-ref-format` and passed after `--` where needed. A branch can
be renamed, given an upstream, compared with another branch, or deleted.

### Set or change upstream

```bash
git branch --set-upstream-to=<remote>/<branch> <branch>
```

The status bar and remote preflight show whether the current branch has an upstream and whether
ahead/behind counts are available. Without an upstream those counts are shown as not set, not as
zero.

### Fetch

Use the remote manager’s **Fetch** action and choose prune, tag mode, depth and partial-clone filter.
Every option maps to a real `git fetch` flag:

```bash
git fetch --prune --no-tags --depth=1 --filter=blob:none <remote>
```

Fetch updates remote-tracking refs; it does not modify the current branch or worktree.

### Pull

Choose the integration mode explicitly each time:

```bash
git pull --no-rebase <remote> <branch>   # merge
git pull --rebase <remote> <branch>      # rebase
git pull --ff-only <remote> <branch>     # fast-forward only
```

Repository `pull.rebase`/`pull.ff` configuration does not silently override the chosen mode. If a
pull stops with conflicts, use the operation bar to continue or abort (see
[Merge, rebase and conflicts](#merge-rebase-and-conflicts)).

### Push

The push manager covers the current branch, publishing to another branch, upstream setup, tags,
`--all`, `--mirror`, arbitrary refspecs and remote branch deletion.

```bash
git push -u <remote> <branch>
```

Force is never implicit. Choose one of:

- **No force** — normal fast-forward check.
- **`--force-with-lease`** — overwrite only if the remote still points where the last fetch saw it.
  This protects against a concurrent update; it does **not** make the push undoable.
- **`--force`** — overwrite the remote unconditionally. Use only when you understand who else may
  have based work on the old tip.

Read [Force push](#force-push) before using either force mode.

### Compare branches

The branch comparison shows ahead/behind counts and unique commits. The underlying command is:

```bash
git rev-list --left-right --count <base>...HEAD
git log --oneline <base>..HEAD
git log --oneline HEAD..<base>
```

### Remotes and Custom Git Host

The remote manager can add, edit, fetch and remove remotes and set the remote HEAD. For the
provider-agnostic identity and preflight panel see [Custom Git Host](#custom-git-host).

Removing a remote removes its URL and remote-tracking branches locally; the server repository is
not changed. Recover by adding the URL back and fetching (see
[Remote deletion](#remote-deletion-and-url-changes)).

## Merge, rebase and conflicts

### Merge

Start a merge from the branch/remote workflow and review the command and the loss/conflict preview.
When the merge stops:

```bash
git merge --continue
git merge --abort
```

The app detects an unfinished merge from Git state. It does not invent a skip action where Git
does not support one.

### Interactive rebase and history rewrite

The rebase editor uses a real `git rebase -i --rebase-merges` plan. It supports `pick`, `reword`,
`edit`, `squash`, `fixup`, `drop`, `exec`, `break`, `label`, `reset` and `merge`. The todo can be
edited while a rebase is stopped.

Before a rewrite the app creates a backup ref under `refs/mybeautifulgit/backups/…`. Recent rewrite
operations have an **Undo** action; if local changes appeared since the rewrite, Undo stashes them
first rather than overwriting them.

During a stopped rebase:

```bash
git rebase --continue
git rebase --skip     # only when Git allows skipping the current step
git rebase --abort    # restore the pre-rebase branch
```

After a completed rewrite, recovery is through the app’s **HISTORY → Undo** or the backup ref. A
rewritten branch is only local until you push it; the remote still has the old history until a
force-push.

### Conflicts

Resolve markers in the merge editor or in your editor, stage the resolved files, then continue the
operation. For a conflicted pull, merge or rebase, use the operation bar’s abort action if you want
to return to the pre-operation state. Do not delete `.git/MERGE_HEAD`, `.git/rebase-merge` or
`.git/CHERRY_PICK_HEAD` by hand.

### Delete or drop a commit

The app offers four explicit routes:

- interactive `drop` in a rebase, re-applying descendants;
- hard reset to the parent (deletes the tail, descendants are not re-applied);
- `git revert` (adds a new inverse commit; history is preserved);
- `git filter-repo` across all local refs (last resort for secrets or paths).

Read [Drop or delete a commit](#drop-or-delete-a-commit) and
[filter-repo](#filter-repo-remove-a-path-or-secret-from-all-history) before choosing one.

## Stash, tags, worktrees and submodules

### Stash

Create a stash with optional untracked/ignored files, staged-only mode where supported, and partial
file selection:

```bash
git stash push -u -m "work in progress" -- <path>
git stash apply stash@{0}   # keep the stash
git stash pop   stash@{0}   # apply and drop on success
git stash drop  stash@{0}
```

`git stash pop` keeps the stash if applying it conflicts. Prefer **apply** when the changes matter;
drop only after verifying the result. Recovery after a drop is covered in
[Stash drop](#stash-drop).

### Tags

The tag manager creates lightweight, annotated and signed tags, pushes/deletes them locally or on a
remote, and validates ref names the same way as branches.

```bash
git tag <tag> <sha>
git tag -a <tag> -m "<message>" <sha>
git push <remote> refs/tags/<tag>
git push <remote> --delete refs/tags/<tag>
```

Tags do not have a reflog. A deleted tag may become unreachable, so record the target commit or keep
a second ref before deleting it.

### Worktrees

The worktree manager adds, switches to, locks/unlocks, removes and prunes worktrees. Paths must
belong to the worktree list and are passed after `--`.

```bash
git worktree list
git worktree add <path> <branch>
git worktree remove -- <path>
git worktree prune --dry-run
git worktree prune
```

A worktree is another checkout of the same repository. Removing it does not delete committed
history, but uncommitted files in that worktree are not stored in the main worktree. Commit or stash
them before removal. See [Worktree removal](#worktree-removal).

### Submodules

The submodule manager initializes, updates, syncs, adds and removes submodules; custom configured
updates require explicit confirmation.

```bash
git submodule update --init --recursive
git submodule sync --recursive
git submodule deinit -- <path>
git submodule update --init --recursive -- <path>
```

The superproject records a submodule commit, not the submodule’s uncommitted state. Commit inside
the submodule first. Removal is covered in [Submodule removal](#submodule-removal).

## Search, reflog and bisect

### File history, blame and grep

The **SEARCH** view provides:

- file history with rename following (`git log --follow`);
- line-by-line blame with jump-to-commit;
- repository grep (`git grep`).

Clicking a result opens the commit inspector; the original path and commit hash remain visible.

### Reflog browser

The reflog browser lists HEAD and branch movements and offers restore/reset actions and branch
creation from an entry. If the UI action is unavailable or you prefer the terminal:

```bash
git reflog --date=iso --all
git reflog show <branch> --date=iso
git switch -c rescue <sha>
git reset --hard <sha>
```

Reflog is local. It is not a server backup, and it expires according to Git configuration.

### Bisect

The bisect tab marks good/bad/skip commits, optionally runs an automated test command with a
timeout, and shows `BISECT_LOG`. To stop and return to the starting point:

```bash
git bisect reset
```

Use bisect on a clean worktree. An automated command is executed by Git; make sure it is safe to run
on every candidate.

### Command console

The console runs raw Git commands without a shell, shows stdout/stderr and keeps per-repository
history/favorites. It bypasses the app’s convenience checks, so it can run destructive commands that
have no Undo. Copy the exact command, inspect it, and use the same recovery rules in this guide.

## Undo and the recovery toolkit

There are several different mechanisms. They are not interchangeable.

### HISTORY tab

The application journal records operations with their time, exact command and result. **Undo** is
offered only for the newest reversible operation:

- commits undo with a soft reset that returns their changes to the index;
- rewrites and resets undo through their backup ref;
- checkout restores the previous ref.

Undo verifies the branch and HEAD first. If either moved, it refuses instead of overwriting newer
work. Redo repeats the last undone operation under the same checks. Push is deliberately marked as
non-reversible: undoing a journal entry cannot undo what another machine already received.

### Backup refs

Before history rewriting and reset, the app writes a ref such as:

```text
refs/mybeautifulgit/backups/<timestamp>-<n>-<operation>-<short-tip>
```

These refs are hidden from the normal branch list but are real Git refs. List them from the console
or terminal:

```bash
git for-each-ref --format='%(refname) %(objectname:short)' refs/mybeautifulgit/backups
```

Recover a tip without moving your current branch:

```bash
git branch chan-rescue <backup-ref>
git show <backup-ref>
```

Or move the current branch after stashing any local work:

```bash
git stash push -u -m "before recovery"
git reset --hard <backup-ref>
git stash pop
```

Backup refs protect **commits**, not uncommitted file contents. A backup taken before a
`reset --hard` does not restore the dirty worktree that was discarded at the same time.

### Reflog

Reflog records where HEAD and local branch refs pointed. It is the first place to look after an
accidental reset, amend, rebase, checkout or branch deletion:

```bash
git reflog --date=iso --all
git reset --hard <old-sha>
```

Prefer creating a rescue branch over resetting immediately:

```bash
git switch -c chan-rescue <old-sha>
```

Reflog entries expire. `git gc`, `git prune`, an expired reflog or a fresh clone can remove the
objects you need. Do not run maintenance after an accident until you have saved a rescue branch or
bundle.

### Patch and stash backups

Discard can save a patch or a stash before deleting changes. The patch path is shown in the toast
and normally looks like:

```text
<repo>/.git/chan-backups/discard-<timestamp>-<path>.patch
```

Check and apply it from the repository root:

```bash
git apply --check .git/chan-backups/discard-<timestamp>-<path>.patch
git apply .git/chan-backups/discard-<timestamp>-<path>.patch
```

A stash backup is a normal stash entry:

```bash
git stash list
git stash show -p stash@{0}
git stash branch chan-discard-recovery stash@{0}
```

### Bundles

`filter-repo` creates a bundle of all refs under:

```text
<repo>/.git/mybeautifulgit/backups/*.bundle
```

Verify and inspect it before relying on it:

```bash
git bundle verify .git/mybeautifulgit/backups/<bundle>
git bundle list-heads .git/mybeautifulgit/backups/<bundle>
```

A bundle is a portable backup. Copy it somewhere outside the repository before any further rewrite.

### Limits of automatic recovery

- The journal Undo only covers the newest reversible operation and only while branch/HEAD still
  match its recorded state.
- Reflog and backup refs are local. A force-push changes the server; only a copy on the server,
  another clone, or an explicit remote backup can restore it.
- `discard` with **None** intentionally has no backup.
- A backup ref does not contain uncommitted or untracked files.
- `filter-repo`, `gc` and `prune` can remove old objects regardless of what the UI showed earlier.
- Always verify a recovery in a separate rescue branch before force-pushing it anywhere.

## Dangerous operations and rollback

The app’s philosophy is “warnings, not nannies”: dangerous operations are exposed with the exact
command, a preview where possible, and a recovery path where one exists. The table is a map, not a
substitute for reading the specific section.

| Operation | What can be lost | App protection | Primary recovery |
|---|---|---|---|
| [Discard changes](#discard-and-file-tracking) | Worktree edits, untracked files | Optional patch or stash backup; warning | Patch/stash backup, editor local history |
| [Reset](#reset-soft-mixed-hard-keep) | Commits, index, worktree files | Loss preview; backup ref; journal Undo | Backup ref, reflog, rescue branch |
| [Amend/reword](#amend-reword-and-rebase) | Previous commit object | Backup ref; journal Undo | Backup ref, reflog |
| [Drop/delete commit](#drop-or-delete-a-commit) | Commit and descendants | Backup ref; journal Undo where supported | Backup ref, reflog |
| [Force push](#force-push) | Remote branch history | Explicit force choice; consequences shown | Another clone/server copy; refetch and re-push |
| [Delete branch/tag](#branch-and-tag-deletion) | Ref, upstream config, tag object | Safe/forced choice; no blanket undo | Reflog, tracking ref, known commit/tag object |
| [Stash drop](#stash-drop) | Stash commit | No automatic undo | `refs/stash` reflog, `fsck` before gc |
| [filter-repo](#filter-repo-remove-a-path-or-secret-from-all-history) | Rewritten history across refs | Mandatory dry-run; backup ref; all-ref bundle | Bundle and backup ref |
| [Worktree removal](#worktree-removal) | Uncommitted files in that checkout | Path validation; no file backup | Re-add from branch; restore from stash/commit |
| [Submodule removal](#submodule-removal) | Submodule checkout and uncommitted state | Confirmation for custom updates | Superproject commit, `.gitmodules`, re-init |
| [gc/prune](#gc-and-prune) | Unreachable objects | Prune dry-run; exact command | Bundle/copy made beforehand; otherwise none |
| [Remote deletion/edit](#remote-deletion-and-url-changes) | Remote URL and tracking refs | Server is untouched; exact command | Re-add URL and fetch |

### Discard and file tracking

**Where:** changes view → discard for a file, hunk or line.

Discard is the most immediate destructive action because it edits the worktree, not history. Before
confirming, choose:

- **Stash** — stores tracked changes (and untracked content with `-u`) in a stash. Partial/hunk
  discard cannot use a stash.
- **Patch** — writes a patch under `.git/chan-backups/`; works for files and partial selections.
- **None** — deletes immediately. The app cannot undo it.

The underlying file operations are:

```bash
git restore --worktree -- <path>   # tracked file
git clean -f -- <path>             # untracked file
```

For a partial discard the app restores the file and re-applies the remainder patch, so it refuses
if the current diff no longer matches.

**Rollback:** apply the patch or restore from the stash as shown in
[Patch and stash backups](#patch-and-stash-backups). If **None** was used with no external copy, the
edit is gone; check your editor’s local history if it has one.

### Reset: soft, mixed, hard, keep

**Where:** commit actions → reset, or the branch/commit inspector.

| Mode | Branch | Index | Worktree |
|---|---|---|---|
| `--soft` | moves | keeps staged changes | keeps |
| `--mixed` | moves | resets to target | keeps |
| `--hard` | moves | resets | resets, deleting tracked edits |
| `--keep` | moves | resets | keeps local changes when safe; refuses if it would overwrite them |

The app shows a reset preview (commits leaving the branch, unique/unreachable objects, dirty files
and `--keep` conflicts) and creates a backup ref before the reset. A completed reset is reversible
through **HISTORY → Undo** or the backup ref.

```bash
git reset --hard <target>
git reset --hard <backup-ref>   # after checking you are on the intended branch
```

**Rollback:** create `chan-rescue` from the backup ref first and inspect it. `reset --hard` also
discards tracked worktree/index edits; the backup ref alone does not bring those back. Use the
discard patch/stash if one was created, or recover from your editor/local backup.

### Amend, reword and rebase

**Where:** commit inspector, commit actions, interactive rebase editor.

Amend and reword replace commit objects; interactive rebase can replace a whole range. The app
creates a backup ref before the rewrite and offers Undo for recent rewrites. If a rebase is still
stopped, abort it instead of resetting:

```bash
git rebase --abort
```

After a completed rewrite:

```bash
git stash push -u -m "before recovery"
git reset --hard <backup-ref>
git stash pop
```

If the app’s Undo sees that HEAD or the branch moved, it refuses. That is intentional: recover
manually into a rescue branch instead of overwriting newer work.

**Force-push only after verifying the local recovery.** Other people may have fetched the rewritten
history; coordinate before changing the remote.

### Drop or delete a commit

**Where:** commit inspector → delete, interactive rebase → `drop`, or hard reset to a parent.

- **Drop in rebase** removes the commit and re-applies descendants. Conflicts can stop the rebase;
  resolve them or abort.
- **Hard reset to parent** cuts off the whole tail. Descendants are not re-applied.
- **Revert** is the safe public-history option: it adds an inverse commit and does not rewrite.
- **`filter-repo`** removes content from every ref and has a different recovery model.

```bash
git rebase --abort                    # while stopped
git reset --hard <backup-ref>         # after completion
git branch chan-rescue <old-sha>      # from reflog
```

The app backup ref and journal Undo cover the rewrite/reset routes. A revert needs no object
recovery; undo it with another revert or reset the new commit if it was not pushed.

### Force push

**Where:** push manager → force mode.

```bash
git push --force-with-lease <remote> <branch>
git push --force <remote> <branch>
```

Both replace the remote branch tip. The lease version checks the remote’s last observed value and
rejects a concurrent update; it is not an undo. The app shows the consequences and never selects
force by default. Local backup refs do **not** back up the server.

`--mirror` is stronger still: it pushes all local refs and deletes remote refs that do not exist
locally. Treat it as a remote-rewrite operation, not as a normal sync, and make a remote backup or
mirror clone first. Recovery is the same as for a force-push or remote deletion.

**Rollback:**

1. Find the old commit in another clone, a server-side backup, or this clone’s remote-tracking
   ref/reflog when your Git configuration records one:
   ```bash
   git reflog show refs/remotes/<remote>/<branch> --date=iso
   ```
2. Verify the old commit and create a rescue branch:
   ```bash
   git branch chan-remote-rescue <old-sha>
   ```
3. Push that exact object back, preferably with a lease if you know the current remote tip:
   ```bash
   git push --force-with-lease <remote> <old-sha>:refs/heads/<branch>
   ```
   If the lease rejects because the remote moved again, stop and coordinate; do not fall back to
   `--force` blindly.
4. If the remote branch was deleted:
   ```bash
   git push <remote> <old-sha>:refs/heads/<branch>
   ```

If no clone has the old object, recovery depends on the hosting provider’s backups. Local Git cannot
invent it.

### Branch and tag deletion

**Where:** branch manager and tag manager.

Local branch deletion uses `git branch -d` (safe: refuses if unmerged) or `-D` (forced). The app
validates the ref and passes it after `--`. Remote branch deletion uses a push deletion:

```bash
git push <remote> --delete <branch>
```

**Local branch rollback:**

```bash
git reflog --all --date=iso
git branch chan-rescue <sha>       # from the deleted branch’s last tip
```

The branch’s upstream configuration is not restored automatically; set it again with
`git branch --set-upstream-to`.

**Remote branch rollback:** use a local remote-tracking ref or another clone:

```bash
git push <remote> refs/remotes/<remote>/<branch>:refs/heads/<branch>
```

If the tracking ref was pruned or the remote was removed, use another clone or a provider backup.

**Tag deletion:** tags have no reflog. A lightweight tag can be recreated from the commit if it is
still reachable:

```bash
git tag <tag> <sha>
git push <remote> refs/tags/<tag>
```

An annotated/signed tag also needs its tag object or must be recreated with a new signature. Before
deleting a tag you may not be able to identify, run `git show <tag>` and copy the commit/tag object
id. If the object is already unreachable, try `git fsck --unreachable` **before** `gc`; there is no
guarantee it is recoverable.

### Stash drop

**Where:** stash list → drop.

A stash is a commit reachable through `refs/stash`; dropping the ref removes the easy path to it.

```bash
git stash list
git reflog show refs/stash --date=iso
git stash apply <sha>
git stash branch chan-stash-rescue <sha>
```

If the reflog entry is gone but the object is still present, `git fsck --no-reflogs --unreachable`
may find it. Do this before `git gc` or `git prune`; after object pruning the stash is gone.

### filter-repo: remove a path or secret from all history

**Where:** commit inspector → filter-repo / history rewrite.

`filter-repo` is an optional external tool. It rewrites every local ref, and can invalidate reflogs,
old objects and remote configuration. The app requires a dry-run preview before a real rewrite, and
creates both:

- a backup ref pointing at the current branch tip;
- a bundle containing all refs under `.git/mybeautifulgit/backups/`.

It deliberately disables branch-only Undo for this operation.

Inspect the preview and complete the real rewrite only when the changed refs and paths are expected.
Afterward, check the remotes and reflog state; `filter-repo` may remove the `origin` remote and
rewrite refs that other work depended on.

**Rollback:** the bundle is the all-ref backup:

```bash
git bundle verify .git/mybeautifulgit/backups/<bundle>
git clone .git/mybeautifulgit/backups/<bundle> ../chan-rescue
```

Or fetch selected refs into rescue namespaces in the current repository:

```bash
git fetch .git/mybeautifulgit/backups/<bundle> \
  'refs/heads/*:refs/heads/rescue/*' \
  'refs/tags/*:refs/tags/rescue/*'
```

The backup ref alone restores only the recorded branch tip. The bundle can restore every ref it
contains. Copy the bundle outside the repository first, and do not run `gc`/`prune` until recovery is
verified. Then restore or push the chosen refs deliberately.

### Worktree removal

**Where:** worktree manager → remove, or prune for stale metadata.

```bash
git worktree remove -- <path>
git worktree prune
```

Removing a worktree deletes that checkout; committed history remains in the shared repository, but
uncommitted files in the removed directory are not saved by the app. Commit or stash them first.

**Rollback:** recreate a checkout from the branch:

```bash
git worktree add <path> <branch>
git worktree repair
```

If the worktree was removed while dirty and no stash/patch exists, recover only from the editor,
filesystem backup or a separate copy. `prune` removes stale administrative entries, not commits.

### Submodule removal

**Where:** submodule manager → deinit/remove.

```bash
git submodule deinit -- <path>
git rm -- <path>
```

The superproject records a gitlink; uncommitted changes inside the submodule are separate and can be
lost. Commit/push in the submodule before removing it. Removing a submodule changes `.gitmodules`
and the index; review the staged diff before committing.

**Rollback:** restore the superproject commit, `.gitmodules` and gitlink, then initialize:

```bash
git checkout <superproject-commit> -- .gitmodules <path>
git submodule update --init --recursive -- <path>
```

If the submodule URL was changed, restore it from `.gitmodules`/the superproject commit. A commit
inside the submodule is recoverable only if it was committed/pushed or still exists in that
submodule’s reflog.

### gc and prune

**Where:** **ADVANCED** → repository maintenance.

`git gc` can expire reflogs and delete unreachable objects; `git prune` removes unreachable objects
immediately. The app shows a mandatory dry-run before prune, but a maintenance run is not an undoable
operation.

```bash
git gc
git gc --aggressive
git prune --dry-run
git prune
```

**There is no Git-level rollback after object pruning.** Before maintenance, create a bundle or
mirror clone if old commits, deleted branches, dropped stashes or pre-filter-repo objects might
still matter:

```bash
git bundle create /path/to/rescue.bundle --all
git clone --mirror <repo> /path/to/rescue.git
```

A rescue branch/ref keeps an object reachable; naming a commit in a note is not enough. Do not run
maintenance to “clean up” after an accidental reset before checking
[Reflog](#reflog) and [Backup refs](#backup-refs).

### Remote deletion and URL changes

**Where:** remote manager → remove/edit URL.

Removing a remote deletes its URL and remote-tracking refs locally; the server repository and its
branches are untouched. Editing the URL loses the previous value unless you recorded it.

**Rollback:** add the remote and fetch again:

```bash
git remote add <remote> <url>
git fetch <remote>
```

Remote-tracking refs are recreated from the server. Local branches are not deleted. If you removed a
remote that had credentials or a special push URL, re-enter them deliberately; do not paste a token
into a shell history or a copied command.

### Command console and hooks

The console and hooks are escape hatches, not sandboxes:

- Raw commands can bypass the app’s previews and validation. A destructive command has only the
  recovery paths that apply to Git itself.
- Hooks are executable code. Enabling a hook or changing a hooks path outside `.git/hooks` requires
  confirmation because a hook can run arbitrary commands with your user permissions.
- `--no-verify` skips commit/push hooks for one operation; it does not change repository history.
- Disable or restore a hook through the hooks manager; keep a copy of a hook you edit. The app does
  not promise an automatic backup for arbitrary hook files.

## Custom Git Host

**Where:** remote manager → select a remote → **CUSTOM GIT HOST** and **PREFLIGHT**.

Level 1 is provider-agnostic. It reads the remote configuration and shows what can be known without
calling a forge API:

- all `remote.<name>.url` and `remote.<name>.pushurl` values;
- `url.<base>.insteadOf` rewrites;
- host, port, owner, repository and transport for scp-like, `ssh://`, `https://`, `git://` and local
  paths;
- `CUSTOM`/`UNKNOWN` when the host or path is not unambiguous;
- branch, upstream, ahead/behind and staged/unstaged/untracked/conflicted counts in the local
  preflight.

Permissions, pull requests/MRs and checks remain **UNKNOWN** at level 1. The app does not guess them
from a folder name, an author email or a remote called `origin`, and it does not call GitHub/GitLab
APIs. If a safe compare/open link cannot be built, use the displayed URL manually.

Useful inspection commands:

```bash
git remote -v
git config --get-all remote.<name>.url
git config --get-all remote.<name>.pushurl
git config --get-regexp '^url\..*\.insteadof$'
```

Credentials are redacted in the UI. If the URL field is hidden because a credential was embedded in
it, edit the remote with the full replacement URL or move authentication to a credential helper.
For self-hosted forges, the absence of permission/PR data is expected, not a failed API call.

## AI commit messages

**Where:** commit message area → **GENERATE** (enabled in Settings).

1. Configure an OpenAI-compatible endpoint and model in Settings, and store the API key in the
   separate encrypted credential store. External endpoints must use HTTPS; local HTTP is an explicit
   mode for Ollama/LM Studio.
2. Review the endpoint, branch, file count and payload size shown in the confirmation.
3. **GENERATE** sends the selected diff and fills the message field only. It never commits.
4. Use **REGENERATE** or restore the previous draft before committing. The final message remains
   editable.

By default only the staged diff is sent. **WORKTREE** is a separate explicit action that includes
unstaged and untracked text files. `.env`, credential/key files and binary patches are excluded, and
common secret patterns are redacted, but redaction is best-effort: the configured endpoint receives
the remaining diff. Do not send a diff containing a secret to an endpoint you do not trust.

The API key is not stored in `settings.properties`, prompts, logs or the Git journal. See
[Security notes](#security-notes).

## Security notes

- **Credentials:** HTTPS tokens use a temporary credential helper and an AES-GCM-encrypted
  `credentials.bin`; the key is stored beside it with owner-only permissions where supported. This
  protects accidental plaintext copies, not another process running as the same desktop user.
- **`credential.helper`** is an arbitrary command and is stored in plaintext. Do not put a token or
  password in that field; use the encrypted token field.
- **SSH:** use the system agent/OpenSSH or an explicit key path.
- **URLs and commands:** credentials are redacted in remote displays, command previews, copied
  commands and the command history. Never paste a token into a public issue or a shared log.
- **AI:** opt-in only; the default payload is staged-only; the API key is separate from settings.
  The endpoint is part of your trust boundary.
- **Hooks and the command console:** arbitrary code/commands can run with your user permissions.
  Review them like you would a shell script.
- **Telemetry:** the app does not send telemetry.
- **Local data:** settings, recent repositories, command history and credentials stay on the
  machine. Deleting repository tabs does not delete the repository or its Git data.

## Troubleshooting

### Git is not found

Install Git or set its path in **Settings**. The app verifies `git --version`; a stale PATH entry or
an incomplete command-line-tools shim on macOS is not enough.

### Push rejected / non-fast-forward

Fetch first, compare the branches, and integrate the remote changes with merge or rebase. A
non-fast-forward is protecting the remote. Force is a separate, explicit decision; read
[Force push](#force-push).

### Conflict or stopped operation

Use the operation bar to continue or abort. Do not delete Git state files manually. The correct
commands are `git merge --continue/--abort`, `git rebase --continue/--skip/--abort`,
`git cherry-pick --continue/--abort`, `git revert --continue/--abort`, or `git am --continue/--abort`
where supported.

### Detached HEAD

A detached HEAD is not a branch. Create a branch before committing if the work should survive:

```bash
git switch -c <branch>
```

The status/preflight panel reports detached HEAD rather than inventing an upstream.

### Upstream shows “not set”

Set it with the branch manager or:

```bash
git branch --set-upstream-to=<remote>/<branch> <branch>
```

Until then, ahead/behind and pull/push defaults may be unavailable; the app shows them as unknown
instead of zero.

### Undo is unavailable

The operation may be older than the newest reversible journal entry, may be a push, or HEAD/branch
may have moved. Use [Reflog](#reflog) and [Backup refs](#backup-refs) manually. If the branch moved,
create a rescue branch instead of hard-resetting over newer work.

### `index.lock` exists

Another Git process may still be running. Close the app’s operation, wait for it to finish, and check
for a live Git process before removing `.git/index.lock`. Never delete the lock while a Git process
holds it.

### Large diff, patch or file

The app has bounded readers and shows a “too large”/truncated notice instead of freezing. Use the
command console or terminal with a narrower path/filter if you need the complete content.

### A file was discarded and there is no patch

Check the toast for the patch path, `git stash list` for a discard stash, and your editor’s local
history. With **Discard → None** and no external copy, there is no Git object to restore.

### A commit, branch or stash seems gone

Stop before running `gc` or `prune`. Check `git reflog --all`, backup refs, `refs/stash`,
`git fsck --no-reflogs --unreachable`, bundles and another clone. Then save anything you find into a
rescue branch or bundle.

### A remote branch was overwritten or deleted

Local recovery cannot recreate an object that no clone has. Check remote-tracking refs (and their
reflogs when available) and other clones first, then push the recovered object back. Coordinate with
anyone who may have fetched the old history; a force-push is not a private operation on a shared
remote.
