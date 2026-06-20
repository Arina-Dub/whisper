package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.pril.databinding.FragmentLanguageBinding;

public class LanguageFragment extends Fragment {
    private FragmentLanguageBinding binding;
    private AppPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLanguageBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        String currentLang = prefs.getLanguage();
        if ("ru".equals(currentLang)) {
            binding.radioRussian.setChecked(true);
        } else {
            binding.radioEnglish.setChecked(true);
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = (checkedId == R.id.radioRussian) ? "ru" : "en";
            if (!newLang.equals(prefs.getLanguage())) {
                prefs.setLanguage(newLang);
                // Перезапускаем активность для применения языка
                if (getActivity() != null) {
                    getActivity().recreate();
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