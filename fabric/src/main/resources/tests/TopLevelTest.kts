// Constants and literals at the top level
val number = 42
val message = "Hello, KotlinScript!"
val isTrue = true
val items = listOf(1, 2, 3, 4)

// Function definition and usage at the top level
fun greet(name: String): String {
    return "Hello, $name!"
}
val greeting = greet("World")
try{}catch(e: Exception){}
val iterable = arrayOf("")
for (i in iterable) {

}



// If expression at the top level
val status = if (isTrue) "Active" else "Inactive"

// When expression at the top level
val dayOfWeek = 3
val dayName = when (dayOfWeek) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    else -> "Weekend"
}

// Type check expressions
val isString = message is String
val isInList = 2 in items

// Collection and object literals
val newList = items + listOf(5, 6)
val sampleObject = object {
    val id = 1
    val name = "Sample"
}

// Expression with operators and assignments
val result = (number + 8) * 2 - 4 / 2
val anotherResult = result % 3
val combinedMessage = message + " Welcome!"

// Callable reference
val stringLengthFunc = String::length
val lengthOfMessage = stringLengthFunc(message)
