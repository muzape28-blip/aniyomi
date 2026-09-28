package eu.kanade.presentation.more.settings.screen

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.core.diagnostics.Breadcrumb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** On-device diagnostics; the rendered window is bounded, while copy/share uses the full file. */
object DiagnosticsScreen : Screen() {
    private const val RENDER_LINES = 300

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val back = LocalBackPress.currentOrThrow
        val clipboard = LocalClipboardManager.current
        var reload by remember { mutableIntStateOf(0) }
        var fullLog by remember { mutableStateOf("") }
        var filter by remember { mutableStateOf("ALL") }
        var confirmClear by remember { mutableStateOf(false) }

        LaunchedEffect(reload) {
            fullLog = withContext(Dispatchers.IO) { Breadcrumb.dumpFull() }
        }

        val allLines = remember(fullLog) { fullLog.lineSequence().filter { it.isNotBlank() }.toList() }
        val filteredLines = remember(allLines, filter) {
            if (filter ==
                "ALL"
            ) {
                allLines
            } else {
                allLines.filter {
                    it.contains("| $filter") ||
                        it.contains("| STREAM_") &&
                        filter == "STREAM"
                }
            }
        }
        val visibleLines = filteredLines.takeLast(RENDER_LINES)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Diagnostics") },
                    navigationIcon = {
                        IconButton(onClick = back) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("ALL", "STREAM", "EXT", "RESOLVE", "PLAYER", "FATAL").forEach { name ->
                        if (filter == name) {
                            Button(onClick = { filter = name }) { Text(name) }
                        } else {
                            OutlinedButton(onClick = { filter = name }) { Text(name) }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(fullLog)) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy")
                        Text(" Copy")
                    }
                    TextButton(onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, fullLog)
                                },
                                "Share diagnostics",
                            ),
                        )
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                        Text(" Share")
                    }
                    TextButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear")
                        Text(" Clear")
                    }
                }

                Text(
                    text = if (filteredLines.size > visibleLines.size) {
                        "Showing last $RENDER_LINES of ${filteredLines.size} lines. Copy/share includes the full log."
                    } else {
                        "${filteredLines.size} lines"
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = visibleLines.joinToString("\n").ifBlank { "No diagnostics recorded." },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text("Clear diagnostics?") },
                text = { Text("This removes the active and archived breadcrumb logs.") },
                confirmButton = {
                    TextButton(onClick = {
                        Breadcrumb.clear()
                        confirmClear = false
                        reload++
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
                },
            )
        }
    }
}
