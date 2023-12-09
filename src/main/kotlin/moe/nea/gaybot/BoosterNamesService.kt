package moe.nea.gaybot

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object BoosterNamesService {
    private val mutex = Mutex()
    private val users = mutableMapOf<Snowflake, String>()
    private val database = File("database.json")
    private val databaseBackup = File("database_backup.json")
    private val gson = Gson()
    private var saved = false

    suspend fun load() {
        if (database.exists()) {
            val map = gson.fromJson(database.readText(), object : TypeToken<Map<String, String>>() {})
            mutex.withLock {
                users.putAll(map.mapKeys { Snowflake(it.key) })
            }
        }
    }

    suspend fun setUuid(discord: Snowflake, uuid: String) {
        return mutex.withLock {
            users[discord] = uuid
            saved = false
        }
    }

    suspend fun save() {
        mutex.withLock {
            if (!saved) {
                databaseBackup.writeText(gson.toJson(JsonObject().also {
                    users.forEach { (user, value) ->
                        it.addProperty(user.value.toString(), value)
                    }
                }))
                gson.fromJson(databaseBackup.readText(), JsonObject::class.java)
                Files.move(databaseBackup.toPath(), database.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            }
            saved = true
        }
    }

    suspend fun getAllUsers(): List<Pair<Snowflake, String>> {
        return mutex.withLock { users.toList() }
    }
}