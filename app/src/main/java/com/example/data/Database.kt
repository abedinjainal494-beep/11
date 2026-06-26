package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "eleven"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVent: Boolean = false
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueTime: Long,
    val isCompleted: Boolean = false,
    val createdTime: Long = System.currentTimeMillis(),
    val comment: String? = null
)

@Entity(tableName = "user_details")
data class UserDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueTime ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, comment = :comment WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean, comment: String?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)
}

@Dao
interface UserDetailDao {
    @Query("SELECT * FROM user_details")
    fun getAllDetailsFlow(): Flow<List<UserDetail>>

    @Query("SELECT * FROM user_details")
    suspend fun getAllDetails(): List<UserDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetail(detail: UserDetail)

    @Query("DELETE FROM user_details WHERE `key` = :key")
    suspend fun deleteDetailByKey(key: String)
}

@Database(entities = [ChatMessage::class, Task::class, UserDetail::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun taskDao(): TaskDao
    abstract fun userDetailDao(): UserDetailDao
}
