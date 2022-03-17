package checkers

import CHAT_ID
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId

class TelegramGroupChecker(private val bot: Bot, private val userId: Long) : Checker {
    companion object {
        private val successStates = setOf("creator", "administrator", "member")
    }
    override suspend fun message(): CheckerResult {
        val chatMember = bot.getChatMember(ChatId.fromId(CHAT_ID), userId)
        val isValid = chatMember.isSuccess && successStates.contains(chatMember.get().status)
        if (!isValid) {
            return CheckerFailureResult("Вы не состоите в чате")
        }
        return CheckerSuccessResult("Поздравляем вы в тг чате")
    }
}