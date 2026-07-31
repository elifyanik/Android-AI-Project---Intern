package com.example.demo1
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.demo1.data.ChatDatabase
import com.example.demo1.data.ChatMessageEntity
import com.example.demo1.data.ChatSessionEntity
import com.example.demo1.data.ChatSessionPreview
import com.example.demo1.llm.LlmEngine
import com.example.demo1.ui.theme.DemoTheme
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.demo1.speech.VoskSpeechManager
import androidx.compose.material.icons.filled.AttachFile
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.LocalContentColor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import org.apache.poi.xwpf.usermodel.XWPFDocument
import android.provider.OpenableColumns



enum class ColorTheme {
    Neutral,
    Purple,
    Blue,
    Pink
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { //onCreate, Activity ilk kez oluşturulduğunda çalışan fonksiyondur.
        super.onCreate(savedInstanceState)

        setContent { //setContent, Jetpack Compose kullanılarak ekranda gösterilecek arayüzü belirler.
            var isDarkTheme by remember {
                mutableStateOf(true)
            }

            var selectedColorTheme by remember {
                mutableStateOf(ColorTheme.Neutral)
            }
            DemoTheme(
                darkTheme = isDarkTheme,
                colorTheme = selectedColorTheme,
                dynamicColor = false                                                                    //“Renkleri telefondan alma. Benim Theme.kt dosyasında tanımladığım renkleri kullan.”
            ) {                                                                         //“Bu tema ayarlarını uygula ve bu temanın içinde ChatScreen ekranını göster.”
                ChatScreen(
                    isDarkTheme = isDarkTheme,
                    selectedColorTheme = selectedColorTheme,
                    onThemeModeChange = { newIsDarkTheme ->                             //sagdaki parametre.
                        isDarkTheme = newIsDarkTheme
                    },
                    onColorThemeChange = { newColorTheme ->                     //isdarktheme degeri tutuyor, ama SettingsPanel içinden o değeri değiştirmek için ayrıca bir yol gerekiyor.
                        selectedColorTheme = newColorTheme
                    }
                )
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

data class ChatSession(
    val id: Long,
    val title: String,
    val messageCount: Int,
    val isPinned: Boolean
)

@Composable
fun ChatScreen(
    isDarkTheme: Boolean,
    selectedColorTheme: ColorTheme,
    onThemeModeChange: (Boolean) -> Unit, //Buradaki Unit, bu fonksiyonun geriye bir değer döndürmediği anlamına gelir.
    onColorThemeChange: (ColorTheme) -> Unit
) {
    val context = LocalContext.current

    val llmEngine = remember {
        LlmEngine(context)
    }

    val database = remember {
        ChatDatabase.getDatabase(context)
    }

    val chatDao = remember {
        database.chatDao()
    }

    val scope = rememberCoroutineScope() //Bu satır, Compose ekranı içinde coroutine başlatabilmek için bir CoroutineScope oluşturur. Coroutine, uzun sürebilecek bir işi uygulamanın ekranını dondurmadan çalıştırmamızı sağlayan yapıdır.


    var inputText by remember {
        mutableStateOf("")
    }

    var uploadedFileText by remember{
        mutableStateOf("") }


    var uploadedFileName by remember {
        mutableStateOf("") }

    var isListening by remember {
        mutableStateOf(false) }

    val voskSpeechManager = remember {
        VoskSpeechManager( //Burada VoskSpeechManager adı büyük harfle başladığı için sınıf olma ihtimali yüksektir
            context = context,///VoskSpeechManager sınıfına Android context bilgisi gönderiliyor.

            onTextResult = { recognizedText ->
                inputText = recognizedText//Vosk tarafından tanınan metni temsil eder.
            },
            onError = { errorMessage ->
                inputText = errorMessage
            }
        )
    }

    DisposableEffect(Unit) { //DisposableEffect, Composable ekrandan kaldırıldığında temizleme işlemi yapmak için kullanılır.
        onDispose {
            llmEngine.close()
            voskSpeechManager.close()
        }
    }

    LaunchedEffect(Unit) { //LaunchedEffect, Composable ekrana geldiğinde coroutine içinde işlem başlatmak için kullanılır.
        PDFBoxResourceLoader.init(context) //PDF dosyalarını okuyabilmek için PDFBox başlatılır.
        voskSpeechManager.initialize() //Vosk sistemini hazırlar. Vosk modelini yükler vs
    }

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(//Bu yapı, Android’den bir sonuç alınacak işlemleri Compose içinde yönetir. Burada alınacak sonuç, kullanıcının mikrofon iznine verdiği cevaptır.
            contract = ActivityResultContracts.RequestPermission() //contract, hangi işlemin yapılacağını belirtir.
        ) { isGranted ->
            if (isGranted) {
                voskSpeechManager.startListening()
                isListening = true
            } else {
                inputText = "Microphone permission not granted."
            }
        }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
              contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) { ////Çünkü kullanıcı dosya seçmeden geri tuşuna basabilir. O zaman uri boş olabilir.
                val isPdf = isPdfFile(context, uri)
                val isDocx = isDocxFile(context, uri)
                val isImage = isImageFile(context, uri)
                val selectedFileName = getFileNameFromUri(context, uri)


                if (isPdf) {
                    uploadedFileName = selectedFileName
                    inputText = "PDF okunuyor..." //readHybridPdfTextFromUri() bittikten sonra inputtext alanı tekrar degistirilir "" yapılır


                    scope.launch {
                        uploadedFileText = withContext(Dispatchers.Default) {
                            readHybridPdfTextFromUri( //kullanıcı pdf i secer secmez bu fonk calıstırılır
                                context = context,
                                uri = uri,
                                maxPages = 3
                            )
                        }

                        inputText =
                            if (uploadedFileText.isBlank()) {
                                "PDF içinden metin çıkarılamadı."
                            } else {
                                //"PDF okundu. Karakter sayısı: ${uploadedFileText.length}"
                                //asagıdaki satır, acaba dosya gercekten gonderildi mi ve icerigi dogru sekilde okundu mu testi icin
                                //"File selected. Character count: ${uploadedFileText.length}. First part: ${uploadedFileText.take(80)}"
                                ""
                            }
                    }
                } else if (isDocx) {
                    uploadedFileName = selectedFileName
                    uploadedFileText = ""
                    inputText = "DOCX okunuyor..."

                    scope.launch { //uzun durdugu icin ispdf gib arka planda okunsun
                        uploadedFileText = withContext(Dispatchers.Default) {
                            readDocxTextFromUri(context, uri)
                        }

                        inputText =
                            if (uploadedFileText.isBlank()) {
                                "DOCX içinden metin çıkarılamadı."
                            } else {
                                ""
                            }
                    }

                }

                else if (isImage) {
                    uploadedFileName = selectedFileName
                    uploadedFileText = ""
                    inputText = "Görsel okunuyor..."

                    scope.launch {
                        uploadedFileText = withContext(Dispatchers.Default) {
                            readImageTextFromUri(context, uri)
                        }

                        inputText =
                            if (uploadedFileText.isBlank()) {
                                "Görsel içinden metin çıkarılamadı."
                            } else {
                                ""
                            }
                    }
                }

                else {
                    uploadedFileName = selectedFileName
                    uploadedFileText = readTextFromUri(context, uri)
                    inputText = ""
                    //inputText = "File selected. Character count: ${uploadedFileText.length}. First part: ${uploadedFileText.take(80)}"

                }
            }

        }

    var isGenerating by remember {
        mutableStateOf(false)
    }

    val sessions = remember {
        mutableStateListOf<ChatSession>()
    }

    val currentMessages = remember {
        mutableStateListOf<ChatMessage>()
    }

    var currentSessionId by remember {
        mutableStateOf<Long?>(null)
    }

    var showHistoryPanel by remember {
        mutableStateOf(false)
    }

    var showSettingsPanel by remember {
        mutableStateOf(false)
    }

        suspend fun refreshSessions() {
            val savedSessions = withContext(Dispatchers.IO) { //Dispatchers.IO = Hangi thread’de çalışsın
                chatDao.getSessionPreviews()
            }

            sessions.clear()

            sessions.addAll(
                savedSessions.map { session ->
                    ChatSession(
                        id = session.id,
                        title = session.title,
                        messageCount = session.messageCount,
                        isPinned = session.isPinned
                    )
                }
            )
        }

    suspend fun loadMessagesForSession(
        sessionId: Long
    ) {
        val savedMessages = withContext(Dispatchers.IO) {
            chatDao.getMessagesForSession(sessionId)
        }

        currentMessages.clear()

        currentMessages.addAll(
            savedMessages.map { message ->
                ChatMessage(
                    text = message.text,
                    isFromUser = message.isFromUser
                )
            }
        )
    }

    suspend fun createSession(
        title: String
    ): Long {
        val now = System.currentTimeMillis()
        val newSessionId = now

        val newSession = ChatSessionEntity(
            id = newSessionId,
            title = title,
            createdAt = now,
            updatedAt = now
        )

        withContext(Dispatchers.IO) {
            chatDao.insertSession(newSession)
        }

        return newSessionId
    }

    fun createNewChat() {
        scope.launch {
            val newSessionId = createSession("New chat")

            currentSessionId = newSessionId
            currentMessages.clear()
            inputText = ""
            showHistoryPanel = false

            refreshSessions()
        }
    }

    fun deleteChat(sessionId: Long) {
        scope.launch {
            withContext(Dispatchers.IO) {
                chatDao.deleteMessagesBySessionId(sessionId)

                val sessionToDelete = ChatSessionEntity(
                    id = sessionId,//Room silme sırasında primary key’e, yani id değerine bakar.
                    title = "", //Bunlar yalnızca nesneyi oluşturabilmek için doldurulmuş geçici değerlerdir.
                    createdAt = 0,
                    updatedAt = 0
                )

                chatDao.deleteSession(sessionToDelete)
            }

            if (currentSessionId == sessionId) {
                currentSessionId = null
                currentMessages.clear()
            }

            refreshSessions()
        }
    }

    fun editChatTitle(sessionId: Long, newTitle: String) {
        scope.launch {
            val now = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                chatDao.updateSessionTitleManual(
                    sessionId = sessionId,
                    newTitle = newTitle,
                    updatedAt = now
                )
            }

            refreshSessions()
        }
    }

    fun togglePinChat(session: ChatSession) {
        scope.launch {
            withContext(Dispatchers.IO) {
                chatDao.updateSessionPinned(
                    sessionId = session.id,
                    isPinned = !session.isPinned //Eğer sohbet pinli değilse:pinli yapıyor.Eğer zaten pinliyse:pinini kaldırıyor.
                )
            }

            refreshSessions()
        }
    }


    suspend fun ensureCurrentSession(
        firstUserMessage: String
    ): Long {
        val existingSessionId = currentSessionId

        if (existingSessionId != null) {
            return existingSessionId
        }

        val title = firstUserMessage.take(28)
        val newSessionId = createSession(title)

        currentSessionId = newSessionId

        return newSessionId
    }

    LaunchedEffect(Unit) { //ekran ilk acıldıgında en yeni sohbeti ekranda gosterir
        refreshSessions()

        if (sessions.isNotEmpty()) {
            val latestSessionId = sessions.first().id
            currentSessionId = latestSessionId
            loadMessagesForSession(latestSessionId)
        }
    }

    Box( //Box    → bileşenleri aynı alanda, gerekirse üst üste koyar. Bu uygulamanda dıştaki Box muhtemelen sohbet ekranı ile history/settings panelini üst üste göstermek için kullanılıyor:
        modifier = Modifier.fillMaxSize()
    ) {//Bu asagıdaki kısım, Box içine yerleştirilecek arayüz öğelerini belirtir.
        Scaffold( //Scaffold  ekranın sabit bölümlerini daha düzenli yönetir.
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,

            topBar = {
                ChatHeader(
                    isGenerating = isGenerating,
                    onHistoryClick = {
                        showHistoryPanel = true
                    },
                    onSettingsClick = {
                        showSettingsPanel = true
                    },
                    onNewChatClick = {
                        createNewChat()
                    }
                )
            },

            bottomBar = {
                Column {
                    SelectedFileCard(
                        fileName = uploadedFileName,
                        onRemoveClick = {
                            uploadedFileText = ""
                            uploadedFileName = ""
                            inputText = ""
                        }
                    )

                    ChatInputBar(
                        text = inputText,
                        isGenerating = isGenerating,
                        onTextChange = { newText ->
                            inputText = newText
                        },
                        onFileClick = {
                            //filePickerLauncher.launch("text/plain") //sadece text dosyası seçtirmeye çalışır.
                            //filePickerLauncher.launch("*/*")
                            filePickerLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "application/json",
                                    "application/xml",
                                    "application/octet-stream",
                                    "application/pdf",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "image/*"
                                )
                            )

                        },
                        isListening = isListening,
                        onMicClick = {
                            val microphonePermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            )

                            if (microphonePermission == PackageManager.PERMISSION_GRANTED) {
                                if (isListening) {
                                    voskSpeechManager.stopListening()
                                    isListening = false//eger dinliyorsa yani if e girmisse yani islintening var ise ikinci basışta mikrofon kapanır. Yani lamba anahtarı gibi düşünebilirsin
                                } else {//Mikrofon izni varsa ama mic kapalıyken kullanıcı mikrofon butonuna basarsa ilk basısta dinlemeyi baslat islistening true olsun
                                    voskSpeechManager.startListening()
                                    isListening = true
                                }
                            } else {
                                microphonePermissionLauncher.launch( //izin ekranını acar
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        },
                        onSendClick = {
                            val userMessage = inputText.trim()

                            if (userMessage.isNotEmpty() && !isGenerating) {
                                scope.launch {
                                    isGenerating = true
                                    inputText =
                                        "" //Kullanıcı mesajı gönderdikten sonra yazı kutusunu temizler.

                                    val sessionId =
                                        ensureCurrentSession(userMessage)//“Bu mesaj hangi sohbete ait olacak, onu bul.”
                                    val now =
                                        System.currentTimeMillis()//Bu zamanı birazdan sohbetin en son ne zaman güncellendiğini kaydetmek için kullanıyorsun.

                                    val currentTitle =
                                        sessions //“Şu an aktif olan sohbetin başlığı nedir, bul.”
                                            .firstOrNull { it.id == sessionId }
                                            ?.title

                                    if (currentTitle == "New chat") {
                                        withContext(Dispatchers.IO) {
                                            chatDao.updateSessionTitleAndTime(
                                                sessionId = sessionId,
                                                title = userMessage.take(28),
                                                updatedAt = now
                                            )
                                        }
                                    } else {
                                        withContext(Dispatchers.IO) {
                                            chatDao.updateSessionTime(
                                                sessionId = sessionId,
                                                updatedAt = now
                                            )
                                        }
                                    }

                                    val userChatMessage = ChatMessage(
                                        text = userMessage,
                                        isFromUser = true
                                    )

                                    currentMessages.add(userChatMessage)

                                    withContext(Dispatchers.IO) {
                                        chatDao.insertMessage(
                                            ChatMessageEntity(
                                                sessionId = sessionId,
                                                text = userMessage,
                                                isFromUser = true,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                    }

                                    refreshSessions()

                                    //ekleme baslıyor dosya okuması icin gemmanın
                                    val finalPrompt =
                                        if (uploadedFileText.isNotBlank()) {
                                            """
                                    Below is the content of text information uploaded by the user.
                                    
                                    File contents:
                                    $uploadedFileText

                                    User question:
                                    $userMessage
                                
                                    
                                    The user has uploaded a file. Use the file only when the user's question is about the file.
                                    If the user's question is general and not related to the uploaded file, answer directly using your general knowledge. Do not say that the file does not contain information about the question.                                    Do not force every answer to be based on the file.
                                    Do not switch to Turkish unless the user writes in Turkish.
                                        """.trimIndent()
                                        } else {
                                            userMessage
                                        }

                                    val answer = try {
                                        withContext(Dispatchers.Default) {
                                            llmEngine.ask(finalPrompt)
                                        }
                                    } catch (e: Exception) {
                                        "Hata oluştu: ${e.message}"
                                    }

                                    val modelChatMessage = ChatMessage(
                                        text = answer,
                                        isFromUser = false
                                    )

                                    currentMessages.add(modelChatMessage)

                                    withContext(Dispatchers.IO) {
                                        chatDao.insertMessage(
                                            ChatMessageEntity(
                                                sessionId = sessionId,
                                                text = answer,
                                                isFromUser = false,
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )

                                        chatDao.updateSessionTime(
                                            sessionId = sessionId,
                                            updatedAt = System.currentTimeMillis()
                                        )
                                    }

                                    refreshSessions()

                                    isGenerating = false
                                }
                            }
                        }

                    )
                }
            }
        ) { innerPadding -> //Mesajlar üstteki başlığın altından başlamalı ve alttaki yazı kutusunun arkasına girmemeli.

            if (currentMessages.isEmpty()) {
                ChatContent(
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(currentMessages) { message ->                             //items, LazyColumn içindeki listedeki elemanları tek tek ekrana koymak için kullanılır.                       // isfromuser ise sag da degilse solda bubble gozukur
                        MessageBubble(
                            chatMessage = message
                        )
                    }

                    //donen yuvarlak
                    if (isGenerating) {                                          //loading gorunumu veriliyor
                        item {                                                           //LazyColumn içinde normal mesajlardan ayrı, tek bir satır eklemek için kullanılır. Buradaki ekstra satır Gemma’nın loading satırı.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,        //İçindekileri sola hizalar.
                                verticalAlignment = Alignment.CenterVertically      //Yuvarlak ve yazı aynı yatay çizgide ortalanır.
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )

                                Spacer(modifier = Modifier.width(8.dp))          //Yuvarlak ile yazı arasına 8 dp boşluk koyar.

                                //Text("Gemma is thinking...")
                            }
                        }
                    }

                }

            }
        }

        if (showHistoryPanel) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart
            ) {

                // Ekranın tamamını kaplar. Sağ tarafa basılırsa panel kapanır.
                Box(                                                    //mantıgı videoda 07.07.2026 19.33
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { //Bu Boxa kullanıcı dokunabilsin. Yani görünmez olsa bile tıklamayı algılar.
                            showHistoryPanel = false
                        }
                )

                HistoryPanel(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onCloseClick = {
                        showHistoryPanel = false
                    },
                    onNewChatClick = {
                        createNewChat()
                    },
                    onSessionClick = { selectedId ->
                        scope.launch {
                            currentSessionId = selectedId
                            loadMessagesForSession(selectedId)
                            showHistoryPanel = false
                        }
                    },
                    onDeleteClick = { idToDelete ->
                        deleteChat(idToDelete)
                    },
                    onEditTitleClick = { idToEdit, newTitle ->
                        editChatTitle(idToEdit, newTitle)
                    },
                     onPinClick = { session ->
                        togglePinChat(session)
                    }
                )
            }
        }

        if (showSettingsPanel) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                SettingsPanel(
                    isDarkTheme = isDarkTheme,
                    selectedColorTheme = selectedColorTheme,
                    onCloseClick = {
                        showSettingsPanel = false
                    },
                    onThemeModeChange = onThemeModeChange,
                    onColorThemeChange = onColorThemeChange
                )
            }
        }

    }
}

@Composable
fun ChatHeader(
    isGenerating: Boolean,
    onHistoryClick: () -> Unit,//Buradaki Unit, bu fonksiyonun bir değer geri döndürmediği anlamına gelir.
    onSettingsClick: () -> Unit,
    onNewChatClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ELOCHAT",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isGenerating) {
                        "Gemma is thinking..."
                    } else {
                        "On-device AI assistant"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onHistoryClick // onHistoryClick fonk cagrılır => showHistoryPanel = true olur
            ) {
                Text("Chats")
            }

            Button(
                onClick = onNewChatClick
            ) {
                Text("+")
            }

            Button(
                onClick = onSettingsClick
            ) {
                Text("⚙️")
            }

        }
    }
}

@Composable
fun ChatContent( //ortadaki alan
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "👋",
                fontSize = 42.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Ask me anything",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Gemma runs directly on this device.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    isGenerating: Boolean,
    isListening: Boolean,
    onTextChange: (String) -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    onFileClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                enabled = !isGenerating,
                placeholder = {
                    Text("Ask something...")
                },
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4
            )

            IconButton(
                onClick = onFileClick
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file"
                )
            }

            IconButton(
                onClick = onMicClick,
                enabled = !isGenerating //Model cevap üretirken mikrofon tuşu pasif olsun
            ) {
                Icon( //Buradan sonrası, IconButtonun içinde ne görüneceğini söyler.
                    imageVector = Icons.Default.Mic, //Mikrofon simgesinin oluştuğu yer tam olarak burasıdır.
                    contentDescription = "Speak",
                    tint = if (isListening) Color.Red else Color.White

                )
            }

            Button(
                onClick = onSendClick,
                enabled = text.isNotBlank() && !isGenerating,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 14.dp
                )
            ) {
                Text(
                    text = if (isGenerating) {
                        "..."
                    } else {
                        "Send"
                    }
                )
            }
        }
    }
}


@Composable
fun SelectedFileCard(
    fileName: String,
    onRemoveClick: () -> Unit
) {
    if (fileName.isNotBlank()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$fileName seçildi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onRemoveClick
                ) {
                    Text("Kaldır")
                }
            }
        }
    }
}


@Composable
fun HistoryPanel(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onCloseClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onEditTitleClick: (Long, String) -> Unit,
    onPinClick: (ChatSession) -> Unit
) {
    // --- 1. POP-UP KUTUSUNU KONTROL EDEN YENİ DEĞİŞKENLER ---
    var showDialog by remember { mutableStateOf(false) } //Bu değişken, düzenleme penceresinin açık mı kapalı mı olduğunu tutar.
    var textToEdit by remember { mutableStateOf("") }
    var selectedSessionIdForEdit by remember { mutableStateOf<Long?>(null) }

    // --- 2. BAŞLIK DÜZENLEME POP-UP KUTUSU (Diyalog) ---
    if (showDialog) {
        androidx.compose.material3.AlertDialog( //AlertDialog, bu asagıdaki iptal, kaydet butonlarını Material Design kurallarına göre kendisi yerleştirir. Bu yüzden ayrıca Row yazmana gerek kalmaz.
            onDismissRequest = { showDialog = false }, //Bu, pop-up kullanıcı tarafından dışarı dokunarak veya geri tuşuyla kapatılmak istenirse ne olacağını belirler.
            title = { Text("Sohbet Başlığını Düzenle") },
            text = {
                OutlinedTextField(
                    value = textToEdit,
                    onValueChange = { textToEdit = it },//kullanıcı kutudaki yazıyı değiştirince, yeni yazı bu bloğa gelir. Kotlin o yeni yazıya otomatik olarak it adını verir.
                    label = { Text("Yeni Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textToEdit.isNotBlank() && selectedSessionIdForEdit != null) {
                            onEditTitleClick(selectedSessionIdForEdit!!, textToEdit.trim())
                            showDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // --- MEVCUT TASARIMIN AYNEN DEVAM EDİYOR ---
    Surface(//Surface, Jetpack Compose’da bir arayüz bölümüne arka plan, şekil, kenarlık, gölge ve renk vermek için kullanılan kapsayıcıdır.
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp),
        tonalElevation = 10.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier //Columnun görünüşünü ve boyutunu ayarlamak için Modifier kullanılır.
                .fillMaxSize()
                .padding(16.dp)//Columnun içeriği ile kenarları arasında 16.dp boşluk bırakır.
        ) {
            // Başlık ve Kapatma Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Chats",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onCloseClick) {
                    Text("X")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Yeni Sohbet Butonu
            Button(
                onClick = onNewChatClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("New chat")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sessions.isEmpty()) {
                Text(
                    text = "No chats yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = session.id == currentSessionId

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSessionClick(session.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = session.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${session.messageCount} messages",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                //Pin butonu
                                IconButton(
                                    onClick = {
                                        onPinClick(session)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin chat",
                                        tint = if (session.isPinned) {
                                            Color.Red
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(20.dp)

                                    )
                                }

                                Button(
                                    onClick = {
                                        textToEdit = session.title
                                        selectedSessionIdForEdit = session.id
                                        showDialog = true //showDialog icinde degistiriyorsun baslıgı
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("✏️", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Button(
                                    onClick = { onDeleteClick(session.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("🗑️", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SettingsPanel(
    isDarkTheme: Boolean,
    selectedColorTheme: ColorTheme,
    onCloseClick: () -> Unit,
    onThemeModeChange: (Boolean) -> Unit,
    onColorThemeChange: (ColorTheme) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(310.dp),
        tonalElevation = 10.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onCloseClick
                ) {
                    Text("X")
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Theme mode",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Button(
                onClick = {
                    onThemeModeChange(false)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (!isDarkTheme) {
                        "✓ Light"
                    } else {
                        "Light"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    onThemeModeChange(true)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isDarkTheme) {
                        "✓ Dark"
                    } else {
                        "Dark"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Color theme",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Button(
                onClick = {
                    onColorThemeChange(ColorTheme.Neutral)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedColorTheme == ColorTheme.Neutral) {
                        "✓ Neutral"
                    } else {
                        "Neutral"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    onColorThemeChange(ColorTheme.Purple)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedColorTheme == ColorTheme.Purple) {
                        "✓ Purple"
                    } else {
                        "Purple"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    onColorThemeChange(ColorTheme.Blue)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedColorTheme == ColorTheme.Blue) {
                        "✓ Blue"
                    } else {
                        "Blue"
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    onColorThemeChange(ColorTheme.Pink)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedColorTheme == ColorTheme.Pink) {
                        "✓ Pink"
                    } else {
                        "Pink"
                    }
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    chatMessage: ChatMessage,
    //showLoading: Boolean = false
) {
    val bubbleColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (chatMessage.isFromUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (chatMessage.isFromUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (chatMessage.isFromUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically
    ) {


        Surface(
            color = bubbleColor,
            shape = bubbleShape,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(
                max = 280.dp
            )
        ) {
            Text(
                text = chatMessage.text,
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            )
        }
    }
}


//readTextFromUri fonk un genel akısı:
//Fonksiyona Context ve Uri gelir
//↓
//ContentResolver(araç) ile InputStream(bir nesne) açılır
//↓
//Stream, BufferedReader'a(bir nesne) dönüştürülür (InputStream, dosyayı ham byte olarak okur. BufferedReader ise bu byte verilerini karakterlere çevirip metin olarak okumayı kolaylaştırır.). Bu yüzden: reader.readText() ile dosyanın tamamını kolayca String olarak alabiliyorsun.
//↓
//Dosyanın tamamı String olarak okunur
//↓
//Stream ve reader otomatik kapatılır
//↓
//Metin döndürülür

//Uri, kullanıcı dosya seçtiğinde Android’in dosya seçici sistemi tarafından geri döndürülür.
//ContentResolver, Android’in uygulamaya içerik sağlayıcılar üzerinden dosya veya veri açma imkânı veren sistem aracıdır.

//Kısaca:
//Android dosya seçildikten sonra bir Uri döndürür. ContentResolver, bu Uri üzerinden dosyayı açar. InputStream ham veriyi verir; BufferedReader ise bu veriyi metin olarak kolay ve verimli biçimde okumamızı sağlar.
fun readTextFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream -> //Android, bana bu Uri’nin gösterdiği dosyayı okumam için bir okuma akışı aç. Yani uygulama dosyanın gerçek yolunu bilmek zorunda kalmıyor.Andrıid e diyor ki Bende şu Uri var. Bu dosyayı kullanıcı seçti. Bana bunun içeriğini okumam için stream aç.  Eğer açılan stream null değilse use bloğunu çalıştır.
            inputStream.bufferedReader().use { reader -> //InputStream → byte okur, BufferedReader → metin okur
                reader.readText()
            }
        } ?: "" //Bu bölüm, openInputStream(uri) sonucu null olursa devreye girer.
    } catch (e: Exception) {
        "Dosya okunurken hata oluştu: ${e.message}"
    }
}


fun isPdfFile(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    return mimeType == "application/pdf"
}

fun isDocxFile(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)

    return mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" //MIME type, bir içeriğin veya dosyanın hangi türde olduğunu belirten standart metindir. Genel yapısı şöyledir:anaTür/altTür. Örnekler:image/png, application/pdf vs...
}

fun isImageFile(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)

    return mimeType != null && mimeType.startsWith("image/")
}


fun copyUriToCacheFile(
    context: Context,
    uri: Uri,
    fileName: String
): File {
    val file = File(context.cacheDir, fileName) //context.cacheDir, uygulamanın geçici dosyaları için ayrılmış klasördür. Android gerektiğinde bu dosyaları silebilir; kalıcı saklama yeri değildir.

    context.contentResolver.openInputStream(uri)?.use { inputStream -> //inputStream, kaynak dosyadan veri okumak için kullanılır. use, işlem bitince stream’i otomatik kapatır.
        file.outputStream().use { outputStream -> //outputStream, veriyi hedef dosyaya (cache dosyasına) yazmak için kullanılır.
            inputStream.copyTo(outputStream) //Kaynak dosyadaki bütün veriyi hedef dosyaya kopyalar.
        }
    }

    return file
}



fun renderPdfPageToBitmap(
    renderer: PdfRenderer,
    pageIndex: Int
): Bitmap {
    renderer.openPage(pageIndex).use { page ->
        val scale = 2

        val bitmap = Bitmap.createBitmap(
            page.width * scale,
            page.height * scale,
            Bitmap.Config.ARGB_8888
        )

        bitmap.eraseColor(AndroidColor.WHITE)

        page.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        return bitmap
    }
}




//recognizeTextFromBitmap fonku, bir Bitmap resmin içindeki yazıları OCR ile okuyup String olarak döndürüyor.
//OCR bir işlemin adıdır, ML Kit ise bu işlemi yapabilen kütüphanedir.
//Bitmap gelir
//↓
//ML Kit metin tanıyıcı oluşturulur
//↓
//Bitmap, InputImage'a çevrilir
//↓
//OCR işlemi başlatılır
//↓
//await ile sonuç beklenir
//↓
//Tanınan metin alınır
//↓
//Baştaki ve sondaki boşluklar temizlenir
//↓
//String olarak döndürülür
suspend fun recognizeTextFromBitmap(
    bitmap: Bitmap
): String {
    return try {
        val recognizer =
            TextRecognition.getClient(//ML Kit’in OCR aracını hazırlıyor.
                TextRecognizerOptions.DEFAULT_OPTIONS
            ) //Burada ML Kit’in metin tanıma motoru alınır.

        val image = InputImage.fromBitmap(bitmap, 0) //ML Kit doğrudan sıradan Bitmap yerine InputImage türünde görüntü bekler. Bu yüzden bitmap, ML Kit’in anlayacağı InputImage nesnesine dönüştürülür.

        //Bu bölüm OCR işlemini yapar. Bu OCR aracı resme bakıp yazıları çıkarır.Bu resme bak ve içindeki yazıları bul. Ama işlem hemen bitmeyebilir. Bu yüzden: .await() kullnaılır.
        val result = recognizer
            .process(image)
            .await()

        result.text.trim()
    } catch (e: Exception) {
        ""
    }
}



//Asagıdaki fonk,iki farklı PDF okuma yöntemini birleştiriyor:
//İlk, sayfada gerçek/seçilebilir metin varsa → PDFBox ile oku
//Sonra, sayfada gerçek metin yoksa → sayfayı görüntü yap, OCR ile oku

//PDF seçildi
//↓
//PDF geçici dosyaya kopyalandı
//↓
//PDF sayfa sayfa gezildi
//↓
//Her sayfa için:
//    Önce PDFBox ile yazı var mı bakıldı
//    Varsa o yazı alındı
//    Yoksa sayfa resim/Bitmap yapıldı
//    OCR ile görüntüdeki yazı okundu
//↓
//Tüm sayfaların yazısı birleştirildi
//↓
//String olarak döndürüldü


//Burada iki sistem aynı anda var:
//PDDocument / PDFBox
//→ seçilebilir metin çıkarmak için

//PdfRenderer
//→ sayfayı görüntüye çevirip/bitmap'e cevirip OCR’a vermek için


suspend fun readHybridPdfTextFromUri(
    context: Context,
    uri: Uri,
    maxPages: Int = 3
): String {
    return try {
        val pdfFile = copyUriToCacheFile( //Buradaki pdfFile, artık uygulamanın geçici klasöründeki PDF kopyasıdır.
            context = context,
            uri = uri,
            fileName = "hybrid_temp.pdf"                                                                    //Bu satır, seçilen PDF’nin cache klasörüne hangi adla kopyalanacağını belirler.
        )

        //StringBuilder, metinleri parça parça biriktirmek için kullanılır.
        val resultBuilder = StringBuilder()

        val document: PDDocument? =  // PDDocument, PDFBox’ın PDF dosyasını temsil eden nesnesidir.
            try {
                PDDocument.load(pdfFile) //Burada PDFBox’a şu deniyor:  Cache klasöründeki pdfFile dosyasını PDF olarak aç. Yani document = açılmış PDF nesnesi olur
            } catch (e: Exception) {
                null
            }

        val parcelFileDescriptor = //PDF dosyasını Android’in PdfRenderer aracına açıyor
            ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )


        //PdfRendererın görevi PDF içindeki sayfaları görüntü olarak açabilmektir. Yani bir PDF sayfasını Bitmap hâline çevirmemizi sağlar.
        PdfRenderer(parcelFileDescriptor).use { renderer -> //Burada PdfRenderer oluşturuluyor.
            val pageCount =
                if (document != null) { //Eğer PDF, PDFBox ile de başarıyla açılmışsa 3 değer karşılaştırılıyor:
                    minOf( //minOf, bunların en küçüğünü seçer.
                        document.numberOfPages,
                        renderer.pageCount,
                        maxPages
                    )
                } else { //PDFBox metin çıkarmada başarısız olsa bile, PDF sayfalarını resme çevirip OCR ile okumayı dene.
                    minOf(
                        renderer.pageCount,
                        maxPages
                    )
                }

            for (pageIndex in 0 until pageCount) {
                val pageNumber = pageIndex + 1

                val selectableText =
                    if (document != null) { //PDF, PDFBox ile başarıyla açılmışsa metin çıkarma denenir.
                        try {
                            val stripper = PDFTextStripper().apply { ////PDFTextStripper, PDFBox’ın metin çıkarma aracıdır.
                                startPage = pageNumber
                                endPage = pageNumber
                            } //Burada hem başlangıç hem bitiş sayfası aynı verildiği için yalnızca tek sayfa okunur:

                            stripper.getText(document).trim()
                        } catch (e: Exception) {
                            ""
                        }
                    } else {
                        ""
                    }

                val pageText =
                    if (selectableText.isNotBlank()) {
                        selectableText
                    } else { //{//eger secilebilir metin yoksa bu else kısmına geçeriz ve OCR yaparız.
                        val bitmap = renderPdfPageToBitmap(
                            renderer = renderer,
                            pageIndex = pageIndex
                        )

                        val ocrText = recognizeTextFromBitmap(bitmap) ////Bu satır Bitmap üzerindeki yazıları okumaya çalışır.

                        bitmap.recycle() ////OCR işi bittikten sonra o sayfa görüntüsüne artık ihtiyacımız yok. Bu görüntüyle işim bitti, belleği temizle.

                        ocrText //Eğer seçilebilir metin yoksa o sayfanın sonucu OCR’dan gelen metin olur.
                    }


                //Bu kısım her sayfanın metnini düzenli şekilde biriktirir.
                resultBuilder.append("Sayfa ")
                resultBuilder.append(pageNumber)
                resultBuilder.append(":\n")
                resultBuilder.append(pageText)
                resultBuilder.append("\n\n")
            }
        }

        parcelFileDescriptor.close()
        document?.close()

        //Tüm sayfalardan biriken yazıyı normal String haline getirir ve gereksiz boşlukları temizler.
        resultBuilder.toString().trim()

    } catch (e: Exception) {
        ""
    }
}


fun readDocxTextFromUri(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            XWPFDocument(inputStream).use { document ->
                val resultBuilder = StringBuilder()

                document.paragraphs.forEach { paragraph ->
                    val text = paragraph.text

                    if (text.isNotBlank()) {
                        resultBuilder.append(text)
                        resultBuilder.append("\n")
                    }
                }

                resultBuilder.toString().trim()
            }
        } ?: ""
    } catch (e: Exception) {
        ""
    }
}

//hangi dosyayı sectiysen ui de gozukmesi icin
fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = "Dosya"

    try {
        val cursor = context.contentResolver.query( //Cursor → Dosya bilgilerini tutan tablo
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex( //Bu bilgiler içinde dosya adının bulunduğu yer neresi?
                OpenableColumns.DISPLAY_NAME //DISPLAY_NAME = dosya adı
            )

            if (nameIndex != -1 && it.moveToFirst()) { //displayname bulundu mu (getColumnIndex() sütunu bulamazsa -1 döndürür.) ve İlk satıra geçebildik mi?
                fileName = it.getString(nameIndex) //İşte gerçek dosya adını alıyoruz.
            }
        }
    } catch (e: Exception) {
        fileName = "Dosya" //Eğer gerçek ismi bulamazsak sonunda Dosya donecek
    }

    return fileName
}


suspend fun readImageTextFromUri(
    context: Context,
    uri: Uri
): String {
    return try {
        val recognizer =
            TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
            )

        val image = InputImage.fromFilePath(context, uri)

        val result = recognizer
            .process(image)
            .await()

        result.text.trim()
    } catch (e: Exception) {
        ""
    }
}