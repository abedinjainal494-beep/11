package com.example.data

import kotlinx.coroutines.flow.Flow

class MainRepository(private val db: AppDatabase) {
    private val chatDao = db.chatDao()
    private val taskDao = db.taskDao()
    private val userDetailDao = db.userDetailDao()

    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allDetailsFlow: Flow<List<UserDetail>> = userDetailDao.getAllDetailsFlow()

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun clearChat() {
        chatDao.clearChat()
    }

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean, comment: String?) {
        taskDao.updateTaskStatus(id, isCompleted, comment)
    }

    suspend fun deleteTask(id: Int) {
        taskDao.deleteTask(id)
    }

    suspend fun getAllDetails(): List<UserDetail> {
        return userDetailDao.getAllDetails()
    }

    suspend fun insertDetail(detail: UserDetail) {
        userDetailDao.insertDetail(detail)
    }

    suspend fun deleteDetailByKey(key: String) {
        userDetailDao.deleteDetailByKey(key)
    }
}
