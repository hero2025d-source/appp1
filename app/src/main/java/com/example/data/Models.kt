package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val registeredAt: Long = System.currentTimeMillis(),
    val faceWidthHeightRatio: Float = 1.0f,
    val eyeDistanceRatio: Float = 0.5f,
    val isActive: Boolean = true
)

@Entity(tableName = "secure_notes")
data class SecureNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // e.g., "Credentials", "Private Key", "Intel", "Personal"
    val isLocked: Boolean = true,
    val securityLevel: String = "HIGH", // "CRITICAL", "HIGH", "MEDIUM"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "biometric_logs")
data class BiometricLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operatorName: String,
    val status: String, // e.g., "ACCESS_GRANTED", "ACCESS_DENIED", "CALIBRATED"
    val method: String, // e.g., "FACE_SCANNER", "SYSTEM_BIOMETRICS"
    val confidence: Float, // percentage match
    val details: String // e.g., "Left eye blinked, lightning optimal"
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE isActive = 1 ORDER BY registeredAt DESC")
    fun getActiveProfiles(): Flow<List<Profile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("DELETE FROM profiles")
    suspend fun clearProfiles()
}

@Dao
interface SecureNoteDao {
    @Query("SELECT * FROM secure_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<SecureNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: SecureNote)

    @Update
    suspend fun updateNote(note: SecureNote)

    @Delete
    suspend fun deleteNote(note: SecureNote)
}

@Dao
interface BiometricLogDao {
    @Query("SELECT * FROM biometric_logs ORDER BY timestamp DESC LIMIT 50")
    fun getLogs(): Flow<List<BiometricLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BiometricLog)

    @Query("DELETE FROM biometric_logs")
    suspend fun clearLogs()
}
