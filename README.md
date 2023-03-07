# PteroBot

PteroBot is simple discord bot utilizing pterodactyl api made using [JDA](https://github.com/DV8FromTheWorld/JDA)
and [Pterodactyl4J](https://github.com/mattmalec/Pterodactyl4J).

## First time running

You run this bot like any other Java application, simple `java -jar PteroBot.jar`.  
On first startup you will be asked for some basic information that bot requires to function normally.

![Setup](https://media.discordapp.net/attachments/976766831182368768/1033771478870663228/unknown.png)

## Features

- Fully customizable (all messages and embeds)
- Specific node info
- Status of all nodes
- Discord and pterodactyl account linking
- Account info
- Managing (sort of) servers over discord
- Panel registration

### Customizing

Templates of all embeds used by the bot are present in `/embeds/` directory and are in yaml format.  
Example of node info embed:

```yaml
title: Node info
description: "%name\n%description"
color: 39129
author:
  name: PteroBot
fields:
  f1:
    name: Status
    value: %statusEmoji %status
    inline: true
  f2:
    name: Running servers
    value: %runningServers
    inline: false
  f3:
    name: Location
    value: %location
    inline: true
  f4:
    name: Maintenance mode
    value: %maintenance
    inline: true
  f5:
    name: Allocations
    value: %allocationsCount
    inline: true
  f6:
    name: CPU usage
    value: %cpuUsed %
    inline: false
  f7:
    name: Disk usage
    value: %diskUsed GB / %diskLimit GB
    inline: false
  f8:
    name: Memory usage
    value: %ramUsed MB %memoryUsageBar %ramLimit MB
    inline: false
footer:
  text: PteroBot | Node
  icon_url: https://images.g2crowd.com/uploads/product/image/social_landscape/social_landscape_9f7bed1018bc7ad75c94da92c83c76de/pterodactyl-panel.png
timestamp: %timestamp
```

### Specific node info

![Node Info](/imgs/node_info.png)  
It also comes with an option to automatically update message every 5 minutes.
> **Warning**
> Be careful while using auto update option, bot might get rate-limited (Discord API being very strict for editing
> messages)

### Nodes status

![Node Status](https://cdn.discordapp.com/attachments/976766831182368768/1036448248669425684/unknown.png)
> **Warning**
> Same as with node info, there is option for auto update, be careful

### Managing servers

![Server list](/imgs/server_list.png) ![Server manage](/imgs/server_manage.png)

By default, every member can *manage* servers with their own api key !

### Registration

![Registration](https://cdn.discordapp.com/attachments/976766831182368768/1033776792898642021/unknown.png)  
By default, `/register` command is admin only, you can enable it to everyone by setting `EnabledRegistration` config
option to true
