# Cep Prompter — Değiştirilemez Ürün Kapsamı

## Birinci kural

Bu belgede yer alan özellikler final uygulamanın kapsamıdır. Bir özellik teknik olarak cihaz desteğine bağlıysa kullanıcıya açık bir uyumluluk mesajı ve güvenli geri dönüş seçeneği sunulur; özellik sessizce kaldırılmaz.

## 1. Teleprompter motoru

- [x] Sabit hızla otomatik kaydırma
- [ ] Kelime/dakika ile hız ayarı
- [ ] Belirlenen sürede metni tamamlama
- [ ] Türkçe konuşmayı takip ederek kaydırma
- [ ] Konuşma durduğunda bekleme
- [ ] Metinde ileri/geri atlamayı yeniden bulma
- [x] Dokunarak oynatma ve duraklatma
- [x] Dokununca geçici duraklatma ve bırakınca sürdürme
- [x] Parmakla yukarı/aşağı serbest gezinme
- [x] Metinde elle ileri/geri gezinme kontrolü
- [ ] İlerleme çubuğundan konuma atlama
- [x] Döngü altyapısı
- [ ] Bölüm işaretleri ve işaretler arasında atlama
- [x] Kaldığı konumu metin bazında hatırlama
- [x] Başlangıç geri sayımı
- [ ] Metin bittiğinde otomatik kayıt sonlandırma

## 2. Okuma görünümü

- [x] Yazı boyutu
- [ ] Yazı tipi ve renk seçimi
- [x] Satır aralığı veri modeli
- [x] Okuma alanı genişliği ve saydamlığı veri modeli
- [x] Aktif satır göstergesi
- [x] Yatay ayna
- [x] Dikey ayna altyapısı
- [ ] Okuma alanını üst/orta/alt konumlandırma
- [ ] Kalın, italik, altı çizili ve renkli metin
- [ ] Her metne özel görünüm ayarı
- [ ] Disleksi dostu yazı tipleri
- [x] Sağdan sola dil altyapısı
- [x] Dikey ve yatay ekran

## 3. Kamera ve video

- [x] Ön kamera
- [x] Arka kamera
- [x] Kayıttan önce kamera değiştirme
- [ ] Desteklenen cihazlarda kayıt sırasında kamera değiştirme
- [x] Video ve ses kaydı
- [x] Filigransız kayıt
- [x] Galeriye MediaStore üzerinden kayıt
- [x] Başlangıç geri sayımı
- [x] Arka kamera flaşı
- [x] Sıkıştırma hareketiyle yakınlaştırma
- [ ] Dokunarak netleme
- [ ] Netlik/pozlama kilidi
- [x] 3x3 kadraj ızgarası
- [x] 720p/1080p/4K kullanıcı seçimi ve cihaz uyumluluk geri dönüşü
- [ ] Kare hızı seçimi
- [ ] Dahili/harici mikrofon seçimi
- [ ] Mikrofon seviye göstergesi
- [ ] Video çekimlerini uygulama içinde listeleme
- [ ] Birden fazla çekim arasından seçim

## 4. Metin ve dosya yönetimi

- [x] Sınırsız yerel metin
- [x] Oluşturma, düzenleme ve kaydetme
- [x] Başka uygulamanın paylaş menüsünden metin alma
- [x] Arama ve güncellenme tarihine göre sıralama
- [ ] Klasörler
- [x] Favoriler
- [x] Çoğaltma, düzenleyerek yeniden adlandırma ve tekli silme
- [x] TXT içe/dışa aktarma
- [x] HTML ve RTF düz metin içe aktarma
- [ ] DOCX içe aktarma
- [ ] Metin tabanlı PDF içe aktarma
- [x] Android dosya sağlayıcıları üzerinden Drive/Dropbox/OneDrive dosya seçimi
- [ ] Yerel yedekleme ve geri yükleme
- [ ] Gerçek zamanlı cihazlar arası eşitleme için maliyetsiz seçenek

## 5. Kontrol ve kayan pencere

- [ ] Android kayan pencere teleprompter servisi
- [ ] Pencere taşıma, boyut ve saydamlık
- [ ] Kamera/canlı yayın/görüşme uygulamalarının üzerinde çalışma
- [ ] Bluetooth klavye
- [ ] Bluetooth sunum kumandası
- [ ] Bluetooth pedal
- [ ] Ses tuşları
- [ ] Oyun kumandası
- [ ] Wear OS kontrolü
- [ ] Aynı Wi-Fi üzerinden ikinci Android cihazla kumanda
- [ ] Uzaktan başlatma, durdurma, hız ve konum kontrolü
- [ ] İkinci cihazda önizleme

## 6. Video düzenleme ve çıktı

- [ ] Videoyu kırpma ve döndürme
- [ ] Dikey, yatay ve kare çıktı
- [ ] Otomatik altyazı zamanlaması
- [ ] Altyazı metni/zamanı düzenleme
- [ ] SRT dışa aktarma
- [ ] Videoya isteğe bağlı altyazı basma
- [ ] Logo ve yazı ekleme
- [ ] Yerel/telifsiz fon müziği ekleme
- [ ] Basit arka plan bulanıklaştırma
- [ ] Yeşil perde ile arka plan değiştirme
- [ ] Cihaz içi temel gürültü azaltma
- [ ] Android paylaşım menüsü

## 7. Deneyim, erişilebilirlik ve gizlilik

- [x] Türkçe arayüz
- [x] Açık/koyu sistem teması
- [x] Hesapsız temel kullanım
- [x] Çevrimdışı metin ve teleprompter
- [x] Kamera/mikrofon çalışma anında izin isteme
- [x] İzin reddinde uygulamaya özel Ayarlar yönlendirmesi
- [x] Ayarların kalıcı saklanması
- [ ] Büyük metin ve ekran okuyucu denetimi
- [ ] Tablet ve katlanabilir ekran uyarlaması
- [ ] İlk kullanım rehberi
- [ ] Hata/kayıt kurtarma akışı

## Ücretsiz yapılamayan veya ek altyapı gerektiren işlemler

Göz teması düzeltme, yapay zekâ dublajı, ses klonlama, otomatik B-roll ve büyük üretken modeller çevrimdışı ve tüm Android cihazlarda güvenilir biçimde çalışmadığı sürece çekirdek uygulamanın zorunlu bağımlılığı yapılmaz. Cihaz üzerinde ücretsiz ve güvenilir çözüm bulunduğunda isteğe bağlı modül olarak değerlendirilir.
