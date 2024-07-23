package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;
import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Tokenizer;
import net.liopyu.kotlinscript.util.Executor;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Scope;
import net.liopyu.kotlinscript.util.TypeChecker;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


@Mod(KotlinScript.MODID)
public class KotlinScript {
    public static final String MODID = "kotlin_script";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KotlinScript() throws IOException {
        //Executor.main(null);
    }

}
