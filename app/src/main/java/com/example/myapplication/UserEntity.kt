package com.example.myapplication

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One local Loopi account. Passwords and recovery codes are never stored:
 * only a random salt and a PBKDF2-HMAC-SHA256 hash of each (Base64 text).
 * [isLoggedIn] is the session flag used by the splash screen.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fullName: String,
    /** Stored lower-case so sign-in is case-insensitive. */
    val email: String,
    val passwordSalt: String,
    val passwordHash: String,
    val recoverySalt: String,
    val recoveryHash: String,
    /** Comma-separated study interests chosen in first-time setup. */
    val interests: String = "",
    val studentId: String = "",
    val bio: String = "",
    val profileImageUri: String? = null,
    val setupCompleted: Boolean = false,
    val isLoggedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L
)
