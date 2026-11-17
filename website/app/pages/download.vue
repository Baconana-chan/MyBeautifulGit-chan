<script setup lang="ts">
useHead({ title: 'Download — MyBeautifulGit-chan' })

const { release, failed, reason, loading } = useLatestRelease()

const find = (pattern: RegExp) => release.value?.assets.find((a) => pattern.test(a.name))

const systems = computed(() => [
  {
    name: 'Windows',
    arch: 'x64',
    items: [
      { label: 'Installer', ext: '.msi', asset: find(/windows-x64\.msi$/i) },
      { label: 'Executable', ext: '.exe', asset: find(/windows-x64\.exe$/i) },
    ],
    note: 'SmartScreen may warn: the build is not code-signed.',
  },
  {
    name: 'macOS',
    arch: 'arm64 · x64',
    items: [
      { label: 'Apple Silicon', ext: '.dmg', asset: find(/macos-arm64\.dmg$/i) },
      { label: 'Intel', ext: '.dmg', asset: find(/macos-x64\.dmg$/i) },
    ],
    note: 'Unsigned and not notarized: right-click the app → Open on first launch.',
  },
  {
    name: 'Linux',
    arch: 'x86_64',
    items: [
      { label: 'AppImage', ext: '.AppImage', asset: find(/linux-x86_64\.AppImage$/i) },
      { label: 'Debian', ext: '.deb', asset: find(/\.deb$/i) },
      { label: 'Fedora', ext: '.rpm', asset: find(/\.rpm$/i) },
    ],
    note: 'AppImage needs chmod +x before the first run.',
  },
])

const statusLine = computed(() => {
  if (loading.value) return 'checking the latest release…'
  if (release.value) {
    const date = formatDate(release.value.publishedAt)
    return `latest release ${release.value.tag}${date ? ` · ${date}` : ''}`
  }
  return reason.value === 'not-found' ? 'release data is not public' : 'github is unreachable'
})

const failureNote = computed(() =>
  reason.value === 'not-found'
    ? 'GitHub returned no release for this repository: it is private, or the release has no published assets yet. The buttons below open the releases page, which needs an account with access. Direct links start working once the repository is public.'
    : 'GitHub did not answer, so the buttons below open the releases page instead of a direct file.',
)
</script>

<template>
  <section class="wrap hero">
    <p class="prompt"><span class="user">git-chan</span> ~ git-chan --download</p>
    <h1>Download</h1>
    <p class="tagline">
      Desktop builds for Windows, macOS and Linux.
      <span class="muted">Links point at the newest published release, so they never go stale.</span>
    </p>
  </section>

  <section class="wrap section">
    <div class="section-head">
      <h2>Packages</h2>
      <span class="hint">{{ statusLine }}</span>
    </div>

    <p v-if="failed" class="note">
      <span class="prompt-char">&gt;</span> {{ failureNote }}
    </p>

    <div class="download-grid">
      <article v-for="system in systems" :key="system.name" class="panel">
        <div class="panel-head">
          <span>{{ system.name }}</span>
          <span>{{ system.arch }}</span>
        </div>
        <div class="os-body">
          <a
            v-for="item in system.items"
            :key="item.label"
            class="btn"
            :class="{ primary: item === system.items[0] }"
            :href="item.asset?.url ?? LATEST_RELEASE_URL"
            rel="noopener"
          >
            <span class="k">[d]</span>
            {{ item.label }}
            <span class="ext">{{ item.ext }}</span>
            <span v-if="item.asset" class="size">{{ formatSize(item.asset.size) }}</span>
          </a>
          <p class="os-note">{{ system.note }}</p>
        </div>
      </article>
    </div>
  </section>

  <section class="wrap section">
    <div class="section-head">
      <h2>Elsewhere</h2>
      <span class="hint">source and history</span>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span>links</span>
        <span>github</span>
      </div>
      <div class="row">
        <span class="key">Repository</span>
        <a class="val link" :href="REPO_URL" rel="noopener">{{ REPO_URL.replace('https://', '') }}</a>
      </div>
      <div class="row">
        <span class="key">All releases</span>
        <a class="val link" :href="RELEASES_URL" rel="noopener">releases</a>
      </div>
      <div class="row">
        <span class="key">Build from source</span>
        <span class="val"><code>./kotlin.bat build -m desktopApp</code></span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.download-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}

.os-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.os-body .btn {
  justify-content: flex-start;
}

.os-body .ext {
  color: var(--faint);
  text-transform: none;
  letter-spacing: 0;
}

.os-body .size {
  margin-left: auto;
  color: var(--faint);
  font-size: 10.5px;
  letter-spacing: 0.04em;
}

.os-body .btn.primary .ext,
.os-body .btn.primary .size {
  color: var(--black);
  opacity: 0.6;
}

.os-note {
  margin: 4px 0 0;
  color: var(--faint);
  font-size: 11.5px;
  line-height: 1.5;
}

.link {
  color: var(--green-dim);
  border-bottom: 1px dotted var(--green-dim);
}

.link:hover {
  color: var(--green);
  border-bottom-style: solid;
}
</style>
