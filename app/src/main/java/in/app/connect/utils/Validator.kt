package `in`.app.connect.utils

class Validator {
    companion object{

    fun validateEmailId(emailId:String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return if (emailId.isEmpty()) {
            false
        } else emailId.matches(emailPattern.toRegex())
    }

     fun validatePassword(password:String): Boolean {

        val passwordPattern = "^(?=.*[a-zA-Z])(?=.*[@#$%^&+=]).{5,}$"
        return if (password.isEmpty()) {
            false
        } else if (!password.matches(passwordPattern.toRegex())) {
                "Password must contain at least one alphabetical character, one special character, and be at least 5 characters long"
            false
        } else {
            true
        }
    }}
}