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

package me.dkim19375.breezegrappler.util

import me.dkim19375.breezegrappler.BreezeGrappler
import me.dkim19375.dkimbukkitcore.function.getEntity
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.craftbukkit.v1_20_R1.CraftWorld
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sign

class CustomGrappler(
    val player: Player,
    private val plugin: BreezeGrappler,
) : FishingHook((player as CraftPlayer).handle, (player.world as CraftWorld).handle, 0, 0) {

    private var originalWorld = player.world.name
    var attachToEntity: UUID? = null

    private val synchronizedRandom = RandomSource.create()
    private var life = 0
    private var openWater = true

    override fun isOpenWaterFishing(): Boolean = openWater

    fun spawn() {
        (player.world as CraftWorld).addEntityToWorld(this, CreatureSpawnEvent.SpawnReason.CUSTOM)
        plugin.currentlyGrappling[player.uniqueId]?.kill()
        plugin.currentlyGrappling[player.uniqueId] = this
    }

    override fun kill() {
        if (!isAlive) {
            return
        }
        super.discard()
    }

    override fun remove(reason: RemovalReason) {
        if (isRemoved) {
            return
        }
        super.remove(reason)
        plugin.currentlyGrappling[player.uniqueId]?.let {
            if (it === this) {
                plugin.currentlyGrappling.remove(player.uniqueId)
            }
        }
        attachToEntity?.getEntity()?.remove()
    }

    override fun tick() {
        // vanilla
        synchronizedRandom.setSeed(getUUID().leastSignificantBits xor level().gameTime)
        super.baseTick()

        if (!level().isClientSide && shouldStopFishing()) {
            return
        }
        if (onGround()) {
            if (++this.life >= 1200) {
                kill()
                return
            }
        } else {
            this.life = 0
        }
        var fluidHeight = 0.0f
        val blockPosition = blockPosition()
        val fluid = level().getFluidState(blockPosition)
        if (fluid.`is`(FluidTags.WATER)) {
            fluidHeight = fluid.getHeight(level(), blockPosition)
        }
        val flag = fluidHeight > 0.0f
        when (currentState) {
            FishHookState.FLYING -> {
                if (hookedIn != null) {
                    deltaMovement = Vec3.ZERO
                    currentState = FishHookState.HOOKED_IN_ENTITY
                    return
                }
                plugin.mainConfig.get().particles.spawnParticles(position().let {
                    Location(player.world, it.x, it.y, it.z)
                })
                if (flag) {
                    deltaMovement = deltaMovement.multiply(0.3, 0.2, 0.3)
                    currentState = FishHookState.BOBBING
                    return
                }
                checkCollision()
            }

            FishHookState.HOOKED_IN_ENTITY -> {
                val hookedIn = hookedIn
                if (hookedIn != null && !hookedIn.isRemoved && hookedIn.level().dimension() === level().dimension()) {
                    if (hookedIn.uuid == attachToEntity) {
                        //this.setPos(hookedIn.x, hookedIn.getY(0.8), hookedIn.z)
                        this.setPos(hookedIn.x, hookedIn.y, hookedIn.z)
                        return
                    }
                }
                setHookedEntity(null)
                currentState = FishHookState.FLYING
                return
            }

            else -> {
                val velocity = deltaMovement
                var d0 = this.y + velocity.y - blockPosition.y.toDouble() - fluidHeight.toDouble()
                if (abs(d0) < 0.01) {
                    d0 += sign(d0) * 0.1
                }
                this.setDeltaMovement(
                    velocity.x * 0.9,
                    velocity.y - d0 * random.nextFloat().toDouble() * 0.2,
                    velocity.z * 0.9
                )
            }
        }
        if (!fluid.`is`(FluidTags.WATER)) {
            deltaMovement = deltaMovement.add(0.0, -0.03, 0.0)
        }
        move(MoverType.SELF, deltaMovement)
        updateRotation()
        if (currentState == FishHookState.FLYING && (onGround() || horizontalCollision)) {
            deltaMovement = Vec3.ZERO
        }
        deltaMovement = deltaMovement.scale(0.92)
        reapplyPosition()
    }

    private fun checkCollision() =
        preOnHit(
            ProjectileUtil.getHitResultOnMoveVector(
                this,
                this::canHitEntity
            )
        ) // CraftBukkit - projectile hit event

    private fun shouldStopFishing(): Boolean {
        if (!player.isOnline || !player.isValid || player.world.name != originalWorld) {
            kill()
            return true
        }
        if (player.inventory.itemInMainHand.itemMeta?.let(plugin::isGrappler) != true) {
            kill()
            return true
        }
        return false
    }
}