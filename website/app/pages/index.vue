<script setup lang="ts">
const commands = [
  { action: 'Checkout', command: 'git checkout <ref>' },
  { action: 'Commit', command: 'git commit -m "<message>"' },
  { action: 'Amend', command: 'git commit --amend --no-edit' },
  { action: 'Undo last commit', command: 'git reset --soft HEAD~1' },
  { action: 'Discard changes', command: 'git restore <path>' },
  { action: 'Force push', command: 'git push --force-with-lease' },
]

const features = [
  {
    title: 'The command is visible',
    body: 'Every action prints the git call it makes, so you can read it, copy it or script it before it touches the repository.',
  },
  {
    title: 'Undo is a first-class verb',
    body: 'History rewrites show the way back in the same panel as the action itself, not in a postmortem afterwards.',
  },
  {
    title: 'Your git, not a reimplementation',
    body: 'The desktop app drives the system git with your config, hooks and credentials — nothing is reimplemented in a library.',
  },
]
</script>

<template>
  <section class="wrap hero">
    <p class="prompt">
      <span class="user">git-chan</span> ~ git status
      <span class="faint"># on branch main</span>
    </p>

    <h1>MyBeautifulGit-chan</h1>
    <p class="tagline">
      all of git. no babysitting. with undo.
      <span class="muted">A desktop Git client that shows the command behind every action.</span>
    </p>

    <div class="actions">
      <NuxtLink to="/docs" class="btn primary"><span class="k">[d]</span> Read the docs</NuxtLink>
      <a href="#actions" class="btn"><span class="k">[a]</span> Actions</a>
    </div>
  </section>

  <section id="actions" class="wrap section">
    <div class="section-head">
      <h2>Actions</h2>
      <span class="hint">every panel shows the real command</span>
    </div>

    <div class="panel cmd-table">
      <div class="panel-head">
        <span>inspect</span>
        <span>git 2.56.1</span>
      </div>
      <div v-for="item in commands" :key="item.action" class="row">
        <span class="key">{{ item.action }}</span>
        <span class="val">{{ item.command }}</span>
      </div>
    </div>
  </section>

  <section class="wrap section">
    <div class="section-head">
      <h2>What it does</h2>
      <span class="hint">3 things, no marketing</span>
    </div>

    <div class="cards">
      <article v-for="feature in features" :key="feature.title" class="card">
        <h3><span class="marker">[+]</span>{{ feature.title }}</h3>
        <p>{{ feature.body }}</p>
      </article>
    </div>
  </section>

  <section class="wrap section">
    <div class="section-head">
      <h2>Status</h2>
      <span class="hint">1.0 in progress</span>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span>build</span>
        <span>desktop</span>
      </div>
      <div class="row">
        <span class="key">Packages</span>
        <span class="val">Windows msi/exe · macOS dmg · Linux deb/rpm/AppImage</span>
      </div>
      <div class="row">
        <span class="key">Docs</span>
        <span class="val">being ported here · full guide ships with the app</span>
      </div>
      <div class="row">
        <span class="key">Build from source</span>
        <span class="val"><code>./kotlin.bat build -m desktopApp</code></span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.faint {
  color: var(--faint);
}
</style>
