package com.example.pril;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentSplashBinding;

public class SplashFragment extends Fragment {

    private FragmentSplashBinding binding;
    private AppPreferences prefs;
    private final Handler handler = new Handler();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSplashBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Начальное состояние
        binding.imageViewSplash.setRotation(0);
        
        // Запускаем цепочку плавных поворотов
        rotateStep(90);
    }

    private void rotateStep(final float targetRotation) {
        if (binding == null) return;

        binding.imageViewSplash.animate()
                .rotation(targetRotation)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> {
                    // Пауза 0.1 сек
                    handler.postDelayed(() -> {
                        if (targetRotation < 360) {
                            rotateStep(targetRotation + 90);
                        } else {
                            finishSplash();
                        }
                    }, 100);
                })
                .start();
    }

    private void finishSplash() {
        if (!isAdded()) return;
        
        if (prefs.isLoggedIn()) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SplashFragment_to_ChatsFragment);
        } else {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SplashFragment_to_LoginFragment);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (binding != null) {
            binding.imageViewSplash.animate().cancel();
        }
        binding = null;
    }
}