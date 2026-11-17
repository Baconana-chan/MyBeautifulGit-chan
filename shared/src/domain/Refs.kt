package org.baconana.mybeautifulgit_chan.domain

/**
 * `--` keeps a tag name such as `-foo` from being parsed as a Git option.
 * The caller still validates the full ref and confirms the tag exists.
 */
fun tagDeleteArgs(name: String): List<String> = listOf(
    "tag",
    "-d",
    "--",
    name.trim(),
)

/** `--` is required for a branch that was created as `refs/heads/-foo`. */
fun branchDeleteArgs(name: String, force: Boolean): List<String> = listOf(
    "branch",
    if (force) "-D" else "-d",
    "--",
    name.trim(),
)

/** Rename arguments keep both old and new names positional. */
fun branchRenameArgs(oldName: String, newName: String): List<String> = listOf(
    "branch",
    "-m",
    "--",
    oldName.trim(),
    newName.trim(),
)

/** The upstream value is an option argument; `--` protects the local branch. */
fun branchUpstreamArgs(localName: String, upstream: String): List<String> = listOf(
    "branch",
    "--set-upstream-to=${upstream.trim()}",
    "--",
    localName.trim(),
)
