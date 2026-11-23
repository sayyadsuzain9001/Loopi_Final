package com.example.myapplication

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import kotlin.random.Random

/**
 * Creates one clearly labelled demo account with three weeks of sample study history,
 * so a fresh install (or a phone) has realistic data to present.
 *
 * Everything is written to Room like real data. It runs once per install (flag in app_settings)
 * and only after the starter topics and cards exist.
 */
object DemoDataSeeder {
    const val DEMO_EMAIL = "demo@loopi.app"
    const val DEMO_PASSWORD = "Demo1234"
    private const val FLAG = "demo_data_seeded_v1"
    private const val DAY = 86_400_000L

    /** Target accuracy per topic, so Progress shows a clear strong topic and a clear weak spot. */
    private val topicSkill = mapOf(
        "Databases" to 0.85,
        "Networks" to 0.65,
        "Algorithms" to 0.55,
        "Operating Systems" to 0.35
    )

    suspend fun seedIfNeeded(db: LoopiDatabase) {
        val settings = db.appSettingDao()
        if (settings.getAll().any { it.key == FLAG }) return
        val topics = db.topicDao().getAllTopics().first()
        val cards = db.flashcardDao().getAllFlashcards().first()
        if (topics.isEmpty() || cards.isEmpty()) return

        val now = System.currentTimeMillis()
        val random = Random(42)

        db.withTransaction {
            if (db.userDao().getUserByEmail(DEMO_EMAIL) == null) {
                val password = PasswordHasher.hash(DEMO_PASSWORD)
                val recovery = PasswordHasher.hash("DEMO0000DEMO0000")
                db.userDao().insertUser(
                    UserEntity(
                        fullName = "Demo Student",
                        email = DEMO_EMAIL,
                        passwordSalt = password.salt,
                        passwordHash = password.hash,
                        recoverySalt = recovery.salt,
                        recoveryHash = recovery.hash,
                        interests = "Computer Science,Information Technology",
                        studentId = "DEMO-001",
                        bio = "Demo account with sample study history",
                        setupCompleted = true,
                        isLoggedIn = false,
                        createdAt = now - 21 * DAY,
                        lastLoginAt = now - DAY
                    )
                )
            }

            // 1 to 20 days ago, with every one of the last 6 days present so there is a streak.
            val studyDays = (1..6).toList() + listOf(8, 9, 11, 13, 14, 16, 18, 20)
            studyDays.forEach { daysAgo ->
                val dayStart = now - daysAgo * DAY
                topics.shuffled(random).take(2).forEach { topic ->
                    val topicCards = cards.filter { it.topicId == topic.id }
                    if (topicCards.isEmpty()) return@forEach
                    val skill = topicSkill[topic.title] ?: 0.6
                    topicCards.shuffled(random).take(random.nextInt(3, 6)).forEachIndexed { index, card ->
                        val correct = random.nextDouble() < skill
                        val confidence = when {
                            correct && random.nextDouble() < 0.7 -> "I know it"
                            correct -> if (random.nextBoolean()) "Maybe" else "Not sure"
                            random.nextDouble() < 0.35 -> "I know it" // sure but wrong
                            else -> if (random.nextBoolean()) "Not sure" else "Maybe"
                        }
                        val wrongAnswer = topicCards.firstOrNull { it.id != card.id }?.answer ?: "I don't know"
                        db.reviewResultDao().insertResult(
                            ReviewResultEntity(
                                topicId = topic.id,
                                flashcardId = card.id,
                                question = card.question,
                                correctAnswer = card.answer,
                                explanation = card.explanation,
                                userAnswer = if (correct) card.answer else wrongAnswer,
                                isCorrect = correct,
                                confidence = confidence,
                                responseTimeMillis = random.nextLong(4_000, 25_000),
                                reviewedAt = dayStart + (18 * 3_600_000L) + index * 40_000L
                            )
                        )
                    }
                }
            }

            db.studyPackDao().insertStudyPack(
                StudyPackEntity(
                    name = "Exam revision (demo)",
                    description = "Sample pack created by the demo account",
                    topicNamesJson = JSONArray(topics.take(2).map { it.title }).toString(),
                    createdAt = now - 5 * DAY,
                    updatedAt = now - 5 * DAY
                )
            )
            db.communityDao().join(CommunityMembershipEntity("db", joinedAt = now - 10 * DAY))
            db.communityDao().insertPost(
                CommunityPostEntity(
                    groupId = "db",
                    author = "Demo Student",
                    text = "Practised normal forms today. 3NF finally makes sense!",
                    postedAt = now - 3 * DAY
                )
            )
            db.supportReportDao().insertReport(
                SupportReportEntity(
                    type = "Other",
                    description = "Sample report from the demo account: the chart labels are small on my phone.",
                    steps = "Open Progress and look at the week chart.",
                    createdAt = now - 2 * DAY
                )
            )
            settings.put(AppSettingEntity(FLAG, "true"))
        }
    }
}
