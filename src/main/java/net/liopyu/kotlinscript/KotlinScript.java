package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;


@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();
    private final KotlinScriptInterpreter scriptInterpreter;

    public KotlinScript() {
        scriptInterpreter = new KotlinScriptInterpreter();
        String scriptFolderPath = "data/scripts";
        scriptInterpreter.loadScriptsFromFolder(scriptFolderPath);
        scriptInterpreter.interpretKotlinScripts();
    }
}
