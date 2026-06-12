# TJJupiterVM-demo-android

## Overview

TJJupiterVM-demo-android is a minimal Android sample app for integrating **TJLabs Jupiter VM SDK (JitPack)**.

<!-- JUPITER_VM_SDK_VERSION_START -->
Jupiter VM SDK version: 1.0.5
<!-- JUPITER_VM_SDK_VERSION_END -->

The app demonstrates a simple VM service lifecycle with:
- Authentication (`AUTH`)
- Service initialize (`SDK Init`)
- Service start (`SDK Start`)
- View attach/detach (`뷰 보기` / `뷰 종료`)
- Service stop (`SDK 종료`)
- Parking location APIs (`setSavedParkingLocations`, `updateSavedParkingLocations`, `setVacantParkingLocationStates`, `updateVacantParkingLocationStates`)
- Optional mock mode before service start (`setMockMode`)

## Features

- VM SDK auth/init/start/stop flow example
- WebView frame attach/detach flow
- Runtime permission request flow
- Parking-space tap callback handling
- Hardcoded vacant parking update button (`빈주차 업데이트`)

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
        mavenLocal()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

### 2. Add dependencies

```kotlin
// app/build.gradle.kts
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
```

## Quick Guide

### 1. Configure credentials

Set in `local.properties`:

```properties
sdk.dir=/Users/your_name/Library/Android/sdk
AUTH_ACCESS_KEY=YOUR_ACCESS_KEY
AUTH_SECRET_ACCESS_KEY=YOUR_SECRET_ACCESS_KEY
```

### 2. Authenticate

Input:
- `accessKey: String`
- `accessSecretKey: String`

Output:
- callback `(code: Int, success: Boolean)`

```kotlin
TJJupiterVMAuth.auth(application, accessKey, accessSecretKey) { code, success ->
    // handle auth result
}
```

### 3. Set delegate + initialize service

Input:
- `application: Application`
- `userId: String`
- `sectorId: Int`

Output:
- `onInitSuccess(isSuccess, code)`

```kotlin
vmnaviView.setDelegate(delegate)
```

```kotlin
vmnaviView.initialize(
    application,
    userId,
    sectorId
)
```

### 4. Start service

Input:
- `mode: UserMode` (this demo uses `UserMode.MODE_VEHICLE`)

Output:
- `onJupiterSuccess(isSuccess, code)`
- `onJupiterResult(result)`

```kotlin
vmnaviView.startService(UserMode.MODE_VEHICLE)
```

Optional mock mode before `startService`:

```kotlin
vmnaviView.setMockMode(selectedMockMode)
```

### 5. Show / close VM view

```kotlin
vmnaviView.configureFrame(vmnaviContainer) // show
vmnaviView.closeFrame()                     // close
```

### 6. Stop service

```kotlin
vmnaviView.stopService()
vmnaviView.closeFrame()
```

### 7. Parking APIs in this demo

Initialize parking states after init success:

```kotlin
vmnaviView.setVacantParkingLocationStates(
    mapOf(PARKING_LEVEL_ID to initVacantParkingLocations)
)
vmnaviView.setSavedParkingLocations(
    mapOf(PARKING_LEVEL_ID to initParkingLocationIds)
)
```

Save selected parking location:

```kotlin
vmnaviView.updateSavedParkingLocations(
    mapOf(PARKING_LEVEL_ID to listOf(parkingId))
)
```

Vacant parking update example (hardcoded button):

```kotlin
val parkingLevelId = 52
val updatedVacantParkingLocations = mapOf(
    "OB-1h82101id68tx3548" to TJJupiterVMModel.ParkingLocationState.VACANT,
    "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.VACANT,
    "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.VACANT
)
vmnaviView.updateVacantParkingLocationStates(
    mapOf(parkingLevelId to updatedVacantParkingLocations)
)
```

Tap callback signature in current SDK:

```kotlin
override fun isParkingLocationTapped(levelId: Int, parkingLocationId: String)
```

## Delegate

```kotlin
vmnaviView.setDelegate(object : TJJupiterVMView.TJJupiterVMViewDelegate {
    override fun onInitSuccess(
        isSuccess: Boolean,
        code: InitErrorCode?
    ) {}

    override fun onJupiterSuccess(
        isSuccess: Boolean,
        code: JupiterErrorCode?
    ) {}

    override fun onJupiterResult(result:JupiterResult) {}

    override fun onWebViewSuccess(
        isSuccess: Boolean,
        code: TJJupiterVMModel.VMErrorCode?
    ) {}

    override fun didWebViewRemoved() {}

    override fun isEnteringWardDetected(wardInfo: TJJupiterVMModel.EnteringInfo) {}

    override fun isParkingLocationTapped(levelId: Int, parkingLocationId: String) {}
})
```

Current callback signatures in SDK:

```kotlin
interface TJJupiterVMViewDelegate {
    fun onInitSuccess(isSuccess: Boolean, code: JupiterInitErrorCode? = null)
    fun onJupiterSuccess(isSuccess: Boolean, code: JupiterSdkErrorCode? = null)
    fun onJupiterResult(result: JupiterResult)
    fun onWebViewSuccess(isSuccess: Boolean, code: TJJupiterVMModel.VMErrorCode? = null)
    fun didWebViewRemoved()
    fun isEnteringWardDetected(wardInfo: TJJupiterVMModel.EnteringInfo)
    fun isParkingLocationTapped(levelId: Int, parkingLocationId: String)
}
```

## Position Result

### JupiterResult

```kotlin
data class JupiterResult(
    val mobile_time: Long,
    val index: Int,
    val building_name: String,
    val level_name: String,
    val jupiter_pos: PositionRequest,
    val navi_pos: PositionRequest?,
    val llh: LLH?,
    val velocity: Float,
    val is_vehicle: Boolean,
    val is_indoor: Boolean,
    val validity_flag: Int,
    val remain_distance : Int? = null
)
```

### Position

```kotlin
data class Position(
    val x: Int,
    val y: Int,
    val heading: Int
)
```

### LLH

```kotlin
data class LLH(
    val lat: Double,
    val lon: Double,
    val azimuth: Double
)
```

### EnteringInfo

```kotlin
data class EnteringInfo(
    val id: Int,
    val number: Int,
    val name: String
)
```

## Core Enums

### InitErrorCode

| Name | Value | Description |
| --- | --- | --- |
| `NOT_AUTHORIZED` | `0` | Not authorized |
| `INVALID_ID` | `1` | Invalid ID (blank or includes unsupported characters) |
| `NETWORK_DISCONNECT` | `2` | Network disconnected |
| `LOGIN_FAIL` | `3` | Login/authentication failed |
| `LOAD_RESOURCE_FAIL` | `4` | Resource loading / calc init failed |

### JupiterErrorCode

| Name | Value | Description |
| --- | --- | --- |
| `NOT_INITIALIZED` | `0` | Service is not initialized |
| `DUPLICATED_SERVICE` | `1` | Service already running |
| `GENERATOR_FAIL` | `2` | Generator failed |
| `INVALID_ID` | `3` | Invalid ID |
| `INVALID_MODE` | `4` | Invalid mode |
| `NETWORK_DISCONNECT` | `5` | Network disconnected |
| `LOGIN_FAIL` | `6` | Login/authentication failed |
| `CALC_INIT_FAIL` | `7` | Calc manager initialization failed |
| `BLUETOOTH_OFF` | `8` | Bluetooth is off |
| `BLUETOOTH_UNAVAILABLE` | `9` | Bluetooth unavailable on device |
| `BLE_SCAN_STOP` | `10` | BLE scan stopped |
| `PERMISSION_DENIED` | `11` | Required permission denied |
| `SIMULATION_DATA_LOAD_FAIL` | `12` | Simulation data load failed |
| `GENERATOR_PRECHECK_FAIL` | `13` | Generator precheck failed |

### VMErrorCode (`TJJupiterVMModel.VMErrorCode`)

| Name | Value | Description |
| --- | --- | --- |
| `UNKNOWN` | `-1` | Unknown error |
| `VM_VIEW_FAIL` | `0` | WebView/VM view initialization failed |

### ParkingLocationState (`TJJupiterVMModel.ParkingLocationState`)

| Name | Value | Description |
| --- | --- | --- |
| `VACANT` | `0` | Vacant parking space |
| `OCCUPIED` | `1` | Occupied parking space |


## License

TJJupiterVM SDK is proprietary software provided by TJLabs under a separate commercial license agreement.
