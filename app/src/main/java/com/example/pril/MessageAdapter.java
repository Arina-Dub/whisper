package com.example.pril;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.ItemMessageReceivedBinding;
import com.example.pril.databinding.ItemMessageSentBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;
    private Context context;

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
    }

    public interface OnVideoClickListener {
        void onVideoClick(String videoUrl);
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageModel message, int position);
    }

    private final List<MessageModel> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private OnImageClickListener onImageClickListener;
    private OnVideoClickListener onVideoClickListener;
    private OnMessageLongClickListener onMessageLongClickListener;

    public MessageAdapter(List<MessageModel> messages) {
        this.messages = messages;
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.onMessageLongClickListener = listener;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public void setOnVideoClickListener(OnVideoClickListener listener) {
        this.onVideoClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (viewType == VIEW_TYPE_SENT) {
            ItemMessageSentBinding binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new SentViewHolder(binding);
        } else {
            ItemMessageReceivedBinding binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ReceivedViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        String time = message.getTimestamp() != null ? timeFormat.format(message.getTimestamp().toDate()) : "";

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            resetUI(h.binding.layoutMediaSent, h.binding.layoutAudioSent, h.binding.textViewMessageSent, h.binding.imageViewPlayIconSent);

            if ("image".equals(message.getType())) {
                h.binding.layoutMediaSent.setVisibility(View.VISIBLE);
                h.binding.textViewMessageSent.setVisibility(message.getText().isEmpty() ? View.GONE : View.VISIBLE);
                h.binding.imageViewPlayIconSent.setVisibility(View.GONE);
                Glide.with(h.itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(h.binding.imageViewMessageSent);
                h.binding.imageViewMessageSent.setOnClickListener(v -> {
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(message.getImageUrl());
                    }
                });
            } else if ("video".equals(message.getType())) {
                h.binding.layoutMediaSent.setVisibility(View.VISIBLE);
                h.binding.imageViewPlayIconSent.setVisibility(View.VISIBLE);
                h.binding.textViewMessageSent.setVisibility(View.GONE);
                // Загружаем превью видео
                Glide.with(h.itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(h.binding.imageViewMessageSent);
                // Клик по иконке Play или по всему контейнеру
                View.OnClickListener videoClickListener = v -> {
                    String videoUrl = message.getImageUrl();
                    if (onVideoClickListener != null) {
                        onVideoClickListener.onVideoClick(videoUrl);
                    } else {
                        openVideo(videoUrl);
                    }
                };
                h.binding.layoutMediaSent.setOnClickListener(videoClickListener);
                h.binding.imageViewPlayIconSent.setOnClickListener(videoClickListener);
                h.binding.imageViewMessageSent.setOnClickListener(null);
            } else if ("audio".equals(message.getType())) {
                h.binding.layoutAudioSent.setVisibility(View.VISIBLE);
                h.binding.textViewMessageSent.setVisibility(View.GONE);

                boolean isPlaying = playingPosition == position;
                h.binding.buttonPlayAudioSent.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                h.binding.buttonPlayAudioSent.setOnClickListener(v -> toggleAudio(h.itemView.getContext(), message.getImageUrl(), position));
            } else {
                h.binding.textViewMessageSent.setVisibility(View.VISIBLE);
                h.binding.textViewMessageSent.setText(message.getText());
            }
            h.binding.textViewMessageSent.setText(message.getText());
            h.binding.textViewTimeSent.setText(time);

            // Устанавливаем статус прочтения
            if (message.isRead()) {
                h.binding.imageViewStatusSent.setImageResource(R.drawable.ic_double_check);
                h.binding.imageViewStatusSent.setColorFilter(0xFF00E676);
            } else {
                h.binding.imageViewStatusSent.setImageResource(R.drawable.ic_check);
                h.binding.imageViewStatusSent.setColorFilter(null);
            }

            h.itemView.setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(message, position);
                }
                return true;
            });

        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            resetUI(h.binding.layoutMediaReceived, h.binding.layoutAudioReceived, h.binding.textViewMessageReceived, h.binding.imageViewPlayIconReceived);

            if ("image".equals(message.getType())) {
                h.binding.layoutMediaReceived.setVisibility(View.VISIBLE);
                h.binding.textViewMessageReceived.setVisibility(message.getText().isEmpty() ? View.GONE : View.VISIBLE);
                h.binding.imageViewPlayIconReceived.setVisibility(View.GONE);
                Glide.with(h.itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(h.binding.imageViewMessageReceived);
                h.binding.imageViewMessageReceived.setOnClickListener(v -> {
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(message.getImageUrl());
                    }
                });
            } else if ("video".equals(message.getType())) {
                h.binding.layoutMediaReceived.setVisibility(View.VISIBLE);
                h.binding.imageViewPlayIconReceived.setVisibility(View.VISIBLE);
                h.binding.textViewMessageReceived.setVisibility(View.GONE);
                // Загружаем превью видео
                Glide.with(h.itemView.getContext())
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(h.binding.imageViewMessageReceived);
                // Клик по иконке Play или по всему контейнеру
                View.OnClickListener videoClickListener = v -> {
                    String videoUrl = message.getImageUrl();
                    if (onVideoClickListener != null) {
                        onVideoClickListener.onVideoClick(videoUrl);
                    } else {
                        openVideo(videoUrl);
                    }
                };
                h.binding.layoutMediaReceived.setOnClickListener(videoClickListener);
                h.binding.imageViewPlayIconReceived.setOnClickListener(videoClickListener);
                h.binding.imageViewMessageReceived.setOnClickListener(null);
            } else if ("audio".equals(message.getType())) {
                h.binding.layoutAudioReceived.setVisibility(View.VISIBLE);
                h.binding.textViewMessageReceived.setVisibility(View.GONE);

                boolean isPlaying = playingPosition == position;
                h.binding.buttonPlayAudioReceived.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                h.binding.buttonPlayAudioReceived.setOnClickListener(v -> toggleAudio(h.itemView.getContext(), message.getImageUrl(), position));
            } else {
                h.binding.textViewMessageReceived.setVisibility(View.VISIBLE);
                h.binding.textViewMessageReceived.setText(message.getText());
            }
            h.binding.textViewMessageReceived.setText(message.getText());
            h.binding.textViewTimeReceived.setText(time);

            h.itemView.setOnLongClickListener(v -> {
                if (onMessageLongClickListener != null) {
                    onMessageLongClickListener.onMessageLongClick(message, position);
                }
                return true;
            });
        }
    }

    private void resetUI(View media, View audio, View text, View playIcon) {
        media.setVisibility(View.GONE);
        audio.setVisibility(View.GONE);
        text.setVisibility(View.GONE);
        if (playIcon != null) playIcon.setVisibility(View.GONE);
    }

    /**
     * Открывает видео через Intent
     */
    private void openVideo(String videoPath) {
        if (context == null || videoPath == null || videoPath.isEmpty()) {
            if (context != null) {
                Toast.makeText(context, "Видео не найдено", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            // Проверяем, является ли путь URL
            if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
                // Открываем URL в браузере или видеоплеере
                Uri uri = Uri.parse(videoPath);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setDataAndType(uri, "video/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    // Если нет видеоплеера, пробуем открыть в браузере
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (browserIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(browserIntent);
                    } else {
                        Toast.makeText(context, "Нет приложения для открытия видео", Toast.LENGTH_SHORT).show();
                    }
                }
                return;
            }

            // Локальный файл
            File videoFile = new File(videoPath);
            if (!videoFile.exists()) {
                Toast.makeText(context, "Файл видео не найден", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    videoFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Нет приложения для открытия видео", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Ошибка открытия видео: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleAudio(Context context, String url, int position) {
        if (playingPosition == position) {
            stopAudio();
        } else {
            playAudio(context, url, position);
        }
    }

    private void playAudio(Context context, String url, int position) {
        stopAudio();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                playingPosition = position;
                notifyItemChanged(position);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                stopAudio();
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(context, "Ошибка воспроизведения аудио", Toast.LENGTH_SHORT).show();
                stopAudio();
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Ошибка воспроизведения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAudio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaPlayer = null;
            int oldPos = playingPosition;
            playingPosition = -1;
            if (oldPos != -1) notifyItemChanged(oldPos);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        stopAudio();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        ItemMessageSentBinding binding;
        SentViewHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        ItemMessageReceivedBinding binding;
        ReceivedViewHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}