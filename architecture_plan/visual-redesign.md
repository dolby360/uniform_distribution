# Visual Redesign: Editorial Wardrobe Look

## Summary and Motivation

A full visual overhaul of the Android app. The previous UI was a near-default Material 3 setup (Google-blue primary, default typography, generic white cards). The redesign gives the app a distinctive "fashion editorial" identity that fits a wardrobe app: warm ivory/ink neutrals with a terracotta accent, serif display typography, and photo-first grid cards.

No backend, schema, or API changes — this is Android UI only.

## Design System

### Color palette (`ui/theme/Color.kt`, `ui/theme/Theme.kt`)

| Role | Light | Dark |
|------|-------|------|
| Background | Ivory `#FAF6F0` | Warm charcoal `#171512` |
| Surface | Warm white `#FFFDF9` | `#1F1C18` |
| Primary | Ink `#2B2722` | Bone `#EAE3D7` |
| Tertiary (accent / CTAs) | Terracotta `#BE5A3C` | Soft terracotta `#E49678` |
| Secondary | Taupe `#7A6F5F` | Sand `#CFC5B4` |

Also adds four wear-recency indicator colors (`WornRecently` green, `WornAWhileAgo` amber, `WornLongAgo` terracotta, `NeverWorn` gray) used for the freshness dots on grid cards — terracotta signals an item is "due for a wear", which matches the app's uniform-distribution goal.

### Typography (`ui/theme/Type.kt`)

Full Material 3 type scale (previously only `bodyLarge` was customized):
- **Display / headline / titleLarge**: system serif, medium weight, tight letter spacing — the editorial "lookbook" voice used for screen titles and stat numbers.
- **Body / labels**: system sans-serif for crisp UI text.

### Shapes (`Theme.kt`)

App-wide shape scale registered on `MaterialTheme`: 8 / 12 / 16 / 20 / 28 dp. Cards use `shapes.large` (20dp); dialogs inherit `extraLarge` (28dp).

## Screen Changes

### Wardrobe grid (`ItemsListScreen.kt`) — main screen
- **Cards**: now photo-first — 3:4 portrait images filling the card, with a bottom gradient scrim and the wear count / last-worn label overlaid in white. A small color-coded recency dot precedes the last-worn label.
- **Tabs**: the stock `TabRow` is replaced by a pill-style segmented control (animated ink pill on a translucent track) that stays synced with the `HorizontalPager`.
- **Top bar**: left-aligned serif `headlineMedium` title on the background color (no surface band).
- **FAB**: `ExtendedFloatingActionButton` ("Add outfit") in terracotta.
- **Loading**: pulsing placeholder card grid instead of a centered spinner.
- **Empty state**: hanger icon (`Icons.Outlined.Checkroom`) + headline + hint copy.
- **Micro-interaction**: cards scale to 0.96 while pressed.
- Grid bottom padding (96dp) keeps the last row clear of the extended FAB.

### Item detail (`ItemDetailScreen.kt`)
- Stat header: two tonal stat cards (wear count, photo count) with large serif numerals.
- Photo cards use the shared 20dp shape; the delete button is a translucent black circle with a white icon (readable over any photo).
- Title simplified to the item type ("Shirt" / "Pants") in serif.

### Smaller touches
- `MainActivity.kt`: `enableEdgeToEdge()` for content drawing behind system bars.
- `LoadingOverlay.kt`: 28dp-radius panel, terracotta spinner.
- `MarkWornDialog.kt`: "Yesterday" upgraded to `FilledTonalButton` for clearer action hierarchy.

## Files Modified

| File | Change |
|------|--------|
| `android/.../ui/theme/Color.kt` | New warm editorial palette + recency colors |
| `android/.../ui/theme/Theme.kt` | New light/dark schemes, app shape scale |
| `android/.../ui/theme/Type.kt` | Full serif/sans type scale |
| `android/.../ui/screens/itemslist/ItemsListScreen.kt` | Photo-first cards, pill tabs, extended FAB, skeleton loading, empty state |
| `android/.../ui/screens/itemdetail/ItemDetailScreen.kt` | Stat cards, restyled photo list |
| `android/.../ui/screens/itemslist/MarkWornDialog.kt` | Button hierarchy tweak |
| `android/.../ui/components/LoadingOverlay.kt` | Shape/color polish |
| `android/.../MainActivity.kt` | Edge-to-edge |

Camera, crop, and match-confirmation screens were intentionally left structurally unchanged — they pick up the new colors, typography, and shapes through the theme.

## Key Implementation Details

- The recency dot thresholds mirror `lastWornLabel`: ≤7 days green, <30 amber, ≥30 terracotta, never-worn gray.
- The pill tab control drives the pager via `animateScrollToPage` and reads `pagerState.currentPage`, preserving the existing swipe-to-switch behavior and ViewModel tab sync.
- Press-scale on grid cards uses `collectIsPressedAsState` + `graphicsLayer` with ripple indication disabled (the scale is the feedback).
- Dynamic color (Material You) remains available but off by default, since it would override the brand palette.

## Related Commit Hash(es)

- (pending — fill in after commit)
