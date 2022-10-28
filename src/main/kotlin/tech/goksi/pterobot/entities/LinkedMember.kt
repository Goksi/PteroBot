package tech.goksi.pterobot.entities

import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.database.impl.SQLiteImpl
import tech.goksi.pterobot.manager.ConfigManager

class LinkedMember(val discordID: Long) {


    companion object Data {
        private val data: DataStorage = SQLiteImpl()
    }

    val apiKey: ApiKey?
        get() {
            TODO()
        }

    val registeredAccounts: Set<String>
        get() {
            TODO()
        }

    fun canRegisterMoreAccounts(): Boolean {
        val amount = ConfigManager.config.getInt("BotInfo.MaxRegisteredAccounts")
        return if(amount == 0) true
        else registeredAccounts.size < amount
    }
}