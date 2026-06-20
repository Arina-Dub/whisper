package com.example.pril;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.pril.databinding.ItemChatBinding;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatModel> chatList;
    private final List<ChatModel> chatListFull;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(ChatModel chat);
        void onChatLongClick(ChatModel chat, int position);
    }

    public ChatAdapter(List<ChatModel> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.chatListFull = new ArrayList<>(chatList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatModel chat = chatList.get(position);
        holder.binding.textViewName.setText(chat.getName());
        holder.binding.textViewLastMessage.setText(chat.getLastMessage());
        holder.binding.textViewTime.setText(chat.getTime());
        
        if (chat.getAvatarUrl() != null && !chat.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chat.getAvatarUrl())
                    .placeholder(R.drawable.profile)
                    .circleCrop()
                    .into(holder.binding.imageViewAvatar);
        } else {
            holder.binding.imageViewAvatar.setImageResource(R.drawable.profile);
        }
        
        holder.binding.textViewStatus.setText(chat.getStatus());
        
        if (chat.getUnreadCount() > 0) {
            holder.binding.textViewUnreadCount.setText(String.valueOf(chat.getUnreadCount()));
            holder.binding.textViewUnreadCount.setVisibility(View.VISIBLE);
        } else {
            holder.binding.textViewUnreadCount.setVisibility(View.GONE);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onChatLongClick(chat, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public void updateList(List<ChatModel> newList) {
        this.chatList = newList;
        this.chatListFull.clear();
        this.chatListFull.addAll(newList);
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < chatList.size()) {
            ChatModel removedItem = chatList.get(position);
            chatList.remove(position);
            chatListFull.remove(removedItem);
            notifyItemRemoved(position);
        }
    }

    public void filter(String text) {
        List<ChatModel> filteredList = new ArrayList<>();
        if (text.isEmpty()) {
            filteredList.addAll(chatListFull);
        } else {
            String filterPattern = text.toLowerCase().trim();
            for (ChatModel item : chatListFull) {
                if (item.getName().toLowerCase().contains(filterPattern)) {
                    filteredList.add(item);
                }
            }
        }
        this.chatList = filteredList;
        notifyDataSetChanged();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ItemChatBinding binding;
        public ChatViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
