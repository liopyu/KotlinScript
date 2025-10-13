package net.liopyu.kotlinscript;

val MODID: String = "kotlinscript";

//@Mod("kotlinscript")
class KotlinScriptNeoKS {
    init {
        ensure()
        try {
            KotlinScriptInit.preInitialize()
        } catch (t: Throwable) {
            t.printStackTrace()
            throw t
        }

    }

    fun ensure() {
        val k = "net.liopyu.kotlinscript.shadow.java.stdlib.jar"
        if (System.getProperty(k) != null) return
        val url = kotlin.KotlinVersion::class.java.protectionDomain.codeSource.location
        if (url != null) System.setProperty(k, java.nio.file.Paths.get(url.toURI()).toString())
    }
}
