package ch.awakened.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.awakened.app.ui.theme.Sys

/** Leuchtender Rahmen im Holo-Stil: mehrere weiche Striche + scharfe Kontur. */
fun Modifier.glowBorder(color: Color = Sys.Cyan, radius: Dp = 6.dp, width: Dp = 1.5.dp, glow: Float = 1f): Modifier =
    drawBehind {
        val r = radius.toPx()
        for (i in 4 downTo 1) {
            val w = width.toPx() + i * 3.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = 0.06f * glow * (5 - i)),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = w),
            )
        }
        drawRoundRect(color = color, cornerRadius = CornerRadius(r, r), style = Stroke(width = width.toPx()))
    }

/** Eckmarken an den vier Ecken eines Fensters. */
fun Modifier.cornerTicks(color: Color = Sys.Cyan, len: Dp = 14.dp, inset: Dp = 5.dp): Modifier = drawBehind {
    val l = len.toPx(); val i = inset.toPx(); val sw = 2.dp.toPx()
    val w = size.width; val h = size.height
    val c = color.copy(alpha = 0.9f)
    drawLine(c, Offset(i, i), Offset(i + l, i), sw); drawLine(c, Offset(i, i), Offset(i, i + l), sw)
    drawLine(c, Offset(w - i, i), Offset(w - i - l, i), sw); drawLine(c, Offset(w - i, i), Offset(w - i, i + l), sw)
    drawLine(c, Offset(i, h - i), Offset(i + l, h - i), sw); drawLine(c, Offset(i, h - i), Offset(i, h - i - l), sw)
    drawLine(c, Offset(w - i, h - i), Offset(w - i - l, h - i), sw); drawLine(c, Offset(w - i, h - i), Offset(w - i, h - i - l), sw)
}

/** Hintergrund: Navy-Verlauf, feines Raster und eine langsam wandernde Scanlinie. */
@Composable
fun SystemBackground(modifier: Modifier = Modifier, accent: Color = Sys.Cyan, base: Color = Sys.Navy0, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "scan")
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "scanY")
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(base, Color(0xFF081831).takeIf { base == Sys.Navy0 } ?: base, base)))
            .drawBehind {
                val step = 32.dp.toPx()
                val gridColor = accent.copy(alpha = 0.05f)
                var x = 0f
                while (x < size.width) { drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                var y = 0f
                while (y < size.height) { drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1f); y += step }
                val sy = scan * size.height
                drawRect(
                    Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.06f), Color.Transparent), sy - 60f, sy + 60f),
                    topLeft = Offset(0f, sy - 60f), size = Size(size.width, 120f),
                )
            },
        content = content,
    )
}

/** Das klassische System-Fenster mit Titelzeile. */
@Composable
fun SystemWindow(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color = Sys.Cyan,
    icon: String? = "!",
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glowBorder(accent)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.verticalGradient(listOf(Sys.PanelHi.copy(alpha = 0.92f), Sys.Panel.copy(alpha = 0.92f), Sys.Navy1.copy(alpha = 0.95f))))
            .cornerTicks(accent)
            .padding(contentPadding),
    ) {
        if (title != null) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (icon != null) {
                    Box(
                        Modifier.size(26.dp).glowBorder(accent, 4.dp, 1.2.dp, 0.6f),
                        contentAlignment = Alignment.Center,
                    ) { Text(icon, color = accent, style = MaterialTheme.typography.titleMedium) }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    title,
                    color = Sys.Text,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .drawBehind {
                            drawLine(accent.copy(alpha = 0.7f), Offset(0f, size.height + 4f), Offset(size.width, size.height + 4f), 2f)
                        }
                        .padding(horizontal = 6.dp),
                )
            }
        }
        content()
    }
}

@Composable
fun SystemButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Sys.Cyan,
    filled: Boolean = false,
    enabled: Boolean = true,
    minHeight: Dp = 52.dp,
) {
    val c = if (enabled) accent else Sys.Dim
    Box(
        modifier
            .defaultMinSize(minHeight = minHeight, minWidth = 48.dp)
            .glowBorder(c, 6.dp, 1.5.dp, if (enabled) 1f else 0.3f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (filled && enabled) c.copy(alpha = 0.22f) else c.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), color = if (enabled) Sys.Text else Sys.Dim, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun GlowProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Sys.Cyan, height: Dp = 10.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "progress")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(Sys.Navy0, cornerRadius = r)
        drawRoundRect(color.copy(alpha = 0.35f), cornerRadius = r, style = Stroke(1.dp.toPx()))
        if (p > 0f) {
            val w = size.width * p
            drawRoundRect(color.copy(alpha = 0.25f), topLeft = Offset(0f, -3f), size = Size(w, size.height + 6f), cornerRadius = r)
            drawRoundRect(
                Brush.horizontalGradient(listOf(color.copy(alpha = 0.6f), color, Color.White.copy(alpha = 0.9f)), 0f, w),
                size = Size(w, size.height), cornerRadius = r,
            )
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Sys.Cyan) {
    Text(
        text.uppercase(), color = color, style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Sys.Text) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = Sys.Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SystemText(text: String, modifier: Modifier = Modifier, color: Color = Sys.Text, align: TextAlign = TextAlign.Start) {
    Text(text, color = color, style = MaterialTheme.typography.bodyLarge, modifier = modifier, textAlign = align)
}

/** Kleine runde Zähltaste mit grosser Touch-Fläche. */
@Composable
fun RowScope.CountButton(label: String, onClick: () -> Unit, accent: Color = Sys.Cyan, enabled: Boolean = true) {
    SystemButton(label, onClick, Modifier.weight(1f), accent = accent, enabled = enabled, minHeight = 48.dp)
}

@Composable
fun SystemSwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Sys.Text, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, color = Sys.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Sys.Navy0, checkedTrackColor = Sys.Cyan,
                uncheckedThumbColor = Sys.Muted, uncheckedTrackColor = Sys.Navy0, uncheckedBorderColor = Sys.Dim,
            ),
        )
    }
}

@Composable
fun SystemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    decimal: Boolean = false,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = when {
                decimal -> KeyboardType.Decimal
                numeric -> KeyboardType.Number
                else -> KeyboardType.Text
            },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Sys.Cyan, unfocusedBorderColor = Sys.Dim,
            focusedLabelColor = Sys.Cyan, unfocusedLabelColor = Sys.Muted,
            cursorColor = Sys.Cyan, focusedTextColor = Sys.Text, unfocusedTextColor = Sys.Text,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

/** Auswahl aus mehreren Optionen als Reihe von Buttons. */
@Composable
fun <T> SegmentedChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            val isSel = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .glowBorder(if (isSel) Sys.Cyan else Sys.Dim, 6.dp, if (isSel) 1.8.dp else 1.dp, if (isSel) 1f else 0.2f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSel) Sys.Cyan.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(o) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(o), color = if (isSel) Sys.Text else Sys.Muted, style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** Dialog im System-Look. */
@Composable
fun SystemDialog(
    title: String,
    onDismiss: () -> Unit,
    accent: Color = Sys.Cyan,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissible, dismissOnBackPress = dismissible),
    ) {
        SystemWindow(Modifier.padding(20.dp), title = title, accent = accent, content = content)
    }
}

@Composable
fun StatusDot(ok: Boolean) {
    Box(
        Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (ok) Sys.Green else Sys.Red),
    )
}

