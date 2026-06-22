package com.example.pril;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.FragmentChatDetailBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.io.File;
import java.io.IOException;
import android.media.MediaRecorder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatDetailFragment extends Fragment {

    private FragmentChatDetailBinding binding;
    private MessageAdapter adapter;
    private List<MessageModel> messages;

    private FirebaseFirestore db;
    private String currentUserId;
    private String receiverId;
    private String chatId;
    private ListenerRegistration messageListener;
    private ListenerRegistration userListener;
    private Uri cameraImageUri;
    private Uri cameraVideoUri;

    private MediaRecorder mediaRecorder;
    private String audioPath;
    private long audioStartTime;

    private final ActivityResultLauncher<String[]> mGetFiles = registerForActivityResult(
            new ActivityResultContracts.OpenMultipleDocuments(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        uploadChatFile(uri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Uri> mTakePicture = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraImageUri != null) {
                    uploadChatFile(cameraImageUri);
                }
            }
    );

    private final ActivityResultLauncher<Uri> mRecordVideo = registerForActivityResult(
            new ActivityResultContracts.CaptureVideo(),
            success -> {
                if (success && cameraVideoUri != null) {
                    uploadChatFile(cameraVideoUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> mPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean recordAudio = result.get(Manifest.permission.RECORD_AUDIO);
                if (recordAudio != null && recordAudio) {
                    Toast.makeText(getContext(), "Разрешение получено", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final SimpleDateFormat timeOnlyFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    private String formatLastSeen(Timestamp timestamp) {
        if (timestamp == null) return "";
        java.util.Calendar lastSeenCal = java.util.Calendar.getInstance();
        lastSeenCal.setTime(timestamp.toDate());
        java.util.Calendar now = java.util.Calendar.getInstance();
        if (now.get(java.util.Calendar.YEAR) == lastSeenCal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == lastSeenCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "был(а) в " + timeOnlyFormat.format(timestamp.toDate());
        }
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DATE, -1);
        if (yesterday.get(java.util.Calendar.YEAR) == lastSeenCal.get(java.util.Calendar.YEAR) &&
            yesterday.get(java.util.Calendar.DAY_OF_YEAR) == lastSeenCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            return "был(а) вчера в " + timeOnlyFormat.format(timestamp.toDate());
        }
        return "был(а) " + dateOnlyFormat.format(timestamp.toDate()) + " в " + timeOnlyFormat.format(timestamp.toDate());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (getArguments() != null) receiverId = getArguments().getString("receiverId");
        if (currentUserId != null && receiverId != null) {
            chatId = getChatId(currentUserId, receiverId);
            MainService.activeChatId = chatId;
        }
        if (savedInstanceState != null) {
            String imageUriStr = savedInstanceState.getString("cameraImageUri");
            if (imageUriStr != null) cameraImageUri = Uri.parse(imageUriStr);
            String videoUriStr = savedInstanceState.getString("cameraVideoUri");
            if (videoUriStr != null) cameraVideoUri = Uri.parse(videoUriStr);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messages = new ArrayList<>();
        adapter = new MessageAdapter(messages);
        adapter.setOnImageClickListener(this::openFullScreenImage);
        adapter.setOnVideoClickListener(this::openFullScreenVideo);
        adapter.setOnMessageLongClickListener(this::showMessageOptions);
        binding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewMessages.setAdapter(adapter);

        binding.buttonBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        
        View.OnClickListener openProfileListener = v -> {
            if (receiverId != null) {
                Bundle bundle = new Bundle();
                bundle.putString("userId", receiverId);
                NavHostFragment.findNavController(this).navigate(R.id.action_ChatDetailFragment_to_UserProfileFragment, bundle);
            }
        };

        binding.textViewTitle.setOnClickListener(openProfileListener);
        binding.imageViewAvatar.setOnClickListener(openProfileListener);
        binding.textViewStatus.setOnClickListener(openProfileListener);

        binding.buttonCall.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("receiverId", receiverId);
            bundle.putString("receiverName", binding.textViewTitle.getText().toString());
            NavHostFragment.findNavController(this).navigate(R.id.action_ChatDetailFragment_to_CallMenuFragment, bundle);
        });

        binding.buttonMic.setOnTouchListener((v, event) -> {
            String text = binding.editTextMessage.getText().toString().trim();
            if (text.isEmpty()) {
                handleVoiceRecording(event);
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendMessage(text, "text", null);
                binding.editTextMessage.setText("");
                return true;
            }
            return false;
        });

        binding.buttonAttach.setOnClickListener(v -> showAttachmentOptions());
        binding.buttonCamera.setOnClickListener(v -> showCameraOptions());

        binding.editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    binding.buttonMic.setImageResource(android.R.drawable.ic_menu_send);
                } else {
                    binding.buttonMic.setImageResource(R.drawable.microphone);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private String getChatId(String id1, String id2) {
        return (id1.compareTo(id2) < 0) ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    private void listenForReceiverStatus() {
        userListener = db.collection("users").document(receiverId).addSnapshotListener((doc, error) -> {
            if (doc != null && doc.exists() && binding != null) {
                Boolean isDeleted = doc.getBoolean("isDeleted");
                if (isDeleted != null && isDeleted) {
                    binding.textViewTitle.setText("Аккаунт удален");
                    binding.textViewStatus.setText("");
                    binding.buttonCall.setVisibility(View.GONE);
                    binding.layoutInput.setVisibility(View.GONE);
                    return;
                }
                
                binding.layoutInput.setVisibility(View.VISIBLE);
                binding.buttonCall.setVisibility(View.VISIBLE);
                
                String name = doc.getString("name");
                String avatarUrl = doc.getString("avatarUrl");
                String status = doc.getString("status");
                Timestamp lastSeen = doc.getTimestamp("lastSeen");
                Boolean showOnline = doc.getBoolean("showOnlineStatus");
                if (showOnline == null) showOnline = true;

                binding.textViewTitle.setText(name != null ? name : "Чат");
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(this).load(avatarUrl).placeholder(R.drawable.profile).circleCrop().into(binding.imageViewAvatar);
                }

                if (!showOnline) {
                    binding.textViewStatus.setText("");
                } else {
                    if ("online".equals(status)) {
                        binding.textViewStatus.setText("в сети");
                        binding.textViewStatus.setTextColor(getResources().getColor(R.color.primary_pink));
                    } else if (lastSeen != null) {
                        binding.textViewStatus.setText(formatLastSeen(lastSeen));
                        binding.textViewStatus.setTextColor(0xFF999999);
                    } else {
                        binding.textViewStatus.setText("");
                    }
                }
            }
        });
    }

    private void openFullScreenImage(String imageUrl) {
        Bundle bundle = new Bundle();
        bundle.putString("imageUrl", imageUrl);
        NavHostFragment.findNavController(this).navigate(R.id.action_ChatDetailFragment_to_ImageDetailFragment, bundle);
    }

    private void openFullScreenVideo(String videoUrl) {
        Bundle bundle = new Bundle();
        bundle.putString("videoUrl", videoUrl);
        NavHostFragment.findNavController(this).navigate(R.id.action_ChatDetailFragment_to_VideoDetailFragment, bundle);
    }

    private void showCameraOptions() {
        String[] options = {"Сделать фото", "Записать видео"};
        new AlertDialog.Builder(requireContext()).setTitle("Камера").setItems(options, (dialog, which) -> {
            if (which == 0) openCamera(); else recordVideo();
        }).show();
    }

    private void openCamera() {
        try {
            File file = new File(requireContext().getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "com.example.pril.fileprovider", file);
            mTakePicture.launch(cameraImageUri);
        } catch (Exception e) { Log.e("ChatLog", "Error opening camera", e); }
    }

    private void recordVideo() {
        try {
            File file = new File(requireContext().getCacheDir(), "camera_video_" + System.currentTimeMillis() + ".mp4");
            cameraVideoUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "com.example.pril.fileprovider", file);
            mRecordVideo.launch(cameraVideoUri);
        } catch (Exception e) { Log.e("ChatLog", "Error recording video", e); }
    }

    private void handleVoiceRecording(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                } else {
                    startRecording();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                stopRecording();
                break;
        }
    }

    private void startRecording() {
        try {
            audioPath = requireContext().getCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioPath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            audioStartTime = System.currentTimeMillis();
            Toast.makeText(getContext(), "Запись голосового...", Toast.LENGTH_SHORT).show();
            binding.buttonMic.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start();
        } catch (IOException e) {
            Log.e("ChatLog", "Recording failed", e);
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                long duration = System.currentTimeMillis() - audioStartTime;
                if (duration > 1000) {
                    uploadDirectly(Uri.fromFile(new File(audioPath)), "audio");
                } else {
                    new File(audioPath).delete();
                }
            } catch (Exception e) {
                Log.e("ChatLog", "Stop recording failed", e);
            }
            binding.buttonMic.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    private void listenForMessages() {
        if (chatId == null || messageListener != null) return;
        messageListener = db.collection("chats").document(chatId).collection("messages").addSnapshotListener((value, error) -> {
            if (error != null || value == null || binding == null) return;
            List<MessageModel> newMessages = new ArrayList<>();
            for (DocumentSnapshot doc : value.getDocuments()) {
                MessageModel msg = doc.toObject(MessageModel.class);
                if (msg != null) {
                    msg.setMessageId(doc.getId());
                    
                    if (msg.getDeletedBy() != null && msg.getDeletedBy().contains(currentUserId)) {
                        continue;
                    }
                    
                    newMessages.add(msg);
                    Boolean isRead = doc.getBoolean("read");
                    if (currentUserId.equals(msg.getReceiverId()) && (isRead == null || !isRead)) {
                        doc.getReference().update("read", true);
                    }
                }
            }
            Collections.sort(newMessages, (m1, m2) -> {
                if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                if (m1.getTimestamp() == null) return 1;
                if (m2.getTimestamp() == null) return -1;
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            });
            messages.clear();
            messages.addAll(newMessages);
            adapter.notifyDataSetChanged();
            if (!messages.isEmpty()) binding.recyclerViewMessages.scrollToPosition(messages.size() - 1);
        });
    }

    private void showMessageOptions(MessageModel message, int position) {
        String[] options;
        if (currentUserId.equals(message.getSenderId())) {
            options = new String[]{"Удалить у меня", "Удалить у всех"};
        } else {
            options = new String[]{"Удалить у меня"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите действие")
                .setItems(options, (dialog, which) -> {
                    if (options[which].equals("Удалить у меня")) {
                        deleteMessageForMe(message.getMessageId());
                    } else if (options[which].equals("Удалить у всех")) {
                        deleteMessageForEveryone(message.getMessageId());
                    }
                })
                .show();
    }

    private void deleteMessageForMe(String messageId) {
        if (chatId == null || messageId == null) return;
        db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update("deletedBy", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Сообщение удалено для вас", Toast.LENGTH_SHORT).show();
                    updateLastMessageAfterDeletion();
                });
    }

    private void deleteMessageForEveryone(String messageId) {
        if (chatId == null || messageId == null) return;
        db.collection("chats").document(chatId).collection("messages").document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Сообщение удалено у всех", Toast.LENGTH_SHORT).show();
                    updateLastMessageAfterDeletion();
                });
    }

    private void updateLastMessageAfterDeletion() {
        if (chatId == null) return;
        
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Object> update = new HashMap<>();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot lastDoc = queryDocumentSnapshots.getDocuments().get(0);
                        update.put("lastMessage", lastDoc.getString("text"));
                        update.put("lastMessageTime", lastDoc.getTimestamp("timestamp"));
                        update.put("lastMessageSenderId", lastDoc.getString("senderId"));
                    } else {
                        update.put("lastMessage", "");
                        update.put("lastMessageTime", null);
                        update.put("lastMessageSenderId", "");
                    }
                    db.collection("chats").document(chatId).update(update);
                });
    }

    private void sendMessage(String text, String type, String imageUrl) {
        if (chatId == null || currentUserId == null || receiverId == null) return;
        Map<String, Object> message = new HashMap<>();
        message.put("text", text != null ? text : "");
        message.put("senderId", currentUserId);
        message.put("receiverId", receiverId);
        message.put("type", type);
        message.put("read", false);
        message.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        message.put("deletedBy", new ArrayList<String>());
        if (imageUrl != null) message.put("imageUrl", imageUrl);
        db.collection("chats").document(chatId).collection("messages").add(message);

        Map<String, Object> chatUpdate = new HashMap<>();
        String lastMessageText = text != null && !text.isEmpty() ? text : getMediaTypeText(type);
        chatUpdate.put("lastMessage", lastMessageText);
        chatUpdate.put("lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp());
        chatUpdate.put("lastMessageSenderId", currentUserId);
        chatUpdate.put("participants", java.util.Arrays.asList(currentUserId, receiverId));
        db.collection("chats").document(chatId).set(chatUpdate, com.google.firebase.firestore.SetOptions.merge());
    }

    private String getMediaTypeText(String type) {
        if (type == null) return "Сообщение";
        switch (type) {
            case "image": return "Фото";
            case "video": return "Видео";
            case "audio": return "Голосовое";
            default: return "Файл";
        }
    }
    private void showAttachmentOptions() {
        String[] options = {"Галерея (Фото)", "Файловый менеджер"};
        new AlertDialog.Builder(requireContext()).setTitle("Вложение").setItems(options, (dialog, which) -> {
            if (which == 0) mGetFiles.launch(new String[]{"image/*"}); else mGetFiles.launch(new String[]{"*/*"});
        }).show();
    }

    private void uploadChatFile(Uri uri) {
        if (uri == null) return;
        String mediaType = "file";
        String mime = requireContext().getContentResolver().getType(uri);
        if (mime == null) {
            String path = uri.getPath();
            if (path != null) {
                if (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")) mediaType = "image";
                else if (path.endsWith(".mp4") || path.endsWith(".3gp")) mediaType = "video";
            }
        } else {
            if (mime.startsWith("image")) mediaType = "image";
            else if (mime.startsWith("video")) mediaType = "video";
            else if (mime.startsWith("audio")) mediaType = "audio";
        }
        uploadDirectly(uri, mediaType);
    }

    private void uploadDirectly(Uri uri, String forcedType) {
        if (getContext() == null) return;
        Toast.makeText(requireContext(), "Отправка...", Toast.LENGTH_SHORT).show();
        FileUploader.UploadCallback callback = new FileUploader.UploadCallback() {
            @Override
            public void onSuccess(String fileUrl) {
                if (isAdded()) {
                    sendMessage(null, forcedType, fileUrl);
                }
            }
            @Override
            public void onFailure(Exception e) {
                if (isAdded()) Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show();
            }
        };
        if ("image".equals(forcedType)) FileUploader.uploadImage(requireContext(), uri, callback);
        else FileUploader.uploadFile(requireContext(), uri, callback);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) outState.putString("cameraImageUri", cameraImageUri.toString());
        if (cameraVideoUri != null) outState.putString("cameraVideoUri", cameraVideoUri.toString());
    }

    @Override public void onStart() {
        super.onStart();
        if (chatId != null) listenForMessages();
        if (receiverId != null) listenForReceiverStatus();
    }

    @Override public void onStop() {
        super.onStop();
        if (messageListener != null) { messageListener.remove(); messageListener = null; }
        if (userListener != null) { userListener.remove(); userListener = null; }
        MainService.activeChatId = null;
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}