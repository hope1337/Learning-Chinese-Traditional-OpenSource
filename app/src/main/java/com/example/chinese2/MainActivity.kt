package com.example.chinese2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.chinese2.backup.BackupCodec
import com.example.chinese2.backup.BackupPayload
import com.example.chinese2.dictionary.DictionaryEntry
import com.example.chinese2.dictionary.MoeDictionary
import com.example.chinese2.models.ModelManager
import com.example.chinese2.models.ModelRole
import com.example.chinese2.ocr.CropRegion
import com.example.chinese2.ocr.ImagePipeline
import com.example.chinese2.ocr.MlKitOcrEngine
import com.example.chinese2.ocr.OcrDocument
import com.example.chinese2.ocr.OcrMode
import com.example.chinese2.ocr.OnnxOcrEngine
import com.example.chinese2.pronunciation.TtsSpeaker
import com.example.chinese2.storage.AppDataRoot
import com.example.chinese2.storage.Flashcard
import com.example.chinese2.storage.HistoryStore
import com.example.chinese2.storage.UninstallSurvivingStore
import com.example.chinese2.storage.UserDataRepository
import com.example.chinese2.strokes.StrokeRepository
import com.example.chinese2.strokes.StrokeViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private enum class Screen { HOME, REGION, REVIEW, RESULT, CHARACTER, CARDS }

class MainActivity : ComponentActivity() {
    private var speaker: TtsSpeaker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speaker = TtsSpeaker(applicationContext)
        setContent {
            MaterialTheme {
                FullApp(onSpeak = { speaker?.speak(it) }, onStop = { speaker?.stop() })
            }
        }
    }

    override fun onDestroy() {
        speaker?.release()
        speaker = null
        super.onDestroy()
    }
}

@Composable
private fun FullApp(onSpeak: (String) -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dictionary = remember { MoeDictionary(context.applicationContext) }
    val dataRoot = remember { AppDataRoot(context.applicationContext) }
    val store = remember { UninstallSurvivingStore(context.applicationContext, dataRoot) }
    val internalRoot = remember { File(context.filesDir, "app-data-root").apply { mkdirs() } }
    val historyStore = remember { HistoryStore(internalRoot) }
    val strokeRepo = remember { StrokeRepository(context.applicationContext) }
    val modelManager = remember { ModelManager() }
    val mlKit = remember { MlKitOcrEngine(context.applicationContext) }
    val onnx = remember {
        OnnxOcrEngine(
            activeModelId = { modelManager.activeId(ModelRole.OCR) },
            weightsPresent = { false },
        )
    }

    var screen by remember { mutableStateOf(Screen.HOME) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var region by remember { mutableStateOf(CropRegion()) }
    var turns by remember { mutableIntStateOf(0) }
    var ocrMode by remember { mutableStateOf(OcrMode.FAST) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var rawDoc by remember { mutableStateOf<OcrDocument?>(null) }
    var draft by remember { mutableStateOf("") }
    var confirmed by remember { mutableStateOf("") }
    var segments by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }
    var selectedChar by remember { mutableStateOf<String?>(null) }
    var cards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    var showModels by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    // SRS session state
    var queue by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    fun notify(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun refreshCards() {
        cards = runCatching { store.loadCards() }.getOrDefault(emptyList())
    }

    fun refreshHistory() {
        history = runCatching { historyStore.load().map { it.text } }.getOrDefault(emptyList())
    }

    LaunchedEffect(screen) {
        if (screen == Screen.CARDS) refreshCards()
        if (screen == Screen.HOME) refreshHistory()
    }

    val cameraFile = remember {
        File(File(context.cacheDir, "photos").apply { mkdirs() }, "capture.jpg")
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) {
            photoPath = cameraFile.absolutePath
            region = CropRegion()
            turns = 0
            rawDoc = null
            screen = Screen.REGION
        }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val dst = ImagePipeline.copyToCache(context, uri, "picked-${System.currentTimeMillis()}.jpg")
            if (dst != null) {
                photoPath = dst.absolutePath
                region = CropRegion()
                turns = 0
                rawDoc = null
                screen = Screen.REGION
            } else notify("Could not read that photo")
        }
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && dataRoot.adoptTreeUri(uri)) {
            notify("Data folder linked. Cards now survive reinstall.")
            refreshCards()
        } else notify("Folder not linked")
    }
    val exportDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            runCatching {
                BackupCodec.export(
                    context, uri,
                    BackupPayload(cards = store.loadCards(), history = history),
                )
                notify("Backup exported")
            }.onFailure { notify("Export failed: ${it.message}") }
        }
    }
    val importDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val payload = BackupCodec.parse(context, uri)
                payload.cards.forEach { store.saveCard(it) }
                payload.history.forEach { historyStore.add(it) }
                refreshCards()
                refreshHistory()
                notify("Restored ${payload.cards.size} cards")
            }.onFailure { notify("Import failed: ${it.message}") }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) takePicture.launch(cameraUri)
        else notify("Camera permission denied")
    }

    fun runOcr() {
        val path = photoPath ?: return
        scope.launch {
            busy = true
            error = null
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ImagePipeline.cropForOcr(File(path), region, turns)
                } ?: throw IllegalStateException("Could not decode photo")
                val doc = withContext(Dispatchers.Default) {
                    when (ocrMode) {
                        OcrMode.FAST -> mlKit.recognize(bytes, ocrMode)
                        else -> onnx.recognize(bytes, ocrMode)
                    }
                }
                rawDoc = doc
                draft = doc.rawText
                if (doc.rawText.isBlank()) {
                    error = "No text recognized (${doc.engine}). Try a tighter crop or better light."
                } else {
                    screen = Screen.REVIEW
                }
            } catch (t: Exception) {
                error = t.message ?: "Recognition failed"
            } finally {
                busy = false
            }
        }
    }

    fun confirmReview() {
        if (draft.isBlank()) {
            error = "Text is empty; edit or retake."
            return
        }
        confirmed = draft.trim().replace("\\s+".toRegex(), "")
        segments = dictionary.segment(confirmed)
        runCatching { historyStore.add(confirmed) }
        refreshHistory()
        screen = Screen.RESULT
    }

    fun gradeCurrent(grade: UserDataRepository.ReviewGrade) {
        val current = queue.getOrNull(index) ?: return
        val repo = UserDataRepository(internalRoot)
        val updated = repo.review(current, grade)
        store.saveCard(updated)
        refreshCards()
        if (index < queue.size - 1) {
            index++
            revealed = false
        } else {
            notify("Session complete")
            queue = emptyList()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

        when (screen) {
            Screen.HOME -> {
                Text("Learn Traditional Chinese (Lab)", style = MaterialTheme.typography.headlineMedium)
                Text("Test build — keeps the old “Chinese” app untouched.")
                Text("Tip: link a different folder than the old app, e.g. Documents/Chinese2Lab.")
                Text("Dictionary entries bundled: ${dictionary.size()}")
                Text("Data: ${dataRoot.displayName()}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) takePicture.launch(cameraUri)
                        else cameraPermission.launch(Manifest.permission.CAMERA)
                    }) { Text("Take photo") }
                    Button(onClick = {
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Choose photo") }
                }
                Text("OCR mode: ${ocrMode.name}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OcrMode.entries.forEach { m ->
                        OutlinedButton(onClick = { ocrMode = m }) { Text(m.name) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showModels = true }) { Text("Models") }
                    OutlinedButton(onClick = { showSources = true }) { Text("Sources") }
                    OutlinedButton(onClick = { screen = Screen.CARDS }) {
                        Text("Flashcards (${cards.size})")
                    }
                }
                Text("Data & backup", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickFolder.launch(null) }) { Text("Link folder") }
                    OutlinedButton(onClick = {
                        exportDoc.launch("chinese2-backup.zip")
                    }) { Text("Export") }
                    OutlinedButton(onClick = { importDoc.launch(arrayOf("application/zip")) }) { Text("Import") }
                }
                if (history.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.titleMedium)
                    history.take(10).forEach { h ->
                        OutlinedButton(onClick = {
                            confirmed = h
                            segments = dictionary.segment(h)
                            screen = Screen.RESULT
                        }) { Text(h.take(24)) }
                    }
                }
            }

            Screen.REGION -> {
                Text("Select region, then recognize", style = MaterialTheme.typography.titleMedium)
                PhotoPreview(photoPath)
                SliderRow("Left", region.left) { region = region.copy(left = it.coerceAtMost(region.right - 0.05f)) }
                SliderRow("Top", region.top) { region = region.copy(top = it.coerceAtMost(region.bottom - 0.05f)) }
                SliderRow("Right", region.right) { region = region.copy(right = it.coerceAtLeast(region.left + 0.05f)) }
                SliderRow("Bottom", region.bottom) { region = region.copy(bottom = it.coerceAtLeast(region.top + 0.05f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { turns = (turns + 1) % 4 }) { Text("Rotate ($turns)") }
                    OutlinedButton(onClick = { region = CropRegion() }) { Text("Full image") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::runOcr) { Text("Recognize (${ocrMode.name})") }
                    OutlinedButton(onClick = { screen = Screen.HOME }) { Text("Back") }
                }
                rawDoc?.let { Text("Engine: ${it.engine}") }
            }

            Screen.REVIEW -> {
                Text("Check the text, then confirm", style = MaterialTheme.typography.titleMedium)
                Text("Raw engine output stays separate from confirmed text.")
                TextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::confirmReview) { Text("Confirm") }
                    OutlinedButton(onClick = ::runOcr) { Text("Retry OCR") }
                    OutlinedButton(onClick = { screen = Screen.REGION }) { Text("Adjust region") }
                }
            }

            Screen.RESULT -> {
                Text("Result", style = MaterialTheme.typography.titleMedium)
                Text(confirmed, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSpeak(confirmed) }) { Text("Speak") }
                    OutlinedButton(onClick = { onStop() }) { Text("Stop") }
                    OutlinedButton(onClick = {
                        store.saveCard(
                            Flashcard(id = "sentence:${UUID.randomUUID()}", text = confirmed, meaning = ""),
                        )
                        refreshCards()
                        notify("Saved as flashcard")
                    }) { Text("Save sentence") }
                    OutlinedButton(onClick = { screen = Screen.HOME }) { Text("Home") }
                }
                Text("Words (longest match):", style = MaterialTheme.typography.titleSmall)
                segments.forEach { entry ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${entry.headword}  ${entry.pinyin}")
                            Text(entry.meaning, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = {
                            selectedChar = entry.headword.firstOrNull()?.toString()
                            screen = Screen.CHARACTER
                        }) { Text("Detail") }
                    }
                }
                Text("Characters: tap to inspect")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    confirmed.toList().distinct().take(12).forEach { ch ->
                        OutlinedButton(onClick = {
                            selectedChar = ch.toString()
                            screen = Screen.CHARACTER
                        }) { Text(ch.toString()) }
                    }
                }
            }

            Screen.CHARACTER -> {
                val glyph = selectedChar ?: ""
                val entry = dictionary.lookup(glyph)
                Text(glyph, style = MaterialTheme.typography.displayLarge)
                Text("Pinyin: ${entry.pinyin.ifBlank { "—" }}")
                Text("Meaning: ${entry.meaning}")
                Text("Han-Viet: ${entry.hanViet.ifBlank { "—" }}")
                Text("Source: ${entry.source} (Taiwan MOE seed sample)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSpeak(glyph) }) { Text("Speak") }
                    OutlinedButton(onClick = {
                        store.saveCard(Flashcard(id = "character-$glyph", text = glyph, meaning = entry.meaning))
                        refreshCards()
                        notify("Saved $glyph")
                    }) { Text("Save") }
                    OutlinedButton(onClick = { screen = Screen.RESULT }) { Text("Back") }
                }
                StrokeViewer(glyph = glyph, repository = strokeRepo)
            }

            Screen.CARDS -> {
                val due = cards.filter { it.dueAt <= System.currentTimeMillis() }
                Text("Flashcards", style = MaterialTheme.typography.titleMedium)
                Text("Due: ${due.size} / ${cards.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { screen = Screen.HOME }) { Text("Home") }
                    OutlinedButton(onClick = {
                        queue = due.ifEmpty { cards }.shuffled().take(20)
                        index = 0
                        revealed = false
                        if (queue.isEmpty()) notify("No cards yet")
                    }) { Text("Review") }
                    OutlinedButton(onClick = {
                        store.deleteAll()
                        historyStore.clear()
                        refreshCards()
                        refreshHistory()
                        notify("All local data deleted")
                    }) { Text("Delete all") }
                }
                if (queue.isNotEmpty()) {
                    val current = queue.getOrNull(index)
                    if (current != null) {
                        Text("Card ${index + 1} / ${queue.size}", style = MaterialTheme.typography.titleSmall)
                        Text(current.text, style = MaterialTheme.typography.headlineMedium)
                        if (!revealed) {
                            Button(onClick = { revealed = true }) { Text("Reveal answer") }
                        } else {
                            val full = dictionary.lookup(current.text)
                            Text("Pinyin: ${full.pinyin.ifBlank { "—" }}")
                            Text("Meaning: ${current.meaning.ifBlank { full.meaning }}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { gradeCurrent(UserDataRepository.ReviewGrade.AGAIN) }) { Text("Again") }
                                OutlinedButton(onClick = { gradeCurrent(UserDataRepository.ReviewGrade.HARD) }) { Text("Hard") }
                                OutlinedButton(onClick = { gradeCurrent(UserDataRepository.ReviewGrade.GOOD) }) { Text("Good") }
                                OutlinedButton(onClick = { gradeCurrent(UserDataRepository.ReviewGrade.EASY) }) { Text("Easy") }
                            }
                            OutlinedButton(onClick = { onSpeak(current.text) }) { Text("Speak") }
                        }
                    }
                }
                Text("All cards:", style = MaterialTheme.typography.titleSmall)
                cards.forEach { card ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(card.text)
                            Text(card.meaning, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = {
                            store.deleteCard(card.id)
                            refreshCards()
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }

    if (showModels) {
        AlertDialog(
            onDismissRequest = { showModels = false },
            title = { Text("Models") },
            text = {
                Column {
                    Text("Active OCR: ${modelManager.activeId(ModelRole.OCR) ?: "mlkit-chinese-fast (default)"}")
                    ModelManager.BUILT_IN.forEach { m ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${m.displayName} [${m.id}]", modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                modelManager.setActive(m.role, m.id)
                                showModels = false
                            }) { Text("Use") }
                        }
                    }
                    Text("Weights are never committed. ONNX contributors see models/CONTRIBUTING-MODELS.md.")
                }
            },
            confirmButton = { TextButton(onClick = { showModels = false }) { Text("Close") } },
        )
    }
    if (showSources) {
        AlertDialog(
            onDismissRequest = { showSources = false },
            title = { Text("Dictionary sources") },
            text = {
                Text(
                    "Seed sample: Taiwan MOE (CC BY-NC-ND 3.0 TW). " +
                        "Full import via tools/fetch_moe.py on the maintainer PC. " +
                        "Traditional forms preserved exactly; no Simplified conversion.",
                )
            },
            confirmButton = { TextButton(onClick = { showSources = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Text("$label: ${(value * 100).toInt()}%")
    Slider(value = value, onValueChange = onChange, valueRange = 0f..1f)
}

@Composable
private fun PhotoPreview(photoPath: String?) {
    var bitmap by remember(photoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(photoPath) {
        bitmap = withContext(Dispatchers.IO) {
            photoPath?.let { ImagePipeline.loadPreview(File(it)) }
        }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "Selected photo")
    } ?: Text("Loading preview…")
}
