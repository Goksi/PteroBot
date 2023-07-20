package tech.goksi.pterobot.entities

import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.simpleyaml.configuration.serialization.ConfigurationSerializable

data class ButtonInfo(val label: String, val style: ButtonStyle, val emoji: Emoji) : ConfigurationSerializable {

    companion object {
        @JvmStatic
        fun deserialize(map: MutableMap<String, Any>): ButtonInfo {
            return ButtonInfo(
                map["label"] as String,
                ButtonStyle.valueOf(map["style"] as String),
                Emoji.fromUnicode(map["Emoji"] as String)
            )
        }
    }

    override fun serialize(): MutableMap<String, Any> {
        val map: MutableMap<String, Any> = HashMap()
        map["label"] = label
        map["style"] = style.toString()
        map["emoji"] = emoji.formatted // check
        return map
    }
}
