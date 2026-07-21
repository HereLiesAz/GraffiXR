package com.hereliesaz.graffux

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.hereliesaz.aznavrail.*
import com.hereliesaz.aznavrail.model.*
import com.hereliesaz.graffitixr.common.model.BlendMode
import com.hereliesaz.graffitixr.common.model.EditorPanel
import com.hereliesaz.graffitixr.common.model.EditorUiState
import com.hereliesaz.graffitixr.common.model.ShapeKind
import com.hereliesaz.graffitixr.common.model.Tool
import com.hereliesaz.graffitixr.design.theme.AppStrings
import com.hereliesaz.graffitixr.design.theme.Cyan
import com.hereliesaz.graffitixr.design.theme.rememberAppStrings
import com.hereliesaz.graffitixr.feature.editor.AlignMode
import com.hereliesaz.graffitixr.feature.editor.BackgroundColorDialog
import com.hereliesaz.graffitixr.feature.editor.BlendModePicker
import com.hereliesaz.graffitixr.feature.editor.CornerRadiusDialog
import com.hereliesaz.graffitixr.feature.editor.DocumentSizeDialog
import com.hereliesaz.graffitixr.feature.editor.EditorScreen
import com.hereliesaz.graffitixr.feature.editor.EditorViewModel
import com.hereliesaz.graffitixr.feature.editor.PolygonSidesDialog
import com.hereliesaz.graffitixr.feature.editor.ShapeSizeDialog
import com.hereliesaz.graffitixr.feature.editor.TextEditDialog
import com.hereliesaz.graffitixr.feature.editor.VectorStrokeDialog
import com.hereliesaz.graffitixr.feature.editor.toModelBlendMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Graffux entry point — hosts the shared [EditorScreen] (the single source of truth for the
 * multi-layer image editor, migrated from GraffitiXR into :feature:editor). The Hilt-provided
 * [EditorViewModel] and its whole dependency graph (core modules + native bridge) resolve here; the
 * screen forces DESIGN mode, so no AR / SLAM / co-op is involved.[span_3](start_span)[span_3](end_span)
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedImage = incomingImageUri(intent)
        setContent {
            MaterialTheme {
                GraffuxApp(sharedImageUri = sharedImage)
            }
        }
    }
}

/**
 * Extracts a single image [Uri] from an inbound share/view intent, or null if this launch isn't one.[span_4](start_span)[span_4](end_span)
 */
private fun incomingImageUri(intent: Intent?): Uri? {
    if (intent == null) return null
    val isImage = intent.type?.startsWith("image/") == true
    return when (intent.action) {
        Intent.ACTION_SEND ->
            if (isImage) IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java) else null
        Intent.ACTION_VIEW -> intent.data?.takeIf { isImage }
        else -> null
    }
}

@Composable
private fun GraffuxApp(sharedImageUri: Uri?) {
    val vm: EditorViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val uiState by vm.uiState.collectAsState()
    val strings = rememberAppStrings()
    val scope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()

    var showDocDialog by remember { mutableStateOf(false) }
    var showBlendDialog by remember { mutableStateOf(false) }
    var showStrokeDialog by remember { mutableStateOf(false) }
    var showCornerDialog by remember { mutableStateOf(false) }
    var showShapeSizeDialog by remember { mutableStateOf(false) }
    var showSidesDialog by remember { mutableStateOf(false) }
    var manualEditTextId by remember { mutableStateOf<String?>(null) }
    var showBgDialog by remember { mutableStateOf(false) }

    // Pre-calculate `@Composable` colors outside the non-composable DSL block
    val activeRailColor = MaterialTheme.colorScheme.onSurface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    LaunchedEffect(sharedImageUri) {
        sharedImageUri?.let { vm.onAddLayer(it) }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { vm.onAddLayer(it) } }

    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.onImportDocument(it) } }

    val brushPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.installExtensionFromUri(it) } }

    val brushes by vm.installedBrushes.collectAsState()

    val navItemColor = remember(uiState.canvasBackground) {
        val bg = uiState.canvasBackground
        val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
        if (luminance > 0.5f) Color.Black else Color.White
    }

    AzHostActivityLayout(navController = navController, initiallyExpanded = false) {
        azTheme(
            activeColor = activeRailColor, // Passed dynamically to avoid `@Composable` invocation errors
            defaultShape = AzButtonShape.CIRCLE,
            headerIconShape = AzHeaderIconShape.CIRCLE,
            translucentBackground = Color.Black.copy(alpha = 0.55f)
        )
        azConfig(
            noMenu = true,
            packButtons = true,
            dockingSide = if (uiState.isRightHanded) AzDockingSide.LEFT else AzDockingSide.RIGHT,
            railItemWidth = 56.dp 
        )

        ConfigureRailItems(
            vm = vm,
            uiState = uiState,
            brushes = brushes,
            strings = strings,
            navItemColor = navItemColor,
            activeColor = activeRailColor, // Pass down to DSL builder
            onBlendMode = { showBlendDialog = true },
            onStrokeWidth = { showStrokeDialog = true },
            onCornerRadius = { showCornerDialog = true },
            onShapeSize = { showShapeSizeDialog = true },
            onPolygonSides = { showSidesDialog = true },
            onEditText = { id -> manualEditTextId = id },
            onSettings = { showSettings = true },
            onInstallBrush = { brushPicker.launch(arrayOf("*/*")) }
        )

        background(weight = 0) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(vm = vm, modifier = Modifier.fillMaxSize())
            }
        }
        
        // Standalone Top-Right File Operations Dropdown[span_5](start_span)[span_5](end_span)
        onscreen(alignment = Alignment.TopEnd) {
            AzDropdownMenu(navController = navController) {
                azConfig(design = AzDropdownDesign.MENU, dockingSide = if (uiState.isRightHanded) AzDockingSide.RIGHT else AzDockingSide.LEFT)
                azItem(text = strings.nav.open, onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                azItem(text = "Open File", onClick = { documentPicker.launch(arrayOf("*/*")) })
                azItem(text = strings.nav.new, onClick = { vm.onAddBlankLayer() })
                azItem(text = "${uiState.documentWidth}×${uiState.documentHeight}", onClick = { showDocDialog = true })
                azItem(text = "Background", onClick = { showBgDialog = true })
                azItem(text = strings.nav.save, onClick = { vm.saveProject() })
                azItem(text = strings.nav.export, onClick = { vm.exportImage() })
                azItem(text = strings.nav.share, onClick = {
                    scope.launch {
                        try {
                            val uri = vm.exportForShare() ?: return@launch
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = android.content.ClipData.newRawUri(null, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(send, null).apply {
                                clipData = send.clipData
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(chooser)
                        } catch (t: Throwable) {
                            android.util.Log.w("Graffux", "Share failed", t)
                        }
                    }
                })
            }
        }

        // Onscreen Foreground Elements explicitly pinned over the canvas
        onscreen(alignment = Alignment.BottomCenter) {
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(onClick = { /* TODO: Bind to EditorScreen view reset */ }, containerColor = surfaceVariantColor) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Reset Canvas")
                }
                FloatingActionButton(onClick = { /* TODO: Bind to EditorScreen fit logic */ }, containerColor = surfaceVariantColor) {
                    Icon(Icons.Filled.FitScreen, contentDescription = "Fit to Screen")
                }
                FloatingActionButton(onClick = { /* TODO: Bind to EditorScreen undo */ }, containerColor = surfaceVariantColor) {
                    Icon(Icons.Filled.Undo, contentDescription = "Undo")
                }
                FloatingActionButton(onClick = { /* TODO: Bind to EditorScreen redo */ }, containerColor = surfaceVariantColor) {
                    Icon(Icons.Filled.Redo, contentDescription = "Redo")
                }
            }
        }

        onscreen(alignment = Alignment.Center) {
            if (showDocDialog) {
                DocumentSizeDialog(
                    currentWidth = uiState.documentWidth,
                    currentHeight = uiState.documentHeight,
                    onConfirm = { w, h ->
                        vm.setDocumentSize(w, h)
                        showDocDialog = false
                    },
                    onDismiss = { showDocDialog = false },
                )
            }

            if (showBlendDialog) {
                val activeLayer = uiState.layers.find { it.id == uiState.activeLayerId }
                BlendModePicker(
                    current = activeLayer?.blendMode?.toModelBlendMode() ?: BlendMode.SrcOver,
                    onSelect = { mode ->
                        vm.setBlendMode(mode)
                        showBlendDialog = false
                    },
                    onDismiss = { showBlendDialog = false },
                )
            }

            if (showStrokeDialog) {
                val activeLayer = uiState.layers.find { it.id == uiState.activeLayerId }
                VectorStrokeDialog(
                    currentWidth = activeLayer?.shapes?.firstOrNull()?.strokeWidth ?: 0f,
                    onApply = { w ->
                        vm.setVectorStrokeWidth(w)
                        showStrokeDialog = false
                    },
                    onDismiss = { showStrokeDialog = false },
                )
            }

            if (showCornerDialog) {
                val activeLayer = uiState.layers.find { it.id == uiState.activeLayerId }
                val rect = activeLayer?.shapes?.firstOrNull { it.kind == ShapeKind.RECTANGLE }
                CornerRadiusDialog(
                    currentRadius = rect?.cornerRadius ?: 0f,
                    onApply = { r ->
                        vm.setVectorCornerRadius(r)
                        showCornerDialog = false
                    },
                    onDismiss = { showCornerDialog = false },
                )
            }

            if (showShapeSizeDialog) {
                val activeLayer = uiState.layers.find { it.id == uiState.activeLayerId }
                val shape = activeLayer?.shapes?.firstOrNull()
                if (shape != null) {
                    ShapeSizeDialog(
                        currentWidth = shape.width,
                        currentHeight = shape.height,
                        isLine = shape.kind == ShapeKind.LINE,
                        onConfirm = { w, h ->
                            vm.setVectorSize(w, h)
                            showShapeSizeDialog = false
                        },
                        onDismiss = { showShapeSizeDialog = false },
                    )
                }
            }

            if (showSidesDialog) {
                val activeLayer = uiState.layers.find { it.id == uiState.activeLayerId }
                val polygon = activeLayer?.shapes?.firstOrNull { it.kind == ShapeKind.POLYGON }
                if (polygon != null) {
                    PolygonSidesDialog(
                        currentSides = polygon.sides,
                        onApply = { n ->
                            vm.setPolygonSides(n)
                            showSidesDialog = false
                        },
                        onDismiss = { showSidesDialog = false },
                    )
                }
            }

            val editTextId = uiState.autoEditTextLayerId ?: manualEditTextId
            if (editTextId != null) {
                val params = uiState.layers.find { it.id == editTextId }?.textParams
                if (params != null) {
                    TextEditDialog(
                        initialText = params.text,
                        initialSizeDp = params.fontSizeDp,
                        initialColorArgb = params.colorArgb,
                        initialBold = params.isBold,
                        initialItalic = params.isItalic,
                        onTextChange = { vm.onTextContentChanged(editTextId, it) },
                        onSizeChange = { vm.onTextSizeChanged(editTextId, it) },
                        onColorChange = { vm.onTextColorChanged(editTextId, it) },
                        onStyleChange = { b, i ->
                            vm.onTextStyleChanged(editTextId, b, i, params.hasOutline, params.hasDropShadow)
                        },
                        onDismiss = {
                            vm.consumeAutoEditTextLayer()
                            manualEditTextId = null
                        },
                    )
                }
            }

            if (showBgDialog) {
                BackgroundColorDialog(
                    current = uiState.canvasBackground,
                    onSelect = { vm.setCanvasBackground(it) },
                    onDismiss = { showBgDialog = false },
                )
            }

            if (showSettings) {
                SettingsScreen(
                    vm = settingsVm,
                    appVersion = BuildConfig.VERSION_NAME,
                    onClose = { showSettings = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun BrushSizePad(vm: EditorViewModel) {
    val state by vm.uiState.collectAsState()
    val density = LocalDensity.current
    var itemPx by remember { mutableFloatStateOf(120f) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { itemPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val maxPx = maxOf(itemPx, 1f)
                    if (drag.y != 0f) {
                        val cur = vm.uiState.value.brushSize
                        vm.setBrushSize((cur - drag.y * 0.5f).coerceIn(1f, maxPx))
                    }
                    if (drag.x != 0f) {
                        if (vm.uiState.value.activeBrushName != null) {
                            val cur = vm.uiState.value.brushFlow
                            vm.setBrushFlow((cur + drag.x * 0.005f).coerceIn(0f, 1f))
                        } else {
                            val cur = vm.uiState.value.brushFeathering
                            vm.setBrushFeathering((cur + drag.x * 0.005f).coerceIn(0f, 1f))
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val maxPx = maxOf(itemPx, 1f)
        val isStamp = state.activeBrushName != null
        val feather = state.brushFeathering
        val haloDp = with(density) { state.brushSize.coerceIn(1f, maxPx).toDp() }
        val coreDp = with(density) {
            (if (isStamp) state.brushSize else state.brushSize * (1f - feather * 0.7f))
                .coerceIn(2f, maxPx).toDp()
        }
        if (!isStamp && feather > 0.05f) {
            Box(Modifier.size(haloDp).background(Cyan.copy(alpha = 0.3f), CircleShape))
        }
        val coreAlpha = if (isStamp) state.brushFlow.coerceIn(0.08f, 1f) else 1f
        Box(Modifier.size(coreDp).background(Cyan.copy(alpha = coreAlpha), CircleShape))
    }
}

/**
 * ConfigureRailItems builder block.
 * Icons are strictly enforced alongside text for all rail items.
 */
private fun AzNavHostScope.ConfigureRailItems(
    vm: EditorViewModel,
    uiState: EditorUiState,
    brushes: List<Pair<String, String>>,
    strings: AppStrings,
    navItemColor: Color,
    activeColor: Color,
    onBlendMode: () -> Unit,
    onStrokeWidth: () -> Unit,
    onCornerRadius: () -> Unit,
    onShapeSize: () -> Unit,
    onPolygonSides: () -> Unit,
    onEditText: (String) -> Unit,
    onSettings: () -> Unit,
    onInstallBrush: () -> Unit,
) {
    val navStrings = strings.nav

    azRailHostItem(
        id = "grp.design", text = "Design", content = Icons.Filled.Brush, color = navItemColor,
        initiallyExpanded = true,
    )
    azRailSubItem(
        id = "tool.brush", hostId = "grp.design", text = uiState.activeBrushName ?: navStrings.brush,
        shape = AzButtonShape.NONE,
        content = Icons.Filled.Brush,
        co
