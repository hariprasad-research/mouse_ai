package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.ConversationEntity
import com.example.data.local.entities.KnowledgeEntity
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import com.example.data.local.entities.ResearchEntity
import com.example.data.local.entities.SecurityAuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: String)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesList(conversationId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY importance DESC, updatedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY importance DESC, updatedAt DESC")
    suspend fun getAllMemoriesList(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' OR key LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM memories WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_base ORDER BY createdAt DESC")
    fun getAllKnowledge(): Flow<List<KnowledgeEntity>>

    @Query("SELECT * FROM knowledge_base ORDER BY createdAt DESC")
    suspend fun getAllKnowledgeList(): List<KnowledgeEntity>

    @Query("SELECT * FROM knowledge_base WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    suspend fun searchKnowledge(query: String): List<KnowledgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: KnowledgeEntity)

    @Query("DELETE FROM knowledge_base WHERE id = :id")
    suspend fun deleteKnowledge(id: String)

    @Query("DELETE FROM knowledge_base")
    suspend fun deleteAll()
}

@Dao
interface ResearchDao {
    @Query("SELECT * FROM research_records ORDER BY researchTimestamp DESC")
    fun getAllResearch(): Flow<List<ResearchEntity>>

    @Query("SELECT * FROM research_records ORDER BY researchTimestamp DESC")
    suspend fun getAllResearchList(): List<ResearchEntity>

    @Query("SELECT * FROM research_records WHERE question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%'")
    suspend fun searchResearch(query: String): List<ResearchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearch(research: ResearchEntity)

    @Update
    suspend fun updateResearch(research: ResearchEntity)

    @Query("DELETE FROM research_records WHERE id = :id")
    suspend fun deleteResearch(id: String)

    @Query("DELETE FROM research_records")
    suspend fun deleteAll()
}

@Dao
interface SecurityDao {
    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<SecurityAuditEntity>>

    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogsList(): List<SecurityAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: SecurityAuditEntity)

    @Query("DELETE FROM security_audit_logs")
    suspend fun clearAuditLogs()
}
