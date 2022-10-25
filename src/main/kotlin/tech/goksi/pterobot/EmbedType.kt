package tech.goksi.pterobot
import java.io.File.separator as SEP

enum class EmbedType(val path: String){
    GENERIC_ERROR("embeds/generic_error.json"),
    GENERIC_SUCCESS("embeds/generic_success.json"),
    NODE_INFO("embeds/node_info.json"),
    SERVERS_COMMAND("embeds/servers_command.json"),
    SERVER_INFO("embeds/server_info.json");


    override fun toString(): String {
        return path
    }

}