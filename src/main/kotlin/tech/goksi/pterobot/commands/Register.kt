package tech.goksi.pterobot.commands

import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Modal
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.replace
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Checks
import tech.goksi.pterobot.util.Common

private const val CONFIG_PREFIX = "Messages.Commands.Register."

class Register : SimpleCommand() {
    private val logger by SLF4J
    private val modal: Modal

    init {
        this.name = "register"
        this.description = ConfigManager.config.getString(CONFIG_PREFIX + "Description")
        this.enableDefault = false
        this.enabledPermissions = listOf(Permission.ADMINISTRATOR)
        SendDefaults.ephemeral = true
    }

    /*modal init*/
    init {
        val email = TextInput.create("email", "Email", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(9, 100)
            .setPlaceholder(ConfigManager.config.getString(CONFIG_PREFIX + "Modal.EmailPlaceholder")).build()
        val username = TextInput.create("username", "Username", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString(CONFIG_PREFIX + "Modal.UsernamePlaceholder")).build()
        val password = TextInput.create("password", "Password", TextInputStyle.SHORT)
            .setRequired(ConfigManager.config.getBoolean(CONFIG_PREFIX + "PasswordRequired")).setRequiredRange(7, 30)
            .setPlaceholder(ConfigManager.config.getString(CONFIG_PREFIX + "Modal.PasswordPlaceholder")).build()
        val firstName = TextInput.create("firstName", "First Name", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString(CONFIG_PREFIX + "Modal.FirstNamePlaceholder")).build()
        val lastName = TextInput.create("lastName", "Last Name", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString(CONFIG_PREFIX + "Modal.LastNamePlaceholder")).build()
        modal = Modal.create("pterobot:register", ConfigManager.config.getString(CONFIG_PREFIX + "Modal.Name"))
            .addActionRows(
                ActionRow.of(email),
                ActionRow.of(username),
                ActionRow.of(password),
                ActionRow.of(firstName),
                ActionRow.of(lastName)
            )
            .build()
    }

    override fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.user)
        if (pteroMember.canRegisterMoreAccounts()) {
            event.replyModal(modal).queue()
        } else {

        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "pterobot:register") return
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        val email = event.getValue("email")!!.asString
        if (!Checks.validEmail(email)) {
            event.hook.sendMessageEmbeds(
                listOf(
                    EmbedManager.getGenericFailure(
                        ConfigManager.config.getString(
                            CONFIG_PREFIX + "InvalidEmail"
                        )
                    ).toEmbed(event.jda)
                )
            )
                .queue()
            return
        }
        val pteroApplication = Common.getDefaultApplication()
        val userBuilder = pteroApplication.userManager.createUser().apply {
            setEmail(email)
            setUserName(event.getValue("username")!!.asString)
            setFirstName(event.getValue("firstName")!!.asString)
            setLastName(event.getValue("lastName")!!.asString)
            if (event.getValue("password") != null) {
                setPassword(event.getValue("password")!!.asString)
            }
        }
        userBuilder.executeAsync({
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericSuccess(
                    ConfigManager.config.getString(CONFIG_PREFIX + "Success")!!.replace("%pteroName" to it.userName)
                ).toEmbed(event.jda)
            )
                .complete()
        }, {
            val errorMessage = it.message ?: ""
            if (errorMessage.contains("Source:") && errorMessage.contains("taken")) {
                val takenField = errorMessage.substring(
                    errorMessage.indexOf("Source:") + 8,
                    errorMessage.indexOf(")")
                ) //lvl pro string extraction

                event.hook.sendMessageEmbeds(
                    EmbedManager.getGenericFailure(
                        ConfigManager.config.getString(CONFIG_PREFIX + "FieldTaken")
                            .replace("%takenField" to takenField)
                    ).toEmbed(event.jda)
                )
                    .complete()

            } else event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                    .toEmbed(event.jda)
            )
                .complete()
        })

    }
}