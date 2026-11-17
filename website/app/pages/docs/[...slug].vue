<script setup lang="ts">
const route = useRoute()

const { data: page } = await useAsyncData(`docs:${route.path}`, () =>
  queryCollection('docs').path(route.path).first(),
)

if (!page.value) {
  throw createError({ statusCode: 404, statusMessage: 'Page not found', fatal: true })
}

useHead({ title: `${page.value.title} — MyBeautifulGit-chan` })

const sections = computed(() => page.value?.body?.toc?.links ?? [])
const sectionCount = computed(() => sections.value.length)
</script>

<template>
  <div class="wrap docs-layout">
    <aside class="docs-nav panel">
      <div class="panel-head">
        <span>contents</span>
        <span>{{ sectionCount }}</span>
      </div>
      <nav>
        <template v-for="section in sections" :key="section.id">
          <a :href="`#${section.id}`" class="nav-item">
            <span class="marker">›</span>{{ section.text }}
          </a>
          <a
            v-for="child in section.children ?? []"
            :key="child.id"
            :href="`#${child.id}`"
            class="nav-item child"
          >
            <span class="marker">·</span>{{ child.text }}
          </a>
        </template>
      </nav>
    </aside>

    <article class="docs-article panel">
      <div class="panel-head">
        <span>{{ page.title }}</span>
        <span>en</span>
      </div>
      <div class="prose">
        <ContentRenderer :value="page" />
      </div>
    </article>
  </div>
</template>

<style scoped>
.docs-layout {
  display: grid;
  grid-template-columns: 258px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
  padding-top: 24px;
}

.docs-nav {
  position: sticky;
  top: 16px;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 32px);
  min-height: 0;
}

.docs-nav nav {
  overflow-y: auto;
  padding: 4px 0 8px;
  scrollbar-width: thin;
}

.nav-item {
  display: flex;
  gap: 7px;
  padding: 3px 10px;
  border-left: 2px solid transparent;
  color: var(--dim);
  font-size: 11.5px;
  line-height: 1.5;
}

.nav-item .marker {
  color: var(--green-dim);
}

.nav-item:hover {
  background: var(--raised);
  color: var(--text);
  border-left-color: var(--green-dim);
}

.nav-item.child {
  padding-left: 24px;
  font-size: 11px;
  color: var(--faint);
}

.nav-item.child .marker {
  color: var(--faint);
}

.docs-article {
  min-width: 0;
}

@media (max-width: 860px) {
  .docs-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .docs-nav {
    position: static;
    max-height: 40vh;
  }
}
</style>
