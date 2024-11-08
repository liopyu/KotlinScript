package net.liopyu.kotlinscript.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class TypingsDumper {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonObject typingsObject = new JsonObject();
    private static final Set<String> processedClasses = new HashSet<>();

    public static void dumpTypingsToJSONFile(String packageName) {
        Set<Class<?>> classes = ClassScanner.getClassesInPackage(packageName);
        for (Class<?> clazz : classes) {
            addClassToTypings(clazz);
        }
        writeTypingsToJSONFile();
    }

    private static void addClassToTypings(Class<?> clazz) {
        String className = clazz.getName();
        if (processedClasses.contains(className) || !Modifier.isPublic(clazz.getModifiers())) {
            return; // Skip if already processed or not public
        }

        processedClasses.add(className);
        JsonObject classJson = new JsonObject();
        JsonArray methodsArray = new JsonArray();
        JsonArray fieldsArray = new JsonArray();

        // Add methods to JSON
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                JsonObject methodJson = new JsonObject();
                methodJson.addProperty("name", method.getName());
                methodJson.addProperty("returnType", method.getReturnType().getTypeName());

                if (!processedClasses.contains(method.getReturnType().getName())) {
                    addClassToTypings(method.getReturnType()); // Recursively add return type
                }

                JsonArray paramsArray = new JsonArray();
                for (Class<?> paramType : method.getParameterTypes()) {
                    paramsArray.add(paramType.getTypeName());
                    if (!processedClasses.contains(paramType.getName()) && !paramType.isPrimitive()) {
                        addClassToTypings(paramType); // Recursively add parameter types
                    }
                }
                methodJson.add("parameters", paramsArray);
                methodsArray.add(methodJson);
            }
        }

        // Add fields to JSON
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())) {
                JsonObject fieldJson = new JsonObject();
                fieldJson.addProperty("name", field.getName());
                fieldJson.addProperty("fieldType", field.getType().getTypeName());

                if (!processedClasses.contains(field.getType().getName()) && !field.getType().isPrimitive()) {
                    addClassToTypings(field.getType()); // Recursively add field type
                }

                fieldsArray.add(fieldJson);
            }
        }

        classJson.add("methods", methodsArray);
        classJson.add("fields", fieldsArray);
        typingsObject.add(className, classJson);
    }

    private static void writeTypingsToJSONFile() {
        File outputFile = new File("config/scripts/minecraft_typings.json");
        outputFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(typingsObject, writer);
            System.out.println("Typings JSON file generated successfully at " + outputFile.getPath());
        } catch (IOException e) {
            System.err.println("Failed to write JSON file: " + outputFile.getPath());
            e.printStackTrace();
        }
    }
}
