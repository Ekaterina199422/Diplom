package ru.netology.diplom.service


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diplom.R
import ru.netology.diplom.auth.AppAuth
import ru.netology.diplom.model.PushModel
import ru.netology.diplom.ui.MainActivity
import javax.inject.Inject

@AndroidEntryPoint
class FCMService: FirebaseMessagingService() {
    private val content = "content"
    private val channelId = "remote"

    @Inject
    lateinit var auth : AppAuth



    override fun onMessageReceived(message: RemoteMessage) {
        val notificationObject  = Gson().fromJson(message.data[content], PushModel::class.java)
        val notificationContent = notificationObject.content

        showNotification(notificationContent)
    }


    override fun onNewToken(token: String) {
        auth.sendPushTokenToServer(token)

    }


    private fun showNotification(content: String?) {

        val intent = Intent(this, MainActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentTitle("New notification")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .build()

        val name = "Server notifications"
        val descriptionText = "Notifications from remote server"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.notify(0, notification)
    }
}