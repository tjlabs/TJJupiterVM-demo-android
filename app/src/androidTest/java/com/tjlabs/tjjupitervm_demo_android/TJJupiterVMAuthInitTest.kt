package com.tjlabs.tjjupitervm_demo_android

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMAuth
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMModel
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMView
import com.tjlabs.tjlabsjupiter_sdk_android.InitErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.JupiterErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.api.JupiterResult
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TJJupiterVMAuthInitTest {

    @Test
    fun verifyAuthAndInit() {
        val args = InstrumentationRegistry.getArguments()
        val sectorId = args.getString("sectorId")?.toIntOrNull() ?: DEFAULT_SECTOR_ID
        val userId = args.getString("userId") ?: DEFAULT_USER_ID
        val label = "sectorId=$sectorId, userId=$userId"

        val accessKey = BuildConfig.AUTH_ACCESS_KEY
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY
        assertTrue("AUTH_ACCESS_KEY missing", accessKey.isNotBlank())
        assertTrue("AUTH_SECRET_ACCESS_KEY missing", accessSecretKey.isNotBlank())

        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application

        val authLatch = CountDownLatch(1)
        var authCode = -1
        var authSuccess = false
        TJJupiterVMAuth.auth(application, accessKey, accessSecretKey) { code, success ->
            authCode = code
            authSuccess = success
            authLatch.countDown()
        }
        assertTrue("auth callback timeout ($label)", authLatch.await(AUTH_TIMEOUT_SEC, TimeUnit.SECONDS))
        assertTrue("auth failed ($label, code=$authCode)", authSuccess)

        val scenario = ActivityScenario.launch(VMHostActivity::class.java)
        try {
            val initLatch = CountDownLatch(1)
            var initSuccess = false
            var initErrorCode: InitErrorCode? = null

            val delegate = object : TJJupiterVMView.TJJupiterVMViewDelegate {
                override fun onInitSuccess(isSuccess: Boolean, code: InitErrorCode?) {
                    initSuccess = isSuccess
                    initErrorCode = code
                    initLatch.countDown()
                }
                override fun onJupiterSuccess(isSuccess: Boolean, code: JupiterErrorCode?) = Unit
                override fun onJupiterResult(result: JupiterResult) = Unit
                override fun onWebViewSuccess(
                    isSuccess: Boolean,
                    code: TJJupiterVMModel.VMErrorCode?
                ) = Unit
                override fun didWebViewRemoved() = Unit
                override fun isEnteringWardDetected(wardInfo: TJJupiterVMModel.EnteringInfo) = Unit
                override fun isParkingLocationTapped(levelId: Int, parkingLocationId: String) = Unit
            }

            scenario.onActivity { activity ->
                activity.vmView.setDelegate(delegate)
                activity.vmView.initialize(application, userId, sectorId)
            }

            assertTrue("init callback timeout ($label)", initLatch.await(INIT_TIMEOUT_SEC, TimeUnit.SECONDS))
            assertTrue("init failed ($label, errorCode=$initErrorCode)", initSuccess)
        } finally {
            scenario.close()
        }
    }

    companion object {
        private const val DEFAULT_USER_ID = "ci_vm_verify_user_android"
        private const val DEFAULT_SECTOR_ID = 20
        private const val AUTH_TIMEOUT_SEC = 60L
        private const val INIT_TIMEOUT_SEC = 120L
    }
}
