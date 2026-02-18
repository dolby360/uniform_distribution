#!/bin/bash

# Deploy Cloud Function: process-outfit

set -e

echo "Deploying process-outfit Cloud Function..."

gcloud functions deploy process-outfit \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=process_outfit \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=60s \
  --memory=1GB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,GEMINI_API_KEY=$GEMINI_API_KEY,STORAGE_BUCKET=$STORAGE_BUCKET,GCP_REGION=$GCP_REGION

echo "Deployment complete!"
echo ""
echo "Function URL:"
gcloud functions describe process-outfit --region=us-central1 --gen2 --format="value(serviceConfig.uri)"
