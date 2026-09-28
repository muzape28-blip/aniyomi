# RFC-0002: Streaming Breadcrumb dan In-App Diagnostics

- **Status:** APPROVED FOR IMPLEMENTATION — implementasi belum dimulai
- **Tanggal:** 28 September 2026
- **Repository:** `muzape28-blip/aniyomi`
- **Branch kerja:** `agent/streaming-breadcrumb-diagnostics`
- **Target device UAT:** Infinix SMART 9 HD X6532C, Android 14
- **Aturan verifikasi:** tidak melakukan compile/build lokal; verifikasi build dilakukan melalui GitHub Actions dan UAT artifact yang dihasilkan CI.

## 1. Tujuan

Menambahkan diagnostik persisten yang dapat dibaca langsung dari KITSUNE tanpa PC atau `adb logcat`. Diagnostik harus mempertahankan bukti terakhir dari alur streaming setelah force close, JVM crash, native failure, atau proses dibunuh OS.

Implementasi ini diadaptasi secara konseptual dari Breadcrumb dan Diagnostics pada ZCODE, bukan disalin mentah-mentah.

## 2. Batasan jujur

Breadcrumb dapat menyimpan event terakhir yang sempat berhasil di-flush, tetapi tidak dapat menangkap semua native crash, SIGSEGV, atau pembunuhan proses karena OOM. CrashReporter hanya menjamin penangkapan Throwable JVM yang melewati uncaught exception handler.

## 3. Kontrak Breadcrumb

- Diinisialisasi sedini mungkin pada `Application.onCreate`, sebelum operasi extension, repository, player, atau streaming.
- Menulis event secara sinkron dan melakukan flush segera.
- Tidak boleh melempar exception ke caller atau menjadi penyebab crash baru.
- Memakai penyimpanan internal aplikasi tanpa permission storage tambahan.
- Memakai bounded rotation minimal satu arsip.
- Menyediakan `tail()` untuk crash report dan `dumpFull()` untuk copy/share/export.
- Menulis hanya event transisi penting; callback berfrekuensi tinggi seperti posisi playback tidak ditulis satu per satu.
- Clear log memerlukan tindakan eksplisit user.

## 4. Retensi awal

```text
<filesDir>/logs/diagnostics/breadcrumb.log       maksimum 512 KB
<filesDir>/logs/diagnostics/breadcrumb.1.log     satu arsip
<filesDir>/logs/diagnostics/crash-*.txt          maksimum 5 laporan terbaru
<filesDir>/logs/streaming/session-*.log          maksimal 20 atau 50 sesi terbaru
```

Nilai akhir boleh disesuaikan setelah melihat batas storage dan performa pada device target, tetapi harus tetap bounded.

## 5. Format event

Setiap event harus memiliki prefix stabil yang mudah difilter:

```text
timestamp | EVENT_NAME | operationId=... | safe-context=...
```

Context yang diperbolehkan:

- `operationId`;
- source id/name yang sudah direduksi bila perlu;
- episode number atau identifier yang aman;
- stage;
- result count;
- elapsed time;
- HTTP status;
- exception class yang sudah direduksi;
- reason code.

Jangan mencatat penuh:

- credential;
- cookie;
- Authorization header;
- API key;
- token;
- unrestricted request header;
- URL video atau URL query yang mengandung token.

Untuk URL, simpan host, scheme, panjang, status validasi, dan hash pendek bila diperlukan.

## 6. Taxonomy streaming

Minimal event yang harus tersedia:

```text
STREAM_SOURCE_SELECTED
STREAM_TITLE_MAP_BEGIN
STREAM_TITLE_MAP_OK
STREAM_TITLE_MAP_FAIL
STREAM_EPISODE_BEGIN
STREAM_EPISODE_OK
STREAM_EPISODE_EMPTY
STREAM_EPISODE_FAIL
STREAM_HOSTER_BEGIN
STREAM_HOSTER_OK
STREAM_HOSTER_EMPTY
STREAM_HOSTER_FAIL
STREAM_VIDEO_BEGIN
STREAM_VIDEO_OK
STREAM_VIDEO_EMPTY
STREAM_RESOLVE_BEGIN
STREAM_RESOLVE_OK
STREAM_RESOLVE_FAIL
STREAM_URL_REJECTED
STREAM_PLAYER_BEGIN
STREAM_PLAYER_OK
STREAM_PLAYER_FAIL
STREAM_CANCELLED
STREAM_FALLBACK_OFFERED
STREAM_FALLBACK_SELECTED
STREAM_FALLBACK_FAIL
```

Operation yang berubah source, episode, atau resolver harus membawa operation identity agar callback stale dapat dibedakan dari operasi aktif.

## 7. UI Diagnostics MVP

Screen Diagnostics harus menyediakan:

- tab atau filter `SEMUA`, `STREAMING`, `EXTENSION`, `RESOLVER`, `PLAYER`, dan `CRASH`;
- pemuatan file melalui dispatcher I/O;
- render window terbatas agar Compose tidak ANR;
- kontrol untuk memuat baris lama;
- copy seluruh log dari disk, bukan hanya baris yang sedang tampil;
- share/export seluruh log melalui mekanisme Android yang sesuai;
- tampilan crash Java terakhir bila tersedia;
- penjelasan bahwa tidak adanya crash Java tidak menyingkirkan native crash/OOM;
- clear log dengan konfirmasi atau tindakan eksplisit.

## 8. Integrasi CrashReporter

CrashReporter dipasang setelah Breadcrumb siap. Laporan JVM menyimpan versi aplikasi, Android/API, ABI, thread, stack trace terbatas, dan tail Breadcrumb. Handler harus tetap meneruskan Throwable ke handler bawaan sistem setelah laporan ditulis.

CrashReporter tidak boleh mengklaim dapat menangkap crash native MPV atau OOM.

## 9. Urutan implementasi

1. Tambahkan kontrak dan unit tests untuk redaksi, URL detail, rotasi, clear, dan operasi dump.
2. Tambahkan Breadcrumb infrastructure dan startup initialization.
3. Tambahkan CrashReporter integration tanpa mengubah perilaku default crash sistem.
4. Tambahkan Diagnostics screen/navigation.
5. Instrumentasikan source selection, title mapping, episode, hoster, resolver, URL validation, player, cancellation, dan fallback.
6. Tambahkan session log terbatas bila Breadcrumb ringkas belum cukup untuk diagnosis.
7. Commit dan push perubahan ke GitHub.
8. Biarkan GitHub Actions melakukan compile/test/build; tidak menjalankan compile lokal.
9. Instal artifact CI ke Infinix X6532C dan lakukan UAT.

## 10. Acceptance criteria

- Event streaming penting tetap tersedia setelah force close jika event telah berhasil di-flush.
- Logging failure tidak menggagalkan startup, source loading, resolver, atau player.
- Rotasi mempertahankan active log dan minimal satu archive.
- Full copy/share memuat archive dan active log yang tersedia.
- URL/token/cookie sensitif tidak muncul dalam bentuk penuh.
- Diagnostics membedakan source, episode, hoster, resolver, URL validation, dan player failure.
- Stale operation dapat dibedakan dari operasi aktif melalui `operationId`.
- Clear log tidak terjadi tanpa intent user.
- Tidak ada klaim bahwa native crash atau OOM pasti tertangkap.
- Build verification dilakukan di GitHub Actions, bukan di workspace lokal.
