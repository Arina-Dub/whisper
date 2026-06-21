package com.example.pril;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class ImageUploader {

    private static final String API_KEY = "897615af70815ef6556928edb7d053dc";

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception e);
    }

    public static void uploadImage(Context context, Uri imageUri, UploadCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                byte[] bytes = getBytes(inputStream);
                String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

                URL url = new URL("https://api.imgbb.com/1/upload?key=" + API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "image=" + Uri.encode(base64Image);
                byte[] postDataBytes = postData.getBytes("UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postDataBytes);
                }

                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                
                JSONObject json = new JSONObject(response);
                String imageUrl = json.getJSONObject("data").getString("url");

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(imageUrl));

            } catch (Exception e) {
                Log.e("ImageUploader", "Upload failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }

    private static byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}