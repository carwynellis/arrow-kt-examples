package arrow.examples.datatypes

import arrow.core.*
import arrow.data.NonEmptyList
import arrow.data.Validated
import arrow.syntax.either.left
import arrow.syntax.either.right
import arrow.syntax.validated.invalid
import arrow.syntax.validated.valid

/**
 * Source http://arrow-kt.io/docs/datatypes/validated/
 *
 * Imagine you are filling out a web form to sign up for an account. You input
 * your username and password and submit. Response comes back saying your
 * username can’t have dashes in it, so you make some changes and resubmit.
 * Can’t have special characters either. Change, resubmit. Passwords need to
 * have at least one capital letter. Change, resubmit. Password needs to have
 * at least one number.
 */
fun main(args: Array<String>) {

    // Or perhaps you’re reading from a configuration file. One could imagine
    // the configuration library you’re using returns a Try, or maybe a Either.
    // Your parsing may look something like:

    data class ConnectionParams(val url: String, val port: Int)

    fun <A> config(key: String): Either<String, A> = TODO()

    val connectionParams by lazy {
        config<String>("url").flatMap { url ->
            config<Int>("port").map { ConnectionParams(url, it) }
        }
    }

    // You run your program and it says key “url” not found, turns out the key
    // was “endpoint”. So you change your code and re-run. Now it says the
    // “port” key was not a well-formed integer.

    // It would be nice to have all of these errors reported simultaneously.
    // That the username can’t have dashes can be validated separately from it
    // not having special characters, as well as from the password needing to
    // have certain requirements. A misspelled (or missing) field in a config
    // can be validated separately from another field not being well-formed.

    // Enter Validated.

    // Parallel Validation

    // Our goal is to report any and all errors across independent bits of data.
    // For instance, when we ask for several pieces of configuration, each
    // configuration field can be validated separately from one another. How
    // then do we enforce that the data we are working with is independent? We
    // ask for both of them up front.

    // As our running example, we will look at config parsing. Our config will
    // be represented by a Map<String, String>. Parsing will be handled by a
    // Read type class - we provide instances only for String and Int for
    // brevity.

    // See abstract class Read<A>

    // Then we enumerate our errors - when asking for a config value, one of two
    // things can go wrong: the field is missing, or it is not well-formed with
    // regards to the expected type.

    // See sealed class ConfigError

    // We need a data type that can represent either a successful value (a
    // parsed configuration), or an error.

    // Arrow provides in arrow.data.Validated

    // Now we are ready to write our parser.

    data class Config(val map: Map<String, String>) {

        fun <A> parse(read: Read<A>, key: String): Validated<ConfigError, A> {
            val v = Option.fromNullable(map[key])
            return when(v) {
                is Some -> {
                    val s = read.read(v.t)
                    when(s) {
                        is Some -> s.t.valid()
                        is None -> ConfigError.ParseConfig(key).invalid()
                    }
                }
                is None -> Validated.Invalid(ConfigError.MissingConfig(key))
            }
        }

    }

    // Everything is in place to write the parallel validator. Recall that we
    // can only do parallel validation if each piece is independent. How do we
    // enforce the data is independent? By asking for all of it up front. Let’s
    // start with two pieces of data.

    fun <E, A, B, C> parallelValidate1(v1: Validated<E, A>,
                                       v2: Validated<E, B>,
                                       f: (A, B) -> C): Validated<E, C> {
        return when {
            v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
            v1 is Validated.Valid && v2 is Validated.Invalid -> v2
            v1 is Validated.Invalid && v2 is Validated.Valid -> v1
            v1 is Validated.Invalid && v2 is Validated.Invalid -> TODO()
            else -> TODO()
        }
    }

    // We’ve run into a problem. In the case where both have errors, We want to
    // report both. we don’t have a way to combine ConfigErrors. But as clients,
    // we can change our Validated values where the error can be combined, say,
    // a List<ConfigError>.

    // We are going to use a NonEmptyList<ConfigError> — the NonEmptyList
    // statically guarantees we have at least one value, which aligns with the
    // fact that if we have an Invalid, then we most certainly have at least one
    // error. This technique is so common there is a convenient method on
    // Validated called toValidatedNel that turns any Validated<E, A> value to a
    // Validated<NonEmptyList<E>, A>. Additionally, the type alias
    // ValidatedNel<E, A> is provided.

    // Time to parse.

    fun <E, A, B, C> parallelValidate2(v1: Validated<E, A>,
                                       v2: Validated<E, B>,
                                       f: (A, B) -> C): Validated<NonEmptyList<E>, C> {
        return when {
            v1 is Validated.Valid && v2 is Validated.Valid -> Validated.Valid(f(v1.a, v2.a))
            v1 is Validated.Valid && v2 is Validated.Invalid -> v2.toValidatedNel()
            v1 is Validated.Invalid && v2 is Validated.Valid -> v1.toValidatedNel()
            v1 is Validated.Invalid
                && v2 is Validated.Invalid -> Validated.Invalid(NonEmptyList(v1.e, listOf(v2.e)))
            else -> throw IllegalStateException("Not possible value")
        }
    }

    // Kotlin says that our match is not exhaustive and we have to add else.

    // When no errors are present in the configuration, we get a
    // ConnectionParams wrapped in a Valid instance.

    fun validateConfig(config: Config) = parallelValidate2(
        config.parse(Read.stringRead, "url"),
        config.parse(Read.intRead, "port")
    ) { url, port ->
        ConnectionParams(url, port)
    }

    val validConfig = Config(mapOf("url" to "127.0.0.1", "port" to "1337"))
    val validResult = validateConfig(validConfig)

    println("Result of validating valid config: $validResult")

    // But what happens when having one or more errors?

    // They are accumulated in a NonEmptyList wrapped in an Invalid instance.

    val invalidConfig = Config(mapOf("url" to "127.0.0.1", "port" to "not a number"))

    val invalid = validateConfig(invalidConfig)

    println("Result of validating invalid config: $invalid")

    // Sequential Validation

    // If you do want error accumulation but occasionally run into places where
    // sequential validation is needed, then Validated provides withEither
    // method to allow you to temporarily turn a Validated instance into an
    // Either instance and apply it to a function.

    fun positive(field: String, i: Int): Either<ConfigError, Int> {
        return if (i >= 0) i.right()
        else ConfigError.ParseConfig(field).left()
    }

    val invalidHouseNumber = Config(mapOf("house_number" to "-42"))

    val houseNumberResult = invalidHouseNumber.parse(Read.intRead, "house_number").withEither { either ->
        either.flatMap { positive("house_number", it) }
    }

    println("houseNumberResult of validation withEither: $houseNumberResult")
}

abstract class Read<A> {

    abstract fun read(s: String): Option<A>

    companion object {

        val stringRead: Read<String> =
            object: Read<String>() {
                override fun read(s: String): Option<String> = Option(s)
            }

        val intRead: Read<Int> =
            object: Read<Int>() {
                override fun read(s: String): Option<Int> =
                    if (s.matches(Regex("-?[0-9]+"))) Option(s.toInt()) else None
            }

    }

}

sealed class ConfigError {
    data class MissingConfig(val field: String): ConfigError()
    data class ParseConfig(val field: String): ConfigError()
}


