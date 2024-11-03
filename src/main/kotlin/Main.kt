package org.example
object KtMain {
    @JvmStatic fun otherMain() {
        /*net.liopyu.kotlinscript.KotlinScript.LOGGER.info("testing123")
        println("Hello from Kotlin!")
        main()*/
    }
}
fun main() {
    val kotlinScript = KotlinScript()
   /* System.setProperty("idea.use.native.fs.for.win", "false")
    System.setProperty("idea.home.path", "C:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2023.3.3")
   */ kotlinScript.loadScriptsFromFolder("scripts")
    kotlinScript.evaluateScripts()
}