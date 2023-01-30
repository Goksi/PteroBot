package tech.goksi.pterobot.commands

import com.mattmalec.pterodactyl4j.exceptions.HttpException
import dev.minn.jda.ktx.messages.SendDefaults
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.util.SLF4J
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import tech.goksi.pterobot.commands.manager.abs.SimpleCommand
import tech.goksi.pterobot.entities.ApiKey
import tech.goksi.pterobot.entities.PteroMember
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.manager.EmbedManager
import tech.goksi.pterobot.manager.EmbedManager.toEmbed
import tech.goksi.pterobot.util.Checks
import tech.goksi.pterobot.util.Common
import java.sql.SQLException

private const val ACCOUNT_PREFIX = "Messages.Commands.Account"

class AccountCommand : SimpleCommand(
    name = "account",
    description = "Main account command, have no influence",
    subcommands = listOf(Link(), Unlink(), Register())
) {

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        // Base command
    }
}

private class Link : SimpleCommand(
    name = "link",
    description = ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.Description"),
    options = listOf(
        OptionData(
            OptionType.STRING,
            "apikey",
            ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.OptionDescription"),
            true
        )
    )
) {
    private val logger by SLF4J

    init {
        SendDefaults.ephemeral = true
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val key = event.getOption("apikey")!!.asString
        val response: MessageEmbed
        val pteroMember = PteroMember(event.user)
        if (!pteroMember.isLinked()) {
            response = try {
                if (!Checks.validClientKey(key)) throw HttpException("Wrong key format !")
                val pteroAccount = Common.createClient(key)!!
                val account = pteroAccount.retrieveAccount().execute()
                pteroMember.link(ApiKey(key, account.isRootAdmin))
                logger.info("User ${event.user.asTag} linked his discord with ${account.userName} pterodactyl account !")
                EmbedManager.getGenericSuccess(
                    ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkSuccess")
                        .replace("%pteroName", account.userName)
                )
                    .toEmbed(event.jda)
            } catch (exception: SQLException) {
                logger.error("Failed to link ${event.user.idLong}", exception)
                EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                    .toEmbed(event.jda)
            } catch (httpException: HttpException) {
                EmbedManager.getGenericFailure(ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkWrongKey"))
                    .toEmbed(event.jda) // its probably wrong key if we got here, add check maybe
            }
        } else {
            response =
                EmbedManager.getGenericFailure(ConfigManager.config.getString("$ACCOUNT_PREFIX.Link.LinkExist"))
                    .toEmbed(event.jda)
        }
        event.reply_(embeds = listOf(response)).queue()
    }
}

private class Unlink : SimpleCommand(
    name = "unlink",
    description = ConfigManager.config.getString("$ACCOUNT_PREFIX.Unlink.Description")
) {

    init {
        SendDefaults.ephemeral = true
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        val pteroMember = PteroMember(event.user)
        if (pteroMember.isLinked()) {
            pteroMember.unlink()
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericSuccess(ConfigManager.config.getString("$ACCOUNT_PREFIX.Unlink.SuccessUnlink"))
                        .toEmbed(event.jda)
                )
            ).queue()
        } else {
            event.reply_(
                embeds = listOf(
                    EmbedManager
                        .getGenericFailure(ConfigManager.config.getString("$ACCOUNT_PREFIX.Unlink.NotLinked"))
                        .toEmbed(event.jda)
                )
            ).queue()
        }
    }
}

/*TODO: edit README*/
private class Register : SimpleCommand(
    name = "register",
    description = ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Description")
) {
    private val modal: Modal

    init {
        SendDefaults.ephemeral = true
    }

    init {
        val email = TextInput.create("email", "Email", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(9, 100)
            .setPlaceholder(ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.EmailPlaceholder")).build()
        val username = TextInput.create("username", "Username", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.UsernamePlaceholder"))
            .build()
        val password = TextInput.create("password", "Password", TextInputStyle.SHORT)
            .setRequired(ConfigManager.config.getBoolean("$ACCOUNT_PREFIX.Register.PasswordRequired"))
            .setRequiredRange(7, 30)
            .setPlaceholder(ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.PasswordPlaceholder"))
            .build()
        val firstName = TextInput.create("firstName", "First Name", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.FirstNamePlaceholder"))
            .build()
        val lastName = TextInput.create("lastName", "Last Name", TextInputStyle.SHORT)
            .setRequired(true).setRequiredRange(3, 25)
            .setPlaceholder(ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.LastNamePlaceholder"))
            .build()
        modal = Modal.create("pterobot:register", ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Modal.Name"))
            .addActionRows(
                ActionRow.of(email),
                ActionRow.of(username),
                ActionRow.of(password),
                ActionRow.of(firstName),
                ActionRow.of(lastName)
            )
            .build()
    }

    override suspend fun execute(event: SlashCommandInteractionEvent) {
        if (!ConfigManager.config.getBoolean("BotInfo.EnabledRegistration") &&
            !event.member!!.hasPermission(Permission.ADMINISTRATOR)
        ) {
            event.replyEmbeds(
                EmbedManager.getGenericFailure("$ACCOUNT_PREFIX.Register.DisabledMessage").toEmbed(event.jda)
            ).queue()
            return
        }
        val pteroMember = PteroMember(event.user)
        if (pteroMember.canRegisterMoreAccounts()) {
            event.replyModal(modal).queue()
        } else {
            event.replyEmbeds(
                EmbedManager.getGenericFailure(
                    ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.LimitReached")
                        .replace("%accounts", pteroMember.registeredAccounts.joinToString(","))
                ).toEmbed(event.jda)
            ).setEphemeral(true).queue()
        }
    }

    override suspend fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "pterobot:register") return
        event.deferReply(ConfigManager.config.getBoolean("BotInfo.Ephemeral")).queue()
        val email = event.getValue("email")!!.asString
        if (!Checks.validEmail(email)) {
            event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(
                    ConfigManager.config.getString(
                        "$ACCOUNT_PREFIX.Register.InvalidEmail"
                    )
                ).toEmbed(event.jda)
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
                    ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.Success")!!
                        .replace("%pteroName", it.userName)
                ).toEmbed(event.jda)
            ).queue()
            PteroMember(event.user).registerAccount(it.userName)
        }, {
            val errorMessage = it.message ?: ""
            if (errorMessage.contains("Source:") && errorMessage.contains("taken")) {
                val takenField = errorMessage.substring(
                    errorMessage.indexOf("Source:") + 8,
                    errorMessage.indexOf(")")
                ) // lvl pro string extraction
                event.hook.sendMessageEmbeds(
                    EmbedManager.getGenericFailure(
                        ConfigManager.config.getString("$ACCOUNT_PREFIX.Register.FieldTaken")
                            .replace("%takenField", takenField)
                    ).toEmbed(event.jda)
                ).queue()
            } else event.hook.sendMessageEmbeds(
                EmbedManager.getGenericFailure(ConfigManager.config.getString("Messages.Embeds.UnexpectedError"))
                    .toEmbed(event.jda)
            ).queue()
        })
    }
}
