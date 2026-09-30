# Calculator Vault V1

Aplikasi kalkulator Android yang berfungsi normal, dengan vault tersembunyi
yang hanya bisa dibuka lewat kombinasi PIN rahasia yang diketik lewat
kalkulator itu sendiri. Ditulis dalam Kotlin, native Android (bukan hybrid),
tanpa dependensi internet/iklan/analytics.

Status: **V1 sesuai roadmap di brief** (kalkulator normal, PIN rahasia,
vault lokal dasar berupa catatan terenkripsi, auto-lock, tanpa internet,
tanpa iklan). V2 (enkripsi foto/video, import/export, decoy vault) dan V3
(Hide Apps, optimasi POCO/HyperOS, pengujian keamanan) **belum
diimplementasikan** — lihat bagian "Batasan & yang belum diuji" di bawah.

---

## 1. Cara membuka & build project

### Prasyarat
- Android Studio (Koala/2024.x atau lebih baru direkomendasikan)
- JDK 17 (biasanya sudah dibundel Android Studio)
- Koneksi internet **hanya saat proses build** (Gradle perlu mengunduh
  dependency dari Google/Maven sekali di awal) — aplikasi hasil build itu
  sendiri tidak memerlukan internet sama sekali saat berjalan.

### Langkah
1. Buka Android Studio → **Open** → pilih folder `CalculatorVaultV1/` ini.
2. Android Studio akan menawarkan membuat Gradle Wrapper (`gradlew`) secara
   otomatis jika belum ada — terima saja ("Sync Now"). Proyek ini sudah
   menyertakan `gradle/wrapper/gradle-wrapper.properties` yang menunjuk ke
   Gradle 8.7; hanya `gradle-wrapper.jar` biner yang belum disertakan
   (dihasilkan otomatis oleh Android Studio saat sync, atau jalankan
   `gradle wrapper` sekali jika Anda punya Gradle terinstal secara lokal).
3. Tunggu Gradle sync selesai (mengunduh AGP, Kotlin plugin, dan library
   yang tercantum di `app/build.gradle.kts`).
4. Sambungkan perangkat POCO (Android 8.0/API 26 ke atas) via USB dengan
   USB debugging aktif, atau gunakan emulator.
5. **Run ▶** untuk install & jalankan langsung, atau **Build → Generate
   Signed App Bundle / APK** untuk menghasilkan APK yang bisa dibagikan.

### Build APK release dari command line (setelah wrapper jar ada)
```bash
./gradlew assembleRelease
# Hasil: app/build/outputs/apk/release/app-release-unsigned.apk
```
APK release perlu ditandatangani (signing) sebelum diinstal di luar mode
developer — atur signing config sendiri di Android Studio
(**Build → Generate Signed Bundle/APK**) sebelum distribusi.

> **Catatan jujur:** proyek ini disusun dan diperiksa baris demi baris di
> lingkungan tanpa akses internet/Gradle, sehingga **belum pernah benar-benar
> dikompilasi di sini**. Strukturnya mengikuti konvensi Android Gradle
> Kotlin DSL standar dan sudah diverifikasi konsistensinya (nama kelas,
> resource ID, package, binding), tapi Anda tetap perlu menjalankan Gradle
> sync pertama kali di mesin Anda untuk menangkap kemungkinan typo kecil.

Ganti `applicationId` di `app/build.gradle.kts` sebelum rilis produksi.

---

## 2. Struktur proyek (pemisahan modul sesuai brief)

```
app/src/main/java/com/example/calculatorvault/
├── MainActivity.kt            # UI kalkulator + titik deteksi PIN rahasia
├── CalculatorEngine.kt        # Logika kalkulator murni (tanpa tahu soal vault)
├── VaultApplication.kt        # Pemicu auto-lock tingkat aplikasi
├── security/
│   ├── PinManager.kt          # Hash PIN (PBKDF2) + Keystore + anti brute-force
│   └── CryptoManager.kt       # Enkripsi file per-item (AES256-GCM via EncryptedFile)
├── vault/
│   ├── VaultActivity.kt       # Layar vault (FLAG_SECURE, cek auto-lock)
│   ├── VaultRepository.kt     # CRUD catatan terenkripsi
│   └── VaultNoteAdapter.kt    # RecyclerView adapter
├── ui/
│   └── ChangePinActivity.kt   # Ganti PIN (butuh PIN lama dulu)
└── util/
    ├── VaultSession.kt        # Status lock in-memory (tidak pernah ditulis ke disk)
    └── HideAppsManager.kt     # STUB desain V3 — lihat bagian batasan di bawah
```

---

## 3. Cara kerja PIN rahasia

- Ketik PIN Anda (4-8 digit) **sebagai angka biasa, tanpa operator apa pun**,
  lalu tekan `=`. Jika cocok, layar vault terbuka.
- Jika Anda mengetik ekspresi apa pun yang memakai `+ − × ÷ %`, atau angka
  yang tidak cocok dengan PIN, aplikasi berperilaku 100% seperti kalkulator
  biasa — tidak ada tanda, toast, getar, atau log apa pun yang membocorkan
  bahwa ada pengecekan PIN terjadi.
- PIN pertama kali dibuat lewat dialog setup saat aplikasi pertama kali
  dijalankan (belum ada mekanisme rahasia untuk disembunyikan pada titik
  ini, karena belum ada PIN untuk disembunyikan).
- Ganti PIN: buka vault → menu (titik tiga / overflow) → "Ubah PIN" →
  masukkan PIN lama + PIN baru.

**Batasan yang jujur harus disebutkan:** karena deteksi PIN memakai heuristik
"angka murni tanpa operator", seseorang yang membongkar APK (decompile) akan
bisa menemukan logika ini di `MainActivity.onEqualsPressed()`. Ini
**bukan steganografi sempurna** — ini adalah pola "hidden-in-plain-sight"
yang cukup untuk menyamarkan dari pengguna kasual/pengintaian visual, bukan
dari analisis forensik/reverse-engineering serius.

---

## 4. Apa yang sudah benar-benar aman, dan apa yang masih prototipe

### Sudah menggunakan mekanisme keamanan Android yang nyata
- **PIN tidak pernah disimpan sebagai plaintext.** Yang disimpan hanya salt
  acak + hash `PBKDF2WithHmacSHA256` (120.000 iterasi), di dalam
  `EncryptedSharedPreferences` (Jetpack Security), yang kunci enkripsinya
  sendiri dibuat & disimpan di **Android Keystore** (`MasterKey`,
  AES256-GCM) — kunci itu tidak pernah terlihat dalam bentuk yang bisa
  dibaca aplikasi.
- **Setiap item vault dienkripsi per-file** dengan `EncryptedFile`
  (AES256-GCM-HKDF, 4KB chunk), disimpan hanya di
  `context.filesDir/vault_store/` (penyimpanan internal privat aplikasi),
  **tidak pernah** ditulis ke penyimpanan eksternal/publik, dan tidak ada
  salinan sementara plaintext yang ditinggalkan di disk.
- **Anti brute-force**: setelah 5 kali percobaan PIN salah, ada lockout
  dengan exponential backoff (30 detik → naik ganda tiap kegagalan
  berikutnya, dibatasi maksimum 30 menit).
- **Auto-lock**: memakai `ProcessLifecycleOwner` (level aplikasi, bukan
  cuma satu Activity) — begitu seluruh aplikasi masuk background
  (`onStop`), status "unlocked" langsung dihapus dari memori. Status ini
  **tidak pernah ditulis ke disk**, jadi proses yang di-kill oleh sistem
  selalu kembali ke kondisi terkunci.
- **`FLAG_SECURE`** dipasang di `VaultActivity` dan `ChangePinActivity`
  sebelum `setContentView`, mencegah screenshot dan mencegah konten vault
  muncul di thumbnail recent-apps/task switcher.
- **`android:allowBackup="false"`** + `dataExtractionRules.xml` mencegah
  data vault ikut ter-backup ke Google/cloud atau transfer perangkat.
- **Tidak ada permission `INTERNET`** di manifest sama sekali — build ini
  secara fisik tidak bisa membuka koneksi jaringan, terlepas dari kode apa
  pun di dalamnya. Tidak ada SDK iklan, analytics, atau tracker pihak
  ketiga di `build.gradle.kts`.
- Nilai PIN yang diketik tidak pernah dikirim ke `Log.*` di mana pun dalam
  kode ini.

### Masih level prototipe / perlu kerja lanjutan sebelum dianggap "production-grade"
- **Belum ada pengujian keamanan independen** (belum di-audit, belum
  di-fuzz, belum diuji di perangkat fisik). Klaim di atas adalah klaim
  desain berdasarkan API resmi Android — bukan hasil pentest.
- **Model ancaman terbatas**: ini melindungi dari "seseorang mengambil HP
  Anda dan mengintip", bukan dari penyerang dengan akses fisik + root +
  waktu tak terbatas. Di perangkat **rooted/bootloader unlocked**,
  proteksi Keystore bisa melemah tergantung apakah perangkat punya
  StrongBox/TEE, dan aplikasi ini tidak mendeteksi status root.
- **PIN 4-8 digit tetap PIN numerik** — kuat di sisi hash, tapi ruang
  kemungkinannya kecil (maks 10^8). Rate-limiting adalah lapisan
  pertahanan utama, bukan panjang PIN.
- **Heuristik deteksi PIN via kalkulator** (dijelaskan di bagian 3) adalah
  "security through obscurity" tambahan di atas enkripsi asli — jangan
  dianggap sebagai lapisan keamanan kriptografis.
- Vault V1 hanya berisi **catatan teks**, belum foto/video (itu scope V2).
- **`HideAppsManager`** hanyalah kerangka desain (lihat komentar di
  file-nya) — **tidak diaktifkan di UI V1**, dan sengaja tidak
  mengklaim bisa menyembunyikan aplikasi lain, karena secara teknis
  Android tidak mengizinkan aplikasi biasa melakukan itu (lihat bagian 5).

---

## 5. Modul Hide Apps — batasan yang harus dipahami sebelum melanjutkan ke V3

`util/HideAppsManager.kt` berisi dokumentasi rinci, ringkasnya:

- Aplikasi biasa (non-root, non-Device-Owner) **hanya bisa** menyalakan/
  mematikan ikon launcher milik **aplikasi itu sendiri**
  (`PackageManager.setComponentEnabledSetting` pada `<activity-alias>`
  miliknya sendiri).
- Aplikasi biasa **tidak bisa** menyembunyikan atau menghapus aplikasi lain
  dari launcher, dari Settings → Apps, atau dari daftar aplikasi terinstal
  yang dilihat Android — ini berlaku di semua Android termasuk MIUI/HyperOS
  di perangkat POCO.
- Satu-satunya cara resmi untuk membatasi visibilitas aplikasi lain adalah
  lewat **Device Owner / MDM (enterprise provisioning)**, yang memerlukan
  persetujuan eksplisit pengguna saat setup perangkat — ini scope besar,
  di luar V1-V2, dan baru relevan didesain lebih lanjut di V3 jika memang
  dibutuhkan.
- Proyek ini **tidak dan tidak akan** memakai root, penyalahgunaan
  Accessibility Service, exploit, atau API tersembunyi OEM untuk mencapai
  "hide app" — sesuai batasan yang diminta di brief.

---

## 6. Permission yang diminta

**Tidak ada satu pun runtime permission berbahaya yang diminta.** Manifest
tidak mendeklarasikan permission apa pun (cek `AndroidManifest.xml`) —
tidak ada `INTERNET`, tidak ada storage, tidak ada kamera (kamera baru akan
relevan di V2 untuk foto/video vault, dan saat itu tiba harus diminta
sebagai runtime permission dengan penjelasan jelas ke pengguna).

---

## 7. Rencana V2 & V3 (belum diimplementasikan di sini)

- **V2**: enkripsi file yang lebih lengkap untuk foto/video (perlu
  `EncryptedFile` + `MediaStore`/`Scoped Storage` yang benar, kompres/thumbnail
  aman), import/export terenkripsi antar-perangkat, decoy vault (PIN kedua
  yang membuka vault "umpan" berisi data palsu).
- **V3**: implementasi Hide Apps sejauh benar-benar diizinkan API resmi
  Android (dengan batasan seperti di bagian 5 didokumentasikan ke pengguna
  di dalam UI, bukan disembunyikan), optimasi untuk quirks MIUI/HyperOS
  (mis. autostart battery optimization yang bisa mematikan proses
  background), dan pengujian keamanan formal (static analysis, pentest,
  uji recovery saat PIN lupa — catatan: saat ini **tidak ada mekanisme
  recovery PIN**, PIN yang lupa = data tidak bisa dibuka lagi, sesuai
  desain zero-knowledge; ini perlu didiskusikan dengan pengguna sebagai
  trade-off yang disengaja).
