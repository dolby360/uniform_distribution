# Removal of Home & Statistics, Log Wear as Main Screen

## Summary and motivation

The Home screen and Statistics screen were removed. The Log Wear screen
(`ItemsListScreen`) already surfaces all the information the user needs — per-item
wear counts and "last worn" labels — so it now serves as the app's main/start
screen. Adding new clothes is still available, demoted to a small floating action
button in the bottom corner.

## Architecture and data flow

### Before

```
Home (start)
├── Camera → Confirmation → Home
├── Statistics → ItemDetail
└── ItemsList (Log Wear) → ItemDetail
```

### After

```
ItemsList / Log Wear (start)
├── [Add-clothes FAB] → Camera → (Manual)Crop → Confirmation → back to ItemsList
└── ItemDetail
```

`HomeScreen` and `StatisticsScreen` (plus `StatisticsViewModel` and their helper
composables) are deleted. `ItemsList` is the `NavHost` start destination.

## Files modified

| File | Change |
|------|--------|
| `ui/navigation/Screen.kt` | Removed `Home` and `Statistics` route objects |
| `ui/navigation/NavGraph.kt` | Start destination → `ItemsList`; removed Home/Statistics destinations; `MatchConfirmation.onDone` now pops back to `ItemsList`; `ItemsList` wired with `onAddClothes` → Camera |
| `ui/screens/itemslist/ItemsListScreen.kt` | Renamed `onBack` param to `onAddClothes`; removed back nav icon; title "Log Wear Manually" → "My Wardrobe"; added a `SmallFloatingActionButton` (AddAPhoto icon) that opens the camera flow |
| `ui/screens/home/` | Deleted |
| `ui/screens/statistics/` | Deleted |

## Key implementation details

- The Camera → Crop → Confirmation flow is unchanged; only its return target moved
  from `Home` to `ItemsList`.
- `ItemDetail` still uses `popBackStack()`, so it correctly returns to `ItemsList`.
- The backend `getStatistics` endpoint is now unused by the Android client but was
  left in place.

## Related commit hash(es)

- `f22316b` — Remove Home and Statistics screens, make wardrobe grid the main screen
