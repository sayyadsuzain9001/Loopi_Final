package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    /** ABORT makes the unique e-mail index reject a duplicate account. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logOutEveryone()

    @Query("UPDATE users SET isLoggedIn = 1, lastLoginAt = :time WHERE id = :id")
    suspend fun markLoggedIn(id: Int, time: Long)

    /** Only one account can hold the session flag at a time. */
    @Transaction
    suspend fun startSession(id: Int, time: Long = System.currentTimeMillis()) {
        logOutEveryone()
        markLoggedIn(id, time)
    }
}
