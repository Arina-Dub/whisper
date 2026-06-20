package com.example.pril;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.example.pril.databinding.FragmentVideoDetailBinding;

public class VideoDetailFragment extends Fragment {

    private FragmentVideoDetailBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String videoUrl = null;
        if (getArguments() != null) {
            videoUrl = getArguments().getString("videoUrl");
        }

        if (videoUrl != null) {
            binding.progressBarVideo.setVisibility(View.VISIBLE);
            binding.videoView.setVideoURI(Uri.parse(videoUrl));
            
            MediaController mediaController = new MediaController(requireContext());
            mediaController.setAnchorView(binding.videoView);
            binding.videoView.setMediaController(mediaController);
            
            binding.videoView.setOnPreparedListener(mp -> {
                binding.progressBarVideo.setVisibility(View.GONE);
                binding.videoView.start();
            });

            binding.videoView.setOnErrorListener((mp, what, extra) -> {
                binding.progressBarVideo.setVisibility(View.GONE);
                return false;
            });
        }


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
