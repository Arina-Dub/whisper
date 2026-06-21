package com.example.pril;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainService extends Service {
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration messageListener;
    private ListenerRegistration callListener;
    public static String activeChatId = null;

    private static final String STATUS_CHANNEL_ID = "service_status";

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        
        // Создаем каналы при создании сервиса
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager == null) return;

            // Канал для уведомлений о сообщениях (Высокий приоритет)
            android.app.NotificationChannel alertChannel = new android.app.NotificationChannel(
                    NotificationHelper.CHANNEL_ID,
                    "Messenger Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(alertChannel);

            // Канал для статуса сервиса (Минимальный приоритет, чтобы не мешал)
            android.app.NotificationChannel statusChannel = new android.app.NotificationChannel(
                    STATUS_CHANNEL_ID,
                    "Service Status",
                    android.app.NotificationManager.IMPORTANCE_MIN
            );
            manager.createNotificationChannel(statusChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.isEnergySaverEnabled()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Для Foreground Service используем тихий канал
        Notification notification = new NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
                .setContentTitle("Whisper")
                .setContentText("Поиск новых сообщений...")
                .setSmallIcon(R.mipmap.icon)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        startForeground(1001, notification);
        
        startListeners();
        
        return START_STICKY;
    }

    private void startListeners() {
        if (currentUserId == null) {
            currentUserId = FirebaseAuth.getInstance().getUid();
        }
        if (currentUserId == null) return;

        // Слушаем сообщения
        if (messageListener == null) {
            messageListener = db.collection("chats")
                    .whereArrayContains("participants", currentUserId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.e("MainService", "Chat listener error", error);
                            return;
                        }
                        if (value != null) {
                            for (DocumentChange dc : value.getDocumentChanges()) {
                                if (dc.getType() == DocumentChange.Type.ADDED || dc.getType() == DocumentChange.Type.MODIFIED) {
                                    String lastSenderId = dc.getDocument().getString("lastMessageSenderId");
                                    String lastMessage = dc.getDocument().getString("lastMessage");
                                    String chatId = dc.getDocument().getId();
                                    
                                    Log.d("MainService", "Message update in " + chatId + " from " + lastSenderId);

                                    AppPreferences prefs = new AppPreferences(this);
                                    if (lastSenderId != null && !lastSenderId.equals(currentUserId) && 
                                        !chatId.equals(activeChatId) &&
                                        prefs.isMsgNotificationsEnabled()) {
                                        
                                        Intent intent = new Intent(this, MainActivity.class);
                                        intent.putExtra("type", "chat");
                                        intent.putExtra("chatId", chatId);
                                        intent.putExtra("receiverId", lastSenderId); // Чтобы знать, с кем чат
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        
                                        NotificationHelper.showNotification(this, "Новое сообщение", lastMessage, intent);
                                    }
                                }
                            }
                        }
                    });
        }

        // Слушаем звонки
        if (callListener == null) {
            callListener = db.collection("calls")
                    .whereEqualTo("receiverId", currentUserId)
                    .whereEqualTo("status", "DIALING")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        if (value != null && !value.isEmpty()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                            String callId = doc.getId();
                            String callerName = doc.getString("senderName");
                            String senderId = doc.getString("senderId");
                            String jitsiRoom = doc.getString("jitsiRoom");

                            AppPreferences prefs = new AppPreferences(this);
                            if (prefs.isCallNotificationsEnabled()) {
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.putExtra("type", "call");
                                intent.putExtra("callId", callId);
                                intent.putExtra("callerName", callerName);
                                intent.putExtra("senderId", senderId);
                                intent.putExtra("jitsiRoomName", jitsiRoom);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                
                                NotificationHelper.showNotification(this, "Входящий вызов", "Вам звонит " + (callerName != null ? callerName : "Неизвестный"), intent);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroy() {
        if (messageListener != null) messageListener.remove();
        if (callListener != null) callListener.remove();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
