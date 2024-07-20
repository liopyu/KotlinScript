package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();
    private final KotlinScriptInterpreter scriptInterpreter;
    public static final Map<String, KotlinScriptInterpreter> interpreterMap = new HashMap<>();

    public KotlinScript() throws FileNotFoundException {
        String scriptFolderPath = "scripts";
        scriptInterpreter = new KotlinScriptInterpreter(scriptFolderPath);
        scriptInterpreter.loadScriptsFromFolder();
        scriptInterpreter.interpretKotlinScripts();
        interpreterMap.put(scriptFolderPath, scriptInterpreter);
    }
}
