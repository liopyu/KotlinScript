package net.liopyu.kotlinscript;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * This class is not thread-safe, don't use it for parallel executions and create new instances instead.
 */
public class KtsObjectLoader {
    public static ScriptEngineManager manager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());
    private static final ScriptEngine engine = manager.getEngineByExtension("kts");

   /* static {
        ScriptEngineManager manager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());
        engine = manager.getEngineByExtension("kts");
    }
*/
    public static <T> T castOrError(Object obj, Class<T> clazz) {
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        } else {
            throw new IllegalArgumentException("Cannot cast " + obj + " to expected type " + clazz.getSimpleName());
        }
    }

    public static <T> T load(String script, Class<T> clazz) {
        try {
            Object result = engine.eval(script);
            return castOrError(result, clazz);
        } catch (ScriptException e) {
            throw new RuntimeException("Cannot load script", e);
        }
    }

    public static <T> T load(Reader reader, Class<T> clazz) {
        try {
            Object result = engine.eval(reader);
            return castOrError(result, clazz);
        } catch (ScriptException e) {
            throw new RuntimeException("Cannot load script", e);
        }
    }

    public static <T> T load(InputStream inputStream, Class<T> clazz) {
        return load(new InputStreamReader(inputStream, StandardCharsets.UTF_8), clazz);
    }

    public static <T> List<T> loadAll(Class<T> clazz, InputStream... inputStreams) {
        List<T> results = new ArrayList<>();
        Arrays.stream(inputStreams).forEach(inputStream -> results.add(load(inputStream, clazz)));
        return results;
    }
}