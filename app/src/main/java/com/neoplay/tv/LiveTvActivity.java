package com.neoplay.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.neoplay.tv.adapters.CategoryAdapter;
import com.neoplay.tv.adapters.ChannelAdapter;
import com.neoplay.tv.api.ApiClient;
import com.neoplay.tv.databinding.ActivityLiveTvBinding;
import com.neoplay.tv.models.Category;
import com.neoplay.tv.models.Channel;
import com.neoplay.tv.models.XtreamCategory;
import com.neoplay.tv.models.XtreamChannel;
import com.neoplay.tv.utils.DataManager;
import com.neoplay.tv.utils.FavoriteManager;
import com.neoplay.tv.utils.M3UParser;
import com.neoplay.tv.utils.NetworkUtils;
import com.neoplay.tv.utils.PinDialog;
import com.neoplay.tv.utils.XMLTVParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveTvActivity extends AppCompatActivity {

    private ActivityLiveTvBinding binding;
    private ExoPlayer miniPlayer;
    private CategoryAdapter categoryAdapter;
    private ChannelAdapter channelAdapter;
    private final List<Category> categories = new ArrayList<>();
    private final List<Channel> channels = new ArrayList<>();
    private final List<Channel> allChannels = new ArrayList<>();
    private final Map<String, List<Channel>> channelMap = new HashMap<>();
    
    private boolean isVodMode = false;
    private String playlistType;
    private String m3uUrl;
    private String xtHost, xtUser, xtPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveTvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        com.neoplay.tv.utils.WallpaperManager.INSTANCE.applyWallpaper(this, binding.ivAppBackground);

        SharedPreferences prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);
        playlistType = prefs.getString("playlist_type", "m3u");
        m3uUrl = prefs.getString("m3u_url", "");
        xtHost = prefs.getString("xtream_host", "");
        xtUser = prefs.getString("xtream_user", "");
        xtPass = prefs.getString("xtream_pass", "");

        // Xtream və ya M3U rejimini dəqiq təyin et
        boolean isXtream = "xtream".equalsIgnoreCase(playlistType) || (xtHost != null && !xtHost.trim().isEmpty() && xtUser != null && !xtUser.trim().isEmpty());

        initMiniPlayer();
        setupRecyclerViews();
        setupSearch();
        
        if (isXtream) {
            loadXtreamData();
        } else {
            loadM3UData();
            // Manual EPG-ni də yoxla
            String manualEpg = prefs.getString("manual_epg_url", "");
            if (!manualEpg.isEmpty()) {
                XMLTVParser.downloadAndParse(manualEpg);
            }
        }
    }

    private void setupSearch() {
        Handler searchHandler = new Handler(Looper.getMainLooper());
        Runnable searchRunnable = () -> filterChannelsBySearch(binding.etSearch.getText().toString());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 500); 
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterChannelsBySearch(String query) {
        channels.clear();
        for (Channel channel : allChannels) {
            if (channel.getName().toLowerCase().contains(query.toLowerCase())) {
                channels.add(channel);
            }
        }
        channelAdapter.notifyDataSetChanged();
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    private void initMiniPlayer() {
        androidx.media3.datasource.okhttp.OkHttpDataSource.Factory dataSourceFactory = NetworkUtils.getDataSourceFactory(this);
        
        androidx.media3.extractor.DefaultExtractorsFactory extractorsFactory = new androidx.media3.extractor.DefaultExtractorsFactory()
                .setTsExtractorFlags(androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES 
                                   | androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
                                   | androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory.FLAG_IGNORE_SPLICE_INFO_STREAM);

        androidx.media3.exoplayer.source.DefaultMediaSourceFactory mediaSourceFactory = new androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory);

        androidx.media3.exoplayer.trackselection.DefaultTrackSelector trackSelector = new androidx.media3.exoplayer.trackselection.DefaultTrackSelector(this);
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setExceedAudioConstraintsIfNecessary(true)
                .setExceedRendererCapabilitiesIfNecessary(true)
                .setExceedVideoConstraintsIfNecessary(true)
        );

        // Geniş audio/video kodek dəstəyi (AC3, DTS və s. üçün)
        androidx.media3.exoplayer.DefaultRenderersFactory renderersFactory = new androidx.media3.exoplayer.DefaultRenderersFactory(this)
                .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                .setEnableDecoderFallback(true);

        miniPlayer = new ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector)
                .build();
        binding.miniPlayerView.setPlayer(miniPlayer);

        // Mini-player üçün altyazı stili
        CaptionStyleCompat style = new CaptionStyleCompat(
                android.graphics.Color.WHITE,
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                android.graphics.Color.BLACK,
                null
        );
        if (binding.miniPlayerView.getSubtitleView() != null) {
            binding.miniPlayerView.getSubtitleView().setApplyEmbeddedStyles(false);
            binding.miniPlayerView.getSubtitleView().setApplyEmbeddedFontSizes(false);
            binding.miniPlayerView.getSubtitleView().setStyle(style);
            binding.miniPlayerView.getSubtitleView().setFixedTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f);
        }
    }

    private void setupRecyclerViews() {
        categoryAdapter = new CategoryAdapter(categories, category -> {
            if ("Sevimlilər".equals(category.getName())) {
                setVodMode(false);
                loadFavorites();
            } else if (M3UParser.isSensitiveCategory(category.getName())) {
                PinDialog.show(this, new PinDialog.PinListener() {
                    @Override
                    public void onSuccess() {
                        checkAndSetVodMode(category);
                        filterChannelsByCategory(category);
                    }

                    @Override
                    public void onCancel() {}
                });
            } else {
                checkAndSetVodMode(category);
                filterChannelsByCategory(category);
            }
            binding.rvChannels.requestFocus();
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCategories.setAdapter(categoryAdapter);

        FavoriteManager favoriteManager = new FavoriteManager(this);
        channelAdapter = new ChannelAdapter(channels, new ChannelAdapter.OnChannelClickListener() {
            @Override
            public void onChannelClick(Channel channel) {
                DataManager.setCurrentChannelList(channels);
                Intent intent = new Intent(LiveTvActivity.this, PlayerActivity.class);
                intent.putExtra("channel_index", channels.indexOf(channel));
                startActivity(intent);
            }

            @Override
            public void onChannelFocus(Channel channel) {
                if (!isVodMode) {
                    playMiniStream(channel);
                }
            }

            @Override
            public void onChannelLongClick(Channel channel) {
                boolean isAdded = favoriteManager.toggleFavorite(channel.getId());
                channelAdapter.notifyDataSetChanged();
                String message = isAdded ? "Sevimli siyahısına əlavə edildi" : "Sevimli siyahısından çıxarıldı";
                Toast.makeText(LiveTvActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
        binding.rvChannels.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChannels.setAdapter(channelAdapter);
    }

    private void loadFavorites() {
        FavoriteManager favoriteManager = new FavoriteManager(this);
        channels.clear();
        for (Channel channel : allChannels) {
            if (favoriteManager.isFavorite(channel.getId())) {
                channels.add(channel);
            }
        }
        channelAdapter.notifyDataSetChanged();
    }

    private void playMiniStream(Channel channel) {
        binding.tvCurrentChannel.setText(channel.getName());
        binding.tvEpgTitle.setText("Yüklənir...");
        
        com.bumptech.glide.Glide.with(this)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.default_logo)
                .error(R.drawable.default_logo)
                .into(binding.ivCurrentChannelLogo);

        String url = channel.getStreamUrl();
        MediaItem.Builder builder = new MediaItem.Builder();
        if (url != null) {
            builder.setUri(android.net.Uri.parse(url));
            String lower = url.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("m3u8") || lower.contains("stream.php") || lower.contains(".php") || lower.contains("/hls/")) {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8);
            } else if (lower.contains(".ts") || lower.contains("output=ts") || lower.contains("output=mpegts") || lower.contains("/live/") || lower.contains("/mpegts")) {
                builder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP2T);
            } else if (lower.contains(".mpd")) {
                builder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_MPD);
            }
        }
        miniPlayer.setMediaItem(builder.build());
        miniPlayer.prepare();
        miniPlayer.play();
    }

    private void loadM3UData() {
        loadM3UFromUrl(m3uUrl);
    }

    private void loadXtreamData() {
        if (xtHost.isEmpty() || xtUser.isEmpty() || xtPass.isEmpty()) {
            Toast.makeText(this, "Xtream giriş məlumatları tapılmadı", Toast.LENGTH_SHORT).show();
            return;
        }

        String filterCategory = getIntent().getStringExtra("filter_category");
        String cleanHost = xtHost != null ? xtHost.trim().replaceAll("/+$", "") : "";
        if (!cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
            cleanHost = "http://" + cleanHost;
        }
        String baseUrl = cleanHost + "/player_api.php?username=" + xtUser + "&password=" + xtPass;
        
        if ("VOD_MOVIES".equals(filterCategory)) {
            String catUrl = baseUrl + "&action=get_vod_categories";
            ApiClient.getService().getXtreamVodCategories(catUrl).enqueue(new Callback<List<XtreamCategory>>() {
                @Override
                public void onResponse(Call<List<XtreamCategory>> call, Response<List<XtreamCategory>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String streamUrl = baseUrl + "&action=get_vod_streams";
                        processXtreamCategories(response.body(), streamUrl, "movie");
                    }
                }
                @Override
                public void onFailure(Call<List<XtreamCategory>> call, Throwable t) {}
            });
        } else if ("VOD_SERIES".equals(filterCategory)) {
            String catUrl = baseUrl + "&action=get_series_categories";
            ApiClient.getService().getXtreamSeriesCategories(catUrl).enqueue(new Callback<List<XtreamCategory>>() {
                @Override
                public void onResponse(Call<List<XtreamCategory>> call, Response<List<XtreamCategory>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String streamUrl = baseUrl + "&action=get_series_streams";
                        processXtreamCategories(response.body(), streamUrl, "series");
                    }
                }
                @Override
                public void onFailure(Call<List<XtreamCategory>> call, Throwable t) {}
            });
        } else {
            String catUrl = baseUrl + "&action=get_live_categories";
            String streamUrl = baseUrl + "&action=get_live_streams";

            android.util.Log.d("XTREAM_DEBUG", "Live Cat URL: " + catUrl);
            
            // Xam cavabı yoxlamaq üçün loglama
            ApiClient.getService().getRawResponse(catUrl).enqueue(new Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String rawJson = response.body().string();
                            android.util.Log.d("XTREAM_RAW", "Raw JSON Response length: " + rawJson.length());
                            if (rawJson.length() < 500) {
                                android.util.Log.d("XTREAM_RAW", "Raw JSON Content: " + rawJson);
                            } else {
                                android.util.Log.d("XTREAM_RAW", "Raw JSON Content (start): " + rawJson.substring(0, 500));
                            }
                        } else {
                            android.util.Log.e("XTREAM_RAW", "Raw Response failed: " + response.code());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                    android.util.Log.e("XTREAM_RAW", "Raw Response failure: " + t.getMessage());
                }
            });

            ApiClient.getService().getXtreamCategories(catUrl).enqueue(new Callback<List<XtreamCategory>>() {
                @Override
                public void onResponse(Call<List<XtreamCategory>> call, Response<List<XtreamCategory>> response) {
                    android.util.Log.d("XTREAM_DEBUG", "Live Categories response success: " + response.isSuccessful() + ", size: " + (response.body() != null ? response.body().size() : "null"));
                    if (response.isSuccessful() && response.body() != null) {
                        processXtreamCategories(response.body(), streamUrl, "live");
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                android.util.Log.e("XTREAM_DEBUG", "Error body: " + response.errorBody().string());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void onFailure(Call<List<XtreamCategory>> call, Throwable t) {
                    android.util.Log.e("XTREAM_DEBUG", "Live Categories failure: " + t.getMessage(), t);
                }
            });
        }
    }

    private void processXtreamCategories(List<XtreamCategory> xtCats, String streamUrl, String type) {
        categories.clear();
        categories.add(new Category("0", "Sevimlilər", 0));
        for (XtreamCategory xc : xtCats) {
            categories.add(new Category(xc.getId(), xc.getName(), 0));
        }
        runOnUiThread(() -> categoryAdapter.notifyDataSetChanged());

        if (type.equals("live")) {
            ApiClient.getService().getXtreamChannels(streamUrl).enqueue(new Callback<List<XtreamChannel>>() {
                @Override
                public void onResponse(Call<List<XtreamChannel>> call, Response<List<XtreamChannel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        processXtreamChannels(response.body());
                    }
                }
                @Override
                public void onFailure(Call<List<XtreamChannel>> call, Throwable t) {}
            });
        } else {
            fetchXtreamVod(streamUrl, type);
        }
    }

    private void fetchXtreamVod(String url, String type) {
        ApiClient.getService().getXtreamVod(url).enqueue(new Callback<List<XtreamChannel>>() {
            @Override
            public void onResponse(Call<List<XtreamChannel>> call, Response<List<XtreamChannel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processXtreamVod(response.body(), type);
                }
            }
            @Override
            public void onFailure(Call<List<XtreamChannel>> call, Throwable t) {}
        });
    }

    private void processXtreamVod(List<XtreamChannel> vods, String type) {
        android.util.Log.d("XTREAM_DEBUG", "VOD size received: " + (vods != null ? vods.size() : "null"));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            allChannels.clear();
            channelMap.clear();
            Map<String, List<Channel>> tempMap = new HashMap<>();
            List<Channel> tempAll = new ArrayList<>();

            String cleanHost = xtHost != null ? xtHost.trim().replaceAll("/+$", "") : "";
            if (!cleanHost.isEmpty() && !cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
                cleanHost = "http://" + cleanHost;
            }

            if (vods != null && !vods.isEmpty()) {
                for (XtreamChannel xc : vods) {
                    if (xc.getStreamId() == null || xc.getStreamId().isEmpty() || xc.getName() == null) continue;
                    
                    String logo = xc.getLogo();
                    if (logo == null || logo.isEmpty()) {
                        logo = com.neoplay.tv.utils.LogoManager.INSTANCE.getLogoForChannel(xc.getName());
                    }
                    
                    String ext = type.equals("series") ? "mkv" : xc.getContainerExtension();
                    String vodTypePath = type.equals("series") ? "series" : "movie";
                    String streamLink = cleanHost + "/" + vodTypePath + "/" + xtUser + "/" + xtPass + "/" + xc.getStreamId() + "." + ext;
                    Channel channel = new Channel(xc.getStreamId(), xc.getName(), logo != null ? logo : "", streamLink, xc.getCategoryId());
                    tempAll.add(channel);
                    
                    String catId = xc.getCategoryId();
                    if (!tempMap.containsKey(catId)) tempMap.put(catId, new ArrayList<>());
                    tempMap.get(catId).add(channel);
                }
            }
            
            runOnUiThread(() -> {
                allChannels.addAll(tempAll);
                channelMap.putAll(tempMap);
                
                // Pleyer üçün məlumatları yadda saxla
                DataManager.setCurrentChannelMap(channelMap);
                
                updateCategoryCounts();
                handleStartCategory();
            });
        });
    }

    private void processXtreamChannels(List<XtreamChannel> xtChannels) {
        android.util.Log.d("XTREAM_DEBUG", "Live Channels size received: " + (xtChannels != null ? xtChannels.size() : "null"));
        
        // Asinxron olaraq siyahını dərhal emal et və UI-ı bloklama
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Channel> tempAll = new ArrayList<>();
            Map<String, List<Channel>> tempMap = new HashMap<>();
            String cleanHost = xtHost != null ? xtHost.trim().replaceAll("/+$", "") : "";
            if (!cleanHost.isEmpty() && !cleanHost.startsWith("http://") && !cleanHost.startsWith("https://")) {
                cleanHost = "http://" + cleanHost;
            }

            if (xtChannels != null && !xtChannels.isEmpty()) {
                for (XtreamChannel xc : xtChannels) {
                    if (xc.getStreamId() == null || xc.getName() == null) continue;
                    
                    String logo = xc.getLogo();
                    if (logo == null || logo.isEmpty()) {
                        logo = com.neoplay.tv.utils.LogoManager.INSTANCE.getLogoForChannel(xc.getName());
                    }
                    
                    String streamLink = cleanHost + "/live/" + xtUser + "/" + xtPass + "/" + xc.getStreamId() + ".ts";
                    Channel channel = new Channel(xc.getStreamId(), xc.getName(), logo != null ? logo : "", streamLink, xc.getCategoryId());
                    tempAll.add(channel);
                    
                    String catId = xc.getCategoryId() != null ? xc.getCategoryId() : "0";
                    if (!tempMap.containsKey(catId)) tempMap.put(catId, new ArrayList<>());
                    tempMap.get(catId).add(channel);
                }
            }
            
            runOnUiThread(() -> {
                allChannels.clear();
                allChannels.addAll(tempAll);
                channelMap.clear();
                channelMap.putAll(tempMap);
                
                // Pleyer üçün məlumatları yadda saxla
                DataManager.setCurrentChannelMap(channelMap);
                
                updateCategoryCounts();
                handleStartCategory();
            });
        });
    }

    private void updateCategoryCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, List<Channel>> entry : channelMap.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        
        FavoriteManager fm = new FavoriteManager(this);
        int favs = 0;
        for (Channel c : allChannels) if (fm.isFavorite(c.getId())) favs++;

        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            if (cat.getId().equals("0")) {
                categories.set(i, new Category("0", "Sevimlilər", favs));
            } else {
                Integer count = counts.get(cat.getId());
                categories.set(i, new Category(cat.getId(), cat.getName(), count == null ? 0 : count));
            }
        }
        
        // Pleyer üçün kateqoriyaları yenilə
        DataManager.setCurrentCategoryList(categories);
        
        categoryAdapter.notifyDataSetChanged();
    }

    private void loadM3UFromUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) return;
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "IPTVSmartersPlayer");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true);

                int status = connection.getResponseCode();
                if (status >= 300 && status <= 307) {
                    String newUrl = connection.getHeaderField("Location");
                    if (newUrl != null) {
                        connection = (HttpURLConnection) new URL(newUrl).openConnection();
                        connection.setRequestProperty("User-Agent", "IPTVSmartersPlayer");
                    }
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                
                List<Channel> parsedChannels = M3UParser.parse(sb.toString());
                
                // Loqoları yoxla və çatışmayanları qlobal bazadan götür
                for (Channel ch : parsedChannels) {
                    if (ch.getLogoUrl() == null || ch.getLogoUrl().isEmpty()) {
                        String globalLogo = com.neoplay.tv.utils.LogoManager.INSTANCE.getLogoForChannel(ch.getName());
                        if (globalLogo != null) {
                            ch.setLogoUrl(globalLogo);
                        }
                    }
                }
                
                // EPG yükləməsini başlat
                String epgUrl = DataManager.getGlobalEpgUrl();
                if (!epgUrl.isEmpty()) {
                    XMLTVParser.downloadAndParse(epgUrl);
                }
                
                runOnUiThread(() -> {
                    allChannels.clear();
                    allChannels.addAll(parsedChannels);
                    processLoadedChannels();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void processLoadedChannels() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            Map<String, List<Channel>> tempMap = new HashMap<>();
            Set<String> seenCats = new LinkedHashSet<>();
            FavoriteManager favoriteManager = new FavoriteManager(this);
            int favCount = 0;

            for (Channel channel : allChannels) {
                if (favoriteManager.isFavorite(channel.getId())) favCount++;
                String catName = channel.getCategoryName();
                if (!tempMap.containsKey(catName)) tempMap.put(catName, new ArrayList<>());
                tempMap.get(catName).add(channel);
                seenCats.add(catName);
            }

            final int finalFavCount = favCount;
            runOnUiThread(() -> {
                channelMap.clear();
                channelMap.putAll(tempMap);
                categories.clear();
                categories.add(new Category("0", "Sevimlilər", finalFavCount));
                int id = 1;
                for (String cname : seenCats) {
                    List<Channel> list = channelMap.get(cname);
                    categories.add(new Category(String.valueOf(id++), cname, list != null ? list.size() : 0));
                }
                
                // Pleyer üçün məlumatları yadda saxla
                DataManager.setCurrentCategoryList(categories);
                DataManager.setCurrentChannelMap(channelMap);
                
                categoryAdapter.notifyDataSetChanged();
                handleStartCategory();
            });
        });
    }

    private void handleStartCategory() {
        boolean autoStart = getIntent().getBooleanExtra("auto_start", false);
        if (autoStart) {
            getIntent().removeExtra("auto_start"); // Təkrar işə düşməməsi üçün
            SharedPreferences prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);
            String lastUrl = prefs.getString("last_channel_url", "");
            if (!lastUrl.isEmpty() && !allChannels.isEmpty()) {
                for (int i = 0; i < allChannels.size(); i++) {
                    if (allChannels.get(i).getStreamUrl().equals(lastUrl)) {
                        DataManager.setCurrentChannelList(allChannels);
                        Intent intent = new Intent(this, PlayerActivity.class);
                        intent.putExtra("channel_index", i);
                        startActivity(intent);
                        return;
                    }
                }
            }
        }

        String filterCategory = getIntent().getStringExtra("filter_category");
        if (filterCategory != null) {
            if ("Sevimlilər".equals(filterCategory)) {
                loadFavorites();
            } else if ("VOD_MOVIES".equals(filterCategory) || "VOD_SERIES".equals(filterCategory)) {
                setVodMode(true);
                loadVodContent();
            } else {
                for (Category cat : categories) {
                    if (cat.getName().equalsIgnoreCase(filterCategory)) {
                        filterChannelsByCategory(cat);
                        return;
                    }
                }
                setVodMode(true);
                loadVodContent();
            }
        } else if (!categories.isEmpty()) {
            setVodMode(false);
            filterChannelsByCategory(categories.get(0));
        }
    }

    private void checkAndSetVodMode(Category category) {
        String key = "xtream".equalsIgnoreCase(playlistType) ? category.getId() : category.getName();
        List<Channel> list = channelMap.get(key);
        if (list != null && !list.isEmpty()) {
            setVodMode(M3UParser.isVodChannel(list.get(0).getStreamUrl()));
        } else {
            setVodMode(false);
        }
    }

    private void setVodMode(boolean enabled) {
        this.isVodMode = enabled;
        if (enabled) {
            binding.rvChannels.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 5));
            channelAdapter.setViewType(com.neoplay.tv.adapters.ChannelAdapter.VIEW_TYPE_GRID);
            binding.panelPlayer.setVisibility(android.view.View.GONE);
            binding.tvPanelTitle.setText("FILMLƏR / SERIALYAR");
            if (miniPlayer != null && miniPlayer.isPlaying()) {
                miniPlayer.stop();
            }
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) binding.panelChannels.getLayoutParams();
            params.weight = 6.2f;
            binding.panelChannels.setLayoutParams(params);
        } else {
            binding.rvChannels.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            channelAdapter.setViewType(com.neoplay.tv.adapters.ChannelAdapter.VIEW_TYPE_LIST);
            binding.panelPlayer.setVisibility(android.view.View.VISIBLE);
            binding.tvPanelTitle.setText("KANALLAR");
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) binding.panelChannels.getLayoutParams();
            params.weight = 2.8f;
            binding.panelChannels.setLayoutParams(params);
        }
    }

    private void loadVodContent() {
        channels.clear();
        for (Channel channel : allChannels) {
            if (M3UParser.isVodChannel(channel.getStreamUrl())) {
                channels.add(channel);
            }
        }
        channelAdapter.notifyDataSetChanged();
    }

    private void filterChannelsByCategory(Category category) {
        channels.clear();
        String key = "xtream".equalsIgnoreCase(playlistType) ? category.getId() : category.getName();
        List<Channel> list = channelMap.get(key);
        if (list != null) {
            channels.addAll(list);
        }
        channelAdapter.notifyDataSetChanged();
        // Əgər kanallar varsa, birinci kanala fokus ver və ya siyahını yenilə
        if (!channels.isEmpty()) {
            binding.rvChannels.scrollToPosition(0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (binding.rvChannels.hasFocus() || binding.etSearch.hasFocus()) {
                binding.rvCategories.requestFocus();
                return true;
            }
        }
        
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            event.startTracking(); // Uzun basmanı izləmək üçün
            if (binding.rvChannels.hasFocus()) {
                // Siyahıdan kateqoriyalara qayıtmaq üçün
                binding.rvCategories.requestFocus();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // Uzun basıldıqda kateqoriyalara qayıt
            binding.rvCategories.requestFocus();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (miniPlayer != null) miniPlayer.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayer != null) miniPlayer.release();
    }
}
