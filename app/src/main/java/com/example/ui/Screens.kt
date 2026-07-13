package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.FaceAnalyzer
import com.example.data.BiometricLog
import com.example.data.Profile
import com.example.data.SecureNote
import com.example.ui.theme.CyberAccent
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberError
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberGlowGreen
import com.example.ui.theme.CyberPrimary
import com.example.ui.theme.CyberSecondary
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setCameraPermission(hasPermission)
        if (!hasPermission) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    Scaffold(
        bottomBar = {
            Column(modifier = Modifier.background(CyberBackground)) {
                // Cyber Ticker Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.isVaultUnlocked.collectAsStateWithLifecycle().value) CyberPrimary else CyberError)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENCLAVE STATUS: ${if (viewModel.isVaultUnlocked.collectAsStateWithLifecycle().value) "SECURE ACCESS GRANTED" else "ENCRYPTED COLD STORAGE LOCKED"}",
                        color = if (viewModel.isVaultUnlocked.collectAsStateWithLifecycle().value) CyberPrimary else CyberTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (viewModel.simulationEnabled.collectAsStateWithLifecycle().value) "[SIMULATION MODE]" else "[HARDWARE LOCKED]",
                        color = CyberSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Cyber Bottom Nav Bar
                NavigationBar(
                    containerColor = CyberBackground,
                    tonalElevation = 8.dp,
                    modifier = Modifier.border(width = 1.dp, color = CyberBorder)
                ) {
                    val items = listOf(
                        Triple("Сейф", Icons.Default.Lock, "tab_safe"),
                        Triple("Сканер", Icons.Default.Face, "tab_scanner"),
                        Triple("Логи", Icons.Default.History, "tab_logs"),
                        Triple("Профиль", Icons.Default.Settings, "tab_settings")
                    )

                    items.forEachIndexed { index, (label, icon, testTag) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(imageVector = icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.testTag(testTag),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberPrimary,
                                selectedTextColor = CyberPrimary,
                                indicatorColor = CyberBorder,
                                unselectedIconColor = CyberTextSecondary,
                                unselectedTextColor = CyberTextSecondary
                            )
                        )
                    }
                }
            }
        },
        containerColor = CyberBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> VaultLockerTab(viewModel = viewModel, activity = activity, onNavigateToScanner = { selectedTab = 1 })
                1 -> FaceScannerTab(viewModel = viewModel)
                2 -> LogsTab(viewModel = viewModel)
                3 -> ProfileSettingsTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VaultLockerTab(
    viewModel: VaultViewModel,
    activity: FragmentActivity,
    onNavigateToScanner: () -> Unit
) {
    val isUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val profiles by viewModel.activeProfiles.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isUnlocked) {
            // LOCKED SCREEN
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Face ID Lock Indicator Target
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer border
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(4.dp, CyberPrimary.copy(alpha = 0.2f), RoundedCornerShape(48.dp))
                    )

                    // Dashed lines and brackets
                    val primaryColor = CyberPrimary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.4f),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(52.dp.toPx()),
                            style = stroke
                        )

                        // Corner brackets
                        val bLen = 24.dp.toPx()
                        val bThick = 4.dp.toPx()
                        val bColor = primaryColor

                        // Top-Left corner bracket
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, bLen)
                                lineTo(0f, 4.dp.toPx())
                                quadraticTo(0f, 0f, 4.dp.toPx(), 0f)
                                lineTo(bLen, 0f)
                            },
                            color = bColor,
                            style = Stroke(width = bThick)
                        )
                        // Top-Right corner bracket
                        drawPath(
                            path = Path().apply {
                                moveTo(size.width - bLen, 0f)
                                lineTo(size.width - 4.dp.toPx(), 0f)
                                quadraticTo(size.width, 0f, size.width, 4.dp.toPx())
                                lineTo(size.width, bLen)
                            },
                            color = bColor,
                            style = Stroke(width = bThick)
                        )
                        // Bottom-Left corner bracket
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, size.height - bLen)
                                lineTo(0f, size.height - 4.dp.toPx())
                                quadraticTo(0f, size.height, 4.dp.toPx(), size.height)
                                lineTo(bLen, size.height)
                            },
                            color = bColor,
                            style = Stroke(width = bThick)
                        )
                        // Bottom-Right corner bracket
                        drawPath(
                            path = Path().apply {
                                moveTo(size.width - bLen, size.height)
                                lineTo(size.width - 4.dp.toPx(), size.height)
                                quadraticTo(size.width, size.height, size.width, size.height - 4.dp.toPx())
                                lineTo(size.width, size.height - bLen)
                            },
                            color = bColor,
                            style = Stroke(width = bThick)
                        )
                    }

                    // Inner container and lock icon
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(CyberSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CyberPrimary,
                            modifier = Modifier
                                .size(64.dp)
                                .offset(y = (-2).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "HIKFACE ACCESS TERMINAL",
                    color = CyberTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "High-security biometric authorization system. Unlock the local classified database sector utilizing facial matching analysis or system credentials.",
                    color = CyberTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Buttons
                Button(
                    onClick = {
                        viewModel.setScanMode(ScanMode.VERIFICATION)
                        onNavigateToScanner()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("unlock_face_scanner_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberPrimary,
                        contentColor = Color.White
                    ),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Face, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ENGAGE FACE ID SCANNER",
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Default,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.unlockViaSystemBiometrics(activity)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, CyberBorder, CircleShape)
                        .testTag("unlock_system_biometrics_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = CyberPrimary
                    ),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "USE DEVICE PASSCODE / ID",
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Default,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        viewModel.forceUnlockVault()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(1.dp, CyberError, CircleShape)
                        .testTag("emergency_bypass_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberError.copy(alpha = 0.15f),
                        contentColor = CyberError
                    ),
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(20.dp), tint = CyberError)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "EMERGENCY SYSTEM BYPASS",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        } else {
            // UNLOCKED SCREEN: SHOW SECURE INTEL NOTES
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Welcome Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .background(CyberSurface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DECRYPTED SECTOR",
                            color = CyberPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "AUTHORIZED ENCLAVE",
                            color = CyberTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Primary Operator: ${profiles.firstOrNull()?.name ?: "Special Agent (Not Registered)"}",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier
                            .background(CyberBorder, CircleShape)
                            .testTag("lock_vault_btn")
                    ) {
                        Icon(imageVector = Icons.Default.LockOpen, contentDescription = "Lock", tint = CyberPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CLASSIFIED DATA KEYCHAINS (${notes.size})",
                    color = CyberTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyberTextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "NO INTELLIGENCE FOUND",
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes) { note ->
                            SecureNoteCard(note = note, onDelete = { viewModel.deleteSecureNote(note) })
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = CyberPrimary,
                contentColor = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_intel_fab"),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Intel")
            }
        }

        // Add Dialog
        if (showAddDialog) {
            AddIntelDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, content, category, security ->
                    viewModel.addSecureNote(title, content, category, security)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun SecureNoteCard(
    note: SecureNote,
    onDelete: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Top Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = note.category.uppercase(),
                        color = CyberSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = note.title,
                        color = CyberTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (note.securityLevel == "CRITICAL") CyberError.copy(alpha = 0.2f) else CyberBorder)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.securityLevel,
                            color = if (note.securityLevel == "CRITICAL") CyberError else CyberTextSecondary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content with Hide/Reveal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSurfaceVariant, RoundedCornerShape(6.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(6.dp))
                    .clickable { revealed = !revealed }
                    .padding(12.dp)
            ) {
                if (revealed) {
                    Text(
                        text = note.content,
                        color = CyberPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = null,
                            tint = CyberTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TAP TO DECRYPT INTEL CODES",
                            color = CyberTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIntelDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Credentials") }
    var securityLevel by remember { mutableStateOf("HIGH") }

    val categories = listOf("Credentials", "Private Key", "Intel", "Personal")
    val securityLevels = listOf("CRITICAL", "HIGH", "MEDIUM")

    var categoryExpanded by remember { mutableStateOf(false) }
    var securityExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            color = CyberSurface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "INJECT SECURE INTELLIGENCE",
                    color = CyberPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Intel Title", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_intel_title"),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        focusedContainerColor = CyberSurfaceVariant,
                        unfocusedContainerColor = CyberSurfaceVariant,
                        focusedIndicatorColor = CyberPrimary,
                        unfocusedIndicatorColor = CyberBorder
                    )
                )

                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Intel Secure Content", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("add_intel_content"),
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = CyberTextPrimary,
                        unfocusedTextColor = CyberTextPrimary,
                        focusedContainerColor = CyberSurfaceVariant,
                        unfocusedContainerColor = CyberSurfaceVariant,
                        focusedIndicatorColor = CyberPrimary,
                        unfocusedIndicatorColor = CyberBorder
                    )
                )

                // Dropdowns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Category
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = category,
                                onValueChange = {},
                                label = { Text("Category", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary,
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption, fontFamily = FontFamily.Monospace) },
                                        onClick = {
                                            category = selectionOption
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Security
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = securityExpanded,
                            onExpandedChange = { securityExpanded = !securityExpanded }
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = securityLevel,
                                onValueChange = {},
                                label = { Text("Level", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = securityExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CyberTextPrimary,
                                    unfocusedTextColor = CyberTextPrimary,
                                    focusedBorderColor = CyberPrimary,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = securityExpanded,
                                onDismissRequest = { securityExpanded = false }
                            ) {
                                securityLevels.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption, fontFamily = FontFamily.Monospace) },
                                        onClick = {
                                            securityLevel = selectionOption
                                            securityExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberTextSecondary)
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onAdd(title, content, category, securityLevel)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.White),
                        shape = CircleShape,
                        modifier = Modifier.testTag("dialog_submit_intel")
                    ) {
                        Text("INJECT CODE", fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default)
                    }
                }
            }
        }
    }
}

@Composable
fun FaceScannerTab(viewModel: VaultViewModel) {
    val scanMode by viewModel.scanMode.collectAsStateWithLifecycle()
    val livenessStep by viewModel.livenessStep.collectAsStateWithLifecycle()
    val uiStatusText by viewModel.uiStatusText.collectAsStateWithLifecycle()
    val calibrationProgress by viewModel.calibrationProgress.collectAsStateWithLifecycle()
    val simulationEnabled by viewModel.simulationEnabled.collectAsStateWithLifecycle()
    val faceBounds by viewModel.faceBounds.collectAsStateWithLifecycle()
    val terminalLogs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val cameraPermissionGranted by viewModel.cameraPermissionGranted.collectAsStateWithLifecycle()

    val termName by viewModel.terminalName.collectAsStateWithLifecycle()
    val termModel by viewModel.terminalModel.collectAsStateWithLifecycle()
    val ipAddr by viewModel.ipAddress.collectAsStateWithLifecycle()
    val attMode by viewModel.attendanceMode.collectAsStateWithLifecycle()
    val tempCheck by viewModel.tempCheckEnabled.collectAsStateWithLifecycle()
    val isRelayOpen by viewModel.isRelayOpen.collectAsStateWithLifecycle()
    val lastTemp by viewModel.lastScannedTemp.collectAsStateWithLifecycle()

    var showRegNameDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    var systemTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            systemTime = sdf.format(java.util.Date())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper status banner (Hikvision industrial terminal design)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .background(CyberSurface)
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HIKVISION ACCESS CONTROL TERMINAL",
                        color = CyberError, // Red Hikvision accent
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "IP: $ipAddr",
                        color = CyberTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NODE: ${termName.uppercase()} [${attMode.uppercase()}]",
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = systemTime.substringAfter(" "),
                        color = CyberSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(CyberBorder)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiStatusText.uppercase(),
                    color = if (uiStatusText.contains("GRANTED") || uiStatusText.contains("SUCCESS") || uiStatusText.contains("WELCOME")) CyberPrimary else if (uiStatusText.contains("FAIL") || uiStatusText.contains("WARNING")) CyberError else CyberTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Viewport (Camera or Simulated Grid)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .border(2.dp, if (isRelayOpen) CyberPrimary else CyberBorder, RoundedCornerShape(20.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (scanMode != ScanMode.NONE) {
                if (simulationEnabled || !cameraPermissionGranted) {
                    // SIMULATION VIEWPORT DRAWING
                    CyberSimulatedGrid(faceBounds = faceBounds, progress = calibrationProgress)
                } else {
                    // REAL CAMERAX FEED
                    CameraPreview(
                        onFacesDetected = { faces, width, height ->
                            viewModel.onFacesDetected(faces, width, height)
                        }
                    )
                    // Canvas HUD overlay on Camera
                    CyberCameraHUDOverlay(faceBounds = faceBounds, progress = calibrationProgress)
                }

                // Laser Scan sweep line
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sweep"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineY = laserOffset * size.height
                    drawLine(
                        color = CyberPrimary,
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = 3.dp.toPx()
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(CyberPrimary.copy(alpha = 0.15f), Color.Transparent),
                            startY = lineY - 60.dp.toPx(),
                            endY = lineY
                        ),
                        topLeft = Offset(0f, lineY - 60.dp.toPx()),
                        size = Size(size.width, 60.dp.toPx())
                    )
                }

                // HUD Overlays inside the active viewport box
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Top-Left: Model
                    Text(
                        text = "[HIKVISION] $termModel",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    // Top-Right: live clock
                    Text(
                        text = systemTime,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // Forehead Temperature placement target overlay
                    if (tempCheck) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = (-60).dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .border(1.5.dp, CyberAccent, CircleShape)
                                    .background(CyberAccent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(CyberAccent, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lastTemp != null) "TEMP OK: $lastTemp" else "THERMO TEMP ZONE",
                                color = if (lastTemp != null) CyberPrimary else CyberAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bottom-Center Access Granted banner overlay
                    if (isRelayOpen) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-10).dp)
                                .border(2.dp, CyberPrimary, RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Relay Open",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ACCESS GRANTED: RELAY ACTIVE",
                                    color = CyberPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else {
                // Standby Viewport screen (Industrial look)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = CyberBorder,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TERMINAL STANDBY",
                        color = CyberTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SELECT MODE TO INITIATE SCANNER",
                        color = CyberBorder,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // If door relay is open, overlay it on Standby too!
                if (isRelayOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .border(2.dp, CyberPrimary, RoundedCornerShape(12.dp))
                                .background(CyberSurface)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Door Open",
                                    tint = CyberPrimary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "DOOR UNLOCKED",
                                    color = CyberPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Lock Relay Active",
                                    color = CyberTextSecondary,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration progression metrics
        if (scanMode != ScanMode.NONE) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCANNING CALIBRATION: ${(calibrationProgress * 100).toInt()}%",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    if (scanMode == ScanMode.REGISTRATION) {
                        Text(
                            text = "PHASE: ${livenessStep.name}",
                            color = CyberSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { calibrationProgress },
                    color = CyberPrimary,
                    trackColor = CyberBorder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (scanMode == ScanMode.REGISTRATION) {
                        viewModel.setScanMode(ScanMode.NONE)
                    } else {
                        showRegNameDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanMode == ScanMode.REGISTRATION) CyberError else CyberSurfaceVariant,
                    contentColor = if (scanMode == ScanMode.REGISTRATION) Color.White else CyberSecondary
                ),
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mode_registration_btn")
            ) {
                Text(
                    text = if (scanMode == ScanMode.REGISTRATION) "ABORT PROFILE" else "ENROLL FACE",
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Default,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = {
                    if (scanMode == ScanMode.VERIFICATION) {
                        viewModel.setScanMode(ScanMode.NONE)
                    } else {
                        viewModel.setScanMode(ScanMode.VERIFICATION)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scanMode == ScanMode.VERIFICATION) CyberError else CyberPrimary,
                    contentColor = Color.White
                ),
                shape = CircleShape,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("mode_verification_btn")
            ) {
                Text(
                    text = if (scanMode == ScanMode.VERIFICATION) "STOP SCAN" else "VERIFY MATCH",
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Default,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live terminal tickers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)
                .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                .background(CyberBackground)
                .padding(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {
                items(terminalLogs.reversed()) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("SUCCESS") || log.contains("GRANTED") || log.contains("OK")) CyberPrimary else if (log.contains("WARNING") || log.contains("ERROR") || log.contains("DEVIATION") || log.contains("DENIED")) CyberError else CyberTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }

    // Name prompt dialog
    if (showRegNameDialog) {
        Dialog(onDismissRequest = { showRegNameDialog = false }) {
            Surface(
                modifier = Modifier.border(1.dp, CyberBorder, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = CyberSurface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "BIOMETRIC PROFILE ID",
                        color = CyberPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Please input the name identifier for this face calibration envelope.",
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    TextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("e.g. SPECIAL_AGENT_CARTER", color = CyberTextSecondary, fontFamily = FontFamily.Monospace) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedIndicatorColor = CyberPrimary,
                            unfocusedIndicatorColor = CyberBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showRegNameDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = CyberTextSecondary)
                        ) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    viewModel.setScanMode(ScanMode.REGISTRATION, inputName)
                                    showRegNameDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.White),
                            shape = CircleShape,
                            modifier = Modifier.testTag("submit_registration_name_btn")
                        ) {
                            Text("CALIBRATE CORES", fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberCameraHUDOverlay(faceBounds: FaceBoundState?, progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw crosshairs
        drawLine(CyberBorder, Offset(w / 2, h / 2 - 20.dp.toPx()), Offset(w / 2, h / 2 + 20.dp.toPx()), 1.dp.toPx())
        drawLine(CyberBorder, Offset(w / 2 - 20.dp.toPx(), h / 2), Offset(w / 2 + 20.dp.toPx(), h / 2), 1.dp.toPx())

        // Draw scanning radar rings
        drawCircle(
            color = CyberBorder,
            radius = w / 2.8f,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = CyberPrimary.copy(alpha = 0.2f),
            radius = w / 3.4f,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Draw tracked face box
        if (faceBounds != null) {
            val fLeft = faceBounds.left * w
            val fTop = faceBounds.top * h
            val fRight = faceBounds.right * w
            val fBottom = faceBounds.bottom * h
            val fWidth = fRight - fLeft
            val fHeight = fBottom - fTop

            // Glowing target boundaries
            drawRect(
                color = if (faceBounds.eyeBlinked) CyberSecondary else CyberPrimary,
                topLeft = Offset(fLeft, fTop),
                size = Size(fWidth, fHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Draw futuristic target brackets at corners
            val bLength = (fWidth * 0.15f).coerceAtMost(24.dp.toPx())
            val bThick = 4.dp.toPx()
            val bracketColor = if (faceBounds.eyeBlinked) CyberSecondary else CyberPrimary

            // Top-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(fLeft, fTop + bLength)
                    lineTo(fLeft, fTop)
                    lineTo(fLeft + bLength, fTop)
                },
                color = bracketColor,
                style = Stroke(width = bThick)
            )

            // Top-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(fRight - bLength, fTop)
                    lineTo(fRight, fTop)
                    lineTo(fRight, fTop + bLength)
                },
                color = bracketColor,
                style = Stroke(width = bThick)
            )

            // Bottom-Left corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(fLeft, fBottom - bLength)
                    lineTo(fLeft, fBottom)
                    lineTo(fLeft + bLength, fBottom)
                },
                color = bracketColor,
                style = Stroke(width = bThick)
            )

            // Bottom-Right corner bracket
            drawPath(
                path = Path().apply {
                    moveTo(fRight - bLength, fBottom)
                    lineTo(fRight, fBottom)
                    lineTo(fRight, fBottom - bLength)
                },
                color = bracketColor,
                style = Stroke(width = bThick)
            )
        }
    }
}

@Composable
fun CyberSimulatedGrid(faceBounds: FaceBoundState?, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val gridStep = 24.dp.toPx()
                val w = size.width
                val h = size.height

                // Draw technical grid dots
                for (x in 0..(w / gridStep).toInt()) {
                    for (y in 0..(h / gridStep).toInt()) {
                        drawCircle(
                            color = CyberBorder,
                            radius = 1.5.dp.toPx(),
                            center = Offset(x * gridStep, y * gridStep)
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // High Tech Concentric scan graphics
        val infiniteTransition = rememberInfiniteTransition(label = "sim_radar")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rot"
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-10).dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width / 3.2f

            // Inner circle
            drawCircle(
                color = CyberSecondary.copy(alpha = 0.3f),
                radius = baseRadius * 0.6f,
                style = Stroke(width = 1.dp.toPx())
            )

            // Dynamic scan bracket lines
            drawCircle(
                color = CyberPrimary.copy(alpha = 0.4f),
                radius = baseRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Draw vector simulated face wireframe
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val rad = size.width / 2.3f

            // Head oval contour
            drawOval(
                color = CyberSecondary.copy(alpha = 0.4f),
                topLeft = Offset(center.x - rad * 0.8f, center.y - rad * 1.1f),
                size = Size(rad * 1.6f, rad * 2.2f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Horizontal eye-line grid
            drawLine(
                color = CyberBorder,
                start = Offset(center.x - rad * 0.8f, center.y - rad * 0.1f),
                end = Offset(center.x + rad * 0.8f, center.y - rad * 0.1f),
                strokeWidth = 1.dp.toPx()
            )

            // Left eye node
            drawCircle(
                color = if (faceBounds?.eyeBlinked == true) CyberSecondary else CyberPrimary,
                radius = if (faceBounds?.eyeBlinked == true) 2.dp.toPx() else 5.dp.toPx(),
                center = Offset(center.x - rad * 0.3f, center.y - rad * 0.1f)
            )

            // Right eye node
            drawCircle(
                color = if (faceBounds?.eyeBlinked == true) CyberSecondary else CyberPrimary,
                radius = if (faceBounds?.eyeBlinked == true) 2.dp.toPx() else 5.dp.toPx(),
                center = Offset(center.x + rad * 0.3f, center.y - rad * 0.1f)
            )

            // Nose node
            drawPath(
                path = Path().apply {
                    moveTo(center.x, center.y - rad * 0.1f)
                    lineTo(center.x - rad * 0.08f, center.y + rad * 0.3f)
                    lineTo(center.x + rad * 0.08f, center.y + rad * 0.3f)
                    close()
                },
                color = CyberPrimary.copy(alpha = 0.6f),
                style = Stroke(width = 1.dp.toPx())
            )

            // Mouth outline node
            drawOval(
                color = CyberSecondary.copy(alpha = 0.5f),
                topLeft = Offset(center.x - rad * 0.2f, center.y + rad * 0.5f),
                size = Size(rad * 0.4f, rad * 0.15f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Overlay corner bracket boxes
        CyberCameraHUDOverlay(faceBounds = faceBounds, progress = progress)
    }
}

@Composable
fun CameraPreview(
    onFacesDetected: (List<com.google.mlkit.vision.face.Face>, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview config
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Analysis config
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        FaceAnalyzer(onFacesDetected)
                    )
                }

            // Use Front Camera for Face ID
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun LogsTab(viewModel: VaultViewModel) {
    val logs by viewModel.accessLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Analytics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "BIOMETRIC MONITORING ANALYTICS",
                    color = CyberSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TOTAL SCANS", color = CyberTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("${logs.size}", color = CyberTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val successCount = logs.count { it.status == "ACCESS_GRANTED" || it.status == "CALIBRATED" }
                        val successRate = if (logs.isNotEmpty()) (successCount.toFloat() / logs.size * 100).toInt() else 100
                        Text("SUCCESS RATE", color = CyberTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$successRate%", color = CyberPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        val threats = logs.count { it.status == "ACCESS_DENIED" }
                        Text("THREAT ALERTS", color = CyberTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Text("$threats", color = if (threats > 0) CyberError else CyberTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACCESS HISTORIC LOG TAPE",
                color = CyberTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = "PURGE RECORDS",
                color = CyberError,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable { viewModel.clearHistoryLogs() }
                    .testTag("purge_logs_btn")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = CyberBorder, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "NO BIO-RECORDS COLLECTED",
                        color = CyberTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogItemRow(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItemRow(log: BiometricLog) {
    val formatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()) }
    val timeStr = formatter.format(java.util.Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when (log.status) {
                        "ACCESS_GRANTED" -> CyberPrimary.copy(alpha = 0.15f)
                        "CALIBRATED" -> CyberSecondary.copy(alpha = 0.15f)
                        else -> CyberError.copy(alpha = 0.15f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (log.status) {
                    "ACCESS_GRANTED" -> Icons.Default.LockOpen
                    "CALIBRATED" -> Icons.Default.Face
                    else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = when (log.status) {
                    "ACCESS_GRANTED" -> CyberPrimary
                    "CALIBRATED" -> CyberSecondary
                    else -> CyberError
                },
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.operatorName.uppercase(),
                    color = CyberTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = timeStr,
                    color = CyberTextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "${log.method} • Confidence: ${log.confidence.toInt()}%",
                color = CyberSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = log.details,
                color = CyberTextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProfileSettingsTab(viewModel: VaultViewModel) {
    val profiles by viewModel.activeProfiles.collectAsStateWithLifecycle()
    val simulationEnabled by viewModel.simulationEnabled.collectAsStateWithLifecycle()

    val termName by viewModel.terminalName.collectAsStateWithLifecycle()
    val termModel by viewModel.terminalModel.collectAsStateWithLifecycle()
    val ipAddr by viewModel.ipAddress.collectAsStateWithLifecycle()
    val doorDelay by viewModel.doorOpenDelay.collectAsStateWithLifecycle()
    val tempCheck by viewModel.tempCheckEnabled.collectAsStateWithLifecycle()
    val ttsVoice by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val attMode by viewModel.attendanceMode.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf(termName) }
    var editIp by remember { mutableStateOf(ipAddr) }
    var editDelay by remember { mutableIntStateOf(doorDelay) }
    var editTemp by remember { mutableStateOf(tempCheck) }
    var editVoice by remember { mutableStateOf(ttsVoice) }
    var editMode by remember { mutableStateOf(attMode) }

    LaunchedEffect(termName, ipAddr, doorDelay, tempCheck, ttsVoice, attMode) {
        editName = termName
        editIp = ipAddr
        editDelay = doorDelay
        editTemp = tempCheck
        editVoice = ttsVoice
        editMode = attMode
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enrolled user identity card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE SCAN ENVELOPE RECORD",
                        color = CyberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (profiles.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(CyberBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = CyberTextSecondary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "UNREGISTERED OPERATOR",
                                    color = CyberTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "No custom face envelope registered yet.",
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                             }
                        }
                    } else {
                        val profile = profiles.first()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(CyberPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Face, contentDescription = null, tint = CyberPrimary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = profile.name.uppercase(),
                                    color = CyberTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Registered: ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(profile.registeredAt))}",
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "BIOMETRIC VECTOR RATIOS:",
                            color = CyberSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Face Aspect: ${profile.faceWidthHeightRatio}", color = CyberTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Inter-ocular Distance: ${profile.eyeDistanceRatio}", color = CyberTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.clearAllProfiles() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberError.copy(alpha = 0.15f), contentColor = CyberError),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("clear_profiles_btn")
                        ) {
                            Text("PURGE PROFILE DATA", fontWeight = FontWeight.Medium, fontFamily = FontFamily.Default, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // HIKVISION Identity & Terminal Info configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HIKVISION TERMINAL IDENTITY",
                        color = CyberSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Device Model: $termModel", color = CyberTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Terminal ID Name", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editIp,
                        onValueChange = { editIp = it },
                        label = { Text("Terminal IP Network Address", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary,
                            focusedBorderColor = CyberPrimary,
                            unfocusedBorderColor = CyberBorder
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ATTENDANCE EVENT MODE:",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Check-In", "Check-Out")
                        modes.forEach { mode ->
                            Button(
                                onClick = { editMode = mode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (editMode == mode) CyberPrimary else CyberSurfaceVariant,
                                    contentColor = if (editMode == mode) Color.White else CyberTextPrimary
                                ),
                                shape = CircleShape,
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text(mode.uppercase(), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Biometric, Relay & Simulation preferences
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HARDWARE RELAY & BIOMETRIC CRITERIA",
                        color = CyberSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Door Relay delay slider
                    Text("Door Lock Relay Delay: $editDelay seconds", color = CyberTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Slider(
                        value = editDelay.toFloat(),
                        onValueChange = { editDelay = it.toInt() },
                        valueRange = 1f..15f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberPrimary,
                            activeTrackColor = CyberPrimary,
                            inactiveTrackColor = CyberBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Forehead temp check toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Forehead Thermal Sensor", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Simulate contactless thermal scanning during verification.", color = CyberTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = editTemp,
                            onCheckedChange = { editTemp = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberPrimary, checkedTrackColor = CyberPrimary.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice Prompts TTS toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TTS Voice Notifications", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Speak 'Thank you', 'Access Denied', and scanner warnings.", color = CyberTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = editVoice,
                            onCheckedChange = { editVoice = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberPrimary, checkedTrackColor = CyberPrimary.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulation Mode override
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulation Mode Override", color = CyberTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Plays simulated face scans (ideal for emulators).", color = CyberTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Switch(
                            checked = simulationEnabled,
                            onCheckedChange = { viewModel.toggleSimulationMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberPrimary, checkedTrackColor = CyberPrimary.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            viewModel.updateTerminalConfig(
                                name = editName,
                                ip = editIp,
                                delay = editDelay,
                                tempCheck = editTemp,
                                voice = editVoice,
                                mode = editMode
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary, contentColor = Color.White),
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("APPLY TERMINAL CHANGES", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }

        // About biometrics info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ENCRYPTION ENCLAVE STANDARD",
                        color = CyberSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This terminal complies with standard HikFace security protocol. Face scan biometric metrics are calculated using custom on-device AI algorithms and stored strictly sandboxed inside standard Android Room DB databases.",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
