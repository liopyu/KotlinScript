// Enum with constructor for entries that need additional information
enum class Color(val description: String) {
    RED("Vibrant Red"),            // simple identifier with description
    GREEN("Soothing Green"),       // description for green
    BLUE("Cool Blue"),             // description for blue
    YELLOW("Bright Yellow") {      // custom description for yellow
        override fun additionalInfo() = "Yellow is often associated with happiness"
    };

    // Default function to provide extra info (can be overridden)
    open fun additionalInfo(): String = "Color description is: $description"
}

// Testing type grammar, including nullable types, projections, and function types
val color: Color? = Color.RED                // nullable type with quest symbol
val result: (Int, String) -> Boolean = { num, str -> num.toString() == str } // function type with parameters
val data: Array<out String> = arrayOf("A", "B", "C") // type projection with variance modifier
val status: (() -> Unit)? = null             // nullable function type

// Function to demonstrate type references and function type usage
fun getColorInfo(input: Color): String {
    return when (input) {
        Color.RED -> "Red Color"
        Color.GREEN -> "Green Color"
        Color.BLUE -> "Blue Color"
        Color.YELLOW -> input.additionalInfo()
    }
}

// Usage of different type grammar with function type and nullable types
fun calculate(action: (Int) -> String, value: Int?): String? {
    return value?.let { action(it) }
}

// Testing enum in a function
fun describeColor(color: Color): String {
    return color.additionalInfo()
}

// Example usage
fun main() {
    println(getColorInfo(Color.RED))
    println(describeColor(Color.YELLOW))
    println(calculate({ it.toString() }, 42))
}
