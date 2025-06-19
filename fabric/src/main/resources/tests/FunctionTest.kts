import net.liopyu.kotlinscript.util.KUtils

val clazz = KUtils.getKtsClass("MyTypes", "MyClass")
val instance = clazz.getDeclaredConstructor().newInstance()
clazz.getMethod("greet").invoke(instance)
