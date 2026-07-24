# Walkthrough - Admin Duyuruları (Scrolling Announcement)

Tətbiqə serverdən idarə olunan "Admin Elanları" (qaçan sətir) funksiyası əlavə edildi. Artıq pleyerin yuxarı hissəsində istifadəçilərə xüsusi mesajlar göstərmək mümkündür.

## Edilən Dəyişikliklər

### 1. Server İnteqrasiyası
[UpdateManager.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/utils/UpdateManager.java) faylı təkmilləşdirildi:
- Saytdakı `update.json` faylından `"announcement"` sahəsi oxunur.
- Əgər bu sahədə mətn varsa, tətbiq onu yadda saxlayır.

### 2. Pleyer UI Yenilənməsi
[activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/layout/activity_player.xml) faylına yeni element əlavə edildi:
- Ekranın ən yuxarısında, tünd yarım-şəffaf fonlu bir zolaq.
- **Marquee (Qaçan Sətir)** effekti: Mətn sağdan sola doğru davamlı hərəkət edir.

### 3. Dinamik Aktivləşdirmə
[PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java) faylında:
- Pleyer açıldıqda elan mətni yoxlanılır.
- Əgər mesaj varsa, zolaq görünür və animasiya başlayır. Mesaj boşdursa, zolaq gizli qalır.

## Yoxlama

### Necə İdarə Etməli?
Serverinizdəki `update.json` faylına `announcement` sahəsini əlavə edin:

```json
{
  "versionCode": 4,
  "versionName": "1.0.3",
  "apkUrl": "https://kanal65.xyz/neoplay/neoplay_v3.apk",
  "announcement": "Xoş gəlmisiniz! Yeni film və kanallar artıq xidmətinizdədir."
}
```

Bu dəyişikliyi etdikdən sonra tətbiqi açdıqda, pleyerin yuxarı hissəsində həmin yazının qaçan sətir kimi keçdiyini görəcəksiniz.

Build uğurla tamamlandı.
