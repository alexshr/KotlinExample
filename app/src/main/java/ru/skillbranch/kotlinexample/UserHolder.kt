package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import ru.skillbranch.kotlinexample.User.Companion.normalizeLogin

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User(
        fullName = fullName,
        email = email,
        password = password
    )
        .apply {
            require(map[login] == null) { "A user with this email already exists" }
            map[login] = this
        }

    fun registerUserByPhone(
        fullName: String?,
        phone: String?
    ): User {
        return User(
            fullName = fullName,
            _phone = phone
        )
            .apply {
                require(map[login] == null) { "A user with this phone already exists" }
                map[login] = this
            }
    }

    fun loginUser(login: String, password: String) =
        try {
            map[normalizeLogin(login)].apply {
                check(this != null) { "A user with this login not exists" }
                check(checkPassword(password)) { "User password is incorrect" }
            }!!.userInfo
        } catch (e: Exception) {
            print(e.message)
            null
        }

    fun requestAccessCode(login: String) {
        map[normalizeLogin(login)].apply {
            if (this == null) println("A user with this login not exists")
            else generateEncryptAccessCode()
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val users = mutableListOf<User>()
        list.forEach {
            val csv = it.split(";")
            try {
                User(csv).apply {
                    map[login] = this
                    users.add(this)
                }
            } catch (e: Exception) {
                println("""${e.message} from $csv""")
            }
        }
        return users
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() = map.clear()
}