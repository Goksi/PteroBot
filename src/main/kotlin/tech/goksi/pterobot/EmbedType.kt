package tech.goksi.pterobot

enum class EmbedType(private val fileName: String) {
    GENERIC_ERROR("generic_error.yml"),
    GENERIC_SUCCESS("generic_success.yml"),
    NODE_INFO("node_info.yml"),
    SERVERS_COMMAND("server_list.yml"),
    SERVER_CREATE("server_create.yml"),
    SERVER_INFO("server_info.yml"),
    ACCOUNT_INFO("account_info.yml"),
    NODE_STATUS("node_status.yml");

    val path
        get() = "embeds/$fileName"
}
