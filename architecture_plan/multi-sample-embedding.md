# Feature: Multi-Sample Embedding Support

## Summary

When a user confirms a garment match, the system appends the new photo's embedding and cropped image to the existing garment's reference data. This enriches the garment with multiple visual samples, improving future matching accuracy across different angles, lighting conditions, and contexts.

## Motivation

The original system stored a single embedding and single image per clothing item. Matching relied on one reference photo, making it brittle when the garment appeared in different conditions.

Key scenario: a photo's similarity score falls **below the 0.85 threshold**, so the system reports "no match found" and offers to add as a new item. The user recognizes it IS the same garment and wants to confirm the match. Upon confirmation, the new embedding and cropped image are stored alongside the original, giving the garment multiple reference points for future comparisons.

Even above-threshold confirmations benefit - every confirmation adds a new sample, making the model of each garment more robust over time.

## Schema Changes

### Before (single sample)
```
clothing_items/{id}:
  type: "shirt" | "pants"
  image_url: string              # single Cloud Storage URL
  embedding: number[1408]        # single embedding vector
```

### After (multi-sample)
```
clothing_items/{id}:
  type: "shirt" | "pants"
  image_urls: string[]                                        # list of cropped image URLs
  embeddings: { "0": number[1408], "1": number[1408], ... }   # map of embeddings (max 10)
  wear_count: int
  last_worn: timestamp
  created_at: timestamp
```

Cap: `MAX_SAMPLES = 10` per item.

## Data Flow

```
[User takes photo]
       |
       v
[process_outfit_image]  (backend/functions/process_outfit.py)
  - Gemini detects shirt/pants bounding boxes
  - Crops each item with garment-specific padding
  - Generates 1408-dim embedding via Vertex AI
  - Iterates ALL embeddings per candidate item (lines 80-84)
  - find_most_similar returns best match across all samples
  - Returns: matched=true/false, similarity, embedding, cropped_url
       |
       v
[MatchConfirmationScreen]  (Android)
  - If matched: shows side-by-side (detected vs existing) + similarity %
    - "Confirm Match" or "Add as New"
  - If not matched: shows "New Item Detected"
    - "Add to Wardrobe"
       |
       v
[confirm_match]  (backend/functions/confirm_match.py)
  - Increments wear_count, updates last_worn
  - If new_embedding + cropped_url provided AND len(embeddings) < MAX_SAMPLES:
    - Appends embedding to embeddings map (key = str(len(embeddings)))
    - Appends cropped_url to image_urls list
  - Creates wear_log entry with actual similarity_score
```

## Key Implementation Details

- **Multi-embedding comparison** (`process_outfit.py:80-84`): iterates over ALL embeddings in a garment's `embeddings` map, creating one candidate per embedding. `find_most_similar` returns the best match across all samples for all items.

- **Sample cap** (`confirm_match.py:42`): `if len(embeddings) < MAX_SAMPLES` prevents unbounded growth. `MAX_SAMPLES = 10`.

- **Embedding key scheme**: String numeric keys (`"0"`, `"1"`, `"2"`, ...) in a Firestore map. Next key = `str(len(embeddings))`.

- **No threshold gate on sample addition**: `confirm_match` adds a sample whenever `new_embedding` and `cropped_url` are provided, regardless of similarity score. Both above-threshold and below-threshold confirmations enrich the item.

- **Similarity search** (`embeddings/similarity.py`): cosine similarity via numpy dot product on L2-normalized vectors. Threshold = 0.85.

## Files Modified

### Backend (Python)
| File | Change |
|------|--------|
| `backend/functions/process_outfit.py` | Compare query against all embeddings per item (lines 80-84); return `embedding` in match response |
| `backend/functions/confirm_match.py` | Accept `new_embedding` + `cropped_url`; append sample if under `MAX_SAMPLES=10` |
| `backend/functions/add_new_item.py` | Initialize with `embeddings` map (`{"0": embedding}`) and `image_urls` list |
| `backend/functions/statistics.py` | Read from `image_urls[0]` instead of `image_url` |
| `backend/embeddings/similarity.py` | No change - already works with any number of candidates |

### Android (Kotlin)
| File | Change |
|------|--------|
| `android/.../data/model/Models.kt` | `ConfirmMatchRequest` includes `embedding` and `cropped_url` fields |
| `android/.../data/repository/OutfitRepository.kt` | Pass `embedding` + `croppedUrl` in `confirmMatch()` call |
| `android/.../ui/screens/confirmation/MatchConfirmationViewModel.kt` | Forward `item.embedding` + `item.cropped_url` from match result to repository (line 68-69) |

## Related Commit

`0af5326` - "Add multi-sample embedding support for clothing items"
