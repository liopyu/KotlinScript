package net.liopyu.kotlinscript.util


import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper


object KSRemapper {
    fun remapClassName(name: String): String {
        return deobfToObfClassMap[name] ?: name
    }

    fun remapMethodName(owner: String, name: String, desc: String): String {
        val map = deobfToObfMethodMap[owner] ?: return name
        return map[name] ?: name
    }

    fun remapFieldName(owner: String, name: String, desc: String): String {
        val map = deobfToObfFieldMap[owner] ?: return name
        return map[name] ?: name
    }
}

object KSASMRemapper : Remapper() {
    override fun map(internalName: String): String {
        return KSRemapper.remapClassName(internalName.replace('/', '.')).replace('.', '/')
    }

    override fun mapMethodName(owner: String, name: String, desc: String): String {
        return KSRemapper.remapMethodName(owner, name, desc)
    }

    override fun mapFieldName(owner: String, name: String, desc: String): String {
        return KSRemapper.remapFieldName(owner, name, desc)
    }
}

fun remapClassBytes(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    val remapper = ClassRemapper(writer, KSASMRemapper)
    reader.accept(remapper, 0)
    return writer.toByteArray()
}
