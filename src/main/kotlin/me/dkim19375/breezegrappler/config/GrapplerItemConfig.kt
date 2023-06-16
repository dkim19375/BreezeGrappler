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

package me.dkim19375.breezegrappler.config

import kotlinx.serialization.Serializable
import me.dkim19375.breezegrappler.BreezeGrappler
import me.dkim19375.breezegrappler.util.removeDefaultItalics
import me.dkim19375.dkimbukkitcore.function.applyPAPI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Serializable
data class GrapplerItemConfig(
    val name: String = "<dark_red><bold>Grappler",
    val lore: List<String> = listOf(""),
) {
    fun toItemStack(plugin: BreezeGrappler, player: Player? = null): ItemStack = ItemStack(Material.LEAD).apply {
        val miniMessage = MiniMessage.miniMessage()
        itemMeta = itemMeta?.apply {
            displayName(miniMessage.deserialize(name.applyPAPI(player)).removeDefaultItalics())
            lore(this@GrapplerItemConfig.lore.map {
                miniMessage.deserialize(it.applyPAPI(player)).removeDefaultItalics()
            })
            plugin.applyGrappler(this)
        }
    }
}