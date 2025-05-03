package net.liopyu.kotlinscript.util

data class KotlinObject(
    val fullyQualifiedName: String,
    val source: String,
    val type: String,
    val path: String,
    val parentType: String? = null,
    val requiresImport: Boolean,
    val returnType: String,
    val members: MutableList<KotlinObject> = mutableListOf()
)


data class MethodEntry(
    val name: String,
    val args: List<String>? = null,
    val returns: String
)

data class FieldEntry(
    val name: String,
    val type: String
)
