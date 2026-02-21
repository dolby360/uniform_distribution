# Architecture Plan

Documentation for the Uniform Distribution project.

## Directory Structure

### `initial-plan/`

The original project plan (steps 01-09), covering the full system design from GCP setup through Android app development and statistics. Written before implementation as the build roadmap.

| File | Topic |
|------|-------|
| 01-gcp-project-setup.md | GCP project, APIs, service accounts |
| 02-firestore-schema-setup.md | Firestore collections and indexes |
| 03-cloud-storage-setup.md | Cloud Storage buckets and structure |
| 04-gemini-object-detection.md | Gemini Vision for clothing detection |
| 05-vertex-ai-embedding.md | Embedding generation for similarity |
| 06-cloud-function-similarity-search.md | Similarity search logic |
| 07-confirm-and-log-endpoint.md | Confirm match and add-new-item endpoints |
| 08-frontend-backend-integration.md | Android integration overview |
| 08.1 - 08.10 | Android sub-steps (SDK, emulator, scaffold, shell, data, camera, confirmation, tests, e2e, polish) |
| 09-statistics-and-analytics.md | Statistics endpoint and UI |

### Feature Documentation (top-level)

Each significant feature added after initial implementation gets its own `.md` file.

| File | Feature |
|------|---------|
| multi-sample-embedding.md | Multi-sample embedding support for improved garment matching |

### Other

| File | Purpose |
|------|---------|
| private-gcp-config.md | GCP credentials and project config (gitignored) |
