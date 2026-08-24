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

# Design QA — About screen lower-section redesign

- Source visual truth: `C:\Users\wwwri\.codex\generated_images\01a03217-a826-7240-98d4-8f4a27c4a30e\exec-06ca4bd3-a0d8-4c0f-900b-7d914941bd3e.png`
- Implementation screenshots:
  - Top state: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-redesign-final-top.png`
  - Lower state: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-redesign-final-bottom.png`
- Combined comparison evidence:
  - Full views: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-redesign-comparison-final.png`
  - Focused lower region: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-redesign-comparison-focused-final.png`
- Device viewport: 1224 × 2720 px at 480 dpi, approximately 408 × 907 dp at 3× density
- Source pixels: 853 × 1844 px; implementation pixels: 1224 × 2720 px
- Density normalization: the source and both implementation captures were proportionally scaled to a common 1024 px comparison height. The focused lower-region crops were proportionally scaled to 1200 px height.
- State: Japanese, light theme, Nothing-like UI, internal-development build, update not checked

## Findings

No actionable P0, P1, or P2 differences remain for the selected direction and the explicit icon-free adjustment.

- Fonts and typography: the existing Nothing-style display type remains confined to the preserved version mosaic. The redesigned information and action areas use the app's normal UI typography with clear section-title, body, and action hierarchy. No NType or NDot was added to Material 3-only content.
- Spacing and layout rhythm: `最新情報` and `既知の問題` use 22 dp cards with consistent 20 dp content padding. The final support/log/license group uses compact, evenly divided rows rather than the taller first implementation.
- Colors and visual tokens: cards use the existing `surfaceContainerHigh`, gutters use the screen surface, dividers use `outlineVariant`, and update state uses the active theme primary color. No hard-coded red accent was added.
- Image quality and asset fidelity: the lower redesign contains no raster assets. The existing app icon and upper mosaic were intentionally preserved. Decorative status and row icons from the concept were omitted at the user's request; no substitute glyphs or custom-drawn assets were introduced.
- Copy and content: release notes, the real known-issue text, update status, support, log sharing/clearing, and open-source licenses remain available. Japanese and English update-status/short-action strings are aligned.

## Comparison History

1. The first coded pass used a full-width update button before a status existed and expanded each utility row with a description.
   - Finding: P2 — the update action and utilities were more vertically prominent than the selected release-first concept.
   - Fix: added a truthful `未確認` status beside the compact update button and converted support, debug log, and licenses to compact text-and-action rows.
2. The generated concept used leading status and utility icons.
   - User constraint: no icons.
   - Fix: removed all lower-section icons and chevrons, then retained affordance through explicit text actions and grouped dividers.
3. The checked update state initially wrapped a long success sentence.
   - Finding: P2 — the status competed with the update button at this phone width.
   - Fix: shortened the localized success state to `最新バージョンです` / `Up to date`.

## Primary Interactions Tested

- Scrolled from the preserved version mosaic through the complete redesigned lower section.
- Ran the update check and confirmed the status changed from `未確認` to the latest-version result.
- Confirmed log share/clear enablement still follows whether an app log exists.
- Confirmed the screen compiles and installs on the connected A069 test device without a layout crash.

## Implementation Checklist

- [x] Preserve the existing connected version mosaic.
- [x] Make release information the first lower-section destination.
- [x] Keep known issues readable as a dedicated card.
- [x] Group support, logs, and licenses into an icon-free compact list.
- [x] Preserve all existing actions and saved behavior.
- [x] Verify Japanese light-theme rendering on the physical device.

## Follow-up Polish

The generated direction compresses the whole page into one illustrative viewport, while the production screen keeps accessible Android text sizing and scrolls. This is an intentional implementation constraint rather than a fidelity defect.

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

---

# Design QA — About actions aligned to Settings

- Structural source visual truth: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-settings.png`
- Earlier implementation: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-redesign-final-bottom.png`
- Final implementation screenshots:
  - Top: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-settings-style-top.png`
  - Actions: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-settings-style-bottom.png`
  - Checked update state: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-settings-style-update.png`
- Combined comparison: `C:\Users\wwwri\Desktop\glyphvisualizer\artifacts\glyph-about-settings-style-comparison.png`
- Device viewport: 1224 × 2720 px at 480 dpi, approximately 408 × 907 dp at 3× density
- State: Japanese, Nothing-like UI, update unchecked and checked states

## Findings

No actionable P0, P1, or P2 issues remain for the requested Settings alignment and button clarity.

- Fonts and typography: section labels now use the same Sans Serif, bold, theme-primary treatment as Settings categories. Card titles and descriptions reuse Settings typography hierarchy.
- Spacing and layout rhythm: lower content now uses `SettingsItemSurface`, `SettingsGroupPosition`, and `SettingsDividerGap`, matching Settings card radii, inner junctions, padding, and grouping.
- Colors and visual tokens: primary actions use Material theme buttons, so Nothing mode receives its active Nothing colors and Material 3 mode receives its configured theme color. Log clearing remains outlined as a secondary action.
- Image and asset fidelity: no lower-section icons, custom glyphs, or new image assets were added. The existing app icon and version mosaic remain unchanged.
- Copy and content: each utility restores its explanatory description and places the action inside an unmistakable button. Japanese and English license labels were restored to the explicit `ライセンス一覧を表示` / `Show licenses` wording.

## Comparison History

1. The earlier icon-free list used plain trailing text actions.
   - Finding: P1 — actions could be read as labels, so the tappable target was unclear.
   - Fix: replaced trailing text actions with full-width filled buttons; debug share and clear use paired filled/outlined buttons.
2. The earlier lower group lacked the Settings page's category hierarchy.
   - Finding: P2 — the section felt like a separate design language despite matching colors.
   - Fix: added the existing `情報` category label and reused Settings card primitives for all lower content.
3. The update action was outlined and visually similar to passive card borders.
   - Finding: P2 — the primary action did not stand out enough.
   - Fix: changed it to the same filled primary treatment used by the other main actions.

## Primary Interactions Tested

- Update check changed from `未確認` to `最新バージョンです`.
- Scrolled through the Settings-shaped information group without clipping or horizontal overflow.
- Confirmed log share/clear enabled states remain tied to log availability.
- Installed and rendered the build on the connected A069 device without a layout crash.

## Follow-up Polish

No blocking polish remains. The explicit buttons intentionally make these cards taller than standard navigation-only Settings rows.

final result: passed
