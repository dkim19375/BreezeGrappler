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

package me.dkim19375.breezegrappler

import com.charleskorn.kaml.Yaml
import me.dkim19375.breezegrappler.command.BreezeGrapplerCommand
import me.dkim19375.breezegrappler.config.BreezeGrapplerConfig
import me.dkim19375.breezegrappler.listener.GrapplerListeners
import me.dkim19375.breezegrappler.util.CustomGrappler
import me.dkim19375.dkimbukkitcore.javaplugin.CoreJavaPlugin
import me.dkim19375.dkimcore.file.DataFile
import me.dkim19375.dkimcore.file.KotlinxFile
import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

class BreezeGrappler : CoreJavaPlugin() {
    private val leadKey by lazy { NamespacedKey(this, "grappler_lead") }
    val currentlyGrappling = mutableMapOf<UUID, CustomGrappler>()

    val mainConfig by lazy {
        KotlinxFile(
            type = BreezeGrapplerConfig::class,
            format = Yaml.default,
            serializer = BreezeGrapplerConfig.serializer(),
            file = dataFolder.resolve("config.yml"),
        )
    }

    override fun onEnable() {
        registerConfig(mainConfig)
        registerCommand("breezegrappler", BreezeGrapplerCommand(this))
        registerListener(GrapplerListeners(this))
    }

    override fun onDisable() {
        super.onDisable()
        currentlyGrappling.values.forEach { grappler ->
            grappler.kill()
        }
    }

    override fun reloadConfig() {
        super.reloadConfig()
        getRegisteredFiles().forEach(DataFile::save)
    }

    fun applyGrappler(item: ItemMeta): ItemMeta = item.apply {
        persistentDataContainer.set(leadKey, PersistentDataType.BYTE, 1)
    }

    fun isGrappler(item: ItemMeta): Boolean = item.persistentDataContainer.has(leadKey, PersistentDataType.BYTE)

}