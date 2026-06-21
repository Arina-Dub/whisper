package com.example.pril;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private String avatarUrl;
    private String fcmToken;
    private String status;
    private boolean showOnlineStatus = true;
    private com.google.firebase.Timestamp lastSeen;

    public UserModel() {
    }

    public UserModel(String uid, String name, String email, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isShowOnlineStatus() { return showOnlineStatus; }
    public void setShowOnlineStatus(boolean showOnlineStatus) { this.showOnlineStatus = showOnlineStatus; }

    public com.google.firebase.Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(com.google.firebase.Timestamp lastSeen) { this.lastSeen = lastSeen; }
}