package tech.goksi.pterobot.database.impl

import com.mattmalec.pterodactyl4j.client.entities.Account
import com.mattmalec.pterodactyl4j.exceptions.LoginException
import net.dv8tion.jda.api.entities.UserSnowflake
import org.slf4j.LoggerFactory
import tech.goksi.pterobot.database.DataStorage
import tech.goksi.pterobot.util.Common
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.system.exitProcess
/*TODO: test*/
class SQLiteImpl: DataStorage {
    private val connection: Connection
    private val logger = LoggerFactory.getLogger(DataStorage::class.java)

    init {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:database.db")
        val statement = connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS Keys(DiscordID BIGINT, ApiKey VARCHAR(48), isAdmin BOOLEAN)"
        )
        try{
            statement.executeUpdate()
        }catch (exception: SQLException){
            logger.error("Failed to initialize SQLite database... exiting", exception)
            exitProcess(1)
        }
    }

    override fun getApiKey(id: Long): String? {
        val statement = connection.prepareStatement(
            "SELECT ApiKey FROM Keys WHERE DiscordID = ?"
        )
        statement.setLong(1, id)
        try{
            val resultSet = statement.executeQuery()
            if(resultSet.next()) return resultSet.getString("ApiKey")
        }catch (exception: SQLException){
            logger.error("Failed to get api key for $id", exception)
        }
        return null
    }

    override fun isPteroAdmin(id: Long): Boolean {
        val statement = connection.prepareStatement(
            "SELECT isAdmin FROM Keys WHERE DiscordID = ?"
        )
        statement.setLong(1, id)
        try{
            val resultSet = statement.executeQuery()
            if(resultSet.next()) return resultSet.getBoolean("isAdmin")
        }catch (exception: SQLException){
            logger.error("Failed to get admin status of $id", exception)
        }
        return false
    }

    @Throws(LoginException::class, SQLException::class)
    override fun link(snowflake: UserSnowflake, apiKey: String): Account  {
        val statement = connection.prepareStatement(
            "INSERT INTO Keys (DiscordID, ApiKey, isAdmin) VALUES (?,?,?)"
        )
        val pteroUser = Common.createClient(apiKey).retrieveAccount().execute() //throws LoginException
        statement.setLong(1, snowflake.idLong)
        statement.setString(2, apiKey)
        statement.setBoolean(3, pteroUser.isRootAdmin)
        statement.executeUpdate() //should catch SQLException later in command
        return pteroUser
    }

    override fun unlink(id: Long) {
        val statement = connection.prepareStatement(
            "DELETE FROM Keys WHERE DiscordID = ?"
        )
        statement.setLong(1, id)
        try{
            statement.executeUpdate()
        }catch (exception: SQLException){
            logger.error("Failed to delete api key for $id", exception)
        }
    }

    override fun isLinked(id: Long): Boolean {
        val statement = connection.prepareStatement(
            "SELECT DiscordID FROM Keys WHERE DiscordID = ?"
        )
        statement.setLong(1, id)
        return try {
            val resultSet = statement.executeQuery()
            resultSet.next()
        } catch (exception: SQLException){
            logger.error("Failed to check linked status for $id", exception)
            false;
        }
    }
}