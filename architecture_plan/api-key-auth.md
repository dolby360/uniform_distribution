# API Key Authentication

## Summary

Added shared-secret API key authentication to all Cloud Function endpoints to prevent unauthorized access and protect against GCP billing abuse. Includes email alerting on unauthorized access attempts via Cloud Monitoring.

## Motivation

All endpoints were deployed with `--allow-unauthenticated` and no auth checks. Anyone discovering the Cloud Run URLs could spam expensive Gemini Vision and Vertex AI embedding calls, running up the GCP bill.

## Architecture

### Request Flow
```
Android App
  → OkHttp interceptor adds X-API-Key header
  → Cloud Run (still allow-unauthenticated)
  → @require_api_key decorator checks header
  → 401 if missing/wrong (logs warning for alerting)
  → Handler executes if valid
```

### Alert Flow
```
Unauthorized request → auth.py logs warning
  → Cloud Logging captures log
  → Log-based metric counts occurrences
  → Monitoring alert fires if count > 0 in 5-min window
  → Email sent to configured address
```

## Files Modified

| File | Change |
|------|--------|
| `backend/auth.py` | New — `require_api_key` decorator with timing-safe comparison |
| `backend/main.py` | Applied decorator to all 6 handlers, added `X-API-Key` to CORS |
| `backend/functions/main.py` | Applied decorator to all 8 handlers, added `X-API-Key` to CORS |
| `backend/deploy.sh` | Added `API_KEY` env var to all 7 deploy commands |
| `backend/.env` | Added `API_KEY` for local development |
| `android/app/build.gradle.kts` | Added `buildConfigField` reading API_KEY from local.properties |
| `android/local.properties` | Added `API_KEY` value (gitignored) |
| `android/.../di/NetworkModule.kt` | Added OkHttp interceptor for X-API-Key header |
| `infra/main.tf` | Added monitoring API, log metric, notification channel, alert policy |
| `infra/variables.tf` | Added `alert_email` variable |
| `infra/terraform.tfvars` | Added alert email address |

## Key Details

- API key is a 32-byte random token generated via `secrets.token_urlsafe(32)`
- Comparison uses `hmac.compare_digest` to prevent timing attacks
- CORS preflight (OPTIONS) requests pass through without auth check
- Alert auto-closes after 30 minutes
- The key is stored in `local.properties` (Android, gitignored) and `.env` (backend, gitignored)

## Deployment

1. Set `API_KEY` environment variable before running `deploy.sh`
2. Run `terraform apply` in `infra/` to create monitoring resources
3. Verify email notification channel in GCP Console (may require confirmation)
