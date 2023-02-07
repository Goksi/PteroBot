package tech.goksi.pterobot.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.simpleyaml.configuration.file.YamlConfiguration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

object EmbedParser {

    fun parse(configString: String): MessageEmbed {
        val config = YamlConfiguration.loadConfigurationFromString(configString)
        val builder = EmbedBuilder()
        builder.setTitle(config.getString("title"))
        builder.setDescription(config.getString("description"))
        var currentSection = config.getConfigurationSection("author")
        builder.setAuthor(
            currentSection?.getString("name"),
            currentSection?.getString("url"),
            currentSection?.getString("icon_url")
        )
        builder.setColor(config.getInt("color"))
        builder.setThumbnail(config.getString("thumbnail"))
        builder.setImage(config.getString("image"))
        currentSection = config.getConfigurationSection("fields")
        if (currentSection != null) {
            val fields = currentSection.getKeys(false).map { currentSection.getConfigurationSection(it) }
            for (field in fields) {
                builder.addField(field.getString("name"), field.getString("value"), field.getBoolean("inline"))
            }
        }
        builder.setTimestamp(
            if (config.getString("timestamp") == null) null
            else OffsetDateTime.ofInstant(Instant.ofEpochMilli(config.getLong("timestamp")), ZoneOffset.UTC)
        )
        currentSection = config.getConfigurationSection("footer")
        builder.setFooter(currentSection.getString("text"), currentSection.getString("icon_url"))

        return builder.build()
    }
}
