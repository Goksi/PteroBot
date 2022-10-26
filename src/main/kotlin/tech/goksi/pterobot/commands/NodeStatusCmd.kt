package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.exceptions.HttpException
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import tech.goksi.pterobot.NodeStatus
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.replace
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Common
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val CONFIG_PREFIX = "Messages.Commands.NodeStatus."
class NodeStatusCmd: SimpleCommand() {

    init {
        this.name = "nodestatus"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        this.options = listOf(OptionData(OptionType.BOOLEAN, "update", ConfigManager.config.getString(CONFIG_PREFIX + "OptionUpdateDescription"), false))
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        val update = event.getOption("update")?.asBoolean ?: false
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        val embedBuilder = EmbedBuilder(EmbedManager.getNodeStatus().toEmbed(event.jda))
        val fieldTemplate = embedBuilder.fields[0].also { embedBuilder.clearFields() }
        val pteroApplication = Common.getDefaultApplication()
        pteroApplication.retrieveNodes().forEach { node ->
            val connectionString = "${node.scheme}://${node.fqdn}:${node.daemonListenPort}"
            val connection = URL(connectionString).openConnection() as HttpURLConnection
            val status = try{
                if(connection.responseCode == 401) NodeStatus.ONLINE else NodeStatus.OFFLINE
            }catch (exception: Exception){
                NodeStatus.OFFLINE
            }
            connection.disconnect()
            val field = MessageEmbed.Field(fieldTemplate.name?.replace("%nodeName" to node.name),
                fieldTemplate.value?.replace("%statusEmoji" to status.emoji, "%status" to status.message), fieldTemplate.isInline)
            embedBuilder.addField(field)
        }
        event.hook.sendMessageEmbeds(embedBuilder.build()).queue() //update
    }
}