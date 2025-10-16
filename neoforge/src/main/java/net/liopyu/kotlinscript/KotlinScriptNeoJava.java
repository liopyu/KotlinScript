package net.liopyu.kotlinscript;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;


public class KotlinScriptNeoJava {
    public KotlinScriptNeoJava() {
        ensureStdlib();
        ClassLoader cl = KotlinScriptNeoJava.class.getClassLoader();
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            //   KotlinScriptLoader.loadScripts();
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    private void ensureStdlib() {
   /*     String k = "kotlin.java.stdlib.jar";
        if (System.getProperty(k) != null) return;
        try {
            var url = kotlin.KotlinVersion.class.getProtectionDomain().getCodeSource().getLocation();
            if (url != null) System.setProperty(k, java.nio.file.Paths.get(url.toURI()).toString());
        } catch (Exception ignored) {
        }*/
    }

}
