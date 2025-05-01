package net.liopyu.kotlinscript.util

data class KotlinObject(
    val fullyQualifiedName: String,
    val args: List<String>,
    val source: String,
    val type: String,
    val path: String,
    val parentType: String? = null,
    val requiresImport: Boolean,
    val returnType: String,
)

data class ClassEntry(
    val extends: List<String> = emptyList(),
    val implements: List<String> = emptyList(),
    val methods: MutableList<MethodEntry> = mutableListOf(),
    val fields: MutableList<FieldEntry> = mutableListOf()
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
