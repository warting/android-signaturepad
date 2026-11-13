# CLAUDE.md

## Project Overview

Android Signature Pad is a multi-module Android library for drawing smooth signatures using variable-width Bezier curve interpolation. Published to Maven Central under the group `se.warting.signature`.

## Repository Structure

```
android-signaturepad/
├── app/                    # Demo/sample application
├── signature-core/         # Core signature logic (Bezier curves, SVG, bitmap)
├── signature-pad/          # Jetpack Compose canvas and state (depends on core)
├── signature-view/         # Legacy Android View implementation (depends on core)
├── config/detekt/          # Detekt static analysis configuration
├── scripts/                # Build helper scripts (checksum.sh for CI caching)
├── gradle/libs.versions.toml  # Version catalog
└── .github/                # CI workflows, templates, dependabot
```

### Module Dependency Graph

```
signature-pad (Compose) ──api──> signature-core
signature-view (View)   ──api──> signature-core
```

- **signature-core**: Pure logic — `SignatureSDK`, `Event`, `Signature` data classes, Bezier math, SVG builder. No UI framework dependency beyond Android Canvas/Bitmap.
- **signature-view**: Android `View`-based `SignaturePad` widget. Uses `SignatureSDK` internally. Provides XML attributes via `res/values/attrs.xml`. Uses data binding and AGP's `com.android.legacy-kapt` plugin.
- **signature-pad**: Native Compose `SignaturePadView` canvas with hoisted `SignaturePadState`, created by `rememberSignaturePadState()`. The older adapter API remains available but is deprecated.
- **app**: Demo application showcasing both Compose and View-based usage with fragments.

### Key Packages

- `se.warting.signaturecore` — Core SDK, data models, experimental API annotation
- `se.warting.signaturecore.utils` — Bezier, SVG, TimedPoint utilities
- `se.warting.signatureview.views` — Legacy View widget
- `se.warting.signatureview.utils` — Data binding adapter
- `se.warting.signaturepad` — Compose composable + adapter

## Build System

- **Gradle** with Kotlin DSL (`.gradle.kts` files)
- **Version catalog**: `gradle/libs.versions.toml` for all dependency versions
- **JDK 21** for both the Gradle daemon (`gradle/gradle-daemon-jvm.properties`) and compilation (`jvmToolchain(21)`), with automatic provisioning via Foojay. CI also installs JDK 21.
- **compileSdk 37**, **minSdk 23**
- **Kotlin 2.4.x** with Compose compiler plugin
- **AGP** (Android Gradle Plugin) 9.3.x and Gradle 9.7.x; exact versions live in the version catalog and wrapper properties
- Uses AGP's built-in Kotlin support and new DSL. The Kotlin compiler version is pinned to match Compose and Parcelize. Data binding uses `com.android.legacy-kapt` because it does not support KSP.
- Dokka documents the published release variant. Do not suppress the `release` source set: this produces empty documentation JARs with built-in Kotlin. Verify `./gradlew dokkaJavadocJar` when changing this setup.

### Common Build Commands

```bash
# Full check (lint + detekt + tests) — this is what CI runs on PRs
./gradlew check apiCheck :app:assembleDebug --stacktrace

# Build the demo app
./gradlew :app:assembleDebug

# Run detekt static analysis only
./gradlew detekt

# Run lint only
./gradlew lint

# Validate binary API compatibility
./gradlew apiCheck

# Update version catalog
./gradlew versionCatalogUpdate
```

### Versioning

Versions are derived from `v*` Git tags using native `git describe`, which supports linked worktrees.
Release tags keep their version without the `v` prefix. Development versions include the commit and
dirty suffix when applicable. Override the publication version with `-PVERSION_NAME=...` or
`VERSION_NAME` in `local.properties`. Signature data version codes retain the tag-based `MMMNNNPPP` format.

## Code Quality & Static Analysis

### Detekt

- Configured in `config/detekt/detekt.yml` with `autoCorrect = true`
- Each module applies detekt independently
- Notable rules: `FunctionNaming` allows PascalCase (for Compose), `MagicNumber` ignores property declarations, `TooManyFunctions` is disabled
- Uses `detekt-formatting` plugin for code style enforcement

### Android Lint

- All modules: `warningsAsErrors = true`, `abortOnError = true`
- Lint baselines at `<module>/lint-baseline.xml`
- SARIF output generated for each module

### Binary Compatibility Validator

- Plugin: `org.jetbrains.kotlinx.binary-compatibility-validator`
- The `app` module is excluded from API validation
- BCV 0.18.x does not discover AGP's built-in Kotlin automatically. The root build registers `apiBuild`, `apiCheck`, and `apiDump` for each library using release Kotlin and Java compilation outputs, and adds `apiCheck` to `check`.
- Run `./gradlew apiCheck` to validate, `./gradlew apiDump` to update API dumps

## CI/CD Workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| `pr.yml` | Pull requests | Runs `./gradlew check apiCheck :app:assembleDebug` |
| `code_quality.yml` | Manual dispatch | Qodana code quality scan |
| `publish.yml` | GitHub release published | Publishes to Maven Central |
| `snapshot.yml` | Push to `main` | Publishes SNAPSHOT to Maven Central |
| `release-management.yml` | Push to `main` + PRs | Drafts releases via release-drafter |
| `auto-merge.yml` | Dependabot PRs | Auto-approves and merges |

## Publishing

- Published to **Maven Central** via the `com.vanniktech.maven.publish` plugin
- Three artifacts: `se.warting.signature:signature-core`, `se.warting.signature:signature-pad`, `se.warting.signature:signature-view`
- Release publishing triggered by GitHub release events
- Snapshot versions (`2.0.0-SNAPSHOT`) published on every push to `main`

## Coding Conventions

- **Language**: Kotlin (no Java source files)
- **Build scripts**: Kotlin DSL exclusively (`.gradle.kts`)
- **Code style**: `kotlin.code.style=official` (Kotlin official formatting)
- **Function naming**: PascalCase allowed for `@Composable` functions (detekt configured for `[a-zA-Z][a-zA-Z0-9]*`)
- **Experimental APIs**: Use `@ExperimentalSignatureApi` opt-in annotation for unstable APIs
- **Parcelize**: Data classes that cross process boundaries use `@Parcelize`
- **Suppress annotations**: Use `@Suppress` or `@SuppressWarnings` for intentional detekt/lint rule overrides

## Testing

- Unit tests: JUnit 4 (`testImplementation`), plus Robolectric native graphics tests for bitmap export and lifecycle regressions
- Instrumented tests: AndroidX Test + Espresso (`androidTestImplementation`)
- Compose UI tests available via `ui-test-junit4`
- Test files located in `src/test/` and `src/androidTest/` per module

## Important Notes for AI Assistants

- Always run `./gradlew check` to validate changes — this is the CI gate
- Do not modify API surfaces of library modules without updating API dumps (`./gradlew apiDump`)
- The `signature-pad` module is the primary consumer-facing module for Compose users
- The `signature-view` module is the legacy View-based API
- The `signature-core` module should remain framework-agnostic (no Compose UI dependencies, only Compose runtime for state)
- Respect the `@ExperimentalSignatureApi` annotation boundary for unstable features
- PRs target the `main` branch
- Detekt auto-correct is enabled — run `./gradlew detekt` to auto-fix formatting issues
