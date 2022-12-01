package tech.goksi.pterobot.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.replace
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.fixedRateTimer

private const val CONFIG_PREFIX = "Messages.Commands.NodeStatus."

class NodeStatusCmd : SimpleCommand() {

    companion object TaskMapping {
        val mapping: MutableMap<Long, Timer> = HashMap()
    }

    init {
        this.name = "nodestatus"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        this.options = listOf(
            OptionData(
                OptionType.BOOLEAN,
                "update",
                ConfigManager.config.getString(CONFIG_PREFIX + "OptionUpdateDescription"),
                false
            )
        )
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.member!!)
        if (event.member!!.hasPermission(Permission.ADMINISTRATOR) || pteroMember.isPteroAdmin()) {
            val update = event.getOption("update")?.asBoolean ?: false
            event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()

            event.hook.sendMessageEmbeds(withContext(Dispatchers.IO) { getInfoEmbed(event.jda) }).queue {
                if (update) {
                    val timer = fixedRateTimer(
                        name = "NodeStatusDaemon#${mapping.size}",
                        daemon = true,
                        period = 300_000,
                        initialDelay = 300_000
                    ) { // hardcoded 5 minutes, probably wrong to use mapping.size
                        it.editMessageEmbeds(getInfoEmbed(event.jda))
                            .queue()
                    }
                    mapping[it.idLong] = timer
                }
            }
        } else {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString(CONFIG_PREFIX + "NotAdmin"))
                    .toEmbed(event.jda)
            )
        }
    }

    private fun getInfoEmbed(jda: JDA): MessageEmbed {
        val embedBuilder = EmbedBuilder(EmbedManager.getNodeStatus().toEmbed(jda))
        val fieldTemplate = embedBuilder.fields[0].also { embedBuilder.clearFields() }
        val pteroApplication = Common.getDefaultApplication()
        pteroApplication.retrieveNodes().forEach { node ->
            val connectionString = "${node.scheme}://${node.fqdn}:${node.daemonListenPort}"
            val connection = URL(connectionString).openConnection() as HttpURLConnection
            val status = try {
                if (connection.responseCode == 401) NodeStatus.ONLINE else NodeStatus.OFFLINE
            } catch (exception: Exception) {
                NodeStatus.OFFLINE
            }
            connection.disconnect()
            val field = MessageEmbed.Field(
                fieldTemplate.name?.replace("%nodeName" to node.name),
                fieldTemplate.value?.replace("%statusEmoji" to status.emoji, "%status" to status.message),
                fieldTemplate.isInline
            )
            embedBuilder.addField(field)
        }
        return embedBuilder.build()
    }
}
