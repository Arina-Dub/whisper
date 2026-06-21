package com.example.pril;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.pril.databinding.FragmentNotificationSettingsBinding;

public class NotificationSettingsFragment extends Fragment {

    private FragmentNotificationSettingsBinding binding;
    private AppPreferences prefs;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    prefs.setMsgNotifications(true);
                    binding.switchMessages.setChecked(true);
                } else {
                    Toast.makeText(requireContext(), "Уведомления отключены", Toast.LENGTH_SHORT).show();
                    binding.switchMessages.setChecked(false);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.switchMessages.setChecked(prefs.isMsgNotificationsEnabled());
        binding.switchCalls.setChecked(prefs.isCallNotificationsEnabled());
        binding.switchVibration.setChecked(prefs.isVibrationEnabled());

        binding.switchMessages.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) 
                        != PackageManager.PERMISSION_GRANTED) {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
            }
            prefs.setMsgNotifications(isChecked);
        });
        
        binding.switchCalls.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setCallNotifications(isChecked));
        binding.switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> prefs.setVibration(isChecked));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}