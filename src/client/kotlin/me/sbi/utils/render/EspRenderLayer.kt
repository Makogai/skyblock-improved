package me.sbi.utils.render

import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import java.util.*

/**
 * Custom render layer for ESP wireframe (renders through walls).
 * Based on OdinFabric's CustomRenderLayer.LINE_LIST_ESP.
 */
object EspRenderLayer {

    val LINE_ESP: RenderType = RenderType.create(
        "sbi-line-esp",
        RenderType.BIG_BUFFER_SIZE,
        CustomRenderPipelines.LINE_LIST_ESP,
        RenderType.CompositeState.builder()
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(3.0)))
            .createCompositeState(false)
    )

    val LINE_ESP_THICK: RenderType = RenderType.create(
        "sbi-line-esp-thick",
        RenderType.BIG_BUFFER_SIZE,
        CustomRenderPipelines.LINE_LIST_ESP,
        RenderType.CompositeState.builder()
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(5.0)))
            .createCompositeState(false)
    )

    val FILLED_ESP: RenderType = RenderType.create(
        "sbi-filled-esp",
        RenderType.BIG_BUFFER_SIZE,
        false,
        true,
        CustomRenderPipelines.TRIANGLE_STRIP_ESP,
        RenderType.CompositeState.builder().createCompositeState(false)
    )
}
