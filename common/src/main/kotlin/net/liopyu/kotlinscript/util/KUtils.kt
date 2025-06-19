package net.liopyu.kotlinscript.util

object KUtils {
    private val loadedInstances = mutableMapOf<String, Any>()

    fun register(scriptName: String, instance: Any) {
        loadedInstances[scriptName] = instance
    }

    fun getKtsClass(scriptName: String, nestedClassSimpleName: String): Class<*> {
        val script = getKtsFileOrThrow(scriptName)
        val outer = script::class.java
        return outer.declaredClasses.firstOrNull { it.simpleName == nestedClassSimpleName }
            ?: error("Nested class '$nestedClassSimpleName' not found in script '$scriptName'")
    }

    fun bindClassAs(
        alias: String,
        scriptName: String,
        className: String,
        bindAs: BindTarget = BindTarget.CONSTRUCTOR
    ): Pair<String, Any> {
        val clazz = getKtsClass(scriptName, className)
        return when (bindAs) {
            BindTarget.CONSTRUCTOR -> {
                alias to { clazz.getDeclaredConstructor().newInstance() }
            }

            BindTarget.COMPANION -> {
                val companion = clazz.getField("Companion").get(null)
                alias to companion
            }

            BindTarget.OBJECT_INSTANCE -> {
                val instance = clazz.getField("INSTANCE").get(null)
                alias to instance
            }
        }
    }

    fun bindScriptNamespace(scriptName: String, instance: Any): Pair<String, Any> {
        val scriptClass = instance::class.java
        val innerClasses = scriptClass.declaredClasses

        val constructors = mutableMapOf<String, () -> Any>()
        for (inner in innerClasses) {
            val simple = inner.simpleName
            constructors[simple] = { inner.getDeclaredConstructor().newInstance() }
        }

        return scriptName to ScriptNamespace(constructors)
    }

    class ScriptNamespace(private val constructors: Map<String, () -> Any>) {
        fun get(className: String): () -> Any =
            constructors[className] ?: error("Class '$className' not found in script namespace")

        // Optional: allow calling like MyTypes.MyClass()
        operator fun invoke(className: String): Any = get(className).invoke()

        // Optional: property-style access (e.g. MyTypes.MyClass())
        operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): () -> Any {
            return get(property.name)
        }
    }

    enum class BindTarget {
        CONSTRUCTOR,
        COMPANION,
        OBJECT_INSTANCE
    }

    fun getKtsFile(name: String): Any? = loadedInstances[name]

    fun getKtsFileOrThrow(name: String): Any =
        loadedInstances[name] ?: error("No script registered under name: $name")
}
