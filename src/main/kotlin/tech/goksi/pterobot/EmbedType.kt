package tech.goksi.pterobot

enum class EmbedType(private val fileName: String) {
    GENERIC_ERROR("generic_error.json"),
    GENERIC_SUCCESS("generic_success.json"),
    NODE_INFO("node_info.json"),
    SERVERS_COMMAND("servers_command.json"),
    SERVER_INFO("server_info.json"),
    NODE_STATUS("node_status.json");

    val path
        get() = "embeds/$fileName"
}
