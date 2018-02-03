package uk.carwynellis.arrowexamples.datatypes

import arrow.*
import arrow.core.*

/**
 * Source http://arrow-kt.io/docs/datatypes/either/
 *
 * In day-to-day programming, it is fairly common to find ourselves writing
 * functions that can fail. For instance, querying a service may result in a
 * connection issue, or some unexpected JSON response.
 *
 * To communicate these errors it has become common practice to throw
 * exceptions. However, exceptions are not tracked in any way, shape, or form
 * by the compiler. To see what kind of exceptions (if any) a function may
 * throw, we have to dig through the source code. Then to handle these
 * exceptions, we have to make sure we catch them at the call site. This all
 * becomes even more unwieldy when we try to compose exception-throwing
 * procedures.
 */
fun main(args: Array<String>) {

    val throwsSomeStuff: (Int) -> Double = {x -> x.toDouble()}
    val throwsOtherThings: (Double) -> String = {x -> x.toString()}
    val moreThrowing: (String) -> List<String> = {x -> listOf(x) }
    val magic = throwsSomeStuff.andThen(throwsOtherThings).andThen(moreThrowing)

    val result = magic(12)

    println("passing 12 to magic returned: $result")

    // Assume we happily throw exceptions in our code. Looking at the types, any
    // of those functions can throw any number of exceptions, we don’t know.
    // When we compose, exceptions from any of the constituent functions can be
    // thrown. Moreover, they may throw the same kind of exception (e.g.
    // IllegalArgumentException) and thus it gets tricky tracking exactly where
    // that exception came from.

    // How then do we communicate an error? By making it explicit in the data
    // type we return.

    // Either vs Validated

    // In general, Validated is used to accumulate errors, while Either is used
    // to short-circuit a computation upon the first error. For more
    // information, see the Validated vs Either section of the Validated
    // documentation.

    // More often than not we want to just bias towards one side and call it a
    // day - by convention, the right side is most often chosen.

    val right: Either<String, Int> = Either.Right(5)

    val left: Either<String, Int> = Either.Left("Something went wrong")

    // Because Either is right-biased, it is possible to define a Monad instance
    // for it.

    // Since we only ever want the computation to continue in the case of Right
    // (as captured by the right-bias nature), we fix the left type parameter
    // and leave the right one free.

    // So the flatMap method is right-biased.

    val rightFlatMapped = right.flatMap { Either.right { it + 1 } }

    println("flatMap of $right returned: $rightFlatMapped")

    val leftFlatMapped = left.flatMap { Either.right { it + 1 } }

    println("flatMap of $left returned: $leftFlatMapped")

    // Using Either instead of exceptions

    // As a running example, we will have a series of functions that will parse
    // a string into an integer, take the reciprocal, and then turn the
    // reciprocal into a string.

    // In exception-throwing code, we would have something like this.

    // Exception Style

    val exceptionStyle = object {
        fun parse(s: String): Int =
            if (s.matches(Regex("-?[0-9]+"))) s.toInt()
            else throw NumberFormatException("$s is not a valid integer.")

        fun reciprocal(i: Int): Double =
            if (i == 0) throw IllegalArgumentException("Cannot take reciprocal of 0.")
            else 1.0 / i

        fun stringify(d: Double): String = d.toString()
    }

    println("Exception Style")
    println("string '12' parsed to int returns ${exceptionStyle.parse("12")}")
    println("reciprocal of 12 is ${exceptionStyle.reciprocal(12)}")
    println("stringify of 12.0 returns ${exceptionStyle.stringify(12.0)}")

    // Instead, let’s make the fact that some of our functions can fail explicit in the return type.

    // Either Style

    val eitherStyle = object {
        fun parse(s: String): Either<NumberFormatException, Int> =
            if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
            else Either.Left(NumberFormatException("$s is not a valid integer."))

        fun reciprocal(i: Int): Either<IllegalArgumentException, Double> =
            if (i == 0) Either.Left(IllegalArgumentException("Cannot take reciprocal of 0."))
            else Either.Right(1.0 / i)

        fun stringify(d: Double): String = d.toString()

        fun magic(s: String): Either<Exception, String> =
            parse(s).flatMap{reciprocal(it)}.map{stringify(it)}

    }

    println("Either Style")
    // Calls to parse return a Left or Right value depending on whether the
    // parse succeeded or not.
    println("parse of 12 returns ${eitherStyle.parse("12")}")
    println("parse of 'foo' returns ${eitherStyle.parse("foo")}")

    // Using combinators like flatMap and map we can compose our functions
    // together.
    println("magic of 0 returns: ${eitherStyle.magic("0")}")
    println("magic of 1 returns: ${eitherStyle.magic("1")}")
    println("magic of 'foo' returns: ${eitherStyle.magic("foo")}")
}