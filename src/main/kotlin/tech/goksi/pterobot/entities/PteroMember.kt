package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.client.entities.Account
import com.mattmalec.pterodactyl4j.client.entities.ClientServer
import com.mattmalec.pterodactyl4j.client.entities.PteroClient
import net.dv8tion.jda.api.entities.UserSnowflake
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.database.impl.SQLiteImpl
import tech.goksi.pterobot.manager.ConfigManager
import tech.goksi.pterobot.util.Common
import tech.goksi.pterobot.util.await

class PteroMember(private val discordID: Long) {
    constructor(snowflake: UserSnowflake) : this(snowflake.idLong)

    companion object Data {
        private val data: DataStorage = SQLiteImpl()
    }

    private val apiKey: ApiKey? by lazy {
        data.getApiKey(discordID)
    }

    val client: PteroClient?
        get() = Common.createClient(apiKey?.key)

    val registeredAccounts: Set<String> by lazy {
        data.getRegisteredAccounts(discordID)
    }

    fun isPteroAdmin(): Boolean {
        return apiKey?.admin ?: false
    }

    fun link(key: ApiKey) {
        data.link(discordID, key)
    }

    fun unlink() {
        data.unlink(discordID)
    }

    fun isLinked(): Boolean {
        return apiKey != null
    }

    fun canRegisterMoreAccounts(): Boolean {
        val amount = ConfigManager.config.getInt("BotInfo.MaxRegisteredAccounts")
        return if (amount == 0) true
        else registeredAccounts.size < amount
    }

    fun registerAccount(accountName: String) = data.addRegisteredAccount(discordID, accountName)

    suspend fun getServers(): List<ClientServer> {
        return client!!.retrieveServers().await()
    }

    suspend fun getServerById(id: String): ClientServer {
        return client!!.retrieveServerByIdentifier(id).await()
    }

    suspend fun getAccount(): Account {
        return client!!.retrieveAccount().await()
    }
}
