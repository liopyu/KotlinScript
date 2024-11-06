package net.liopyu.kotlinscript.mixin;

import com.mojang.logging.LogUtils;
import net.liopyu.kotlinscript.KotlinScript;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(value = ReloadCommand.class, remap = true)
public class ReloadMixin {
    @Inject(method = "reloadPacks", at = @At("HEAD"))
    private static void onReload(Collection<String> p_138236_, CommandSourceStack p_138237_, CallbackInfo ci) {
        try {
            var kotlinScript = KotlinScript.getGetInstance();
            kotlinScript.loadScriptsFromFolder("config/scripts");
            kotlinScript.evaluateScripts();
        } catch (Exception e) {
            LogUtils.getLogger().error("",e);
        }

    }
}