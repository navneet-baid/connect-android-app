package `in`.app.connect.utils


import org.mindrot.jbcrypt.BCrypt

object CryptoUtils {

    private const val SHIFT = 256

    fun encrypt(message: String): String {
        val shiftedMessage = message.map { char ->
            if (char.isLetter()) {
                val base = if (char.isUpperCase()) 'A' else 'a'
                val shiftedChar =
                    ((char.code - base.code + SHIFT) % 26 + base.code).toChar()
                shiftedChar
            } else {
                char
            }
        }
        return shiftedMessage.joinToString("")
    }

    fun decrypt(encryptedMessage: String): String {
        val shiftedMessage = encryptedMessage.map { char ->
            if (char.isLetter()) {
                val base = if (char.isUpperCase()) 'Z' else 'z'
                val shiftedChar =
                    ((char.code - base.code - SHIFT) % 26 + base.code).toChar()
                shiftedChar
            } else {
                char
            }
        }
        return shiftedMessage.joinToString("")
    }

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(password, hashedPassword)
    }
}







