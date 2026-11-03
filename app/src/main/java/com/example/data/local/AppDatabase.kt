package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.KnowledgeDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.ResearchDao
import com.example.data.local.dao.SecurityDao
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.KnowledgeEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ResearchEntity
import com.example.data.local.entities.SecurityAuditEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        KnowledgeEntity::class,
        ResearchEntity::class,
        SecurityAuditEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun researchDao(): ResearchDao
    abstract fun securityDao(): SecurityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mouse_os_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate core identity and foundational knowledge
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val convId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            // 1. Initial Conversation
            database.conversationDao().insertConversation(
                ConversationEntity(
                    id = convId,
                    title = "System Genesis & Welcome",
                    createdAt = now,
                    updatedAt = now
                )
            )

            database.messageDao().insertMessage(
                MessageEntity(
                    conversationId = convId,
                    role = "assistant",
                    content = "Greetings. I am MOUSE (My Own Ultimate Smart Entity) — your personal, offline-first AI companion.\n\n" +
                            "By design, Internet access is strictly OFF by default to guarantee your total privacy and security. " +
                            "I operate using local knowledge retrieval, persistent personal memory, and on-device reasoning. " +
                            "When you explicitly enable Internet access in the Permission Gate, I can tap into online providers like Gemini or OpenAI-compatible models.\n\n" +
                            "How may I assist you today?",
                    providerUsed = "MOUSE Local Core",
                    isOnline = false,
                    timestamp = now,
                    sourcesJson = "[\"Core Operating Directive 1.0\"]",
                    confidence = 1.0f
                )
            )

            // 2. Initial Core Memories
            val initialMemories = listOf(
                MemoryEntity(
                    category = "PERSONAL_FACT",
                    key = "assistant_identity",
                    content = "MOUSE: My Own Ultimate Smart Entity. Single continuous identity, honest, privacy-first, owner-aligned.",
                    importance = 5
                ),
                MemoryEntity(
                    category = "PREFERENCE",
                    key = "default_network_policy",
                    content = "Internet access must remain OFF by default until explicitly enabled by owner.",
                    importance = 5
                ),
                MemoryEntity(
                    category = "PREFERENCE",
                    key = "response_tone",
                    content = "Clear, analytical, concise, and truthful. Never fabricate sources or hallucinate network activity.",
                    importance = 4
                )
            )
            for (mem in initialMemories) {
                database.memoryDao().insertMemory(mem)
            }

            // 3. Initial Foundational Knowledge
            val initialKnowledge = listOf(
                KnowledgeEntity(
                    title = "Offline Intelligence Architecture",
                    category = "TECHNICAL",
                    content = "MOUSE offline intelligence operates across three distinct layers: Layer A performs local keyword & semantic retrieval over memories and research; Layer B executes on-device reasoning and small language model inference; Layer C performs transparent offline routing.",
                    tags = "architecture,offline,ai",
                    isVerified = true
                ),
                KnowledgeEntity(
                    title = "Android Keystore Security",
                    category = "SECURITY",
                    content = "All API keys for online providers (Gemini, OpenAI) are encrypted using AES-256-GCM hardware-backed keys inside AndroidKeyStore. Keys are never printed in plain text, logged, or exported without authorization.",
                    tags = "security,keystore,encryption",
                    isVerified = true
                ),
                KnowledgeEntity(
                    title = "Honesty & Non-Hallucination Directive",
                    category = "CORE_PRINCIPLES",
                    content = "MOUSE must never claim a web search occurred when it did not, never claim an offline answer came from an online model, and never pretend a failed task was completed.",
                    tags = "honesty,ethics,rules",
                    isVerified = true
                )
            )
            for (k in initialKnowledge) {
                database.knowledgeDao().insertKnowledge(k)
            }

            // 4. Initial Security Audit Log
            database.securityDao().insertAuditLog(
                SecurityAuditEntity(
                    timestamp = now,
                    action = "INITIAL_SYSTEM_BOOT",
                    operatingMode = "CONTROLLED",
                    status = "AUTHORIZED",
                    details = "MOUSE OS database initialized. Internet permission initialized to OFF."
                )
            )
        }
    }
}
