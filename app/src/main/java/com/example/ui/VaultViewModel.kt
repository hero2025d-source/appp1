package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BiometricLog
import com.example.data.Profile
import com.example.data.SecureNote
import com.example.data.VaultRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

enum class ScanMode {
    NONE, REGISTRATION, VERIFICATION
}

enum class LivenessStep {
    ALIGN, TILT, BLINK, SUCCESS
}

// UI State for drawing the tracked face frame
data class FaceBoundState(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val eyeBlinked: Boolean = false,
    val yawAngle: Float = 0f
)

class VaultViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val database = AppDatabase.getDatabase(application)
    private val repository = VaultRepository(
        database.profileDao(),
        database.secureNoteDao(),
        database.biometricLogDao()
    )

    // TTS Engine
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    // Hikvision Access Terminal States
    private val _terminalName = MutableStateFlow("FRONT_MAIN_ENTRANCE")
    val terminalName: StateFlow<String> = _terminalName.asStateFlow()

    private val _terminalModel = MutableStateFlow("DS-K1T671-MV")
    val terminalModel: StateFlow<String> = _terminalModel.asStateFlow()

    private val _ipAddress = MutableStateFlow("192.168.1.100")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _doorOpenDelay = MutableStateFlow(4) // seconds
    val doorOpenDelay: StateFlow<Int> = _doorOpenDelay.asStateFlow()

    private val _tempCheckEnabled = MutableStateFlow(true)
    val tempCheckEnabled: StateFlow<Boolean> = _tempCheckEnabled.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _isRelayOpen = MutableStateFlow(false)
    val isRelayOpen: StateFlow<Boolean> = _isRelayOpen.asStateFlow()

    private val _attendanceMode = MutableStateFlow("Check-In")
    val attendanceMode: StateFlow<String> = _attendanceMode.asStateFlow()

    private val _lastScannedTemp = MutableStateFlow<String?>(null)
    val lastScannedTemp: StateFlow<String?> = _lastScannedTemp.asStateFlow()

    private var relayJob: Job? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsInitialized = true
            }
        }
    }

    fun speak(text: String) {
        if (_ttsEnabled.value && ttsInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }

    fun updateTerminalConfig(
        name: String,
        ip: String,
        delay: Int,
        tempCheck: Boolean,
        voice: Boolean,
        mode: String
    ) {
        _terminalName.value = name.ifBlank { "FRONT_MAIN_ENTRANCE" }
        _ipAddress.value = ip.ifBlank { "192.168.1.100" }
        _doorOpenDelay.value = delay.coerceIn(1, 30)
        _tempCheckEnabled.value = tempCheck
        _ttsEnabled.value = voice
        _attendanceMode.value = mode
        addTerminalLog("CONFIG UPDATED: Terminal `${_terminalName.value}` (${_ipAddress.value}) Delay=${_doorOpenDelay.value}s")
    }

    // Reactive DB states
    val activeProfiles: StateFlow<List<Profile>> = repository.activeProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<SecureNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accessLogs: StateFlow<List<BiometricLog>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Interactive States
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.NONE)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private val _livenessStep = MutableStateFlow(LivenessStep.ALIGN)
    val livenessStep: StateFlow<LivenessStep> = _livenessStep.asStateFlow()

    private val _uiStatusText = MutableStateFlow("INITIATING SCAN CORE...")
    val uiStatusText: StateFlow<String> = _uiStatusText.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress: StateFlow<Float> = _calibrationProgress.asStateFlow()

    private val _simulationEnabled = MutableStateFlow(false)
    val simulationEnabled: StateFlow<Boolean> = _simulationEnabled.asStateFlow()

    private val _faceBounds = MutableStateFlow<FaceBoundState?>(null)
    val faceBounds: StateFlow<FaceBoundState?> = _faceBounds.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(listOf("SYSTEM READY: BIOMETRICS ENCRYPTED"))
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted.asStateFlow()

    // Temporary registration holder
    private var tempRegisteringName = "Guest Operator"

    // Job for scanning simulations
    private var simulationJob: Job? = null

    init {
        // Initialize TTS
        tts = TextToSpeech(application, this)

        // Pre-populate Vault with some starter secret data if database is empty
        viewModelScope.launch {
            repository.allNotes.collect { notes ->
                if (notes.isEmpty()) {
                    populateDefaultSecrets()
                }
            }
        }

        // Pre-populate default profile if empty to ensure face scanner works out of the box
        viewModelScope.launch {
            repository.activeProfiles.collect { profiles ->
                if (profiles.isEmpty()) {
                    populateDefaultProfile()
                }
            }
        }
    }

    private suspend fun populateDefaultProfile() {
        repository.insertProfile(
            Profile(
                name = "Admin Operator",
                faceWidthHeightRatio = 1.12f,
                eyeDistanceRatio = 0.52f
            )
        )
        addTerminalLog("SYSTEM INITIALIZATION: Pre-configured 'Admin Operator' biometric profile loaded.")
    }

    fun forceUnlockVault() {
        _isVaultUnlocked.value = true
        _isRelayOpen.value = true
        addTerminalLog("EMERGENCY OVERRIDE: ACCESS GRANTED VIA INTERFACE CONTROL BYPASS.")
        speak("Access granted by override")

        // Handle automated lock relay timeout
        relayJob?.cancel()
        relayJob = viewModelScope.launch {
            delay(_doorOpenDelay.value * 1000L)
            _isRelayOpen.value = false
            addTerminalLog("DOOR RELAY RESET: LOCK ENGAGED")
        }
    }

    private suspend fun populateDefaultSecrets() {
        repository.insertNote(
            SecureNote(
                title = "Mainframe Access Bypass",
                content = "IP: 192.168.99.12\nUser: admin\nPass: $#@cyber_scanner_9921!",
                category = "Credentials",
                securityLevel = "CRITICAL"
            )
        )
        repository.insertNote(
            SecureNote(
                title = "Ethereum Hot Wallet Seed",
                content = "matrix neon pulse glow scan biometric laser grid shield secure cyber key",
                category = "Private Key",
                securityLevel = "CRITICAL"
            )
        )
        repository.insertNote(
            SecureNote(
                title = "Project FaceID Launch Date",
                content = "Deployment set for Quarter 3. Ensure hardware biometric modules are active and certified.",
                category = "Intel",
                securityLevel = "HIGH"
            )
        )
        repository.insertNote(
            SecureNote(
                title = "Emergency Extraction Coordinates",
                content = "Sector 7, Safehouse Delta. Heli extraction keyphrase: 'Blink twice to unlock'.",
                category = "Personal",
                securityLevel = "MEDIUM"
            )
        )
    }

    fun setCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
        if (!granted) {
            _simulationEnabled.value = true
            addTerminalLog("CAMERA PERMISSION DENIED. FALLBACK TO BIOMETRIC SIMULATION CORE.")
        } else {
            addTerminalLog("CAMERA ACCESS CONNECTED. REAL-TIME FACE ANALYSIS ENGINE STANDBY.")
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        _simulationEnabled.value = enabled
        cancelRunningProcesses()
        if (enabled) {
            addTerminalLog("EMULATOR SCANNER OVERRIDE INSTALLED. INITIATING WIREFRAME GENERATOR.")
        } else {
            _faceBounds.value = null
            addTerminalLog("HARDWARE OPTICS PREFERRED. LENS SYNCHRONIZING.")
        }
    }

    fun setScanMode(mode: ScanMode, name: String = "Admin") {
        cancelRunningProcesses()
        _scanMode.value = mode
        _livenessStep.value = LivenessStep.ALIGN
        _calibrationProgress.value = 0f
        _faceBounds.value = null

        if (mode == ScanMode.NONE) {
            _uiStatusText.value = "CORE IN STANDBY"
            return
        }

        if (mode == ScanMode.REGISTRATION) {
            tempRegisteringName = name.ifBlank { "Guest User" }
            _uiStatusText.value = "ALIGN YOUR FACE INSIDE TARGET RADAR"
            addTerminalLog("INITIATING NEW PROFILE CREATION FOR: $tempRegisteringName")
            addTerminalLog("CALIBRATING DIGITAL GRID LENS CORES...")
            speak("Please look at the camera to start enrollment")
        } else {
            _uiStatusText.value = "SCANNING TO VERIFY AUTHORITY"
            addTerminalLog("VERIFYING IDENTITY MATRIX... STANDBY")
            speak("Please look at the camera")
        }

        // Trigger simulation loop if simulation is enabled
        if (_simulationEnabled.value) {
            runSimulationFlow()
        }
    }

    private fun cancelRunningProcesses() {
        simulationJob?.cancel()
        simulationJob = null
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
        addTerminalLog("VAULT COLD LOCKDOWN ENGAGED.")
    }

    // Handles ML Kit face updates directly from camera analyzer
    fun onFacesDetected(faces: List<com.google.mlkit.vision.face.Face>, frameWidth: Int, frameHeight: Int) {
        if (_simulationEnabled.value || _scanMode.value == ScanMode.NONE) return

        if (faces.isEmpty()) {
            _faceBounds.value = null
            _uiStatusText.value = "ACQUIRING FEED... TARGET FACE MISSING"
            return
        }

        val face = faces.first()
        val boundingBox = face.boundingBox

        // Map bounding box to relative screen percentages (assuming front camera mirroring)
        // CameraX frame coordinates need mirroring and mapping
        val left = (boundingBox.left.toFloat() / frameWidth).coerceIn(0f, 1f)
        val top = (boundingBox.top.toFloat() / frameHeight).coerceIn(0f, 1f)
        val right = (boundingBox.right.toFloat() / frameWidth).coerceIn(0f, 1f)
        val bottom = (boundingBox.bottom.toFloat() / frameHeight).coerceIn(0f, 1f)

        // Real front mirror correction: Swap left and right percentages if needed
        val relLeft = 1f - right
        val relRight = 1f - left

        val leftEyeOpenProb = face.leftEyeOpenProbability ?: 1.0f
        val rightEyeOpenProb = face.rightEyeOpenProbability ?: 1.0f
        val isEyeBlinked = leftEyeOpenProb < 0.15f && rightEyeOpenProb < 0.15f
        val yawAngle = face.headEulerAngleY

        _faceBounds.value = FaceBoundState(
            left = relLeft,
            top = top,
            right = relRight,
            bottom = bottom,
            eyeBlinked = isEyeBlinked,
            yawAngle = yawAngle
        )

        // Run verification or calibration states using actual values
        processLivenessLogic(
            isAligned = relLeft > 0.15f && relRight < 0.85f && top > 0.15f && bottom < 0.85f,
            isTilted = abs(yawAngle) > 18f,
            isBlinked = isEyeBlinked
        )
    }

    private fun processLivenessLogic(isAligned: Boolean, isTilted: Boolean, isBlinked: Boolean) {
        viewModelScope.launch {
            when (_scanMode.value) {
                ScanMode.NONE -> {}
                ScanMode.REGISTRATION -> {
                    when (_livenessStep.value) {
                        LivenessStep.ALIGN -> {
                            if (isAligned) {
                                _calibrationProgress.value = (_calibrationProgress.value + 0.05f).coerceAtMost(0.3f)
                                _uiStatusText.value = "ALIGNMENT SECURED. HOLD STILL..."
                                if (_calibrationProgress.value >= 0.3f) {
                                    _livenessStep.value = LivenessStep.TILT
                                    addTerminalLog("STEP 1 COMPLETE: STRUCTURAL DEPTH CODES SECURED.")
                                    _uiStatusText.value = "SLOWLY TURN YOUR HEAD LEFT OR RIGHT"
                                }
                            } else {
                                _uiStatusText.value = "CENTER YOUR FACE IN THE GREEN GLOW BOX"
                            }
                        }
                        LivenessStep.TILT -> {
                            if (isTilted) {
                                _calibrationProgress.value = (_calibrationProgress.value + 0.05f).coerceAtMost(0.65f)
                                _uiStatusText.value = "SCANNING ANGULAR GEOMETRY... KEEP TURNING"
                                if (_calibrationProgress.value >= 0.65f) {
                                    _livenessStep.value = LivenessStep.BLINK
                                    addTerminalLog("STEP 2 COMPLETE: FACIAL ANGLE GRADIENT CALCULATED.")
                                    _uiStatusText.value = "NOW BLINK BOTH EYES FIRMLY TWICE"
                                }
                            } else {
                                _uiStatusText.value = "TURN HEAD AT LEAST 20 DEGREES"
                            }
                        }
                        LivenessStep.BLINK -> {
                            if (isBlinked) {
                                _calibrationProgress.value = (_calibrationProgress.value + 0.1f).coerceAtMost(1f)
                                _uiStatusText.value = "LIVENESS BIO-MATCH CONFIRMED!"
                                if (_calibrationProgress.value >= 1f) {
                                    completeFaceRegistration()
                                }
                            }
                        }
                        LivenessStep.SUCCESS -> {}
                    }
                }
                ScanMode.VERIFICATION -> {
                    // Verification uses faster checks: Needs alignment + instant matching
                    if (isAligned) {
                        _calibrationProgress.value = (_calibrationProgress.value + 0.15f).coerceAtMost(1f)
                        _uiStatusText.value = "MATCHING FACE KEYWORDS... VERIFYING"
                        if (_calibrationProgress.value >= 1.0f) {
                            completeFaceVerificationSuccess()
                        }
                    } else {
                        _uiStatusText.value = "CENTER YOUR FACE TO UNLOCK"
                    }
                }
            }
        }
    }

    private fun runSimulationFlow() {
        simulationJob = viewModelScope.launch {
            if (_scanMode.value == ScanMode.REGISTRATION) {
                // Step 1: Align
                _livenessStep.value = LivenessStep.ALIGN
                _uiStatusText.value = "[SIMULATING] ALIGNING WIREFRAME FACE..."
                _faceBounds.value = FaceBoundState(0.25f, 0.25f, 0.75f, 0.75f, false, 0f)
                addTerminalLog("CONNECTING TO SYNTHETIC VIRTUAL FEED...")
                delay(1200)

                for (i in 1..10) {
                    _calibrationProgress.value = i * 0.03f
                    delay(100)
                }
                addTerminalLog("CALIBRATION OK: SPATIAL MAPPING BOUNDS SECURED.")

                // Step 2: Tilt
                _livenessStep.value = LivenessStep.TILT
                _uiStatusText.value = "[SIMULATING] TILTING LEFT & RIGHT..."
                _faceBounds.value = FaceBoundState(0.2f, 0.25f, 0.7f, 0.75f, false, 25f)
                delay(1500)

                for (i in 11..22) {
                    _calibrationProgress.value = i * 0.03f
                    delay(100)
                }
                addTerminalLog("CALIBRATION OK: ANGULAR HEAD-AXIS RATIO VALIDATED.")

                // Step 3: Blink
                _livenessStep.value = LivenessStep.BLINK
                _uiStatusText.value = "[SIMULATING] REGISTERING EYE BLINK EVENT..."
                _faceBounds.value = FaceBoundState(0.25f, 0.25f, 0.75f, 0.75f, true, 0f)
                delay(800)
                _faceBounds.value = FaceBoundState(0.25f, 0.25f, 0.75f, 0.75f, false, 0f)
                delay(800)

                for (i in 23..33) {
                    _calibrationProgress.value = i * 0.03f
                    delay(100)
                }
                _calibrationProgress.value = 1f
                addTerminalLog("CALIBRATION OK: LIVENESS SIGNS ANALYZED.")

                completeFaceRegistration()

            } else if (_scanMode.value == ScanMode.VERIFICATION) {
                _uiStatusText.value = "[SIMULATING] MATCHING CODES..."
                _faceBounds.value = FaceBoundState(0.25f, 0.25f, 0.75f, 0.75f, false, 0f)
                delay(1200)

                val activeList = activeProfiles.value
                if (activeList.isEmpty()) {
                    // No profile registered
                    _uiStatusText.value = "ACCESS DENIED: NO USER RECORDED"
                    addTerminalLog("ERROR: AUTHORIZED SCAN SIGNATURE MISSING.")
                    repository.insertLog(
                        BiometricLog(
                            operatorName = "Unknown Intruder",
                            status = "ACCESS_DENIED",
                            method = "SIMULATOR",
                            confidence = 0f,
                            details = "Attempted unlock with zero enrolled biometric profiles."
                        )
                    )
                    _scanMode.value = ScanMode.NONE
                    _faceBounds.value = null
                } else {
                    _calibrationProgress.value = 1.0f
                    completeFaceVerificationSuccess()
                }
            }
        }
    }

    private suspend fun completeFaceRegistration() {
        _livenessStep.value = LivenessStep.SUCCESS
        _uiStatusText.value = "REGISTRATION COMPLETED!"
        val p = Profile(
            name = tempRegisteringName,
            faceWidthHeightRatio = 1.12f,
            eyeDistanceRatio = 0.52f
        )
        repository.insertProfile(p)
        addTerminalLog("SUCCESS: BIOMETRIC KEY REGISTERED FOR $tempRegisteringName.")
        speak("Enrollment completed. Thank you!")

        repository.insertLog(
            BiometricLog(
                operatorName = tempRegisteringName,
                status = "CALIBRATED",
                method = if (_simulationEnabled.value) "SIMULATOR" else "MLKIT_FACE",
                confidence = 100f,
                details = "Registered face profile on Hikvision biometric node."
            )
        )

        delay(1500)
        _scanMode.value = ScanMode.NONE
        _faceBounds.value = null
    }

    private suspend fun completeFaceVerificationSuccess() {
        val operator = activeProfiles.value.firstOrNull()?.name ?: "Operator"
        
        // Simulating forehead temperature reading
        val temp = if (_tempCheckEnabled.value) {
            val randomTemp = 36.2f + Random.nextFloat() * 0.6f
            String.format("%.1f°C", randomTemp)
        } else {
            null
        }
        _lastScannedTemp.value = temp

        val tempMsg = if (temp != null) " Temp: $temp (Normal)" else ""
        _uiStatusText.value = "AUTHENTICATED: WELCOME $operator"
        _isVaultUnlocked.value = true
        _isRelayOpen.value = true
        addTerminalLog("ACCESS GRANTED: $operator. Temp=$tempMsg. Relay Open.")

        speak("Thank you!")

        // Handle automated lock relay timeout
        relayJob?.cancel()
        relayJob = viewModelScope.launch {
            delay(_doorOpenDelay.value * 1000L)
            _isRelayOpen.value = false
            addTerminalLog("DOOR RELAY RESET: LOCK ENGAGED")
        }

        repository.insertLog(
            BiometricLog(
                operatorName = operator,
                status = "ACCESS_GRANTED",
                method = if (_simulationEnabled.value) "SIMULATOR" else "MLKIT_FACE",
                confidence = 98.7f,
                details = "Identified as registered personnel.$tempMsg"
            )
        )

        delay(2500)
        _scanMode.value = ScanMode.NONE
        _faceBounds.value = null
    }

    fun reportVerificationFailure() {
        viewModelScope.launch {
            _lastScannedTemp.value = null
            _uiStatusText.value = "VERIFICATION FAILED"
            addTerminalLog("WARNING: AUTHENTICATION DEVIATION DETECTED!")
            speak("Please try again")
            
            repository.insertLog(
                BiometricLog(
                    operatorName = "Unknown Person",
                    status = "ACCESS_DENIED",
                    method = if (_simulationEnabled.value) "SIMULATOR" else "MLKIT_FACE",
                    confidence = 12.3f,
                    details = "Face signature failed matching tests."
                )
            )
            delay(2000)
            _scanMode.value = ScanMode.NONE
            _faceBounds.value = null
        }
    }

    // Native Biometric Prompt Flow
    fun unlockViaSystemBiometrics(activity: FragmentActivity) {
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            addTerminalLog("SYSTEM BIOMETRICS NOT CONFIGURED OR NOT AVAILABLE ON THIS DEVICE.")
            addTerminalLog("SUGGESTION: USE INTERACTIVE CYBER FACE SCANNER TAB INSTEAD!")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModelScope.launch {
                        _isVaultUnlocked.value = true
                        val user = activeProfiles.value.firstOrNull()?.name ?: "Primary Operator"
                        addTerminalLog("SYSTEM BIOMETRICS AUTH SUCCESSFUL. VAULT OPENED.")
                        repository.insertLog(
                            BiometricLog(
                                operatorName = user,
                                status = "ACCESS_GRANTED",
                                method = "SYSTEM_BIOMETRICS",
                                confidence = 100f,
                                details = "Device Hardware Biometrics signature validated successfully."
                            )
                        )
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    viewModelScope.launch {
                        addTerminalLog("BIOMETRIC CRITICAL ERROR ($errorCode): $errString")
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    viewModelScope.launch {
                        addTerminalLog("SYSTEM BIOMETRIC MATCH FAILED.")
                        repository.insertLog(
                            BiometricLog(
                                operatorName = "Unknown Intrusive Signal",
                                status = "ACCESS_DENIED",
                                method = "SYSTEM_BIOMETRICS",
                                confidence = 0f,
                                details = "Hardware biometric scan failed matching tests."
                            )
                        )
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Decrypt Biometric Vault")
            .setSubtitle("Use your device's registered fingerprint or Face ID")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Vault Note Actions
    fun addSecureNote(title: String, content: String, category: String, securityLevel: String = "HIGH") {
        viewModelScope.launch {
            repository.insertNote(
                SecureNote(
                    title = title,
                    content = content,
                    category = category,
                    securityLevel = securityLevel
                )
            )
            addTerminalLog("ENCRYPTED DATA RECORD INJECTED: $title")
        }
    }

    fun deleteSecureNote(note: SecureNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            addTerminalLog("CLASSIFIED NOTE REMOVED: ${note.title}")
        }
    }

    fun clearAllProfiles() {
        viewModelScope.launch {
            repository.clearProfiles()
            _isVaultUnlocked.value = false
            addTerminalLog("ALL ENROLLED BIOMETRIC PROFILES CLEARED FROM SECURE ENCLAVE.")
        }
    }

    fun clearHistoryLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            addTerminalLog("ACCESS HISTORY PURGED SUCCESSFULLY.")
        }
    }

    private fun addTerminalLog(msg: String) {
        val list = _terminalLogs.value.toMutableList()
        val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        list.add("[$timeStr] $msg")
        if (list.size > 12) {
            list.removeAt(0)
        }
        _terminalLogs.value = list
    }
}
