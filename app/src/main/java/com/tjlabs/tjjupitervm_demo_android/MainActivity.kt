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
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMAuth
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMModel
import com.tjlabs.tjjupitervm_sdk_android.TJJupiterVMRegion
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
        private const val DEFAULT_SECTOR_ID = 20

        // Hardcoded sector 목록 (tenant/me/sectors API 대체).
        // 필요 시 신규 sector 를 이 리스트에 추가하면 즉시 spinner 에 반영된다.
        private val HARDCODED_SECTORS: List<TenantSectorSummary> = listOf(
            TenantSectorSummary(1, "TJLABS Seoul"),
            TenantSectorSummary(6, "COEX"),
            TenantSectorSummary(8, "Hana Financial Group"),
            TenantSectorSummary(20, "Songdo Convensia"),
            TenantSectorSummary(111, "Samhan Parking"),
            TenantSectorSummary(112, "Bujairi Parking"),
            TenantSectorSummary(113, "King Salman Knowledge Foundation"),
        )
    }

    data class TenantSectorSummary(val id: Int, val name: String) {
        val label: String get() = "$name (id=$id)"
    }

    private var isSdkInitCompleted = false
    private var isSdkStarted = false
    private var isAuthCompleted = false
    private var pendingStartAll = false
    private var pendingParkingSpaceId: String? = null
    private var pendingParkingSpaceLevelId: Int? = null
    private var selectedMockMode: JupiterMockMode = JupiterMockMode.VEHICLE_OUTDOOR_PARKING
    private var sectorOptions: List<TenantSectorSummary> = HARDCODED_SECTORS
    private var selectedSectorId: Int = DEFAULT_SECTOR_ID
    private var suppressSectorCallback = false
    private val authRegion: TJJupiterVMRegion = TJJupiterVMRegion.SAUDI
    // OFF=PROD (기본), ON=DEV. switchDevEnv 토글 상태와 동기화.
    private var isDevEnv: Boolean = false
    private var isViewOpen = false
    private var pendingAutoAuth = false

    private val initParkingLocationIds = listOf("OB-rhaj0t4ctwzb4491")

    private val initOccupiedParkingLocations = mapOf(
        "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
        "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.OCCUPIED
    )

    private val updatedOccupiedParkingLocations = mapOf(
        "OB-1h82101id68tx3548" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
        "OB-1h7zbmxfa10z93809" to TJJupiterVMModel.ParkingLocationState.OCCUPIED,
        "OB-1h84se62jidlw3811" to TJJupiterVMModel.ParkingLocationState.OCCUPIED
    )

    private lateinit var vmnaviView: TJJupiterVMView
    private lateinit var vmDelegate: TJJupiterVMView.TJJupiterVMViewDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestRequiredPermissionsIfNeeded()

        val accessKey = BuildConfig.AUTH_ACCESS_KEY.trim()
        val accessSecretKey = BuildConfig.AUTH_SECRET_ACCESS_KEY.trim()
        val userId = "demo_user"

        val vmnaviContainer = findViewById<FrameLayout>(R.id.vmnaviContainer)
        val parkingSelectionOverlay = findViewById<FrameLayout>(R.id.parkingSelectionOverlay)
        val parkingSelectionSheet = findViewById<View>(R.id.parkingSelectionSheet)
        val selectedParkingIdText = findViewById<TextView>(R.id.textSelectedParkingId)
        val buttonParkingSheetClose = findViewById<Button>(R.id.buttonParkingSheetClose)
        val buttonParkingSheetConfirm = findViewById<Button>(R.id.buttonParkingSheetConfirm)
        val switchSetMockMode = findViewById<SwitchCompat>(R.id.switchSetMock)
        val spinnerMockMode = findViewById<Spinner>(R.id.spinnerMockMode)
        val spinnerSector = findViewById<Spinner>(R.id.spinnerSector)
        val textSectorStatus = findViewById<TextView>(R.id.textSectorStatus)
        val switchDevEnv = findViewById<SwitchCompat>(R.id.switchDevEnv)
        val textEnvStatus = findViewById<TextView>(R.id.textEnvStatus)
        switchDevEnv.setOnCheckedChangeListener { _, isChecked ->
            // auth 후 env 변경 시 재-auth 없이 다음 액션이 자동으로 반영되도록 상태만 갱신.
            // 이미 auth 된 상태에서 env 를 바꾸면 다음 initialize/startService 는 이전 세션 토큰을
            // 재사용하지 못하므로 사용자가 명시적으로 다시 auth 하도록 안내.
            isDevEnv = isChecked
            textEnvStatus.text = if (isChecked) "DEV" else "PROD"
            if (isAuthCompleted) {
                isAuthCompleted = false
                isSdkInitCompleted = false
                isSdkStarted = false
                Toast.makeText(this, "Env 변경됨 — 다시 Auth 필요", Toast.LENGTH_SHORT).show()
            }
        }
        val controlsPanel = findViewById<LinearLayout>(R.id.controlsPanel)
        val buttonToggleControls = findViewById<Button>(R.id.buttonToggleControls)
        buttonToggleControls.setOnClickListener {
            val nowVisible = controlsPanel.visibility != View.VISIBLE
            controlsPanel.visibility = if (nowVisible) View.VISIBLE else View.GONE
            buttonToggleControls.text = if (nowVisible) "컨트롤 접기 ▲" else "컨트롤 펼치기 ▼"
        }
        renderSectors(spinnerSector, textSectorStatus, sectorOptions)

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
                    vmnaviView.setParkingLocationStates(
                        mapOf(PARKING_LEVEL_ID to initOccupiedParkingLocations)
                    )

                    vmnaviView.setSavedParkingLocations(
                        mapOf(PARKING_LEVEL_ID to initParkingLocationIds)
                    )
                    if (pendingStartAll) {
                        runStartAndShow(vmnaviContainer, switchSetMockMode.isChecked)
                    }

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

        findViewById<Button>(R.id.buttonStartAll).setOnClickListener {
            runStartAll(accessKey, accessSecretKey, userId, selectedSectorId, vmnaviContainer, switchSetMockMode.isChecked)
        }

        findViewById<Button>(R.id.buttonInitSdk).setOnClickListener {
            runInitialize(userId, selectedSectorId)
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

        val buttonToggleView = findViewById<Button>(R.id.buttonToggleView)
        buttonToggleView.setOnClickListener {
            if (isViewOpen) {
                closeView(vmnaviContainer, buttonToggleView)
            } else {
                if (!isSdkInitCompleted) {
                    Toast.makeText(this, "SDK init을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                openView(vmnaviContainer, buttonToggleView)
            }
        }

        findViewById<Button>(R.id.buttonStopSdk).setOnClickListener {
            vmnaviView.stopService()
            closeView(vmnaviContainer, buttonToggleView)
            isSdkStarted = false
            Toast.makeText(this, "SDK 종료", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.buttonUpdateOccupiedParking).setOnClickListener {
            if (!isSdkInitCompleted) {
                Toast.makeText(this, "SDK init을 먼저 진행해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vmnaviView.updateParkingLocationStates(mapOf(PARKING_LEVEL_ID to updatedOccupiedParkingLocations))
            Toast.makeText(this, "점유 주차면 3개 업데이트 전송", Toast.LENGTH_SHORT).show()
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

        autoAuthIfPossible(accessKey, accessSecretKey)
    }

    private fun autoAuthIfPossible(accessKey: String, accessSecretKey: String) {
        if (isAuthCompleted) return
        if (accessKey.isEmpty() || accessSecretKey.isEmpty()) return
        if (!hasAllRequiredPermissions()) {
            pendingAutoAuth = true
            return
        }
        pendingAutoAuth = false
        runAuth(accessKey, accessSecretKey)
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

    private fun runAuth(
        accessKey: String,
        accessSecretKey: String,
        onSuccess: (() -> Unit)? = null
    ) {
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
        
        val envLabel = if (isDevEnv) "DEV" else "PROD"
        Log.d("TJJupiterVM-Demo", "auth start env=$envLabel region=$authRegion")
        val authCallback: (Int, Boolean) -> Unit = { code, success ->
            Log.d("TJJupiterVM-Demo", "auth code : $code // success : $success (env=$envLabel)")
            if (success) {
                isAuthCompleted = true
                Toast.makeText(this, "Auth 성공 ($envLabel)", Toast.LENGTH_SHORT).show()
                onSuccess?.invoke()
            } else {
                pendingStartAll = false
                isAuthCompleted = false
                isSdkInitCompleted = false
                isSdkStarted = false
                Toast.makeText(this, "Auth 실패 // code: $code ($envLabel)", Toast.LENGTH_SHORT).show()
            }
        }
        if (isDevEnv) {
            TJJupiterVMAuth.authForDevelopment(application, accessKey, accessSecretKey, authRegion, authCallback)
        } else {
            TJJupiterVMAuth.auth(application, accessKey, accessSecretKey, authRegion, authCallback)
        }
    }

    private fun renderSectors(
        spinner: Spinner,
        status: TextView,
        sectors: List<TenantSectorSummary>
    ) {
        if (sectors.isEmpty()) {
            suppressSectorCallback = true
            spinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                listOf("(auth 후 로드됩니다)")
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            spinner.onItemSelectedListener = null
            suppressSectorCallback = false
            status.text = "Sectors not loaded"
            return
        }

        val labels = sectors.map { it.label }
        suppressSectorCallback = true
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val idx = sectors.indexOfFirst { it.id == selectedSectorId }.let { if (it >= 0) it else 0 }
        spinner.setSelection(idx)
        selectedSectorId = sectors[idx].id
        suppressSectorCallback = false
        status.text = "${sectors.size} sector(s) loaded"
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (suppressSectorCallback) return
                selectedSectorId = sectors.getOrNull(position)?.id ?: DEFAULT_SECTOR_ID
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
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

    private fun runStartAll(
        accessKey: String,
        accessSecretKey: String,
        userId: String,
        sectorId: Int,
        vmnaviContainer: FrameLayout,
        applyMockMode: Boolean
    ) {
        pendingStartAll = true

        if (!hasAllRequiredPermissions()) {
            requestRequiredPermissionsIfNeeded()
            Toast.makeText(this, "앱 권한을 먼저 허용해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isAuthCompleted) {
            runAuth(accessKey, accessSecretKey) {
                runInitialize(userId, selectedSectorId)
            }
            return
        }

        if (!isSdkInitCompleted) {
            runInitialize(userId, sectorId)
            return
        }

        runStartAndShow(vmnaviContainer, applyMockMode)
    }

    private fun runStartAndShow(vmnaviContainer: FrameLayout, applyMockMode: Boolean) {
        if (applyMockMode) {
            vmnaviView.setMockMode(selectedMockMode)
        }
        vmnaviView.startService(UserMode.MODE_VEHICLE)
        openView(vmnaviContainer, findViewById(R.id.buttonToggleView))
        pendingStartAll = false
    }

    private fun openView(container: FrameLayout, toggleButton: Button) {
        container.visibility = View.VISIBLE
        vmnaviView.configureFrame(container)
        isViewOpen = true
        toggleButton.text = "뷰 닫기"
    }

    private fun closeView(container: FrameLayout, toggleButton: Button) {
        vmnaviView.closeFrame()
        container.visibility = View.GONE
        isViewOpen = false
        toggleButton.text = "뷰 열기"
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
            return
        }

        if (pendingAutoAuth) {
            autoAuthIfPossible(
                BuildConfig.AUTH_ACCESS_KEY.trim(),
                BuildConfig.AUTH_SECRET_ACCESS_KEY.trim(),
            )
        }
    }
}
