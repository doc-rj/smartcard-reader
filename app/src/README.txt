Note: src/androidTest/AndroidManifest.xml is not needed as it is created automatically.

The debug and release source folders may be used as follows.

Taken from: http://tools.android.com/tech-docs/new-build-system/user-guide

Possible use case:
 - Permissions in debug mode only, but not in release mode
 - Custom implementation for debugging
 - Different resources for debug mode (for instance when a resource value is tied to the signing certificate).

The code/resources of the BuildType are used in the following way:
 - The manifest is merged into the app manifest
 - The code acts as just another source folder
 - The resources are overlayed over the main resources, replacing existing values.
