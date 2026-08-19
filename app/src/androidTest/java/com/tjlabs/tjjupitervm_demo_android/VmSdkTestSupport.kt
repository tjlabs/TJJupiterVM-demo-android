package com.tjlabs.tjjupitervm_demo_android

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMRegion
import org.junit.Assert.assertTrue
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object VmSdkTestSupport {
    const val DEFAULT_USER_ID = "ci_vm_verify_user_android"
    const val DEFAULT_SECTOR_ID = 20
    val DEFAULT_REGION: TJJupiterVMRegion = TJJupiterVMRegion.KOREA
    const val AUTH_TIMEOUT_SEC = 60L
    const val INIT_TIMEOUT_SEC = 120L
    const val START_TIMEOUT_SEC = 60L
    const val RESULT_TIMEOUT_SEC = 90L
    const val WEBVIEW_TIMEOUT_SEC = 90L

    data class Args(
        val sectorId: Int,
        val userId: String,
        val region: TJJupiterVMRegion,
        val label: String
    )

    fun resolveArgs(): Args {
        val args = InstrumentationRegistry.getArguments()
        val sectorId = args.getString("sectorId")?.toIntOrNull() ?: DEFAULT_SECTOR_ID
        val userId = args.getString("userId") ?: DEFAULT_USER_ID
        val region = parseRegion(args.getString("region")) ?: DEFAULT_REGION
        return Args(
            sectorId = sectorId,
            userId = userId,
            region = region,
            label = "region=${region.name}, sectorId=$sectorId, userId=$userId"
        )
    }

    /**
     * CI 가 넘긴 region 문자열을 SDK enum 으로 매핑. 알 수 없는 값이면 null 을 반환해
     * [resolveArgs] 가 [DEFAULT_REGION] 으로 폴백한다.
     */
    private fun parseRegion(raw: String?): TJJupiterVMRegion? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            TJJupiterVMRegion.valueOf(raw.trim().uppercase(Locale.ROOT))
        }.getOrNull()
    }

    fun requireCredentials(): Pair<String, String> {
        val accessKey = BuildConfig.AUTH_ACCESS_KEY
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY
        assertTrue("AUTH_ACCESS_KEY missing", accessKey.isNotBlank())
        assertTrue("AUTH_SECRET_ACCESS_KEY missing", accessSecretKey.isNotBlank())
        return accessKey to accessSecretKey
    }

    fun application(): Application {
        return InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
    }

    fun grantRequiredPermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val uiAutomation = instrumentation.uiAutomation

        uiAutomation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            uiAutomation.grantRuntimePermission(packageName, Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    fun await(latch: CountDownLatch, timeoutSec: Long, message: String) {
        assertTrue(message, latch.await(timeoutSec, TimeUnit.SECONDS))
    }
}
