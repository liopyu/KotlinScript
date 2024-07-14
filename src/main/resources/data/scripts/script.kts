import net.liopyu.kotlinscript.TestClass

TestClass().execute { input ->
    println("Received in consumer: $input")
}

