package com.example.pril;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentIncomingCallBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IncomingCallFragment extends Fragment {

    private FragmentIncomingCallBinding binding;
    private String callId;
    private String callerName;
    private String senderId;
    private FirebaseFirestore db;
    private ListenerRegistration callStatusListener;
    private Ringtone ringtone;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIncomingCallBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            callId = getArguments().getString("callId");
            callerName = getArguments().getString("callerName");
            senderId = getArguments().getString("senderId");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (callerName != null) {
            binding.textViewIncomingName.setText(callerName);
        }
        
        // Загружаем аватарку звонящего
        if (senderId != null) {
            db.collection("users").document(senderId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && isAdded() && binding != null) {
                    String avatarUrl = documentSnapshot.getString("avatarUrl");
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(this)
                                .load(avatarUrl)
                                .placeholder(R.drawable.profile)
                                .circleCrop()
                                .into(binding.imageViewIncomingAvatar);
                    }
                }
            });
        }

        binding.buttonAnswer.setOnClickListener(v -> answerCall());
        binding.buttonDecline.setOnClickListener(v -> declineCall());
        
        listenForCancellation();
        startRingtone();
    }

    private void startRingtone() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            ringtone = RingtoneManager.getRingtone(requireContext(), notification);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void listenForCancellation() {
        if (callId == null) return;
        callStatusListener = db.collection("calls").document(callId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                String status = value.getString("status");
                if ("ENDED".equals(status)) {
                    navigateBack();
                }
            }
        });
    }

    private void answerCall() {
        stopRingtone();
        if (callId != null) {
            db.collection("calls").document(callId).update("status", "ACTIVE");
            Bundle bundle = new Bundle();
            bundle.putString("callId", callId);
            bundle.putString("receiverName", callerName);
            bundle.putString("receiverId", senderId);
            bundle.putString("jitsiRoomName", getArguments().getString("jitsiRoomName"));
            bundle.putBoolean("isIncoming", true);
            
            stopCallListener();
            NavHostFragment.findNavController(this).navigate(R.id.CallMenuFragment, bundle);
        }
    }

    private void declineCall() {
        stopRingtone();
        if (callId != null) {
            db.collection("calls").document(callId).update("status", "REJECTED");
        }
        navigateBack();
    }

    private void navigateBack() {
        stopRingtone();
        if (isAdded()) {
            stopCallListener();
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    private void stopCallListener() {
        if (callStatusListener != null) {
            callStatusListener.remove();
            callStatusListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCallListener();
        binding = null;
    }
}
