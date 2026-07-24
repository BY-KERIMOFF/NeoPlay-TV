package com.neoplay.tv.utils;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String UPDATE_URL = "https://kanal65.xyz/neoplay/update.json";
    
    private final Context context;
    private long downloadId = -1;

    public UpdateManager(Context context) {
        this.context = context;
    }

    public void checkForUpdates() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Checking for updates...");
                URL url = new URL(UPDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                int latestVersionCode = json.getInt("versionCode");
                String latestVersionName = json.getString("versionName");
                String apkUrl = json.getString("apkUrl");
                String notes = json.optString("releaseNotes", "");

                long currentVersionCode = getAppVersionCode();

                if (latestVersionCode > currentVersionCode) {
                    Log.d(TAG, "Update available: " + latestVersionName);
                    showUpdateDialog(latestVersionName, apkUrl, notes);
                } else {
                    Log.d(TAG, "App is up to date");
                }

            } catch (Exception e) {
                Log.e(TAG, "Update check failed: " + e.getMessage());
            }
        }).start();
    }

    private long getAppVersionCode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
            } else {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            }
        } catch (Exception e) {
            return 1;
        }
    }

    private void showUpdateDialog(String versionName, String apkUrl, String notes) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle("Yeni Yeniləmə Mövcuddur (v" + versionName + ")")
                        .setMessage(notes.isEmpty() ? "Tətbiqi yeniləmək istəyirsiniz?" : notes)
                        .setPositiveButton("YENİLƏ", (dialog, which) -> downloadAndInstall(apkUrl))
                        .setNegativeButton("SONRA", null)
                        .setCancelable(false)
                        .show();
            });
        }
    }

    private void downloadAndInstall(String apkUrl) {
        Toast.makeText(context, "Yükləmə başlayır...", Toast.LENGTH_LONG).show();
        
        // Daha etibarlı qovluq (Public Downloads)
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "neoplay_v2.apk");
        if (file.exists()) file.delete();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Neo Play Yeniləmə")
                .setDescription("Yeni versiya yüklənir...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file));

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            final long id = manager.enqueue(request);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == completedId) {
                        if (file.exists() && file.length() > 1000) { // Faylın real olduğunu yoxla
                            installApk(file);
                        } else {
                            Log.e(TAG, "Downloaded file is invalid or too small");
                        }
                        context.unregisterReceiver(this);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        }
    }

    private void installApk(File file) {
        try {
            Uri apkUri;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                apkUri = Uri.fromFile(file);
            }

            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Installation failed: " + e.getMessage());
            Toast.makeText(context, "Quraşdırma xətası: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
