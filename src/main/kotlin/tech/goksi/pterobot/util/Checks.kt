package tech.goksi.pterobot.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Checks  {
    val logger: Logger = LoggerFactory.getLogger(Checks::class.java)

    fun arguments(expression: Boolean, message: String) {
        if(!expression) throw IllegalArgumentException(message)
    }


}