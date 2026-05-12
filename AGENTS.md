# Agent Guidelines

This repository is an Android/Kotlin project for Glyph visualizer behavior. Keep changes small, reversible, and easy to review.

## Required Checks

- After Kotlin or Compose changes, run `./gradlew.bat :app:compileDebugKotlin`.
- If the compile cannot be run, clearly mention that in the final response.

## Localization

- When changing user-facing UI text, update every supported locale resource.
- Current locale resources include:
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-ja/strings.xml`
- If more `values-*` locale directories are added later, include them in text changes too.
- Keep translations semantically aligned across locales.

## Settings Compatibility

- Do not break existing `SettingsPreferences` compatibility.
- Preserve old preference keys unless there is an explicit migration plan.
- New settings should have safe defaults and should not change existing saved behavior unexpectedly.

## Existing Features

- Ask the user before removing or disabling an existing feature.
- Be especially careful around:
  - Quick Settings tile startup
  - app language settings
  - latency settings
  - Nothing style UI
  - Phone (4a) base indicator behavior
- If a feature is intentionally hidden but may return later, prefer leaving the internal state/service code in place and hiding only the UI.

## Scope Control

- Avoid broad refactors unless they are necessary for the requested change.
- Do not revert unrelated user changes.
- Ask the user before running destructive git commands such as `git reset --hard`.
- Ask the user before deleting unrelated files, even if they look generated or unused.
- Do not delete generated or ignored files unless the user explicitly asks for cleanup.
