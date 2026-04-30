import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

fun String.toBuildConfigString(): String = this.replace("\\", "\\\\").replace("\"", "\\\"")

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val authAccessKey = (
    providers.gradleProperty("AUTH_ACCESS_KEY").orNull
        ?: localProperties.getProperty("AUTH_ACCESS_KEY", "")
    ).trim()

val authSecretAccessKey = (
    providers.gradleProperty("AUTH_SECRET_ACCESS_KEY").orNull
        ?: localProperties.getProperty("AUTH_SECRET_ACCESS_KEY", "")
    ).trim()

val jupiterVmSdkVersion = providers.gradleProperty("JUPITER_VM_SDK_VERSION").orNull
    ?: "0.0.1"

android {
    namespace = "com.tjlabs.tjjupitervm_demo_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tjlabs.tjjupitervm_demo_android"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AUTH_ACCESS_KEY", "\"${authAccessKey.toBuildConfigString()}\"")
        buildConfigField("String", "AUTH_SECRET_ACCESS_KEY", "\"${authSecretAccessKey.toBuildConfigString()}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("com.tjlabs:TJJupiterVMUI-sdk-android:$jupiterVmSdkVersion")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
