package tech.goksi.pterobot.database

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

    fun link(snowflake: UserSnowflake, apiKey: String)

    fun unlink(id: Long)

    fun unlink(snowflake: UserSnowflake){
        unlink(snowflake.idLong)
    }
}