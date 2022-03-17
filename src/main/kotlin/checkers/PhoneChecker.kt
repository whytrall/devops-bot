package checkers

import com.github.kotlintelegrambot.entities.Message
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import java.math.BigInteger
import java.security.MessageDigest

class PhoneChecker(private val message: Message, private val userId: Long, private val redisClient: KredsClient) : Checker {
    private lateinit var phone: String
    override suspend fun message(): CheckerResult {
        val contact = message.contact ?: return CheckerFailureResult("Попробуйте еще раз. Нажмите на кнопку 'Поделиться'")
        phone = contact.phoneNumber
        if (contact.userId != userId) {
            return CheckerFailureResult("Поделиться нужно именно своим номером =)")
        }

        val existing = redisClient.get(getPhoneHash())
        if (existing != null && existing != userId.toString()) {
            return CheckerFailureResult("Данный номер уже был использован в розыгрыше")
        }

        redisClient.set(getPhoneHash(), userId.toString())

        return CheckerSuccessResult("jopa")
    }

    private fun getPhoneHash() = md5(phone)

    private fun md5(input:String): String {
        val md = MessageDigest.getInstance("MD5")
        return "phone_${BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')}"
    }
}