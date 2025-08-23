package net.liopyu.kotlinscript.util

data class KotlinObject(
    val fullyQualifiedName: String,
    val source: String,
    val type: String,
    val path: String,
    val parentType: String? = null,
    val requiresImport: Boolean,
    val returnType: String,
    val members: MutableList<KotlinObject> = mutableListOf(),
    val typeParameters: List<String> = emptyList(),
    val modifiers: List<String> = emptyList()
)


data class MethodEntry(
    val name: String,
    val type: String,
    val parentClass: List<String>? = null
)

data class FieldEntry(
    val name: String,
    val type: String,
    val parentClass: String
)
