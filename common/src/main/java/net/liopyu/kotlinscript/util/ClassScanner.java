package net.liopyu.kotlinscript.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClassScanner {
    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access Unsafe", e);
        }
    }

    public static void widenClassesField() {
        try {
            Unsafe unsafe = getUnsafe();
            Field classesField = ClassLoader.class.getDeclaredField("classes");

            // Force the field to be public
            unsafe.putInt(classesField, Unsafe.ARRAY_OBJECT_INDEX_SCALE, classesField.getModifiers() & ~java.lang.reflect.Modifier.PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllLoadedClasses() {
        List<String> classNames = new ArrayList<>();
               /* for (Class<?> c : this.getClass().getClasses()) {
                    classNames.add(c.getName());
                }*/
        return classNames;
        /*

        try {
            widenClassesField(); // Make the 'classes' field public

            for (ClassLoader loader = ClassLoader.getSystemClassLoader(); loader != null; loader = loader.getParent()) {
                Field classesField = ClassLoader.class.getDeclaredField("classes");
                classesField.setAccessible(true);

                @SuppressWarnings("unchecked")
                ArrayList<Class<?>> loadedClasses = (ArrayList<Class<?>>) classesField.get(loader);

                for (Class<?> clazz : loadedClasses) {
                    classNames.add(clazz.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classNames;*/
    }

    private static final Logger LOGGER = Logger.getLogger(ClassScanner.class.getName());

    public static Set<Class<?>> getClassesInPackage(String packageName) {
        Set<Class<?>> classes = new HashSet<>();

        try {
            // Get all classes from the specified package in the JVM environment
            // For demonstration, this is a list of some known classes in `net.minecraft` package.
            String[] classNames = {
                    "net.minecraft.server.MinecraftServer",
                    "net.minecraft.world.level.Level",
                    "net.minecraft.client.Minecraft",
                    "net.minecraft.world.entity.Entity"
                    // Add more classes as needed.
            };

            for (String className : classNames) {
                if (className.startsWith(packageName)) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        classes.add(clazz);
                    } catch (ClassNotFoundException e) {
                        LOGGER.log(Level.WARNING, "Class not found: " + className, e);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while scanning for classes", e);
        }

        return classes;
    }
}
