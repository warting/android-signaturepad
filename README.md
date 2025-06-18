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
* Bitmap, SVG and Raw-data support

## Installation

Latest version of the library can be found on Maven Central.

### For Gradle users

Open your `build.gradle` and make sure that Maven Central repository is declared into `repositories`
section:

```gradle
   repositories {
       mavenCentral()
   }
```

Then, include the library as dependency:

```gradle
implementation 'se.warting.signature:signature-pad:<latest_version>' // jetpack Compose views
```
or
```gradle
implementation 'se.warting.signature:signature-view:<latest_version>' // legacy android views
```

## Usage

*Please see the `/app` example app for a more detailed code example of how to use the
library.*

1. Add the `SignaturePad` view to the layout you want to show.

```kotlin
var signaturePadAdapter: SignaturePadAdapter? = null

SignaturePadView(onReady = {
    signaturePadAdapter = it
})

Button(onClick = {
    Log.d("", signaturePadAdapter?.getSignatureSvg() ?: "null")
}) {
    Text("Save")
}
```

2. Configure attributes.

* `penMinWidth` - The minimum width of the stroke (default: 3dp).
* `penMaxWidth` - The maximum width of the stroke (default: 7dp).
* `penColor` - The color of the stroke (default: Color.BLACK).
* `velocityFilterWeight` - Weight used to modify new velocity based on the previous velocity (
  default: 0.9).
* `clearOnDoubleClick` - Double click to clear pad (default: false)

3. Get signature data

* `getSignatureBitmap()` - A signature bitmap with a default white background. You can also customize the image's coloring e.g. `getSignatureBitmap(backgroundColor: Int, penColor: Int)`

* `getTransparentSignatureBitmap(trimBlankSpace: Boolean = false)` - A signature bitmap with a transparent background. Set `trimBlankSpace = true` to crop the bitmap to the signature bounds, removing surrounding blank space. You can also customize the image's coloring, e.g. `getTransparentSignatureBitmap(penColor: Int)`

* `getSignatureSvg()` - A signature Scalable Vector Graphics document.
