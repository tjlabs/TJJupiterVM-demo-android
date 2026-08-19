package com.tjlabs.tjjupitervm_demo_android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class AuthInitTest {

    @Test
    fun verifyAuthAndInit() {
        VmSdkTestSupport.grantRequiredPermissions()

        val args = VmSdkTestSupport.resolveArgs()
        val (accessKey, accessSecretKey) = VmSdkTestSupport.requireCredentials()
        val application = VmSdkTestSupport.application()

        val authLatch = CountDownLatch(1)
        var authCode = -1
        var authSuccess = false
        TJJupiterVMAuth.auth(application, accessKey, accessSecretKey, args.region) { code, success ->
            authCode = code
            authSuccess = success
            authLatch.countDown()
        }
        VmSdkTestSupport.await(
            authLatch,
            VmSdkTestSupport.AUTH_TIMEOUT_SEC,
            "auth callback timeout (${args.label})"
        )
        assertTrue("auth failed (${args.label}, code=$authCode)", authSuccess)

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
                activity.vmView.initialize(application, args.userId, args.sectorId)
            }

            VmSdkTestSupport.await(
                initLatch,
                VmSdkTestSupport.INIT_TIMEOUT_SEC,
                "init callback timeout (${args.label})"
            )
            assertTrue("init failed (${args.label}, errorCode=$initErrorCode)", initSuccess)
        } finally {
            scenario.close()
        }
    }
}
