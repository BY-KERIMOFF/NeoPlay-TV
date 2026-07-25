# Səs Problemi Həlli Walkthrough

Bəzi kanallarda səsin çıxmaması problemi (xüsusilə AC3 və MPEG-TS axınları üçün) pleyer sazlamalarının optimallaşdırılması ilə həll edildi.

## Görülən İşlər

### 1. TrackSelector Parametrləri Genişləndirildi
`DefaultTrackSelector` sazlamalarına cihazın rəsmi dəstəyindən kənar formatları belə açmağa cəhd etmək üçün aşağıdakı parametrlər əlavə edildi:
- `setExceedAudioConstraintsIfNecessary(true)`
- `setExceedRendererCapabilitiesIfNecessary(true)`
- `setExceedVideoConstraintsIfNecessary(true)`

Bu, pleyerin "ən yaxşı dəstəklənən" səs yolunu tapmaqda daha aqressiv olmasını təmin edir.

### 2. MIME Type Təyini Təkmilləşdirildi
IPTV kanalları (MPEG-TS və HLS) üçün MIME növü təyini daha dəqiq və geniş edildi. Bu, pleyerin axını emal edərkən daha uyğun dekoderləri (məsələn, AC3 üçün lazımi drayverləri) işə salmasına kömək edir.
- `/hls/`, `/mpegts/`, `/live/` kimi URL hissələrinə görə avtomatik MIME type seçimi əlavə edildi.

### 3. Decoder Fallback Rejimi Aktivləşdirildi
Həm əsas pleyerdə, həm də mini-playerdə proqram təminatı (software) dekoderlərinə keçid rejimi daha stabil hala gətirildi. Bu, aparat (hardware) dekoderi səsi aça bilmədikdə pleyerin proqram yolu ilə səsi bərpa etməsinə şərait yaradır.

## Yoxlama Nəticələri

- **Build:** Layihə uğurla build edildi.
- **Dəyişikliklər:** [PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java) və [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/LiveTvActivity.java) fayllarında lazımi sazlamalar tətbiq edildi.

> [!TIP]
> Əgər hələ də səs gəlməyən spesifik bir kanal olsa, zəhmət olmasa həmin kanalın axın növünü (stream type) bildirin, daha dəqiq tənzimləmə edək.
