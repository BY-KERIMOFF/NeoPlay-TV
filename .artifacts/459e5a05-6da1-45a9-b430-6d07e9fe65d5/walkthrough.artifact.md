# Walkthrough - İdarəetmə və Kanal Seçimi Təkmilləşdirilməsi

Bu yeniləmə ilə tətbiqin həm naviqasiyası, həm də pleyer daxilində kanal seçimi funksiyaları təkmilləşdirildi.

## Edilən Dəyişikliklər

### 1. Rəqəmlərlə Kanal Seçimi (Numeric Switching)
[PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/PlayerActivity.java) faylına yeni funksiya əlavə edildi:
- Pleyerdə olarkən pultun rəqəm düymələrini (0-9) basdıqda həmin nömrəli kanala keçid edilir.
- Məsələn, "1" və "2" düymələrini ardıcıl basdıqda ekranda "12" görünür və 2.5 saniyə sonra 12-ci kanal açılır.
- Bunun üçün [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/layout/activity_player.xml) faylına mərkəzdə görünən böyük rəqəm paneli əlavə edildi.

### 2. Sol Düymə Dəstəyi (DPAD LEFT)
- Kanal siyahısında olarkən pultun **Sol** düyməsi ilə kateqoriyalara sürətli keçid əlavə edildi.
- Canlı yayım izləyərkən **Sol** düyməsi pleyeri bağlayaraq kateqoriyalara qayıdır (VOD-larda isə geri çəkmə funksiyasını saxlayır).

### 3. Səs Paneli (Volume UI) Təkmilləşdirilməsi
[PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/PlayerActivity.java) və [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/res/layout/activity_player.xml) yeniləndi:
- **Rəngarəng Səs Paneli:** Səs səviyyəsi artıq rəngarəng bir qradiyentlə (Yaşıl -> Qızılı -> Qırmızı) göstərilir. Bunun üçün xüsusi [volume_progress_drawable.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/drawable/volume_progress_drawable.xml) yaradıldı.
- **Daha Böyük Faiz:** Səs faizi (`%`) daha böyük və qalın fontla (`32sp`) yazıldı ki, uzaqdan rahat görünsün.
- **Dəqiq İşləmə:** Səs düymələri basıldıqda UI-ın dərhal və dəqiq yenilənməsi üçün məntiq təkmilləşdirildi.

### 4. Pleyer Daxili Kateqoriya Naviqasiyası
Artıq canlı kanalı izləyərkən pultun **Sol** düyməsini basdıqda pleyer bağlanmır, əvəzində ekranın sol tərəfində **Kateqoriyalar Menyusu** açılır:
- **Sol Düymə:** Kateqoriya menyusunu açır.
- **Sağ Düymə:** Kateqoriyadan həmin kateqoriyanın kanallarına keçid edir.
- **Kateqoriya Seçimi:** Bir kateqoriyanı seçdikdə sağ tərəfdə həmin kateqoriyanın kanalları görünür. Beləliklə, yayımı dayandırmadan digər kanallara baxa bilərsiniz.
- **Geri (Back):** Menyuları bağlayır, əgər menyu yoxdursa pleyerdən çıxır.

### 5. Texniki Nasazlıq Ekranı (Technical Failure Overlay)
[PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/PlayerActivity.java) və [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/res/layout/activity_player.xml) yeniləndi:
- **Animasiyalı Xəbərdarlıq:** Əgər bir kanal texniki səbəblərdən açılmazsa, ekranda tam ekran ölçüsündə **"MÜVƏQQƏTİ TEXNİKİ NASAZLIQ"** yazısı görünəcək.
- **Pulsasiya Effekti:** Bu yazı sadə durmayacaq, pulsasiya (`pulse`) animasiyası ilə hərəkət edəcək ki, bu da tətbiqin donmadığını, sadəcə yayımın olmadığını göstərir.
- **Avtomatik Bərpa:** Əgər yayım bərpa olunarsa və ya pleyer kanalı aça bilsə, bu xəbərdarlıq dərhal itəcək.

### 8. Pleyer Daxili Kanal Axtarışı (In-Player Search)
[PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/example/neo_play/PlayerActivity.java) və [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/res/layout/activity_player.xml) yeniləndi:
- **Sürətli Axtarış Paneli:** Pleyerdə kanal siyahısını açdıqda ən üst hissədə axtarış sahəsi görünəcək.
- **Real-vaxt Süzgəcləmə:** Siz hərfləri yazdıqca siyahı avtomatik olaraq süzülür və yalnız uyğun kanallar göstərilir.
- **TV Pultu ilə Uyğunluq:** Kanal siyahısında olarkən pultun **Yuxarı** düyməsini basmaqla axtarış sahəsinə keçmək mümkündür.
- **Avtomatik Sıfırlama:** Kateqoriya dəyişdikdə axtarış sahəsi avtomatik təmizlənir.

### 10. Play Store-a Hazırlıq (Package Refactoring)
Tətbiqin bütün daxili strukturu Play Store qaydalarına uyğun olaraq yeniləndi:
- **Yeni Paket Adı:** Artıq tətbiqin rəsmi adı `com.neoplay.tv`-dir.
- **Kod Yenilənməsi:** Bütün Java/Kotlin faylları, AndroidManifest və build scriptləri yeni paket adına uyğunlaşdırıldı.
- **Qovluq Strukturu:** Fayllar `com/neoplay/tv` qovluq iyerarxiyasına köçürüldü.
- **Build Təsdiqi:** Tətbiq yeni paket adı ilə uğurla yığıldı.

## Növbəti Addım: Keystore Yaradılması
Play Store-a yükləmək üçün tətbiqi imzalamalısınız. Bunun üçün Android Studio-da:
1. `Build` -> `Generate Signed Bundle / APK...` menyusuna girin.
2. `Android App Bundle` seçin.
3. `Create new...` düyməsi ilə yeni bir **Keystore** faylı yaradın və şifrənizi təyin edin.
4. Yaradılan `.aab` faylını Play Console-a yükləyə bilərsiniz.
