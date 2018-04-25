package leia.logic

// The rule matches and the
data class LogAppend(
    val sinkDescription: SinkDescription,
    val request: IncomingRequest,
    val receipt: Receipt
) : Match()

sealed class Result {
    abstract fun combine(other: Result): Result
}

// The incoming request doesn't match this particular rule
object NoMatch : Result() {
    override fun combine(other: Result): Result = other
}

// The incoming request did match this rule
sealed class Match : Result() {
    // Match + Match = Undefined
    // Match + NoMatch = Match
    // Match + ErrorMatch = Match
    override fun combine(other: Result) =
        if (other is Match) {
            throw UnsupportedOperationException("we cant handle this yet")
        } else this
}

// There is a match but it doesn't satisify the full rules engine
sealed class ErrorMatch : Match() {
    override fun combine(other: Result): Match =
        when(other) {
            is Match -> other
            is ErrorMatch -> throw UnsupportedOperationException("we cant handle this yet")
            is NoMatch -> this
        }
}
// The rule matches but authorization is missing but is required
object NotAuthorzied: ErrorMatch()
// The rule matches but authorization presented is invalid
object Forbidden: ErrorMatch()

object CorsNotAllowed : ErrorMatch()
