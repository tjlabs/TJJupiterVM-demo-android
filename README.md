# TJJupiterVM-demo-android

## Overview

TJJupiterVM-demo-android is a minimal Android sample app for integrating **TJLabs Jupiter VM SDK (JitPack)**.

<!-- JUPITER_VM_SDK_VERSION_START -->
Jupiter VM SDK version: 1.0.0-webview-fix
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

### 2. Add dependencies

```kotlin
// app/build.gradle.kts
<!-- APP_DEPENDENCIES_START -->
dependencies {
    implementation("com.github.tjlabs:TJJupiterVM-sdk-android:1.0.0-webview-fix")
    implementation("com.github.tjlabs:TJLabsJupiter-sdk-android:2.0.10")
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

<!-- JUPITER_SDK_VERSION_START -->
Jupiter SDK version: 2.0.10
<!-- JUPITER_SDK_VERSION_END -->
