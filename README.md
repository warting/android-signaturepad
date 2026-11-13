[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.warting.signature/signature-pad/badge.png)](https://maven-badges.herokuapp.com/maven-central/se.warting.signature/signature-pad)

Android Signature Pad
====================

Android Signature Pad is an Android library for drawing smooth signatures. It uses variable width
Bézier curve interpolation based
on [Smoother Signatures](https://developer.squareup.com/blog/smoother-signatures) post
by [Square](https://squareup.com).

![Screenshot](/images/header.png)

## Features

* Bézier implementation for a smoother line
* Variable point size based on velocity
* Customizable pen color and size
* Optional live finger shadow on Android 13+
* Bitmap, SVG and raw signature data export
* Jetpack Compose state API and classic Android Views
* Undo the last stroke and restore saved signatures

## Installation

The latest published version is listed on [Maven Central](https://central.sonatype.com/artifact/se.warting.signature/signature-pad).
The library supports Android 6.0 (API 23) and newer.

### For Gradle users

Declare Maven Central in your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Then, include the library as dependency:

```kotlin
implementation("se.warting.signature:signature-pad:<latest_version>") // Jetpack Compose
```
or
```kotlin
implementation("se.warting.signature:signature-view:<latest_version>") // Android Views
```

## Usage

*Please see the `/app` example app for a more detailed code example of how to use the
library.*

### Jetpack Compose

Hoist the signature state and give the drawing area an explicit size:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import se.warting.signaturepad.SignaturePadView
import se.warting.signaturepad.rememberSignaturePadState

@Composable
fun SignatureForm(onSave: (String) -> Unit) {
    val signatureState = rememberSignaturePadState()

    Column {
        SignaturePadView(
            state = signatureState,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        Row {
            Button(onClick = { signatureState.undo() }) { Text("Undo") }
            Button(onClick = { signatureState.clear() }) { Text("Clear") }
            Button(onClick = { onSave(signatureState.getSignatureSvg()) }) { Text("Save") }
        }
    }
}
```

`rememberSignaturePadState()` preserves signature events across configuration changes using
`rememberSaveable`. Use `isEmpty` and `canUndo()` to inspect the current state, and the
`onStartSigning`, `onSigning`, `onSigned`, and `onClear` callbacks to respond to input.

Existing `onReady` / `SignaturePadAdapter` integrations still work, but the adapter is deprecated.
For new code, use `SignaturePadState` as shown above.

### Android Views

```xml
<se.warting.signatureview.views.SignaturePad
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/signature_pad"
    android:layout_width="match_parent"
    android:layout_height="200dp" />
```

Use `clear()`, `undo()`, `canUndo()`, and the export methods on the view. Register a
`SignedListener` with `setOnSignedListener()` for signing callbacks. See the sample app for
XML attributes and data binding examples.

### Configuration

* `penMinWidth` - The minimum width of the stroke (default: 3dp).
* `penMaxWidth` - The maximum width of the stroke (default: 7dp).
* `penColor` - The color of the stroke (default: Color.BLACK).
* `velocityFilterWeight` - Weight used to modify new velocity based on the previous velocity (
  default: 0.9).
* `clearOnDoubleClick` - Double click to clear pad (default: false)

### Export

Call these methods on `SignaturePadState` or the classic `SignaturePad` view:

* `getSignatureBitmap()` - A signature bitmap with a default white background. You can also customize the image's coloring e.g. `getSignatureBitmap(backgroundColor: Int, penColor: Int)`

* `getTransparentSignatureBitmap(trimBlankSpace: Boolean = false)` - A signature bitmap with a transparent background. Set `trimBlankSpace = true` to crop the bitmap to the signature bounds, removing surrounding blank space. You can also customize the image's coloring, e.g. `getTransparentSignatureBitmap(penColor: Int)`

* `getSignatureSvg()` - A signature Scalable Vector Graphics document. The default SVG stroke is black; use `getSignatureSvg(penColor, backgroundColor)` to override its colors.

Raw event persistence and standalone rendering are available with `@OptIn(ExperimentalSignatureApi::class)`:
`getSignature()` / `setSignature(signature)` save and restore events, while `Signature.toBitmap()`,
`Signature.toTransparentBitmap()`, and `Signature.toSvg()` render without a view. Import the rendering
extensions from `se.warting.signaturecore`. These APIs are experimental.

## Live finger shadow

On Android 13 (API 33) and newer, an optional shadow follows the active pointer.
It is disabled by default and never becomes part of bitmap, SVG, or raw event exports.
Older Android versions and software-rendered canvases keep working without drawing the shadow.

```kotlin
SignaturePadView(
    state = signatureState,
    shadowIntensity = 0.5f, // 0..1; explicitly selects the shadow-enabled overload
    shadowColor = Color.Black,
    shadowAngleDegrees = 50.710594f,
)
```

For Views, use `setShadowIntensity`, `setShadowColor`, and `setShadowAngleDegrees`,
or the XML attributes `shadowIntensity`, `shadowColor`, and `shadowAngleDegrees`.
The Compose and View samples include a switch to try the effect.
The shader is adapted from [Romain Guy's shadow-pointer sample](https://github.com/romainguy/shadow-pointer).

## Development

Use JDK 21 and Android SDK 37. Set `ANDROID_HOME` or `sdk.dir` in your untracked
`local.properties` file. Both ordinary Git clones and linked worktrees are supported.
The published libraries support API 23 and newer; the sample app requires API 24 because of Navigation 3.

```shell
./gradlew check apiCheck :app:assembleDebug
```

`check` runs unit tests, Android lint, and Detekt. Bitmap regression tests use Robolectric's
native graphics renderer; an emulator is not required. `apiCheck` verifies the published API snapshots.

Review dependency updates with the existing Version Catalog Update plugin:

```shell
./gradlew versionCatalogUpdate --interactive
# Review gradle/libs.versions.updates.toml before applying updates.
./gradlew versionCatalogApplyUpdates
./gradlew versionCatalogUpdate --check check apiCheck :app:assembleDebug
```

The catalog check prefers stable releases for dependencies already on stable versions.
Gradle wrapper, settings plugins, and GitHub Actions versions must be checked separately.

The build uses AGP 9's built-in Kotlin support and new DSL. Compose and Parcelize use the same
Kotlin version as the compiler. The View library and sample app use AGP's `legacy-kapt` plugin
for data binding. API checks include both Kotlin and generated Java classes and run as part of `check`.

Release versions come from `v*` Git tags. Development versions use `git describe`, including the
commit and dirty suffix when applicable. To override the publication version, pass
`-PVERSION_NAME=0.3.0-SNAPSHOT` or set `VERSION_NAME` in `local.properties`.
