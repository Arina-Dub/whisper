package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonBack.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });

        binding.buttonResetPassword.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Введите email", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonResetPassword.setEnabled(false);

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (binding == null) return;
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonResetPassword.setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(requireContext(), R.string.reset_link_sent, Toast.LENGTH_LONG).show();
                            NavHostFragment.findNavController(this).navigateUp();
                        } else {
                            Toast.makeText(requireContext(), "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
