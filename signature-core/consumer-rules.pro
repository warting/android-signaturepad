# Consumer ProGuard rules for signature-core library
# These rules are automatically applied to apps that use this library

# Keep Event class for Parcelable serialization (used in state restoration)
# The Event class is saved in instance state and needs to be kept to prevent crashes
# when restoring state after process death in minified builds
-keep class se.warting.signaturecore.Event { *; }
