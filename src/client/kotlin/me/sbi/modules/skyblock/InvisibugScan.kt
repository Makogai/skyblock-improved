package me.sbi.modules.skyblock

import me.sbi.SkyblockImproved
import me.sbi.modules.render.EspModule
import me.sbi.util.SbiMessage
import net.minecraft.client.resources.language.I18n
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.io.File

/**
 * Invisibug ESP in Galatea. Locations discovered by hitting invisibugs (CRIT particle).
 * - CRIT hit → add position to config JSON
 * - ESP shows armor stands that are isCompletelyDefault AND near a known position
 * - Mapping complete when no new locations found in last N hits on confirmed spots
 */
object InvisibugScan {

    private const val PARTICLE_DISTANCE = 5.0
    private const val SCAN_CHUNKS = 12
    private const val SCAN_RADIUS = SCAN_CHUNKS * 16.0
    private const val BOX_SIZE = 0.25
    private const val MATCH_DISTANCE_SQ = 9.0

    private const val REPEAT_THRESHOLD = 3
    private const val CONSECUTIVE_CONFIRMED_TO_STOP = 8
    private const val MIN_LOCATIONS_FOR_COMPLETE = 25

    private val configFile: File get() = File(SkyblockImproved.configDir, "bug-positions.json")

    private var bugPositions: MutableSet<BlockPos> = mutableSetOf()
    private var positionsLoaded = false
    private var cachedBoxes: List<AABB> = emptyList()

    private val hitCount = mutableMapOf<String, Int>()
    private var consecutiveConfirmedHits = 0
    private var mappingCompleteNotified = false

    val possibleBugs: List<AABB>
        get() = cachedBoxes

    val possibleBugCenters: List<Vec3>
        get() = cachedBoxes.map { Vec3(it.minX + (it.maxX - it.minX) / 2, it.minY + (it.maxY - it.minY) / 2, it.minZ + (it.maxZ - it.minZ) / 2) }

    private val excludePositions = setOf(
        BlockPos(-698, 122, 80),
        BlockPos(-728, 98, 87),
        BlockPos(-719, 94, 36)
    )

    fun isInGalatea(): Boolean {
        val area = SkyBlockData.area
        val subArea = SkyBlockData.subArea
        return area.contains("Garden", ignoreCase = true) ||
            area.contains("Galatea", ignoreCase = true) ||
            subArea.contains("Garden", ignoreCase = true) ||
            subArea.contains("Galatea", ignoreCase = true)
    }

    private fun key(pos: BlockPos) = "${pos.x},${pos.y},${pos.z}"

    private fun ArmorStand.isCompletelyDefault(): Boolean = isDefaultName() && hasEmptyInventory()

    private fun ArmorStand.isDefaultName(): Boolean {
        val clean = Regex("§[0-9a-fk-or]").replace(name.string, "")
        return clean == I18n.get("entity.minecraft.armor_stand")
    }

    private fun ArmorStand.hasEmptyInventory(): Boolean {
        val slots = listOf(
            EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.OFFHAND
        )
        return slots.all { getItemBySlot(it).isEmpty }
    }

    private fun loadPositions() {
        if (positionsLoaded) return
        positionsLoaded = true

        val fromConfig = loadFromFile(configFile)
        if (fromConfig.isNotEmpty()) {
            bugPositions = fromConfig.toMutableSet()
            return
        }

        InvisibugScan::class.java.classLoader.getResourceAsStream("data/skyblock-improved/bug-positions.json")?.use { input ->
            val str = input.readBytes().toString(Charsets.UTF_8)
            val arr = com.google.gson.JsonParser.parseString(str).asJsonArray
            bugPositions = arr.map { el ->
                val o = el.asJsonObject
                BlockPos(o.get("x").asInt, o.get("y").asInt, o.get("z").asInt)
            }.toMutableSet()
            saveToFile()
        } ?: run { bugPositions = mutableSetOf() }
    }

    private fun loadFromFile(file: File): Set<BlockPos> {
        if (!file.exists()) return emptySet()
        return try {
            val str = file.readText()
            val arr = com.google.gson.JsonParser.parseString(str).asJsonArray
            arr.map { el ->
                val o = el.asJsonObject
                BlockPos(o.get("x").asInt, o.get("y").asInt, o.get("z").asInt)
            }.toSet()
        } catch (_: Exception) { emptySet() }
    }

    private fun saveToFile() {
        try {
            SkyblockImproved.configDir.mkdirs()
            val arr = com.google.gson.JsonArray()
            for (p in bugPositions.sortedWith(compareBy({ it.x }, { it.y }, { it.z }))) {
                val obj = com.google.gson.JsonObject()
                obj.addProperty("x", p.x)
                obj.addProperty("y", p.y)
                obj.addProperty("z", p.z)
                arr.add(obj)
            }
            configFile.writeText(com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(arr))
        } catch (e: Exception) {
            SkyblockImproved.logger.warn("InvisibugScan: failed to save bug-positions: ${e.message}")
        }
    }

    @JvmStatic
    fun onParticlePacket(packet: ClientboundLevelParticlesPacket) {
        if (packet.particle.type != ParticleTypes.CRIT) return
        if (!isInGalatea()) return

        val px = packet.x
        val py = packet.y
        val pz = packet.z

        SkyblockImproved.mc.execute {
            if (!EspModule.enabled || !EspModule.invisibugs.value) return@execute

            val level = SkyblockImproved.mc.level ?: return@execute
            val aabb = AABB(px - PARTICLE_DISTANCE, py - PARTICLE_DISTANCE, pz - PARTICLE_DISTANCE,
                px + PARTICLE_DISTANCE, py + PARTICLE_DISTANCE, pz + PARTICLE_DISTANCE)

            val stands = level.getEntitiesOfClass(ArmorStand::class.java, aabb)
            if (stands.isEmpty()) return@execute

            val nearest = stands.minByOrNull { (it.x - px) * (it.x - px) + (it.y - py) * (it.y - py) + (it.z - pz) * (it.z - pz) } ?: return@execute
            if (!nearest.isCompletelyDefault()) return@execute

            val pos = nearest.blockPosition()
            if (pos in excludePositions) return@execute

            val k = key(pos)
            val existingPos = bugPositions.find { it.distSqr(pos) <= MATCH_DISTANCE_SQ }
            val wasNew = if (existingPos != null) {
                val ek = key(existingPos)
                hitCount[ek] = (hitCount[ek] ?: 0) + 1
                false
            } else {
                bugPositions.add(pos)
                hitCount[k] = 1
                saveToFile()
                SbiMessage.send("ESP", "Discovered invisibug location (${bugPositions.size} total). Saved to config.")
                true
            }

            val count = hitCount[key(existingPos ?: pos)] ?: 1
            if (count >= REPEAT_THRESHOLD) {
                consecutiveConfirmedHits = if (wasNew) 0 else consecutiveConfirmedHits + 1
                if (!mappingCompleteNotified && bugPositions.size >= MIN_LOCATIONS_FOR_COMPLETE && consecutiveConfirmedHits >= CONSECUTIVE_CONFIRMED_TO_STOP) {
                    mappingCompleteNotified = true
                    SbiMessage.info("ESP", "Mapped all invisibug locations! Saved to config/skyblock-improved/bug-positions.json")
                }
            } else {
                consecutiveConfirmedHits = 0
            }
        }
    }

    private fun nearKnownSpawn(pos: BlockPos): Boolean {
        for (spawn in bugPositions) {
            if (pos.distSqr(spawn) <= MATCH_DISTANCE_SQ) return true
        }
        return false
    }

    fun tick() {
        if (!EspModule.enabled || !EspModule.invisibugs.value) {
            cachedBoxes = emptyList()
            return
        }
        if (!isInGalatea()) {
            cachedBoxes = emptyList()
            return
        }

        loadPositions()
        if (bugPositions.isEmpty()) {
            cachedBoxes = emptyList()
            return
        }

        val player = SkyblockImproved.mc.player ?: return
        val level = SkyblockImproved.mc.level ?: return
        val pos = player.position()
        val searchBox = AABB(pos, pos).inflate(SCAN_RADIUS)

        val boxes = mutableListOf<AABB>()
        for (entity in level.getEntitiesOfClass(ArmorStand::class.java, searchBox)) {
            val blockPos = entity.blockPosition()
            if (blockPos in excludePositions) continue
            if (!entity.isCompletelyDefault()) continue
            if (!nearKnownSpawn(blockPos)) continue

            val bb = entity.boundingBox
            val cx = (bb.minX + bb.maxX) / 2
            val cy = (bb.minY + bb.maxY) / 2
            val cz = (bb.minZ + bb.maxZ) / 2
            val h = BOX_SIZE / 2
            boxes.add(AABB(cx - h, cy - h, cz - h, cx + h, cy + h, cz + h))
        }
        cachedBoxes = boxes
    }

    fun clear() {
        hitCount.clear()
        consecutiveConfirmedHits = 0
        mappingCompleteNotified = false
    }
}
