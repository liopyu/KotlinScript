
import java.io.File
import kotlin.math.abs
import kotlin.system.measureNanoTime
apply {}
takeUnless {}
to {}

kotlin.sequences.filterNotNull
File("").walkBottomUp()
walkBottomUp
kotlin.text.isLowerCase
throwCountOverflow()
terminateCollectionToArray
Grouping
iterator()
abs()
kotlin.comparisons.compareBy
kotlin.sequences.groupingBy()
kotlin.time.saturatingAdd()
kotlin.time.saturatingAdd
kotlin.io.path.insecureEnterDirectory
measureNanoTime()
arrayListOf()
setOf()
List<String>(1,)
UInt.hashCode
ArrayList
RandomAccess
ByteIterator
Pair
Result.success()
DeprecationLevel
AbstractList.checkBoundsIndexes()
kotlin.ulongToString
AbstractMutableSet()
readln
arrayListOf()
arrayOfNulls<>()
kotlin.coroutines.minusPolymorphicKey
toString()
kotlin.io.readln
emptyArray<>()
arrayOf()
printWriter()
10.toBigInteger()
synchronized
assert
kotlin.collections.mapOf<String,String>()
ulongDivide
var a = [].withIndex()
listOf()
kotlin.Result.equals()
hashSetOf<String>()
assert()
arrayOf()
arrayListOf<>()
arrayOfNulls<>()
uintArrayOf()
buildMap<>()
byteArrayOf()
booleanArrayOf()
buildList<>()
buildSet<>()
_Assertions
iterator
DeprecationLevel()
to
buildString()
compareBy<>()
charArrayOf()
charset()
check()
checkNotNull()
compareByDescending<>()
compareValues()
compareValuesBy()
doubleArrayOf()
error()
equals()
emptyList<>()
emptyArray<>()
emptyMap<>()
emptySequence<>()
emptySet<>()
enumValueOf<>()
enumValues<>()
floatArrayOf()
generateSequence()
hashSetOf<>()
hashCode()
hashMapOf<Int,String>()
intArrayOf()
iterator<Int> {  }
listOf()
SlidingWindowKt.checkWindowSizeStep
kotlin.doubleToUInt
UArraySortingKt.sortArray
AbstractMap.access
ListBuilderKt.arrayOfUninitializedElements
AbstractList.checkBoundsIndexes
AbstractMap.entryEquals
AbstractList.orderedHashCode$kotlin_stdlib
kotlin.createFailure
kotlin.uintRemainder
kotlin.TODO()
linkedMapOf()
linkedSetOf()
listOfNotNull()
longArrayOf()
kotlin.ulongDivide()
kotlin.ulongDivide-eb3DHEI(long, long)
kotlin.uintCompare(int, int)
mapOf()
mutableMapOf<>()
mutableListOf()
mutableSetOf()
nullsLast<>()
nullsFirst<>()
naturalOrder<>()
println()
print()
readln()
kotlin.assert()
run {  }
runCatching {  }
TODO()
AbstractList

kotlin.emptyArray()
kotlin.emptyArray
kotlin.Exception
AbstractSet.setEquals

IllegalStateException
IllegalStateException
ArithmeticException
OverloadResolutionByLambdaReturnType
emptyArray
AbstractMutableSet
with()

fun main() {
    // Create a sequence containing nullable integers
    val numbers: Sequence<Int?> = sequenceOf(1, null, 2, 3, null, 4, 5, null)

    // Use the filterNotNull extension to remove nulls from the sequence
    val nonNullNumbers: Sequence<Int> = numbers.filterNotNull()

    // Print each non-null number
    nonNullNumbers.forEach { println(it) }
}
to {}
val result = 1 apply 1
fun main() {
    val value = 64
    // Use infix notation to shift 'value' right by 2 bits.
    val result = value shr 2
    println("Result of 64 shr 2: $result")
}
