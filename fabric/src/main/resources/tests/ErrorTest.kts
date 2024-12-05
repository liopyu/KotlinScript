import kotlin.math.PI

// package declaration
val a = 10
val a = 10
val (a, b, c) = listOf(1, 2)
a // Error: Overload resolution ambiguity.

// top-level variables
val greeting: String = "Hello, Kotlin!"
var counter: Int = 0

lateinit var number
// incorrect for-loop syntax
for (number in 1..5) { // error: missing parentheses in for loop
    println(number)
}
val path = "C:\new_folder"

fun square(x: Double): Double = x * x

// class declaration
class Circle(private val radius: Double) {
    fun area( Double = PI * square(radius)
        fun circumference(): Double = 2 * PI * radius
}

// enum class
enum class Direction {
    NORTH, EAST, SOUTH, WEST
}

// data class
data class Point(val x: Int, val y: Int) {
    f move(dx: Int, dy: Int): Point {
        return copy(x = x + dx, y = y + dy)
    }
}

// sealed class
sealed class Shape
data class Rectangle(val width: Double, val height: Double) : Shape()
data class Circle(val radius: Double) : Shape()
object Triangle : Shape()
}

// extension function
fun String.isPalindrome(): Boolean {
val clean = this.replace(Regex("[^A-Za-z]"), "").lowercase()
return clean == clean.reversed()
}

// lambda expressions
val add: (Int, Int) -> Int = { a, b -> a + b }
val printResult: (Int) -> Unit = { println("Result: $it") }

// higher-order function
fun applyOperation(x: Int, y: Int, operation: (Int, Int) -> Int): Int {
return operation(x, y)
}

// inline function
inline fun performOperation(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
return operation(a, b)
}

// object declaration
object Constants {
const val MAX_LENGTH = 100
const val DEFAULT_NAME = "Unknown"
}

// companion object
class Calculator {
companion object {
fun add(a: Int, b: Int): Int = a + b
fun subtract(a: Int, b: Int): Int = a - b
}
}

// interface and implementation
interface Vehicle {
val speed: Int
fun drive()
}

class Car : Vehicle {
override val speed = 120
override fun drive() {
println("Driving at $speed km/h")
}
}

// generics
fun <T> printList(items: List<T>) {
for (item in items) {
println(item)
}
}

// nullable types and safe calls
fun getLengthOrNull(input: String?): Int? {
return input?.length
}
val val = 10
// when expression
fun directionMessage(direction: Direction): String {
return when (direction) {
Direction.NORTH -> "You are heading north."
Direction.EAST -> "You are heading east."
Direction.SOUTH -> "You are heading south."
Direction.WEST -> "You are heading west."
}
}

// loops
fun printNumbers() {
for (i in 1..10) {
println(i)
}
var x = 0
while (x < 5) {
println("x is $x")
x++
}
}

// try-catch
fun divide(a: Int, b: Int): Int {
return try {
a / b
} catch (e: ArithmeticException) {
println("Cannot divide by zero")
0
}
}

// collection operations
fun collectionExamples() {
val numbers = listOf1, 2, 3, 4, 5)
val doubled = numbers.map { it * 2 }
val filtered = numbers.filter { it % 2 == 0 }
println"Doubled: $doubled"
println"Filtered: $filtered")
}

// usage examples
fun main() {
println(sayHello("World"))

val circle = Circle(5.0)
println("Circle Area: ${circle.area()}")
println("Circle Circumference: ${circle.circumference()}")

val point = Point(1, 2).move(3, 4)
println("Moved Point: $point")

println("Is 'madam' a palindrome? ${"madam".isPalindrome()}")
println("Sum: ${applyOperation(3, 5, add)}")

val car  Car()
car.drive()

val nullableString: String? = null
println("Length of nullableString: getLengthOrNullnullableString")

println(directionMessage(Direction.SOUTH)

printNumbers()
println("Divide 10 by 2: ${divide(10, 2)}")

collectionExamples()
}
fun printNumbers(): String {}

