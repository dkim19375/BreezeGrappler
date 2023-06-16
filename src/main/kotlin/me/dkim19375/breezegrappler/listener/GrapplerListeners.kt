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

package me.dkim19375.breezegrappler.listener

import me.dkim19375.breezegrappler.BreezeGrappler
import me.dkim19375.breezegrappler.util.CustomGrappler
import me.dkim19375.breezegrappler.util.GrapplerSettings
import net.minecraft.world.entity.projectile.FishingHook
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftArmorStand
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftFishHook
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class GrapplerListeners(private val plugin: BreezeGrappler) : Listener {
    @EventHandler
    private fun PlayerInteractEvent.onInteract() {
        if (item?.itemMeta?.let { plugin.isGrappler(it) } != true) {
            return
        }
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            val hook = plugin.currentlyGrappling[player.uniqueId]?.takeIf { hook ->
                hook.currentState != FishingHook.FishHookState.FLYING || hook.onGround()
            } ?: return
            if (!hook.isAlive) return
            val bukkitLoc = hook.position().let { Location(player.world, it.x, it.y, it.z) }
            val diff = bukkitLoc.clone().subtract(player.location.clone()).toVector()
            diff.y = sign(diff.y) * abs(diff.y).pow(GrapplerSettings.Y_POWER)
            player.velocity = diff.multiply(GrapplerSettings.MULTIPLIER).multiply(
                Vector(1.0, GrapplerSettings.Y_MULTIPLIER, 1.0)
            ).add(
                Vector(0.0, GrapplerSettings.Y_ADD, 0.0)
            )
            plugin.mainConfig.get().sounds.grappleHook.playSound(player)
            return
        }
        val hook = CustomGrappler(player, plugin)
        plugin.mainConfig.get().sounds.throwHook.playSound(player)
        hook.spawn()
    }

    @EventHandler
    private fun ProjectileHitEvent.onHit() {
        val hook = entity as? CraftFishHook ?: return
        val grappling = hook.handle as? CustomGrappler ?: return
        val player = grappling.player
        if (plugin.currentlyGrappling[player.uniqueId] !== grappling) {
            return
        }
        val hitBlock = hitBlock ?: return
        if (grappling.attachToEntity != null) {
            return
        }
        val blockLoc = hitBlock.location.clone().add(0.5, 0.5, 0.5)
        val hitBlockLoc = hitBlock.getRelative(hitBlockFace ?: return).location.clone().add(0.5, 0.5, 0.5)
        val between = hitBlockLoc.clone().add(blockLoc.clone().subtract(hitBlockLoc).multiply(0.4))
        val armorStand = hitBlock.world.spawn(between, ArmorStand::class.java) { stand ->
            stand.isInvisible = true
            stand.isInvulnerable = true
            stand.isMarker = true
            stand.isSmall = true
            stand.isVisible = false
            EquipmentSlot.values().forEach { stand.addEquipmentLock(it, ArmorStand.LockType.ADDING_OR_CHANGING) }
            stand.setCanMove(false)
            stand.setCanTick(false)
            stand.setGravity(false)
            stand.isCollidable = false
        }
        grappling.attachToEntity = armorStand.uniqueId
        grappling.currentState = FishingHook.FishHookState.HOOKED_IN_ENTITY
        grappling.setHookedEntity((armorStand as CraftArmorStand).handle)
    }

    @EventHandler
    private fun ProjectileLaunchEvent.onLaunch() {
        val hook = entity as? CraftFishHook ?: return
        if (hook.handle !is CustomGrappler) return
        hook.velocity = hook.velocity.multiply(GrapplerSettings.ROD_FORCE)
    }
}