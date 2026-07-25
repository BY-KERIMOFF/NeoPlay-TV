# Səs Problemi Həlli (Mərhələ 2) Walkthrough

Bəzi kanallarda hələ də davam edən səs problemini həll etmək üçün pleyer sazlamaları daha dərindən optimallaşdırıldı.

## Görülən İşlər

### 1. TsExtractor Flag-lərinin Artırılması
IPTV axınlarında (xüsusilə MPEG-TS) səsin düzgün tapılmasına mane olan daxili xətaları (splice info stream) görməzdən gəlmək üçün `FLAG_IGNORE_SPLICE_INFO_STREAM` bayrağı əlavə edildi. Bu, səs yolunun (audio track) pleyer tərəfindən daha stabil tapılmasını təmin edir.

### 2. TrackSelector-un Təkmilləşdirilməsi
Həm əsas pleyerdə, həm də mini-playerdə `DefaultTrackSelector` parametrləri genişləndirildi:
- `setExceedAudioConstraintsIfNecessary(true)`: Cihazın rəsmi səs imkanlarından kənar formatları belə açmağa çalışmaq üçün.
- `setExceedRendererCapabilitiesIfNecessary(true)`: Dekoderin rəsmi imkanlarını "aşaraq" səsi açmaq üçün.

### 3. Decoder Fallback və Software Rendering
Pleyer indi aparat (hardware) dekoderi uğursuz olduqda dərhal proqram (software) dekoderlərinə müraciət edir. Bu, xüsusilə AC3 və digər Dolby formatlı səslərin bəzi TV bokslarda açılmasına kömək edəcək.

## Fayl Dəyişiklikləri
- [PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java)
- [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/LiveTvActivity.java)

## Yoxlama Nəticələri
- **Build:** `./gradlew assembleDebug` uğurla tamamlandı.
- **APK:** Yeni APK (`neoplay_v9.apk`) hazırdır.

> [!IMPORTANT]
> Əgər hələ də səs gəlməyən kanal olsa, bu artıq cihazın sistemində həmin səs formatı üçün ümumiyyətlə dekoderin olmaması deməkdir. Belə halda növbəti mərhələdə FFmpeg kitabxanasını layihəyə əlavə etməli olacağıq.
