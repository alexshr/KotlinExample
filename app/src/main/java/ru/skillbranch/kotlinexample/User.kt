package ru.skillbranch.kotlinexample

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest

@SuppressLint("DefaultLocale")
class User private constructor(
    private var meta: Map<String, String>,
    private var fullName: String? = null,
    private var email: String? = null,
    private var salt: String? = null,
    private var passwordHash: String? = null,
    private var phone: String? = null
) {
    //region ================= properties =================
    private var firstName: String = ""
    private var lastName: String? = null
    private var initials: String? = null

    val login: String
        get() = email?.toLowerCase() ?: phone!!

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    val userInfo: String
        get() = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    //endregion

    init {
        parseFullName()
        normalizeEmail()
        normalizePhone()
    }

    //for email registration
    constructor(
        fullName: String?,
        email: String?,
        password: String?
    ) : this(
        meta = mapOf("auth" to "password"),
        fullName = fullName,
        email = email
    ) {
        requireNotNull(email) {
            "Email number must not be null or blank"
        }
        require(!password.isNullOrBlank()) { "Password for email registration must not be null or empty" }
        passwordHash = encrypt(password)
    }

    //for phone registration
    constructor(
        fullName: String?,
        _phone: String?
    ) : this(meta = mapOf("auth" to "sms"), fullName = fullName, phone = _phone) {

        requireNotNull(phone) {
            "Phone number must not be null or blank"
        }
        generateEncryptAccessCode()
    }

    //for csv registration
    constructor(csv: List<String>) :
            this(
                meta = mapOf("src" to "csv"),
                fullName = csv[0],
                email = if (!csv[1].isBlank()) csv[1] else null,
                salt = csv[2].toSaltHashPair().first,
                passwordHash = csv[2].toSaltHashPair().second,
                phone = if (!csv[3].isBlank()) csv[3] else null
            ) {
        require((email == null) xor (phone == null)) {
            "invalid email or phone for csv user"
        }
        require(passwordHash != null) {
            "hash for csv user must not be empty"
        }
        normalizeEmail()
        normalizePhone()
    }


    //region ================= methods =================
    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun generateEncryptAccessCode() {
        accessCode = generateAccessCode()
        passwordHash = encrypt(accessCode!!)
    }

    private fun parseFullName() {
        if (!fullName.isNullOrBlank()) {
            val matches = Regex("""[a-zA-ZА-Яа-яЁё]+""").findAll(fullName!!)
            val names = matches.toList()
            if (names.size in 1..2) {
                firstName = names[0].value.capitalize()
                initials = firstName.first().toString()
                fullName = firstName
            }
            if (names.size == 2) {
                lastName = names[1].value.capitalize()
                initials += " ${lastName!!.first()}"
                fullName += " $lastName"
            }
        }
        require(firstName.isNotEmpty()) { "Fullname in empty or invalid" }
    }

    private fun encrypt(password: String) = salt.plus(password).md5()

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 bytes
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(length = 32, padChar = '0')
    }

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    private fun normalizeEmail() {
        if (email != null) {
            email = normalizeEmail(email)
            requireNotNull(email) { "invalid email" }
        }
    }

    private fun normalizePhone() {
        if (phone != null) {
            phone = normalizePhone(phone)
            requireNotNull(phone) { "invalid phone" }
        }
    }
//endregion

    companion object {

        fun normalizeLogin(loginStr: String) =
            normalizePhone(loginStr) ?: normalizeEmail(loginStr)?.toLowerCase()
            ?: throw IllegalArgumentException("Incorrect phone or email")

        private fun String.toSaltHashPair(): Pair<String, String> {
            split(":")
                .filter { it.isNotBlank() }
                .run {
                    if (size == 2) return first() to last()
                    else throw IllegalArgumentException("Invalid salt:hash string: $this")
                }
        }

        private fun normalizePhone(phoneStr: String?): String? {
            if (!phoneStr.isNullOrBlank()) {
                phoneStr.replace(Regex("""[^+\d]"""), "").let {
                    if (Regex("""^\+\d{11}$""").matches(it)) return it
                }
            }
            return null
        }

        private fun normalizeEmail(email: String?): String? =
            if (!email.isNullOrBlank()) email.trim()
            else null
    }

}
