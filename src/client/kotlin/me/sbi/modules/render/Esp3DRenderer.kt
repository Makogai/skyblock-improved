package me.sbi.modules.render

import me.sbi.SkyblockImproved
import me.sbi.modules.skyblock.InvisibugScan
import me.sbi.modules.skyblock.PartyMemberScan
import me.sbi.utils.render.EspRenderLayer
import me.sbi.utils.render.PrimitiveRenderer
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.AABB

/**
 * Renders 3D ESP boxes. Supports Outline, Filled, and Minecraft style (glow + black outline).
 */
object Esp3DRenderer {

    fun init() {
        WorldRenderEvents.END_MAIN.register(::onRender)
    }

    private fun getColor(): FloatArray {
        if (EspModule.espRainbow.value) {
            val t = System.currentTimeMillis() % 3000 / 3000.0
            val hue = t * 360.0
            return hsvToRgb(hue.toFloat(), 1f, 1f)
        }
        val c = EspModule.espColor.value
        return floatArrayOf(
            ((c shr 16) and 0xFF) / 255f,
            ((c shr 8) and 0xFF) / 255f,
            (c and 0xFF) / 255f
        )
    }

    private fun getOutlineColor(): FloatArray {
        val c = EspModule.espOutlineColor.value
        return floatArrayOf(
            ((c shr 16) and 0xFF) / 255f,
            ((c shr 8) and 0xFF) / 255f,
            (c and 0xFF) / 255f,
            1f
        )
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): FloatArray {
        val c = v * s
        val x = c * (1 - kotlin.math.abs(((h / 60f) % 2) - 1))
        val m = v - c
        val (r, g, b) = when ((h / 60).toInt() % 6) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return floatArrayOf(r + m, g + m, b + m)
    }

    private fun expandBox(box: AABB, sizeMult: Double): AABB {
        val cx = (box.minX + box.maxX) / 2
        val cy = (box.minY + box.maxY) / 2
        val cz = (box.minZ + box.maxZ) / 2
        val hx = (box.maxX - box.minX) / 2 * sizeMult
        val hy = (box.maxY - box.minY) / 2 * sizeMult
        val hz = (box.maxZ - box.minZ) / 2 * sizeMult
        return AABB(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz)
    }

    private fun onRender(context: WorldRenderContext) {
        if (!EspModule.enabled) return
        val invisibugBoxes = if (EspModule.invisibugs.value && InvisibugScan.isInGalatea())
            InvisibugScan.possibleBugs else emptyList()
        val partyBoxes = if (EspModule.partyMembers.value) PartyMemberScan.partyMemberBoxes else emptyList()
        val boxes = invisibugBoxes + partyBoxes
        if (boxes.isEmpty()) return

        val matrices = context.matrices() ?: return
        val consumers = context.consumers() as? MultiBufferSource.BufferSource ?: return
        val camPos = context.gameRenderer().mainCamera?.position ?: return

        val rgb = getColor()
        val r = rgb[0]; val g = rgb[1]; val b = rgb[2]
        val a = (EspModule.espOpacity.value / 100.0).toFloat().coerceIn(0f, 1f)
        val sizeMult = EspModule.espSize.value
        val displayStyle = EspModule.espDisplayStyle.value

        matrices.pushPose()
        matrices.translate(-camPos.x, -camPos.y, -camPos.z)

        val pose = matrices.last()
        val scaledBoxes = boxes.map { expandBox(it, sizeMult) }

        when (displayStyle) {
            "Minecraft" -> {
                val outline = getOutlineColor()
                val oa = (EspModule.espOpacity.value / 100.0).toFloat().coerceIn(0f, 1f)
                val fillBuffer = consumers.getBuffer(EspRenderLayer.FILLED_ESP)
                val lineBuffer = consumers.getBuffer(EspRenderLayer.LINE_ESP_THICK)
                for (box in scaledBoxes) {
                    PrimitiveRenderer.addChainedFilledBoxVertices(pose, fillBuffer,
                        box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(),
                        box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(),
                        r, g, b, a * 0.9f)
                    PrimitiveRenderer.renderLineBox(pose, lineBuffer, box, outline[0], outline[1], outline[2], oa)
                }
                consumers.endBatch(EspRenderLayer.FILLED_ESP)
                consumers.endBatch(EspRenderLayer.LINE_ESP_THICK)
            }
            else -> {
                val style = EspModule.espStyle.value
                when (style) {
                    "Outline" -> {
                        val buffer = consumers.getBuffer(EspRenderLayer.LINE_ESP)
                        for (box in scaledBoxes) {
                            PrimitiveRenderer.renderLineBox(pose, buffer, box, r, g, b, a)
                        }
                        consumers.endBatch(EspRenderLayer.LINE_ESP)
                    }
                    "Filled" -> {
                        val buffer = consumers.getBuffer(EspRenderLayer.FILLED_ESP)
                        for (box in scaledBoxes) {
                            PrimitiveRenderer.addChainedFilledBoxVertices(pose, buffer,
                                box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(),
                                box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(),
                                r, g, b, a * 0.6f)
                        }
                        consumers.endBatch(EspRenderLayer.FILLED_ESP)
                    }
                    else -> {}
                }
            }
        }

        if (EspModule.partyMembers.value && EspModule.espShowNames.value) {
            val font = SkyblockImproved.mc.font
            val camera = context.gameRenderer().mainCamera
            val cameraRot = camera.rotation()
            for (info in PartyMemberScan.partyMemberInfos) {
                val dist = info.distance.toFloat().coerceAtLeast(1f)
                val scaleFactor = (0.02f * (dist / 5f)).coerceIn(0.015f, 0.15f)
                matrices.pushPose()
                val pose = matrices.last().pose()
                pose.translate(info.labelPos.x.toFloat(), info.labelPos.y.toFloat(), info.labelPos.z.toFloat())
                    .rotate(cameraRot)
                    .scale(scaleFactor, -scaleFactor, scaleFactor)
                val textWidth = font.width(info.name).toFloat()
                font.drawInBatch(
                    info.name, -textWidth / 2f, 0f, -1, true, pose, consumers,
                    Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT
                )
                matrices.popPose()
            }
        }

        matrices.popPose()
    }
}
