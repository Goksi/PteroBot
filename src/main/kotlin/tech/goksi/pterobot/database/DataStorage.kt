package tech.goksi.pterobot.database

import tech.goksi.pterobot.entities.ApiKey

interface DataStorage {

    fun getApiKey(id: Long): ApiKey?

    fun link(id: Long, apiKey: ApiKey)

    fun unlink(id: Long)

    fun getRegisteredAccounts(id: Long): List<String>
}