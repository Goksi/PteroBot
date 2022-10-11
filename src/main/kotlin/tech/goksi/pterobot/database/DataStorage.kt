package tech.goksi.pterobot.database

import com.mattmalec.pterodactyl4j.client.entities.Account
import net.dv8tion.jda.api.entities.UserSnowflake

interface DataStorage {

    fun getApiKey(id: Long): String?

    fun getApiKey(snowflake: UserSnowflake): String? {
        return getApiKey(snowflake.idLong)
    }

    fun isPteroAdmin(id: Long): Boolean

    fun isPteroAdmin(snowflake: UserSnowflake): Boolean{
        return isPteroAdmin(snowflake.idLong)
    }

    fun link(snowflake: UserSnowflake, apiKey: String): Account

    fun unlink(id: Long)

    fun unlink(snowflake: UserSnowflake){
        unlink(snowflake.idLong)
    }

    fun isLinked(id: Long): Boolean

    fun isLinked(snowflake: UserSnowflake): Boolean{
        return isLinked(snowflake.idLong)
    }
}