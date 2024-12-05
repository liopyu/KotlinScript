class UserProfile(val userId: String) {
    // Immutable property, initialized at the time of object creation
    val createdAt: Long = System.currentTimeMillis()

    // Mutable property, can be changed after the object is created
    var loginCount: Int = 0

    // Late-initialized property; must be initialized before use
    lateinit var nickname: String

    // Lazy-initialized property, computed only when first accessed
    val userInfo: String by lazy {
        "User $nickname has logged in $loginCount times since ${createdAt.toDateString()}"
    }

    fun updateLoginCount() {
        loginCount++

    }

    fun initializeNickname(name: String) {
        nickname = name
    }

    private fun Long.toDateString(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(this))
    }
}
