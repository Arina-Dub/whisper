package com.example.pril;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentChangePasswordBinding;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordFragment extends Fragment {

    private FragmentChangePasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSavePassword.setOnClickListener(v -> {
            String oldPass = binding.editTextOldPassword.getText().toString();
            String newPass = binding.editTextNewPassword.getText().toString();
            String confirmPass = binding.editTextConfirmPassword.getText().toString();

            if (oldPass.isEmpty()) {
                Toast.makeText(requireContext(), "Введите текущий пароль", Toast.LENGTH_SHORT).show();
            } else if (newPass.length() < 6) {
                Toast.makeText(requireContext(), "Новый пароль — минимум 6 символов", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirmPass)) {
                Toast.makeText(requireContext(), "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            } else {
                updatePassword(oldPass, newPass);
            }
        });
    }

    private void updatePassword(String oldPass, String newPass) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.buttonSavePassword.setEnabled(false);

        user.reload().addOnCompleteListener(reloadTask -> {
            if (!reloadTask.isSuccessful()) {
                binding.buttonSavePassword.setEnabled(true);
                Toast.makeText(requireContext(), "Ошибка обновления данных", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPass);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(passTask -> {
                        if (passTask.isSuccessful()) {
                            Toast.makeText(requireContext(), "Пароль изменен", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(this).navigateUp();
                        } else {
                            binding.buttonSavePassword.setEnabled(true);
                            Exception e = passTask.getException();
                            String err = e != null ? e.getMessage() : "Ошибка";
                            Toast.makeText(requireContext(), "Firebase Pass Error: " + err, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    binding.buttonSavePassword.setEnabled(true);
                    Exception e = task.getException();
                    Toast.makeText(requireContext(), "Ошибка: " + (e != null ? e.getMessage() : "неверный старый пароль"), Toast.LENGTH_LONG).show();
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