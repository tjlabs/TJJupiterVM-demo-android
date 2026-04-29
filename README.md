# TJJupiterVMUI-demo-android

## Overview

`TJJupiterVMUI-demo-android` is a demo app for integrating `TJJupiterVMUI-sdk-android` from `mavenLocal`.

This app follows the sample flow from `TJJupiter-demo-android` and the VMUI SDK sample app:
- runtime permission request
- `AUTH` -> `SDK Init` -> `SDK Start`
- attach/detach VMUI view
- stop SDK and release resources
- simulation/upload/route-driving toggles

## Maven Local First

This project resolves dependencies with `mavenLocal()` first.

`settings.gradle.kts`
- `pluginManagement.repositories`: includes `mavenLocal()`
- `dependencyResolutionManagement.repositories`: `google()`, `mavenLocal()`, `mavenCentral()`, `jitpack`

## Prerequisite: Publish SDK to mavenLocal

In `TJJupiterVMUI-sdk-android` repository:

```bash
./gradlew :sdk:publishReleasePublicationToMavenLocal
```

Published coordinates used by this demo:
- `groupId`: `com.tjlabs`
- `artifactId`: `TJJupiterVMUI-sdk-android`
- `version`: default `0.0.1` (or your published version)

## Configure Demo App

### 1) Set SDK version

In this demo app, set Gradle property (recommended in `~/.gradle/gradle.properties` or project `gradle.properties`):

```properties
JUPITER_VMUI_SDK_VERSION=0.0.1
```

If omitted, app defaults to `0.0.1`.

### 2) Set auth keys

Add in `local.properties` (or as Gradle properties):

```properties
AUTH_ACCESS_KEY=YOUR_ACCESS_KEY
AUTH_SECRET_ACCESS_KEY=YOUR_SECRET_ACCESS_KEY
```

## Dependency

`app/build.gradle.kts`

```kotlin
implementation("com.tjlabs:TJJupiterVMUI-sdk-android:$jupiterVmuiSdkVersion")
```

## Run Flow

1. Launch app and grant required permissions
2. Tap `SDK Init`
3. Tap `SDK Start`
4. Tap `뷰 보기` to attach VMUI frame
5. Use `뷰 종료` / `SDK 종료` to clean up

## Notes

- Sample `sectorId` is set to `20`.
- URL override is supported through the URL input field before init.
- Parking-space tap callback opens a confirmation bottom sheet and calls `setSavedParkingLocations`.
