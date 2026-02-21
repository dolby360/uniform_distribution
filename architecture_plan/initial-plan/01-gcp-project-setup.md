# Step 1: GCP Project Setup

## Goal

Initialize GCP project, enable required APIs, configure service accounts, and set up local development environment with credentials.

## Free Tier Budget

> All services in this plan use GCP's **Always Free** tier or Google AI Studio's free tier.
> No billing charges expected for development-scale usage.

| Service | Free Tier Limits | Region Restriction |
|---------|------------------|--------------------|
| **Firestore** | 50K reads/day, 20K writes/day, 20K deletes/day, 1 GB storage | — |
| **Cloud Storage** | 5 GB storage, 1 GB egress/month | us-west1, us-central1, us-east1 only |
| ~~Cloud Functions (1st gen)~~ | ~~2M invocations/mo~~ | **REMOVED — using Cloud Run instead** |
| **Cloud Build** | 2,500 build-minutes/month | — |
| **Cloud Run** | 180K vCPU-sec, 360K GiB-sec, 2M requests/mo | — |
| **Gemini API (AI Studio)** | Free with rate limits (5-15 RPM, ~250-1000 RPD depending on model) | Not available for EU/UK/Swiss users |
| ~~Vertex AI~~ | ~~No free tier — pay per token~~ | **REMOVED from plan** |

> **Why Vertex AI was removed:** Vertex AI (`aiplatform.googleapis.com`) has no always-free tier — it charges per token.
> Instead, we use the **Gemini API via Google AI Studio** (`google-generativeai` library) which has a free tier.
> The Vertex AI API is already enabled on the project but won't be used or billed unless called.

## Current State (Audited 2026-02-14)

> **Real values** are in [`private-gcp-config.md`](../private-gcp-config.md) (gitignored).
> Placeholders below map to that file's "Quick Reference" table.

### Project Details
| Field | Value |
|-------|-------|
| Project Name | Uniform Distribution |
| Project ID | `<PROJECT_ID>` |
| Project Number | `<PROJECT_NUMBER>` |
| Status | ACTIVE |
| Created | 2025-10-29 |
| Auth Account | `<AUTH_EMAIL>` |

### Service Account
| Field | Value |
|-------|-------|
| Display Name | uniform-distribution |
| Email | `<SA_EMAIL>` |
| Key Exists | Yes (created 2025-12-06, expires 2027-12-19) |
| Key ID | `<SA_KEY_ID>` |
| IAM Roles Assigned | **NONE** — roles need to be granted |

### APIs — What Exists vs What's Needed

| API | Status | Free Tier? | Notes |
|-----|--------|------------|-------|
| Cloud Storage (`storage.googleapis.com`) | **ENABLED** | Yes | 5 GB free in US regions |
| Cloud Build (`cloudbuild.googleapis.com`) | **ENABLED** | Yes | 2,500 min/mo free |
| Cloud Datastore (`datastore.googleapis.com`) | **ENABLED** | Yes | See Firestore note below |
| Compute Engine (`compute.googleapis.com`) | **ENABLED** | Yes | Not directly used |
| Cloud Run (`run.googleapis.com`) | **ENABLED** | Yes | **Backend runs here** — 2M requests/mo free |
| IAM (`iam.googleapis.com`) | **ENABLED** | Yes | No usage charges |
| Vertex AI (`aiplatform.googleapis.com`) | **ENABLED** | **NO** | Already enabled but **not used** — no cost unless called |
| Firestore (`firestore.googleapis.com`) | **NOT ENABLED** | Yes | 50K reads/day free — must enable |
| ~~Cloud Functions~~ | **NOT ENABLED** | — | **Not needed** — using Cloud Run instead |

> **Important Note on Firestore vs Datastore:** Datastore API is enabled but Firestore API is not. These are different modes. The project may be in Datastore mode. If the plan requires Firestore (Native mode), the Firestore API must be enabled and a database created. A project can only use one mode per database — check before enabling.

### Cloud Storage Buckets

| Bucket | Location | Notes |
|--------|----------|-------|
| `<PROJECT_ID>_cloudbuild` | US (multi-region) | Auto-created by Cloud Build |
| **App bucket needed** | — | Must be in **us-central1** for free tier |

### What's Missing (TODO)

- [ ] **Enable Firestore API** and create a Firestore database (Native mode)
- [ ] **Grant IAM roles** to the service account (currently has ZERO roles):
  - `roles/datastore.user` (for Firestore)
  - `roles/storage.objectAdmin` (for Cloud Storage)
  - `roles/run.developer` (for Cloud Run deployments)
- [ ] **Create application Cloud Storage bucket** in **us-central1** (free tier region)
- [ ] **Download service account key** locally to `backend/service-account-key.json`
- [ ] **Create `.env` file** with real project values
- [ ] **Get Gemini API Key** from Google AI Studio (free)

---

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│           GCP Project Setup (FREE TIER ONLY)            │
│              <PROJECT_ID> (ACTIVE)                       │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────┐          │
│  │         GCP Project  EXISTS              │          │
│  │  - Name: Uniform Distribution           │          │
│  │  - ID: <PROJECT_ID>                     │          │
│  │  - Number: <PROJECT_NUMBER>              │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │      Enable APIs (all free tier)         │          │
│  │  [x] Cloud Storage API        (free)    │          │
│  │  [x] Cloud Build API          (free)    │          │
│  │  [x] Cloud Datastore API      (free)    │          │
│  │  [x] Cloud Run API            (free)    │          │
│  │  [x] Cloud Run API   (BACKEND RUNS HERE)│          │
│  │  [ ] Firestore API     (NEEDS ENABLING) │          │
│  │                                          │          │
│  │  [x] Vertex AI API  (enabled, NOT USED) │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │    Service Account  EXISTS               │          │
│  │  - Name: uniform-distribution            │          │
│  │  - Key: exists (expires 2027-12-19)     │          │
│  │  - WARNING: IAM Roles: NONE ASSIGNED    │          │
│  │  - Needs:                                │          │
│  │    * roles/datastore.user               │          │
│  │    * roles/storage.objectAdmin          │          │
│  │    * roles/run.developer               │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │   AI: Gemini API via Google AI Studio    │          │
│  │   - FREE tier (5-15 RPM, rate-limited)  │          │
│  │   - Uses google-generativeai library    │          │
│  │   - NOT Vertex AI (which costs money)   │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │   Local Setup  NOT DONE                  │          │
│  │   -> Download SA key to backend/         │          │
│  │   -> Create .env with real values        │          │
│  │   -> Get Gemini API key (free)           │          │
│  └──────────────────────────────────────────┘          │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Data Schemas

N/A (Infrastructure only)

## API Endpoint Signatures

N/A

## File Structure

```
backend/
  .env                           # GCP credentials (gitignored)
  .env.example                   # Template for environment variables
  service-account-key.json       # SA key file (gitignored)
  gcp-config.json                # GCP project configuration
  requirements.txt               # Updated with GCP libraries
```

## Key Code Snippets

### .env.example

```bash
# GCP Project Configuration
GCP_PROJECT_ID=<PROJECT_ID>
GCP_REGION=us-central1
GOOGLE_APPLICATION_CREDENTIALS=./service-account-key.json

# Google AI Studio — Gemini API (FREE tier)
GEMINI_API_KEY=your-api-key-here

# Firestore
FIRESTORE_DATABASE=(default)

# Cloud Storage (must be us-central1 for free tier)
STORAGE_BUCKET=<PROJECT_ID>-app
```

### requirements.txt (updated)

```
google-generativeai==0.3.2       # Gemini API via AI Studio (FREE)
Flask==3.0.0
Pillow==10.1.0
google-cloud-firestore==2.14.0   # Firestore (free tier)
google-cloud-storage==2.14.0     # Cloud Storage (free tier)
firebase-admin==6.3.0
numpy==1.24.3
# NOTE: google-cloud-aiplatform REMOVED — Vertex AI has no free tier
```

### gcp-config.json

```json
{
  "projectId": "<PROJECT_ID>",
  "projectNumber": "<PROJECT_NUMBER>",
  "region": "us-central1",
  "serviceAccount": "<SA_EMAIL>",
  "apis": [
    "firestore.googleapis.com",
    "storage.googleapis.com",
    "run.googleapis.com",
    "cloudbuild.googleapis.com"
  ]
}
```

## Acceptance Criteria

- [x] GCP project created and accessible
- [ ] All required APIs enabled and verified
- [x] Service account created (**but needs IAM roles**)
- [ ] Service account key downloaded and stored locally
- [x] Can authenticate using: `gcloud auth list`
- [ ] Python can import GCP libraries without errors
- [ ] Test script successfully connects to GCP project

## Remaining Setup Instructions

> Steps 1 and 3 (project creation and service account creation) are already done.
> Below are the remaining steps needed.
> Replace `<PROJECT_ID>` and `<SA_EMAIL>` with values from `private-gcp-config.md`.

### 1. Enable Firestore API

```bash
gcloud services enable firestore.googleapis.com --project=<PROJECT_ID>
```

### 2. Create Firestore Database

```bash
gcloud firestore databases create --location=us-central1 --project=<PROJECT_ID>
```

### 3. Grant IAM Roles to Service Account

```bash
# Datastore/Firestore User
gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member="serviceAccount:<SA_EMAIL>" \
  --role="roles/datastore.user"

# Storage Object Admin
gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member="serviceAccount:<SA_EMAIL>" \
  --role="roles/storage.objectAdmin"

# Cloud Run Developer
gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member="serviceAccount:<SA_EMAIL>" \
  --role="roles/run.developer"
```

### 4. Create Application Storage Bucket (us-central1 for free tier)

```bash
gcloud storage buckets create gs://<PROJECT_ID>-app \
  --location=us-central1 \
  --project=<PROJECT_ID>
```

### 5. Download Service Account Key

```bash
gcloud iam service-accounts keys create backend/service-account-key.json \
  --iam-account=<SA_EMAIL>
```

> Note: A key already exists (see `private-gcp-config.md` for key ID). If you still have the JSON file for that key, you can use it directly. Otherwise, create a new one with the command above.

### 6. Get Gemini API Key (FREE)

- Visit https://aistudio.google.com/app/apikey
- Create API key for Gemini access (no billing required)
- Copy to `.env` file

### 7. Configure Environment

```bash
cd backend
cp .env.example .env
# Edit .env with actual values from private-gcp-config.md
```

### 8. Install Dependencies

```bash
pip install -r requirements.txt
```

## Verification

Test GCP connectivity:

```python
# backend/scripts/test_gcp_connection.py
from google.cloud import firestore
from google.cloud import storage
import os

def test_gcp_connection():
    try:
        # Test Firestore
        db = firestore.Client(project=os.getenv('GCP_PROJECT_ID'))
        print("Firestore connection successful")

        # Test Cloud Storage
        storage_client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
        print("Cloud Storage connection successful")

        print("\nAll GCP connections working!")
        return True
    except Exception as e:
        print(f"Connection failed: {e}")
        return False

if __name__ == '__main__':
    test_gcp_connection()
```

Run test:
```bash
python backend/scripts/test_gcp_connection.py
```
