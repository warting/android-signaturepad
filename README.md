Android Signature Pad
====================

Android Signature Pad is an Android library for drawing smooth signatures. It uses variable width
Bézier curve interpolation based
on [Smoother Signatures](http://corner.squareup.com/2012/07/smoother-signatures.html) post
by [Square](https://squareup.com).

![Screenshot](/images/header.png)

## Features

* Bézier implementation for a smoother line
* Variable point size based on velocity
* Customizable pen color and size
* Bitmap and SVG support

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
compile 'se.warting.signature:signature-pad:<latest_version>'
```

## Usage

*Please see the `/SignaturePad-Example` app for a more detailed code example of how to use the
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

3. Get signature data

* `getSignatureBitmap()` - A signature bitmap with a white background.
* `getTransparentSignatureBitmap()` - A signature bitmap with a transparent background.
* `getSignatureSvg()` - A signature Scalable Vector Graphics document.

