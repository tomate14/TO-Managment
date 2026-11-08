# Backend deployment

- Google Cloud project: `tomanagment`
- Region: `us-west2`
- Service: `to-management-backend`
- URL: https://to-management-backend-556030996910.us-west2.run.app
- Runtime identity: `to-management-backend@tomanagment.iam.gserviceaccount.com`
- Source: `backend/Dockerfile` (Java 21)
- Database: Supabase project `lwwudpwsdktgucihkzrg`, schema `app`
- Connection: Session pooler, port 5432, SSL required
- Health endpoint: `/actuator/health`

The service uses Secret Manager versions for `POSTGRES_PASSWORD`, `JWT_SECRET`,
and `ADMIN_PASSWORD`. Never copy `.env` into the image or source upload.
Non-secret runtime settings are configured on the Cloud Run service.
Since É9, `REGISTRATION_MAX_PER_HOUR` is a required runtime variable (the backend does not start without it).
Since É10, `TRIAL_DURATION_DAYS` is a required runtime variable (the backend does not start without it).

Flyway baseline version 8 was recorded when initializing the hosted schema.
Keep `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false`; future changes use new Flyway
migrations. The initial administrator was created from the local `.env`.

The initial service configuration uses 1 CPU, 512 MiB RAM, 0 minimum instances,
2 maximum instances, concurrency 40, and a JDBC connection pool of 5 per instance.
Public HTTP access reaches Spring Security; application endpoints still require
the application's JWT. CORS currently follows the local `.env` and must include
the production frontend origin when that frontend is deployed.

For subsequent deployments, after authenticating with Google Cloud CLI:

```powershell
gcloud run deploy to-management-backend --source ./backend --project tomanagment --region us-west2
```

This updates the source while retaining existing runtime settings and secret
references. Verify `/actuator/health` after each deployment.

# Frontend deployment

- Service: `to-management-frontend` (same project and region as the backend)
- Source: `frontend/Dockerfile` (Angular build served by nginx)
- nginx serves the app and proxies `/api/*` to the backend, so the browser only talks to the frontend origin.

Runtime settings (non-secret, configured on the service):

| Variable | Value |
|---|---|
| `FRONTEND_PORT` | `8080` (the port Cloud Run sends traffic to) |
| `API_BASE_URL` | the backend service URL, e.g. `https://to-management-backend-556030996910.us-west2.run.app` |
| `NGINX_RESOLVER` | `8.8.8.8` (locally it is Docker's DNS, `127.0.0.11`) |

The frontend origin must be listed in the backend's `CORS_ALLOWED_ORIGINS`: browsers send `Origin` on API
writes even though the request is proxied by the same origin.

Deploy after authenticating with Google Cloud CLI:

```powershell
gcloud run deploy to-management-frontend --source ./frontend --project tomanagment --region us-west2 --allow-unauthenticated
```

The service keeps its runtime settings between deployments. Verify that the home page loads and that
`/api/auth/me` answers 401 through the proxy.
