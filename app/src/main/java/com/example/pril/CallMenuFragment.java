package com.example.pril;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentCallMenuBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.jitsi.meet.sdk.BroadcastEvent;
import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.jitsi.meet.sdk.JitsiMeetView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CallMenuFragment extends Fragment {

    private FragmentCallMenuBinding binding;
    private JitsiMeetView jitsiView;
    private final Handler timerHandler = new Handler();
    private int seconds = 0;
    private boolean isCallActive = false;
    private boolean isTimerRunning = false;
    private boolean isMuted = false;
    private boolean isVideoOn = false;
    private boolean isSpeakerOn = false;
    
    private AudioManager audioManager;
    private ToneGenerator toneGenerator;
    private String receiverId;
    private String receiverName;
    private String currentUserId;
    private FirebaseFirestore db;
    private String callId;
    private String jitsiRoomName;
    private boolean isIncoming = false;
    private ListenerRegistration callStatusListener;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onBroadcastReceived(intent);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCallMenuBinding.inflate(inflater, container, false);
        
        // Создаем JitsiMeetView программно, используя контекст Activity,
        // чтобы избежать ошибки "Enclosing Activity must implement JitsiMeetActivityInterface"
        if (getActivity() != null) {
            jitsiView = new JitsiMeetView(getActivity());
            binding.jitsiContainer.addView(jitsiView);
        }
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        
        if (getArguments() != null) {
            receiverId = getArguments().getString("receiverId");
            receiverName = getArguments().getString("receiverName");
            callId = getArguments().getString("callId");
            isIncoming = getArguments().getBoolean("isIncoming", false);
            jitsiRoomName = getArguments().getString("jitsiRoomName");
        }
        
        initJitsi();
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        registerForBroadcasts();
        return binding.getRoot();
    }

    private void initJitsi() {
        try {
            URL serverURL = new URL("https://meet.ffmuc.net"); 
            JitsiMeetConferenceOptions defaultOptions = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("prejoinpage.enabled", false)
                    .setFeatureFlag("toolbox.enabled", false)
                    .setFeatureFlag("notifications.enabled", false)
                    .setFeatureFlag("conference-timer.enabled", false)
                    // Не ставим setAudioOnly(true), иначе видео не включится позже
                    .build();
            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        } catch (MalformedURLException e) {
            Log.e("JitsiLog", "Invalid server URL", e);
        }
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        for (BroadcastEvent.Type type : BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.getAction());
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, intentFilter);
    }

    private void onBroadcastReceived(Intent intent) {
        if (intent != null) {
            BroadcastEvent event = new BroadcastEvent(intent);
            switch (event.getType()) {
                case CONFERENCE_JOINED:
                    // Убрали startDialTone отсюда, так как запускаем его сразу при вызове
                    break;
                case PARTICIPANT_JOINED:
                    stopDialTone();
                    if (binding != null) {
                        binding.textViewCallStatus.setVisibility(View.GONE);
                    }
                    if (!isTimerRunning) {
                        isCallActive = true;
                        startTimer();
                        isTimerRunning = true;
                    }
                    if (callId != null && !isIncoming) {
                        db.collection("calls").document(callId).update("status", "ACTIVE");
                    }
                    break;
                case CONFERENCE_TERMINATED:
                    endCall();
                    break;
            }
        }
    }

    private void startDialTone() {
        try {
            if (toneGenerator == null) {
                // Используем STREAM_VOICE_CALL с максимальной громкостью (100)
                toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_SUP_RINGTONE);
        } catch (Exception e) {
            Log.e("CallMenu", "Error starting dial tone", e);
        }
    }

    private void playDisconnectTone() {
        stopDialTone();
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 300); // Короткий сигнал
            new Handler().postDelayed(this::navigateBack, 400);
        } catch (Exception e) {
            navigateBack();
        }
    }

    private void playBusyTone() {
        stopDialTone();
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100);
            }
            toneGenerator.startTone(ToneGenerator.TONE_SUP_BUSY, 1500); // Играем 1.5 секунды
            // Закрываем через 2 секунды после начала сигнала
            new Handler().postDelayed(this::navigateBack, 2000);
        } catch (Exception e) {
            Log.e("CallMenu", "Error playing busy tone", e);
            navigateBack();
        }
    }

    private void stopDialTone() {
        if (toneGenerator != null) {
            toneGenerator.stopTone();
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (receiverName != null) {
            binding.textViewCallerName.setText(receiverName);
        }
        
        // Сразу скрываем "Вызов..." для входящего звонка, так как соединение уже есть
        if (isIncoming && binding != null) {
            binding.textViewCallStatus.setVisibility(View.GONE);
        }
        
        // Загружаем аватарку собеседника
        if (receiverId != null) {
            db.collection("users").document(receiverId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && isAdded() && binding != null) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.profile)
                                .circleCrop()
                                .into(binding.imageViewCallerAvatar);
                    }
                }
            });
        }

        checkInitialPermissions();
        
        binding.buttonEndCall.setOnClickListener(v -> endCall());
        
        binding.buttonMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            Intent intent = new Intent("org.jitsi.meet.SET_AUDIO_MUTED");
            intent.putExtra("muted", isMuted);
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
            binding.buttonMute.setAlpha(isMuted ? 0.4f : 1.0f);
            binding.buttonMute.setColorFilter(isMuted ? 0xFFFF0000 : 0xFFFFFFFF);
        });

        binding.buttonVideo.setOnClickListener(v -> toggleVideo());

        binding.buttonSpeaker.setOnClickListener(v -> toggleSpeaker());
    }

    private void toggleSpeaker() {
        if (audioManager != null) {
            isSpeakerOn = !isSpeakerOn;
            audioManager.setSpeakerphoneOn(isSpeakerOn);
            
            // Визуальное обновление
            binding.buttonSpeaker.setAlpha(isSpeakerOn ? 1.0f : 0.6f);
            binding.buttonSpeaker.setBackgroundResource(isSpeakerOn ? R.drawable.ellipse2_light : R.drawable.ellipse2);
            
            if (isSpeakerOn) {
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        }
    }

    private void toggleVideo() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            performVideoSwitch();
        }
    }

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    performVideoSwitch();
                } else {
                    Toast.makeText(getContext(), "Нужен доступ к камере", Toast.LENGTH_SHORT).show();
                }
            });

    private void performVideoSwitch() {
        isVideoOn = !isVideoOn;
        
        // Отправляем команду в Jitsi
        Intent intent = new Intent("org.jitsi.meet.SET_VIDEO_MUTED");
        intent.putExtra("muted", !isVideoOn);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        
        // Визуальное переключение
        if (isVideoOn) {
            binding.layoutOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            binding.layoutOverlay.setBackgroundResource(R.drawable.chats_background_gradient);
        }

        binding.imageViewCallerAvatar.setVisibility(isVideoOn ? View.GONE : View.VISIBLE);
        binding.buttonVideo.setAlpha(isVideoOn ? 1.0f : 0.6f);
        binding.textViewVideo.setText(isVideoOn ? "Выкл.видео" : "Видео");
        
        if (isVideoOn) {
            binding.textViewVideo.setTextColor(0xFF4CAF50); // Зеленый текст при включении
        } else {
            binding.textViewVideo.setTextColor(0xFFFFFFFF);
        }
    }

    private void checkInitialPermissions() {
        // При старте аудио-звонка запрашиваем только микрофон
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startOrJoinCall();
        }
    }

    private final ActivityResultLauncher<String> requestAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startOrJoinCall();
                } else {
                    Toast.makeText(getContext(), "Без микрофона нельзя позвонить", Toast.LENGTH_SHORT).show();
                    navigateBack();
                }
            });

    private void startOrJoinCall() {
        if (isIncoming) {
            joinExistingCall();
        } else {
            createNewCall();
        }
    }

    private void createNewCall() {
        if (receiverId == null || currentUserId == null) return;
        jitsiRoomName = "PrilCall_" + UUID.randomUUID().toString().substring(0, 8);
        
        binding.textViewCallStatus.setText("Вызов...");
        AppPreferences prefs = new AppPreferences(requireContext());
        
        Map<String, Object> callData = new HashMap<>();
        callData.put("senderId", currentUserId);
        callData.put("senderName", prefs.getUserName());
        callData.put("receiverId", receiverId);
        callData.put("receiverName", receiverName);
        callData.put("status", "DIALING");
        callData.put("jitsiRoom", jitsiRoomName);
        callData.put("timestamp", Timestamp.now());
        callData.put("participants", Arrays.asList(currentUserId, receiverId));
        callData.put("type", CallModel.CallType.OUTGOING.name());

        db.collection("calls").add(callData).addOnSuccessListener(doc -> {
            callId = doc.getId();
            joinRoom();
            listenForCallChanges();
        });
    }

    private void joinExistingCall() {
        if (jitsiRoomName == null) {
            db.collection("calls").document(callId).get().addOnSuccessListener(doc -> {
                jitsiRoomName = doc.getString("jitsiRoom");
                joinRoom();
                listenForCallChanges();
            });
        } else {
            joinRoom();
            listenForCallChanges();
        }
    }

    private void joinRoom() {
        if (jitsiRoomName == null) return;
        Log.d("CallLog", "Joining Jitsi room: " + jitsiRoomName);
        
        if (!isIncoming) {
            // Запускаем гудки с небольшой задержкой, чтобы Jitsi успел инициализировать аудио
            new Handler().postDelayed(this::startDialTone, 1000);
        }
        
        AppPreferences prefs = new AppPreferences(requireContext());
        JitsiMeetUserInfo userInfo = new JitsiMeetUserInfo();
        userInfo.setDisplayName(prefs.getUserName());

        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.ffmuc.net"))
                    .setRoom(jitsiRoomName)
                    .setUserInfo(userInfo)
                    .setAudioMuted(false)
                    .setVideoMuted(true) // По умолчанию видео ВЫКЛЮЧЕНО
                    .build();
            jitsiView.join(options);
        } catch (MalformedURLException e) {
            Log.e("CallLog", "Invalid Jitsi URL", e);
        }
    }

    private void listenForCallChanges() {
        if (callId == null) return;
        callStatusListener = db.collection("calls").document(callId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                String status = value.getString("status");
                Log.d("CallLog", "Call status update: " + status);
                
                if ("ENDED".equals(status)) {
                    playDisconnectTone();
                } else if ("REJECTED".equals(status)) {
                    playBusyTone();
                } else if ("ACTIVE".equals(status)) {
                    // Если статус ACTIVE, значит собеседник ответил. 
                    // Скрываем "Вызов...", если Jitsi ивент еще не пришел.
                    if (binding != null && binding.textViewCallStatus.getVisibility() == View.VISIBLE) {
                        binding.textViewCallStatus.setVisibility(View.GONE);
                        if (!isTimerRunning) {
                            isCallActive = true;
                            startTimer();
                            isTimerRunning = true;
                        }
                        stopDialTone();
                    }
                }
            }
        });
    }

    private void endCall() {
        isCallActive = false;
        stopDialTone();
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(false);
        }
        if (callId != null) {
            db.collection("calls").document(callId).update("status", "ENDED");
        }
        Intent intent = new Intent("org.jitsi.meet.HANG_UP");
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        navigateBack();
    }

    private void navigateBack() {
        if (isAdded()) {
            stopCallListener();
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
            } catch (Exception ignored) {}
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    private void stopCallListener() {
        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }
    }

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCallActive && isAdded() && binding != null) {
                    seconds++;
                    binding.textViewCallDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopDialTone();
        if (jitsiView != null) {
            jitsiView.dispose();
        }
        isCallActive = false;
        timerHandler.removeCallbacksAndMessages(null);
        stopCallListener();
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {}
        binding = null;
    }
}
