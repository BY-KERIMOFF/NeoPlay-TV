# Hərəkət Edən Elan Mətninin (Ticker) Təkmilləşdirilməsi Planı

Bu plan, pleyer ekranında sağdan sola hərəkət edən elan lentinin (announcement) hər hansı bir arxa fon üzərində tam aydın və oxunaqlı olmasını təmin edəcək.

## İstifadəçi Review Tələb Olunan Məqamlar

> [!IMPORTANT]
> Elan mətni artıq sarı rəngdə deyil, **Ağ** rəngdə olacaq. Mətnin altına isə ekranın tam eni boyu uzanan, yarı-şəffaf **Qara zolaq** əlavə ediləcək. Bu, peşəkar TV kanallarındakı "son dəqiqə" xəbər lenti stilidir.

## Təklif Olunan Dəyişikliklər

### 1. Vizual Konteynerin Əlavə Edilməsi (Ticker Bar)
Mətnin hər hansı fonda (məsələn, ağ səhnələrdə) itməməsi üçün altına tünd zolaq qoyulacaq.

#### [MODIFY] [activity_player.xml](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/res/layout/activity_player.xml)
- `tvAnnouncement` elementi bir `FrameLayout` (Konteyner) daxilinə yerləşdiriləcək.
- Bu konteynerə `android:background="#80000000"` (yarı-şəffaf qara) fon veriləcək.
- Mətnin rəngi `@color/white` olaraq dəyişdiriləcək.

### 2. Animasiya Parametrlərinin Düzəldilməsi
Mətnin yeni zolaq daxilində tam mərkəzdə durması və kəsilmədən hərəkət etməsi üçün kodda kiçik düzəliş ediləcək.

#### [MODIFY] [PlayerActivity.java](file:///home/by-kerimoff/AndroidStudioProjects/Neoplay/app/src/main/java/com/neoplay/tv/PlayerActivity.java)
- Animasiya zamanı mətni kəsilməsinə səbəb ola biləcək köhnə padding və margin dəyərləri yeni "zolaq" dizaynına uyğunlaşdırılacaq.

## Yoxlama Planı

### Avtomatlaşdırılmış Testlər
- `./gradlew assembleDebug` ilə build yoxlanışı.

### Manual Yoxlama
- Pleyer ekranında elan mətni çıxdıqda, onun altında tünd zolağın olduğu və ağ hərflərin kontrastlı şəkildə axdığı yoxlanılacaq.
