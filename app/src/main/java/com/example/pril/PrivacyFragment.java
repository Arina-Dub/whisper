package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.pril.databinding.FragmentPrivacyBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PrivacyFragment extends Fragment {
    private FragmentPrivacyBinding binding;
    private AppPreferences prefs;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPrivacyBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.switchPrivacyOnline.setChecked(prefs.isPrivacyOnlineVisible());
        binding.switchPrivacyOnline.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.setPrivacyOnline(isChecked);
            if (currentUserId != null) {
                db.collection("users").document(currentUserId)
                        .update("showOnlineStatus", isChecked);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}