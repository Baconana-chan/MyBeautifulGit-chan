# Website

Landing page and documentation for MyBeautifulGit-chan. This is a separate Nuxt project: it is
not part of the Kotlin/Amper build, and nothing in `shared/`, `desktopApp/` or `androidApp/`
depends on it.

## Requirements

Node `^22.21.0 || ^24.11.0 || >=26.0.0`. Nuxt warns on older 22.x releases, which this machine
currently has (22.18.0) — the build works, but upgrade before relying on it.

## Commands

Run from this directory:

| Command | What it does |
| --- | --- |
| `npm install` | Install dependencies (runs `nuxt prepare`). |
| `npm run dev` | Dev server on http://localhost:3000 with hot reload. |
| `npm run generate` | Build the static site into `.output/public`. |
| `npm run build` | Build the server output (only needed once the site has on-demand routes). |
| `npm run preview` | Serve the last build locally. |

## Why Nuxt

The site has to start static and grow a backend later without a second stack. Nuxt covers both in
one project: pages are prerendered by default, and `server/api/*` files become real endpoints in
the same deploy when that is needed.

Astro was the alternative; Next.js was rejected as too heavy for a landing plus docs site.

## Layout

```
website/
├── app/
│   ├── app.vue                 # titlebar, tabs, statusbar
│   ├── assets/css/main.css     # terminal palette, chrome, prose styles
│   └── pages/
│       ├── index.vue           # landing
│       └── docs/[...slug].vue  # guide + section sidebar
├── content/docs/index.md       # the user guide (moved from ../docs)
├── content.config.ts           # `docs` collection
├── public/favicon.svg
├── server/api/health.get.ts    # example endpoint, not emitted by static generation
└── nuxt.config.ts
```

## Docs content

The guide is plain Markdown in `content/docs/`. Front matter holds `title` and `description`;
every `##` and `###` heading becomes an entry in the page sidebar, and heading ids stay as
generated, so existing `#anchors` inside the guide keep working.

To add a page, drop a `.md` file under `content/docs/` — `content/docs/foo.md` is served at
`/docs/foo`.

## Adding the backend later

`server/api/*` is live in `npm run dev` and in the server output of `npm run build`, but
`npm run generate` writes only pages, so `/api/health` is absent from `.output/public`. That is
expected: the static site has no backend yet.

When a route must run per request rather than at build time:

1. Add a deploy adapter (`nitro.preset`, e.g. `cloudflare-pages` or `node-server`).
2. Mark that route on demand: `export const prerender = false`.
3. Leave every other page prerendered — the rest of the site stays static.

## Next steps

- Add i18n (German) once the English pages are stable; the collection source can become
  `docs/{lang}/**`.
- Point the deploy at `.output/public`.
