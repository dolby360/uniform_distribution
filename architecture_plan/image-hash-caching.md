# Hash-Based Image Caching & Background Sync

## Summary and motivation

The wardrobe grid re-downloaded every thumbnail on every launch. Root cause:
`list-items` returns **V4 signed URLs** that carry a fresh signature (and expiry)
on every request, so Coil's default URL-keyed cache never hits.

The fix is content-addressed caching. The backend returns a stable `image_hash`
(the Cloud Storage object's base64 MD5) per item. The app caches images by that
hash using Coil's `diskCacheKey`/`memoryCacheKey`, so thumbnails load instantly
from disk on later launches and only re-download when content actually changes.
The last list is also persisted so the grid renders immediately on launch, while
a background sync refreshes it. A small bottom-left spinner becomes a checkmark
when sync completes (cloud-off icon on failure).

## Schema/data model changes

`list-items` response item gained `image_hash`:
```json
{ "id", "image_url", "image_hash", "wear_count", "last_worn", "days_since_worn" }
```
`image_hash` is the GCS base64 MD5 of the item's first image (or `null` if
missing). No Firestore change — hashes are read live from GCS object listing.

## Architecture and data flow

On launch (`ItemsListViewModel`):
1. `WardrobeStore.load()` → render the last-saved list instantly. Images resolve
   from Coil's disk cache (keyed by hash) with no network.
2. `sync()`:
   - `repository.listItems()` → render fresh list; `WardrobeStore.save()`.
   - `prewarmImages()` enqueues a Coil request per item (both tabs) so every
     thumbnail is cached by the time the checkmark shows; unchanged hashes are
     served from disk, changed/new ones downloaded once.
   - status = SYNCED (✓); on failure keep stale data, status = FAILED.

Image requests (`ViewModel.buildImageRequest`): `data = signed image_url`, but
`memoryCacheKey = diskCacheKey = image_hash`. The Application configures a
singleton Coil `ImageLoader` (256 MB disk cache, `respectCacheHeaders(false)` so
the signed URLs' headers don't disable caching).

## Why Coil instead of a hand-rolled file cache

An earlier iteration downloaded/pruned files manually in the ViewModel. Coil
does this better: parallel fetches, automatic downsampling to the grid size,
memory + disk tiers, and dedup — all keyed by hash. Far less code, faster warmup.

## Files modified

| File | Change |
|------|--------|
| `backend/storage/storage_client.py` | Added `_blob_path()` + `get_hashes_by_url(prefix)` (one `list_blobs` pass → gs URL→MD5 map) |
| `backend/functions/list_items.py` | Add `image_hash` per item from the hash map; signing stays local/per-item |
| `android/.../data/model/Models.kt` | `ItemListEntry.image_hash: String?` (nullable, back-compat) |
| `android/.../UniformDistApp.kt` | Implements `ImageLoaderFactory`: 256 MB disk cache, ignore cache headers |
| `android/.../di/AppModule.kt` | Provides the singleton Coil `ImageLoader` |
| `android/.../data/cache/WardrobeStore.kt` | **New.** Persists `ListItemsResponse` as JSON for instant launch |
| `android/.../ui/screens/itemslist/ItemsListViewModel.kt` | Stale-while-revalidate; `SyncStatus`; `buildImageRequest()` (hash cache keys); `prewarmImages()` |
| `android/.../ui/screens/itemslist/ItemsListScreen.kt` | Cells load via `buildImageRequest`; `SyncIndicator` (spinner→✓→cloud-off) bottom-left |

## Performance notes

- Initial backend attempt did one GCS metadata GET per image (~2.9 s warm). The
  single-listing `get_hashes_by_url` removes that overhead.
- Back-compat: if `image_hash` is null (e.g. backend not yet redeployed), Coil
  falls back to URL-keyed caching — correct, just not cross-launch persistent.

## Deployment note

The `list-items` Cloud Function must be redeployed for `image_hash` to appear.

## Related commit hash(es)

- `cb1314a` — Add hash-based image caching with stale-while-revalidate sync
