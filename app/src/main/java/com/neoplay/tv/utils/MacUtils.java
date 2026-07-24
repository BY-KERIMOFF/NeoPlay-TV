package com.neoplay.tv.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MacUtils {

    public static String getMacAddress(Context context) {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0") && !nif.getName().equalsIgnoreCase("eth0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    continue;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Fallback to Android ID if MAC is not available
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (androidId != null) {
            // Create a pseudo-MAC from Android ID
            StringBuilder pseudoMac = new StringBuilder();
            for (int i = 0; i < Math.min(androidId.length(), 12); i += 2) {
                if (i > 0) pseudoMac.append(":");
                pseudoMac.append(androidId.substring(i, i + 2).toUpperCase());
            }
            // Pad if shorter than 12 chars
            while (pseudoMac.length() < 17) {
                pseudoMac.append(":00");
            }
            return pseudoMac.toString();
        }

        return "00:00:00:00:00:00";
    }
}
