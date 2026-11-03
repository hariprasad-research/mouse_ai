package com.example.service

import com.example.data.local.AppDatabase
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.KnowledgeEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ResearchEntity
import com.example.data.local.entities.SecurityAuditEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class BackupMetadata(
    val version: Int,
    val exportTimestamp: Long,
    val conversationCount: Int,
    val messageCount: Int,
    val memoryCount: Int,
    val knowledgeCount: Int,
    val researchCount: Int
)

class BackupRestoreService(private val database: AppDatabase) {

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("format", "MOUSE_BACKUP_ARCHIVE")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Conversations
        val convList = database.conversationDao().getAllConversations()
        val convArray = JSONArray()
        // Using direct query
        // Let's get messages & conversations
        val messages = database.messageDao().getAllMessages()
        val messageArray = JSONArray()
        for (m in messages) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("conversationId", m.conversationId)
                put("role", m.role)
                put("content", m.content)
                put("providerUsed", m.providerUsed)
                put("isOnline", m.isOnline)
                put("timestamp", m.timestamp)
                put("sourcesJson", m.sourcesJson)
                put("confidence", m.confidence.toDouble())
            }
            messageArray.put(obj)
        }
        root.put("messages", messageArray)

        // 2. Memories
        val memories = database.memoryDao().getAllMemoriesList()
        val memArray = JSONArray()
        for (mem in memories) {
            val obj = JSONObject().apply {
                put("id", mem.id)
                put("category", mem.category)
                put("key", mem.key)
                put("content", mem.content)
                put("importance", mem.importance)
                put("createdAt", mem.createdAt)
                put("updatedAt", mem.updatedAt)
            }
            memArray.put(obj)
        }
        root.put("memories", memArray)

        // 3. Knowledge
        val knowledge = database.knowledgeDao().getAllKnowledgeList()
        val knArray = JSONArray()
        for (k in knowledge) {
            val obj = JSONObject().apply {
                put("id", k.id)
                put("title", k.title)
                put("category", k.category)
                put("content", k.content)
                put("tags", k.tags)
                put("isVerified", k.isVerified)
                put("createdAt", k.createdAt)
            }
            knArray.put(obj)
        }
        root.put("knowledge", knArray)

        // 4. Research
        val research = database.researchDao().getAllResearchList()
        val resArray = JSONArray()
        for (r in research) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("question", r.question)
                put("answer", r.answer)
                put("sourcesJson", r.sourcesJson)
                put("publisher", r.publisher)
                put("researchTimestamp", r.researchTimestamp)
                put("confidence", r.confidence.toDouble())
                put("verificationStatus", r.verificationStatus)
            }
            resArray.put(obj)
        }
        root.put("research", resArray)

        root.toString(2)
    }

    suspend fun inspectBackup(jsonString: String): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.optString("format") != "MOUSE_BACKUP_ARCHIVE") return@withContext null
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val msgCount = root.optJSONArray("messages")?.length() ?: 0
            val memCount = root.optJSONArray("memories")?.length() ?: 0
            val knCount = root.optJSONArray("knowledge")?.length() ?: 0
            val resCount = root.optJSONArray("research")?.length() ?: 0

            BackupMetadata(
                version = version,
                exportTimestamp = exportedAt,
                conversationCount = 1,
                messageCount = msgCount,
                memoryCount = memCount,
                knowledgeCount = knCount,
                researchCount = resCount
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreFromJson(jsonString: String, clearExisting: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.optString("format") != "MOUSE_BACKUP_ARCHIVE") return@withContext false

            if (clearExisting) {
                database.messageDao().deleteAll()
                database.memoryDao().deleteAll()
                database.knowledgeDao().deleteAll()
                database.researchDao().deleteAll()
            }

            // Restore Memories
            val memArray = root.optJSONArray("memories")
            if (memArray != null) {
                for (i in 0 until memArray.length()) {
                    val obj = memArray.getJSONObject(i)
                    database.memoryDao().insertMemory(
                        MemoryEntity(
                            id = obj.getString("id"),
                            category = obj.getString("category"),
                            key = obj.getString("key"),
                            content = obj.getString("content"),
                            importance = obj.optInt("importance", 3),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Knowledge
            val knArray = root.optJSONArray("knowledge")
            if (knArray != null) {
                for (i in 0 until knArray.length()) {
                    val obj = knArray.getJSONObject(i)
                    database.knowledgeDao().insertKnowledge(
                        KnowledgeEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            category = obj.getString("category"),
                            content = obj.getString("content"),
                            tags = obj.optString("tags", ""),
                            isVerified = obj.optBoolean("isVerified", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Research
            val resArray = root.optJSONArray("research")
            if (resArray != null) {
                for (i in 0 until resArray.length()) {
                    val obj = resArray.getJSONObject(i)
                    database.researchDao().insertResearch(
                        ResearchEntity(
                            id = obj.getString("id"),
                            question = obj.getString("question"),
                            answer = obj.getString("answer"),
                            sourcesJson = obj.optString("sourcesJson", "[]"),
                            publisher = obj.optString("publisher", "MOUSE Archive"),
                            researchTimestamp = obj.optLong("researchTimestamp", System.currentTimeMillis()),
                            confidence = obj.optDouble("confidence", 0.9).toFloat(),
                            verificationStatus = obj.optString("verificationStatus", "VERIFIED")
                        )
                    )
                }
            }

            // Restore Messages
            val msgArray = root.optJSONArray("messages")
            if (msgArray != null) {
                for (i in 0 until msgArray.length()) {
                    val obj = msgArray.getJSONObject(i)
                    database.messageDao().insertMessage(
                        MessageEntity(
                            id = obj.getString("id"),
                            conversationId = obj.getString("conversationId"),
                            role = obj.getString("role"),
                            content = obj.getString("content"),
                            providerUsed = obj.optString("providerUsed", "MOUSE Local Core"),
                            isOnline = obj.optBoolean("isOnline", false),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            sourcesJson = obj.optString("sourcesJson", "[]"),
                            confidence = obj.optDouble("confidence", 1.0).toFloat()
                        )
                    )
                }
            }

            // Log security audit
            database.securityDao().insertAuditLog(
                SecurityAuditEntity(
                    action = "RESTORE_DATABASE",
                    operatingMode = "INTELLIGENT",
                    status = "AUTHORIZED",
                    details = "Restored archive (clearExisting=$clearExisting)"
                )
            )

            true
        } catch (e: Exception) {
            false
        }
    }
}
