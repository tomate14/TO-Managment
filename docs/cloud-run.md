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
