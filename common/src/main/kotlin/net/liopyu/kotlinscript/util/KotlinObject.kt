package net.liopyu.kotlinscript.util
data class KotlinObject(
    val fullyQualifiedName: String,
    val simpleName: String,
    val source: String,
    val type: String,
    val path: String,
    val parentType: String? = null
)

