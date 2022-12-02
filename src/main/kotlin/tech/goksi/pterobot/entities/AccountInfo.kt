package tech.goksi.pterobot.entities

import com.mattmalec.pterodactyl4j.client.entities.Account

@Suppress("unused")
data class AccountInfo(private val account: Account) {
    val firstName: String = account.firstName ?: ""
    val lastName: String = account.lastName ?: ""
    val fullName: String = account.fullName ?: ""
    val username: String = account.userName
    val rootAdmin = account.isRootAdmin
    val email: String = account.email
    val id = account.id
}
