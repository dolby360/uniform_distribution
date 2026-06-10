# Manual Wear Logging

## Summary and motivation

The original wear-logging path required photographing the outfit (Camera → `/process-outfit` → match → `/confirm-match`). When the user wore something but didn't take a photo, that wear never made it into Firestore, so `wear_count` and `last_worn` drifted away from reality.

This feature adds a no-photo path: from Home, browse all known clothing items in a tabbed grid (Shirts | Pants), tap an item, choose a date (Today / Yesterday / pick date), and log a wear. Behind the scenes it reuses the existing `confirm_match` machinery — no new wear-event schema, no parallel code path.

## Schema/data model changes

No new collections or fields. Two semantic changes inside existing schemas:

- `wear_logs.worn_at` may now hold a user-supplied timestamp instead of strictly server time. `confidence_score` stays at the existing default of `1.0` for manual entries (same as a confirmed match without an explicit score).
- `clothing_items.last_worn` now follows a **monotonic-forward** rule: it only advances when the new wear is more recent than the existing value. Backdating "I wore this last Friday" must not regress `last_worn` if the user wore it more recently. Without this rule, manual backdating would corrupt the "Not Worn (30+ days)" statistic.

`original_image_url` is set to `""` for manual logs.

## Architecture and data flow

```
Home  ──[Log Wear Manually]──▶  ItemsListScreen
                                     │  GET /list-items
                                     ▼
                              [Shirts | Pants]
                              2-col thumbnail grid
                                     │  tap thumbnail
                                     ▼
                              MarkWornDialog
                              Today / Yesterday / Pick date
                                     │  POST /confirm-match
                                     │  { item_id, item_type,
                                     │    original_photo_url: "",
                                     │    worn_at?: ISO-8601 }
                                     ▼
                              Firestore:
                                clothing_items.wear_count += 1
                                clothing_items.last_worn = max(prev, worn_at)
                                wear_logs.add({ worn_at, confidence_score: 1.0,
                                                original_image_url: "" })
                                     │
                                     ▼
                              Snackbar + list refresh
```

Date semantics in the client: "Today" sends `worn_at = null`, letting the backend stamp `SERVER_TIMESTAMP`. "Yesterday" and the date picker send an ISO-8601 timestamp anchored at noon UTC on the chosen day, so timezone math can't drift the wear into a neighbouring date.

## Files modified

### Backend
- `backend/functions/list_items.py` *(new)* — enumerates every clothing item, signs thumbnail URLs, groups by type, sorts each group by `last_worn` desc with never-worn items last.
- `backend/functions/confirm_match.py` — now takes optional `worn_at: datetime`; uses it for the `wear_logs` entry and (only when monotonic-forward) for `clothing_items.last_worn`.
- `backend/functions/main.py` — adds `list_items_handler` (mirroring `statistics_handler`); `confirm_match_handler` now parses optional `worn_at` from the request body via `datetime.fromisoformat`.
- `scripts/deploy.sh` — adds `list-items` to `ALL_FUNCTIONS` and a deploy block (GET, 256 MB, needs `STORAGE_BUCKET` for URL signing).

### Android
- `data/api/ApiConfig.kt` — `LIST_ITEMS_URL` constant.
- `data/api/UniformDistApi.kt` — `listItems()` GET.
- `data/model/Models.kt` — `ItemListEntry`, `ListItemsResponse`; optional `worn_at` on `ConfirmMatchRequest`.
- `data/repository/OutfitRepository.kt` — `listItems()`; `wornAt` parameter on `confirmMatch()`.
- `ui/screens/itemslist/ItemsListScreen.kt` *(new)* — top tabs, 2-column `LazyVerticalGrid`, info-icon corner button to open ItemDetail, snackbar for confirmations.
- `ui/screens/itemslist/ItemsListViewModel.kt` *(new)* — Hilt + Repository; calls `listItems()` on init and after each successful log.
- `ui/screens/itemslist/MarkWornDialog.kt` *(new)* — three-button dialog (Today / Yesterday / Pick date…) with M3 `DatePicker`; future dates disabled.
- `ui/navigation/Screen.kt`, `ui/navigation/NavGraph.kt` — `ItemsList` route, wired from Home.
- `ui/screens/home/HomeScreen.kt` — second `OutlinedButton` ("Log Wear Manually", `Icons.Default.Checklist`) under the existing "View Statistics" button.

## Key implementation details

- **Backend `last_worn` rule**: `confirm_match` reads the doc once before update. If `worn_at` is `None`, it writes `SERVER_TIMESTAMP`. Otherwise it writes `worn_at` only if the existing `last_worn` is null or older. The single read is cheap (one document).
- **Grid sort**: items with a `last_worn` date sort newest-first; never-worn items go to the bottom of the list. Recently-worn items are the most likely candidates for "I wore this today," so they should be at the top.
- **Date anchor**: ISO timestamps for Yesterday / picked date are anchored at 12:00 UTC. The `DatePicker` returns midnight UTC of the selected calendar date, which can land on the previous day in negative-offset timezones; noon avoids that.
- **No future dates**: the date picker's `SelectableDates` rejects future timestamps. (The dialog still allows "Today.")
- **Reuse**: the wear-logging path is the existing `confirm_match` function — no parallel "manual wear" code path. The signed-URL caching and Firestore iteration in `list_items.py` mirrors `statistics.py`.

## Related commit hash(es)

(filled in after commit)
