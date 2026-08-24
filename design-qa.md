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

---

# Design QA — About screen refinement

- Source visual truth:
  - Nothing About phone: `C:\Users\wwwri\Desktop\SendAnyWhere用\Screenshot_20260824-150629.png`
  - Layout sketch: `C:\Users\wwwri\Desktop\SendAnyWhere用\Screenshot_20260824-151007.png`
- Implementation screenshot: `C:\Users\wwwri\.codex\visualizations\2026\08\24\01a03217-a826-7240-98d4-8f4a27c4a30e\about-qa\implementation-aligned-version.png`
- Device viewport: 1224 × 2720 px (approximately 408 × 907 dp at 3× density)
- State: Japanese, light theme, Nothing-like UI, internal development channel

## Findings

No actionable P0, P1, or P2 differences remain for the requested header sizing and connected-card treatment.

- Fonts and typography: the screen title now uses the same TopAppBar title treatment as the other settings pages. NType remains limited to the Nothing-like mode.
- Spacing and layout rhythm: the overview starts 18 dp below the app bar content area. Every tile uses the same 20 dp radius on all four corners. The gutters are 3 dp. The two lower tiles are equal-width squares; their shared gutter has a centered bridge, and only the channel tile has a bridge to the hero.
- Colors and visual tokens: the overview uses the existing light surface-container color and app icon asset; no new palette was introduced.
- Image and icon fidelity: the installed adaptive icon is shown at a 92 dp badge size with its original artwork intact.
- Copy and content: app, channel, build, and version values remain fully visible and use the existing localized strings.

## Comparison History

1. The first implementation rendered the app icon too small inside the hero tile.
   - Fix: increased the badge to 92 dp and removed internal image padding.
2. The second implementation used a large content heading and widely rounded, separated cards.
   - User finding: the heading was oversized compared with other settings screens, and the tiles did not feel sufficiently connected.
   - Fix: restored the standard TopAppBar title, reduced the cluster gaps from 4 dp to 2 dp, reduced outer corners from 36 dp to 20 dp, and reduced junction corners from 12 dp to 4 dp.
3. The third implementation made junction corners smaller than exterior corners.
   - User finding: Nothing's source tiles keep the same radius on all four corners and express connection with explicit bridges.
   - Fix: set every tile corner to 20 dp and added centered 48 dp bridges across each relevant horizontal and vertical gutter.
4. The fourth implementation split build and version into separate cards and connected every tile in the cluster.
   - User finding: build and version should share one card; the lower cards should be squares and connect only to each other.
   - Fix: combined build and version into one square card, made the channel card the same square size, removed every hero bridge, and retained one vertical bridge between the lower pair.
5. The fifth implementation left the hero disconnected from both lower cards and used a 2 dp gutter.
   - User finding: the channel tile should connect to the hero, and the gutters should be slightly wider.
   - Fix: increased gutters to 3 dp and added one horizontal hero bridge centered over the channel tile. The build/version tile remains disconnected from the hero.
6. The sixth implementation left a sub-pixel seam where the channel-to-version bridge met its cards and used straight bridge boundaries.
   - User finding: the connection showed a small gap and the exposed gutter ends should be rounded like the Nothing reference.
   - Fix: overlapped each bridge 1 dp into the adjacent cards and added circular gutter caps at both ends of the horizontal and vertical connections.
7. The seventh implementation rendered the version value smaller and slightly lower than the channel value.
   - User finding: `v2.0.0` should match the size and position of `IntDev`.
   - Fix: matched the right card padding to 22 dp and the version value to 25 sp with a 29 sp line height, aligning both values along the same lower baseline.

## Focused Comparison Evidence

The reference, sketch, and full-device implementation were reviewed together at their original resolutions. A separate crop was unnecessary because the complete overview cluster and title are legible in the full-device capture.

## Follow-up Polish

No blocking polish remains. The bridges are decorative overlays; the underlying cards retain independent click targets and ripple clipping.

final result: passed
