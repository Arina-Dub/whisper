package com.example.pril;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.pril.databinding.FragmentChatsBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatsFragment extends Fragment {

    private FragmentChatsBinding binding;
    private ChatAdapter adapter;
    private List<ChatModel> chatList;
    private FirebaseFirestore db;
    private String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final java.util.Map<String, com.google.firebase.firestore.ListenerRegistration> unreadListeners = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> chatUnreadCounts = new java.util.HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatsBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatList = new ArrayList<>();
        adapter = new ChatAdapter(chatList, new ChatAdapter.OnChatClickListener() {
            @Override
            public void onChatClick(ChatModel chat) {
                if (NavHostFragment.findNavController(ChatsFragment.this).getCurrentDestination().getId() != R.id.ChatsFragment) {
                    return;
                }
                
                Bundle bundle = new Bundle();
                String name = chat.getName();
                bundle.putString("contactName", name != null ? name : getString(R.string.chat_default));
                bundle.putString("receiverId", chat.getOtherUserId());
                bundle.putString("chatId", chat.getChatId());
                bundle.putBoolean("isGroup", chat.isGroup());
                NavHostFragment.findNavController(ChatsFragment.this)
                        .navigate(R.id.action_ChatsFragment_to_ChatDetailFragment, bundle);
            }

            @Override
            public void onChatLongClick(ChatModel chat, int position) {
                showDeleteDialog(chat, position);
            }
        });

        binding.recyclerViewChats.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewChats.setAdapter(adapter);

        if (currentUserId != null) {
            loadChatsFromFirestore();
        }

        binding.searchViewChats.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        binding.fabAddChat.setOnClickListener(v -> 
                NavHostFragment.findNavController(this).navigate(R.id.action_ChatsFragment_to_ContactsFragment));
    }

    private void loadChatsFromFirestore() {
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("ChatLog", "Error loading chats: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;

                    List<ChatModel> displayChats = new ArrayList<>();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String chatId = doc.getId();
                        ChatModel chat = new ChatModel();
                        chat.setChatId(chatId);
                        chat.setLastMessage(doc.getString("lastMessage"));
                        Boolean isGroup = doc.getBoolean("isGroup");
                        chat.setGroup(isGroup != null && isGroup);
                        
                        Timestamp ts = doc.getTimestamp("lastMessageTime");
                        chat.setLastMessageTime(ts);
                        if (ts != null) {
                            chat.setTime(timeFormat.format(ts.toDate()));
                        }

                        List<String> participants = (List<String>) doc.get("participants");
                        chat.setParticipants(participants);

                        // Устанавливаем сохраненный счетчик
                        Integer savedCount = chatUnreadCounts.get(chatId);
                        chat.setUnreadCount(savedCount != null ? savedCount : 0);

                        if (chat.isGroup()) {
                            chat.setName(doc.getString("groupName"));
                            chat.setAvatarUrl(doc.getString("groupAvatarUrl"));
                        } else if (participants != null && participants.size() >= 2) {
                            String otherId = participants.get(0).equals(currentUserId) 
                                    ? participants.get(1) : participants.get(0);
                            chat.setOtherUserId(otherId);

                            db.collection("users").document(otherId).get().addOnSuccessListener(userDoc -> {
                                if (userDoc.exists() && isAdded()) {
                                    String name = userDoc.getString("name");
                                    String avatar = userDoc.getString("avatarUrl");
                                    Boolean isDeleted = userDoc.getBoolean("isDeleted");
                                    
                                    if (isDeleted != null && isDeleted) {
                                        chat.setName("Аккаунт удален");
                                        chat.setAvatarUrl(null);
                                    } else {
                                        chat.setName(name);
                                        chat.setAvatarUrl(avatar);
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }

                        // Слушатель непрочитанных сообщений (один на чат)
                        if (!unreadListeners.containsKey(chatId)) {
                            unreadListeners.put(chatId, db.collection("chats").document(chatId).collection("messages")
                                    .whereEqualTo("read", false)
                                    .addSnapshotListener((msgValue, msgError) -> {
                                        if (msgError != null) return;
                                        
                                        int count = 0;
                                        if (msgValue != null) {
                                            for (DocumentSnapshot mDoc : msgValue.getDocuments()) {
                                                if (currentUserId.equals(mDoc.getString("receiverId"))) {
                                                    count++;
                                                }
                                            }
                                        }
                                        
                                        chatUnreadCounts.put(chatId, count);
                                        
                                        // Обновляем текущие объекты в списке
                                        boolean changed = false;
                                        for (ChatModel c : chatList) {
                                            if (c.getChatId().equals(chatId)) {
                                                c.setUnreadCount(count);
                                                changed = true;
                                            }
                                        }
                                        if (isAdded() && changed) {
                                            adapter.notifyDataSetChanged();
                                        }
                                    }));
                        }

                        displayChats.add(chat);
                    }
                    
                    // Сортировка локально
                    java.util.Collections.sort(displayChats, (c1, c2) -> {
                        Timestamp t1 = c1.getLastMessageTime();
                        Timestamp t2 = c2.getLastMessageTime();
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        return t2.compareTo(t1);
                    });

                    chatList.clear();
                    chatList.addAll(displayChats);
                    adapter.updateList(chatList);
                });
    }

    private void showDeleteDialog(ChatModel chat, int position) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_chat_title))
                .setMessage("Удалить переписку с " + chat.getName() + "?")
                .setPositiveButton("Да", (d, w) -> {
                    // Удаляем текущего пользователя из списка участников чата
                    db.collection("chats").document(chat.getChatId())
                            .update("participants", FieldValue.arrayRemove(currentUserId))
                            .addOnSuccessListener(aVoid -> {
                                adapter.removeItem(position);
                            });
                })
                .setNegativeButton("Нет", null).show();
    }

    @Override
    public void onDestroyView() {
        for (com.google.firebase.firestore.ListenerRegistration lr : unreadListeners.values()) {
            lr.remove();
        }
        unreadListeners.clear();
        super.onDestroyView();
        binding = null;
    }
}
