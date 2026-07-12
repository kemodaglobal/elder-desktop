# The app's Target SDK documentation
This is the documentation for `targetSdk`, intended to ensure compatibility with various new mobile phones.

As it stands, the required target SDK for mainland China is 30 or higher (with a future requirement of 33 or higher).

Currently, the target SDK for the majority of apps on Google Play is 35 or higher (as the requirement is updated annually).

This app upgrades its target SDK and compile SDK once a year.

## 1. Impact of downgrading to version 36
   - Influence: The system no longer enforces the new restrictions introduced in Android 17 (such as the lock-free MessageQueue implementation, the requirement that dynamically loaded .so files be read-only, and SMS OTP delays).
   - Elderly User Experience: It will not be subject to new restrictions introduced in Android 17—such as the forced orientation lock for large screens and protections regarding LAN device discovery—but it will also miss out on the corresponding security enhancements.
   - Risk: If the device runs Android 17 or higher with `targetSdk=36`, the system will follow Android 16 behavior, potentially resulting in the absence of new system features on the device.
## 2. Impact of downgrading to version 35
   - Influence: 
     - Edge-to-edge layout is not enforced by default; the status bar or navigation bar may obscure your Compose layout.
     - Predictive back animation is not enabled; the back gesture still follows the old logic.
     - The timeout policy for dataSync background tasks is handled according to the legacy version.
   - Elderly User Experience: On the desktop UI, the status bar might overlap (related to the ClockWidget positioning issue you mentioned earlier); you need to handle `WindowInsets` manually, otherwise the display will appear abnormal on Android 15+ devices.
## 3. Impact of downgrading to version 34
   - Influence: 
     - Foreground services must explicitly declare the `foregroundServiceType`; otherwise, newer system versions may throw an exception (though the system does not currently enforce this if `targetSdkVersion` is below 34).
     - The permission policy for exact alarms has been relaxed to match the legacy behavior.
   - Elderly User Experience: Background processes for weather updates and alarm reminders may operate with fewer restrictions, but they are also more likely to be terminated by the system, potentially causing elderly users to miss weather warnings.
## 4. Impact of downgrading to version 33
   - Influence: 
     - Notification permissions are not mandatory, so the system will not prompt the user to allow notifications; consequently, elderly users might not receive weather warning Snackbars.
     - Media permissions have been refined; storage/media access follows the legacy logic.
   - Elderly User Experience: Weather alerts and incoming call notifications may fail silently because they do not follow the new notification permission process.
## 5. Impact of downgrading to version 31
   - Influence:
     - The restriction on launching activities from the background is not enforced, allowing apps to pop up interfaces while in the background (a feature prone to abuse, though it might actually prove useful for elderly users who accidentally trigger actions).
     - Notifications must use standard templates; there are no restrictions on custom notification areas (templates are mandatory only for target API level 31 or higher).
     - Granular control via Bluetooth permission splitting is not taking effect.
     - The SplashScreen behavior follows the legacy implementation.
   - Risk: Custom notification layouts may display incorrectly on newer system versions, but the system maintains compatibility with older styles when `targetSdkVersion` is 31 or lower.
## 6. Impact of downgrading to version 30
   - Influence: 
     - There are no restrictions on package visibility; your launcher can detect all installed apps without needing to declare `<queries>`.
     - Scoped Storage is not fully enforced on Android 10 devices (it can be bypassed by setting `requestLegacyExternalStorage` when `targetSdkVersion` is 29).
   - Elderly User Experience: Relying on the legacy storage model for weather caching and photo album access would be simpler, but it entails greater privacy risks (as the app could traverse external storage).

In accordance with Elder Desktop requirements, the default target SDK is set to 37, unless specific requirements dictate otherwise.