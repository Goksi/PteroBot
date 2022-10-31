# PteroBot
PteroBot is simple discord bot utilizing pterodactyl api made using [JDA](https://github.com/DV8FromTheWorld/JDA) and [Pterodactyl4J](https://github.com/mattmalec/Pterodactyl4J).
## First time running
You run this bot like any other Java application, simple `java -jar PteroBot.jar`.  
On first startup you will be asked for some basic information that bot requires to function normally.


![Setup](https://media.discordapp.net/attachments/976766831182368768/1033771478870663228/unknown.png)
## Features
- Fully customizable (all messages and embeds)
- Specific node info
- Status of all nodes
- Discord and pterodactyl account linking
- Managing (sort of) servers over discord
- Panel registration

### Customizing
Templates of all embeds used by the bot are present in `/embeds/` directory and are in json format.  
Example of server info embed:
```json
{
  "type": "IMAGE",
  "color": 39129,
  "author": {
    "icon_url": "",
    "name": ""
  },
  "footer": {
    "icon_url": "https://images.g2crowd.com/uploads/product/image/social_landscape/social_landscape_9f7bed1018bc7ad75c94da92c83c76de/pterodactyl-panel.png",
    "text": "PteroBot | %serverId"
  },
  "timestamp": "%timestamp",
  "title": "PteroBot",
  "description": "%serverName",
  "fields": [
    {
      "name": "Status",
      "value": "%statusEmoji %status",
      "inline": true
    },
    {
      "name": "Node",
      "value": "%nodeName",
      "inline": true
    },
    {
      "name": "Allocation",
      "value": "%primaryAllocation",
      "inline": false
    },
    {
      "name": "CPU usage",
      "value": "%cpuUsed %",
      "inline": true
    },
    {
      "name": "Disk usage",
      "value": "%diskUsed GB / %diskMax GB",
      "inline": true
    },
    {
      "name": "Memory usage",
      "value": "%usedMb MB %memoryUsageBar %maxMb MB",
      "inline": false
    }
  ]
}
```
For creating embeds, you can use site like [Embed Builder](https://glitchii.github.io/embedbuilder/?editor=json) with little customization (Removing embeds array and adding `type` property)
### Specific node info
![Node Info](https://cdn.discordapp.com/attachments/976766831182368768/1033774324743667752/unknown.png)  
It also comes with an option to automatically update message every 5 minutes.
> **Warning**
> Be careful while using auto update option, bot might get rate-limited (Discord API being very strict for editing messages)
### Nodes status
![Node Status](https://cdn.discordapp.com/attachments/976766831182368768/1036448248669425684/unknown.png)
> **Warning**
> Same as with node info, there is option for auto update, be careful
### Managing servers
<pre>
<img src = "https://cdn.discordapp.com/attachments/976766831182368768/1033775952129437799/unknown.png"></img>   <img src = "https://cdn.discordapp.com/attachments/976766831182368768/1033776116495822928/unknown.png"></img>
</pre>
By default, every member can *manage* servers with their own api key !
### Registration
![Registration](https://cdn.discordapp.com/attachments/976766831182368768/1033776792898642021/unknown.png)  
By default, `/register` command is admin only, you can enable it to everyone by going into `Server Settings -> Integrations -> Your Bot` and enable command for everyone
