package net.liopyu.kotlinscript.util


import com.mojang.logging.LogUtils
import net.liopyu.kotlinscript.PluginScript
import java.security.ProtectionDomain

class KSClassLoader(
    parent: ClassLoader = PluginScript::class.java.classLoader
) : ClassLoader(parent) {
    fun defineClass(name: String, bytes: ByteArray): Class<*> {
        val domain: ProtectionDomain = PluginScript::class.java.protectionDomain
        return super.defineClass(name, bytes, 0, bytes.size, domain)
    }

    fun defineRemappedClass(name: String, bytes: ByteArray): Class<*> {
        val remapped = remapClassBytes(bytes)
        return defineClass(name, remapped, 0, remapped.size)
    }

    fun linkClass(clazz: Class<*>) {
        resolveClass(clazz)
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        var clazz = findLoadedClass(name)
        LogUtils.getLogger().info("loading class: " + name)
        if (clazz == null) {
            clazz = super.getParent()?.loadClass(name) ?: findSystemClass(name)
        }
        if (resolve) resolveClass(clazz)
        return clazz
    }
}