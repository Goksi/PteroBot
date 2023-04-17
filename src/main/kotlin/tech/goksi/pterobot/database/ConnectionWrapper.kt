package tech.goksi.pterobot.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

class ConnectionWrapper private constructor(val connection: Connection) {

    companion object {
        private val drivers = mapOf("SQLite" to "jdbc:sqlite")

        fun getConnection(driver: String, url: String): ConnectionWrapper {
            return ConnectionWrapper(DriverManager.getConnection("${drivers[driver]}:$url"))
        }
    }

    inline fun <reified T> withConnection(
        query: String,
        vararg params: Any,
        body: (statement: PreparedStatement) -> T
    ): T {
        val statement = connection.prepareStatement(query)
        for (i in 1..params.size) {
            statement.setObject(i, params[i - 1])
        }
        return body(statement).also { statement.close() }
    }
}
