package tech.goksi.pterobot.database.impl

import dev.minn.jda.ktx.util.SLF4J
import tech.goksi.pterobot.database.ConnectionWrapper
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.entities.ApiKey
import java.sql.SQLException
import kotlin.system.exitProcess

class SQLiteImpl : DataStorage {
    private val connectionWrapper: ConnectionWrapper
    private val logger by SLF4J

    init {
        Class.forName("org.sqlite.JDBC")
        connectionWrapper = ConnectionWrapper.getConnection("SQLite", "database.db")
        val statement = connectionWrapper.connection.createStatement()
        statement.addBatch(
            "create table if not exists Members(id integer not null primary key autoincrement, discordID bigint not null unique, apiID integer, foreign key(apiID) references Keys(id) on delete set null)"
        )
        statement.addBatch(
            "create table if not exists Keys(id integer not null primary key autoincrement, \"key\" char(48) not null unique, \"admin\" boolean not null)"
        )
        statement.addBatch(
            "create table if not exists Accounts(memberID integer, username varchar(25), foreign key(memberID) references Members(id), primary key (memberID, username))"
        )
        statement.addBatch(
            "pragma foreign_keys = ON"
        )
        statement.use {
            try {
                it.executeBatch()
            } catch (exception: SQLException) {
                logger.error("Failed to initialize SQLite database... exiting", exception)
                exitProcess(1)
            }
        }
    }

    override fun getApiKey(id: Long): ApiKey? {
        return connectionWrapper.withConnection(
            query = "select Keys.key, Keys.admin from Keys inner join Members on Keys.id = Members.apiID where Members.discordID = ?",
            id
        ) {
            try {
                val resultSet = it.executeQuery()
                if (resultSet.next()) return ApiKey(resultSet.getString("key"), resultSet.getBoolean("admin"))
            } catch (exception: SQLException) {
                logger.error("Failed to get api key for $id", exception)
            }
            return null
        }
    }

    @Throws(SQLException::class)
    override fun link(id: Long, apiKey: ApiKey) {
        connectionWrapper.withConnection(
            query = "insert into Keys(\"key\", \"admin\") values (?, ?)",
            apiKey.key,
            apiKey.admin
        ) { it.executeUpdate() }

        /*TODO: probably should not relay on last_insert_rowid()*/
        connectionWrapper.withConnection(
            query = "insert into Members(discordID, apiID) values (?, last_insert_rowid()) on conflict(discordID) do update set apiID = last_insert_rowid()",
            id
        ) { it.executeUpdate() }
    }

    override fun unlink(id: Long) {
        connectionWrapper.withConnection(
            query = "delete from Keys where id in ( select apiID from Members where discordID = ? )",
            id
        ) {
            try {
                it.executeUpdate()
            } catch (exception: SQLException) {
                logger.error("Failed to delete api key for $id", exception)
            }
        }
    }

    override fun getRegisteredAccounts(id: Long): Set<String> {
        return connectionWrapper.withConnection(
            query = "select username from Accounts inner join Members on Accounts.memberID = Members.id where Members.discordID = ?",
            id
        ) {
            try {
                val resultSet = it.executeQuery()
                return buildSet {
                    while (resultSet.next())
                        this.add(resultSet.getString("username"))
                }
            } catch (exception: SQLException) {
                logger.error("Error while retrieving registered accounts of $id", exception)
                return emptySet()
            }
        }
    }

    override fun addRegisteredAccount(id: Long, accountName: String) {
        connectionWrapper.withConnection(
            query = "insert or ignore into Members(discordID) values (?)",
            id
        ) {
            try {
                it.executeUpdate()
            } catch (exception: SQLException) {
                logger.error("Error while adding member with discord id $id", exception)
            }
        }

        connectionWrapper.withConnection(
            query = "insert into Accounts(memberID, username) values ((select id from members where discordID = ?),?)",
            id,
            accountName
        ) {
            try {
                it.executeUpdate()
            } catch (exception: SQLException) {
                logger.error("Error while adding account to user with id $id", exception)
            }
        }
    }
}
