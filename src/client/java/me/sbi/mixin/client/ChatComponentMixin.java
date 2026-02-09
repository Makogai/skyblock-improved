package me.sbi.mixin.client;

import me.sbi.modules.skyblock.ChatTranscriptCollector;
import me.sbi.modules.skyblock.SkyBlockParty;
import me.sbi.util.SbiLog;
import me.sbi.util.SbiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts ALL messages added to chat - catches party list regardless of how Hypixel sends it.
 * READ-ONLY: Observes chat display only. Does not modify or send packets. Watchdog-safe.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onAddMessage(Component message, CallbackInfo ci) {
        String text;
        try {
            text = message.getString();
        } catch (Exception e) {
            text = message.toString();
        }
        if (text == null || text.isEmpty()) return;

        ChatTranscriptCollector.INSTANCE.add(text);

        if (text.toLowerCase().contains("party") && SbiLog.INSTANCE.isDebugEnabled()) {
            SbiLog.INSTANCE.debug("ChatComponent received: " + text.substring(0, Math.min(200, text.length())) + (text.length() > 200 ? "..." : ""));
        }
        if (text.contains("Party Members (")
            || text.contains("Party Leader:")
            || text.contains("Party Members:")
            || text.contains("Party Moderators:")
            || text.toLowerCase().contains("party was transferred to")
            || text.toLowerCase().contains("joined the party")
            || text.toLowerCase().contains("has left the party")) {
            SkyBlockParty.parseFromChat(text);
            if (SbiLog.INSTANCE.isDebugEnabled()) {
                SbiMessage.debug("Party", "Parsed: " + SkyBlockParty.getAllMembersForDebug());
            }
        } else if (text.toLowerCase().contains("party was disbanded")
            || text.toLowerCase().contains("kicked from the party")) {
            SkyBlockParty.clear();
        }
    }
}
