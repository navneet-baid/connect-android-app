package `in`.app.connect

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random

const val channelId = "notification_channel"
const val channelName = "in.connect.app"

class ConnectionMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send the token to your server for further use
        println("Firebase Token is: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Handle the FCM data message here
        println(remoteMessage.data)
        val notificationBody = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""
        val phoneNumber = remoteMessage.data["phoneNumber"].toString()
        generateNotification(remoteMessage.notification?.title ?: "", notificationBody,phoneNumber)
    }


    private fun generateNotification(title: String, message: String,phoneNumber:String) {
        val intent = Intent(this, PopupViewProfile::class.java)
        intent.putExtra("phoneNumber",phoneNumber)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
        )
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.logo_32_32)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(1000, 1000, 1000))
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(message)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelExists = notificationManager.getNotificationChannel(channelId) != null
            if (!channelExists) {
                val channel =
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                notificationManager.createNotificationChannel(channel)
            }
        }
        val notificationId = Random().nextInt(100000)
        notificationManager.notify(notificationId, builder.build())
    }
}
