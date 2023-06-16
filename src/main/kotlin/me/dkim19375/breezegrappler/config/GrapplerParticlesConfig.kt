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

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions

@Serializable
data class GrapplerParticlesConfig(
    val particle: Particle = Particle.REDSTONE,
    val amount: Int = 1,
    @YamlComment("Only for particle \"REDSTONE\"")
    val color: String = "#FF0000",
    @YamlComment("Only for particle \"REDSTONE\"")
    var size: Float = 1f,
) {
    fun spawnParticles(location: Location) {
        if (particle == Particle.REDSTONE) {
            val awtColor = java.awt.Color.decode(color)
            location.world.spawnParticle(
                particle, location, amount, DustOptions(
                    Color.fromRGB(
                        awtColor.red,
                        awtColor.green,
                        awtColor.blue
                    ), size
                )
            )
            return
        }
        location.world.spawnParticle(particle, location, amount)
    }
}