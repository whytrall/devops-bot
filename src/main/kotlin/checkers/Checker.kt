package checkers

abstract class CheckerResult(val message: String)
class CheckerSuccessResult(message: String) : CheckerResult(message)
class CheckerFailureResult(message: String) : CheckerResult(message)
interface Checker {
    suspend fun message(): CheckerResult
}