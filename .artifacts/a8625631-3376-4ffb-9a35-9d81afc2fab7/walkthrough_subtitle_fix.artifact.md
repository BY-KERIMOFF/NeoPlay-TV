# Altyazı Oxunulurluğu Walkthrough

Proqramda altyazıların bütün səhnələrdə və arxa fonlarda aydın görünməsi üçün yeni vizual stil tətbiq edildi.

## Nələr Edildi?

### 1. Yeni Altyazı Dizaynı
Altyazılar üçün ən yüksək oxunulurluq dərəcəsinə malik olan "Outline" (Haşiyə) stili seçildi:
- **Rəng:** Saf Ağ (#FFFFFF)
- **Kənar:** Qalın Qara Outline (Bu, ağ mətni açıq rəngli səhnələrdə belə aydın göstərir).
- **Fon:** Şəffaf (Görüntünün qarşısını kəsməmək üçün).

### 2. Pleyer İnteqrasiyası
Həm əsas pleyerdə, həm də mini-playerdə bu sazlamalar tətbiq edildi:
- [PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java) faylında `SubtitleView` üçün stil və ölçü (24sp) təyin olundu.
- [LiveTvActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/LiveTvActivity.java) faylında kiçik pleyer üçün daha uyğun ölçü (18sp) seçildi.

## Yoxlama Nəticələri

- **Build:** Müvəffəqiyyətlə tamamlandı.
- **Nəticə:** Artıq sarı rəngli və ya nazik ağ altyazılar deyil, hər yerdə aydın oxunan peşəkar altyazı stili görünəcək.

> [!TIP]
> Altyazıların ölçüsü TV ekranları üçün optimallaşdırılıb. Əgər ölçünü böyütmək və ya kiçiltmək istəsəniz, bildirin.
