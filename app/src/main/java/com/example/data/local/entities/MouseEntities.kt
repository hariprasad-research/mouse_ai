package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val providerUsed: String = "MOUSE Local Core",
    val isOnline: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val sourcesJson: String = "[]",
    val confidence: Float = 1.0f
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "PERSONAL_FACT", "PREFERENCE", "PROJECT", "EVENT", "CONVERSATION_SUMMARY", "CORRECTION"
    val key: String,
    val content: String,
    val importance: Int = 3, // 1 to 5
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_base")
data class KnowledgeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val content: String,
    val tags: String = "",
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "research_records")
data class ResearchEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String,
    val sourcesJson: String = "[]",
    val publisher: String = "MOUSE Research Engine",
    val researchTimestamp: Long = System.currentTimeMillis(),
    val verificationTimestamp: Long? = null,
    val confidence: Float = 0.95f,
    val verificationStatus: String = "VERIFIED" // "VERIFIED", "UNVERIFIED", "CONFLICTING"
)

@Entity(tableName = "security_audit_logs")
data class SecurityAuditEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val operatingMode: String, // "CONTROLLED", "INTELLIGENT", "AUTONOMOUS"
    val status: String, // "AUTHORIZED", "BLOCKED", "CONFIRMED_BY_USER", "SYSTEM_DENIED"
    val details: String
)
