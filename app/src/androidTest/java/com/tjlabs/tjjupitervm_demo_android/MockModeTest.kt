package com.tjlabs.tjjupitervm_demo_android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMAuth
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMModel
import com.tjlabs.tjlabscommon_sdk_android.uvd.UserMode
import com.tjlabs.tjlabsjupiter_sdk_android.InitErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.JupiterErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.JupiterMockMode
import com.tjlabs.tjlabsjupiter_sdk_android.api.JupiterResult
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class MockModeTest {

    @Test
    fun verifyMockModesAndVmView() {
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

        val scenarios = listOf(
            "indoor" to JupiterMockMode.PEDESTRIAN_INDOOR_PARKING,
            "outdoor" to JupiterMockMode.VEHICLE_OUTDOOR_PARKING,
            "vehicle" to JupiterMockMode.VEHICLE_INDOOR_OUTDOOR
        )

        scenarios.forEach { (label, mockMode) ->
            verifySingleScenario(
                label = "${args.label}, mock=$label",
                userId = args.userId,
                sectorId = args.sectorId,
                application = application,
                mockMode = mockMode
            )
        }
    }

    private fun verifySingleScenario(
        label: String,
        userId: String,
        sectorId: Int,
        application: android.app.Application,
        mockMode: JupiterMockMode
    ) {
        val scenario = ActivityScenario.launch(VMHostActivity::class.java)
        try {
            val initLatch = CountDownLatch(1)
            val startLatch = CountDownLatch(1)
            val resultLatch = CountDownLatch(1)
            val webViewLatch = CountDownLatch(1)
            val waitForWebView = AtomicBoolean(false)

            var initSuccess = false
            var initErrorCode: InitErrorCode? = null
            var startSuccess = false
            var startErrorCode: JupiterErrorCode? = null

            val delegate = object : com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMView.TJJupiterVMViewDelegate {
                override fun onInitSuccess(isSuccess: Boolean, code: InitErrorCode?) {
                    initSuccess = isSuccess
                    initErrorCode = code
                    initLatch.countDown()
                }

                override fun onJupiterSuccess(isSuccess: Boolean, code: JupiterErrorCode?) {
                    startSuccess = isSuccess
                    startErrorCode = code
                    startLatch.countDown()
                }

                override fun onJupiterResult(result: JupiterResult) {
                    resultLatch.countDown()
                }

                override fun onWebViewSuccess(
                    isSuccess: Boolean,
                    code: TJJupiterVMModel.VMErrorCode?
                ) {
                    if (waitForWebView.get() && isSuccess) {
                        webViewLatch.countDown()
                    }
                }

                override fun didWebViewRemoved() = Unit
                override fun isEnteringWardDetected(wardInfo: TJJupiterVMModel.EnteringInfo) = Unit
                override fun isParkingLocationTapped(levelId: Int, parkingLocationId: String) = Unit
            }

            scenario.onActivity { activity ->
                activity.vmView.setDelegate(delegate)
                activity.vmView.initialize(application, userId, sectorId)
            }

            VmSdkTestSupport.await(
                initLatch,
                VmSdkTestSupport.INIT_TIMEOUT_SEC,
                "init callback timeout ($label)"
            )
            assertTrue("init failed ($label, errorCode=$initErrorCode)", initSuccess)

            scenario.onActivity { activity ->
                waitForWebView.set(true)
                activity.vmView.setMockMode(mockMode)
                activity.vmView.startService(UserMode.MODE_VEHICLE)
                activity.vmView.configureFrame(activity.container)
                assertTrue("vm view not attached to container ($label)", activity.vmView.parent === activity.container)
            }

            VmSdkTestSupport.await(
                webViewLatch,
                VmSdkTestSupport.WEBVIEW_TIMEOUT_SEC,
                "web view success timeout ($label)"
            )
            VmSdkTestSupport.await(
                startLatch,
                VmSdkTestSupport.START_TIMEOUT_SEC,
                "start callback timeout ($label)"
            )
            assertTrue("start failed ($label, errorCode=$startErrorCode)", startSuccess)
            VmSdkTestSupport.await(
                resultLatch,
                VmSdkTestSupport.RESULT_TIMEOUT_SEC,
                "mock result timeout ($label)"
            )
        } finally {
            scenario.close()
        }
    }
}
