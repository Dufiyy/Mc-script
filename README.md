# MC Build Contest Plugin

Bu Minecraft plugin'i, tema tabanlı yapı inşa yarışması için tasarlanmıştır. Oyuncular BuildLobby'ye katılır, tema seçer, bölgelerde yapı yapar ve puanlanır.

## Özellikler

- BuildLobby: Oyuncular /buildermap ile katılır (max oyuncu kontrolü).
- Tema Seçimi: Pusula ile GUI açılır, temalar oylanır. 60 saniye sürer.
- Oyun Başlatma: 10 oyuncu olursa oyun başlar.
- Yapı İnşa: Oyuncular tek tek 44x44 bölgelere dağıtılır, yaratıcı modda, 10 dakika süre. Sadece kendi alanında inşa edebilir.
- Blok Koruması: Ana mapteki bloklar kırılmaz, kendi yaptıkları kırılabilir. Duvarlar barrier ile ayrılmış.
- Görsel Efektler: Title, boss bar, ses efektleri, tab list bilgileri.
- Boss Bar: Süre ve tema gösterir.
- Tab List: Sağ tarafa durum bilgileri.
- Replay Sistemi: /buildreplay start/stop/pause/resume/speed/forward/backward ile kayıtlı değişiklikleri izle, hız kontrolü. Spectator modunda serbest kamera, sinematik oynatma, blok değişikliklerinde oyuncu adı ve eylem mesajları.
- Config Reload: /buildreload ile güvenli reload.
- Dil Desteği: Config ile mesajları özelleştir.
- Zemin Doldurucu: Alan sahibi sağ elindeki diamond block'a sağ tıkla inventory açar, config'deki bloklardan seçim yapar (ileri/geri butonları ile), seçilen blokla zemini doldurur.

## Komutlar

- `/buildermap`: BuildLobby'ye katıl (max oyuncu kontrolü var).
- `/buildreplay start|stop|forward|backward`: Replay kontrolü.
- `/buildreload`: Config'i yeniden yükle.

## Komutlar

- `/buildermap`: BuildLobby'ye katıl.

## Aşamalar

1. **Katılma**: /buildermap ile lobby'ye git, pusula al.
2. **Tema Oylama**: GUI'de tema seç, 60 saniye.
3. **Oyun Başlatma**: 10 oyuncu olursa başlar.
4. **Yapı Yapma**: Bölgelere dağıt, 10 dakika yaratıcı mod.
5. **Puanlama**: Oyun bitince kazanan belirlenir.

## Kurulum

1. Jar dosyasını plugins klasörüne koy.
2. Sunucuyu yeniden başlat.

## Yapılandırma

`config.yml` dosyasında ayarlar:

- `build-lobby`: Lobby konumu.
- `regions`: Bölge konumları listesi.
- `themes`: Tema listesi.
- `timers`: Oylama ve build süreleri (saniye).
- `min-players`: Minimum oyuncu sayısı.
- `max-players`: Maksimum oyuncu sayısı.
- `region-size`: Bölge boyutu (44x44).
Sunucu yeniden başlatıldığında config yüklenir.

## Geliştirme

Mave n ile build: `mvn clean packgage`