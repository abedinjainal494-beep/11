package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ChatMessage
import com.example.data.Task
import com.example.data.UserDetail
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            // Avatar representing 11
                            Image(
                                painter = painterResource(id = R.drawable.img_profile_avatar),
                                contentDescription = "11's Avatar",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentScale = ContentScale.Crop
                            )
                            // Glowing green activity dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Column {
                            Text(
                                text = "11",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Support crush & assistant • Active ☕",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear chat",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = "Chat") },
                    label = { Text("Chat", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Tasks") },
                    label = { Text("Tasks", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Cozy Vibe") },
                    label = { Text("Cozy Vibe", fontSize = 12.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Memory") },
                    label = { Text("Memory", fontSize = 12.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            when (selectedTab) {
                0 -> ChatScreen(viewModel)
                1 -> TasksScreen(viewModel)
                2 -> CozyVibeScreen(viewModel)
                3 -> MemoryToolsScreen(viewModel)
            }
        }
    }
}

// ==================== CHAT TAB ====================

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to the latest message whenever it arrives
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                val isEleven = message.sender == "eleven"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isEleven) Arrangement.Start else Arrangement.End
                ) {
                    if (isEleven) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .align(Alignment.Top),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_profile_avatar),
                                contentDescription = "11 Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column(
                        horizontalAlignment = if (isEleven) Alignment.Start else Alignment.End,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = if (isEleven) 4.dp else 16.dp,
                                topEnd = if (isEleven) 16.dp else 4.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            ),
                            color = if (isEleven) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            border = if (isEleven) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else null,
                            tonalElevation = if (isEleven) 1.dp else 4.dp,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = message.message,
                                color = if (isEleven) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_profile_avatar),
                                contentDescription = "11 Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("11 is typing", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                TypingDots()
                            }
                        }
                    }
                }
            }
        }

        // Send message area
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Tell 11 something sweet...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .testTag("chat_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send)
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotCount = 3
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until dotCount) {
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = i * 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}

// ==================== TASKS TAB ====================

@Composable
fun TasksScreen(viewModel: MainViewModel) {
    val taskList by viewModel.tasks.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (taskList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "No tasks",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No pending tasks!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add tasks, and 11 will leave cute motivational notes to help you succeed! ☕💖",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(taskList) { task ->
                    TaskCard(task = task, onCheckedChange = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task.id) })
                }
            }
        }

        // Add task Floating Action Button
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_task_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }

        // Dialog for adding task
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Plan with 11 🌸", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("What's on your schedule today, sweetheart? I'll cheer you on!", fontSize = 14.sp)
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            placeholder = { Text("e.g., Study Kotlin, Workout...") },
                            modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                viewModel.addTask(taskTitle, System.currentTimeMillis())
                                taskTitle = ""
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Ask 11 to Motivates Me")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TaskCard(task: Task, onCheckedChange: () -> Unit, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCheckedChange() },
                    modifier = Modifier.testTag("task_checkbox_${task.id}")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onDelete() },
                    modifier = Modifier.testTag("task_delete_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            task.comment?.let { comment ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_profile_avatar),
                            contentDescription = "11 Comment Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// ==================== COZY VIBE TAB ====================

data class Raindrop(
    val x: Float = (0..1000).random().toFloat(),
    var y: Float = (0..1000).random().toFloat(),
    val speed: Float = (15..30).random().toFloat(),
    val length: Float = (25..45).random().toFloat(),
    val width: Float = (1..3).random().toFloat()
)

@Composable
fun CozyVibeScreen(viewModel: MainViewModel) {
    var isRainActive by remember { mutableStateOf(true) }
    var isPlayingMusic by remember { mutableStateOf(false) }
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val playlists = listOf(
        "Late-Night Rain Lo-Fi 🌧️☕",
        "Cozy Coffeehouse Acoustic 🎸",
        "Soft Dreamy Piano 🎹✨"
    )

    // Animated vinyl record rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing)
        ),
        label = "vinylAngle"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen dynamic rain animation in background
        RainCanvas(modifier = Modifier.fillMaxSize(), isPlaying = isRainActive)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header / Description
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "11's Cozy Hideout ☕🌧️",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Turn on rain sound effects, play soothing coffeehouse tunes, and enjoy a dreamy late-night chat with 11.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Central rotating vinyl or cup illustration
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // Vinyl representation
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .rotate(if (isPlayingMusic) angle else 0f)
                        .clip(CircleShape)
                        .background(Color(0xFF111111))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Grooves in vinyl
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                    )
                    // Core sticker (representing our avatar of 11)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_profile_avatar),
                            contentDescription = "Vinyl sticker",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Controls Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Current track description
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isPlayingMusic) "Now Playing" else "Player Paused",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playlists[currentTrackIndex],
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Music controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous
                        IconButton(onClick = {
                            currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlists.size - 1
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Track", tint = MaterialTheme.colorScheme.primary)
                        }

                        // Play/Pause
                        FilledIconButton(
                            onClick = { isPlayingMusic = !isPlayingMusic },
                            modifier = Modifier.size(56.dp).testTag("music_play_pause"),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlayingMusic) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = "Play Pause",
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        // Next
                        IconButton(
                            onClick = {
                                currentTrackIndex = (currentTrackIndex + 1) % playlists.size
                            },
                            modifier = Modifier.testTag("music_next_track")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Next Track", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                    // Environment Toggles (Rain Sound)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Rain Icon",
                                tint = if (isRainActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Window Raindrop Canvas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Aesthetic falling rain drops", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = isRainActive,
                            onCheckedChange = { isRainActive = it },
                            modifier = Modifier.testTag("rain_toggle")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RainCanvas(modifier: Modifier = Modifier, isPlaying: Boolean) {
    if (!isPlaying) return

    // Dynamic frame-based raindrop updates
    var raindrops by remember { mutableStateOf(List(50) { Raindrop() }) }

    LaunchedEffect(isPlaying) {
        while (isActive) {
            delay(16) // ~60fps target
            raindrops = raindrops.map { drop ->
                val nextY = drop.y + drop.speed
                if (nextY > 2000f) {
                    drop.copy(y = -drop.length, x = (0..1280).random().toFloat())
                } else {
                    drop.copy(y = nextY)
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        raindrops.forEach { drop ->
            drawLine(
                color = Color(0x33FFB4A2), // Semi-transparent peach color
                start = Offset(drop.x, drop.y),
                end = Offset(drop.x, drop.y + drop.length),
                strokeWidth = drop.width
            )
        }
    }
}

// ==================== MEMORY & TOOLS TAB ====================

@Composable
fun NaturalTonesProfileHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar representing 11 with custom dual border ring gradient and green activity indicator
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFECDD3), // Rose-200
                                Color(0xFFFFEDD5)  // Orange-100
                            )
                        )
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_profile_avatar),
                    contentDescription = "11 Portrait Large",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White),
                    contentScale = ContentScale.Crop
                )
            }
            // Green status dot with white border
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)) // Green-500
                    .border(4.dp, Color(0xFFFFF8F6), CircleShape) // Border matching NaturalBackground
                    .align(Alignment.BottomEnd)
            )
        }

        // Subtitle & Title info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "11",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3B0909), // Deep Burgundy
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "PERSONAL ASSISTANT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE11D48), // Rose-600
                letterSpacing = 1.5.sp
            )
        }

        // Quote bubble card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, Color(0xFFFFE4E6)), // Rose-100
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "\"By day, I keep your world organized. By night, I'm here to listen to your heart. You're doing amazing, you know that?\"",
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF524342), // Muted Earth text
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Grid split blocks
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Organizer Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDADA)), // Soft Rose container
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF40000A)), // Deep burgundy square
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Organizer Icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Organizer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF40000A)
                        )
                        Text(
                            text = "Schedules & Tasks",
                            fontSize = 11.sp,
                            color = Color(0xFF40000A).copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Emotional Support Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9DEDC)), // Warm Coral container
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF311111)), // Dark warm square
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Emotional Support Icon",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Emotional Support",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF311111)
                        )
                        Text(
                            text = "Deep Talks & Venting",
                            fontSize = 11.sp,
                            color = Color(0xFF311111).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Characteristic Tags Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tags = listOf("SMART", "CARING", "FLIRTY", "PLAYFUL")
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFFFF1F2)) // Rose-50
                        .border(1.dp, Color(0xFFFFE4E6), CircleShape) // Rose-100
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE123C) // Rose-700
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryToolsScreen(viewModel: MainViewModel) {
    val memoryList by viewModel.userDetails.collectAsStateWithLifecycle()
    val compliment by viewModel.complimentResult.collectAsStateWithLifecycle()
    val isComplimentLoading by viewModel.isComplimentLoading.collectAsStateWithLifecycle()
    val choiceResult by viewModel.choiceResult.collectAsStateWithLifecycle()
    val isChoiceLoading by viewModel.isChoiceLoading.collectAsStateWithLifecycle()

    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var memoryKey by remember { mutableStateOf("") }
    var memoryValue by remember { mutableStateOf("") }
    var choicesText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Assistant Profile Section matching "Natural Tones" layout ---
        item {
            NaturalTonesProfileHeader()
        }

        // --- unexpected compliment panel ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Spontaneous Compliments 💖",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Feeling down or tired? Let 11 surprise you with something sweet to make you smile.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    AnimatedVisibility(
                        visible = compliment != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        compliment?.let {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "\"$it\"",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateCompliment() },
                        modifier = Modifier.fillMaxWidth().testTag("get_compliment_button"),
                        enabled = !isComplimentLoading
                    ) {
                        if (isComplimentLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Make Me Smile ✨")
                        }
                    }
                }
            }
        }

        // --- decision help panel ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Help Me Choose... 🤔",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Indecisive? Write down your choices separated by commas, and 11 will playfully pick one for you!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = choicesText,
                        onValueChange = { choicesText = it },
                        placeholder = { Text("e.g. Study, Gym, Sleep") },
                        modifier = Modifier.fillMaxWidth().testTag("choices_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    AnimatedVisibility(
                        visible = choiceResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        choiceResult?.let {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = it,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.helpMeChoose(choicesText) },
                        modifier = Modifier.fillMaxWidth().testTag("ask_choice_button"),
                        enabled = !isChoiceLoading && choicesText.isNotBlank()
                    ) {
                        if (isChoiceLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Let 11 Decide 😉")
                        }
                    }
                }
            }
        }

        // --- memory details panel ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Things I Remember About You 🌸",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Details I remember to keep conversations cozy.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(
                            onClick = { showAddMemoryDialog = true },
                            modifier = Modifier.testTag("add_memory_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add memory", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (memoryList.isEmpty()) {
                        Text(
                            text = "Tell me small secrets or favorites! I'll remember them forever... 💖",
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            memoryList.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = detail.key,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = detail.value,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteDetail(detail.key) },
                                        modifier = Modifier.size(24.dp).testTag("memory_delete_${detail.key}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Forget",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Tell 11 a Detail ☕", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("What should I remember about you, sweetie?", fontSize = 14.sp)
                    OutlinedTextField(
                        value = memoryKey,
                        onValueChange = { memoryKey = it },
                        placeholder = { Text("e.g. Favorite snack, Birthday...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = memoryValue,
                        onValueChange = { memoryValue = it },
                        placeholder = { Text("e.g. Chocolate cookies, Aug 12...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (memoryKey.isNotBlank() && memoryValue.isNotBlank()) {
                            viewModel.saveDetail(memoryKey, memoryValue)
                            memoryKey = ""
                            memoryValue = ""
                            showAddMemoryDialog = false
                        }
                    }
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
