# Uniform Distribution — Design Instructions

## Overview

**Platform:** Android mobile app
**Purpose:** A wardrobe tracking app that uses AI/camera to recognize clothing items (shirts and pants), log what you wear each day, and show statistics about your wearing habits — helping you distribute wear evenly across your wardrobe.

## Design Style

- Clean, modern, gender-neutral
- Neutral color palette — slate blues, cool grays, and muted teal accents
- No pink, purple, or pastel tones
- Utilitarian and minimal, like a fitness tracker or productivity app
- Material 3 / Material You design language
- Card-based layouts with generous whitespace
- Standard Material 3 typography scale

## Color Palette

| Role       | Light Mode              | Dark Mode                |
|------------|-------------------------|--------------------------|
| Primary    | Slate blue (#1A73E8) or teal (#2D7D9A) | Lighter variant (~#8AB4F8) |
| Secondary  | Cool gray (#5F6368)     | Light gray (#9AA0A6)     |
| Tertiary   | Muted charcoal or dark teal | Soft teal              |
| Background | White / off-white       | Dark gray / near-black   |
| Surface    | White                   | #1E1E1E                  |
| Error      | Red (#D93025)           | Light red (#F28B82)      |

**No pink, purple, or warm pastels anywhere.**

## Navigation Flow

```
Home ──→ Camera ──→ Match Confirmation ──→ Home
Home ──→ Statistics ──→ Home (back)
```

---

## Screens

### Screen 1: Home Screen

The landing screen. Simple and inviting.

**Layout:**
- **Top app bar** with title "Uniform Distribution"
- **Body text** (centered): "Take a photo of your outfit to log what you're wearing today."
- **Outlined button** (centered below text): "View Statistics"
- **Floating action button** (bottom-right corner): Camera icon

**Notes:**
- Minimal content, lots of whitespace
- The FAB is the primary action (taking a photo)
- "View Statistics" is secondary

---

### Screen 2: Camera Screen

Full-screen camera for capturing outfit photos.

**Layout:**
- Full-screen **live camera preview** (fills entire screen)
- **Back arrow** in top-left corner (semi-transparent background)
- **Circular capture button** at bottom center (large, prominent, white ring with filled center)

**States:**
- **Capturing:** After tap, brief shutter animation
- **Processing:** Semi-transparent dark scrim overlays the entire screen. Centered card with:
  - Circular progress spinner
  - Text: "Analyzing outfit..."
- **Permission denied:** Centered message "Camera permission required" with a button "Open Settings"

---

### Screen 3: Match Confirmation Screen

Shows AI detection results and lets the user confirm or add items.

**Layout:**
- **Top app bar:** "Confirm Outfit" with back arrow
- **Two vertical sections**, one for Shirt and one for Pants:

**Each section contains:**
- Section header: "Shirt" or "Pants"
- A **cropped image** of the detected clothing item (square, 100x100dp, rounded corners)

**If matched to existing item:**
- Side-by-side: cropped image (left) → matched item image (right)
- Similarity badge: "92% match"
- Two buttons in a row:
  - "Confirm Match" (filled/primary button)
  - "Add as New" (outlined button)

**If new item (no match found):**
- Cropped image with label "New Item Detected"
- Single button: "Add to Wardrobe" (filled button)

**If not detected:**
- Gray text: "No shirt detected" or "No pants detected"
- Auto-completed (no action needed)

**Error state:**
- Error card with message and "Retry" button

**Completion:**
- Once both shirt and pants are handled, auto-navigates back to Home

---

### Screen 4: Statistics Screen

Dashboard showing wardrobe analytics.

**Layout:**
- **Top app bar:** "Wardrobe Statistics" with back arrow
- **Pull-to-refresh** enabled
- **Scrollable vertical content** with 16dp padding

**Sections (top to bottom):**

#### 1. Totals Card
- Full-width card with primary container background color
- Large text: "Total Items: 15"
- Subtitle: "8 shirts, 7 pants"
- Subtitle: "Total wears: 45"

#### 2. Most Worn (section title + item list)
- Section title: "Most Worn"
- Up to 5 **ItemStatCards** (see component below)
- If empty: gray text "No items yet"

#### 3. Least Worn (section title + item list)
- Section title: "Least Worn"
- Up to 5 ItemStatCards
- If empty: gray text "No items yet"

#### 4. Not Worn 30+ Days (conditional — only shown if items exist)
- Section title: "Not Worn (30+ days)"
- ItemStatCards with additional "Last worn: 45d ago" or "Never worn" subtitle

#### 5. Wear Frequency Chart
- Section title: "Wear Frequency (30 days)"
- **Bar chart** inside a card:
  - One bar per day (only days with data)
  - Count labels above each bar
  - Primary color bars
  - 160dp height
  - If no data: centered text "No wear data for the last 30 days"

**States:**
- **Loading:** Centered circular progress indicator
- **Error:** Centered "Error loading statistics" with error detail text and "Retry" button

---

## Shared Components

### ItemStatCard
Horizontal card displaying a single clothing item with stats.
- **Left:** 72dp square image (rounded corners, cropped)
- **Right (stacked vertically):**
  - Item type: "Shirt" or "Pants" (title style)
  - "Worn 12 times" (body style, muted color)
  - Optional: "Last worn: 5d ago" or "Never worn" (small text, muted)
- Card has subtle elevation (2dp shadow)
- Full width, 12dp internal padding

### ItemCard
Simple thumbnail card for displaying an item.
- 120dp width
- Square image (100x100dp, cropped, rounded corners)
- Label text below image
- Used in the confirmation screen

### LoadingOverlay
Full-screen overlay shown during processing.
- Semi-transparent dark scrim (50% opacity black)
- Centered card containing:
  - Circular progress indicator
  - Message text (e.g., "Analyzing outfit...", "Loading...")

### WearFrequencyChart
Custom-drawn bar chart for wear frequency data.
- Rendered inside a Material card
- Canvas-drawn bars using primary theme color
- Count labels above bars (gray, small text)
- Responsive to number of data points
- 160dp total height
- Empty state: "No wear data for the last 30 days"

---

## Design Principles

1. **Neutral and utilitarian** — no decorative or gendered elements
2. **Card-based** — all content grouped into Material cards
3. **Progressive disclosure** — show loading/error/content states appropriately
4. **Image-forward** — clothing items always shown as photos, not icons
5. **Minimal chrome** — let the content (photos, stats) be the focus
6. **Consistent spacing** — 16dp screen padding, 8dp between cards, 24dp between sections
