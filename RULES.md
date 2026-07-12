# Elder Desktop Additional Open Source Usage Rules (Not a License Replacement)

This project is licensed under the Apache License 2.0.
The following constitutes **Additional Usage Terms** applicable to all forking, modification, and redistribution activities.  
The goal is to ensure a consistent user experience across old and new Android phones—particularly those used by seniors aged 60 to 80.

## Rules

1. **Adding advertisements is prohibited**：Adding advertisements to any area of ​​the desktop for any reason is prohibited.
2. **Arbitrary modification of minSdk and targetSdk is prohibited.**： 
   - See `build.gradle.kts` for the original minSdk / targetSdk values. 
   - Do not lower the `minSdk`, and do not arbitrarily change the `targetSdk` to circumvent system behaviors (such as permissions or background restrictions). 
   - This project uses Gradle version 9.6.1, Android Gradle Plugin version 9.2.1, and targetSdk 37; therefore, development requires Android Studio Quail 1 or a later version. 
3. **Obligation to disclose compatibility information**：If the `targetSdk` of this open-source software must be adjusted, the impact on new devices must be detailed in the `targetSdk` documentation. 

The author reserves the right to publicly disclose violations of the aforementioned rules—both on moral grounds and within the community—and to remove the violator from the list of contributors. 
This document does not alter the legal effect of the Apache 2.0 License; it serves solely as a behavioral agreement regarding use and contribution.