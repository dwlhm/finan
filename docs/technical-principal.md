# Technical Principles

## 1. Performance is part of the product

Kecepatan bukan sekadar urusan teknis, tapi bagian dari value utama aplikasi.

Karena produk ini berfokus pada pencatatan transaksi cepat, maka semua keputusan teknis harus menjaga:

* startup time
* input responsiveness
* save latency
* app size
* memory usage
* battery usage
* simplicity

Kalau sebuah keputusan teknis membuat aplikasi lebih berat tanpa manfaat langsung ke core experience, keputusan itu harus ditolak atau ditunda.

Prinsipnya:

> Aplikasi yang lambat berarti gagal secara produk, bukan hanya gagal secara teknis.

---

## 2. Critical path must stay clean

Critical path aplikasi adalah:

```txt
Open App
→ Input Amount
→ Choose Category
→ Save
```

Jalur ini harus bebas dari pekerjaan berat.

Di critical path tidak boleh ada:

* network request blocking
* load report besar
* query agregasi berat
* sync blocking
* initialization SDK berat
* image loading besar
* animasi kompleks
* dependency initialization yang tidak perlu

Saat aplikasi dibuka, sistem cukup melakukan hal minimum agar user bisa mencatat transaksi.

Prinsipnya:

> Semua yang tidak dibutuhkan untuk mencatat transaksi sekarang tidak boleh menghalangi pencatatan.

---

## 3. Minimal runtime by default

Stack teknis harus dipilih berdasarkan beban runtime, bukan hanya kenyamanan development.

Untuk Android, baseline yang sesuai:

```txt
Language: Java
UI: Android Views
Screen model: Activity
Storage: SQLite / SharedPreferences
Architecture: simple manual structure
```

Alasannya:

* tidak membawa Kotlin runtime
* tidak membawa Compose runtime
* tidak perlu UI framework deklaratif tambahan
* lebih dekat ke primitive bawaan Android
* ukuran aplikasi lebih mudah dikontrol
* startup behavior lebih eksplisit
* dependency lebih sedikit

Ini bukan berarti Kotlin atau Compose buruk. Tapi untuk aplikasi yang prinsip utamanya **cepat, ringan, dan minim bloat**, Java + Android Views lebih sesuai sebagai baseline awal.

Prinsipnya:

> Pilih stack yang cukup untuk kebutuhan produk, bukan stack yang paling modern.

---

## 4. No dependency by default

Dependency eksternal tidak boleh dianggap default.

Sikap awal terhadap library adalah:

> Jangan tambahkan dependency kecuali ada alasan kuat.

Setiap dependency punya biaya:

* ukuran APK/AAB bertambah
* transitive dependency ikut masuk
* startup bisa lebih berat
* method count bertambah
* risiko bug bertambah
* upgrade menjadi beban maintenance
* behavior runtime lebih sulit dikontrol

Library hanya boleh masuk kalau manfaatnya lebih besar daripada beban teknisnya.

Prinsipnya:

> Dependency adalah liability sampai terbukti memberi value yang sebanding.

---

## 5. Native first, library second

Sebelum memakai library, gunakan kemampuan platform bawaan terlebih dahulu.

Contoh:

| Kebutuhan            | Prioritas awal                 |
| -------------------- | ------------------------------ |
| UI sederhana         | Android Views                  |
| Styling              | XML style/drawable             |
| Local setting        | SharedPreferences              |
| Database lokal       | SQLite                         |
| Date/time default    | Java/platform API              |
| Navigation sederhana | Activity/manual view switching |
| List sederhana       | native/list ringan dulu        |

Library boleh dipakai jika implementasi manual menjadi terlalu mahal, rawan bug, atau justru membuat kode lebih sulit dirawat.

Prinsipnya:

> Gunakan platform dulu. Tambahkan library hanya saat platform tidak cukup.

---

## 6. UI library must justify its weight

Library UI boleh digunakan, tapi syaratnya ketat.

Library UI masuk akal jika:

* banyak komponennya benar-benar dipakai
* mengurangi banyak kode UI custom
* konsisten dengan arah visual aplikasi
* tidak membebani startup
* ukuran akhir setelah shrinking masih sebanding
* tidak membawa transitive dependency besar
* tidak mengunci aplikasi ke arsitektur berat

Library UI tidak layak jika hanya dipakai untuk:

* button
* card
* chip sederhana
* toast custom
* dialog kecil
* icon kecil
* efek visual minor

Prinsipnya:

> Jangan membawa satu library besar untuk menyelesaikan satu masalah kecil.

---

## 7. Measure before accepting bloat

Setiap dependency atau fitur teknis harus bisa diukur dampaknya.

Yang perlu dicek:

* ukuran APK/AAB sebelum dan sesudah
* cold start sebelum dan sesudah
* jumlah dependency tambahan
* resource yang ikut terbawa
* apakah ada auto-initialization
* apakah ada service/background task
* apakah ada reflection atau annotation processing berat
* apakah R8/resource shrink bisa mengeliminasi bagian tidak terpakai

Jangan hanya menilai dari “library ini populer” atau “developer experience-nya enak”.

Prinsipnya:

> Yang tidak diukur akan membesar tanpa terasa.

---

## 8. Local-first save

Pencatatan transaksi harus disimpan lokal terlebih dahulu.

Flow yang benar:

```txt
Tap Save
→ Validate minimal input
→ Insert into local database
→ Update UI instantly
→ Optional sync later
```

Flow yang harus dihindari:

```txt
Tap Save
→ Send request to server
→ Wait response
→ Saved
```

Backend, sync, dan cloud backup boleh ada nanti, tapi tidak boleh menjadi syarat agar transaksi tercatat.

Prinsipnya:

> User harus bisa mencatat transaksi walaupun offline.

---

## 9. Lazy load non-essential features

Tidak semua data perlu dimuat saat aplikasi dibuka.

Yang perlu siap saat launch:

* input nominal
* default wallet
* kategori cepat
* tombol save
* transaksi terakhir secukupnya

Yang bisa ditunda:

* report bulanan
* chart
* agregasi kategori
* insight
* backup status
* sync status
* export/import
* analytics initialization
* remote config

Prinsipnya:

> Load yang dibutuhkan sekarang. Tunda sisanya.

---

## 10. Simple architecture over over-engineering

Aplikasi ini tidak boleh menjadi berat karena arsitektur yang terlalu ambisius.

Hindari di awal:

* Clean Architecture berlapis terlalu banyak
* dependency injection framework
* event bus
* reactive stream kompleks
* state management berat
* repository abstraction berlebihan
* use case untuk operasi trivial
* modularisasi prematur

Struktur boleh sederhana:

```txt
ui
data
domain
service
```

Yang penting:

* UI tidak langsung berantakan
* akses database terpusat
* logic saldo tidak tersebar
* model transaksi jelas
* kode mudah diganti kalau aplikasi tumbuh

Prinsipnya:

> Pisahkan tanggung jawab, tapi jangan membuat abstraksi sebelum masalahnya ada.

---

## 11. Manual wiring before DI framework

Dependency injection framework tidak perlu masuk di awal.

Untuk aplikasi kecil dan ringan, manual wiring cukup.

Contoh konsep:

```txt
Activity membuat database
Activity membuat service
Service memakai DAO
DAO memakai SQLite
```

Selama dependency masih sedikit, ini lebih eksplisit dan lebih ringan.

DI framework baru dipertimbangkan jika:

* object graph mulai besar
* testing mulai sulit
* banyak dependency saling bergantung
* manual wiring mulai rawan error

Prinsipnya:

> Framework masuk setelah kompleksitas nyata, bukan sebelum.

---

## 12. Data model must support speed and correction

Model data harus mendukung pencatatan cepat dan koreksi cepat.

Transaksi harus bisa dibuat dengan data minimum:

```txt
amount
type
wallet
category
occurredAt
```

Field lain boleh opsional.

Contoh opsional:

* note
* tag
* merchant
* attachment
* location
* custom metadata

Model juga harus mendukung:

* edit transaksi
* delete transaksi
* undo save
* change wallet
* change category
* recalculate balance

Prinsipnya:

> Data model boleh lengkap, tapi transaksi harus bisa dibuat dari input minimum.

---

## 13. Balance should be reliable, not fragile

Saldo wallet adalah data penting. Jangan dibuat mudah inkonsisten.

Ada dua pendekatan:

### Hitung dari transaksi

Lebih akurat, tapi bisa lebih berat kalau data besar.

### Simpan cached balance

Lebih cepat, tapi harus hati-hati agar tidak mismatch.

Untuk awal, pendekatan yang sehat:

> Transaksi tetap menjadi source of truth. Cached balance boleh ada untuk performa, tapi harus bisa dihitung ulang.

Prinsipnya:

> Jangan korbankan integritas data demi optimasi prematur.

---

## 14. Privacy by default

Data finansial sensitif.

Aplikasi tidak boleh sembarangan mengirim:

* nominal transaksi
* nama wallet
* catatan transaksi
* kategori custom
* merchant
* lokasi
* isi database
* pola pengeluaran personal

Kalau ada analytics/error tracking, data harus disanitasi.

Yang boleh dicatat:

```txt
transaction_saved
save_duration_ms
screen_opened
crash_stacktrace_sanitized
app_version
device_class
```

Yang tidak boleh:

```txt
amount=25000
wallet=Bank Jago
note=Makan di tempat X
category=Utang Teman
```

Prinsipnya:

> Observability boleh, kebocoran data finansial tidak.

---

## 15. Release build must shrink aggressively

Kalau nanti dependency digunakan, release build wajib mengaktifkan optimasi.

Minimal:

```txt
minify enabled
resource shrink enabled
unused code removed
unused resources removed
```

Tapi shrinking bukan alasan untuk asal tambah dependency.

R8 dan resource shrink adalah pengaman terakhir, bukan pembenaran teknis.

Prinsipnya:

> Shrinking membantu mengurangi bloat, tapi keputusan utama tetap mencegah bloat dari awal.
