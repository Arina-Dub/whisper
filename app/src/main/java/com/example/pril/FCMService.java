package com.example.pril;

import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId != null) {
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("fcmToken", token);
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                    .update(tokenMap)
                    .addOnFailureListener(e -> Log.e("FCM", "Failed to update token", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Новое сообщение";
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "";
        
        // Обработка данных, если они есть
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            if ("call".equals(type)) {
                // Логика для звонка (можно показать уведомление или запустить экран входящего вызова)
                showCallNotification(remoteMessage.getData());
            } else {
                showMessageNotification(title, body, remoteMessage.getData());
            }
        } else {
            showMessageNotification(title, body, null);
        }
    }

    private void showMessageNotification(String title, String body, Map<String, String> data) {
        AppPreferences prefs = new AppPreferences(this);
        if (!prefs.isMsgNotificationsEnabled()) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Здесь можно добавить receiverId в intent, чтобы открыть конкретный чат
        NotificationHelper.showNotification(this, title, body, intent);
    }

    private void showCallNotification(Map<String, String> data) {
        AppPreferences prefs = new AppPreferences(this);
        if (!prefs.isCallNotificationsEnabled()) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Добавляем данные о звонке
        NotificationHelper.showNotification(this, "Входящий звонок", "Вам звонит " + data.get("callerName"), intent);
    }
}
