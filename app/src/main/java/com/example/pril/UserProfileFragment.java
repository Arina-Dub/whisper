package com.example.pril;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentUserProfileBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private String userId;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        if (userId != null) {
            loadUserProfile();
        }

        binding.buttonProfileCall.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("receiverId", userId);
            bundle.putString("receiverName", binding.textViewProfileName.getText().toString());
            NavHostFragment.findNavController(this).navigate(R.id.action_UserProfileFragment_to_CallMenuFragment, bundle);
        });
    }

    private void loadUserProfile() {
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && isAdded() && binding != null) {
                String name = documentSnapshot.getString("name");
                String email = documentSnapshot.getString("email");
                String avatarUrl = documentSnapshot.getString("avatarUrl");

                binding.textViewProfileName.setText(name);
                binding.textViewProfileEmail.setText(email);

                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .placeholder(R.drawable.profile)
                            .into(binding.imageViewProfileAvatar);
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
