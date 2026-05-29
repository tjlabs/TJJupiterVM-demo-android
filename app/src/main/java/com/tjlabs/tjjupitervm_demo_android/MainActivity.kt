package com.tjlabs.tjjupitervm_demo_android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMAuth
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMModel
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMView
import com.tjlabs.tjlabscommon_sdk_android.uvd.UserMode
import com.tjlabs.tjlabsjupiter_sdk_android.InitErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.JupiterErrorCode
import com.tjlabs.tjlabsjupiter_sdk_android.JupiterMockMode
import com.tjlabs.tjlabsjupiter_sdk_android.api.JupiterResult

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val PARKING_LEVEL_ID = 52 //example id
    }

    private var isSdkInitCompleted = false
    private var isSdkStarted = false
    private var isAuthCompleted = false
    private var pendingParkingSpaceId: String? = null
    private var pendingParkingSpaceLevelId: Int? = null
    private var selectedMockMode: JupiterMockMode = JupiterMockMode.VEHICLE_OUTDOOR_PARKING

    private val initParkingLocationIds = listOf("OB-rhaj0t4ctwzb4491")
    private val initialVacantParkingLocations = mapOf(
        "OB-rhaj0t4ctwzb4491" to TJJupiterVMModel.ParkingLocationState.VACANT
    )
    private val initVacantParkingLocations = mapOf(
        "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.VACANT,
        "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.VACANT
    )


    private val updatedVacantParkingLocations = mapOf(
        "OB-1h82101id68tx3548" to TJJupiterVMModel.ParkingLocationState.VACANT,
        "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.VACANT,
        "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.VACANT
    )


    private lateinit var vmnaviView: TJJupiterVMView
    private lateinit var vmDelegate: TJJupiterVMView.TJJupiterVMViewDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestRequiredPermissionsIfNeeded()

        val accessKey = BuildConfig.AUTH_ACCESS_KEY.trim()
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY.trim()
        val sectorId = 20
        val userId = "demo_user"

        val vmnaviContainer = findViewById<FrameLayout>(R.id.vmnaviContainer)
        val parkingSelectionOverlay = findViewById<FrameLayout>(R.id.parkingSelectionOverlay)
        val parkingSelectionSheet = findViewById<View>(R.id.parkingSelectionSheet)
        val selectedParkingIdText = findViewById<TextView>(R.id.textSelectedParkingId)
        val buttonParkingSheetClose = findViewById<Button>(R.id.buttonParkingSheetClose)
        val buttonParkingSheetConfirm = findViewById<Button>(R.id.buttonParkingSheetConfirm)
        val switchSetMockMode = findViewById<SwitchCompat>(R.id.switchSetMock)
        val spinnerMockMode = findViewById<Spinner>(R.id.spinnerMockMode)

        vmnaviView = TJJupiterVMView(this)

        val hideParkingSheet = {
            pendingParkingSpaceId = null
            parkingSelectionOverlay.visibility = View.GONE
        }

        val showParkingSheet: (Int, String) -> Unit = { levelId, parkingId ->
            pendingParkingSpaceLevelId = levelId
            pendingParkingSpaceId = parkingId
            selectedParkingIdText.text = parkingId
            parkingSelectionOverlay.visibility = View.VISIBLE
        }

        vmDelegate = object : TJJupiterVMView.TJJupiterVMViewDelegate {
            override fun didWebViewRemoved() {
                Toast.makeText(this@MainActivity, "web view is removed", Toast.LENGTH_SHORT).show()
            }

            override fun isEnteringWardDetected(wardInfo: TJJupiterVMModel.EnteringInfo) {
            }

            override fun isParkingLocationTapped(
                levelId: Int,
                parkingLocationId: String
            ) {
                Log.d("CheckVMNavi", "[AllProcess] isParkingLocationTapped id=$parkingLocationId // level ID : $levelId")
                runOnUiThread {
                    showParkingSheet(levelId, parkingLocationId)
                }
            }

            override fun onInitSuccess(isSuccess: Boolean, code: InitErrorCode?) {
                isSdkInitCompleted = isSuccess
                if (isSuccess) {
                    Toast.makeText(this@MainActivity, "SDK init 성공", Toast.LENGTH_SHORT).show()


                    vmnaviView.setVacantParkingLocationStates(
                        mapOf(PARKING_LEVEL_ID to initVacantParkingLocations)
                    )

                    vmnaviView.setSavedParkingLocations(
                        mapOf(PARKING_LEVEL_ID to initParkingLocationIds)
                    )

                } else {
                    isSdkStarted = false
                    Toast.makeText(this@MainActivity, "SDK init 실패: $code", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onJupiterResult(result: JupiterResult) {
                Log.d("CheckVMNavi", "onJupiterResult result : $result")

            }

            override fun onJupiterSuccess(isSuccess: Boolean, code: JupiterErrorCode?) {
                isSdkStarted = isSuccess
                val message = if (isSuccess) "SDK start 성공" else "SDK start 실패: $code"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onWebViewSuccess(
                isSuccess: Boolean,
                code: TJJupiterVMModel.VMErrorCode?
            ) {
                Log.d("CheckVMNavi", "onWebViewSuccess isSuccess=$isSuccess code=$code")
                if (!isSuccess) {
                    Toast.makeText(this@MainActivity, "WebView 초기화 실패: $code", Toast.LENGTH_SHORT).show()
                }
            }
        }

        parkingSelectionOverlay.setOnClickListener { hideParkingSheet() }
        parkingSelectionSheet.setOnClickListener { }
        buttonParkingSheetClose.setOnClickListener { hideParkingSheet() }
        buttonParkingSheetConfirm.setOnClickListener {
            val parkingId = pendingParkingSpaceId
            if (parkingId.isNullOrBlank()) {
                Toast.makeText(this, "선택된 주차면 ID가 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vmnaviView.updateSavedParkingLocations(mapOf(PARKING_LEVEL_ID to listOf(parkingId)))

            Toast.makeText(this, "주차 위치 저장 요청: $parkingId", Toast.LENGTH_SHORT).show()
            hideParkingSheet()
        }

        findViewById<Button>(R.id.buttonAuthSdk).setOnClickListener {
            runAuth(accessKey, accessSecretKey)
        }

        findViewById<Button>(R.id.buttonInitSdk).setOnClickListener {
            runInitialize(userId, sectorId)
        }

        findViewById<Button>(R.id.buttonStartSdk).setOnClickListener {
            if (!isSdkInitCompleted) {
                Toast.makeText(this, "SDK init을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (switchSetMockMode.isChecked) {
                vmnaviView.setMockMode(selectedMockMode)
            }

            vmnaviView.startService(UserMode.MODE_VEHICLE)
        }

        findViewById<Button>(R.id.buttonShowView).setOnClickListener {
            if (isSdkInitCompleted) {
                vmnaviView.configureFrame(vmnaviContainer)
            } else {
                Toast.makeText(this, "SDK init을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.buttonCloseView).setOnClickListener {
            vmnaviView.closeFrame()
            Toast.makeText(this, "뷰 종료", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonStopSdk).setOnClickListener {
            vmnaviView.stopService()
            vmnaviView.closeFrame()
            isSdkStarted = false
            Toast.makeText(this, "SDK 종료", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonUpdateVacantParking).setOnClickListener {
            if (!isSdkInitCompleted) {
                Toast.makeText(this, "SDK init을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vmnaviView.updateVacantParkingLocationStates(mapOf(PARKING_LEVEL_ID to updatedVacantParkingLocations))
            Toast.makeText(this, "빈 주차면 3개 업데이트 전송", Toast.LENGTH_SHORT).show()
        }

        switchSetMockMode.setOnCheckedChangeListener { _, isChecked ->
                Toast.makeText(this, "Mock Mode ON", Toast.LENGTH_SHORT).show()
        }

        val modes = JupiterMockMode.entries
        val labels = modes.map { mode ->
            mode.name.lowercase().replace("_", " ")
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerMockMode.adapter = adapter
        spinnerMockMode.setSelection(0)
        spinnerMockMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedMockMode = modes.getOrElse(position) { JupiterMockMode.VEHICLE_OUTDOOR_PARKING }
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }


    }

    override fun onDestroy() {
        if (::vmnaviView.isInitialized) {
            vmnaviView.stopService()
            vmnaviView.closeFrame()
            vmnaviView.release()
            isSdkStarted = false
        }
        super.onDestroy()
    }

    private fun runtimePermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        return permissions.toTypedArray()
    }

    private fun hasAllRequiredPermissions(): Boolean {
        return runtimePermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRequiredPermissionsIfNeeded() {
        if (hasAllRequiredPermissions()) return
        ActivityCompat.requestPermissions(this, runtimePermissions(), PERMISSION_REQUEST_CODE)
    }

    private fun runAuth(accessKey: String, accessSecretKey: String) {
        if (!hasAllRequiredPermissions()) {
            requestRequiredPermissionsIfNeeded()
            Toast.makeText(this, "앱 권한을 먼저 허용해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (accessKey.isEmpty() || accessSecretKey.isEmpty()) {
            Toast.makeText(
                this,
                "Set AUTH_ACCESS_KEY / AUTH_SECRET_ACCESS_KEY in local.properties",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        TJJupiterVMAuth.auth(application, accessKey, accessSecretKey) { code, success ->
            Log.d("TJJupiterVM-Demo", "auth code : $code // success : $success")
            if (success) {
                isAuthCompleted = true
                Toast.makeText(this, "Auth 성공", Toast.LENGTH_SHORT).show()
            } else {
                isAuthCompleted = false
                isSdkInitCompleted = false
                isSdkStarted = false
                Toast.makeText(this, "Auth 실패 // code: $code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun runInitialize(userId: String, sectorId: Int) {
        if (!isAuthCompleted) {
            Toast.makeText(this, "SDK Auth를 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        vmnaviView.setDelegate(vmDelegate)
        vmnaviView.initialize(
            application,
            userId,
            sectorId
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return

        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (!allGranted) {
            Toast.makeText(this, "필수 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
