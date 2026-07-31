package com.example.demo1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete

@Dao
interface ChatDao {

    @Query(
        """
        SELECT
            s.id AS id,
            s.title AS title,
            s.updatedAt AS updatedAt,
            s.isPinned AS isPinned,
            COUNT(m.id) AS messageCount
        FROM chat_sessions s
        LEFT JOIN chat_messages m ON s.id = m.sessionId
        GROUP BY s.id
        ORDER BY s.isPinned DESC, s.updatedAt DESC
        """
    )
    suspend fun getSessionPreviews(): List<ChatSessionPreview>

    @Query(
        """
        SELECT * FROM chat_messages
        WHERE sessionId = :sessionId
        ORDER BY timestamp ASC, id ASC
        """
    )
    suspend fun getMessagesForSession(
        sessionId: Long
    ): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(
        session: ChatSessionEntity
    )

    @Insert
    suspend fun insertMessage(
        message: ChatMessageEntity
    )

    @Query(
        """
        UPDATE chat_sessions
        SET title = :title,
            updatedAt = :updatedAt
        WHERE id = :sessionId
        """
    )
    suspend fun updateSessionTitleAndTime(
        sessionId: Long,
        title: String,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE chat_sessions
        SET updatedAt = :updatedAt
        WHERE id = :sessionId
        """
    )
    suspend fun updateSessionTime(
        sessionId: Long,
        updatedAt: Long
    )

    @Query(
        """
    UPDATE chat_sessions
    SET isPinned = :isPinned
    WHERE id = :sessionId
    """
    )
    suspend fun updateSessionPinned(
        sessionId: Long,
        isPinned: Boolean
    )

    @Delete
    suspend fun deleteSession(
        session: ChatSessionEntity
    )

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySessionId(
        sessionId: Long
    )

    @Query(
        """
        UPDATE chat_sessions
        SET title = :newTitle,
            updatedAt = :updatedAt
        WHERE id = :sessionId
        """
    )
    suspend fun updateSessionTitleManual(
        sessionId: Long,
        newTitle: String,
        updatedAt: Long
    )
}