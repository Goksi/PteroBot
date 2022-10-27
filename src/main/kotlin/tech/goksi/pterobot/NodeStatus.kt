package tech.goksi.pterobot

import tech.goksi.pterobot.manager.ConfigManager

private const val CONFIG_PREFIX = "Messages.Commands.NodeInfo."

enum class NodeStatus(val message: String, val emoji: String) {
    ONLINE("Online", ConfigManager.config.getString(CONFIG_PREFIX + "OnlineEmoji")),
    OFFLINE("Offline", ConfigManager.config.getString(CONFIG_PREFIX + "OfflineEmoji"));
}