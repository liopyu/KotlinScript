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
        var ks =  new KotlinScriptInterpreter();
        ks.loadScriptsFromFolder();
        ks.evaluateScripts();
    }

    public static void main(String[] args) {
       var ks =  new KotlinScriptInterpreter();
        ks.loadScriptsFromFolder();
        ks.evaluateScripts();
      /*  System.setProperty("idea.use.native.fs.for.win", "false");
    System.setProperty("idea.home.path", "C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2023.3.3");
      */
    }
}
