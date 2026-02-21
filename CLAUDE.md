# CLAUDE.md

## Project Overview

Uniform Distribution is a clothing tracking app. Users photograph their outfits; the system detects individual garments (shirts, pants) via Gemini Vision, generates visual embeddings, matches against known items in Firestore, and tracks wear statistics.

## Tech Stack

- **Backend**: Python, Cloud Run (Google Cloud), Firestore, Cloud Storage, Gemini API, Vertex AI embeddings (1408-dim vectors)
- **Android**: Kotlin, Jetpack Compose, Hilt, Retrofit, Coil
- **Infra**: Terraform for GCP resources

## Documentation Convention

**Every significant feature must have an accompanying `.md` file in `architecture_plan/`.**

When implementing a new feature that involves schema changes, new endpoints, architectural decisions, or changes spanning multiple layers (backend + Android), create a file at `architecture_plan/<feature-name>.md` documenting:

1. Summary and motivation
2. Schema/data model changes (before and after)
3. Architecture and data flow
4. Files modified (with brief descriptions)
5. Key implementation details
6. Related commit hash(es)

The initial project plan lives in `architecture_plan/initial-plan/` (steps 01-09).

## Key Directories

| Directory | Contents |
|-----------|----------|
| `architecture_plan/` | Feature docs and initial project plan |
| `backend/` | Python Cloud Run functions |
| `android/` | Native Android app (Kotlin/Compose) |
| `infra/` | Terraform and GCP infrastructure |
| `assets/` | Test images |
