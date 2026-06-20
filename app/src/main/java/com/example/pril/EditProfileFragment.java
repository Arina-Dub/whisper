package com.example.pril;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AppPreferences prefs;
    private ActivityResultLauncher<String> mGetImage;
    private ActivityResultLauncher<String> mGetFile;
    private ActivityResultLauncher<Uri> mTakePicture;
    private Uri cameraImageUri;
    private Uri selectedImageUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mGetImage = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        binding.imageViewEditAvatar.setImageURI(uri);
                    }
                });

        mGetFile = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        binding.imageViewEditAvatar.setImageURI(uri);
                    }
                });

        mTakePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        binding.imageViewEditAvatar.setImageURI(cameraImageUri);
                    }
                });

        if (savedInstanceState != null) {
            String uriStr = savedInstanceState.getString("cameraImageUri");
            if (uriStr != null) cameraImageUri = Uri.parse(uriStr);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) outState.putString("cameraImageUri", cameraImageUri.toString());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = new AppPreferences(requireContext());
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.imageViewEditAvatar.setVisibility(View.VISIBLE);
        binding.buttonChangePhoto.setVisibility(View.VISIBLE);

        binding.buttonChangePhoto.setOnClickListener(v -> showAvatarOptions());

        String uid = mAuth.getUid();
        if (uid != null) {
            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null) {
                        binding.editTextProfileName.setText(user.getName());
                        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                            Glide.with(this).load(user.getAvatarUrl()).into(binding.imageViewEditAvatar);
                        }
                    }
                }
            });
        }

        binding.textViewChangeEmail.setOnClickListener(v -> 
                NavHostFragment.findNavController(this).navigate(R.id.action_EditProfileFragment_to_ChangeEmailFragment));

        binding.textViewChangePassword.setOnClickListener(v -> 
                NavHostFragment.findNavController(this).navigate(R.id.action_EditProfileFragment_to_ChangePasswordFragment));

        binding.buttonSaveProfile.setOnClickListener(v -> {
            String newName = binding.editTextProfileName.getText().toString().trim();
            if (!newName.isEmpty()) {
                saveProfile(newName);
            } else {
                Toast.makeText(requireContext(), "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAvatarOptions() {
        String[] options = {"Камера", "Галерея", "Файлы"};
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Изменить фото")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        mGetImage.launch("image/*");
                    } else {
                        mGetFile.launch("*/*");
                    }
                })
                .show();
    }

    private void openCamera() {
        try {
            File photoFile = File.createTempFile("AVATAR_", ".jpg", requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(requireContext(), "com.example.pril.fileprovider", photoFile);
            mTakePicture.launch(cameraImageUri);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Ошибка камеры", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile(String name) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        binding.progressBarEditProfile.setVisibility(View.VISIBLE);
        binding.buttonSaveProfile.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImage(uid, name);
        } else {
            updateFirestore(uid, name, null);
        }
    }

    private void uploadImage(String uid, String name) {
        ImageUploader.uploadImage(requireContext(), selectedImageUri, new ImageUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                updateFirestore(uid, name, imageUrl);
            }

            @Override
            public void onFailure(Exception e) {
                if (binding == null) return;
                binding.progressBarEditProfile.setVisibility(View.GONE);
                binding.buttonSaveProfile.setEnabled(true);
                Toast.makeText(requireContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestore(String uid, String name, String avatarUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (avatarUrl != null) {
            updates.put("avatarUrl", avatarUrl);
        }

        db.collection("users").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (binding == null) return;
                    
                    // Обновляем локальные настройки
                    prefs.setUserName(name);
                    if (avatarUrl != null) {
                        prefs.setAvatarUri(avatarUrl);
                    }

                    binding.progressBarEditProfile.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Профиль обновлен", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    if (binding == null) return;
                    binding.progressBarEditProfile.setVisibility(View.GONE);
                    binding.buttonSaveProfile.setEnabled(true);
                    Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
