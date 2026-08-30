# Nginx configuration

`nginx.conf` is the version 1 reference web/gateway server configuration. The web image installs it as `/etc/nginx/conf.d/default.conf` so the `server` block is loaded inside Nginx's `http` context.

It is intended to:

- serve the compiled React application from `/usr/share/nginx/html`,
- proxy `/api/` requests to the internal backend service at `backend:8080`,
- provide SPA fallback to `index.html`,
- cache fingerprinted frontend assets aggressively while keeping `index.html` non-cacheable,
- enable response compression for common text formats,
- add a small baseline of HTTP security headers.

The backend hostname is intentionally the Docker Compose service name planned for the reference deployment. The frontend should use same-origin `/api/...` URLs, so normal browser traffic does not require CORS configuration.

TLS is not configured in this file. A self-hosted deployment can terminate TLS in Nginx or in an upstream reverse proxy/ingress according to the deployment environment.
