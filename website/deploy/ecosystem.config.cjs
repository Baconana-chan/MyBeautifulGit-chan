// PM2 config for the Nuxt/Nitro server.
//
// On the VPS, from the `website` directory:
//   npm ci && npm run build
//   pm2 start deploy/ecosystem.config.cjs
//   pm2 save && pm2 startup
//
// Only `npm run build` produces .output/server; `npm run generate` writes static
// files only and has nothing for PM2 to run.
const path = require('node:path')

module.exports = {
  apps: [
    {
      name: 'git-chan-site',
      script: '.output/server/index.mjs',
      // The config lives in deploy/, the app in its parent directory.
      cwd: path.resolve(__dirname, '..'),
      exec_mode: 'fork',
      instances: 1,
      autorestart: true,
      max_restarts: 10,
      min_uptime: '10s',
      max_memory_restart: '350M',
      kill_timeout: 5000,
      env: {
        NODE_ENV: 'production',
        // Loopback only: Caddy terminates TLS and proxies from the internet.
        HOST: '127.0.0.1',
        NITRO_HOST: '127.0.0.1',
        PORT: 4548,
        NITRO_PORT: 4548,
      },
    },
  ],
}
