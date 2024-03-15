package moe.nea.gaybot

import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.createChatInputCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.GuildChatInputCommandInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.allowedMentions
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.onEach


suspend fun main() {
    GitService.ensureInitialized()
    val masterBranch = "prerelease"
    GitService.checkoutBranch(masterBranch)
    GitService.fetchRepo()
    GitService.setHeadTo("origin/$masterBranch")

    val kord = Kord(System.getenv("TOKEN"))
    val moulberryBushId = Snowflake(516977525906341928)
    val boostRoleId = Snowflake(736751156310704259)
    val maintainerRoleId = Snowflake(1014324200598667324)
    val client = HttpClient(CIO)
    BoosterNamesService.load()

    kord.getGlobalApplicationCommands().onEach { it.delete() }
    val moulberryBush = kord.getGuild(moulberryBushId)
    moulberryBush.getApplicationCommands().onEach {
        it.delete()
    }
    val linkRainbow = moulberryBush.createChatInputCommand(
        "rainbowlink",
        "Link a minecraft account to your booster status for rainbow names in /pv"
    ) {
        this.string("mcname", "IGN to link to your booster status") {
            required = true
        }
    }
    moulberryBush.createChatInputCommand(
        "updaterainbownames",
        "Regenerate rainbownames branch. Admin/Debug only"
    )
    suspend fun performGitUpdate(): String {
        val legacy = RainbowManager.getLegacyRainbowName().toMutableList()
        for ((user, uuid) in BoosterNamesService.getAllUsers()) {
            if (boostRoleId in (moulberryBush.getMemberOrNull(user)?.roleIds ?: emptySet())) {
                if (uuid !in legacy) {
                    legacy.add(uuid)
                }
            }
        }
        return GitService.useLock {
            GitService.checkoutBranch(masterBranch)
            GitService.fetchRepo()
            GitService.setHeadTo("origin/$masterBranch")
            RainbowManager.setRainbowNamesInMisc(legacy)
            GitService.commitFiles("Updated rainbownames", RainbowManager.miscFile)
            GitService.pushHeadTo("origin", "bot/rainbownames")
            GitService.parseCommit("HEAD")
        }
    }

    val gson = Gson()
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        if (interaction.command.rootName != "updaterainbownames") return@on
        if (maintainerRoleId !in interaction.user.roleIds) {
            interaction.respondEphemeral {
                content =
                    "You do not have permissions to force an update to the rainbownames branch. You probably meant to use </rainbowlink:${linkRainbow.id.value}>."
            }
            return@on
        }
        val response = interaction.deferPublicResponse()
        val sha = performGitUpdate()
        response.respond {
            content =
                "Pushed [`bot/rainbownames`](<https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/tree/bot/rainbownames>) to [`$sha`](<https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO/commit/$sha>)."
        }
    }
    kord.on<GuildChatInputCommandInteractionCreateEvent> {
        if (interaction.command.rootName != "rainbowlink") return@on
        if (boostRoleId !in interaction.user.roleIds && maintainerRoleId !in interaction.user.roleIds) {
            interaction.respondEphemeral {
                content =
                    "You are not currently boosting this server. Please boost the server in order to link your account and get a rainbow name in /pv. Check out `/pv Eisengolem` to see how it looks!"
            }
            return@on
        }
        val name = interaction.command.strings["mcname"]!!
        val response = interaction.deferPublicResponse()
        if (!name.matches("^[_a-z0-9A-Z]{3,16}$".toRegex())) {
            response.respond {
                content = "$name does not seeem to be a valid minecraft user name"
                allowedMentions()
            }
            return@on
        }
        val mojangText = client.get {
            url("https://api.ashcon.app/mojang/v2/user/$name")
        }.bodyAsText()
        val json = gson.fromJson(mojangText, JsonObject::class.java)
        if (json["error"] != null) {
            response.respond {
                allowedMentions()
                content = "Could not find uuid for $name: `${json["error"]}`"
            }
            return@on
        }
        val uuid = json.getAsJsonPrimitive("uuid").asString.replace("-", "")
        BoosterNamesService.setUuid(interaction.user.id, uuid)
        BoosterNamesService.save()
        response.respond {
            allowedMentions()
            content =
                "Changed your linked username to $name (`$uuid`). Please be aware that it may take about a day until this change is applied. If it takes longer than a day for your name to become rainbowed in /pv, please update your repository."
        }
    }
    kord.login {
        presence { playing("Rainbow names in /pv") }
    }
}

