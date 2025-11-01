# Docker Environment Variables for Frontend

This document describes environment variables for the frontend application when building/running in Docker.

## Build-time Variables

These variables are used during the build process (via `ARG` in Dockerfile) and baked into the production bundle.

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Base URL for the backend API |

**Note:** Since Vite uses build-time environment variables (prefixed with `VITE_`), these values are embedded in the JavaScript bundle at build time. To change them, you need to rebuild the Docker image.

## Usage Examples

### Using docker-compose.yml

```yaml
analytics-frontend:
  build:
    context: ./realtime_analytic_frontend
    dockerfile: Dockerfile
    args:
      - VITE_API_BASE_URL=http://backend.example.com/api
```

### Using docker build

```bash
docker build \
  --build-arg VITE_API_BASE_URL=http://backend.example.com/api \
  -t analytics-frontend \
  ./realtime_analytic_frontend
```

### Using .env file

Create a `.env` file in the frontend directory:

```env
VITE_API_BASE_URL=http://backend.example.com/api
```

**Important Notes:**
1. Frontend environment variables are build-time only (Vite requirement)
2. Changes to environment variables require rebuilding the Docker image
3. For different environments (dev/staging/prod), build separate images with different `--build-arg` values
4. The API URL in production should typically be an absolute URL, not `localhost`

## Production Considerations

For production deployments:
- Use absolute URLs (e.g., `https://api.example.com/api`) instead of `localhost`
- Consider using a reverse proxy (nginx/traefik) to handle routing
- Use environment-specific Docker images rather than runtime configuration
- Consider using a configuration service or API gateway for dynamic configuration

