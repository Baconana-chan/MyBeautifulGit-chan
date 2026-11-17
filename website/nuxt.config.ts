export default defineNuxtConfig({
  compatibilityDate: '2026-11-17',

  modules: ['@nuxt/content'],

  css: ['~/assets/css/main.css'],

  app: {
    head: {
      htmlAttrs: { lang: 'en' },
      title: 'MyBeautifulGit-chan',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        {
          name: 'description',
          content:
            'A desktop Git client that shows the real command behind every action and tells you how to undo it.',
        },
      ],
      link: [{ rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg' }],
    },
  },

  // Static output by default. When the site grows a backend, add an adapter and
  // mark only those routes as on-demand with `export const prerender = false`.
  nitro: {
    prerender: {
      crawlLinks: true,
      routes: ['/'],
    },
  },
})
