<script setup lang="ts">
const route = useRoute()
const isDownload = computed(() => route.path.startsWith('/download'))
const isDocs = computed(() => route.path.startsWith('/docs'))
</script>

<template>
  <div>
    <div class="topbar">
      <div class="topbar-left">
        <span class="badge">GIT-CHAN</span>
        <span>v1.0.0</span>
        <span class="hide-sm">·</span>
        <span class="path hide-sm">root@mybeautifulgit-chan:~</span>
      </div>
      <div class="topbar-right">
        <span class="hide-sm">[-] 100% [+]</span>
        <span class="hide-sm">web-256color</span>
        <span>static</span>
      </div>
    </div>

    <nav class="tabs">
      <NuxtLink to="/" class="tab" :class="{ active: !isDocs && !isDownload }">
        <span class="num">1</span> Home
      </NuxtLink>
      <NuxtLink to="/download" class="tab" :class="{ active: isDownload }">
        <span class="num">2</span> Download
      </NuxtLink>
      <NuxtLink to="/docs" class="tab" :class="{ active: isDocs }">
        <span class="num">3</span> Docs
      </NuxtLink>
    </nav>

    <main>
      <NuxtPage />
    </main>

    <footer class="statusbar">
      <div class="seg">
        <span>main · website/</span>
        <span class="hide-sm">nuxt · static output</span>
        <span class="hide-sm">GPL-2.0-only</span>
      </div>
      <div class="seg">
        <a class="status-link" :href="REPO_URL" rel="noopener">github ↗</a>
        <span class="hide-sm">[q] close</span>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.status-link {
  color: inherit;
  border-bottom: 1px dotted rgba(2, 4, 2, 0.45);
}

.status-link:hover {
  border-bottom-style: solid;
}
</style>
