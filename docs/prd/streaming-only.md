# PRD: KITSUNE StreamingOnly

- **Status:** DRAFT — menunggu review dan approval pengguna
- **Tanggal:** 28 September 2026
- **Project:** KITSUNE, fork Aniyomi
- **Baseline:** `f81cc5c5d7522b45f837bc95a8e59233287e9007`
- **Target UAT utama:** Infinix SMART 9 HD X6532C, Android 14

## 1. Ringkasan produk

`KITSUNE StreamingOnly` adalah varian eksperimen KITSUNE yang difokuskan hanya untuk menemukan dan menonton anime melalui extension. Varian ini tidak menghapus fitur dari aplikasi lengkap; fitur non-streaming hanya dinonaktifkan dalam flavor eksperimen.

Produk ini dibuat untuk mempersempit ruang UAT dan menemukan akar masalah ketika:

- source tidak menemukan anime/season;
- episode list kosong;
- server tersedia tetapi video gagal di-resolve;
- source tertentu berhasil di aplikasi lain tetapi gagal di KITSUNE;
- user membutuhkan pilihan source dan fallback yang lebih jelas.

## 2. Masalah pengguna

Pengguna mengalami kondisi berikut:

1. Extension anime terpasang, tetapi episode menghasilkan `No available videos`.
2. Video list kadang kosong dan kadang berisi server yang tetap gagal saat dipilih.
3. Source yang sama dapat memiliki hasil berbeda pada judul yang berbeda.
4. Satu source dapat gagal sementara source lain berhasil.
5. Aplikasi tidak memberi cara yang cukup jelas untuk mengganti source anime setelah playback gagal.
6. Title/season yang berasal dari AniList tidak selalu memiliki mapping yang sama pada semua source.

## 3. Visi

Menyediakan pengalaman streaming anime yang:

- fokus;
- ringan secara feature surface;
- dapat didiagnosis;
- memungkinkan user memilih source;
- tidak buntu ketika satu source gagal;
- tetap aman untuk rollback ke aplikasi lengkap.

## 4. Sasaran pengguna

Pengguna Android yang:

- ingin mencari anime dari metadata AniList;
- menggunakan extension pihak ketiga yang dipasang sendiri;
- membutuhkan lebih dari satu source anime;
- ingin memilih source ketika provider tertentu tidak memiliki judul/season;
- ingin memutar anime dengan internal player.

## 5. Scope MVP

### 5.1 Fitur wajib

#### Discovery

- Tambah repository extension anime.
- Muat daftar extension.
- Install, update, dan load extension.
- Tampilkan status extension dan API library.

#### Anime discovery

- Browse/search anime.
- Tampilkan detail anime.
- Tampilkan source yang tersedia.
- Simpan source pilihan per anime.
- Ganti source tanpa restart aplikasi.

#### Mapping dan episode

- Muat title/season dari source.
- Sediakan pencarian ulang atau `Wrong Title?` bila mapping otomatis gagal.
- Tampilkan episode source aktif.
- Bersihkan episode lama ketika source diganti.

#### Playback resolution

- Tampilkan hoster/server.
- Tampilkan video quality bila tersedia.
- Resolve video URL.
- Validasi URL sebelum playback.
- Tampilkan error stage yang tepat.
- Sediakan retry.
- Sediakan pilihan source lain jika semua kandidat source aktif gagal.

#### Player

- Play/pause.
- Seek.
- Volume.
- Subtitle track.
- Audio track.
- Playback speed dasar.
- Resume position minimal.
- Riwayat tontonan minimal.

### 5.2 Fitur yang tidak masuk MVP

- Manga dan reader.
- Local Anime/Local Source.
- Local video.
- Download episode.
- Backup/restore.
- Tracker eksternal.
- Torrent.
- Casting.
- External player.
- Statistik lanjutan.
- Sinkronisasi cloud.
- Fitur UI yang tidak diperlukan untuk menemukan dan menonton anime.

Fitur-fitur ini tetap dipertahankan untuk aplikasi lengkap dan akan dikembalikan bertahap setelah MVP streaming lulus UAT.

## 6. User journey target

### Journey A — Source berhasil

```text
Buka anime
→ pilih source
→ pilih season
→ pilih episode
→ pilih server/video
→ play
```

### Journey B — Source tidak menemukan title

```text
Buka anime
→ source gagal mapping
→ pilih Wrong Title?/Search source
→ pilih hasil yang benar
→ muat episode
```

### Journey C — Source punya episode tetapi resolver gagal

```text
Pilih episode
→ server tampil
→ semua server gagal
→ tampilkan alasan
→ user memilih source lain
→ muat ulang episode
```

### Journey D — URL invalid

```text
Resolver mengembalikan URL kosong/null
→ validator menolak URL
→ source/video ditandai gagal
→ user mendapat opsi retry/source lain
```

## 7. Persyaratan fungsional

### FR-01 Repository

Sistem harus dapat memuat repository anime yang valid dan menampilkan extension yang tersedia.

### FR-02 Extension

Sistem harus menampilkan package, versi extension, dan API library untuk diagnostics.

### FR-03 Source selection

Sistem harus mengizinkan user memilih source anime untuk satu anime.

### FR-04 Source persistence

Pilihan source harus disimpan per anime dan aman jika source sudah tidak tersedia.

### FR-05 Source switching

Saat source diganti, sistem harus membatalkan request source lama dan tidak boleh menampilkan callback stale.

### FR-06 Title mapping

Sistem harus menyediakan pemulihan ketika title AniList tidak cocok dengan source.

### FR-07 Episode loading

Sistem harus membedakan episode list kosong dari kegagalan jaringan, mapping, dan exception extension.

### FR-08 Video resolution

Sistem harus mendukung alur legacy dan hoster-based yang didukung oleh API extension KITSUNE.

### FR-09 URL validation

Sistem tidak boleh mengirim `null`, `undefined`, blank, malformed, atau unresolved placeholder ke player.

### FR-10 Server selection

Sistem harus mengizinkan user mengganti server/video dalam source aktif.

### FR-11 Cross-source fallback

Jika semua kandidat source aktif gagal, sistem harus menawarkan source lain yang tersedia.

### FR-12 Playback

Video valid harus dapat dimuat dan diputar melalui internal player.

### FR-13 Error recovery

User harus dapat retry resolver, mengganti server, atau mengganti source tanpa force close aplikasi.

### FR-14 Diagnostics

Error internal harus diberi kategori dan tidak boleh menulis token/URL lengkap ke log.

## 8. Persyaratan non-fungsional

### NFR-01 Resource

StreamingOnly harus diuji dengan batas realistis sekitar 4 GB RAM fisik pada perangkat target.

### NFR-02 Lifecycle

Source/episode/video operation harus aman terhadap:

- pergantian source cepat;
- pergantian episode cepat;
- rotasi bila didukung;
- background/foreground;
- cancellation;
- retry;
- duplicate request.

### NFR-03 Security

- Jangan mencatat credential, cookie, token, atau URL bertoken secara penuh.
- Redact query sensitif.
- Extension tetap diperlakukan sebagai kode pihak ketiga.

### NFR-04 Backward compatibility

Build Full dan branch `KITSUNE` tidak boleh berubah perilakunya hanya karena build flavor eksperimen.

### NFR-05 Measurability

Ukuran APK, waktu build, startup, dan hasil UAT harus dicatat sebelum klaim “lebih ringan”.

## 9. UX minimum

### Tampilan anime

- Judul.
- Poster.
- Season.
- Source aktif.
- Tombol ganti source.
- Tombol mapping ulang bila diperlukan.

### Tampilan episode

- Daftar episode source aktif.
- Status watched/resume minimal.
- Loading state.
- Error state yang menjelaskan tahap gagal.

### Tampilan playback source

- Daftar server/hoster.
- Status loading/resolving.
- Retry.
- Ganti source.
- Pesan ketika semua kandidat gagal.

Contoh pesan:

```text
Tidak ada video yang berhasil di-resolve dari source ini.
Coba server lain atau pilih source anime lain.
```

Hindari pesan tunggal tanpa konteks:

```text
No available videos
```

## 10. UAT plan

### Target device

```text
Device : Infinix SMART 9 HD
Model  : X6532C
Android: 14
RAM    : 4 GB fisik + extended memory
CPU    : ARMv8
Screen : 720 x 1600
```

### Test set

1. Spy x Family pada Samehadaku.
2. Spy x Family pada Oplovers.
3. Spy x Family pada source yang berhasil di Dantotsu.
4. Tensura Season 4 pada seluruh source tersedia.
5. Minimal satu anime yang servernya tampil tetapi resolver gagal.
6. Minimal satu anime yang berhasil di Dantotsu dan diuji di KITSUNE.
7. Network failure/timeout.
8. Retry setelah resolver gagal.
9. Ganti source ketika episode sedang loading.
10. Ganti server ketika resolver berjalan.

### Pass criteria

- Tidak ada crash.
- Source dapat diganti.
- Callback source lama tidak menimpa source baru.
- Episode list sesuai source aktif.
- URL invalid tidak masuk player.
- Video valid dapat diputar.
- Error dapat dipahami dan dipulihkan.
- UAT result tercatat per device, build, extension, source, episode, dan kondisi network.

### Status UAT

UAT dianggap:

- **PASS** jika seluruh skenario wajib lulus.
- **PARTIAL** jika playback berhasil tetapi source switching atau recovery gagal.
- **FAIL** jika app crash, source state stale, atau URL invalid dikirim ke player.

## 11. KPI dan bukti

- APK size per ABI sebelum/sesudah.
- Waktu build sebelum/sesudah.
- Startup time.
- Waktu dari episode dipilih sampai hoster tampil.
- Waktu dari server dipilih sampai resolver selesai.
- Persentase skenario playback pass.
- Jumlah crash saat source/episode switching.
- Jumlah kegagalan yang berhasil dipulihkan dengan source lain.

## 12. Risiko produk

- Tidak semua source memiliki title/season yang sama.
- Provider dapat berubah atau down tanpa perubahan app.
- Extension pihak ketiga dapat mengembalikan URL/token sementara.
- Source fallback dapat memperbesar waktu tunggu dan pemakaian network.
- Flavor dapat menyembunyikan fitur tetapi tidak otomatis mengurangi APK jika dependency masih dibutuhkan.

## 13. Rollout bertahap

1. Internal build `StreamingOnly`.
2. UAT pemilik project pada device target.
3. Perbaikan blocker.
4. Regression test.
5. UAT ulang.
6. Bandingkan artifact dengan Full.
7. Kembalikan fitur satu per satu.

Tidak ada release publik sebelum gate yang disepakati lulus.

## 14. Keputusan yang diperlukan

Mohon review dan approval untuk:

- penggunaan build flavor `StreamingOnly`;
- source selector sebagai fitur wajib MVP;
- title remapping sebagai fitur MVP atau fase kedua;
- fallback lintas-source manual sebagai fitur MVP;
- penonaktifan local source, download, manga, tracker, backup, torrent, dan external player pada UAT awal;
- device utama UAT: Infinix SMART 9 HD X6532C Android 14;
- kriteria PASS/PARTIAL/FAIL di atas.

## 15. Status dokumen

- **DESIGNED:** ya.
- **IMPLEMENTED:** belum.
- **LOCALLY VERIFIED:** belum ada perubahan kode.
- **CI VERIFIED:** belum.
- **DEVICE VERIFIED:** belum.
- **RELEASED:** tidak.
