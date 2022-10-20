package tech.goksi.pterobot
import java.io.File.separator as SEPARATOR

enum class EmbedType(val path: String){
    GENERIC_ERROR("embeds${SEPARATOR}generic_error.json"),
    GENERIC_SUCCESS("embeds${SEPARATOR}generic_success.json"),
    NODE_INFO("embeds${SEPARATOR}node_info.json"),
    SERVERS_COMMAND("embeds${SEPARATOR}servers_command.json"),
    SERVER_INFO("embeds${SEPARATOR}server_info.json");


    override fun toString(): String {
        return path
    }

}