package checkers

private const val TWITTER_PREFIX = "twitter.com/"
private const val MESSAGE_TWITTER_INVALID_USERNAME = "Вы ввели невалидный юзернейм твиттера"

class TwitterChecker(private val inputUrl: String) : Checker {
    // https://twitter.com/keklik
    // twitter.com/keklik
    // @keklik
    override suspend fun message(): CheckerResult {
        // TODO: здесь должна быть нормальная валидация на то что ретвит сделан
        val username = getUsername() ?: return CheckerFailureResult(MESSAGE_TWITTER_INVALID_USERNAME)
        return CheckerSuccessResult("Сделаем вид, что у вас все норм username: $username =)")
    }

    private fun getUsername(): String? {
        var res = inputUrl
        res = res.substringAfter('@')
        res = res.substringAfter(TWITTER_PREFIX)
        if (res.length > 15) return null
        return res
    }
}