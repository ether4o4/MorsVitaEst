package com.ether4o4.morsvitaest.ui.launcher

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ether4o4.morsvitaest.data.AppSettings
import com.ether4o4.morsvitaest.data.HostedWidgetRect
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private const val APPWIDGET_HOST_ID = 0x4D5645 // "MVE"

// A brand-new widget starts at roughly half the board wide and a fifth tall — a sensible
// "medium" size the user can then drag-resize. Kept as board fractions.
private const val DEFAULT_WIDGET_W_FRAC = 0.5f
private const val DEFAULT_WIDGET_H_FRAC = 0.2f
private const val MIN_WIDGET_W_FRAC = 0.15f
private const val MIN_WIDGET_H_FRAC = 0.07f

/** One row in the widget picker: the provider plus its pre-resolved widget + app labels. */
private data class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val widgetLabel: String,
    val appLabel: String,
)

/**
 * Android implementation: a blank, normal-launcher-style widget board. The user long-presses
 * empty space to add a home-screen widget from ANY installed app, then drags it to move and
 * drags its corner to resize — the widgets are freely placed (position + size stored as board
 * fractions), not locked to a list.
 *
 * Adding flow (works whether or not MorsVitaEst is the default launcher):
 *   1. Long-press the board (or tap ＋) to open a custom picker listing every provider from
 *      [AppWidgetManager.getInstalledProviders].
 *   2. Allocate an id and try [AppWidgetManager.bindAppWidgetIdIfAllowed]. When MorsVitaEst
 *      holds the (default-launcher) bind permission this succeeds silently; otherwise it
 *      returns false and we launch the system `ACTION_APPWIDGET_BIND` consent dialog so the
 *      user can grant binding for that one widget.
 *   3. Run the widget's configure activity if it declares one, then host it at the spot the
 *      user long-pressed.
 *
 * The hosted [android.appwidget.AppWidgetHostView] consumes touches for its own content, so
 * move/resize/remove are driven by small overlay grips on each widget rather than by grabbing
 * the widget's face (which no launcher can reliably do).
 */
@Composable
actual fun AppWidgetsSection(contentColor: Color, modifier: Modifier) {
    val context = LocalContext.current
    val settings = koinInject<AppSettings>()
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val host = remember { AppWidgetHost(context, APPWIDGET_HOST_ID) }

    // Receive widget updates only while this page is on screen.
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }

    var widgetIds by remember { mutableStateOf(settings.getHostedWidgetIds()) }
    var rects by remember { mutableStateOf(settings.getHostedWidgetRects()) }
    var pendingConfigureId by remember { mutableStateOf(-1) }
    var pendingBindId by remember { mutableStateOf(-1) }
    var showPicker by remember { mutableStateOf(false) }
    // Board fraction the user long-pressed to add a widget, so the new one lands under the
    // finger. Null means "auto-place at center" (used by the ＋ button).
    var addAtFrac by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    fun persistRects(next: Map<Int, HostedWidgetRect>) {
        rects = next
        settings.setHostedWidgetRects(next)
    }

    fun add(id: Int) {
        if (id !in widgetIds) {
            val next = widgetIds + id
            widgetIds = next
            settings.setHostedWidgetIds(next)
        }
        // Seed a starting rect centered on the long-press point (or the board center).
        if (rects[id] == null) {
            val (cx, cy) = addAtFrac ?: (0.5f to 0.5f)
            val x = (cx - DEFAULT_WIDGET_W_FRAC / 2f).coerceIn(0f, 1f - DEFAULT_WIDGET_W_FRAC)
            val y = (cy - DEFAULT_WIDGET_H_FRAC / 2f).coerceIn(0f, 1f - DEFAULT_WIDGET_H_FRAC)
            persistRects(rects + (id to HostedWidgetRect(x, y, DEFAULT_WIDGET_W_FRAC, DEFAULT_WIDGET_H_FRAC)))
        }
        addAtFrac = null
    }

    fun drop(id: Int) {
        runCatching { host.deleteAppWidgetId(id) }
        val next = widgetIds - id
        widgetIds = next
        settings.setHostedWidgetIds(next)
        persistRects(rects - id)
    }

    // The widget's own configuration screen (when it declares one), run after a successful bind.
    val configureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingConfigureId
        pendingConfigureId = -1
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) add(id) else runCatching { host.deleteAppWidgetId(id) }
        }
    }

    // Once an id is bound to a provider, run its configure activity (if any) or host it directly.
    fun configureOrAdd(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        val configure = info?.configure
        if (configure != null) {
            pendingConfigureId = id
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            }
            if (runCatching { configureLauncher.launch(intent) }.isFailure) add(id)
        } else {
            add(id)
        }
    }

    // System consent dialog for binding — needed when MorsVitaEst isn't the default launcher.
    val bindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val id = pendingBindId
        pendingBindId = -1
        if (id != -1) {
            if (result.resultCode == Activity.RESULT_OK) configureOrAdd(id) else runCatching { host.deleteAppWidgetId(id) }
        }
    }

    fun pick(info: AppWidgetProviderInfo) {
        showPicker = false
        val id = host.allocateAppWidgetId()
        val allowed = runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider) }.getOrDefault(false)
        if (allowed) {
            configureOrAdd(id)
        } else {
            pendingBindId = id
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            if (runCatching { bindLauncher.launch(bindIntent) }.isFailure) {
                pendingBindId = -1
                runCatching { host.deleteAppWidgetId(id) }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boardWpx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boardHpx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Blank layer: long-press empty space to add a widget under the finger. A long-press
        // doesn't move, so this never steals the launcher's horizontal page swipe.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(boardWpx, boardHpx) {
                    detectTapGestures(
                        onLongPress = { off ->
                            addAtFrac = (off.x / boardWpx) to (off.y / boardHpx)
                            showPicker = true
                        },
                    )
                },
        ) {
            if (widgetIds.none { rects[it] != null }) {
                Text(
                    text = "Long-press to add a widget",
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        for (widgetId in widgetIds) {
            key(widgetId) {
                val info = remember(widgetId) { appWidgetManager.getAppWidgetInfo(widgetId) }
                val rect = rects[widgetId]
                if (info != null && rect != null) {
                    WidgetFrame(
                        host = host,
                        widgetId = widgetId,
                        info = info,
                        rect = rect,
                        boardWpx = boardWpx,
                        boardHpx = boardHpx,
                        contentColor = contentColor,
                        onChange = { persistRects(rects + (widgetId to it)) },
                        onRemove = { drop(widgetId) },
                    )
                }
            }
        }

        // A discoverable ＋ in the corner, for adding without knowing the long-press gesture.
        Text(
            text = "＋",
            color = contentColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable {
                    addAtFrac = null
                    showPicker = true
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }

    if (showPicker) {
        WidgetPickerDialog(
            appWidgetManager = appWidgetManager,
            onPick = { pick(it) },
            onDismiss = {
                showPicker = false
                addAtFrac = null
            },
        )
    }
}

/**
 * One freely-placed widget on the board: the hosted view plus its move grip (top), resize grip
 * (bottom-right corner) and remove ✕ (top-right). Position/size are kept in local state during a
 * drag and committed via [onChange] on release so we don't thrash SharedPreferences every frame.
 */
@Composable
private fun WidgetFrame(
    host: AppWidgetHost,
    widgetId: Int,
    info: AppWidgetProviderInfo,
    rect: HostedWidgetRect,
    boardWpx: Float,
    boardHpx: Float,
    contentColor: Color,
    onChange: (HostedWidgetRect) -> Unit,
    onRemove: () -> Unit,
) {
    val density = LocalDensity.current.density
    var xFrac by remember(widgetId) { mutableFloatStateOf(rect.xFrac) }
    var yFrac by remember(widgetId) { mutableFloatStateOf(rect.yFrac) }
    var wFrac by remember(widgetId) { mutableFloatStateOf(rect.wFrac) }
    var hFrac by remember(widgetId) { mutableFloatStateOf(rect.hFrac) }
    // Re-sync if the stored rect changes from elsewhere (e.g. another widget added/removed).
    LaunchedEffect(rect) {
        xFrac = rect.xFrac
        yFrac = rect.yFrac
        wFrac = rect.wFrac
        hFrac = rect.hFrac
    }

    Box(
        modifier = Modifier
            // Read position in the layout phase so moving only re-lays-out (no recompose).
            .offset { IntOffset((xFrac * boardWpx).roundToInt(), (yFrac * boardHpx).roundToInt()) }
            .size(
                width = (wFrac * boardWpx / density).dp,
                height = (hFrac * boardHpx / density).dp,
            )
            .clip(RoundedCornerShape(8.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .border(1.dp, contentColor.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .padding(3.dp),
    ) {
        AndroidView(
            factory = { ctx -> host.createView(ctx, widgetId, info) },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val wDp = (wFrac * boardWpx / density).toInt().coerceAtLeast(40)
                val hDp = (hFrac * boardHpx / density).toInt().coerceAtLeast(40)
                runCatching { view.updateAppWidgetSize(Bundle.EMPTY, wDp, hDp, wDp, hDp) }
            },
        )

        // Move grip — a small bar at the top. Drag it to reposition the widget freely.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
                .size(width = 46.dp, height = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.40f))
                .pointerInput(widgetId, boardWpx, boardHpx) {
                    detectDragGestures(
                        onDragEnd = { onChange(HostedWidgetRect(xFrac, yFrac, wFrac, hFrac)) },
                        onDragCancel = { onChange(HostedWidgetRect(xFrac, yFrac, wFrac, hFrac)) },
                    ) { change, drag ->
                        change.consume()
                        xFrac = (xFrac + drag.x / boardWpx).coerceIn(0f, (1f - wFrac).coerceAtLeast(0f))
                        yFrac = (yFrac + drag.y / boardHpx).coerceIn(0f, (1f - hFrac).coerceAtLeast(0f))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("⠿", color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Remove ✕ — top-right.
        Text(
            text = "✕",
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable { onRemove() }
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )

        // Resize grip — bottom-right corner. Drag to change width + height.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.40f))
                .pointerInput(widgetId, boardWpx, boardHpx) {
                    detectDragGestures(
                        onDragEnd = { onChange(HostedWidgetRect(xFrac, yFrac, wFrac, hFrac)) },
                        onDragCancel = { onChange(HostedWidgetRect(xFrac, yFrac, wFrac, hFrac)) },
                    ) { change, drag ->
                        change.consume()
                        // coerceAtLeast(MIN) on the upper bound keeps max >= min even if a
                        // widget sits hard against the right/bottom edge (coerceIn throws otherwise).
                        val maxW = (1f - xFrac).coerceAtLeast(MIN_WIDGET_W_FRAC)
                        val maxH = (1f - yFrac).coerceAtLeast(MIN_WIDGET_H_FRAC)
                        wFrac = (wFrac + drag.x / boardWpx).coerceIn(MIN_WIDGET_W_FRAC, maxW)
                        hFrac = (hFrac + drag.y / boardHpx).coerceIn(MIN_WIDGET_H_FRAC, maxH)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("◢", color = contentColor, fontSize = 12.sp)
        }
    }
}

/** Lists every installed widget provider on the device (label + owning app) so the user can
 *  add a widget from any app — not just the ones a default-launcher picker would surface. */
@Composable
private fun WidgetPickerDialog(
    appWidgetManager: AppWidgetManager,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    // Each row carries the resolved labels so the list doesn't re-resolve them on every recompose.
    val choices = remember {
        runCatching {
            appWidgetManager.installedProviders.map { info ->
                val widgetLabel = runCatching { info.loadLabel(pm) }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: info.provider.shortClassName.substringAfterLast('.')
                val appLabel = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrNull() ?: info.provider.packageName
                WidgetChoice(info, widgetLabel, appLabel)
            }.sortedWith(compareBy({ it.appLabel.lowercase() }, { it.widgetLabel.lowercase() }))
        }.getOrDefault(emptyList())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add a widget") },
        text = {
            if (choices.isEmpty()) {
                Text("No widgets are available on this device.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(choices) { choice ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(choice.info) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                        ) {
                            Text(choice.widgetLabel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(choice.appLabel, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
    )
}
