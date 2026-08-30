# Developer Analytics Frontend

React + TypeScript + Vite frontend for Developer Analytics.

## Commands

```bash
npm install
npm run dev
npm run lint
npm run typecheck
npm test
npm run build
```

The repository pins direct dependency versions in `package.json`. A `package-lock.json` should be generated and committed by running `npm install` in an environment with access to the npm registry before CI is enabled.

## Production container

The production web image is built from the repository root so the build can include both the frontend sources and the shared Nginx configuration:

```bash
docker build -f frontend/Dockerfile -t developer-analytics-web .
```

The Dockerfile uses a Node build stage and copies only the compiled Vite output into the final Nginx image. Nginx listens on port `8080` and proxies `/api/` according to `deploy/nginx/nginx.conf`.

A dependency lock file will be introduced once npm dependency resolution can be executed in a network-enabled build environment; until then the Docker build uses the pinned direct dependency versions in `package.json`.
