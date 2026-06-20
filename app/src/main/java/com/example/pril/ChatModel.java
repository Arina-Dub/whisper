package com.example.pril;

import java.util.List;

public class ChatModel {
    private String chatId;
    private String otherUserId;
    private String name;
    private String lastMessage;
    private String time;
    private String avatarUrl;
    private String status;
    private int unreadCount;
    private boolean isGroup;
    private List<String> participants;
    private com.google.firebase.Timestamp lastMessageTime;

    public ChatModel() {
        // Required for Firebase
    }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isGroup() { return isGroup; }
    public void setGroup(boolean group) { isGroup = group; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public ChatModel(String name, String lastMessage, String time, String avatarUrl, String status) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.time = time;
        this.avatarUrl = avatarUrl;
        this.status = status;
    }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public com.google.firebase.Timestamp getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(com.google.firebase.Timestamp lastMessageTime) { this.lastMessageTime = lastMessageTime; }
}
