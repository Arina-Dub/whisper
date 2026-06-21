package com.example.pril;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private AppPreferences prefs;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateUI();

        binding.buttonEditProfile.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_EditProfileFragment);
        });

        binding.buttonNotifications.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_NotificationSettingsFragment);
        });

        binding.buttonPrivacy.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_PrivacyFragment);
        });

        binding.buttonData.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_DataStorageFragment);
        });

        binding.buttonLanguage.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_LanguageFragment);
        });

        binding.buttonEnergy.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_EnergySavingFragment);
        });

        binding.buttonLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            prefs.logout();
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SettingsFragment_to_LoginFragment);
        });

        binding.buttonSwitchAccount.setOnClickListener(v -> showSwitchAccountDialog());

        binding.buttonDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());

        binding.buttonThemeToggle.setOnClickListener(v -> toggleTheme());
    }

    private void showDeleteAccountDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие необратимо.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteAccount())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("isDeleted", true)
                    .addOnSuccessListener(aVoid -> {
                        user.delete().addOnCompleteListener(task -> {
                            prefs.logout();
                            if (isAdded()) {
                                NavHostFragment.findNavController(this)
                                        .navigate(R.id.action_SettingsFragment_to_LoginFragment);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Ошибка при удалении данных", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void toggleTheme() {
        boolean isDark = prefs.isDarkMode();
        boolean newDark = !isDark;
        prefs.setDarkMode(newDark);
        
        int mode = newDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
        
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    private void updateUI() {
        if (binding != null) {
            binding.textViewUserName.setText(prefs.getUserName());
            
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                binding.textViewUserEmail.setText(user.getEmail());
                
                user.reload().addOnCompleteListener(task -> {
                    if (binding != null && user.getEmail() != null) {
                        String currentEmail = user.getEmail();
                        binding.textViewUserEmail.setText(currentEmail);
                        
                        if (!currentEmail.equals(prefs.getUserEmail())) {
                            prefs.setUserEmail(currentEmail);
                            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                    .update("email", currentEmail);
                        }
                    }
                });
            } else {
                binding.textViewUserEmail.setText(prefs.getUserEmail());
            }

            if (prefs.isDarkMode()) {
                binding.buttonThemeToggle.setImageResource(R.drawable.night);
            } else {
                binding.buttonThemeToggle.setImageResource(R.drawable.light);
            }
            
            String avatarUri = prefs.getAvatarUri();
            loadAvatar(avatarUri);

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (binding != null && documentSnapshot.exists()) {
                                String firestoreAvatar = documentSnapshot.getString("avatarUrl");
                                if (firestoreAvatar != null && !firestoreAvatar.equals(avatarUri)) {
                                    prefs.setAvatarUri(firestoreAvatar);
                                    loadAvatar(firestoreAvatar);
                                }
                                
                                String firestoreName = documentSnapshot.getString("name");
                                if (firestoreName != null && !firestoreName.equals(prefs.getUserName())) {
                                    prefs.setUserName(firestoreName);
                                    binding.textViewUserName.setText(firestoreName);
                                }
                                
                                String firestoreEmail = documentSnapshot.getString("email");
                                if (firestoreEmail != null && user != null && !firestoreEmail.equals(user.getEmail())) {
                                    FirebaseFirestore.getInstance().collection("users").document(uid)
                                            .update("email", user.getEmail());
                                }
                            }
                        });
            }

        }
    }

    private void loadAvatar(String avatarUrl) {
        if (binding == null) return;
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.profile)
                    .circleCrop()
                    .into(binding.imageViewProfile);
            binding.imageViewProfile.setColorFilter(null);
        } else {
            binding.imageViewProfile.setImageResource(R.drawable.profile);
            if (prefs.isDarkMode()) {
                binding.imageViewProfile.setColorFilter(android.graphics.Color.WHITE);
            } else {
                binding.imageViewProfile.setColorFilter(null);
            }
        }
    }

    private void showSwitchAccountDialog() {
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
                    .setTitle("Переключить аккаунт")
                    .setView(dialogView)
                    .setNegativeButton("Отмена", null)
                    .create();

            SavedAccountAdapter adapter = new SavedAccountAdapter(list, new SavedAccountAdapter.OnAccountClickListener() {
                @Override
                public void onAccountClick(org.json.JSONObject account) {
                    try {
                        String email = account.getString("email");
                        String password = account.getString("password");
                        
                        dialog.dismiss();
                        switchAccount(email, password);
                    } catch (Exception e) { e.printStackTrace(); }
                }

                @Override
                public void onAccountDelete(String email) {
                    prefs.removeAccount(email);
                    dialog.dismiss();
                    showSwitchAccountDialog();
                }
            });

            rv.setAdapter(adapter);
            dialog.show();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void switchAccount(String email, String password) {
        FirebaseAuth.getInstance().signOut();
        prefs.logout();
        
        binding.buttonSwitchAccount.setEnabled(false);
        Toast.makeText(requireContext(), "Переключение...", Toast.LENGTH_SHORT).show();

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() == null) return;
                    String uid = authResult.getUser().getUid();
                    FirebaseFirestore.getInstance().collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (isAdded()) {
                                    prefs.setIsLoggedIn(true);
                                    prefs.setUserEmail(email);
                                    String name = "";
                                    String avatar = "";
                                    if (doc.exists()) {
                                        name = doc.getString("name");
                                        avatar = doc.getString("avatarUrl");
                                        prefs.setUserName(name);
                                        prefs.setAvatarUri(avatar);
                                    }
                                    prefs.saveAccount(email, password, name, avatar);
                                    
                                    Toast.makeText(requireContext(), "Успешно: " + email, Toast.LENGTH_SHORT).show();
                                    if (getActivity() != null) {
                                        getActivity().recreate();
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    prefs.setIsLoggedIn(true);
                                    prefs.setUserEmail(email);
                                    prefs.saveAccount(email, password, "", "");
                                    if (getActivity() != null) getActivity().recreate();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        binding.buttonSwitchAccount.setEnabled(true);
                        Toast.makeText(requireContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}