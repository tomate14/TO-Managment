// Generates frontend/vercel.json from VERCEL_BACKEND_URL in the root .env (the single source of configuration).
// On Vercel there is no nginx: these rewrites take its place, proxying /api/* to the backend and serving the SPA.
// Usage: npm run vercel:config
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDir = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const envFile = resolve(frontendDir, '..', '.env');

function readEnv(path) {
  const values = {};
  for (const line of readFileSync(path, 'utf8').split(/\r?\n/)) {
    const match = /^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/.exec(line);
    if (match) {
      values[match[1]] = match[2].replace(/^["']|["']$/g, '');
    }
  }
  return values;
}

const backendUrl = (process.env.VERCEL_BACKEND_URL ?? readEnv(envFile).VERCEL_BACKEND_URL ?? '').replace(/\/+$/, '');
if (!/^https:\/\/[^\s/]+$/.test(backendUrl)) {
  console.error('VERCEL_BACKEND_URL must be an https origin without a path (set it in the root .env).');
  process.exit(1);
}

const config = {
  $schema: 'https://openapi.vercel.sh/vercel.json',
  framework: 'angular',
  buildCommand: 'npm run build',
  outputDirectory: 'dist/frontend/browser',
  rewrites: [
    { source: '/api/:path*', destination: `${backendUrl}/api/:path*` },
    { source: '/(.*)', destination: '/index.html' },
  ],
};

writeFileSync(resolve(frontendDir, 'vercel.json'), `${JSON.stringify(config, null, 2)}\n`);
console.log(`vercel.json written: /api/* -> ${backendUrl}/api/*`);
