package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();
    private final KotlinScriptInterpreter scriptInterpreter;

    public KotlinScript() {
        scriptInterpreter = new KotlinScriptInterpreter();
        scriptInterpreter.interpretKotlinScript("data/scripts/script.kts");
    }
}