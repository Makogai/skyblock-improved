package me.sbi.mixin.client;

import me.sbi.modules.skyblock.SkyBlockData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures tab list header/footer from the packet Hypixel sends - this is where "Area: X" lives.
 *  READ-ONLY: Does not modify packets. Watchdog-safe. */
@Mixin(ClientPacketListener.class)
public class TabListPacketMixin {

    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void onTabListPacket(ClientboundTabListPacket packet, CallbackInfo ci) {
        SkyBlockData.onTabListPacket(packet.header(), packet.footer());
    }
}
