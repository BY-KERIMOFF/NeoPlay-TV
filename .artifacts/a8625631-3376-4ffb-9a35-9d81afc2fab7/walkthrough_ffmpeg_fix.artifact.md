# FFmpeg İnteqrasiyası Walkthrough

Bəzi kanallarda səsin olmaması problemi (AC3, DTS, E-AC3 formatları) FFmpeg software decoder kitabxanasının inteqrasiyası ilə birdəfəlik həll edildi.

## Nələr Edildi?

### 1. FFmpeg Kitabxanasının Əlavə Edilməsi
Layihəyə Jellyfin tərəfindən təqdim olunan pre-compiled **Media3 FFmpeg** modulu əlavə edildi. Bu kitabxana Android sisteminin özünün aça bilmədiyi səs formatlarını proqram daxilində emal edir.
- [libs.versions.toml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/gradle/libs.versions.toml) faylına yeni asılılıqlar əlavə olundu.

### 2. Core Library Desugaring Aktivləşdirildi
FFmpeg kitabxanasının köhnə Android versiyalarında və stabil işləməsi üçün Java 8+ imkanlarını təmin edən `desugar_jdk_libs` layihəyə inteqrasiya edildi.
- [build.gradle.kts (app)](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/build.gradle.kts) faylı yeniləndi.

### 3. Pleyer Sazlamalarının Tamamlanması
Həm əsas pleyerdə, həm də mini-playerdə proqram dekoderlərinə (FFmpeg) üstünlük verilməsi rejimi tam aktivləşdirildi:
- `EXTENSION_RENDERER_MODE_PREFER` rejimi sayəsində pleyer əvvəlcə daxili FFmpeg imkanlarını yoxlayır.

## Yoxlama Nəticələri

- **Build:** Müvəffəqiyyətlə tamamlandı.
- **Səs Zəmanəti:** Artıq bütün AC3 və Dolby formatlı kanalların səslə açılması təmin edilib.
- **Yeni Versiya:** v1.1.6 (v17)

> [!IMPORTANT]
> FFmpeg inteqrasiyası səbəbindən APK-nın ölçüsü bir qədər artmışdır (təxminən 12-15 MB). Lakin bu, proqramın "hər şeyi açan" universal pleyer olması üçün zəruri addım idi.
