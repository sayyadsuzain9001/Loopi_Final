package com.example.myapplication

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Single entry point for account, session and settings data.
 *
 * Everything is persisted in the Room database (tables `users` and
 * `app_settings`). An in-memory copy of the settings and of the signed-in
 * user is kept as Compose state so screens can read them without blocking;
 * every change is written straight back to Room on a background thread.
 */
object LoopiLocalStore {

    private lateinit var database: LoopiDatabase

    @OptIn(ExperimentalCoroutinesApi::class)
    private val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    private val settings = mutableStateMapOf<String, String>()

    /** The account that currently holds the session flag (users.isLoggedIn = 1), or null. */
    var currentUser by mutableStateOf<UserEntity?>(null)
        private set

    /**
     * Recovery code shown once on the "Account created" screen. Kept in memory only,
     * never written to disk; the database holds just its hash.
     */
    var recoveryCodeToShow: String? = null
        private set

    /** Account verified by "Forgot password" and allowed to set a new password (memory only). */
    private var resetUserId: Int? = null

    @Volatile
    private var ready = false

    fun init(context: Context) {
        if (ready) return
        synchronized(this) {
            if (ready) return
            val appContext = context.applicationContext
            database = LoopiDatabase.getDatabase(appContext)
            val (savedSettings, user) = runBlocking(Dispatchers.IO) {
                LegacyPreferencesImporter.importOnce(appContext, database)
                database.appSettingDao().getAll() to database.userDao().getLoggedInUser()
            }
            savedSettings.forEach { settings[it.key] = it.value }
            currentUser = user
            ready = true
        }
    }

    /* ---------------- settings (table app_settings) ---------------- */

    fun getBoolean(key: String, default: Boolean): Boolean = settings[key]?.toBooleanStrictOrNull() ?: default

    fun getInt(key: String, default: Int): Int = settings[key]?.toIntOrNull() ?: default

    fun setBoolean(key: String, value: Boolean) = put(key, value.toString())

    fun setInt(key: String, value: Int) = put(key, value.toString())

    private fun put(key: String, value: String) {
        settings[key] = value
        writer.launch { database.appSettingDao().put(AppSettingEntity(key, value)) }
    }

    /* ---------------- accounts and session (table users) ---------------- */

    sealed interface SignUpResult {
        data object Created : SignUpResult
        data object EmailTaken : SignUpResult
    }

    sealed interface SignInResult {
        data class Success(val user: UserEntity) : SignInResult
        data object NoAccounts : SignInResult
        data object WrongCredentials : SignInResult
    }

    /** Creates the account with hashed password + recovery code and starts its session. */
    suspend fun signUp(fullName: String, email: String, password: String): SignUpResult {
        val normalisedEmail = email.trim().lowercase()
        val recoveryCode = createRecoveryCode()
        val result = withContext(Dispatchers.IO) {
            val dao = database.userDao()
            if (dao.getUserByEmail(normalisedEmail) != null) return@withContext null
            val password = PasswordHasher.hash(password)
            val recovery = PasswordHasher.hash(recoveryCode)
            val user = UserEntity(
                fullName = fullName.trim(),
                email = normalisedEmail,
                passwordSalt = password.salt,
                passwordHash = password.hash,
                recoverySalt = recovery.salt,
                recoveryHash = recovery.hash
            )
            val id = try {
                dao.insertUser(user).toInt()
            } catch (_: SQLiteConstraintException) {
                return@withContext null
            }
            dao.startSession(id)
            dao.getUserById(id)
        } ?: return SignUpResult.EmailTaken
        recoveryCodeToShow = recoveryCode
        currentUser = result
        return SignUpResult.Created
    }

    fun consumeRecoveryCode() {
        recoveryCodeToShow = null
    }

    suspend fun signIn(email: String, password: String): SignInResult {
        val result = withContext(Dispatchers.IO) {
            val dao = database.userDao()
            if (dao.getUserCount() == 0) return@withContext SignInResult.NoAccounts
            val user = dao.getUserByEmail(email.trim().lowercase())
            if (user == null || !PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
                return@withContext SignInResult.WrongCredentials
            }
            dao.startSession(user.id)
            SignInResult.Success(dao.getUserById(user.id) ?: user)
        }
        if (result is SignInResult.Success) currentUser = result.user
        return result
    }

    /** Checks e-mail + recovery code; on success the next [resetPassword] applies to that account. */
    suspend fun verifyRecovery(email: String, recoveryCode: String): Boolean {
        val user = withContext(Dispatchers.IO) { database.userDao().getUserByEmail(email.trim().lowercase()) }
        val ok = user != null && withContext(Dispatchers.Default) {
            PasswordHasher.verify(recoveryCode.trim(), user.recoverySalt, user.recoveryHash)
        }
        resetUserId = if (ok) user!!.id else null
        return ok
    }

    val canResetPassword: Boolean get() = resetUserId != null

    suspend fun resetPassword(newPassword: String): Boolean {
        val id = resetUserId ?: return false
        withContext(Dispatchers.IO) {
            val dao = database.userDao()
            val user = dao.getUserById(id) ?: return@withContext
            val hashed = PasswordHasher.hash(newPassword)
            dao.updateUser(user.copy(passwordSalt = hashed.salt, passwordHash = hashed.hash))
        }
        resetUserId = null
        return true
    }

    suspend fun completeSetup(interests: Set<String>) {
        val user = currentUser ?: return
        val updated = user.copy(interests = interests.joinToString(","), setupCompleted = true, isLoggedIn = true)
        withContext(Dispatchers.IO) { database.userDao().updateUser(updated) }
        currentUser = updated
    }

    /** Returns false if another account already uses [email]. */
    suspend fun updateProfile(fullName: String, email: String, studentId: String, bio: String): Boolean {
        val user = currentUser ?: return false
        val normalisedEmail = email.trim().lowercase()
        val updated = user.copy(fullName = fullName.trim(), email = normalisedEmail, studentId = studentId.trim(), bio = bio.trim())
        val saved = withContext(Dispatchers.IO) {
            val owner = database.userDao().getUserByEmail(normalisedEmail)
            if (owner != null && owner.id != user.id) return@withContext false
            database.userDao().updateUser(updated)
            true
        }
        if (saved) currentUser = updated
        return saved
    }

    fun setProfileImage(uri: String) {
        val updated = currentUser?.copy(profileImageUri = uri) ?: return
        currentUser = updated
        writer.launch { database.userDao().updateUser(updated) }
    }

    fun signOut() {
        currentUser = null
        writer.launch { database.userDao().logOutEveryone() }
    }

    private fun createRecoveryCode(): String = ByteArray(8).also(SecureRandom()::nextBytes)
        .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}

/** PBKDF2-HMAC-SHA256 with a random 16-byte salt per secret. */
object PasswordHasher {
    data class Hashed(val salt: String, val hash: String)

    fun hash(secret: String): Hashed {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        return Hashed(encode(salt), encode(deriveSecretHash(secret, salt)))
    }

    fun verify(secret: String, salt: String, hash: String): Boolean {
        val saltBytes = runCatching { Base64.getDecoder().decode(salt) }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return false
        val expected = runCatching { Base64.getDecoder().decode(hash) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, deriveSecretHash(secret, saltBytes))
    }

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}

/**
 * Earlier builds kept the account, settings, community and reports in SharedPreferences.
 * On the first launch of this version they are copied into Room once and the old files are cleared.
 */
private object LegacyPreferencesImporter {
    private const val DONE_KEY = "legacy_prefs_imported_v9"

    suspend fun importOnce(context: Context, db: LoopiDatabase) {
        val settingsDao = db.appSettingDao()
        if (settingsDao.getAll().any { it.key == DONE_KEY }) return

        val auth = context.getSharedPreferences("loopi_auth", Context.MODE_PRIVATE)
        val email = auth.getString("email", null)?.trim()?.lowercase()
        if (!email.isNullOrBlank() && db.userDao().getUserByEmail(email) == null) {
            var salt = auth.getString("password_salt", null)
            var hash = auth.getString("password_hash", null)
            val legacyPlain = auth.getString("password", null)
            if ((salt == null || hash == null) && legacyPlain != null) {
                PasswordHasher.hash(legacyPlain).also { salt = it.salt; hash = it.hash }
            }
            if (salt != null && hash != null) {
                val id = db.userDao().insertUser(
                    UserEntity(
                        fullName = auth.getString("full_name", "").orEmpty(),
                        email = email,
                        passwordSalt = salt!!,
                        passwordHash = hash!!,
                        recoverySalt = auth.getString("recovery_salt", "").orEmpty(),
                        recoveryHash = auth.getString("recovery_hash", "").orEmpty(),
                        interests = auth.getStringSet("interests", emptySet()).orEmpty().joinToString(","),
                        studentId = auth.getString("student_id", "").orEmpty(),
                        bio = auth.getString("profile_bio", "").orEmpty(),
                        profileImageUri = auth.getString("profile_image_uri", null),
                        setupCompleted = auth.getBoolean("setup_completed", false)
                    )
                ).toInt()
                if (auth.getBoolean("logged_in", false)) db.userDao().startSession(id)
            }
        }

        listOf("loopi_settings", "loopi_preferences").forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                if (value is Boolean || value is Int || value is Long || value is String) {
                    settingsDao.put(AppSettingEntity(key, value.toString()))
                }
            }
        }

        val community = context.getSharedPreferences("loopi_community", Context.MODE_PRIVATE)
        community.getStringSet("joined", emptySet()).orEmpty().forEach {
            db.communityDao().join(CommunityMembershipEntity(it))
        }
        runCatching {
            val posts = JSONArray(community.getString("posts", "[]"))
            for (i in 0 until posts.length()) {
                val p = posts.getJSONObject(i)
                db.communityDao().insertPost(
                    CommunityPostEntity(groupId = p.getString("g"), author = p.getString("a"), text = p.getString("t"), postedAt = p.getLong("at"))
                )
            }
        }

        runCatching {
            val support = context.getSharedPreferences("loopi_support", Context.MODE_PRIVATE)
            val reports = JSONArray(support.getString("reports", "[]"))
            for (i in 0 until reports.length()) {
                val r = reports.getJSONObject(i)
                db.supportReportDao().insertReport(
                    SupportReportEntity(type = r.optString("type"), description = r.optString("description"), steps = r.optString("steps"), createdAt = r.optLong("createdAt"))
                )
            }
        }

        listOf("loopi_auth", "loopi_settings", "loopi_preferences", "loopi_community", "loopi_support").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
            context.deleteSharedPreferences(it)
        }
        settingsDao.put(AppSettingEntity(DONE_KEY, "true"))
    }
}
