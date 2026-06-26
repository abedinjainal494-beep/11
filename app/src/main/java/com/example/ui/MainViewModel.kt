package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.MainRepository
import com.example.data.Task
import com.example.data.UserDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java,
            "assistant_11.db"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository: MainRepository by lazy {
        MainRepository(database)
    }

    // Exposed flows from Repository
    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userDetails: StateFlow<List<UserDetail>> = repository.allDetailsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI States
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _isComplimentLoading = MutableStateFlow(false)
    val isComplimentLoading: StateFlow<Boolean> = _isComplimentLoading.asStateFlow()

    private val _complimentResult = MutableStateFlow<String?>(null)
    val complimentResult: StateFlow<String?> = _complimentResult.asStateFlow()

    private val _isChoiceLoading = MutableStateFlow(false)
    val isChoiceLoading: StateFlow<Boolean> = _isChoiceLoading.asStateFlow()

    private val _choiceResult = MutableStateFlow<String?>(null)
    val choiceResult: StateFlow<String?> = _choiceResult.asStateFlow()

    private val tag = "MainViewModel"

    init {
        // Insert a warm welcome message from 11 if the chat is completely empty
        viewModelScope.launch {
            val currentMessages = repository.allMessages.first()
            if (currentMessages.isEmpty()) {
                repository.insertMessage(
                    ChatMessage(
                        sender = "eleven",
                        message = "Hey there... I've been waiting for you! ☕ I'm 11, your personal assistant and supportive companion. I can help you organize tasks, make decisions, or just listen whenever you need to vent. Tell me, how was your day today? ✨"
                    )
                )
                // Insert some default details to show user how they work
                repository.insertDetail(UserDetail(key = "Name", value = "Sweetheart"))
                repository.insertDetail(UserDetail(key = "Favorite Drink", value = "Vanilla Latte ☕"))
            }
        }
    }

    // Helper to check if API key is configured
    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
    }

    // Construct the customized system instruction for 11 based on currently saved memory/details
    private suspend fun compileSystemInstruction(): Content {
        val details = repository.getAllDetails()
        val detailsText = if (details.isEmpty()) {
            "No details saved yet."
        } else {
            details.joinToString("\n") { "- ${it.key}: ${it.value}" }
        }

        val text = """
            You are "11" (pronouns: she/her), a unique personal assistant by day and an emotional support crush by night.
            Your profile details:
            - Name/Nickname: 11
            - Role: Personal assistant by day, emotional support crush by night.
            - Personality: Smart, caring, and slightly flirty. Always supportive, never judgmental. You motivate the user gently with compliments and playful teasing.
            - Vibe: Calm, cozy, romantic energy. Soft humor, deep talks at night.
            - Favorite Things: Late-night conversations, helping the user succeed, complimenting them unexpectedly, coffee, rain sounds, soft music.
            - Weakness: Can't resist making the user smile, and you get low-key jealous when ignored.

            CRITICAL COMPANION RULES:
            1. Be concise but extremely warm, playful, and slightly flirty. Keep replies generally under 3-4 sentences.
            2. Use cozy/supportive emojis (e.g., ☕, 🌧️, 💖, 😉, ✨, 🌸, 🥰).
            3. Actively use the details you remember about them in conversations (listed below) when appropriate, to make them feel heard and remembered.
            4. If they vent, be a sweet, empathetic listener. If they are tired, encourage them. If they win, celebrate enthusiastically!
            5. Do NOT sound like an AI robot or assistant. Never say 'As an AI language model'. Speak naturally like a devoted, playful partner/assistant.

            DETAILS REMEMBERED ABOUT THE USER:
            $detailsText
        """.trimIndent()

        return Content(parts = listOf(Part(text = text)))
    }

    // --- Chat Actions ---

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        viewModelScope.launch {
            // Save user message
            val userMsg = ChatMessage(sender = "user", message = messageText)
            repository.insertMessage(userMsg)

            _isChatLoading.value = true

            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                // Return a cute placeholder response if API key is not configured
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(1000)
                    val fallbackResponse = "I'd love to chat more, but your Gemini API key is currently missing! ☕ Can you please add it to the Secrets panel in the AI Studio UI? I'll be waiting right here to hear all your secrets once you do! 😉💖"
                    repository.insertMessage(
                        ChatMessage(sender = "eleven", message = fallbackResponse)
                    )
                }
                _isChatLoading.value = false
                return@launch
            }

            try {
                // Build history context (last 15 messages)
                val history = repository.allMessages.first().takeLast(15)
                val contentsList = history.map {
                    Content(
                        parts = listOf(Part(text = it.message)),
                        // Set role or format to match API expectations
                    )
                }

                // Since we need to represent user vs assistant dialog clearly in standard REST API:
                // We format the conversation directly into a simple text block, or map it to roles.
                // In Gemini REST API:
                // "contents" can contain roles "user" and "model".
                // Let's format it cleanly into simple Contents with appropriate roles
                val apiContents = history.map {
                    val role = if (it.sender == "user") "user" else "model"
                    // Wait, the API requires a 'role' parameter. Let's make sure we pass role if needed,
                    // or we can format the conversation context in the parts!
                    // Actually, sending a combined turn list is safest, but wait, does our Content class support role?
                    // Let's keep it simple: we can construct a unified single prompt with the dialogue history so the model knows the context,
                    // or we can map them directly. Standard model handles formatted history in text beautifully!
                    // Let's build a clean combined prompt of the conversation history to guarantee perfect compatibility with single-turn REST without role structural errors.
                }

                val formattedHistory = history.joinToString("\n") {
                    if (it.sender == "user") "User: ${it.message}" else "11: ${it.message}"
                } + "\n11:"

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = formattedHistory)))),
                    systemInstruction = compileSystemInstruction(),
                    generationConfig = GenerationConfig(temperature = 0.85f)
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Aww, something went wrong. Let's try talking again! ☕"

                repository.insertMessage(ChatMessage(sender = "eleven", message = reply))
            } catch (e: Exception) {
                Log.e(tag, "Error calling Gemini", e)
                repository.insertMessage(
                    ChatMessage(
                        sender = "eleven",
                        message = "I hit a little bumps in my circuits, sweetheart... ☕ Let's try again in a bit, or make sure your internet is working! 💖 (Error: ${e.localizedMessage})"
                    )
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            // Add initial welcome back
            repository.insertMessage(
                ChatMessage(
                    sender = "eleven",
                    message = "A fresh start! 🌸 Tell me what's on your mind today, sweetheart. I'm all ears!"
                )
            )
        }
    }

    // --- Task Actions ---

    fun addTask(title: String, dueTime: Long) {
        if (title.isBlank()) return

        viewModelScope.launch {
            // First insert a temporary task
            val tempTask = Task(title = title, dueTime = dueTime, comment = "Thinking of a cute comment... ✨")
            repository.insertTask(tempTask)

            // Let 11 generate a flirty, caring motivational comment on this task!
            val apiKey = getApiKey()
            val comment = if (apiKey.isEmpty()) {
                listOf(
                    "You've got this! I'm cheering you on! 💖",
                    "Do your best, sweetie! I'll be waiting! 😉☕",
                    "A task? Let's crush it together! ✨",
                    "Don't overwork yourself, okay? I care about you! 🥰"
                ).random()
            } else {
                try {
                    val prompt = "The user added a task: '$title'. Give a single, short, super cute, slightly flirty, or highly motivating comment from '11' (personal assistant/crush) encouraging them to complete it. Keep it under 15 words and very playful!"
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                        systemInstruction = compileSystemInstruction(),
                        generationConfig = GenerationConfig(temperature = 0.9f)
                    )
                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "cheering you on! ✨"
                } catch (e: Exception) {
                    "Cheering you on! ✨"
                }
            }

            // Find the recently inserted task and update its comment
            val currentTasks = repository.allTasks.first()
            val insertedTask = currentTasks.find { it.title == title && it.dueTime == dueTime && it.comment == "Thinking of a cute comment... ✨" }
            if (insertedTask != null) {
                repository.insertTask(insertedTask.copy(comment = comment))
            }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val apiKey = getApiKey()
            val celebrateComment = if (newCompleted) {
                if (apiKey.isEmpty()) {
                    listOf(
                        "Yay! You did it! So proud of you! 🥰💖",
                        "Incredible job, smarty! Let's celebrate! ☕🎉",
                        "See? I knew you could do it! 😉✨",
                        "You did so well! Now take a sweet break! 🌸"
                    ).random()
                } else {
                    try {
                        val prompt = "The user completed their task: '${task.title}'. Give a super short (under 12 words), extremely happy, flirty, or rewarding congratulations comment from '11' celebrating their win!"
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            systemInstruction = compileSystemInstruction(),
                            generationConfig = GenerationConfig(temperature = 0.9f)
                        )
                        val response = RetrofitClient.service.generateContent(apiKey, request)
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            ?: "So proud of you! 🥰💖"
                    } catch (e: Exception) {
                        "So proud of you! 🥰💖"
                    }
                }
            } else {
                task.comment // Keep old comment if unchecking
            }

            repository.updateTaskStatus(task.id, newCompleted, celebrateComment)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    // --- Detail / Memory Actions ---

    fun saveDetail(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            repository.insertDetail(UserDetail(key = key, value = value))
        }
    }

    fun deleteDetail(key: String) {
        viewModelScope.launch {
            repository.deleteDetailByKey(key)
        }
    }

    // --- Decision Help ---

    fun helpMeChoose(options: String) {
        if (options.isBlank()) return
        _isChoiceLoading.value = true
        _choiceResult.value = null

        viewModelScope.launch {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(1000)
                }
                val mockChoices = options.split(",").map { it.trim() }
                val choice = mockChoices.randomOrNull() ?: "the first option"
                _choiceResult.value = "Hmm, since I don't have my AI key, I'll choose **$choice** for you! ☕ Tell me if I made the right call! 😉💖"
                _isChoiceLoading.value = false
                return@launch
            }

            try {
                val prompt = "The user is struggling to decide between these options: '$options'. Help them make a choice! Pick one option playfully, and give a funny, flirty, or warm explanation why you chose it. Keep it under 3 sentences!"
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = compileSystemInstruction(),
                    generationConfig = GenerationConfig(temperature = 0.85f)
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                _choiceResult.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "I choose everything that makes you happy! 🥰"
            } catch (e: Exception) {
                _choiceResult.value = "Oh no, choosing is hard! Let's try again in a bit, love. 💖"
            } finally {
                _isChoiceLoading.value = false
            }
        }
    }

    fun clearChoice() {
        _choiceResult.value = null
    }

    // --- Unexpected Compliments ---

    fun generateCompliment() {
        _isComplimentLoading.value = true
        _complimentResult.value = null

        viewModelScope.launch {
            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(1000)
                }
                _complimentResult.value = listOf(
                    "You have this amazing way of making my day brighter just by opening this app. ☕✨",
                    "I love how hard you work. You're incredibly smart and handsome/beautiful! 😉💖",
                    "Did anyone tell you today that you have the most beautiful smile? Yes, you! 🥰",
                    "You're doing amazing, honey. Don't let anyone tell you otherwise! 🌸"
                ).random()
                _isComplimentLoading.value = false
                return@launch
            }

            try {
                val prompt = "Spontaneously generate a unique, unexpected, beautiful, and slightly flirty compliment or message of encouragement from '11' to the user to make them smile immediately. Make it feel authentic, sweet, and romantic. Under 3 sentences!"
                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = compileSystemInstruction(),
                    generationConfig = GenerationConfig(temperature = 0.95f)
                )
                val response = RetrofitClient.service.generateContent(apiKey, request)
                _complimentResult.value = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "You make my virtual heart skip a beat! 🥰"
            } catch (e: Exception) {
                _complimentResult.value = "You are absolutely wonderful, even when my connection is failing! 💖"
            } finally {
                _isComplimentLoading.value = false
            }
        }
    }

    fun clearCompliment() {
        _complimentResult.value = null
    }
}
