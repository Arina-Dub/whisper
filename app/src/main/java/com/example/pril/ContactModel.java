package com.example.pril;

public class ContactModel {
    private String uid;
    private String name;
    private String phoneNumber;
    private String avatarUrl;

    public ContactModel() {
    }

    public ContactModel(String uid, String name, String phoneNumber, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.avatarUrl = avatarUrl;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}