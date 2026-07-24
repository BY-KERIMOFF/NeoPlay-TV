package com.neoplay.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neoplay.tv.api.ApiClient;
import com.neoplay.tv.api.ApiResponse;
import com.neoplay.tv.databinding.ActivityMainBinding;
import com.neoplay.tv.utils.M3UParser;
import com.neoplay.tv.utils.MacUtils;
import com.neoplay.tv.utils.UpdateManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private int retryCount = 0;
    private final int MAX_RETRIES = 3;
    private String deviceMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deviceMac = MacUtils.getMacAddress(this);
        
        // Yeniləməni yoxla
        new UpdateManager(this).checkForUpdates();
        
        // EPG Sinxronizasiyasını başlat
        com.neoplay.tv.utils.XMLTVParser.syncDefaultSources();

        startSplashAnimation();
        setupListeners();
        startAuthProcess();
    }

    private void startSplashAnimation() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        final Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        
        binding.ivSplashLogo.startAnimation(fadeIn);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                binding.ivSplashLogo.startAnimation(pulse);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
    }

    private void setupListeners() {
        setupFocusEffect(binding.cardLiveTv);
        setupFocusEffect(binding.cardMovies);
        setupFocusEffect(binding.cardSeries);
        setupFocusEffect(binding.cardFavorites);
        setupFocusEffect(binding.btnSettings);
        setupFocusEffect(binding.btnSearch);
        setupFocusEffect(binding.btnSpeedTestMain);

        binding.btnRetry.setOnClickListener(v -> {
            retryCount = 0;
            startAuthProcess();
        });

        binding.cardLiveTv.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveTvActivity.class));
        });

        binding.cardMovies.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LiveTvActivity.class);
            intent.putExtra("filter_category", "VOD_MOVIES");
            startActivity(intent);
        });

        binding.cardSeries.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LiveTvActivity.class);
            intent.putExtra("filter_category", "VOD_SERIES");
            startActivity(intent);
        });

        binding.cardFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LiveTvActivity.class);
            intent.putExtra("filter_category", "Sevimlilər");
            startActivity(intent);
        });

        binding.btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
        
        binding.btnSpeedTestMain.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SpeedTestActivity.class));
        });
        
        // Search düyməsi üçün Live TV-yə yönləndirmə (gələcəkdə axtarış pəncərəsi əlavə edilə bilər)
        binding.btnSearch.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveTvActivity.class));
        });
    }

    private void setupFocusEffect(View view) {
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up));
                v.setElevation(20f);
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down));
                v.setElevation(0f);
            }
        });
    }

    private void startAuthProcess() {
        showLoading();
        checkAuthentication();
    }

    private void checkAuthentication() {
        String url = "api.php?mac=" + deviceMac;
        ApiClient.getService().checkMac(url).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse result = response.body();
                    handleAuthResponse(result);
                } else {
                    handleFailure("Server xətası baş verdi.");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> checkAuthentication(), 5000);
                } else {
                    handleFailure("İnternet bağlantısı yoxdur və ya serverə qoşulmaq mümkün olmadı.");
                }
            }
        });
    }

    private void handleAuthResponse(ApiResponse response) {
        if ("success".equalsIgnoreCase(response.getStatus())) {
            String expiry = response.getExpiryDate();
            
            // Bütün məlumatları yadda saxla
            getSharedPreferences("neoplay_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("expiry_date", expiry)
                    .putString("playlist_type", response.getPlaylistType())
                    .putString("m3u_url", response.getM3uUrl())
                    .putString("xtream_host", response.getXtream() != null ? response.getXtream().getHost() : "")
                    .putString("xtream_user", response.getXtream() != null ? response.getXtream().getUsername() : "")
                    .putString("xtream_pass", response.getXtream() != null ? response.getXtream().getPassword() : "")
                    .putBoolean("is_vod_enabled", response.isVodEnabled())
                    .putBoolean("is_series_enabled", response.isSeriesEnabled())
                    .apply();

            if (expiry != null && !expiry.trim().isEmpty() && !expiry.equalsIgnoreCase("null")) {
                binding.tvExpiryInfo.setText("Abunəlik bitir: " + expiry);
                binding.tvExpiryInfo.setVisibility(View.VISIBLE);
                
                getSharedPreferences("neoplay_prefs", MODE_PRIVATE)
                        .edit()
                        .putString("expiry_date", expiry)
                        .apply();
            } else {
                binding.tvExpiryInfo.setVisibility(View.GONE);
                // Xəbərdarlıq silindi, server düzəldikdə bura özü dolacaq.
            }
            
            // Analiz bitməsini gözləmədən dərhal dashboard-u göstər
            showDashboard(response.isVodEnabled(), response.isSeriesEnabled()); 
            loadAndCheckPlaylist(); // Arxa planda yoxla
        } else if ("expired".equalsIgnoreCase(response.getStatus())) {
            showError("Abunəlik Müddəti Bitib", "Abunəliyiniz bitmişdir. Zəhmət olmasa dilerinizlə əlaqə saxlayın.");
        } else if ("not_found".equalsIgnoreCase(response.getStatus())) {
            showError("Cihaz Aktiv Edilməyib", "Cihazınız sistemdə qeydiyyatdan keçməyib. MAC adresini dilerə göndərin.");
        } else {
            showError("Xəta", response.getMessage() != null ? response.getMessage() : "Naməlum xəta baş verdi.");
        }
    }

    private void showLoading() {
        binding.loadingLayout.setVisibility(View.VISIBLE);
        binding.dashboardLayout.setVisibility(View.GONE);
        binding.errorOverlay.setVisibility(View.GONE);
    }

    private void loadAndCheckPlaylist() {
        SharedPreferences prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);
        String type = prefs.getString("playlist_type", "m3u");
        String m3uUrl = prefs.getString("m3u_url", "https://kanal65.xyz/neoplay/playlist.m3u");
        boolean isVodEnabled = prefs.getBoolean("is_vod_enabled", true);
        boolean isSeriesEnabled = prefs.getBoolean("is_series_enabled", true);

        if ("xtream".equalsIgnoreCase(type)) {
            runOnUiThread(() -> {
                binding.cardMovies.setVisibility(isVodEnabled ? View.VISIBLE : View.GONE);
                binding.cardSeries.setVisibility(isSeriesEnabled ? View.VISIBLE : View.GONE);
                
                float weightSum = 2.0f;
                if (isVodEnabled) weightSum += 1.0f;
                if (isSeriesEnabled) weightSum += 1.0f;
                binding.cardsContainer.setWeightSum(weightSum);
            });
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            boolean hasVod = false;
            try {
                URL url = new URL(m3uUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count < 2000) { // İlk 2000 sətir kifayətdir
                    line = line.trim();
                    if (!line.startsWith("#") && !line.isEmpty()) {
                        if (M3UParser.isVodChannel(line)) {
                            hasVod = true;
                            break;
                        }
                        count++;
                    }
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            final boolean finalHasVod = hasVod;
                runOnUiThread(() -> {
                    boolean finalShowMovies = finalHasVod && isVodEnabled;
                    boolean finalShowSeries = finalHasVod && isSeriesEnabled;

                    binding.cardMovies.setVisibility(finalShowMovies ? View.VISIBLE : View.GONE);
                    binding.cardSeries.setVisibility(finalShowSeries ? View.VISIBLE : View.GONE);
                    
                    float weightSum = 2.0f;
                    if (finalShowMovies) weightSum += 1.0f;
                    if (finalShowSeries) weightSum += 1.0f;
                    binding.cardsContainer.setWeightSum(weightSum);
                });
        });
    }

    private void showDashboard(boolean isVodEnabled, boolean isSeriesEnabled) {
        // Azərbaycan bayrağının rəsmi rəng çalarları (Pantone)
        String colorBlue = "#0097D7";
        String colorRed = "#D2122E";
        String colorGreen = "#00AE42";

        android.text.SpannableString spannable = new android.text.SpannableString("NEO PLAY");
        
        // N, E - Göy (0-2)
        spannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(colorBlue)), 0, 2, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // O, [Boşluq], P - Qırmızı (2-5)
        spannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(colorRed)), 2, 5, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // L, A, Y - Yaşıl (5-8)
        spannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(colorGreen)), 5, 8, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        binding.tvAppTitle.setText(spannable);

        // Tarixi təyin et
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM", new Locale("az"));
        binding.tvDate.setText(sdf.format(new Date()));
        
        // Server statusuna görə göstər
        binding.cardMovies.setVisibility(isVodEnabled ? View.VISIBLE : View.GONE);
        binding.cardSeries.setVisibility(isSeriesEnabled ? View.VISIBLE : View.GONE);
        
        float weightSum = 2.0f;
        if (isVodEnabled) weightSum += 1.0f;
        if (isSeriesEnabled) weightSum += 1.0f;
        binding.cardsContainer.setWeightSum(weightSum);

        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeOut.setDuration(500);
        
        binding.loadingLayout.startAnimation(fadeOut);
        binding.loadingLayout.setVisibility(View.GONE);
        binding.dashboardLayout.setVisibility(View.VISIBLE);
        binding.errorOverlay.setVisibility(View.GONE);
        
        // Dashboard animasiyası
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        binding.dashboardLayout.startAnimation(slideUp);
    }

    private void showError(String title, String message) {
        binding.loadingLayout.setVisibility(View.GONE);
        binding.dashboardLayout.setVisibility(View.GONE);
        binding.errorOverlay.setVisibility(View.VISIBLE);

        binding.errorTitle.setText(title);
        binding.errorMessage.setText(message);
        binding.macDisplay.setText("MAC: " + deviceMac);
    }

    private void handleFailure(String message) {
        showError("Bağlantı Xətası", message);
    }
}
