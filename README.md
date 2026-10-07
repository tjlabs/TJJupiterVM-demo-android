# TJJupiterVM-demo-android

## Overview

TJJupiterVM-demo-android is a minimal Android sample app for integrating **TJLabs Jupiter VM SDK** with Kotlin.

<!-- JUPITER_VM_SDK_VERSION_START -->
Jupiter VM SDK version: 1.0.24
<!-- JUPITER_VM_SDK_VERSION_END -->

The app demonstrates the VM SDK lifecycle step by step:
- Server configuration (`auth(region: ...)` / `authForDevelopment(region: ...)`)
- Authentication (`auth`)
- Service initialize (`initialize`)
- Mock mode apply (`setMockMode`)
- VM frame attach (`configureFrame`)
- VM frame detach (`closeFrame`)
- Service start (`startService`)
- Service stop (`stopService`)
- Parking location APIs (`setSavedParkingLocations`, `updateSavedParkingLocations`, `setParkingLocationStates`, `updateParkingLocationStates`)

## Features

- Permission check and auth flow on launch
- Step-by-step lifecycle controls for `initialize`, `setMockMode`, `configureFrame`, `closeFrame`, `startService`, and `stopService`
- WebView-based VM frame attach/detach flow
- Runtime location and Bluetooth permission request flow
- Mock Mode selector for switching VM scenarios after initialization
- Parking-space tap callback handling
- Hardcoded saved parking / parking-state example after initialization

## Requirements

- Android `minSdk 26+`
- Android Studio (latest stable recommended)
- Kotlin-based Android app

### Required permissions

Declare in `AndroidManifest.xml`:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.BLUETOOTH` (Android 11 and below)
- `android.permission.BLUETOOTH_ADMIN` (Android 11 and below)
- `android.permission.BLUETOOTH_SCAN` (Android 12+)

Runtime permission check in this demo requires:
- Location (`FINE`)
- Bluetooth scan on Android 12+

## Setup

### 1. Add repositories

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Add dependency

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("com.github.tjlabs:TJJupiterVM-sdk-android:$jupiterVmSdkVersion")
}
```

### 3. Open project

Open the project root in Android Studio and let Gradle sync.

## Quick Guide

### 1. Configure credentials

Set your issued credentials in `local.properties`:

```properties
sdk.dir=/Users/your_name/Library/Android/sdk
AUTH_ACCESS_KEY=YOUR_ACCESS_KEY
AUTH_SECRET_ACCESS_KEY=YOUR_SECRET_ACCESS_KEY
```

Input:
- `accessKey: String`
- `accessSecretKey: String`

Output:
- callback `(code: Int, success: Boolean)`

Optionally choose the service `region` **on** the `auth` call. Skip it to use the default (`.SAUDI`). Use `authForDevelopment(...)` for internal DEV / QA; external apps must use `auth(...)` which targets PROD.

```kotlin
// Default region is SAUDI (PROD).
TJJupiterVMAuth.auth(
    application,
    accessKey = "YOUR_ACCESS_KEY",
    secretAccessKey = "YOUR_SECRET_ACCESS_KEY",
    region = TJJupiterVMRegion.KOREA
) { code, success ->
    // handle auth result
}
```

Available regions: `TJJupiterVMRegion.KOREA`, `TJJupiterVMRegion.SAUDI`.

### 2. Launch flow: permissions -> auth

```kotlin
override fun requestAuth() {
    TJJupiterVMAuth.auth(
        application,
        accessKey = "YOUR_ACCESS_KEY",
        secretAccessKey = "YOUR_SECRET_ACCESS_KEY",
        region = TJJupiterVMRegion.KOREA
    ) { code, success ->
        // update UI state
    }
}
```

When the app launches:
- Location and Bluetooth permissions are checked first.
- After all required permissions are granted, auth runs automatically.
- `initialize` becomes enabled only after auth succeeds.

### 3. Step-by-step lifecycle test flow

After auth succeeds, the demo lets you test each stage separately.

1. Tap `initialize`
2. Optionally choose a scenario from `Mock Mode`
3. Tap `configureFrame` if you want to attach the VM web frame to the on-screen container
4. Tap `startService`
5. Tap `stopService`
6. Tap `closeFrame` when you want to detach the frame

`configureFrame` / `closeFrame` and `startService` / `stopService` are intentionally separated so you can verify frame lifecycle and service lifecycle independently.

This demo uses multiple sectors:
- `initialize` can load several sectors at once (e.g. `111`, `112`).
- `configureFrame`, `startService`, and `setMockMode` use the **Active Sector** field on the screen.
- To switch sectors, tap `stopService` and `closeFrame`, change the Active Sector, then tap `configureFrame` / `startService` again. No re-initialization is needed.
- `configureFrame` and `startService` must use the same sector. A different sector fails with `INVALID_SECTOR` (see the SDK README for the rules).

### 4. Initialize service

Input:
- `application: Application`
- `userId: String`
- `sectorIds: List<Int>` (or `sectorId: Int` for a single sector)

Output:
- `onInitSuccess(isSuccess, code)`

```kotlin
vmnaviView.setDelegate(delegate)
vmnaviView.initialize(
    application,
    userId = "vm-test",
    sectorIds = listOf(111, 112)
)
```

Behavior in this demo:
- `initialize` is enabled after successful auth and can be called again when not in progress
- On successful initialization, sample saved parking and parking-state data are applied

### 5. Apply Mock Mode

Input:
- `mode: JupiterMockMode`
- `sectorId: Int`

```kotlin
vmnaviView.setMockMode(JupiterMockMode.VEHICLE_OUTDOOR_PARKING, sectorId)
```

Behavior in this demo:
- `Mock Mode` becomes available after `initialize`
- Mock sector must match the sector you later `configureFrame` / `startService` with
- Available options: `VEHICLE_INDOOR_OUTDOOR`, `VEHICLE_OUTDOOR_PARKING`, `PEDESTRIAN_INDOOR_PARKING`, `PEDESTRIAN_PARKING_INDOOR`, `NONE` (disable)

### 6. Zoom level (optional)

Set WebView zoom range **before** `configureFrame`. Returns `false` without mutating state if validation fails:

```kotlin
val ok = vmnaviView.setZoomLevels(min = 17f, default = 19f, max = 20f)
```

Validation:
- `min >= 0`, `default >= min`, `max >= default + 0.5`, `max <= 24`

Omit this call to let the server-provided `default_position.zoom_level` from the sector bundle drive the WebView zoom range.

### 7. Attach VM frame with `configureFrame`

Input:
- host `FrameLayout`
- `sectorId: Int`

Output:
- `onWebViewSuccess(isSuccess, code)`
- `didWebViewRemoved()`

```kotlin
vmnaviView.configureFrame(vmnaviContainer, sectorId)
```

Behavior in this demo:
- `configureFrame` attaches the VM frame to the dedicated container view
- `closeFrame` removes the attached frame
- Frame attach/detach does not automatically start or stop the service

### 8. Start and stop service

```kotlin
vmnaviView.startService(UserMode.MODE_VEHICLE, sectorId)

vmnaviView.stopService()
vmnaviView.closeFrame()
```

Behavior in this demo:
- `startService` is triggered explicitly by the button
- `stopService` is also triggered explicitly and updates button state in its completion
- Service start/stop is documented separately from frame attach/detach so each SDK step can be tested on its own

### 9. Parking APIs in this demo

Saved parking example:

```kotlin
vmnaviView.setSavedParkingLocations(
    mapOf(PARKING_LEVEL_ID to initParkingLocationIds)
)
```

Parking-state example:

```kotlin
vmnaviView.setParkingLocationStates(
    mapOf(
        PARKING_LEVEL_ID to mapOf(
            "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
            "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.OCCUPIED
        )
    )
)
```

Update parking-state example:

```kotlin
val parkingLevelId = 52
val updates = mapOf(
    "OB-1h82101id68tx3548" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
    "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
    "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.OCCUPIED
)
vmnaviView.updateParkingLocationStates(mapOf(parkingLevelId to updates))
```

Save selected parking location:

```kotlin
vmnaviView.updateSavedParkingLocations(
    mapOf(PARKING_LEVEL_ID to listOf(parkingId))
)
```

Parking-space tap handling (in delegate):

```kotlin
override fun isParkingLocationTapped(levelId: String, parkingLocationId: String) {
    // handle tap
}
```

## License

TJJupiterVM SDK is proprietary software provided by TJLabs under a separate commercial license agreement.


<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation("com.github.tjlabs:TJJupiterVM-sdk-android:$jupiterVmSdkVersion")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
}
<!-- APP_DEPENDENCIES_END -->
