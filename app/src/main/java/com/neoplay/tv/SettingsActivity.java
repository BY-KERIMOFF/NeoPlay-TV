package com.neoplay.tv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.neoplay.tv.databinding.ActivitySettingsBinding;
import com.neoplay.tv.utils.MacUtils;
import com.neoplay.tv.utils.SleepTimerManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("neoplay_prefs", MODE_PRIVATE);

        loadInfo();
        setupListeners();
    }

    private void loadInfo() {
        binding.tvMacAddress.setText("MAC: " + MacUtils.getMacAddress(this));
        
        String expiry = prefs.getString("expiry_date", null);
        if (expiry != null && !expiry.equalsIgnoreCase("null") && !expiry.isEmpty()) {
            binding.tvExpiryDate.setText("Bitiş Tarixi: " + expiry);
        } else {
            binding.tvExpiryDate.setText("Bitiş Tarixi: Sınırsız / Naməlum");
        }
        
        binding.cbBootOnStartup.setChecked(prefs.getBoolean("boot_on_startup", false));
        binding.cbAutoStartLast.setChecked(prefs.getBoolean("auto_start_last_channel", true));
        binding.etEpgUrl.setText(prefs.getString("manual_epg_url", ""));

        String pType = prefs.getString("player_type", "exo2");
        if ("standard".equalsIgnoreCase(pType)) {
            binding.rbExoStandard.setChecked(true);
        } else {
            binding.rbExo2.setChecked(true);
        }
    }

    private void setupListeners() {
        setupFocusEffect(binding.cbBootOnStartup);
        setupFocusEffect(binding.cbAutoStartLast);
        setupFocusEffect(binding.rbExoStandard);
        setupFocusEffect(binding.rbExo2);
        setupFocusEffect(binding.btnRefreshData);
        setupFocusEffect(binding.btnBack);
        
        setupFocusEffect(binding.btnTimerOff);
        setupFocusEffect(binding.btnTimer15);
        setupFocusEffect(binding.btnTimer30);
        setupFocusEffect(binding.btnTimer60);
        setupFocusEffect(binding.btnTimer120);
        
        setupFocusEffect(binding.cbAppLock);
        setupFocusEffect(binding.btnChangePin);
        setupFocusEffect(binding.btnPrivacyPolicy);

        binding.cbBootOnStartup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("boot_on_startup", isChecked).apply();
            Toast.makeText(this, "Parametr yadda saxlanıldı", Toast.LENGTH_SHORT).show();
        });

        binding.cbAutoStartLast.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_start_last_channel", isChecked).apply();
            Toast.makeText(this, "Parametr yadda saxlanıldı", Toast.LENGTH_SHORT).show();
        });

        binding.cbAppLock.setChecked(prefs.getBoolean("app_lock_enabled", false));
        binding.btnChangePin.setVisibility(binding.cbAppLock.isChecked() ? View.VISIBLE : View.GONE);

        binding.cbAppLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && prefs.getString("app_pin", "0000").equals("0000")) {
                Toast.makeText(this, "Lütfən PİN kodu dəyişməyi unutmayın (Default: 0000)", Toast.LENGTH_LONG).show();
            }
            prefs.edit().putBoolean("app_lock_enabled", isChecked).apply();
            binding.btnChangePin.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        binding.btnChangePin.setOnClickListener(v -> showChangePinDialog());

        binding.rgPlayerChoice.setOnCheckedChangeListener((group, checkedId) -> {
            String type = (checkedId == R.id.rbExoStandard) ? "standard" : "exo2";
            prefs.edit().putString("player_type", type).apply();
            Toast.makeText(this, "Seçildi: " + (type.equals("standard") ? "Standart Exo" : "Exo 2 / V2"), Toast.LENGTH_SHORT).show();
        });

        binding.btnRefreshData.setOnClickListener(v -> {
            String manualEpg = binding.etEpgUrl.getText().toString().trim();
            prefs.edit().putString("manual_epg_url", manualEpg).apply();
            
            // Həm daxili mənbələri, həm də manual linki yenilə
            com.neoplay.tv.utils.XMLTVParser.syncDefaultSources();
            if (!manualEpg.isEmpty()) {
                com.neoplay.tv.utils.XMLTVParser.downloadAndParse(manualEpg);
            }
            Toast.makeText(this, "Bütün EPG mənbələri yenilənir...", Toast.LENGTH_SHORT).show();
        });

        binding.btnPrivacyPolicy.setOnClickListener(v -> {
            String url = "https://github.com/BY-KERIMOFF/NeoPlay-TV/blob/main/PRIVACY_POLICY.md"; // Default placeholder
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(android.net.Uri.parse(url));
            startActivity(i);
        });

        binding.btnTimerOff.setOnClickListener(v -> setSleepTimer(0));
        binding.btnTimer15.setOnClickListener(v -> setSleepTimer(15));
        binding.btnTimer30.setOnClickListener(v -> setSleepTimer(30));
        binding.btnTimer60.setOnClickListener(v -> setSleepTimer(60));
        binding.btnTimer120.setOnClickListener(v -> setSleepTimer(120));

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setSleepTimer(int minutes) {
        SleepTimerManager manager = SleepTimerManager.getInstance();
        if (minutes == 0) {
            manager.cancelTimer();
            binding.tvCurrentTimerStatus.setText("Status: Qapalı");
            Toast.makeText(this, "Yuxu taymeri ləğv edildi", Toast.LENGTH_SHORT).show();
        } else {
            manager.startTimer(minutes, this);
            binding.tvCurrentTimerStatus.setText("Status: Aktiv (" + minutes + " dəq)");
        }
    }

    private void showChangePinDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        builder.setTitle("PİN Kodu Dəyişdir");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Yeni 4 rəqəmli PİN");
        
        builder.setView(input);
        builder.setPositiveButton("YADDA SAXLA", (dialog, which) -> {
            String newPin = input.getText().toString();
            if (newPin.length() == 4) {
                prefs.edit().putString("app_pin", newPin).apply();
                Toast.makeText(this, "PİN kod uğurla dəyişdirildi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PİN 4 rəqəmli olmalıdır!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("LƏĞV ET", null);
        builder.show();
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
