package me.koteyka32k.rhplugins.startupsound.mixin;

import me.koteyka32k.rhplugins.startupsound.StartupSoundManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin that makes the sound actually play at startup.
 *
 * @author Koteyka32k
 * @since 1.0
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "onGameLoadFinished",
            at = @At("TAIL"))
    void onLoad(CallbackInfo ci) {
        StartupSoundManager.playStartupSound();
    }
}