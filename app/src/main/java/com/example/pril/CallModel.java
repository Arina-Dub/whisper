package com.example.pril;

import com.google.firebase.Timestamp;
import java.util.List;

public class CallModel {
    public enum CallType { INCOMING, OUTGOING, MISSED }

    private String name;
    private String type; 
    private Timestamp timestamp;
    private int avatarRes;
    private String avatarUrl;
    private String senderId;
    private String senderName;
    private String receiverId;
    private String receiverName;
    private String id; 
    private String status;
    private String jitsiRoom;
    private List<String> participants;

    public CallModel() {}

    public CallModel(String name, CallType type, Timestamp timestamp, int avatarRes) {
        this.name = name;
        this.type = type.name();
        this.timestamp = timestamp;
        this.avatarRes = avatarRes;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public int getAvatarRes() { return avatarRes; }
    public void setAvatarRes(int avatarRes) { this.avatarRes = avatarRes; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJitsiRoom() { return jitsiRoom; }
    public void setJitsiRoom(String jitsiRoom) { this.jitsiRoom = jitsiRoom; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public CallType getCallType() {
        try {
            return CallType.valueOf(type);
        } catch (Exception e) {
            return CallType.INCOMING;
        }
    }
}
