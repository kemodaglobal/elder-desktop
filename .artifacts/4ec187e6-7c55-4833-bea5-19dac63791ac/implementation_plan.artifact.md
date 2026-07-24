# Implementation Plan - Root Detection and Warning

Implement a root check that displays a warning to the user if the device is rooted, explaining the risks for elderly users. The app will remain functional after the warning is acknowledged.

## User Review Required

> [!IMPORTANT]
> The root check is a basic heuristic (checking for `su` binaries and `test-keys`). It may not detect all root methods (like some Magisk configurations), but it covers common scenarios.

## Proposed Changes

### Core Logic

#### [NEW] [RootUtils.kt](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/src/main/java/com/elderdesktop/util/RootUtils.kt)
- Implement `isDeviceRooted()` function to check for `su` binaries and `test-keys`.

#### [MODIFY] [DesktopSettings.kt](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/src/main/java/com/elderdesktop/DesktopSettings.kt)
- Add `rootWarningAcknowledged` boolean property to persist whether the user has seen and dismissed the warning.

### UI Components

#### [MODIFY] [strings.xml](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/src/main/res/values/strings.xml)
- Add strings for root warning title, message, and button.

#### [NEW] [RootWarningDialog.kt](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/src/main/java/com/elderdesktop/ui/RootWarningDialog.kt)
- Create a Compose `AlertDialog` to display the root warning.

#### [MODIFY] [Launcher.kt](file:///C:/Users/vmlim/AndroidStudioProjects/ElderDesktop/app/src/main/java/com/elderdesktop/launcher/Launcher.kt)
- Add logic in `onCreate` to check for root and set a state variable to show the dialog if needed.

## Verification Plan

### Automated Tests
- None planned as root detection is device-dependent.

### Manual Verification
- Test on a non-rooted device: Ensure no warning appears.
- Test on a rooted device (or emulator with root): Ensure the warning appears and can be dismissed.
- Verify that once dismissed, the warning does not reappear on subsequent app launches.
