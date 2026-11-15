# Optional Jetpack Ink renderer

Add `se.warting.signature:signature-ink:<version>` to opt into the Android Ink renderer.
The existing `signature-pad`, `signature-view` and `signature-core` APIs and default rendering
are unchanged. This new module uses stable Jetpack Ink 1.0.0 and supports Android API 23+.

```kotlin
val state = rememberInkSignatureState()
InkSignaturePad(
    modifier = Modifier.fillMaxWidth().height(240.dp),
    state = state,
)
// state.undo(), state.redo(), state.clear()
```

For Views, create `InkSignatureView(context)` or inflate
`se.warting.signatureink.InkSignatureView` from XML. Both adapters expose the same
`InkSignatureState`. Keep it in a ViewModel to retain signatures across activity recreation;
`rememberInkSignatureState()` alone retains state across recomposition only.

The default brush is pressure-sensitive. Supply a Jetpack Ink `Brush` to customize color,
size or brush family. The renderer passes original MotionEvents, including stylus pressure
and historical samples, to Ink. It requests unbuffered dispatch and supplies predicted events
from AndroidX MotionEventPredictor. Ink manages the low-latency drawing layer and hands
completed strokes to the permanent canvas on the UI thread. Secondary pointers do not sign.
No measured latency improvement is claimed for devices that have not been benchmarked.

`state.isDrawing` stays true until Ink completes its asynchronous stroke handoff. Undo/redo
are disabled while drawing; clear/restore cancel active input and invalidate pending results.
Export waits for handoff to avoid silently losing unfinished ink:

```kotlin
if (!state.isDrawing) {
    val png = state.toPngByteArray(widthPx, heightPx, android.graphics.Color.WHITE)
    val bitmap = state.toBitmap(widthPx, heightPx) // transparent; caller owns/recycles it
}
```

Stroke coordinates and export dimensions are pixels. `state.strokes` returns a snapshot of
native Ink `Stroke` objects; `state.restore(strokes)` restores them without losing brush or
stylus metadata. They are not the old Parcelable `Signature` model and are not automatically
persisted across process death. Applications needing durable storage can use Jetpack Ink's
storage APIs. The existing SVG/legacy-model APIs remain on the original renderers.

## Verification

The sample app's **Try Jetpack Ink** button opens a dedicated capture screen. Instrumentation
tests run on an actual API 34 emulator and verify gesture handoff, undo/redo, cancelling an
active gesture with Clear, pressure-dependent stroke width, PNG decoding and exact pixel
comparison against `app/src/androidTest/assets/reference/ink-signature.png` (Git LFS).

```shell
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=se.warting.signaturepad.app.InkSignatureTest
```

Use a dedicated emulator; this Gradle command runs on connected test devices. To intentionally
regenerate the baseline, pass `-Pandroid.testInstrumentationRunnerArguments.updateInkScreenshots=true`,
pull `/sdcard/Android/data/se.warting.signaturepad/files/ink-screenshots/signature.png`, inspect it,
and replace the committed reference. CI always compares; it never updates the baseline.
