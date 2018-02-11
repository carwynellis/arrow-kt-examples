package arrow.examples.datatypes

import arrow.data.*
import arrow.syntax.applicative.tupled
import arrow.typeclasses.binding

/**
 * Source http://arrow-kt.io/docs/datatypes/try/
 *
 * Arrow has lots of different types of error handling and reporting, which
 * allows you to choose the best strategy for your situation.
 *
 * For example, we have Option to model the absence of a value, or Either to
 * model the return of a function as a type that may have been successful, or
 * may have failed.
 *
 * On the other hand, we have Try, which represents a computation that can
 * result in an A result (as long as the computation is successful) or in an
 * exception if something has gone wrong.
 *
 * That is, there are only two possible implementations of Try: a Try instance
 * where the operation has been successful, which is represented as Success<A>;
 * or a Try instance where the computation has failed with a Throwable, which is
 * represented as Failure<A>.
 *
 * With just this explanation you might think that we are talking about an
 * Either<Throwable, A>, and you are not wrong. Try can be implemented in terms
 * of Either, but its use cases are very different.
 */
fun main(args: Array<String>) {

    // If we know that an operation could result in a failure, for example,
    // because it is code from a library over which we have no control, or
    // better yet, some method from the language itself. We can use Try as a
    // substitute for the well-known try-catch, allowing us to rise to all its
    // goodness.

    // The following example represents the typical case when consuming Java
    // code, where domain errors are represented with exceptions.

    open class GeneralException: Exception()

    class NoConnectionException: GeneralException()

    class AuthorizationException: GeneralException()

    fun checkPermissions() {
        throw AuthorizationException()
    }

    fun getLotteryNumbersFromCloud(): List<String> {
        throw NoConnectionException()
    }

    fun getLotteryNumbers(): List<String> {
        checkPermissions()

        return getLotteryNumbersFromCloud()
    }

    // The traditional way to control this would be to use a try-catch block,
    // as we have said before:
    try {
        getLotteryNumbers()
    } catch (e: NoConnectionException) {
        println("Caught NoConnectionException")
    } catch (e: AuthorizationException) {
        println("Caught AuthorizationException")
    }

    // However, we could use Try to retrieve the computation result in a much
    // cleaner way:
    val lotteryTry = Try { getLotteryNumbers() }

    println("Try returned: $lotteryTry")

    // By using getOrDefault we can give a default value to return, when the
    // computation fails, similar to what we can also do with Option when there
    // is no value:
    val tryWithGetOrDefault = lotteryTry.getOrDefault { emptyList() }

    println("Try result using getOrDefault: $tryWithGetOrDefault")

    // If the underlying failure is useful to determine the default value,
    // getOrElse can be used:
    val tryWithGetOrElse = lotteryTry.getOrElse { ex: Throwable -> emptyList() }

    println("Try result using getOrElse: $tryWithGetOrElse")

    // getOrElse can generally be used anywhere getOrDefault is used, ignoring
    // the exception if it’s not needed:
    lotteryTry.getOrElse { emptyList() }

    // If you want to perform a check on a possible success, you can use filter
    // to convert successful computations in failures if conditions aren’t met:
    val tryWithFilter = lotteryTry.filter { it.size < 4 }

    println("Try result using filter: $tryWithFilter")

    // We can also use recover which allow us to recover from a particular error
    // (we receive the error and have to return a new value):
    val tryWithRecover = lotteryTry.recover { exception -> emptyList() }

    println("Try result using recover: $tryWithRecover")

    // Or if you have another different computation that can also fail, you can
    // use recoverWith to recover from an error (as you do with recover, but in
    // this case, returning a new Try):

    fun getLotteryNumbers(source: Source): List<String> {
        println("getLotteryNumbers trying source: $source")

        checkPermissions()

        return getLotteryNumbersFromCloud()
    }

    val tryWithRecoverWith = Try { getLotteryNumbers(Source.NETWORK) }.recoverWith {
        Try { getLotteryNumbers(Source.CACHE) }
    }

    println("Try result using recoverWith: $tryWithRecoverWith")

    // When you want to handle both cases of the computation you can use fold.
    // With fold we provide two functions, one for transforming a failure into
    // a new value, the second one to transform the success value into a new
    // one:
    val tryWithFold = lotteryTry.fold(
        { emptyList<String>() },
        { it.filter { it.toIntOrNull() != null } })

    println("Try result using fold: $tryWithFold")

    // Or, as we have with recoverWith, we can use a version of fold which
    // allows us to handle both cases with functions that return a new instance
    // of Try, transform:
    val tryWithTransform = lotteryTry.transform(
        { Try { it.map { it.toInt() } } },
        { Try.pure(emptyList<Int>()) })

    println("Try result using transform: $tryWithTransform")

    // Lastly, Arrow contains Try instances for many useful typeclasses that
    // allows you to use and transform fallibale values:

    // Functor

    // Transforming the value, if the computation is a success:

    val functorResult = Try.functor().map(Try { "3".toInt() }, { it + 1})

    println("functorResult: $functorResult")

    // Applicative

    // Computing over independent values:

    val successfulApplicativeResult = Try.applicative().tupled(
        Try { "3".toInt() },
        Try { "5".toInt() },
        Try { "7".toInt() }
    )

    println("successfulApplicativeResult: $successfulApplicativeResult")

    val failedApplicativeResult = Try.applicative().tupled(
        Try { "3".toInt() },
        Try { "5".toInt() },
        Try { "nope".toInt() }
    )

    println("failedApplicativeResult: $failedApplicativeResult")

    // Monad

    // Computing over dependent values ignoring failure:

    val successfulMonadResult = Try.monad().binding {
        val a = Try { "3".toInt() }.bind()
        val b = Try { "4".toInt() }.bind()
        val c = Try { "5".toInt() }.bind()

        a + b + c
    }

    println("successfulMonadResult: $successfulMonadResult")

    val failedMonadresult = Try.monad().binding {
        val a = Try { "none".toInt() }.bind()
        val b = Try { "4".toInt() }.bind()
        val c = Try { "5".toInt() }.bind()

        a + b + c
    }

    println("failedMonadResult: $failedMonadresult")
}

enum class Source {
    CACHE, NETWORK
}
