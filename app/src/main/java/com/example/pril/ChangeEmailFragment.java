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
import com.example.pril.databinding.FragmentChangeEmailBinding;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ChangeEmailFragment extends Fragment {

    private FragmentChangeEmailBinding binding;
    private AppPreferences prefs;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChangeEmailBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSaveEmail.setOnClickListener(v -> {
            String newEmail = binding.editTextNewEmail.getText().toString().trim();
            String password = binding.editTextPasswordConfirm.getText().toString();

            if (newEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Введите новый Email", Toast.LENGTH_SHORT).show();
            } else if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Введите пароль для подтверждения", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(requireContext(), "Некорректный формат Email", Toast.LENGTH_SHORT).show();
            } else {
                updateEmail(newEmail, password);
            }
        });
    }

    private void updateEmail(String newEmail, String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(requireContext(), "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.buttonSaveEmail.setEnabled(false);
        Log.d("AuthLog", "Starting update for: " + user.getEmail());

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("AuthLog", "Re-authentication successful. Sending verification to: " + newEmail);
                
                user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(emailTask -> {
                    binding.buttonSaveEmail.setEnabled(true);
                    if (emailTask.isSuccessful()) {
                        Toast.makeText(requireContext(), "Ссылка для подтверждения отправлена на новый Email. Пожалуйста, подтвердите её в письме.", Toast.LENGTH_LONG).show();
                        NavHostFragment.findNavController(this).navigateUp();
                    } else {
                        Exception e = emailTask.getException();
                        Log.e("AuthLog", "Email update verification failed", e);
                        String err = e != null ? e.getMessage() : "Неизвестная ошибка";
                        
                        if (err.contains("operation is not allowed")) {
                            Toast.makeText(requireContext(), "Ошибка: метод подтверждения не включен в консоли Firebase (Email Link)", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(requireContext(), "Ошибка Firebase: " + err, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } else {
                binding.buttonSaveEmail.setEnabled(true);
                Exception e = task.getException();
                Log.e("AuthLog", "Re-authentication failed", e);
                Toast.makeText(requireContext(), "Ошибка проверки пароля: " + (e != null ? e.getMessage() : "Неверный пароль"), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
