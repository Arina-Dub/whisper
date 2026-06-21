package com.example.pril;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FileUploader {

    private static final String IMGBB_API_KEY = "897615af70815ef6556928edb7d053dc";

    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onFailure(Exception e);
    }

    public static void uploadImage(Context context, Uri uri, UploadCallback callback) {
        new Thread(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) throw new Exception("Could not open input stream");
                byte[] bytes = getBytes(inputStream);
                inputStream.close();
                String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

                URL url = new URL("https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String postData = "image=" + Uri.encode(base64Image);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned code: " + conn.getResponseCode());
                }

                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject json = new JSONObject(response);
                String imageUrl = json.getJSONObject("data").getString("url");

                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(imageUrl));
            } catch (Exception e) {
                Log.e("Uploader", "ImgBB upload failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }

    public static void uploadFile(Context context, Uri uri, UploadCallback callback) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String fileName = "file_" + System.currentTimeMillis();
                String mime = context.getContentResolver().getType(uri);
                if (mime != null) {
                    if (mime.contains("video")) fileName += ".mp4";
                    else if (mime.contains("audio")) fileName += ".3gp";
                }

                String boundary = "UploadBoundary" + System.currentTimeMillis();
                URL url = new URL("https://litterbox.catbox.moe/resources/internals/api.php");
                
                applySslBypass();
                
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream os = conn.getOutputStream()) {
                    String boundaryLine = "--" + boundary + "\r\n";

                    os.write(boundaryLine.getBytes(StandardCharsets.UTF_8));
                    os.write("Content-Disposition: form-data; name=\"reqtype\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    os.write("fileupload\r\n".getBytes(StandardCharsets.UTF_8));

                    os.write(boundaryLine.getBytes(StandardCharsets.UTF_8));
                    os.write("Content-Disposition: form-data; name=\"time\"\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    os.write("24h\r\n".getBytes(StandardCharsets.UTF_8));

                    os.write(boundaryLine.getBytes(StandardCharsets.UTF_8));
                    os.write(("Content-Disposition: form-data; name=\"fileToUpload\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                    os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));

                    InputStream inputStream = context.getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        inputStream.close();
                    }

                    os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();
                
                if (is != null) {
                    Scanner s = new Scanner(is).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";
                    is.close();

                    if (responseCode == 200 && result.startsWith("http")) {
                        final String finalUrl = result.trim();
                        new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(finalUrl));
                    } else {
                        throw new Exception("Server: " + result);
                    }
                }
            } catch (Exception e) {
                Log.e("Uploader", "Upload failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static void applySslBypass() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.e("Uploader", "SSL bypass failed", e);
        }
    }

    private static byte[] getBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}