package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonRegister.setOnClickListener(v -> {
            String name = binding.editTextName.getText().toString().trim();
            String email = binding.editTextRegisterEmail.getText().toString().trim();
            String password = binding.editTextRegisterPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                binding.editTextRegisterPassword.setError("Пароль должен быть не менее 6 символов");
                return;
            }

            binding.progressBarRegister.setVisibility(View.VISIBLE);
            binding.buttonRegister.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity(), task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserToFirestore(user.getUid(), name, email, password);
                            }
                        } else {
                            binding.progressBarRegister.setVisibility(View.GONE);
                            binding.buttonRegister.setEnabled(true);
                            Toast.makeText(requireContext(), "Ошибка регистрации: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.textViewLoginLink.setOnClickListener(v -> {
            NavHostFragment.findNavController(RegisterFragment.this)
                    .navigate(R.id.action_RegisterFragment_to_LoginFragment);
        });
    }

    private void saveUserToFirestore(String uid, String name, String email, String password) {
        UserModel userModel = new UserModel(uid, name, email, null);
        db.collection("users").document(uid).set(userModel)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    AppPreferences prefs = new AppPreferences(requireContext());
                    prefs.setIsLoggedIn(true);
                    prefs.setUserName(name);
                    prefs.setUserEmail(email);
                    prefs.saveAccount(email, password, name, null);
                    binding.progressBarRegister.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(RegisterFragment.this)
                            .navigate(R.id.action_RegisterFragment_to_ChatsFragment);
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBarRegister.setVisibility(View.GONE);
                    binding.buttonRegister.setEnabled(true);
                    Toast.makeText(requireContext(), "Ошибка сохранения данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}