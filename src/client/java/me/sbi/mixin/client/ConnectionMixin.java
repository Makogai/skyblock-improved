package me.sbi.mixin.client;

import me.sbi.modules.skyblock.InvisibugScan;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures CRIT particles to discover invisibug locations when you hit them. */
@Mixin(Connection.class)
public class ConnectionMixin {

    @Inject(method = "genericsFtw", at = @At("HEAD"))
    private static void onPacketReceived(Packet<?> packet, net.minecraft.network.PacketListener listener, CallbackInfo ci) {
        if (packet instanceof BundlePacket<?> bundle) {
            for (Packet<?> sub : bundle.subPackets()) {
                if (sub instanceof ClientboundLevelParticlesPacket p) InvisibugScan.onParticlePacket(p);
            }
        } else if (packet instanceof ClientboundLevelParticlesPacket p) {
            InvisibugScan.onParticlePacket(p);
        }
    }
}
