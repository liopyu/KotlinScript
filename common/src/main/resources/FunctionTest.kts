import kotlin.reflect.KCallable
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.valueParameters


// Test function to check KCallable functionality
fun testKCallable() {
    // Create a sample callable: a reference to a function
    val callable: KCallable<*> = ::exampleFunction

    // Try accessing methods from KCallables (e.g., valueParameters, findParameterByName)
    println("Function name: ${callable.name}")
    println("Value parameters: ${callable.valueParameters}")

    val param = callable.findParameterByName("param1")
    if (param != null) {
        println("Found parameter 'param1': $param")
    } else {
        println("Parameter 'param1' not found")
    }
}

// A sample function to test reflection
fun exampleFunction(param1: String, param2: Int): String {
    return "$param1 and $param2"
}

// Call the test function
testKCallable()
