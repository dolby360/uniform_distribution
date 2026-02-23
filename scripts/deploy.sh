#!/bin/bash

# Deploy Cloud Functions
# Usage:
#   bash scripts/deploy.sh                            # deploy all functions
#   bash scripts/deploy.sh process-outfit             # deploy one function
#   bash scripts/deploy.sh statistics confirm-match   # deploy specific functions
#   bash scripts/deploy.sh 5+                         # deploy from #5 to end
#
# Functions (by number):
#   1: process-outfit    2: confirm-match      3: add-new-item
#   4: statistics        5: get-item-images    6: delete-item-image
#   7: process-manual-crop

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"

# Load environment variables from .env
if [ -f "$BACKEND_DIR/.env" ]; then
  set -a
  source "$BACKEND_DIR/.env"
  set +a
  echo "Loaded environment from backend/.env"
else
  echo "ERROR: backend/.env not found"
  exit 1
fi

# Verify required variables
for var in GCP_PROJECT_ID GEMINI_API_KEY STORAGE_BUCKET GCP_REGION API_KEY; do
  if [ -z "${!var}" ]; then
    echo "ERROR: $var is not set in .env"
    exit 1
  fi
done

DEPLOYED=0

deploy() {
  local name=$1
  shift
  DEPLOYED=$((DEPLOYED + 1))
  echo ""
  echo "==> [$DEPLOYED/$TOTAL] Deploying $name..."
  gcloud functions deploy "$name" "$@"
  echo "==> [$DEPLOYED/$TOTAL] $name deployed"
}

should_deploy() {
  # If no args given, deploy everything. Otherwise only deploy if name is in args.
  if [ ${#TARGETS[@]} -eq 0 ]; then
    return 0
  fi
  for target in "${TARGETS[@]}"; do
    if [ "$target" = "$1" ]; then
      return 0
    fi
  done
  return 1
}

ALL_FUNCTIONS=(process-outfit confirm-match add-new-item statistics get-item-images delete-item-image process-manual-crop)

# Collect target functions from arguments, expanding N+ ranges
TARGETS=()
for arg in "$@"; do
  if [[ "$arg" =~ ^([0-9]+)\+$ ]]; then
    start=${BASH_REMATCH[1]}
    if [ "$start" -lt 1 ] || [ "$start" -gt ${#ALL_FUNCTIONS[@]} ]; then
      echo "ERROR: Invalid range '${arg}' (valid: 1-${#ALL_FUNCTIONS[@]})"
      exit 1
    fi
    for ((i=start-1; i<${#ALL_FUNCTIONS[@]}; i++)); do
      TARGETS+=("${ALL_FUNCTIONS[$i]}")
    done
  else
    TARGETS+=("$arg")
  fi
done

# Validate targets
for target in "${TARGETS[@]}"; do
  valid=false
  for fn in "${ALL_FUNCTIONS[@]}"; do
    if [ "$target" = "$fn" ]; then
      valid=true
      break
    fi
  done
  if [ "$valid" = false ]; then
    echo "ERROR: Unknown function '$target'"
    echo "Available: ${ALL_FUNCTIONS[*]}"
    exit 1
  fi
done

# Count how many we'll deploy
if [ ${#TARGETS[@]} -eq 0 ]; then
  TOTAL=${#ALL_FUNCTIONS[@]}
  echo "Deploying all $TOTAL functions..."
else
  TOTAL=${#TARGETS[@]}
  echo "Deploying $TOTAL function(s): ${TARGETS[*]}"
fi

should_deploy process-outfit && deploy process-outfit \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=process_outfit \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=60s \
  --memory=1GB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,GEMINI_API_KEY=$GEMINI_API_KEY,STORAGE_BUCKET=$STORAGE_BUCKET,GCP_REGION=$GCP_REGION,API_KEY=$API_KEY

should_deploy confirm-match && deploy confirm-match \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=confirm_match_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,API_KEY=$API_KEY

should_deploy add-new-item && deploy add-new-item \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=add_new_item_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=10s \
  --memory=256MB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,API_KEY=$API_KEY

should_deploy statistics && deploy statistics \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=statistics_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=30s \
  --memory=256MB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,STORAGE_BUCKET=$STORAGE_BUCKET,API_KEY=$API_KEY

should_deploy get-item-images && deploy get-item-images \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=get_item_images_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=30s \
  --memory=256MB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,STORAGE_BUCKET=$STORAGE_BUCKET,API_KEY=$API_KEY

should_deploy delete-item-image && deploy delete-item-image \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=delete_item_image_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=30s \
  --memory=256MB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,STORAGE_BUCKET=$STORAGE_BUCKET,API_KEY=$API_KEY

should_deploy process-manual-crop && deploy process-manual-crop \
  --gen2 \
  --runtime=python311 \
  --region=us-central1 \
  --source="$BACKEND_DIR" \
  --entry-point=process_manual_crop_handler \
  --trigger-http \
  --allow-unauthenticated \
  --timeout=60s \
  --memory=1GB \
  --max-instances=1 \
  --concurrency=1 \
  --set-env-vars GCP_PROJECT_ID=$GCP_PROJECT_ID,GEMINI_API_KEY=$GEMINI_API_KEY,STORAGE_BUCKET=$STORAGE_BUCKET,GCP_REGION=$GCP_REGION,API_KEY=$API_KEY

echo ""
echo "Done! $DEPLOYED function(s) deployed."
