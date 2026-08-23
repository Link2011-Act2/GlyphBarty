# Design QA

## Comparison target

- Source visual truth: `C:\Users\wwwri\.codex\generated_images\01a02eb9-0dca-7623-932e-7b9e4b0ac396\exec-717f7d76-4b21-4a97-83c9-415b8a6c1927.png`
- Implementation screenshots:
  - Live: `C:\Users\wwwri\.codex\visualizations\2026\08\23\01a02eb9-0dca-7623-932e-7b9e4b0ac396\glyphvisualizer-audit\30-details-return-system.png`
  - Tune: `C:\Users\wwwri\.codex\visualizations\2026\08\23\01a02eb9-0dca-7623-932e-7b9e4b0ac396\glyphvisualizer-audit\27-details-tune-v1.png`
  - System: `C:\Users\wwwri\.codex\visualizations\2026\08\23\01a02eb9-0dca-7623-932e-7b9e4b0ac396\glyphvisualizer-audit\28-details-system-v1.png`
  - Latency reached from System: `C:\Users\wwwri\.codex\visualizations\2026\08\23\01a02eb9-0dca-7623-932e-7b9e4b0ac396\glyphvisualizer-audit\29-latency-from-system.png`
- Viewport: Nothing Phone (4a), portrait, 1224 x 2720 px at 480 dpi (408 dp logical width).
- Source pixels: 853 x 1844 px; intended 390 x 844 mobile content.
- Implementation pixels: 1224 x 2720 px. CSS size and deviceScaleFactor are not applicable to native Android/Compose.
- Density normalization: the generated source and the native screenshot were opened in one comparison input and fitted to the same visible portrait bounds. Android-owned status and gesture-navigation chrome was excluded from fidelity findings. The implementation intentionally uses the connected device's 408 dp logical width rather than forcing the mock's 390 dp width.
- State: experimental UI enabled, idle/waiting, Japanese locale. Pattern and recording-light values reflect the user's saved device settings.

## Findings

- No actionable P0, P1, or P2 issues remain.
- The Capture section now uses the same `SettingsItemSurface`, `SettingsEntry`, `SettingsToggleEntry`, `SettingsGroupPosition`, and 2 dp `SettingsDividerGap` definitions as the Settings screen. It has three separate surfaces with no divider strokes; the outer and inner corner radii come from the shared settings shape code.
- Live preserves the selected mock's hierarchy: Details header, three functional tabs, Capture stack, legacy live meter, and compact category hint.
- Tune moves sensitivity, response, tone focus, advanced parameters, reset, import, and export into Settings-style surfaces instead of exposing the previous continuous legacy control page.
- System keeps latency and Glyph behavior controls discoverable. The latency entry opens the existing route-aware latency screen.
- Dynamic differences from the visual target (`Spectrum Marker` and `低音インジケーター` instead of the mock's sample values) are expected saved-state content, not design drift.

## Interaction verification

- Switched among Live, Tune, and System and verified the selected underline and content update.
- Opened and dismissed the pattern selector and recording-light selector from their Capture cards.
- Opened Latency from System and returned to Details with Android system back.
- Scrolled Tune and verified the parameter cards and settings-data actions remain reachable without overlap.
- Checked the app process's recent error log after the interaction pass; no matching runtime errors were found.
- Build verification: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, and `:app:assembleDebug` all passed. The arm64 debug APK installed successfully on device `00353363H000532`.

## Required fidelity surfaces

- Fonts and typography: the display title/device name retain the experimental NType face; shared Settings rows use the same Material/Nothing typography as Settings. Japanese labels, descriptions, and numeric meter text are readable without unwanted truncation.
- Spacing and layout rhythm: 20 dp page margins, 22/6 dp shared grouped-card radii, 2 dp shared inter-card gaps, section spacing, and meter proportions match the approved direction. Capture uses spacing and separate surfaces instead of divider lines.
- Colors and visual tokens: black base, shared `surfaceContainerHigh` cards, white foreground, gray secondary copy, and Nothing red selected indicators map to the existing app theme.
- Image quality and asset fidelity: no raster content is required. The functional legacy Compose meter is intentionally retained; standard Material icons are used for navigation, disclosure, play/stop, and expansion affordances.
- Copy and content: Details, tab names, Capture, live meter, latency, parameter labels, and the fixed toggle descriptions are localized in both English and Japanese resources.
- Icons and affordances: back, settings, tabs, Start, chevrons, sliders, switches, and expandable Advanced controls remain clear and use practical touch targets.
- Focused-region evidence: no separate crop was required because the shared full-view comparison renders the Capture cards and their corner/gap treatment at readable size. Tune and System were additionally captured at the same device viewport to verify their denser controls.

## Comparison history

1. The selected source used three independent Capture cards but was generated before the user's final instruction to use the actual Settings UI code.
2. The implementation replaced approximate Capture containers with the shared Settings components and exact shared shape/gap definitions.
3. The first installed implementation was captured in Live, Tune, and System states. The full-view source/implementation comparison found no actionable P0/P1/P2 mismatch; no visual fix loop was required.

## Follow-up polish

- P3: Returning from the separate Latency screen currently restores Details on the Live tab. Preserving the last selected tab could make repeated latency tuning slightly faster, but it does not block the main task or access to the setting.

final result: passed
