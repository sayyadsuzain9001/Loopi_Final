package com.example.myapplication

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** A study group the user has joined (the groups themselves are built-in). */
@Entity(tableName = "community_memberships")
data class CommunityMembershipEntity(
    @PrimaryKey
    val groupId: String,
    val joinedAt: Long = System.currentTimeMillis()
)

/** A post the user wrote in a study group. */
@Entity(
    tableName = "community_posts",
    indices = [Index(value = ["groupId"])]
)
data class CommunityPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val groupId: String,
    val author: String,
    val text: String,
    val postedAt: Long = System.currentTimeMillis()
)

@Dao
interface CommunityDao {
    @Query("SELECT * FROM community_memberships")
    suspend fun getMemberships(): List<CommunityMembershipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun join(membership: CommunityMembershipEntity)

    @Query("DELETE FROM community_memberships WHERE groupId = :groupId")
    suspend fun leave(groupId: String)

    @Query("SELECT * FROM community_posts ORDER BY postedAt ASC")
    suspend fun getPosts(): List<CommunityPostEntity>

    @Insert
    suspend fun insertPost(post: CommunityPostEntity): Long
}

/** A "Report a problem" submission from Help & Support. */
@Entity(tableName = "support_reports")
data class SupportReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val description: String,
    val steps: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SupportReportDao {
    @Insert
    suspend fun insertReport(report: SupportReportEntity): Long

    @Query("SELECT * FROM support_reports ORDER BY createdAt DESC")
    suspend fun getReports(): List<SupportReportEntity>
}
