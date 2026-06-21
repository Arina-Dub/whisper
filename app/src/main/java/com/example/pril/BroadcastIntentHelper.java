package com.example.pril;

import android.content.Intent;

/**
 * Вспомогательный класс для создания Broadcast Intents для Jitsi Meet SDK
 * Согласно официальной документации:
 * https://jitsi.github.io/handbook/docs/dev-guide/dev-guide-android-sdk#broadcasting-actions
 */
public class BroadcastIntentHelper {

    // Константы действий согласно документации Jitsi Meet SDK
    private static final String ACTION_SET_AUDIO_MUTED = "org.jitsi.meet.SET_AUDIO_MUTED";
    private static final String ACTION_SET_VIDEO_MUTED = "org.jitsi.meet.SET_VIDEO_MUTED";
    private static final String ACTION_HANG_UP = "org.jitsi.meet.HANG_UP";
    private static final String ACTION_TOGGLE_SCREEN_SHARE = "org.jitsi.meet.TOGGLE_SCREEN_SHARE";
    private static final String ACTION_OPEN_CHAT = "org.jitsi.meet.OPEN_CHAT";
    private static final String ACTION_CLOSE_CHAT = "org.jitsi.meet.CLOSE_CHAT";
    private static final String ACTION_SEND_CHAT_MESSAGE = "org.jitsi.meet.SEND_CHAT_MESSAGE";
    private static final String ACTION_SET_AUDIO_OUTPUT = "org.jitsi.meet.SET_AUDIO_OUTPUT";

    /**
     * Создает Intent для управления аудио (вкл/выкл микрофон)
     */
    public static Intent buildSetAudioMutedIntent(boolean muted) {
        Intent intent = new Intent(ACTION_SET_AUDIO_MUTED);
        intent.putExtra("muted", muted);
        return intent;
    }

    /**
     * Создает Intent для управления видео (вкл/выкл камера)
     */
    public static Intent buildSetVideoMutedIntent(boolean muted) {
        Intent intent = new Intent(ACTION_SET_VIDEO_MUTED);
        intent.putExtra("muted", muted);
        return intent;
    }

    /**
     * Создает Intent для завершения звонка
     */
    public static Intent buildHangUpIntent() {
        return new Intent(ACTION_HANG_UP);
    }

    /**
     * Создает Intent для переключения демонстрации экрана
     */
    public static Intent buildToggleScreenShareIntent(boolean enabled) {
        Intent intent = new Intent(ACTION_TOGGLE_SCREEN_SHARE);
        intent.putExtra("enabled", enabled);
        return intent;
    }

    /**
     * Создает Intent для открытия чата
     */
    public static Intent buildOpenChatIntent(String participantId) {
        Intent intent = new Intent(ACTION_OPEN_CHAT);
        if (participantId != null && !participantId.isEmpty()) {
            intent.putExtra("to", participantId);
        }
        return intent;
    }

    /**
     * Создает Intent для закрытия чата
     */
    public static Intent buildCloseChatIntent() {
        return new Intent(ACTION_CLOSE_CHAT);
    }

    /**
     * Создает Intent для отправки сообщения в чат
     */
    public static Intent buildSendChatMessageIntent(String message, String participantId) {
        Intent intent = new Intent(ACTION_SEND_CHAT_MESSAGE);
        intent.putExtra("message", message);
        if (participantId != null && !participantId.isEmpty()) {
            intent.putExtra("to", participantId);
        }
        return intent;
    }
}