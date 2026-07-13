package com.example.data

import kotlinx.coroutines.flow.Flow

class VaultRepository(
    private val profileDao: ProfileDao,
    private val secureNoteDao: SecureNoteDao,
    private val biometricLogDao: BiometricLogDao
) {
    val activeProfiles: Flow<List<Profile>> = profileDao.getActiveProfiles()
    val allNotes: Flow<List<SecureNote>> = secureNoteDao.getAllNotes()
    val logs: Flow<List<BiometricLog>> = biometricLogDao.getLogs()

    suspend fun insertProfile(profile: Profile) {
        profileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    suspend fun clearProfiles() {
        profileDao.clearProfiles()
    }

    suspend fun insertNote(note: SecureNote) {
        secureNoteDao.insertNote(note)
    }

    suspend fun updateNote(note: SecureNote) {
        secureNoteDao.updateNote(note)
    }

    suspend fun deleteNote(note: SecureNote) {
        secureNoteDao.deleteNote(note)
    }

    suspend fun insertLog(log: BiometricLog) {
        biometricLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        biometricLogDao.clearLogs()
    }
}
