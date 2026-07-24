# Xtream VOD (Filmlər və Seriallar) Tam Bərpası

Bu plan, Xtream Codes rejimində "Movies" və "Series" bölmələrinin niyə boş qaldığını və ya görünmədiyini həll etmək üçün hazırlanmışdır. Xtream panelləri filmləri canlı kanallardan tamamilə fərqli bir API strukturunda saxlayır.

## User Review Required

> [!IMPORTANT]
> Xtream rejimində filmlərin görünməsi üçün həm filmlərin siyahısını, həm də onların kateqoriyalarını ayrıca çəkməliyik. Hazırkı sistem yalnız canlı kanalların kateqoriyalarını göstərirdi.

## Proposed Changes

### 1. API Genişləndirilməsi

#### [MODIFY] [ApiService.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/api/ApiService.java)
- VOD kateqoriyalarını (`get_vod_categories`) və Serial kateqoriyalarını (`get_series_categories`) çəkmək üçün yeni metodlar əlavə ediləcək.

### 2. Xtream VOD Məntiqinin Yenilənməsi

#### [MODIFY] [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/LiveTvActivity.java)
- **Dinamik Yükləmə:** Ana ekrandan "MOVIES" seçildikdə tətbiq artıq:
    1.  İlk olaraq Xtream-dan **Film Kateqoriyalarını** çəkəcək.
    2.  Sonra bütün **Film siyahısını** çəkib arxa planda qruplaşdıracaq (İndeksləmə).
- **Seriallar:** Eyni məntiq "SERIES" bölməsi üçün də tətbiq olunacaq.
- **Link Düzəlişi:** Xtream VOD linklərinin formatı bəzi serverlərdə fərqli ola bilər (`movie` yerinə `vod` və s.). Tətbiq bir neçə variantı yoxlayacaq.

### 3. Dizayn və Naviqasiya

#### [MODIFY] [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/LiveTvActivity.java)
- VOD rejimində sol tərəfdəki sidebarın (kateqoriyaların) film kateqoriyaları ilə dolması təmin ediləcək.
- "Sevimlilər" kateqoriyası VOD rejimində də ən başda görünəcək.

## Verification Plan

### Automated Tests
- `assembleDebug` build testi.

### Manual Verification
- Xtream rejimində "MOVIES" düyməsinə basdıqda sol tərəfdə film kateqoriyalarının (Action, Comedy və s.) gəldiyini yoxlamaq.
- Kateqoriya seçildikdə sağ tərəfdə filmlərin siyahılandığını təsdiqləmək.
- Bir filmə kliklədikdə pleyerin açılması.

> [!TIP]
> Bu dəyişikliklərdən sonra Xtream bölməniz tam dolacaq və artıq heç bir "boş ekran" problemi qalmayacaq.
