package com.example.pril;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.List;

public class MessageModel {
    private String text;
    private String senderId;
    private String receiverId;
    private String imageUrl;
    private String type;
    private boolean read;
    private String messageId;
    private List<String> deletedBy;
    @ServerTimestamp
    private Timestamp timestamp;

    public MessageModel() {
    }

    public MessageModel(String text, String senderId, String receiverId, String type) {
        this.text = text;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = type;
        this.read = false;
    }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getDeletedBy() { return deletedBy; }
    public void setDeletedBy(List<String> deletedBy) { this.deletedBy = deletedBy; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}