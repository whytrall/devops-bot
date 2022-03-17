import checkers.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import io.github.crackthecodeabhi.kreds.connection.KredsClient
class StateMachine(private val state: State?, private val redisClient: KredsClient) {
    suspend fun execute(bot: Bot, message: Message) {
        val from = message.chat.id
        val text = message.text
        when (state) {
            State.Started -> {
                setStateForUser(redisClient, from, State.WaitPhone)
                bot.sendMessage(message.chat.id.toChatId(), "Для продолжения поделитесь своим номером телефона (он не будет сохранен)",
                    replyMarkup = KeyboardReplyMarkup(KeyboardButton("Поделиться", requestContact = true), oneTimeKeyboard = true))
            }

            State.WaitPhone -> {
                when (val checker = PhoneChecker(message, from, redisClient).message()) {
                    is CheckerSuccessResult -> {
                        bot.sendMessage(message.chat.id.toChatId(), MESSAGE_ENTER_BNB)
                        setStateForUser(redisClient, from, State.WaitBnb)
                    }
                    is CheckerFailureResult -> {
                        bot.sendMessage(message.chat.id.toChatId(), checker.message,
                            replyMarkup = KeyboardReplyMarkup(KeyboardButton("Поделиться", requestContact = true), oneTimeKeyboard = true))
                    }
                }
            }

            State.WaitBnb -> {
                if (text == null) return
                when (val result = BnbChecker(text).message()) {
                    is CheckerSuccessResult -> {
                        redisClient.set("${from}_BNB", text)
                        setStateForUser(redisClient, from, State.WaitJoinTg)
                        bot.sendMessage(message.chat.id.toChatId(), MESSAGE_JOIN_TG, replyMarkup = InlineKeyboardMarkup.create(
                            listOf(InlineKeyboardButton.CallbackData("Проверить", "check_tg"))
                        ))
                    }
                    is CheckerFailureResult -> {
                        bot.sendMessage(message.chat.id.toChatId(), result.message)
                    }
                }
            }

            State.WaitJoinTg -> {
                when (val result = TelegramGroupChecker(bot, from).message()) {
                    is CheckerSuccessResult -> {
                        setStateForUser(redisClient, from, State.WaitJoinTwitter)
                        bot.sendMessage(message.chat.id.toChatId(), MESSAGE_JOIN_TWITTER)
                    }
                    is CheckerFailureResult -> {
                        bot.sendMessage(
                            message.chat.id.toChatId(), "Вступите в чат и попробуйте еще раз", replyMarkup =
                            InlineKeyboardMarkup.createSingleButton(InlineKeyboardButton.CallbackData("Попробовать еще раз", "check_tg")))
                    }
                }
            }
            State.WaitJoinTwitter -> {
                if (text == null) return
                when (val result = TwitterChecker(text).message()) {
                    is CheckerSuccessResult -> {
                        setStateForUser(redisClient, from, State.Finished)
                        bot.sendMessage(message.chat.id.toChatId(), "Поздравляем, вы в Танцах")
                        bot.sendMessage(message.chat.id.toChatId(), "Розыгрыш будет тогда-то")
                    }
                    is CheckerFailureResult -> {
                        bot.sendMessage(message.chat.id.toChatId(), result.message)
                    }
                }
            }
            State.Finished -> {
                bot.sendMessage(message.chat.id.toChatId(), MESSAGE_FINISH)
            }
        }
    }
}