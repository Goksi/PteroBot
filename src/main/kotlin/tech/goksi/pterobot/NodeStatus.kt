package tech.goksi.pterobot

import tech.goksi.pterobot.manager.ConfigManager

private const val CONFIG_PREFIX = "Messages.Commands.Node.Info."

enum class NodeStatus(val message: String, val emoji: String) {
    ONLINE("Online", ConfigManager.getString(CONFIG_PREFIX + "OnlineEmoji")),
    OFFLINE("Offline", ConfigManager.getString(CONFIG_PREFIX + "OfflineEmoji"));
}
