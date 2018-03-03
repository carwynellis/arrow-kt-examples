package arrow.examples.datatypes

import arrow.data.*
import arrow.typeclasses.binding

/**
 * Source http://arrow-kt.io/docs/datatypes/sequenceK/
 *
 * SequenceK implements lazy lists representing lazily-evaluated ordered
 * sequence of homogenous values.
 *
 * Note - it looks like SequenceK is now SequenceKW
 */
fun main(args: Array<String>) {

    // It can be created from Kotlin Sequence type with a convenient k()
    // function.
    val someSeq: SequenceKW<Int> = sequenceOf(1, 2, 3).k()

    println("Created sequenceK: $someSeq")

    // SequenceK derives many useful typeclasses. For instance, it has a
    // SemigroupK instance.
    val hello = sequenceOf('h', 'e', 'l', 'l', 'o').k()
    val commaSpace = sequenceOf(',', ' ').k()
    val world = sequenceOf('w', 'o', 'r', 'l', 'd').k()

    val sequencesEqual = hello.combineK(commaSpace.combineK(world)).toList() ==
            hello.combineK(commaSpace).combineK(world).toList()

    println("Hello world sequences are equal: $sequencesEqual")

    // Functor
    // Transforming a sequence.
    val fibonacci = generateSequence(0 to 1) {
        it.second to it.first + it.second
    }.map { it.first }.k()

    val doubledFibonacci = fibonacci.map { it * 2 }.takeWhile { it < 10 }.toList()

    println("Generated doubled fibonacci sequence: $doubledFibonacci")

    // Applicative
    // Applying a sequence of functions to a sequence:
    val applicativeResult =
        SequenceKW.applicative().ap(sequenceOf(1, 2, 3).k(), sequenceOf({ x: Int -> x + 1}, { x: Int -> x * 2}).k()).toList()

    println("Applicative example generated: $applicativeResult")

    // SequenceK is a Monad too.
    // For example, it can be used to model non-deterministic computations.
    // (In a sense that the computations return an arbitrary number of results.)
    val positive = generateSequence(1) { it + 1 }.k() // sequence of positive numbers
    val positiveEven = positive.filter { it % 2 == 0 }.k()

    val monadExample = SequenceKW.monad().binding {
        val p = positive.bind()
        val pe = positiveEven.bind()
        p + pe
    }.ev().take(5).toList()

    println("Monad example generated: $monadExample")

    // Folding a sequence,
    val foldExample = sequenceOf('a', 'b', 'c', 'd', 'e').k().foldLeft("") { x, y -> x + y }

    println("Fold example generated: $foldExample")
}