export const REPO = 'Baconana-chan/MyBeautifulGit-chan'
export const REPO_URL = `https://github.com/${REPO}`
export const RELEASES_URL = `${REPO_URL}/releases`
export const LATEST_RELEASE_URL = `${RELEASES_URL}/latest`

export interface ReleaseAsset {
  name: string
  url: string
  size: number
}

export interface LatestRelease {
  tag: string
  publishedAt: string
  assets: ReleaseAsset[]
}

interface GithubRelease {
  tag_name?: string
  published_at?: string
  assets?: Array<{ name: string; browser_download_url: string; size: number }>
}

/**
 * Reads the latest published release from the GitHub API in the browser.
 * The page still works without it: links fall back to /releases/latest.
 */
export type FailureReason = 'none' | 'not-found' | 'network'

export function useLatestRelease() {
  const release = ref<LatestRelease | null>(null)
  const failed = ref(false)
  const reason = ref<FailureReason>('none')
  const loading = ref(true)

  onMounted(async () => {
    try {
      const data = await $fetch<GithubRelease>(
        `https://api.github.com/repos/${REPO}/releases/latest`,
        {
          headers: { Accept: 'application/vnd.github+json' },
          timeout: 8000,
        },
      )

      release.value = {
        tag: data.tag_name ?? '',
        publishedAt: data.published_at ?? '',
        assets: (data.assets ?? []).map((asset) => ({
          name: asset.name,
          url: asset.browser_download_url,
          size: asset.size,
        })),
      }
    } catch (error) {
      // 404 means the repository or release is not visible anonymously (private
      // repo); anything else is a network or rate-limit problem. Either way the
      // page degrades to the releases page instead of a broken download button.
      const status = (error as { status?: number; statusCode?: number })?.status ??
        (error as { statusCode?: number })?.statusCode
      reason.value = status === 404 ? 'not-found' : 'network'
      failed.value = true
    } finally {
      loading.value = false
    }
  })

  return { release, failed, reason, loading }
}

export function formatSize(bytes: number): string {
  if (!bytes) return ''
  const mb = bytes / 1024 / 1024
  return mb >= 10 ? `${Math.round(mb)} MB` : `${mb.toFixed(1)} MB`
}

export function formatDate(iso: string): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
}
