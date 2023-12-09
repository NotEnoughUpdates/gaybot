package moe.nea.gaybot

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject

object RainbowManager {
    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val rainbowNameFile = GitService.repoRoot.resolve("constants/legacyrainbownames.json")
    val miscFile = GitService.repoRoot.resolve("constants/misc.json")

    fun getLegacyRainbowName(): List<String> {
        val j = gson.fromJson(rainbowNameFile.readText(), JsonObject::class.java)
        return j.getAsJsonArray("rainbow_names").map { it.asString }
    }

    fun setRainbowNamesInMisc(list: List<String>) {
        val j = gson.fromJson(miscFile.readText(), JsonObject::class.java)
        val special_bois = if (j.has("special_bois")) {
            j.getAsJsonArray("special_bois")
        } else {
            JsonArray().also { j.add("special_bois", it) }
        }
        special_bois.removeAll { true }
        for (s in list) {
            special_bois.add(s)
        }
        miscFile.writeText(gson.toJson(j))
    }

}