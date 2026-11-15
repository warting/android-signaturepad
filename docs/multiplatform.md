# Multiplatform implementation

`signature-engine` contains portable input events, single-pointer gesture handling, curve
smoothing, velocity-dependent widths, editable signature history, and SVG export. The existing
Android `signature-core` API delegates gesture handling, smoothing, and SVG generation to it.
Android Parcelable models and the existing Android library coordinates remain available.

`signature-compose` renders the same curves with native Compose Canvas on Android, iOS,
desktop and WebAssembly. Its API is in `se.warting.signaturecompose`; existing
`se.warting.signaturepad` consumers do not need to migrate.

```kotlin
val state = rememberSignaturePadState()
SignaturePad(modifier = Modifier.fillMaxWidth().height(220.dp), state = state)
// state.undo(), state.redo(), state.clear()
val savedEvents = state.events
state.restore(savedEvents)
val svg = state.toSvg(width = 640, height = 220)
```

Coordinates and pen widths in the portable API are density-independent logical units.
Applications own persistence of `state.events`; `rememberSignaturePadState()` currently retains
state across recompositions, not process recreation. SVG dimensions use the same logical units.
The existing Android API continues using pixels and its existing saveable state behavior.

## Run the examples

Use JDK 21, Android SDK 37, and Xcode for iOS. The iOS sample targets ARM64 devices and
Apple Silicon simulators. Kotlin downloads its pinned Node/Yarn tools for web builds.

```shell
./gradlew :sample-android:assembleDebug
android run --device emulator-5554 --apks sample-android/build/outputs/apk/debug/sample-android-debug.apk
./gradlew :sample-multiplatform:run
./gradlew :sample-multiplatform:wasmJsBrowserDevelopmentRun
open sample-ios/SignatureSample.xcodeproj
```

The committed Xcode project is generated from `sample-ios/project.yml` with XcodeGen.
Its build phase compiles and embeds the Kotlin framework. If editing the project definition,
run `xcodegen generate --spec sample-ios/project.yml` before committing.

## Verification

```shell
./gradlew :signature-engine:jvmTest :signature-engine:iosSimulatorArm64Test :signature-engine:wasmJsNodeTest
./gradlew :signature-compose:desktopTest :signature-compose:iosSimulatorArm64Test
./gradlew :sample-android:assembleDebug :sample-multiplatform:desktopJar \
  :sample-multiplatform:wasmJsBrowserDevelopmentExecutableDistribution \
  :sample-multiplatform:linkDebugFrameworkIosSimulatorArm64
```

Common UI tests draw through pointer input, then undo, redo, clear and restore the result.
Common model tests cover replay, geometry, SVG stability, secondary pointers, cancellation,
invalid coordinates and history invalidation. Android regression tests continue checking the
legacy APIs against published 0.3.0 artifacts.

## Screenshot regression

The desktop Compose test renders a restored signature and compares every pixel with
`signature-compose/src/desktopTest/reference/signature.png`. Baselines and example screenshots
are stored in Git LFS. CI uploads the actual image with its test reports.

```shell
# Run comparison (also included in check):
./gradlew :signature-compose:desktopTest
# Only for an intentional visual change; inspect the PNG before committing:
UPDATE_SIGNATURE_SCREENSHOTS=true ./gradlew :signature-compose:desktopTest
```

Maven publication includes the engine's JVM/iOS/Wasm artifacts and the Compose library's
Android/desktop/iOS/Wasm artifacts. Release workflows run on macOS so native artifacts are
built on a supported host. No existing Android coordinate or public declaration is removed.
