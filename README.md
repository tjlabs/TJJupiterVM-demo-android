# TJJupiterVM-demo-android

## Overview

TJJupiterVM-demo-android is a minimal Android sample app for integrating **TJLabs Jupiter VM SDK (AAR)**.

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.7
<!-- JUPITER_SDK_VERSION_END -->

<!-- JUPITER_VM_SDK_VERSION_START -->
Jupiter VM SDK (AAR): TJJupiterVM-sdk-android-1.0.0
<!-- JUPITER_VM_SDK_VERSION_END -->

The app demonstrates a simple VM service lifecycle with:
- Authentication (`AUTH`)
- Service initialize (`SDK Init`)
- Service start (`SDK Start`)
- View attach/detach (`뷰 보기` / `뷰 종료`)
- Service stop (`SDK 종료`)
- Parking location APIs (`setSavedParkingLocations`, `setVacantParkingLocations`)

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

### 2. Place VM AAR

Copy AAR file into:

```text
app/libs/TJJupiterVM-sdk-android-0.0.1.aar
```

If file name changes, update `jupiterVmAarName` in `app/build.gradle.kts`.

### 3. Add dependencies

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(files("libs/TJJupiterVM-sdk-android-0.0.1.aar"))
    implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.7")
}
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

### 3. Initialize service

Input:
- `userId: String`
- `sectorId: Int`
- `region: JupiterRegion` (this demo uses `JupiterRegion.KOREA`)
- `delegate: TJJupiterVMView.TJJupiterVMViewDelegate`

Output:
- `onInitSuccess(isSuccess, code)`

```kotlin
vmnaviView.initialize(
    application,
    userId,
    sectorId,
    delegate
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

Saved parking example:

```kotlin
vmnaviView.setSavedParkingLocations(listOf("OB-..."))
```

Vacant parking update example (hardcoded button):

```kotlin
val parkingLevelId = 52
val updatedVacantParkingLocations = mapOf(
    "OB-1h82101id68tx3548" to TJJupiterVMModel.ParkingLocationState.VACANT,
    "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.VACANT,
    "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.VACANT
)
```
