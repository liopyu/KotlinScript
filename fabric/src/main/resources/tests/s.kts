package some.something
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.ExperimentalPathApi
class Test{
    init {
        DEATH_DURATION
    }
    private val ROOT_CODEC: Codec<Pokemon> = RecordCodecBuilder.create { instance ->

        RecordCodecBuilder.create { codecInstance ->
            codecInstance.group(
                BlockPos.CODEC.forGetter { blockEntity -> blockEntity.pos }
            ).apply(codecInstance) { pos -> CustomBlockEntity(pos, ... ) }
        }

    }
}
val sum = { a: Int, _: Int -> {
    var a = 1
    a + b
}
}
println(sum(5, 3))  // Output: 8
let { one:S,two ->

}

apply {
    println(" ")
}
let { something ->
    println(something.toString())
}
fun something() {
    var varName: Int = 1
    varName
    val valName: Int = 2
    val valName: Int = 2
}

@OptIn(ExperimentalPathApi::class)
kotlin.io.path.fileVisitor()
@OptIn(ExperimentalEncodingApi::class)
kotlin.io.encoding.Base64.platformEncodeToByteArray
val (ability,_, _) = this.form.abilities.select(this.species, this.aspects)
fun name(map: String): Unit {
var something = ""
    run {
        var something = 1
        something
    }
}
val map = mapOf(1 to "One", 2 to "Two")
inline fun <T, R> T.let(noinline block: (T) -> R): (T) -> R {
    return block
}
var something = 1
map.forEach { key, value ->

}

abstract class ClassName {
    fun name(){
        fun (x: Any, y: Any): Unit {

        }.t
    }

}
try {
    if (true) {

    } else {

    }
} catch (e: Exception) {
    TODO("Not yet implemented")
}

try {
    3 +4
} catch (e: Exception) {
    TODO("Not yet implemented")
}

fun s(){
 return with("Hello") {
     uppercase() + " WORLD"

 }
}
var s = true
val something = 10
var varName: String = """${
s
}"""
var asw =1

var propertyName: Int = 1
    get() = field
    set(value) {
        field = value

    }
var l = if (s) {
    false
}else if (s) {
    true
}else false

