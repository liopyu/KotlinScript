// List of test class names to check for existence
val classNames = listOf(
    "jdk.internal.org.objectweb.asm.tree.ModuleNode",
    "net.fabricmc.loader.impl.lib.mappingio.tree.MappingTree",
    "kotlin.reflect.full.KCallables"
)

// Function to test if a class exists
fun testClassExistence(className: String): Boolean {
    return try {
        // Attempt to load the class without initializing it
        val clazz = Class.forName(className)

        // Check if the class is public
        java.lang.reflect.Modifier.isPublic(clazz.modifiers).also {
            if (it) {
                println("Class is public and available: $className")
            } else {
                println("Class is not public: $className")
            }
        }
    } catch (e: Throwable) {
        println("Class not found or inaccessible: $className")
        false
    }
}

// Test each class in the list
classNames.forEach { className ->
    testClassExistence(className)
}
