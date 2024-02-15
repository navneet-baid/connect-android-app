package `in`.app.connect.usermanagment.models

data class UserData (
    val phoneNumber:String="",
    val emailId:String="",
    val password:String="",
    val userName:String="",
    val dob:String="",
    val gender:String="",
    val bio:String="",
    val lookingFor:String="",
    val distance:Int=0,
    val lat:Double=0.0,
    val long:Double=0.0,
    val images:ArrayList<String> = arrayListOf(),
    val fcmToken:String="",
    val location:String="",
    val hometown:String=""
)