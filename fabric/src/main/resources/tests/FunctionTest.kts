import net.liopyu.kotlinscript.util.KUtils
import java.lang.reflect.Modifier

val clazz = KUtils.getKtsClass("MyTypes", "MyClass")
val instance = clazz.getDeclaredConstructor().newInstance()
clazz.getMethod("greet").invoke(instance)





fun some(): Unit {
}

fun main() {
    val clazz = Class.forName("MyTypes")
    val method = clazz.getDeclaredMethod("some")
    val isStatic = Modifier.isStatic(method.modifiers)
    clazz.methods.forEach { LogUtils.getLogger().info(it.name) }
}
main()