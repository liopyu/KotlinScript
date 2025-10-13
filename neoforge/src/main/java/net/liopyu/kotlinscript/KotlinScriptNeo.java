package net.liopyu.kotlinscript;

import kotlin.KotlinVersion;
import net.neoforged.fml.common.Mod;

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

@Mod("kotlinscript")
public class KotlinScriptNeo {
    public KotlinScriptNeo() {
        ensureStdlib();
        ensureKotlinJarsOnDiskAndSetProps();
        ClassLoader cl = KotlinScriptNeo.class.getClassLoader();
        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            KotlinScriptLoader.loadScripts();
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
    }

    private void ensureStdlib() {
        String k = "kotlin.java.stdlib.jar";
        if (System.getProperty(k) != null) return;
        try {
            var url = kotlin.KotlinVersion.class.getProtectionDomain().getCodeSource().getLocation();
            if (url != null) System.setProperty(k, java.nio.file.Paths.get(url.toURI()).toString());
        } catch (Exception ignored) {
        }
    }

    private static void ensureKotlinJarsOnDiskAndSetProps() {
        if (System.getProperty("kotlin.compiler.jar") != null) return;
        try {
            URL loc = net.liopyu.kotlinscript.KS.class.getProtectionDomain().getCodeSource().getLocation();
            Path jarPath;
            if (loc == null) return;
            if (!"file".equalsIgnoreCase(loc.getProtocol())) {
                jarPath = dumpUrlToTempJar(loc, "kotlinscript-kotlin-rt");
            } else {
                Path p = Paths.get(loc.toURI());
                jarPath = Files.isRegularFile(p) ? p : zipDirToTempJar(p, "kotlinscript-kotlin-rt");
            }
            String path = jarPath.toAbsolutePath().toString();
            String base = new File(System.getProperty("user.dir"), "config/kotlinscript/jij-cache").getAbsolutePath();
            System.setProperty("kotlin.home", base);
            System.setProperty("kotlin.compiler.jar", path);
            System.setProperty("kotlin.java.stdlib.jar", path);
            System.setProperty("kotlin.java.reflect.jar", path);
            System.setProperty("kotlin.script.runtime.jar", path);
        } catch (Exception ignored) {
        }
    }

    private static Path dumpUrlToTempJar(URL url, String prefix) throws IOException {
        Path tmp = Files.createTempFile(prefix, ".jar");
        tmp.toFile().deleteOnExit();
        try (InputStream in = url.openStream();
             OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
            in.transferTo(out);
        }
        return tmp;
    }

    private static Path zipDirToTempJar(Path dir, String prefix) throws IOException {
        Path tmp = Files.createTempFile(prefix, ".jar");
        tmp.toFile().deleteOnExit();
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(tmp))) {
            Files.walk(dir).forEach(p -> {
                try {
                    if (Files.isDirectory(p)) return;
                    String rel = dir.relativize(p).toString().replace(File.separatorChar, '/');
                    jos.putNextEntry(new ZipEntry(rel));
                    Files.copy(p, jos);
                    jos.closeEntry();
                } catch (IOException ignored) {
                }
            });
        }
        return tmp;
    }
}
