package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.pril.databinding.FragmentDataStorageBinding;

public class DataStorageFragment extends Fragment {
    private FragmentDataStorageBinding binding;
    private AppPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDataStorageBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.switchAutoDownload.setChecked(prefs.isAutoDownloadEnabled());
        binding.switchAutoDownload.setOnCheckedChangeListener((v, isChecked) -> prefs.setAutoDownload(isChecked));
        
        binding.buttonClearCache.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Кэш очищен", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}