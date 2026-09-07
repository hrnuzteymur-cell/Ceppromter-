# Cep Prompter

Cep Prompter, Android 8.0 ve sonrası için çevrimdışı öncelikli, filigransız teleprompter ve video kayıt uygulamasıdır.

## Beta 01'de çalışan özellikler

- Metin oluşturma, düzenleme, otomatik yerel saklama ve paylaş menüsünden metin alma
- Metin arama, favorileme, çoğaltma, silme, TXT/HTML/RTF içe aktarma ve TXT dışa aktarma
- Otomatik kaydırma, döngü, yatay/dikey ayna ve metin bazında kaldığı yeri hatırlama
- Metne dokunulduğu sürece duraklatma; parmakla yukarı/aşağı serbest gezinme
- Hız ve metin paneli saydamlığında yüzde göstergesi, kaydırıcı ve +/- düğmeleri
- Kalıcı yazı boyutu, satır aralığı, panel genişliği, geri sayım ve görünüm ayarları
- Ön/arka kamera, 720p/1080p/4K tercihi (cihaz desteklemezse güvenli alt kalite)
- Mikrofonlu veya mikrofonsuz MP4 kayıt, flaş, yakınlaştırma, 3x3 ızgara
- Videoyu doğrudan `Galeri > Filmler > Cep Prompter` konumuna kaydetme
- Kamera izni reddedildiğinde uygulama ayarlarına yönlendirme
- Hesapsız kullanım; metinler ve ayarlar cihazda kalır

## GitHub'dan APK indirme

1. Depoda **Actions** sekmesini açın.
2. **Cep Prompter APK** iş akışını seçin.
3. En üstteki başarılı çalıştırmayı açın.
4. Sayfanın altındaki **Cep-Prompter-beta01-debug-apk** dosyasını indirin.
5. ZIP'i açıp `app-debug.apk` dosyasını Android telefona kurun.

Android, GitHub'dan indirilen ilk APK için “bilinmeyen uygulama yükleme” izni isteyebilir. Kaynak kod bu depoda görülebilir; yine de yalnızca bu deponun Actions çıktısını kurun.

## Android Studio

Projeyi Android Studio'da açın, Gradle eşitlemesini tamamlayın ve `app` yapılandırmasını çalıştırın. Java 17 ve Android SDK 35 kullanılır.

## Kapsam durumu

`PRODUCT_SCOPE.md`, hedef ürünün değiştirilemez kapsamını ve henüz tamamlanmamış ileri düzey modülleri ayrı ayrı gösterir. Bir kutunun işaretli olması, özelliğin uygulamada erişilebilir ve gerçek işlem yaptığı anlamına gelir.
