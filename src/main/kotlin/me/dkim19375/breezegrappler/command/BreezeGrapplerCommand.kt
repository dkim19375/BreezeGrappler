/*
 *     BreezeGrappler, a plugin that can make a lead turn into a grappling hook
 *     Copyright (C) 2023  dkim19375
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.dkim19375.breezegrappler.command

import me.dkim19375.breezegrappler.BreezeGrappler
import me.dkim19375.dkimbukkitcore.function.applyPAPI
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.Component.textOfChildren
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

class BreezeGrapplerCommand(private val plugin: BreezeGrappler) : CommandExecutor {
    private val lastUsed = mutableMapOf<UUID, Long>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.getOrNull(0)?.equals("reload", true) == true) {
            if (sender.hasPermission("breezegrappler.reload")) {
                plugin.reloadConfig()
                sender.sendMessage(text("Reloaded config file!", GREEN))
                return true
            }
        }
        if (sender !is Player) {
            sender.sendMessage(text("You must be a player in order to get a grappling hook!", RED))
            return true
        }
        val timePassed = System.currentTimeMillis() - (lastUsed[sender.uniqueId] ?: 0)
        val min = plugin.mainConfig.get().commandCooldown * 1000L
        if (timePassed < min && !sender.hasPermission("breezegrappler.cooldown.bypass")) {
            sender.sendMessage(
                textOfChildren(
                    text("You must wait ", RED),
                    text("${((min - timePassed) / 1000L)}s ", GOLD),
                    text("before getting another grappling hook!", RED),
                )
            )
            return true
        }
        lastUsed[sender.uniqueId] = System.currentTimeMillis()
        val item = plugin.mainConfig.get().item.toItemStack(plugin, sender)
        sender.inventory.addItem(item)
        for (line in plugin.mainConfig.get().message) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize(line.applyPAPI(sender)))
        }
        plugin.mainConfig.get().sounds.getHook.playSound(sender)
        return true
    }
}