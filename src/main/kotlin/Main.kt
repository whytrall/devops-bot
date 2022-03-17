import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.extensions.filters.Filter
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.coroutines.*
import java.util.concurrent.ForkJoinPool

const val TOKEN = "211121503:AAG-cMZZ2pC6XpRfEyyfCla5UdU8TQilitE"
const val CHAT_ID = -1001777031237L
const val REDIS_URL = "127.0.0.1:6379"

const val MESSAGE_ENTER_BNB = "Введите адрес кошелька в сети BNB"
const val MESSAGE_JOIN_TG = "Вступите в телеграм группу https://t.me/+sFQusMkN5TNkNDAy и нажмите на кнопку"
const val MESSAGE_JOIN_TWITTER =
    "Теперь подпишитесь на кого-то, сделайте репост какого-то сообщения " +
            "и отправьте ссылку на свой твиттер (twitter.com/username или @username). Проверьте, что аккаунт открыт"
const val MESSAGE_TRY_AGAIN_TWITTER = "Выполните все условия в твиттере и попробоуйте еще раз"

const val MESSAGE_FINISH = "Вы уже зарегистрированы, ожидайте тогда-то"

fun Long.toChatId() = ChatId.fromId(this)

suspend fun getStateForUser(redisClient: KredsClient, user: Long) = State.fromString(redisClient.get("${user}_state"))
suspend fun setStateForUser(redisClient: KredsClient, user: Long, state: State) {
    redisClient.set("${user}_state", state.key, null)
}
fun main(args: Array<String>) {
    /*val twitterBot = PenicillinClient {
        account {
            application("ConsumerKey", "ConsumerSecret")
            token("AccessToken", "AccessToken Secret")
        }
    }*/

    val executorService = ForkJoinPool()
    val scope = CoroutineScope(executorService.asCoroutineDispatcher())
    val redisClient = newClient(Endpoint.from(REDIS_URL))

    val tgBot = bot {
        token = TOKEN
        dispatch {
            command("start") {
                scope.launch {
                    val from = message.from?.id ?: return@launch
                    val state = getStateForUser(redisClient, from)
                    if (state == State.Finished) {
                        bot.sendMessage(from.toChatId(), MESSAGE_FINISH)
                        return@launch
                    }
                    // Если что-то совсем пошло не так то можно все сбросить и начать сначала (кроме стейта Finished)
                    setStateForUser(redisClient, from, State.Started)
                    StateMachine(State.Started, redisClient).execute(bot, message)
                }
            }
            message(Filter.Text or Filter.Contact) {
                scope.launch {
                    val from = message.from?.id ?: return@launch
                    val state = getStateForUser(redisClient, from)
                    StateMachine(state, redisClient).execute(bot, message)
                }
            }
            callbackQuery {
                scope.launch {
                    val from = callbackQuery.message?.chat?.id ?: return@launch
                    val message = callbackQuery.message ?: return@launch
                    val state = getStateForUser(redisClient, from)
                    StateMachine(state, redisClient).execute(bot, message)
                }
            }


            command("redis_clean") {
                scope.launch {
                    redisClient.flushDb()
                }
            }
            command("all_enrolled") {
                scope.launch {
                    val from = message.from?.id ?: return@launch
                    val participants = redisClient.keys("*_state")
                        .mapNotNull {
                            val s = State.fromString(redisClient.get(it))
                            if (s == null) null else "${it.replace("_state", "")}:${s}/${redisClient.get("${it}_BNB")}"
                        }
                    bot.sendMessage(from.toChatId(), participants.joinToString(separator = "\n") { it })
                }
            }
        }
    }

    tgBot.startPolling()
}