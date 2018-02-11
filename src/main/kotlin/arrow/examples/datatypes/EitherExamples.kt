package arrow.examples.datatypes

import arrow.core.*
import arrow.syntax.applicative.tupled
import arrow.syntax.either.left
import arrow.syntax.either.right
import arrow.typeclasses.binding

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

    // Instead, let’s make the fact that some of our functions can fail explicit
    // in the return type.

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

    // In the following exercise we pattern-match on every case the Either
    // returned by magic can be in. Note the when clause in the Left - the
    // compiler will complain if we leave that out because it knows that given
    // the type Either[Exception, String], there can be inhabitants of Left that
    // are not NumberFormatException or IllegalArgumentException. You should
    // also notice that we are using SmartCast for accessing to Left and Right
    // value.

    val x = eitherStyle.magic("2")

    val value = when(x) {
        is Either.Left -> when (x.a){
            is NumberFormatException -> "Not a number!"
            is IllegalArgumentException -> "Can't take reciprocal of 0!"
            else -> "Unknown error"
        }
        is Either.Right -> "Got reciprocal: ${x.b}"
    }

    println("magic('2') returned reciprocal of $value")

    // Instead of using exceptions as our error value, let’s instead enumerate
    // explicitly the things that can go wrong in our program.

    val eitherWithAdtStyle = object {
        fun parse(s: String): Either<Error, Int> =
            if (s.matches(Regex("-?[0-9]+"))) Either.Right(s.toInt())
            else Either.Left(Error.NotANumber)

        fun reciprocal(i: Int): Either<Error, Double> =
            if (i == 0) Either.Left(Error.NoZeroReciprocal)
            else Either.Right(1.0 / i)

        fun stringify(d: Double): String = d.toString()

        fun magic(s: String): Either<Error, String> =
            parse(s).flatMap{reciprocal(it)}.map{stringify(it)}
    }

    // For our little module, we enumerate any and all errors that can occur.
    // Then, instead of using exception classes as error values, we use one of
    // the enumerated cases. Now when we pattern match, we get much nicer
    // matching. Moreover, since Error is sealed, no outside code can add
    // additional subtypes which we might fail to handle.

    val y = eitherWithAdtStyle.magic("2")

    when(y) {
        is Either.Left -> when (y.a){
            is Error.NotANumber -> "Not a number!"
            is Error.NoZeroReciprocal -> "Can't take reciprocal of 0!"
        }
        is Either.Right -> "Got reciprocal: ${y.b}"
    }

    println("magic('2') using Either/ADT style returned reciprocal of $value")

    // Additional Syntax

    // Either can also map over the left value with mapLeft which is similar to
    // map but applies on left instances.

    val r : Either<Int, Int> = Either.Right(7)
    val rMapLeft = r.mapLeft{it +1}
    println("mapping a right with mapLeft returns: $rMapLeft")

    val l: Either<Int, Int> = Either.Left(7)
    val lMapLeft = l.mapLeft{it + 1}
    println("mapping a left with mapLeft returns: $lMapLeft")

    // Either<A, B> can be transformed to Either<B,A> using the swap() method.

    val r2: Either<String, Int> = Either.Right(7)
    val r2Swapped = r2.swap()
    println("swapping $r2 returns: $r2Swapped")

    // For using Either’s syntax on arbitrary data types arrow provides
    // additional syntax which can be imported. This provides left(), right(),
    // contains() and getOrElse() methods.

    val rightOfNumber = 7.right()

    println("7.right() returns: $rightOfNumber")

    val leftOfString = "hello".left()

    println("\"hello\".left() returns $leftOfString")

    val rightContainsSeven = rightOfNumber.contains(7)

    println("7.right() contains 7: $rightContainsSeven")

    val getOrElseOfLeft = leftOfString.getOrElse { 7 }

    println("getOrElse on left returned: $getOrElseOfLeft")

    // To create an Either instance using a predicate, use the Either.cond()
    // method.
    val predicateTrue = Either.cond(true, { 42 }, { "Error" })

    println("Either.cond with predicate true returned: $predicateTrue")

    val predicateFalse = Either.cond(true, { 42 }, { "Error" })

    println("Either.cond with predicate false returned: $predicateFalse")

    // Another operation is fold. This operation will extract the value from
    // the Either, or provide a default if the value is Left.

    val foldOnRight = 7.right().fold( { 1 }, { it * 2 })

    println("foldOnRight returned: $foldOnRight")

    // TODO - this only worked with explicit type on multiply function.
    val foldOnLeft = 7.left().fold( { 1 }, { n: Int -> n * 2 })

    println("foldOnLeft returned $foldOnLeft")

    // The getOrHandle() operation allows the transformation of an Either.Left
    // value to a Either.Right using the value of Left. This can be useful when
    // a mapping to a single result type is required like fold() but without the
    // need to handle Either.Right case.

    // As an example we want to map an Either<Int, Throwable> to a proper HTTP
    // status code:

    val leftException: Either<Throwable, Int> = Either.Left(NumberFormatException())
    val httpStatusCode = leftException.getOrHandle {
        when(it) {
            is NumberFormatException -> 400
            else -> 500
        }
    }

    println("left.getOrHandle returned: $httpStatusCode")

    // Arrow contains Either instances for many useful typeclasses that
    // allows you to use and transform right values. Both Option and Try don’t
    // require a type parameter with the following functions, but it is
    // specifically used for Either.Left

    // Functor

    // Transforming the inner contents

    val eitherFunctor = Either.functor<Int>().map(Either.Right(1), {it + 1})

    println("Either.functor example returned: $eitherFunctor")

    // Applicative

    // Computing over independent values

    val eitherApplicative = Either.applicative<Int>()
        .tupled(Either.Right(1), Either.Right("a"), Either.Right(2.0))

    println("Either.applicative example returned: $eitherApplicative")

    // Monad

    // Computing over dependent values ignoring absence

    val eitherMonad = Either.monad<Int>().binding {
        val a = Either.Right(1).bind()
        val b = Either.Right(1 + a).bind()
        val c = Either.Right(1 + b).bind()
        a + b + c
    }

    println("Either.monad example returned: $eitherMonad")
}

// Error class for Either with ADT example.
sealed class Error {
    object NotANumber : Error()
    object NoZeroReciprocal : Error()
}

