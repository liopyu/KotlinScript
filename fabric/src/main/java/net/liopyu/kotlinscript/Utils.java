package net.liopyu.kotlinscript;

import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;

public class Utils {
    public static void something() {
        for (Method method : FabricBootstrap.Companion.getClass().getMethods()) {
            LogUtils.getLogger().info(method.getName());
        }
    }
}
