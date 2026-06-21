package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editTextEmail.getText().toString().trim();
            String password = binding.editTextPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.progressBarLogin.setVisibility(View.VISIBLE);
            binding.buttonLogin.setEnabled(false);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity(), task -> {
                        if (binding == null) return;
                        binding.progressBarLogin.setVisibility(View.GONE);
                        binding.buttonLogin.setEnabled(true);

                        if (task.isSuccessful()) {
                            fetchUserDataAndNavigate(email, password);
                        } else {
                            String errorMessage;
                            Exception exception = task.getException();
                            
                            if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                                errorMessage = getString(R.string.error_invalid_password);
                            } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                errorMessage = getString(R.string.error_user_not_found);
                            } else {
                                errorMessage = exception != null ? exception.getMessage() : getString(R.string.error_login_failed);
                            }
                            
                            Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        binding.buttonSavedAccounts.setOnClickListener(v -> showSavedAccountsDialog());

        binding.textViewRegister.setOnClickListener(v -> {
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_LoginFragment_to_RegisterFragment);
        });

        binding.textViewForgotPassword.setOnClickListener(v -> {
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_LoginFragment_to_ForgotPasswordFragment);
        });
    }

    private void showSavedAccountsDialog() {
        AppPreferences prefs = new AppPreferences(requireContext());
        try {
            org.json.JSONArray array = new org.json.JSONArray(prefs.getSavedAccounts());
            if (array.length() == 0) {
                Toast.makeText(requireContext(), "Нет сохраненных аккаунтов", Toast.LENGTH_SHORT).show();
                return;
            }

            java.util.List<org.json.JSONObject> list = new java.util.ArrayList<>();
            for (int i = 0; i < array.length(); i++) list.add(array.getJSONObject(i));

            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_saved_accounts, null);
            androidx.recyclerview.widget.RecyclerView rv = dialogView.findViewById(R.id.recyclerViewSavedAccounts);
            rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Выберите аккаунт")
                    .setView(dialogView)
                    .setNegativeButton("Закрыть", null)
                    .create();

            SavedAccountAdapter adapter = new SavedAccountAdapter(list, new SavedAccountAdapter.OnAccountClickListener() {
                @Override
                public void onAccountClick(org.json.JSONObject account) {
                    try {
                        binding.editTextEmail.setText(account.getString("email"));
                        binding.editTextPassword.setText(account.getString("password"));
                        dialog.dismiss();
                        binding.buttonLogin.performClick();
                    } catch (Exception e) { e.printStackTrace(); }
                }

                @Override
                public void onAccountDelete(String email) {
                    prefs.removeAccount(email);
                    dialog.dismiss();
                    showSavedAccountsDialog();
                }
            });

            rv.setAdapter(adapter);
            dialog.show();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchUserDataAndNavigate(String email, String password) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (binding == null) return;
            AppPreferences prefs = new AppPreferences(requireContext());
            prefs.setIsLoggedIn(true);
            prefs.setUserEmail(email);

            String name = "";
            String avatar = "";

            if (documentSnapshot.exists()) {
                UserModel user = documentSnapshot.toObject(UserModel.class);
                if (user != null) {
                    name = user.getName();
                    avatar = user.getAvatarUrl();
                    prefs.setUserName(name);
                    prefs.setAvatarUri(avatar);
                }
            }
            
            prefs.saveAccount(email, password, name, avatar);

            Toast.makeText(requireContext(), "Вход выполнен", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_LoginFragment_to_ChatsFragment);
        }).addOnFailureListener(e -> {
            if (binding == null) return;
            AppPreferences prefs = new AppPreferences(requireContext());
            prefs.setIsLoggedIn(true);
            prefs.setUserEmail(email);
            prefs.saveAccount(email, password, "", "");

            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_LoginFragment_to_ChatsFragment);
        });
    }
}
