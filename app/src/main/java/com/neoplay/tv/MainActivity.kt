package com.neoplay.tv

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.neoplay.tv.api.ApiClient
import com.neoplay.tv.api.ApiResponse
import com.neoplay.tv.databinding.ActivityMainBinding
import com.neoplay.tv.utils.M3UParser
import com.neoplay.tv.utils.MacUtils
import com.neoplay.tv.utils.UpdateManager
import com.neoplay.tv.utils.XMLTVParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var retryCount = 0
    private val MAX_RETRIES = 3
    private var deviceMac: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("neoplay_prefs", Context.MODE_PRIVATE)
        val lockEnabled = prefs.getBoolean("app_lock_enabled", false)
        val isAlreadyUnlocked = intent.getBooleanExtra("is_unlocked", false)

        if (lockEnabled && !isAlreadyUnlocked) {
            startActivity(Intent(this, LockActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceMac = MacUtils.getMacAddress(this)

        // Yeniləməni yoxla
        UpdateManager(this).checkForUpdates()

        // EPG Sinxronizasiyasını başlat
        XMLTVParser.syncDefaultSources()

        startSplashAnimation()
        setupListeners()
        startAuthProcess()
    }

    private fun startSplashAnimation() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        val textFadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        binding.ivSplashLogo.startAnimation(fadeIn)
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                binding.ivSplashLogo.startAnimation(pulse)
                binding.tvEnjoyWatching.visibility = View.VISIBLE
                binding.tvEnjoyWatching.startAnimation(textFadeIn)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
    }

    private fun setupListeners() {
        setupFocusEffect(binding.cardLiveTv)
        setupFocusEffect(binding.cardMovies)
        setupFocusEffect(binding.cardSeries)
        setupFocusEffect(binding.cardFavorites)
        setupFocusEffect(binding.btnSettings)
        setupFocusEffect(binding.btnSearch)
        setupFocusEffect(binding.btnSpeedTestMain)

        binding.btnRetry.setOnClickListener {
            retryCount = 0
            startAuthProcess()
        }

        binding.cardLiveTv.setOnClickListener {
            startActivity(Intent(this@MainActivity, LiveTvActivity::class.java))
        }

        binding.cardMovies.setOnClickListener {
            val intent = Intent(this@MainActivity, LiveTvActivity::class.java)
            intent.putExtra("filter_category", "VOD_MOVIES")
            startActivity(intent)
        }

        binding.cardSeries.setOnClickListener {
            val intent = Intent(this@MainActivity, LiveTvActivity::class.java)
            intent.putExtra("filter_category", "VOD_SERIES")
            startActivity(intent)
        }

        binding.cardFavorites.setOnClickListener {
            val intent = Intent(this@MainActivity, LiveTvActivity::class.java)
            intent.putExtra("filter_category", "Sevimlilər")
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        binding.btnSpeedTestMain.setOnClickListener {
            startActivity(Intent(this@MainActivity, SpeedTestActivity::class.java))
        }

        // Search düyməsi üçün Live TV-yə yönləndirmə
        binding.btnSearch.setOnClickListener {
            startActivity(Intent(this@MainActivity, LiveTvActivity::class.java))
        }
    }

    private fun setupFocusEffect(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_up))
                v.elevation = 20f
            } else {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scale_down))
                v.elevation = 0f
            }
        }
    }

    private fun startAuthProcess() {
        showLoading()
        checkAuthentication()
    }

    private fun checkAuthentication() {
        val url = "api.php?mac=$deviceMac"
        ApiClient.getService().checkMac(url).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    handleAuthResponse(result)
                } else {
                    handleFailure("Server xətası baş verdi.")
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    Handler(Looper.getMainLooper()).postDelayed({ checkAuthentication() }, 5000)
                } else {
                    handleFailure("İnternet bağlantısı yoxdur və ya serverə qoşulmaq mümkün olmadı.")
                }
            }
        })
    }

    private fun handleAuthResponse(response: ApiResponse) {
        if ("success".equals(response.status, ignoreCase = true)) {
            val expiry = response.expiryDate

            // Bütün məlumatları yadda saxla
            getSharedPreferences("neoplay_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("expiry_date", expiry)
                .putString("playlist_type", response.playlistType)
                .putString("m3u_url", response.m3uUrl)
                .putString("xtream_host", if (response.xtream != null) response.xtream.host else "")
                .putString("xtream_user", if (response.xtream != null) response.xtream.username else "")
                .putString("xtream_pass", if (response.xtream != null) response.xtream.password else "")
                .putBoolean("is_vod_enabled", response.isVodEnabled)
                .putBoolean("is_series_enabled", response.isSeriesEnabled)
                .apply()

            if (!expiry.isNullOrBlank() && !expiry.equals("null", ignoreCase = true)) {
                binding.tvExpiryInfo.text = "Abunəlik bitir: $expiry"
                binding.tvExpiryInfo.visibility = View.VISIBLE

                getSharedPreferences("neoplay_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("expiry_date", expiry)
                    .apply()
            } else {
                binding.tvExpiryInfo.visibility = View.GONE
            }

            showDashboard(response.isVodEnabled, response.isSeriesEnabled)
            loadAndCheckPlaylist()

            // Yenilənmə yoxlanışını və avtomatik başlatmanı idarə et
            handleAutoStart()
        } else if ("expired".equals(response.status, ignoreCase = true)) {
            showError("Abunəlik Müddəti Bitib", "Abunəliyiniz bitmişdir. Zəhmət olmasa dilerinizlə əlaqə saxlayın.")
        } else if ("not_found".equals(response.status, ignoreCase = true)) {
            showError("Cihaz Aktiv Edilməyib", "Cihazınız sistemdə qeydiyyatdan keçməyib. MAC adresini dilerə göndərin.")
        } else {
            showError("Xəta", if (response.message != null) response.message else "Naməlum xəta baş verdi.")
        }
    }

    private fun handleAutoStart() {
        val autoStart = getSharedPreferences("neoplay_prefs", Context.MODE_PRIVATE)
            .getBoolean("auto_start_last_channel", true)

        if (!autoStart) return

        val handler = Handler(Looper.getMainLooper())
        val checkInterval = 200L
        val maxWaitTime = 3000L
        var waitedTime = 0L

        val checkRunnable = object : Runnable {
            override fun run() {
                if (UpdateManager.isCheckFinished) {
                    // Yoxlanış bitdi, indi qərar verək
                    if (!UpdateManager.isUpdateFound) {
                        val intent = Intent(this@MainActivity, LiveTvActivity::class.java)
                        intent.putExtra("auto_start", true)
                        startActivity(intent)
                    } else {
                        // Yenilənmə var, avtomatik başlatmanı skip edirik ki, dialog görünsün
                    }
                } else if (waitedTime < maxWaitTime) {
                    waitedTime += checkInterval
                    handler.postDelayed(this, checkInterval)
                } else {
                    // Gözləmə müddəti bitdi (timeout), ehtiyat olaraq kanalı açırıq
                    val intent = Intent(this@MainActivity, LiveTvActivity::class.java)
                    intent.putExtra("auto_start", true)
                    startActivity(intent)
                }
            }
        }

        handler.post(checkRunnable)
    }

    private fun showLoading() {
        binding.loadingLayout.visibility = View.VISIBLE
        binding.dashboardLayout.visibility = View.GONE
        binding.errorOverlay.visibility = View.GONE
    }

    private fun loadAndCheckPlaylist() {
        val prefs = getSharedPreferences("neoplay_prefs", Context.MODE_PRIVATE)
        val type = prefs.getString("playlist_type", "m3u")
        val m3uUrl = prefs.getString("m3u_url", "https://kanal65.xyz/neoplay/playlist.m3u")
        val isVodEnabled = prefs.getBoolean("is_vod_enabled", true)
        val isSeriesEnabled = prefs.getBoolean("is_series_enabled", true)

        if ("xtream".equals(type, ignoreCase = true)) {
            runOnUiThread {
                binding.cardMovies.visibility = if (isVodEnabled) View.VISIBLE else View.GONE
                binding.cardSeries.visibility = if (isSeriesEnabled) View.VISIBLE else View.GONE

                var weightSum = 2.0f
                if (isVodEnabled) weightSum += 1.0f
                if (isSeriesEnabled) weightSum += 1.0f
                binding.cardsContainer.weightSum = weightSum
            }
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            var hasVod = false
            try {
                val url = URL(m3uUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                var line: String?
                var count = 0
                while (reader.readLine().also { line = it } != null && count < 2000) {
                    val trimmedLine = line!!.trim()
                    if (!trimmedLine.startsWith("#") && trimmedLine.isNotEmpty()) {
                        if (M3UParser.isVodChannel(trimmedLine)) {
                            hasVod = true
                            break
                        }
                        count++
                    }
                }
                reader.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val finalHasVod = hasVod
            runOnUiThread {
                val finalShowMovies = finalHasVod && isVodEnabled
                val finalShowSeries = finalHasVod && isSeriesEnabled

                binding.cardMovies.visibility = if (finalShowMovies) View.VISIBLE else View.GONE
                binding.cardSeries.visibility = if (finalShowSeries) View.VISIBLE else View.GONE

                var weightSum = 2.0f
                if (finalShowMovies) weightSum += 1.0f
                if (finalShowSeries) weightSum += 1.0f
                binding.cardsContainer.weightSum = weightSum
            }
        }
    }

    private fun showDashboard(isVodEnabled: Boolean, isSeriesEnabled: Boolean) {
        val colorBlue = "#0097D7"
        val colorRed = "#D2122E"
        val colorGreen = "#00AE42"

        val spannable = SpannableString("NEO PLAY")

        spannable.setSpan(ForegroundColorSpan(Color.parseColor(colorBlue)), 0, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor(colorRed)), 2, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor(colorGreen)), 5, 8, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvAppTitle.text = spannable

        val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("az"))
        binding.tvDate.text = sdf.format(Date())

        binding.cardMovies.visibility = if (isVodEnabled) View.VISIBLE else View.GONE
        binding.cardSeries.visibility = if (isSeriesEnabled) View.VISIBLE else View.GONE

        var weightSum = 2.0f
        if (isVodEnabled) weightSum += 1.0f
        if (isSeriesEnabled) weightSum += 1.0f
        binding.cardsContainer.weightSum = weightSum

        val fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeOut.duration = 500

        binding.loadingLayout.startAnimation(fadeOut)
        binding.loadingLayout.visibility = View.GONE
        binding.dashboardLayout.visibility = View.VISIBLE
        binding.errorOverlay.visibility = View.GONE

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.dashboardLayout.startAnimation(slideUp)
    }

    private fun showError(title: String, message: String) {
        binding.loadingLayout.visibility = View.GONE
        binding.dashboardLayout.visibility = View.GONE
        binding.errorOverlay.visibility = View.VISIBLE

        binding.errorTitle.text = title
        binding.errorMessage.text = message
        binding.macDisplay.text = "MAC: $deviceMac"
    }

    private fun handleFailure(message: String) {
        showError("Bağlantı Xətası", message)
    }
}
