<script setup lang="ts">
const props = defineProps<{
  code?: string
  language?: string
  filename?: string
}>()

const pre = ref<HTMLElement | null>(null)
const copied = ref(false)
let timer: ReturnType<typeof setTimeout> | undefined

async function copy() {
  const text = props.code ?? pre.value?.textContent ?? ''
  if (!text) return

  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // Clipboard API needs a secure context; fall back to a temporary textarea.
    const area = document.createElement('textarea')
    area.value = text
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.top = '-1000px'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    document.execCommand('copy')
    area.remove()
  }

  copied.value = true
  clearTimeout(timer)
  timer = setTimeout(() => {
    copied.value = false
  }, 1600)
}

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div class="code-block">
    <div class="code-head">
      <span class="code-lang">{{ filename || language || 'text' }}</span>
      <button type="button" class="code-copy" :class="{ done: copied }" @click="copy">
        {{ copied ? 'copied' : 'copy' }}
      </button>
    </div>
    <pre ref="pre" v-bind="$attrs"><slot /></pre>
  </div>
</template>
