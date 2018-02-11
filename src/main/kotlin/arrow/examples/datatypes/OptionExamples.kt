package arrow.examples.datatypes

import arrow.core.*
import arrow.syntax.applicative.tupled
import arrow.syntax.option.*
import arrow.typeclasses.binding

/**
 * Source http://arrow-kt.io/docs/datatypes/option/
 *
 * Arrow models the absence of values through the Option datatype similar to how
 * Scala, Haskell and other FP languages handle optional values.
 *
 * Option<A> is a container for an optional value of type A. If the value of
 * type A is present, the Option<A> is an instance of Some<A>, containing the
 * present value of type A. If the value is absent, the Option<A> is the object
 * None.
 */
fun main(args: Array<String>) {

    // Create an Option populated with a value.
    val someValue: Option<String> = Some("I am wrapped in something")

    println("someValue: $someValue")

    // Create an empty Option.
    val emptyValue: Option<String> = None

    println("emptyValue: $emptyValue")

    // Let's write a function that may or may not return a string, thus
    // returning Option<String>
    fun maybeItWillReturnSomething(flag: Boolean): Option<String> =
        if (flag) Some("Found value") else None

    // Using getOrElse we can provide a default value "No value" when the
    // optional argument None does not exist.
    val value1 = maybeItWillReturnSomething(true).getOrElse { "No value" }
    val value2 = maybeItWillReturnSomething(false).getOrElse { "No value" }

    println("value1: $value1")
    println("value2: $value2")

    // We can also check if an option has a value or not.
    if (emptyValue.isEmpty()) {
        println("emptyValue is empty")
    }

    if (someValue.isDefined()) {
        println("someValue is not empty")
    }

    // Option can also be used with when statements.
    // Note - here we must explicily declare the type as Option<Double> in
    //        order to allow matching for Some or None.
    val anotherValue: Option<Double> = Some(20.0)
    val value = when(anotherValue) {
        is Some -> anotherValue.t
        is None -> 0.0
    }
    println("value of $anotherValue is $value")

    // An alternative for pattern matching is performing Functor/Foldable style
    // operations. This is possible because an option could be looked at as a
    // collection or foldable structure with either one or zero elements.

    // One of these operations is map. This operation allows us to map the inner
    // value to a different type while preserving the option:
    val number: Option<Int> = Some(3)
    val noNumber: Option<Int> = None
    val mappedResult1 = number.map { it * 1.5 }
    val mappedResult2 = noNumber.map { it * 1.5 }

    println("mappedResult1: $mappedResult1")
    println("mappedResult2: $mappedResult2")

    // Another operation is fold. This operation will extract the value from the
    // option, or provide a default if the value is None
    val numberFold = number.fold({ 1 }, { it * 3 })
    val noNumberFold = noNumber.fold({ 1 }, { it * 3 })

    println("numberFold: $numberFold")
    println("noNumberFold: $noNumberFold")

    // Arrow also adds syntax to all datatypes so you can easily lift them into
    // the context of Option where needed.
    val someOne = 1.some()
    val noString = none<String>()

    println("someOne: $someOne")
    println("noString: $noString")

    // Arrow contains Option instances for many useful typeclasses that allows
    // you to use and transform optional values

    // Functor - Transforming the inner contents
    val functorResult = Option.functor().map(Some(1), { it + 1 })

    println("functorResult: $functorResult")

    // Applicative - Computing over independent values
    val applicativeResult = Option.applicative().tupled(Some(1), Some("Hello"), Some(20.0))

    println("applicativeResult: $applicativeResult")

    // Monad - Computing over dependent values ignoring absence
    val monadWithNoAbsences = Option.monad().binding {
        val a = Some(1).bind()
        val b = Some(1 + a).bind()
        val c = Some(1 + b).bind()
        a + b + c
    }

    println("monadWithNoAbsences: $monadWithNoAbsences")

    val monadWithAbsence = Option.monad().binding {
        val x = none<Int>().bind()
        val y = Some(1 + x).bind()
        val z = Some(1 + y).bind()
        x + y + z
    }

    println("monadWithAbsence: $monadWithAbsence")
}
