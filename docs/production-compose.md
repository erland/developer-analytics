# Production Compose Reference

The canonical production deployment is `deploy/compose.yaml`.

Its application services are image-based:

| Service | Image |
| --- | --- |
| web | `${WEB_IMAGE}:${APP_VERSION}` |
| backend | `${BACKEND_IMAGE}:${APP_VERSION}` |
| worker | `${BACKEND_IMAGE}:${APP_VERSION}` |
| db | `postgres:17-alpine` |

Defaults resolve to the Developer Analytics GHCR packages.

This separation is intentional: release hosts should need Docker/Compose and
deployment configuration, not Node.js, Maven, Java build tooling or the source
tree.

`deploy/compose.local-build.yaml` is a development/CI override and must not be
treated as the production reference.

For reproducibility, production should pin `APP_VERSION` to an immutable release
tag such as `1.2.3` rather than relying on `latest`.
