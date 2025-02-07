interface MyInterface {
    fun doSomething()
}

class CustomClass : MyInterface {
    override fun doSomething() {
        println("Doing something in CustomClass")
    }
}


object Outer {
    var obj: MyInterface
    init { obj = object : MyInterface by CustomClass(){} }

}
enumValueOf<>()
class Three<T : MyInterface> {

}
class Three<T> {
    fun some(action: () -> Unit) {
        action()
    }

}

fun main() {

    val d = Three<Int>::some
    d(Three()) { println("Lambda called!") } // Prints: Lambda called!
}


val obj = object : MyInterface by CustomClass() {}