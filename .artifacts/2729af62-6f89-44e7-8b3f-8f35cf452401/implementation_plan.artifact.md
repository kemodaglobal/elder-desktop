# Implementation Plan - Support Android 5 and AS 2025.x Compatibility

The goal is to update the project to support Android 5 (API 21) as the minimum SDK, set the target SDK to 36, and adjust the Android Gradle Plugin (AGP) and Gradle versions for compatibility with Android Studio 2025.x (Meerkat/Otter).

## User Review Required

> [!IMPORTANT]
> - **Target SDK 36**: This is a very new API level. Please ensure your development environment has the Android 16 (Baklava) SDK or similar installed if you plan to build locally.
> - **AGP 9.1.0 / Gradle 9.3.1**: These versions are selected as the "minimum required" for the 2025.x era based on current project state (downgrading from 9.3.1/9.6.1).

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/gradle/libs.versions.toml)
- Lower `agp` version from `9.3.1` to `9.1.0`.

#### [MODIFY] [gradle-wrapper.properties](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/gradle/wrapper/gradle-wrapper.properties)
- Lower Gradle version from `9.6.1` to `9.3.1`.

#### [MODIFY] [gradle.properties](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/gradle.properties)
- Set `elder.targetSdk` property to `36`.

### Module Updates

For each module (`:app`, `:eldercalculator`, `:eldercamera`, `:elderweather`):

#### [MODIFY] [build.gradle.kts (all modules)](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/build.gradle.kts)
- Set `minSdk` to `21`.
- Set `targetSdk` (and the fallback in the `customTargetSdk` logic) to `36`.
- Set `compileSdk` to `36`.
- Set `buildToolsVersion` to `36.0.0`.
- Adjust or remove `compileSdkMinor` if it was set to `1` (unless specific 36.1 support is needed).

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project still builds with the lower versions.
- Run `./gradlew check` to verify basic project health.

### Manual Verification
- Verify that the IDE syncs correctly with the new Gradle and AGP versions.
- Check that `minSdk` 21 is correctly applied in the merged manifest.
