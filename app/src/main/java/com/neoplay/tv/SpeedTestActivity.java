package com.neoplay.tv;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import androidx.appcompat.app.AppCompatActivity;
import com.neoplay.tv.databinding.ActivitySpeedTestBinding;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeedTestActivity extends AppCompatActivity {

    private ActivitySpeedTestBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySpeedTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        setupFocusEffect(binding.btnStartTest);
        setupFocusEffect(binding.btnBack);

        binding.btnStartTest.setOnClickListener(v -> startSpeedTest());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void startSpeedTest() {
        binding.btnStartTest.setEnabled(false);
        binding.btnStartTest.setText("Yoxlanılır...");
        binding.speedProgress.setVisibility(View.VISIBLE);
        binding.speedProgress.setProgress(0);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Daha dəqiq test üçün 10MB-lıq fayl
                URL url = new URL("https://speed.hetzner.de/10MB.bin");
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int fileLength = connection.getContentLength();
                long startTime = System.currentTimeMillis();
                InputStream is = connection.getInputStream();
                byte[] buffer = new byte[16384];
                int totalDownloaded = 0;
                int read;
                
                long lastUpdateTime = startTime;
                int lastDownloadedCount = 0;

                while ((read = is.read(buffer)) != -1) {
                    totalDownloaded += read;
                    long currentTime = System.currentTimeMillis();
                    
                    // Hər 500ms-dən bir ekranı yenilə (Real-vaxt effekti)
                    if (currentTime - lastUpdateTime >= 500) {
                        long timeDiff = currentTime - lastUpdateTime;
                        int downloadedDiff = totalDownloaded - lastDownloadedCount;
                        
                        // Həmin anki anlıq sürət (Mbps)
                        float currentSpeedMbps = (downloadedDiff * 8f / 1000f / 1000f) / (timeDiff / 1000f);
                        int progress = (fileLength > 0) ? (int) (totalDownloaded * 100L / fileLength) : 0;
                        
                        final float finalSpeed = currentSpeedMbps;
                        final int finalProgress = progress;
                        runOnUiThread(() -> {
                            binding.speedProgress.setProgress(finalProgress);
                            binding.tvSpeedValue.setText(String.format(java.util.Locale.US, "%.1f", finalSpeed));
                        });
                        
                        lastUpdateTime = currentTime;
                        lastDownloadedCount = totalDownloaded;
                    }
                }
                
                long endTime = System.currentTimeMillis();
                is.close();

                long totalTimeTakenMs = endTime - startTime;
                if (totalTimeTakenMs <= 0) totalTimeTakenMs = 1;
                
                // Final orta sürət
                final float avgSpeedMbps = (totalDownloaded * 8f / 1000f / 1000f) / (totalTimeTakenMs / 1000f);
                
                runOnUiThread(() -> {
                    binding.speedProgress.setProgress(100);
                    binding.tvSpeedValue.setText(String.format(java.util.Locale.US, "%.1f", avgSpeedMbps));
                    binding.btnStartTest.setEnabled(true);
                    binding.btnStartTest.setText("YENİDƏN BAŞLAT");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.btnStartTest.setEnabled(true);
                    binding.btnStartTest.setText("XƏTA! TƏKRARLA");
                });
            }
        });
    }

    private void setupFocusEffect(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
            }
        });
    }
}
