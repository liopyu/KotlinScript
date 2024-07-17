package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.example.KtMain;
import org.slf4j.Logger;

@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "kotlinscript";
    public KotlinScript() {
        KtMain.otherMain();
        }
}
