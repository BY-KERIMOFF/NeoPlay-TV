package com.neoplay.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.security.cert.X509Certificate;

import com.bumptech.glide.Glide;
import com.neoplay.tv.adapters.CategoryAdapter;
import com.neoplay.tv.adapters.ChannelAdapter;
import com.neoplay.tv.api.ApiClient;
import com.neoplay.tv.databinding.ActivityPlayerBinding;
import com.neoplay.tv.models.Category;
import com.neoplay.tv.models.Channel;
import com.neoplay.tv.models.XtreamEpg;
import com.neoplay.tv.utils.DataManager;
import com.neoplay.tv.utils.FavoriteManager;
import com.neoplay.tv.utils.NetworkUtils;
import com.neoplay.tv.utils.SleepTimerManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;
    private final Handler osdHandler = new Handler(Looper.getMainLooper());
    
    private ExoPlayer exoPlayer;
    private AudioManager audioManager;
    private int currentIndex = 0;
    private List<Channel> channelList;
    private String playerType = "exo2";
    
    private String channelNumberInput = "";
    private final Handler channelSwitchHandler = new Handler(Looper.getMainLooper());
    private final Runnable channelSwitchRunnable = new Runnable() {
        @Override
        public void run() {
            processNumericInput();
        }
    };
    
    private int retryCount = 0;
    private final int MAX_RETRIES = 3;
    
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                long duration = exoPlayer.getDuration();
                long current = exoPlayer.getCurrentPosition();
                if (duration > 0) {
                    binding.vodProgressLayout.setVisibility(View.VISIBLE);
                    binding.vodSeekBar.setMax((int) duration);
                    binding.vodSeekBar.setProgress((int) current);
                    binding.tvCurrentPosition.setText(formatTime(current));
                    binding.tvTotalDuration.setText(formatTime(duration));
                } else {
                    binding.vodProgressLayout.setVisibility(View.GONE);
                }
            }
            progressHandler.postDelayed(this, 1000);
        }
    };
    
    private static final Map<String, String> epgCache = new HashMap<>();

    private CategoryAdapter playerCategoryAdapter;
    private ChannelAdapter channelAdapter;
    private List<Channel> allCategoryChannels = new ArrayList<>();
    private String currentPlaylistType = "m3u";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        channelList = DataManager.getCurrentChannelList();
        currentIndex = getIntent().getIntExtra("channel_index", 0);

        SharedPreferences prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);
        playerType = prefs.getString("player_type", "exo2");
        currentPlaylistType = prefs.getString("playlist_type", "m3u");

        initExoPlayer(playerType);
        setupPlayerChannelList();
        setupPlayerCategoryList();
        setupPlayerSearch();
        
        progressHandler.post(updateProgressRunnable);

        binding.vodSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && exoPlayer != null) {
                    exoPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.rvPlayerCategories.getVisibility() == View.VISIBLE) {
                    binding.rvPlayerCategories.setVisibility(View.GONE);
                } else if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                    binding.playerChannelSidebar.setVisibility(View.GONE);
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });

        if (channelList != null && !channelList.isEmpty()) {
            loadChannel(channelList.get(currentIndex));
        }
    }

    private void setupPlayerChannelList() {
        if (channelList == null) return;
        channelAdapter = new ChannelAdapter(channelList, new ChannelAdapter.OnChannelClickListener() {
            @Override
            public void onChannelClick(Channel channel) {
                currentIndex = channelList.indexOf(channel);
                loadChannel(channel);
                binding.playerChannelSidebar.setVisibility(View.GONE);
                binding.rvPlayerCategories.setVisibility(View.GONE);
            }

            @Override
            public void onChannelFocus(Channel channel) {}

            @Override
            public void onChannelLongClick(Channel channel) {
                FavoriteManager fm = new FavoriteManager(PlayerActivity.this);
                fm.toggleFavorite(channel.getId());
                channelAdapter.notifyDataSetChanged();
            }
        });
        binding.rvPlayerChannels.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        binding.rvPlayerChannels.setAdapter(channelAdapter);
    }

    private void setupPlayerCategoryList() {
        List<Category> categories = DataManager.getCurrentCategoryList();
        if (categories == null || categories.isEmpty()) return;

        playerCategoryAdapter = new CategoryAdapter(categories, category -> {
            updateChannelsByCategory(category);
            binding.etPlayerSearch.requestFocus();
        });

        playerCategoryAdapter.setOnCategoryFocusListener(this::updateChannelsByCategory);

        binding.rvPlayerCategories.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        binding.rvPlayerCategories.setAdapter(playerCategoryAdapter);
    }

    private void updateChannelsByCategory(Category category) {
        String key = "xtream".equalsIgnoreCase(currentPlaylistType) ? category.getId() : category.getName();
        List<Channel> categoryChannels;
        
        if ("0".equals(category.getId()) || "Sevimlilər".equals(category.getName())) {
            categoryChannels = new ArrayList<>();
            FavoriteManager fm = new FavoriteManager(this);
            for (Channel c : DataManager.getCurrentChannelList()) {
                if (fm.isFavorite(c.getId())) categoryChannels.add(c);
            }
        } else {
            categoryChannels = DataManager.getCurrentChannelMap().get(key);
        }

        if (categoryChannels != null) {
            allCategoryChannels = new ArrayList<>(categoryChannels);
            channelList = new ArrayList<>(allCategoryChannels);
            binding.etPlayerSearch.setText(""); // Reset search
            setupPlayerChannelList();
            binding.playerChannelSidebar.setVisibility(View.VISIBLE);
        }
    }

    private void setupPlayerSearch() {
        binding.etPlayerSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterChannels(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterChannels(String query) {
        if (allCategoryChannels == null) return;
        
        List<Channel> filtered = new ArrayList<>();
        for (Channel c : allCategoryChannels) {
            if (c.getName().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(c);
            }
        }
        channelList = filtered;
        setupPlayerChannelList();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initExoPlayer(String mode) {
        if (exoPlayer == null) {
            OkHttpDataSource.Factory dataSourceFactory = NetworkUtils.getDataSourceFactory(this);
            
            // IPTV axınları üçün daha dözümlü Extractor sazlamaları
            androidx.media3.extractor.DefaultExtractorsFactory extractorsFactory = new androidx.media3.extractor.DefaultExtractorsFactory()
                    .setTsExtractorFlags(androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES 
                                       | androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
                    .setAdtsExtractorFlags(androidx.media3.extractor.ts.AdtsExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING);

            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory);
            
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(trackSelector.buildUponParameters()
                    .setPreferredAudioLanguage("az")
                    .setForceHighestSupportedBitrate(false) // Stabil səs üçün
            );

            // Daha mükəmməl buferləmə ayarları
            androidx.media3.exoplayer.DefaultLoadControl loadControl = new androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                            15000, // minBufferMs (donmamaq üçün bufer)
                            50000, // maxBufferMs
                            1500,  // bufferForPlaybackMs
                            2500   // bufferForPlaybackAfterRebufferMs
                    )
                    .build();

            // Bütün audio kodekləri (AC3, AAC və s.) üçün render üstünlüyü və fallback
            androidx.media3.exoplayer.DefaultRenderersFactory renderersFactory = new androidx.media3.exoplayer.DefaultRenderersFactory(this)
                    .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                    .setEnableDecoderFallback(true);

            exoPlayer = new ExoPlayer.Builder(this, renderersFactory)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl)
                    .build();

            binding.playerView.setPlayer(exoPlayer);
            
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        binding.bufferingProgress.setVisibility(View.VISIBLE);
                    } else {
                        binding.bufferingProgress.setVisibility(View.GONE);
                        if (playbackState == Player.STATE_READY) {
                            binding.errorLayout.setVisibility(View.GONE);
                            binding.errorLayout.clearAnimation();
                            retryCount = 0; // Yalnız uğurlu qoşulmada sıfırla
                        }
                    }
                }

                @Override
                public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                    binding.bufferingProgress.setVisibility(View.GONE);
                    binding.errorLayout.setVisibility(View.GONE); // Retry zamanı xətanı gizlət
                    
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        binding.tvEpgInfo.setText("Xəta, yenidən yoxlanılır (" + retryCount + "/" + MAX_RETRIES + ")...");
                        binding.osdLayout.setVisibility(View.VISIBLE);
                        
                        osdHandler.postDelayed(() -> {
                            if (exoPlayer != null && channelList != null && !channelList.isEmpty()) {
                                exoPlayer.prepare();
                                exoPlayer.play();
                            }
                        }, 2500);
                    } else {
                        // Tam ekran xəta mesajını göstər
                        binding.errorLayout.setVisibility(View.VISIBLE);
                        binding.errorLayout.startAnimation(android.view.animation.AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.pulse));
                        
                        binding.osdLayout.setVisibility(View.GONE); // Digər panelləri gizlə
                        binding.volumeLayout.setVisibility(View.GONE);
                        binding.tvEpgInfo.setText("Müvəqqəti texniki nasazlıq");
                    }
                }
            });
        }
    }

    private void loadChannel(Channel channel) {
        if (exoPlayer == null) initExoPlayer(playerType);
        
        retryCount = 0; // Retry sayını sıfırla
        binding.errorLayout.setVisibility(View.GONE);
        binding.errorLayout.clearAnimation();
        
        exoPlayer.stop();
        exoPlayer.clearMediaItems();

        String url = channel.getStreamUrl();
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder();
        if (url != null) {
            mediaItemBuilder.setUri(Uri.parse(url));
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains("m3u8") || lower.contains("stream.php") || lower.contains(".php")) {
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
            } else if (lower.contains(".ts") || lower.contains("output=ts") || lower.contains("output=mpegts") || lower.contains("/live/")) {
                // MPEG-TS formatı bir çox canlı yayımda istifadə olunur və AC3 səs bu formatdadır
                mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP2T);
            }
        }

        MediaItem mediaItem = mediaItemBuilder.build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();

        binding.tvChannelName.setAlpha(0f);
        binding.tvChannelName.setText(channel.getName());
        binding.tvChannelName.animate().alpha(1f).setDuration(400).start();

        if (binding.ivChannelLogo != null) {
            binding.ivChannelLogo.setAlpha(0f);
            Glide.with(this)
                    .load(channel.getLogoUrl())
                    .placeholder(R.drawable.default_logo)
                    .error(R.drawable.default_logo)
                    .into(binding.ivChannelLogo);
            binding.ivChannelLogo.animate().alpha(1f).setDuration(400).start();
        }

        String quality = "HD";
        String nameUpper = channel.getName().toUpperCase();
        if (nameUpper.contains("FHD") || nameUpper.contains("1080")) {
            quality = "FHD";
        } else if (nameUpper.contains("4K") || nameUpper.contains("UHD")) {
            quality = "4K";
        } else if (nameUpper.contains("SD") || nameUpper.contains("576")) {
            quality = "SD";
        } else if (nameUpper.contains("HD") || nameUpper.contains("720")) {
            quality = "HD";
        }
        binding.tvQuality.setText(quality);

        fetchEpg(channel.getId());
        showOsd();
    }

    private void fetchEpg(String channelId) {
        if (epgCache.containsKey(channelId)) {
            binding.tvEpgInfo.setText(epgCache.get(channelId));
            return;
        }

        // Birinci Xtream EPG-ni yoxla
        SharedPreferences prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);
        String host = prefs.getString("xtream_host", "");
        String user = prefs.getString("xtream_user", "");
        String pass = prefs.getString("xtream_pass", "");
        
        if (!host.isEmpty() && !user.isEmpty() && !pass.isEmpty()) {
            String url = host + "/player_api.php?username=" + user + "&password=" + pass + "&action=get_short_epg&id=" + channelId;
            ApiClient.getService().getXtreamEpg(url).enqueue(new Callback<XtreamEpg>() {
                @Override
                public void onResponse(Call<XtreamEpg> call, Response<XtreamEpg> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getListings() != null && !response.body().getListings().isEmpty()) {
                        String title = response.body().getListings().get(0).title;
                        epgCache.put(channelId, title);
                        binding.tvEpgInfo.setText(title);
                    } else {
                        checkXmltvEpg(channelId);
                    }
                }

                @Override
                public void onFailure(Call<XtreamEpg> call, Throwable t) {
                    checkXmltvEpg(channelId);
                }
            });
        } else {
            checkXmltvEpg(channelId);
        }
    }

    private void checkXmltvEpg(String channelId) {
        Map<String, String> xmltv = DataManager.getXmltvCache();
        if (xmltv != null && !xmltv.isEmpty()) {
            // Channel obyektini tap ki tvgId-ni götürək
            Channel currentChannel = null;
            if (channelList != null && currentIndex >= 0 && currentIndex < channelList.size()) {
                currentChannel = channelList.get(currentIndex);
            }

            if (currentChannel != null) {
                String title = xmltv.get(currentChannel.getTvgId());
                if (title == null || title.isEmpty()) title = xmltv.get(currentChannel.getName());
                
                // Ağıllı ad uyğunlaşdırması (Normalized)
                if (title == null || title.isEmpty()) {
                    String normalized = com.neoplay.tv.utils.XMLTVParser.normalizeName(currentChannel.getName());
                    title = xmltv.get(normalized);
                }

                if (title != null && !title.isEmpty()) {
                    epgCache.put(channelId, title);
                    binding.tvEpgInfo.setText(title);
                    return;
                }
            }
        }
        binding.tvEpgInfo.setText("EPG məlumatı yoxdur");
    }

    private String formatTime(long ms) {
        long seconds = (ms / 1000) % 60;
        long minutes = (ms / (1000 * 60)) % 60;
        long hours = (ms / (1000 * 60 * 60));
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Rəqəm düymələrini tut (0-9)
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            appendNumericInput(keyCode - KeyEvent.KEYCODE_0);
            return true;
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                    if (binding.etPlayerSearch.hasFocus()) {
                        // Axtarış yerindədirsə, klaviaturanı açmaq üçün default davranışı saxla
                        return super.onKeyDown(keyCode, event);
                    }
                    binding.playerChannelSidebar.setVisibility(View.GONE);
                } else {
                    binding.playerChannelSidebar.setVisibility(View.VISIBLE);
                    binding.rvPlayerChannels.scrollToPosition(currentIndex);
                    binding.rvPlayerChannels.requestFocus();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // Əgər kanal siyahısı açıqdırsa, kateqoriyalara keç
                if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                    binding.playerChannelSidebar.setVisibility(View.GONE);
                    binding.rvPlayerCategories.setVisibility(View.VISIBLE);
                    binding.rvPlayerCategories.requestFocus();
                    return true;
                }
                
                // Əgər heç bir menyu açıq deyilsə, Sol düyməsi Kateqoriyaları açsın
                if (binding.rvPlayerCategories.getVisibility() != View.VISIBLE) {
                    binding.rvPlayerCategories.setVisibility(View.VISIBLE);
                    binding.rvPlayerCategories.requestFocus();
                    return true;
                }
                return super.onKeyDown(keyCode, event);

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // Əgər kateqoriya siyahısı açıqdırsa, kanal siyahısına keç
                if (binding.rvPlayerCategories.getVisibility() == View.VISIBLE) {
                    if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                        binding.rvPlayerChannels.requestFocus();
                    }
                    return true;
                }
                
                // VOD üçün irəli çəkmə
                if (exoPlayer != null && exoPlayer.getDuration() > 0 && !exoPlayer.isCurrentMediaItemDynamic()) {
                    long newPos = Math.min(exoPlayer.getDuration(), exoPlayer.getCurrentPosition() + 15000);
                    exoPlayer.seekTo(newPos);
                    showOsd();
                    return true;
                }
                return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_DPAD_UP:
                if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                    if (binding.rvPlayerChannels.hasFocus() && 
                        ((androidx.recyclerview.widget.LinearLayoutManager)binding.rvPlayerChannels.getLayoutManager()).findFirstCompletelyVisibleItemPosition() == 0) {
                        binding.etPlayerSearch.requestFocus();
                        return true;
                    }
                    return super.onKeyDown(keyCode, event);
                }
                playNextChannel();
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (binding.playerChannelSidebar.getVisibility() == View.VISIBLE) {
                    if (binding.etPlayerSearch.hasFocus()) {
                        binding.rvPlayerChannels.requestFocus();
                        return true;
                    }
                    return super.onKeyDown(keyCode, event);
                }
                playPreviousChannel();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                // Səsin dəyişməsini gözləmək üçün kiçik gecikmə ilə UI-ı yenilə
                osdHandler.postDelayed(this::updateVolumeUI, 50);
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    private void playNextChannel() {
        if (channelList != null && !channelList.isEmpty()) {
            currentIndex++;
            if (currentIndex >= channelList.size()) {
                currentIndex = 0;
            }
            loadChannel(channelList.get(currentIndex));
        }
    }

    private void playPreviousChannel() {
        if (channelList != null && !channelList.isEmpty()) {
            currentIndex--;
            if (currentIndex < 0) {
                currentIndex = channelList.size() - 1;
            }
            loadChannel(channelList.get(currentIndex));
        }
    }

    private void updateVolumeUI() {
        if (audioManager == null) return;
        
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (maxVolume == 0) maxVolume = 1;
        int percent = (currentVolume * 100) / maxVolume;

        binding.volumeLayout.setVisibility(View.VISIBLE);
        binding.volumeProgress.setProgress(percent);
        binding.tvVolumePercent.setText(percent + "%");
        
        // Əvvəlki taymeri təmizlə və yenisini qoy
        osdHandler.removeCallbacks(volumeHideRunnable);
        osdHandler.postDelayed(volumeHideRunnable, 3000);
    }

    private final Runnable volumeHideRunnable = () -> binding.volumeLayout.setVisibility(View.GONE);

    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        osdHandler.removeCallbacksAndMessages(null);
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }

    private void showOsd() {
        binding.osdLayout.setVisibility(View.VISIBLE);
        
        SleepTimerManager timerManager = SleepTimerManager.getInstance();
        if (timerManager.isRunning()) {
            String remaining = timerManager.getFormattedRemainingTime();
            binding.tvEpgInfo.setText(binding.tvEpgInfo.getText() + " | ⏳ " + remaining);
        }

        osdHandler.removeCallbacksAndMessages(null);
        osdHandler.postDelayed(() -> binding.osdLayout.setVisibility(View.GONE), 5000);
    }

    private void appendNumericInput(int digit) {
        channelNumberInput += digit;
        binding.tvNumericInput.setText(channelNumberInput);
        binding.tvNumericInput.setVisibility(View.VISIBLE);
        
        channelSwitchHandler.removeCallbacks(channelSwitchRunnable);
        channelSwitchHandler.postDelayed(channelSwitchRunnable, 2500); // 2.5 saniyə gözlə
    }

    private void processNumericInput() {
        try {
            int targetIndex = Integer.parseInt(channelNumberInput) - 1; // 1-based to 0-based
            if (channelList != null && targetIndex >= 0 && targetIndex < channelList.size()) {
                currentIndex = targetIndex;
                loadChannel(channelList.get(currentIndex));
            } else {
                binding.tvEpgInfo.setText("Səhv nömrə: " + channelNumberInput);
                showOsd();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        channelNumberInput = "";
        binding.tvNumericInput.setVisibility(View.GONE);
    }
}
