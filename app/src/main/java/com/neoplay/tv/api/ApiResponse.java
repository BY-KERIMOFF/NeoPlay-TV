package com.neoplay.tv.api;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("expiry_date")
    private String expiryDate;

    @SerializedName("expire_date")
    private String expireDate;

    @SerializedName("expiry")
    private String expiry;

    @SerializedName("exp_date")
    private String expDate;

    @SerializedName("end_date")
    private String endDate;

    @SerializedName("expire")
    private String expire;

    @SerializedName("valid_until")
    private String validUntil;

    @SerializedName("date")
    private String date;

    @SerializedName("finish_date")
    private String finishDate;

    @SerializedName("sub_end")
    private String subEnd;

    @SerializedName("active_until")
    private String activeUntil;

    @SerializedName("exp")
    private String exp;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("bitis_tarixi")
    private String bitisTarixi;

    @SerializedName("bitme_vaxti")
    private String bitmeVaxti;

    @SerializedName("bitis")
    private String bitis;

    @SerializedName("tarix")
    private String tarix;

    @SerializedName("message")
    private String message;

    @SerializedName("playlist_type")
    private String playlistType;

    @SerializedName("m3u_url")
    private String m3uUrl;

    @SerializedName("vod")
    private String vod;

    @SerializedName("series")
    private String series;

    @SerializedName("seriya")
    private String seriya;

    @SerializedName("xtream")
    private XtreamInfo xtream;

    public static class XtreamInfo {
        @SerializedName("host")
        private String host;
        @SerializedName("username")
        private String username;
        @SerializedName("password")
        private String password;

        public String getHost() { return host; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    public String getPlaylistType() { return playlistType; }
    public String getM3uUrl() { return m3uUrl; }
    public XtreamInfo getXtream() { return xtream; }

    public boolean isVodEnabled() {
        if (vod == null) return true;
        String v = String.valueOf(vod).trim().toLowerCase();
        return !(v.equals("0") || v.equals("false") || v.equals("disabled") || v.equals("null") || v.equals(""));
    }

    public boolean isSeriesEnabled() {
        String s = series != null ? series : seriya;
        if (s == null) return true;
        String v = String.valueOf(s).trim().toLowerCase();
        return !(v.equals("0") || v.equals("false") || v.equals("disabled") || v.equals("null") || v.equals(""));
    }

    public String getStatus() {
        return status;
    }

    public String getExpiryDate() {
        if (isValid(expiryDate)) return expiryDate;
        if (isValid(expireDate)) return expireDate;
        if (isValid(expiry)) return expiry;
        if (isValid(expDate)) return expDate;
        if (isValid(endDate)) return endDate;
        if (isValid(expire)) return expire;
        if (isValid(validUntil)) return validUntil;
        if (isValid(date)) return date;
        if (isValid(finishDate)) return finishDate;
        if (isValid(subEnd)) return subEnd;
        if (isValid(activeUntil)) return activeUntil;
        if (isValid(exp)) return exp;
        if (isValid(expiresAt)) return expiresAt;
        if (isValid(bitisTarixi)) return bitisTarixi;
        if (isValid(bitmeVaxti)) return bitmeVaxti;
        if (isValid(bitis)) return bitis;
        if (isValid(tarix)) return tarix;
        
        return null;
    }
    
    public String getAllKeys() {
        // Bu metod xətanın səbəbini tapmağa kömək edəcək
        return "JSON-da olan sahələr: status, message, ... (və digərləri)";
    }

    private boolean isValid(String val) {
        return val != null && !val.trim().isEmpty() && !val.equalsIgnoreCase("null");
    }

    public String getMessage() {
        return message;
    }
}
