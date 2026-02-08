package me.sbi.modules.skyblock

import me.sbi.SkyblockImproved
import me.sbi.modules.render.EspModule
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Scans for party member players within chunk radius.
 * Matches player names against SkyBlockParty (from /p l).
 */
object PartyMemberScan {

    data class PartyMemberInfo(val box: AABB, val name: String, val labelPos: Vec3, val distance: Double)

    /** Boxes and labels for party members */
    val partyMemberInfos: List<PartyMemberInfo>
        get() {
            val mc = SkyblockImproved.mc
            val level = mc.level ?: return emptyList()
            val localPlayer = mc.player ?: return emptyList()
            if (!EspModule.enabled || !EspModule.partyMembers.value) return emptyList()
            if (!SkyBlockParty.hasParty) return emptyList()

            val partyNames = SkyBlockParty.allMembers.map { it.name }.toSet()
            if (partyNames.isEmpty()) return emptyList()

            val chunkRadius = EspModule.partyChunkRadius.value.toInt().coerceIn(1, 12)
            val blockRadius = chunkRadius * 16.0
            val playerPos = localPlayer.position()
            val searchBox = AABB(playerPos, playerPos).inflate(blockRadius)

            val result = mutableListOf<PartyMemberInfo>()
            for (entity in level.getEntitiesOfClass(Player::class.java, searchBox)) {
                if (entity == localPlayer) continue
                val name = entity.gameProfile?.name ?: entity.name?.string ?: continue
                if (!partyNames.any { it.equals(name, ignoreCase = true) }) continue
                val labelPos = Vec3(
                    entity.position().x,
                    entity.boundingBox.maxY + 0.35,
                    entity.position().z
                )
                val dist = entity.position().distanceTo(localPlayer.position())
                result.add(PartyMemberInfo(entity.boundingBox, name, labelPos, dist))
            }
            return result
        }

    /** AABBs for party members to render ESP on */
    val partyMemberBoxes: List<AABB>
        get() = partyMemberInfos.map { it.box }
}
