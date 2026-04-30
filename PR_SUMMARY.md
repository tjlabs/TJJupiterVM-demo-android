# PR Summary

## What Changed

- Reworked `README.md` to follow the structure style of `TJJupiter-demo-android` while keeping VM demo-specific content concise.
- Simplified setup guidance to avoid `gradle.properties`-driven SDK version config and allow direct values in Gradle files.
- Added automatic README version sync on `preBuild`:
  - Jupiter SDK version is synced from `app/build.gradle.kts` (`jupiterSdkVersion`)
  - Jupiter VM SDK AAR name is synced from `app/build.gradle.kts` (`jupiterVmAarName`)
- Switched app dependency wiring for AAR delivery use-case:
  - VM SDK from local AAR in `app/libs`
  - Jupiter SDK from JitPack dependency
- Added a demo UI/action for hardcoded vacant parking updates:
  - New button: `빈주차 업데이트`
  - Applies fixed vacant parking states to predefined parking IDs
  - Handles SDK method signature variance by trying `(levelId, map)` then `(map)` fallback.

## Key Files

- `README.md`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/com/tjlabs/tjjupitervm_demo_android/MainActivity.kt`

## Behavior Notes

- README version blocks are updated automatically during `preBuild`.
- For VM AAR updates, only `jupiterVmAarName` and the AAR file in `app/libs` need to be changed.
- Hardcoded vacant parking demo uses:
  - `parkingLevelId = 52`
  - `OB-1h82101id68tx3548`
  - `OB-1h7zbmxfa10z93809`
  - `OB-1h84se62jidlw3811`

## Verification

- `./gradlew :app:preBuild` succeeded
- `./gradlew :app:compileDebugKotlin` succeeded
