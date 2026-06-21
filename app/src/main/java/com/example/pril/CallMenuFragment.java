package com.example.pril;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
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
    private boolean isTimerStarted = false;

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
                    Log.d("CallMenu", "CONFERENCE_JOINED");
                    if (isIncoming && !isTimerStarted) {
                        startCallTimer();
                    }
                    break;
                case PARTICIPANT_JOINED:
                    Log.d("CallMenu", "PARTICIPANT_JOINED");
                    stopDialTone();
                    if (binding != null) {
                        binding.textViewCallStatus.setVisibility(View.GONE);
                    }
                    if (!isTimerStarted) {
                        startCallTimer();
                    }
                    if (callId != null && !isIncoming) {
                        db.collection("calls").document(callId).update("status", "ACTIVE");
                    }
                    break;
                case CONFERENCE_TERMINATED:
                    Log.d("CallMenu", "CONFERENCE_TERMINATED");
                    endCall();
                    break;
            }
        }
    }

    private void startCallTimer() {
        if (isTimerStarted) return;
        isTimerStarted = true;
        isCallActive = true;
        isTimerRunning = true;

        if (binding != null) {
            binding.textViewCallStatus.setVisibility(View.GONE);
        }

        startTimer();
        Log.d("CallMenu", "Timer started");
    }

    private void startDialTone() {
        try {
            if (toneGenerator == null) {
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
            toneGenerator.startTone(ToneGenerator.TONE_PROP_PROMPT, 300);
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
            toneGenerator.startTone(ToneGenerator.TONE_SUP_BUSY, 1500);
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

        if (isIncoming && binding != null) {
            binding.textViewCallStatus.setVisibility(View.GONE);
        }

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
        isSpeakerOn = !isSpeakerOn;

        try {
            // Используем AudioManager для управления динамиком
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(isSpeakerOn);
            }

            // Отправляем команду в Jitsi
            Intent intent = new Intent("org.jitsi.meet.SET_AUDIO_OUTPUT");
            intent.putExtra("output", isSpeakerOn ? "speaker" : "earpiece");
            LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);

            Log.d("CallMenu", "Speaker mode: " + (isSpeakerOn ? "ON" : "OFF"));

        } catch (Exception e) {
            Log.e("CallMenu", "Error toggling speaker", e);
            // Fallback
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(isSpeakerOn);
            }
        }

        updateSpeakerUI();
    }

    private void updateSpeakerUI() {
        if (binding == null) return;

        if (isSpeakerOn) {
            binding.buttonSpeaker.setAlpha(1.0f);
            binding.buttonSpeaker.setBackgroundResource(R.drawable.ellipse2_light);
            binding.buttonSpeaker.setImageResource(R.drawable.volume);
        } else {
            binding.buttonSpeaker.setAlpha(0.6f);
            binding.buttonSpeaker.setBackgroundResource(R.drawable.ellipse2);
            binding.buttonSpeaker.setImageResource(R.drawable.volume1);
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

        Intent intent = new Intent("org.jitsi.meet.SET_VIDEO_MUTED");
        intent.putExtra("muted", !isVideoOn);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);

        if (isVideoOn) {
            binding.layoutOverlay.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            binding.layoutOverlay.setBackgroundResource(R.drawable.chats_background_gradient);
        }

        binding.imageViewCallerAvatar.setVisibility(isVideoOn ? View.GONE : View.VISIBLE);
        binding.buttonVideo.setAlpha(isVideoOn ? 1.0f : 0.6f);
        binding.textViewVideo.setText(isVideoOn ? "Выкл.видео" : "Видео");

        if (isVideoOn) {
            binding.textViewVideo.setTextColor(0xFF4CAF50);
        } else {
            binding.textViewVideo.setTextColor(0xFFFFFFFF);
        }
    }

    private void checkInitialPermissions() {
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
                    .setVideoMuted(true)
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
                    if (!isTimerStarted) {
                        startCallTimer();
                    }
                    stopDialTone();
                    if (binding != null) {
                        binding.textViewCallStatus.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private void endCall() {
        isCallActive = false;
        isTimerRunning = false;
        stopDialTone();
        timerHandler.removeCallbacksAndMessages(null);

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
        isCallActive = false;
        isTimerRunning = false;
        timerHandler.removeCallbacksAndMessages(null);
        if (jitsiView != null) {
            jitsiView.dispose();
        }
        stopCallListener();
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {}
        binding = null;
    }
}