package arrow.examples.datatypes

import arrow.data.NonEmptyList
import arrow.data.applicative
import arrow.data.ev
import arrow.data.monad
import arrow.syntax.applicative.map
import arrow.typeclasses.binding
import java.util.*

/**
 * Source http://arrow-kt.io/docs/datatypes/nonemptylist/
 *
 * NonEmptyList is a data type used in Λrrow to model ordered lists that
 * guarantee to have at least one value. NonEmptyList is available in the
 * arrow-data module under the import arrow.data.NonEmptyList
 */
fun main(args: Array<String>) {

    // of
    // A NonEmptyList guarantees the list always has at least 1 element.
    val nel = NonEmptyList.of(1, 2, 3)

    println("Created a non-empty list with 3 elements: $nel")

    // The following will not compile
    // NonEmptyList.of()

    // head
    // Unlike List.head() NonEmptyList.head() is a safe operation that
    // guarantees that a value will be returned.
    val head = nel.head

    println("Head of list is: $head")

    // foldLeft
    // When we fold over a NonEmptyList, we turn a NonEmptyList< A > into B by
    // providing a seed value and a function that carries the state on each
    // iteration over the elements of the list. The first argument is a function
    // that addresses the seed value, this can be any object of any type which
    // will then become the resulting typed value. The second argument is a
    // function that takes the current state and element in the iteration and
    // returns the new state after transformations have been applied.
    fun sumNel(nel: NonEmptyList<Int>): Int =
        nel.foldLeft(0) { acc, n -> acc + n }

    val sum = sumNel(nel)

    println("Sum of $nel is $sum")

    // map
    // map allows us to transform A into B in NonEmptyList< A >
    val mapped = nel.map { it + 1 }

    println("mapped non-empty list is: $mapped")

    // flatMap
    // flatMap allows us to compute over the contents of multiple NonEmptyList< * > values
    val nelOne: NonEmptyList<Int> = NonEmptyList.of(1)
    val nelTwo: NonEmptyList<Int> = NonEmptyList.of(2)

    val flatMapped = nelOne.flatMap { one ->
        nelTwo.map { two ->
            one + two
        }
    }

    println("flatMap example returned: $flatMapped")

    // Monad binding
    // Λrrow allows imperative style comprehensions to make computing over
    // NonEmptyList values easy.
    val nelA: NonEmptyList<Int> = NonEmptyList.of(1)
    val nelB: NonEmptyList<Int> = NonEmptyList.of(2)
    val nelC: NonEmptyList<Int> = NonEmptyList.of(3)

    val sumExample = NonEmptyList.monad().binding {
        val one = nelA.bind()
        val two = nelB.bind()
        val three = nelC.bind()
        one + two + three
    }.ev()

    println("Sum of $nelA $nelB and $nelC using monad bindng syntax: $sumExample")

    // Monad binding in NonEmptyList and other collection related data type can
    // be used as generators

    val generatorExample = NonEmptyList.monad().binding {
        val x = NonEmptyList.of(1, 2, 3).bind()
        val y = NonEmptyList.of(1, 2, 3).bind()
        x + y
    }.ev()

    println("Monad binding generator example returned: $generatorExample")

    // Applicative Builder
    // Λrrow contains methods that allow you to preserve type information when
    // computing over different NonEmptyList typed values.
    data class Person(val id: UUID, val name: String, val year: Int)

    // Note each NonEmptyList is of a different type
    val nelId: NonEmptyList<UUID> = NonEmptyList.of(UUID.randomUUID(), UUID.randomUUID())
    val nelName: NonEmptyList<String> = NonEmptyList.of("William Alvin Howard", "Haskell Curry")
    val nelYear: NonEmptyList<Int> = NonEmptyList.of(1926, 1900)

    // Note - this produdces the product of the three input values yielding 8 Person instances.
    val applicativeExample = NonEmptyList.applicative().map(nelId, nelName, nelYear, { (id, name, year) ->
        Person(id, name, year)
    })

    println("Applicative example returned: $applicativeExample")
}