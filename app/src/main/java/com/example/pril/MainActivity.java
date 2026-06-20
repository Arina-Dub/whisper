package com.example.pril;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.pril.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import android.os.Build;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;

import com.facebook.react.modules.core.PermissionListener;

public class MainActivity extends AppCompatActivity implements JitsiMeetActivityInterface {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration callListener;
    private GestureDetector gestureDetector;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        AppPreferences prefs = new AppPreferences(newBase);
        String lang = prefs.getLanguage();
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.ChatsFragment, R.id.CallsFragment, R.id.SettingsFragment)
                .build();
        
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        updateFcmToken();
        startMainService();
        checkNotificationPermission();
        setupSwipeBack();

        handleIntent(getIntent());

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            
            if (id == R.id.SplashFragment || id == R.id.LoginFragment || id == R.id.RegisterFragment) {
                binding.appBarLayout.setVisibility(View.GONE);
                binding.bottomNav.setVisibility(View.GONE);
            } 
            else if (id == R.id.ChatDetailFragment || id == R.id.CallMenuFragment || 
                     id == R.id.EditProfileFragment || id == R.id.ContactsFragment || 
                     id == R.id.IncomingCallFragment || id == R.id.UserProfileFragment || 
                     id == R.id.ImageDetailFragment) {
                binding.appBarLayout.setVisibility(View.VISIBLE);
                binding.bottomNav.setVisibility(View.GONE);
                
                if (id == R.id.ChatDetailFragment) {
                    if (arguments != null) {
                        String receiverId = arguments.getString("receiverId");
                        if (currentUserId != null && receiverId != null) {
                            MainService.activeChatId = (currentUserId.compareTo(receiverId) < 0) ? currentUserId + "_" + receiverId : receiverId + "_" + currentUserId;
                        }
                    }
                } else {
                    MainService.activeChatId = null;
                }

                if (id == R.id.CallMenuFragment || id == R.id.IncomingCallFragment || 
                    id == R.id.ChatDetailFragment || id == R.id.UserProfileFragment || 
                    id == R.id.ImageDetailFragment) {
                    binding.appBarLayout.setVisibility(View.GONE);
                }
            } 
            else {
                binding.appBarLayout.setVisibility(View.VISIBLE);
                binding.bottomNav.setVisibility(View.VISIBLE);
                MainService.activeChatId = null;
            }
        });
    }

    private void setupSwipeBack() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null && e1.getX() < 80 && (e2.getX() - e1.getX()) > 150 && 
                    Math.abs(velocityX) > 200 && Math.abs(velocityX) > Math.abs(velocityY) * 2) {
                    
                    NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment_content_main);
                    if (navController.getPreviousBackStackEntry() != null) {
                        int id = navController.getCurrentDestination().getId();
                        if (id != R.id.ChatsFragment && id != R.id.CallsFragment && id != R.id.SettingsFragment && 
                            id != R.id.SplashFragment && id != R.id.LoginFragment) {
                            navController.popBackStack();
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector != null) {
            gestureDetector.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void updateFcmToken() {
        if (currentUserId == null) return;
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                db.collection("users").document(currentUserId).update("fcmToken", token);
            }
        });
    }

    private void startMainService() {
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.isEnergySaverEnabled()) return;

        Intent serviceIntent = new Intent(this, MainService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void startCallListener() {
        if (currentUserId == null) {
            currentUserId = FirebaseAuth.getInstance().getUid();
        }
        if (currentUserId == null || callListener != null) return;

        // Слушаем только свежие вызовы (созданные в последние 5 минут), чтобы избежать "фантомных" звонков при смене темы
        long fiveMinutesAgo = (System.currentTimeMillis() / 1000) - 300;

        callListener = db.collection("calls")
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("status", "DIALING")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null && !value.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                        
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp != null && timestamp.getSeconds() < fiveMinutesAgo) {
                            Log.d("CallLog", "Ignoring old call: " + doc.getId());
                            return;
                        }

                        String callId = doc.getId();
                        String callerName = doc.getString("senderName");
                        String senderId = doc.getString("senderId");
                        String jitsiRoom = doc.getString("jitsiRoom");

                        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
                        if (navController.getCurrentDestination() != null && 
                            navController.getCurrentDestination().getId() != R.id.IncomingCallFragment &&
                            navController.getCurrentDestination().getId() != R.id.CallMenuFragment) {
                            
                            Bundle bundle = new Bundle();
                            bundle.putString("callId", callId);
                            bundle.putString("callerName", callerName != null ? callerName : "Неизвестный");
                            bundle.putString("senderId", senderId);
                            bundle.putString("jitsiRoomName", jitsiRoom);
                            navController.navigate(R.id.IncomingCallFragment, bundle);
                        }
                    }
                });
    }

    private void stopCallListener() {
        if (callListener != null) {
            callListener.remove();
            callListener = null;
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String type = intent.getStringExtra("type");
        if (type == null) return;

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        if ("call".equals(type)) {
            String callId = intent.getStringExtra("callId");
            String callerName = intent.getStringExtra("callerName");
            String senderId = intent.getStringExtra("senderId");
            String jitsiRoomName = intent.getStringExtra("jitsiRoomName");

            if (callId != null) {
                Bundle bundle = new Bundle();
                bundle.putString("callId", callId);
                bundle.putString("callerName", callerName);
                bundle.putString("senderId", senderId);
                bundle.putString("jitsiRoomName", jitsiRoomName);
                
                // Очищаем стек до корня и открываем входящий звонок
                navController.navigate(R.id.ChatsFragment, null, new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true).build());
                navController.navigate(R.id.IncomingCallFragment, bundle);
            }
        } else if ("chat".equals(type)) {
            String receiverId = intent.getStringExtra("receiverId");
            String contactName = intent.getStringExtra("contactName");
            if (receiverId != null) {
                Bundle bundle = new Bundle();
                bundle.putString("receiverId", receiverId);
                bundle.putString("contactName", contactName != null ? contactName : getString(R.string.chat_default));
                
                // Очищаем стек до корня и открываем нужный чат
                navController.navigate(R.id.ChatsFragment, null, new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true).build());
                navController.navigate(R.id.ChatDetailFragment, bundle);
            }
        }
        
        // Очищаем экстрасы, чтобы не срабатывало при повороте экрана или смене темы
        intent.removeExtra("type");
        intent.removeExtra("callId");
        intent.removeExtra("receiverId");
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus("online");
        startCallListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus("offline");
        stopCallListener();
    }

    private void updateUserStatus(String status) {
        if (currentUserId == null) {
            currentUserId = FirebaseAuth.getInstance().getUid();
        }
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .update("status", status, "lastSeen", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // Методы для JitsiMeetActivityInterface
    @Override
    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        JitsiMeetActivityDelegate.requestPermissions(this, permissions, requestCode, listener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        JitsiMeetActivityDelegate.onHostResume(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        JitsiMeetActivityDelegate.onHostDestroy(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        JitsiMeetActivityDelegate.onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        // Если Jitsi активен, он может обработать нажатие (например, закрыть PiP)
        JitsiMeetActivityDelegate.onBackPressed();
        
        // Для Navigation component лучше использовать стандартный механизм,
        // но если мы переопределили onBackPressed, нужно вызвать super.
        // На Android 13+ рекомендуется использовать OnBackPressedDispatcher.
        super.onBackPressed();
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        JitsiMeetActivityDelegate.onNewIntent(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // В новых версиях SDK этот метод может отсутствовать в делегате, 
        // Jitsi сам обрабатывает PiP через манифест
    }
}
