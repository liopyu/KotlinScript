import java.nio.file.Path
import kotlin.reflect.jvm.reflect

java.nio.file.FileVisitor
kotlin.io.path.createTempFile
Path()
kotlin.collections.contentEquals()
fun main() {
    val array1 = arrayOf(1, 2, 3)
    val array2 = arrayOf(1, 2, 3)
    val array3 = arrayOf(4, 5, 6)

    println(array1.contentEquals(array2)) // true
    println(array1.contentEquals(array3)) // false
}

inline fun <T> Sequence<T>.first(predicate: (T) -> Boolean): T {
    for (element in this) if (predicate(element)) return element
    throw NoSuchElementException("Sequence contains no element matching the predicate.")
}

val add: (Int, Int) -> Int = { a, b ->
    a + b
}
val combine: (String, String) -> String = { t, u -> "$t and $u" }

fun <T, U> makeCombiner(): (T, U) -> String {
    return { t, u -> "$t and $u" }
}

