# RFC-0001: StreamingOnly dengan Source Selection dan Cross-Source Fallback

- **Status:** DRAFT — menunggu approval pengguna
- **Tanggal:** 28 September 2026
- **Repository:** `muzape28-blip/aniyomi`
- **Branch baseline:** `KITSUNE`
- **Baseline commit:** `f81cc5c5d7522b45f837bc95a8e59233287e9007`
- **Target device UAT:** Infinix SMART 9 HD, model X6532C, Android 14
- **Pemilik keputusan:** pengguna/project owner

## 1. Ringkasan

RFC ini mengusulkan build flavor eksperimen `StreamingOnly` untuk memfokuskan aplikasi pada pencarian anime, pemilihan source, pemuatan episode, resolusi video, dan pemutaran streaming.

Perubahan dilakukan tanpa menghapus fitur dari branch `KITSUNE`. Fitur non-streaming dinonaktifkan pada flavor eksperimen agar dapat diuji dan di-rollback secara terpisah.

RFC ini juga mengusulkan source selection yang eksplisit, title/season mapping yang dapat dipulihkan, diagnostics resolver, validasi URL video, dan fallback antar-source.

## 2. Masalah yang melatarbelakangi

Keluhan yang perlu diisolasi:

1. Beberapa extension menampilkan daftar server kosong.
2. Beberapa extension menampilkan server, tetapi saat dipilih muncul `No available videos`.
3. Perilaku dapat berbeda antara KITSUNE dan Dantotsu walaupun memakai repository extension yang sama.
4. Source tertentu dapat menemukan `Spy x Family`, sementara source lain tidak.
5. Semua source dapat gagal menemukan `Tensura Season 4`, kemungkinan karena title/season belum tersedia atau mapping salah.
6. KITSUNE belum memberikan pemilihan source anime dan fallback lintas-source sejelas Dantotsu.
7. Error internal seperti hoster kosong, resolver gagal, URL invalid, dan player gagal dimampatkan menjadi pesan generik.

## 3. Tujuan

### 3.1 Tujuan utama

- Mengisolasi debugging pada alur streaming.
- Menyediakan source selector per anime.
- Memisahkan kegagalan title mapping, episode loading, hoster loading, video resolving, dan player loading.
- Mencegah URL invalid seperti `null`, `undefined`, atau blank diteruskan ke player.
- Memberikan fallback manual ke source lain setelah source aktif gagal.
- Membuat UAT dapat menguji pipeline end-to-end secara terukur.
- Menghasilkan build eksperimen yang dapat dibandingkan dengan build lengkap.

### 3.2 Tujuan sekunder

- Mengukur pengurangan ukuran APK dan kompleksitas build.
- Mengurangi noise dari manga, local source, download, tracker, backup, dan fitur non-playback.
- Mempertahankan jalur rollback ke fitur lengkap.

## 4. Non-goals

RFC ini tidak bertujuan untuk:

- Menjamin semua website/provider selalu tersedia.
- Memperbaiki extractor setiap extension pihak ketiga di dalam app core.
- Menambahkan host video baru ke extension tanpa bukti dari diagnostics.
- Mengganti mpv atau membangun ulang FFmpeg pada fase awal.
- Membuat aggregator otomatis yang memanggil semua source secara paralel pada fase pertama.
- Menghapus source code fitur lengkap dari branch `KITSUNE`.
- Mengklaim APK lebih kecil sebelum artifact benar-benar diukur.
- Menjamin kompatibilitas dengan semua device Android.

## 5. Prinsip desain

1. **Branch utama aman:** eksperimen menggunakan branch terpisah.
2. **Flavor, bukan hard delete:** fitur lengkap tetap ada dan dapat dipulihkan.
3. **Manual fallback dahulu:** user memilih source berikutnya; auto-fallback ditunda.
4. **Satu pemilik operasi:** setiap request source/episode/video memiliki identitas dan cancellation yang jelas.
5. **No stale callback:** callback membawa source dan operation identity.
6. **Error tidak disamakan:** failure type dipertahankan sampai UI.
7. **URL harus tervalidasi:** URL kosong, placeholder, dan malformed tidak boleh masuk player.
8. **Bukti sebelum klaim:** semua status memakai level verifikasi yang jelas.

## 6. Arsitektur target

### 6.1 Alur target

```text
AniList anime
  -> daftar source yang tersedia
  -> user memilih source
  -> title/season mapping
  -> daftar episode source tersebut
  -> user memilih episode
  -> daftar hoster/server
  -> daftar video
  -> resolveVideo/getVideoUrl
  -> validasi URL
  -> internal player
  -> gagal? tawarkan source lain
```

### 6.2 State yang harus dipisahkan

- `Anime metadata state`
- `Selected source state`
- `Title mapping state`
- `Episode list state`
- `Selected episode state`
- `Hosters/videos state`
- `Resolver operation state`
- `Player state`

Tidak boleh ada satu state generik yang mewakili semua tahap tersebut.

### 6.3 Operation identity

Setiap operasi streaming minimal membawa:

```text
operationId
animeId
sourceId
sourceAnimeUrl
episodeId/episodeUrl
hosterId bila ada
```

Callback yang tidak lagi sesuai dengan source atau operation aktif harus diabaikan.

## 7. Perubahan yang diusulkan

### 7.1 Build flavor

Tambahkan flavor `StreamingOnly` dengan scope:

**Tetap aktif:**

- metadata AniList/anime;
- anime source repository;
- extension install/update;
- search;
- source selector;
- title mapping;
- episode list;
- hoster/server list;
- video resolver;
- internal player;
- subtitle/audio track;
- playback history minimum.

**Dinonaktifkan sementara:**

- manga dan reader;
- Local Anime/Local Source;
- local video playback;
- download;
- backup/restore;
- tracker;
- torrent;
- cast/external player bila tidak diperlukan untuk gate awal;
- statistik dan fitur non-playback.

Implementasi awal boleh berupa source/UI registration gating. Penghapusan dependency hanya dilakukan setelah dependency graph diaudit.

### 7.2 Source selector

Source selector harus:

- menampilkan source yang tersedia untuk anime;
- menyimpan source pilihan per anime;
- membatalkan operasi source sebelumnya saat source berganti;
- memuat ulang episode dari source baru;
- membersihkan episode/hoster state dari source lama;
- menolak callback stale;
- menyediakan jalur pencarian atau `Wrong Title?` bila title mapping gagal.

### 7.3 Error taxonomy

Minimal gunakan kategori:

```text
SOURCE_TITLE_NOT_FOUND
EPISODE_LIST_EMPTY
HOSTER_LIST_EMPTY
VIDEO_LIST_EMPTY
VIDEO_RESOLVE_FAILED
INVALID_VIDEO_URL
PLAYER_LOAD_FAILED
CANCELLED
NETWORK_OR_ACCESS_FAILURE
```

UI boleh menyederhanakan bahasa untuk pengguna, tetapi diagnostics harus mempertahankan kategori internal.

### 7.4 URL validator

Buat validator tunggal yang menolak:

- `null` reference;
- string kosong/whitespace;
- literal `"null"`;
- literal `"undefined"`;
- URL malformed;
- URL placeholder;
- URL embed yang belum di-resolve bila source menandainya sebagai unresolved.

Validator harus digunakan pada pemilihan video, resolver result, fallback, dan sebelum `MPVLib.loadfile`.

### 7.5 Diagnostics

Diagnostics tidak boleh mencatat token atau URL penuh. Data yang boleh dicatat:

- extension package dan version;
- extension lib version;
- source name/id;
- episode identifier yang sudah direduksi;
- operation stage;
- jumlah hoster/video;
- status URL secara kategori;
- exception class dan pesan yang sudah direduksi;
- HTTP status tanpa credential/token.

### 7.6 Fallback

Fase awal hanya menyediakan fallback manual:

```text
Source aktif gagal.
Coba source lain?
[Pilih source]
```

Auto-fallback baru dipertimbangkan setelah manual fallback stabil, dengan batas maksimal dan cancellation yang jelas.

## 8. Tahapan implementasi

### Tahap A — Baseline

- Pastikan workspace bersih.
- Simpan commit baseline.
- Build varian yang tersedia.
- Catat ukuran APK/AAB, ABI, native libraries, dan waktu build.

### Tahap B — Reproduction matrix

Bandingkan KITSUNE dan Dantotsu menggunakan extension build yang sama untuk:

- Spy x Family pada Samehadaku, Oplovers, dan source yang berhasil.
- Tensura Season 4 pada seluruh source yang tersedia.
- Episode yang terlihat tetapi menghasilkan `No available videos`.

### Tahap C — Guard dan diagnostics

- Tambah unit test validator.
- Tambah test selection/fallback.
- Tambah instrumentation untuk operation identity/stale callback jika harness tersedia.
- Implementasikan error taxonomy.

### Tahap D — StreamingOnly flavor

- Tambahkan flavor.
- Gate feature non-streaming.
- Pastikan player, source API, extension manager, dan source selector tetap masuk.

### Tahap E — Source selector dan title mapping

- Implementasikan source selector minimal.
- Persist source selection.
- Tambahkan jalur title remapping.
- Tambahkan cancellation/stale callback guard.

### Tahap F — Manual cross-source fallback

- Setelah source aktif gagal, tawarkan source lain.
- Pastikan kegagalan source lama tidak merusak state source baru.

### Tahap G — Build dan UAT

- Build `StreamingOnly`.
- Instal pada Infinix X6532C Android 14.
- Jalankan acceptance matrix.
- Catat log, artifact, ukuran, kondisi network, dan hasil per skenario.

## 9. Acceptance criteria teknis

- URL placeholder tidak pernah dikirim ke MPV.
- Resolver failure tidak disamarkan sebagai video valid.
- Callback source lama tidak mengubah UI source baru.
- Source dapat diganti tanpa restart app.
- Episode state berubah sesuai source aktif.
- Hoster/server state tidak terbawa dari source sebelumnya.
- User dapat mencoba source lain setelah semua hoster source aktif gagal.
- Error diagnostics dapat membedakan minimal 7 kategori kegagalan.
- Test guard gagal ketika validator atau stale callback protection sengaja dirusak.
- Build flavor dapat dibuat tanpa mengubah perilaku branch `KITSUNE`.

## 10. Acceptance criteria UAT

Target: Infinix SMART 9 HD X6532C, Android 14, layar 720x1600, RAM fisik sekitar 4 GB.

- Repository extension dapat ditambahkan.
- Extension dapat di-install dan dimuat.
- Search berhasil.
- Source selector tampil.
- Source dapat diganti.
- Spy x Family dapat diuji pada minimal tiga source.
- Tensura Season 4 dapat dibedakan antara title mapping gagal dan content unavailable.
- Episode list source aktif tampil atau error yang tepat ditampilkan.
- Hoster/video list tampil bila tersedia.
- Video yang valid dapat diputar.
- Subtitle/audio track berfungsi bila tersedia.
- Server dapat diganti.
- Source lain dapat dicoba setelah resolver gagal.
- Tidak ada crash saat mengganti source/episode cepat.

Satu device hanya membuktikan `PLATFORM/DEVICE VERIFIED` untuk device tersebut, bukan semua Android.

## 11. Risiko dan mitigasi

| Risiko | Dampak | Mitigasi |
|---|---|---|
| Source selector menambah kompleksitas state | Stale episode/video | Operation identity dan cancellation |
| Extension memakai API berbeda | Video kosong atau crash | Matrix lib 14/16/17 dan compatibility tests |
| Semua source provider down | UAT false negative | Simpan bukti availability dan uji source pembanding |
| APK tidak jauh lebih kecil | Target ukuran tidak tercapai | Ukur dependency graph sebelum klaim |
| Fitur lama rusak | Regression pada Full | Flavor dan branch terpisah |
| Auto-fallback membebani network | Slow/blocked provider | Manual fallback lebih dahulu |
| Logging membocorkan token | Risiko keamanan | Redaction dan URL fingerprint saja |

## 12. Rollback

Rollback utama:

- hentikan branch eksperimen;
- gunakan branch `KITSUNE` pada commit baseline;
- hapus/disable flavor `StreamingOnly` tanpa menghapus source lengkap;
- revert commit per unit, bukan reset perubahan yang sudah bercampur.

Tidak ada data migration atau perubahan database yang boleh masuk sebelum disetujui terpisah.

## 13. Keputusan yang menunggu approval

1. Apakah flavor `StreamingOnly` disetujui dibanding hard delete?
2. Apakah source selector wajib masuk MVP streaming?
3. Apakah `Wrong Title?`/title remapping masuk MVP atau fase berikutnya?
4. Apakah external player/casting dinonaktifkan pada UAT awal?
5. Apakah UAT hanya menggunakan Infinix X6532C atau perlu device kedua?
6. Berapa batas minimal source fallback manual yang wajib didukung?

## 14. Status verifikasi

- **DESIGNED:** proposal ini.
- **IMPLEMENTED:** belum.
- **LOCALLY VERIFIED:** belum ada patch.
- **CI VERIFIED:** belum.
- **PLATFORM/DEVICE VERIFIED:** belum.
- **RELEASED:** tidak.
