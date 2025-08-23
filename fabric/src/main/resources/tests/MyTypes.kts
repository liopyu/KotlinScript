object MyObj {
    @JvmStatic
    val answer = 42
}

object MyOtherObj {
    @JvmStatic
    fun bar() {
    }
}

class Foo {
    companion object {
        @JvmStatic
        fun baz() {
        }
    }
}
