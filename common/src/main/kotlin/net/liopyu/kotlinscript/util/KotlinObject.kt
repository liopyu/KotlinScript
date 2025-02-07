package net.liopyu.kotlinscript.util
data class KotlinObject(
    val fullyQualifiedName: String,
    val simpleName: String,
    val source: String? = null,
    val type: String? = null, // e.g., "Method", "Class", "Property"
    val path: String? = null  // e.g., "kotlin.collections.List"
)
