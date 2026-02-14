# Step 1: GCP Project Setup

## Goal

Initialize GCP project, enable required APIs, configure service accounts, and set up local development environment with credentials.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    GCP Project Setup                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────┐          │
│  │         GCP Console                      │          │
│  │  - Create Project: uniform-distribution │          │
│  │  - Project ID: uniform-dist-XXXXX        │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │      Enable APIs                         │          │
│  │  ✓ Firestore API                        │          │
│  │  ✓ Cloud Storage API                    │          │
│  │  ✓ Cloud Functions API                  │          │
│  │  ✓ Vertex AI API                        │          │
│  │  ✓ Cloud Build API                      │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │    Service Account Creation              │          │
│  │  - Name: uniform-dist-sa                │          │
│  │  - Roles:                                │          │
│  │    • Cloud Datastore User               │          │
│  │    • Storage Object Admin               │          │
│  │    • Vertex AI User                     │          │
│  │    • Cloud Functions Developer          │          │
│  └──────────────────────────────────────────┘          │
│                     │                                   │
│                     ▼                                   │
│  ┌──────────────────────────────────────────┐          │
│  │   Download Service Account Key           │          │
│  │   → backend/.env                         │          │
│  │   → GOOGLE_APPLICATION_CREDENTIALS       │          │
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
  gcp-config.json                # GCP project configuration
  requirements.txt               # Updated with GCP libraries
```

## Key Code Snippets

### .env.example

```bash
# GCP Project Configuration
GCP_PROJECT_ID=uniform-dist-XXXXX
GCP_REGION=us-central1
GOOGLE_APPLICATION_CREDENTIALS=./service-account-key.json

# Google AI Studio (Gemini API)
GEMINI_API_KEY=your-api-key-here

# Firestore
FIRESTORE_DATABASE=(default)

# Cloud Storage
STORAGE_BUCKET=uniform-dist-XXXXX.appspot.com
```

### requirements.txt (updated)

```
functions-framework==3.*
google-generativeai==0.3.2
Flask==3.0.0
Pillow==10.1.0
google-cloud-firestore==2.14.0
google-cloud-storage==2.14.0
google-cloud-aiplatform==1.38.0
firebase-admin==6.3.0
numpy==1.24.3
```

### gcp-config.json

```json
{
  "projectId": "uniform-dist-XXXXX",
  "region": "us-central1",
  "apis": [
    "firestore.googleapis.com",
    "storage.googleapis.com",
    "cloudfunctions.googleapis.com",
    "aiplatform.googleapis.com",
    "cloudbuild.googleapis.com"
  ]
}
```

## Acceptance Criteria

- [ ] GCP project created and accessible
- [ ] All required APIs enabled and verified
- [ ] Service account created with correct permissions
- [ ] Service account key downloaded and stored in backend/.env
- [ ] Can authenticate using: `gcloud auth list`
- [ ] Python can import GCP libraries without errors
- [ ] Test script successfully connects to GCP project

## Setup Instructions

1. **Create GCP Project**:
   ```bash
   gcloud projects create uniform-dist-XXXXX --name="Uniform Distribution"
   gcloud config set project uniform-dist-XXXXX
   ```

2. **Enable Required APIs**:
   ```bash
   gcloud services enable firestore.googleapis.com
   gcloud services enable storage.googleapis.com
   gcloud services enable cloudfunctions.googleapis.com
   gcloud services enable aiplatform.googleapis.com
   gcloud services enable cloudbuild.googleapis.com
   ```

3. **Create Service Account**:
   ```bash
   gcloud iam service-accounts create uniform-dist-sa \
     --display-name="Uniform Distribution Service Account"

   # Grant permissions
   gcloud projects add-iam-policy-binding uniform-dist-XXXXX \
     --member="serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com" \
     --role="roles/datastore.user"

   gcloud projects add-iam-policy-binding uniform-dist-XXXXX \
     --member="serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com" \
     --role="roles/storage.objectAdmin"

   gcloud projects add-iam-policy-binding uniform-dist-XXXXX \
     --member="serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com" \
     --role="roles/aiplatform.user"

   gcloud projects add-iam-policy-binding uniform-dist-XXXXX \
     --member="serviceAccount:uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com" \
     --role="roles/cloudfunctions.developer"
   ```

4. **Download Service Account Key**:
   ```bash
   gcloud iam service-accounts keys create backend/service-account-key.json \
     --iam-account=uniform-dist-sa@uniform-dist-XXXXX.iam.gserviceaccount.com
   ```

5. **Get Gemini API Key**:
   - Visit https://aistudio.google.com/app/apikey
   - Create API key for Gemini access
   - Copy to `.env` file

6. **Configure Environment**:
   ```bash
   cd backend
   cp .env.example .env
   # Edit .env with actual values
   ```

7. **Install Dependencies**:
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
        print("✓ Firestore connection successful")

        # Test Cloud Storage
        storage_client = storage.Client(project=os.getenv('GCP_PROJECT_ID'))
        print("✓ Cloud Storage connection successful")

        print("\n✓ All GCP connections working!")
        return True
    except Exception as e:
        print(f"✗ Connection failed: {e}")
        return False

if __name__ == '__main__':
    test_gcp_connection()
```

Run test:
```bash
python backend/scripts/test_gcp_connection.py
```
