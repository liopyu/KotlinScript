import net.liopyu.kotlinscript.TestClass

TestClass().execute { i ->
    val testString = " test"
    println("Received in consumer: $i $testString")
}

