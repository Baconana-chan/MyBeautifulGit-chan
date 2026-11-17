<script setup lang="ts">
const route = useRoute()
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
      <NuxtLink to="/" class="tab" :class="{ active: !isDocs }">
        <span class="num">1</span> Home
      </NuxtLink>
      <NuxtLink to="/docs" class="tab" :class="{ active: isDocs }">
        <span class="num">2</span> Docs
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
        <span class="hide-sm">desktop · windows / macOS / linux</span>
        <span>[q] close</span>
      </div>
    </footer>
  </div>
</template>
