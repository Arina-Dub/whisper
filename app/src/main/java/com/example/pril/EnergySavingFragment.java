package com.example.pril;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.pril.databinding.FragmentEnergySavingBinding;

public class EnergySavingFragment extends Fragment {
    private FragmentEnergySavingBinding binding;
    private AppPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEnergySavingBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.switchEnergySaver.setChecked(prefs.isEnergySaverEnabled());
        binding.switchEnergySaver.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.setEnergySaver(isChecked);
            Intent serviceIntent = new Intent(requireContext(), MainService.class);
            if (isChecked) {
                requireContext().stopService(serviceIntent);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent);
                } else {
                    requireContext().startService(serviceIntent);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}