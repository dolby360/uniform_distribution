#!/bin/bash

# Deploy Cloud Functions

set -e

echo "Deploying Cloud Functions..."

# Deploy process-outfit
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

echo "process-outfit deployed"

# Deploy confirm-match
gcloud functions deploy confirm-match \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=confirm_match_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID

echo "confirm-match deployed"

# Deploy add-new-item
gcloud functions deploy add-new-item \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=add_new_item_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID

echo "add-new-item deployed"

# Deploy statistics
gcloud functions deploy statistics \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source=. \
  --entry-point=statistics_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID

echo "statistics deployed"

echo ""
echo "All functions deployed!"
