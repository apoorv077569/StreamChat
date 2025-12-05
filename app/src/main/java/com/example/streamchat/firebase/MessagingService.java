package com.example.streamchat.firebase;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.streamchat.R;
import com.example.streamchat.activities.ChatActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

public class MessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "chat_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM", "Refreshed Token: " + token);
    }


    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {

        Log.d("FCM", "Message Received ✔");
        Log.d("FCM", "DATA: " + message.getData());

        String title = message.getData().get("title");
        String body = message.getData().get("body");

        String senderId = message.getData().get("senderId");  // REQUIRED
        String senderName = message.getData().get("senderName"); // OPTIONAL
        String senderImage = message.getData().get("senderImage"); // OPTIONAL

        // =============== OPEN CHAT ACTIVITY =======================
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("senderId", senderId);
        intent.putExtra("senderName", senderName);
        intent.putExtra("senderImage", senderImage);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // ==========================================================

        createNotificationChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);  // <-- IMPORTANT!!!

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        manager.notify(new Random().nextInt(), builder.build());
    }


//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage message) {
//
//        Log.d("FCM", "Message Received ✔");
//        Log.d("FCM", "DATA: " + message.getData());
//
//        String title = message.getData().get("title");
//        String body = message.getData().get("body");
//
//        createNotificationChannel();
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.logo)
//                .setContentTitle(title)
//                .setContentText(body)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setAutoCancel(true);
//
//        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
//                != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//
//        manager.notify(new Random().nextInt(), builder.build());
//    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, "Chat Messages",
                            NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Log.d("CHANNEL", "ID: " + channel.getId() + " | Importance: " + channel.getImportance());


            manager.createNotificationChannel(channel);
        }
    }
}
