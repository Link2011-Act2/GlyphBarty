# Design QA

- Source visual truth: `C:\Users\wwwri\Desktop\SendAnyWhere用\Screenshot_20260824-031814.png`
- Implementation screenshots:
  - Collapsed: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-home-final.png`
  - Expanded: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-home-log-final-open.png`
  - Normalized comparison: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-log-comparison-final.png`
- Device viewport: 1224 × 2720 px on the connected A069 test device
- Source pixels: 1259 × 840 px
- Implementation pixels: 1224 × 2720 px; the bottom 1224 × 840 px region was cropped and scaled to 1259 × 840 px for equal-size comparison
- State: dark Nothing-style home, visualizer stopped, log collapsed; expanded state was also captured separately

## Findings

No actionable P0, P1, or P2 differences remain for the requested foldout-log placement and behavior.

- Fonts and typography: the existing NType pattern label and Sans Serif control labels remain unchanged. The collapsed affordance adds no new visible copy, matching the sparse target.
- Spacing and layout rhythm: the collapsed chevron occupies the blank strip immediately above the existing footer divider. Footer height, divider position, and control alignment remain consistent with the source region.
- Colors and visual tokens: black background, white chevron, and existing dark-gray dividers match the current home design.
- Image and icon fidelity: the target contains no required raster asset. The affordance uses the existing Material chevron icon rather than a custom-drawn asset.
- Copy and content: the collapsed state is intentionally icon-only. Expanding it reveals the same current status/log content that was previously available in the legacy UI.

## Focused Comparison Evidence

The normalized side-by-side image compares the complete annotated source region with the implementation's bottom region. A separate focused crop was unnecessary because the chevron and footer controls are clearly readable at full comparison size.

## Comparison History

1. Initial implementation added a full-width 38 dp row with a visible `ログ` label and an extra top divider.
   - Finding: P2 — the closed control was more visually prominent than the annotated source and introduced an additional visible band.
   - Fix: removed the label and top divider, retained only a left-aligned chevron in the blank area above the footer, and kept the full row as the touch target.
   - Post-fix evidence: `artifacts/glyph-log-comparison-final.png` shows the handle and footer boundary aligned with the source annotation.

## Primary Interactions Tested

- Open and close the log panel; chevron rotation and vertical expansion both work.
- Enable `レガシーUIを使用`, return to the home screen, and confirm the old UI appears.
- Disable `レガシーUIを使用` and confirm the new UI returns.
- Open `設定 → 開発者向け → 実験機能` and return with the back arrow.

## Implementation Checklist

- [x] Keep the new visualizer-first UI as the default for users without a saved preference.
- [x] Expose the old home/details screens as `レガシーUIを使用`.
- [x] Move experimental tools from the home drawer into the developer section in Settings.
- [x] Add a low-profile collapsible log handle immediately above the footer.
- [x] Verify both collapsed and expanded states on the connected test device.

## Follow-up Polish

No blocking polish remains. The exact expanded-panel maximum height can be tuned later if longer device logs need more room.

final result: passed
