package checkers
class BnbChecker(private val string: String) : Checker {
    override suspend fun message(): CheckerResult {
        val isValid = string.startsWith("0x") && string.length == 42
        if (!isValid) {
            return CheckerFailureResult("Неправильный формат кошелька")
        }

        return CheckerSuccessResult("")
    }
}