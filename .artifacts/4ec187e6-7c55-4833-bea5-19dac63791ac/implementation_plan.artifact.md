# Fix Compose UI Test JUnit4 Resolution Error

The project is failing to sync because `androidx.compose.ui:ui-test-junit4` cannot be resolved. This is likely due to the dependency being defined in `gradle/libs.versions.toml` without an explicit version, while other related Compose test libraries (like `ui-test-manifest`) have explicit versions, and the Compose BOM might not be correctly applying versions to `androidTestImplementation` configurations in all subprojects.

## Proposed Changes

### Gradle Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/gradle/libs.versions.toml)
- Add a version reference to `androidx-compose-ui-test-junit4` to match the version used by `ui-test-manifest` (`1.11.4`).
- For consistency, I will define a new version variable `composeUiTest` and use it for both test libraries.

## Verification Plan

### Automated Tests
- Run Gradle sync to ensure the error is resolved.
- Run `./gradlew :app:assembleDebug` and `./gradlew :eldercalculator:assembleDebug` (and other modules) to verify the build passes.
