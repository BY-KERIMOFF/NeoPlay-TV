# HTTP/2 və PHP Stream Linklərinin (`stream.php?stream=...`) Açılması Həlli

`https://3bneoplay65.xyz/M2/stream.php?stream=16045/index.m3u8` və `3bliveneoplay.com` kimi HTTP/2 və PHP mühərriki istifadə edən HLS linklərinin ExoPlayer-də səhvsiz açılması üçün OkHttp DataSourcevə MimeType dəqiqləşdirməsi əlavə olunacaq.

## User Review Required

- **HTTP/2 & OkHttp İnteqrasiyası**: ExoPlayer şəbəkə mühərriyi `OkHttpDataSource` ilə əvəzlənəcək ki, HTTP/2, HTTPS-dən HTTP-yə yönləndirmələr və PHP stream keçidləri problemsiz açılsın.

## Proposed Changes

### [Gradle & Player]
- `gradle/libs.versions.toml` & `app/build.gradle.kts`: `androidx.media3:media3-datasource-okhttp` kitabxanasının əlavə edilməsi.
- `PlayerActivity.java`: OkHttp şəbəkə mühərrikinin, HTTP/2 protokolunun və `stream.php` üçün HLS MimeType (`application/x-mpegURL`) təyin edilməsi.
- `LiveTvActivity.java`: Mini player üçün eyni OkHttp / HLS dəstəyinin tətbiqi.

## Verification Plan

### Manual Verification
- `https://3bneoplay65.xyz/M2/stream.php?stream=16045/index.m3u8` linkinin tətbiqdə dərhal açıldığını və yayımın başladığını yoxlamaq.
