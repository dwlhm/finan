# Product Principles

## 1. Capture First, Analyze Later

Aksi utama aplikasi adalah mencatat transaksi.

Analisis, grafik, laporan, insight, dan budgeting adalah fitur pendukung. Semua fitur tersebut tidak boleh mengganggu flow utama pencatatan.

Aplikasi harus selalu menjawab pertanyaan ini terlebih dahulu:

> Seberapa cepat user bisa mencatat transaksi?

Jika sebuah fitur membuat pencatatan lebih lambat, fitur tersebut harus ditunda, disederhanakan, atau dipindahkan ke flow sekunder.

## 2. Open App Means Ready to Record

Saat user membuka aplikasi, aplikasi harus langsung siap untuk mencatat.

User tidak boleh dipaksa melewati:

- Dashboard berat.
- Splash screen panjang.
- Loading data remote.
- Popup tidak penting.
- Menu navigasi bertingkat.
- Laporan bulanan sebelum input transaksi.

Default home screen harus mengarah ke pencatatan transaksi.

Ideal flow:

```txt
Open App → Type Amount → Pick Category → Save
````

## 3. Minimum Required Input

User tidak boleh dipaksa mengisi data yang belum tentu dibutuhkan.

Field wajib hanya boleh mencakup data minimum yang membuat transaksi valid.

Minimal required fields:

* Amount.
* Type, dengan default `expense`.
* Category.
* Wallet, dengan default wallet.
* Date, dengan default today.

Field opsional:

* Note.
* Tag.
* Attachment.
* Merchant.
* Location.
* Custom timestamp.
* Description panjang.

Prinsipnya:

> Data model boleh lengkap, tetapi UI pencatatan harus tetap ringan.

## 4. Defaults Over Decisions

Aplikasi harus mengurangi jumlah keputusan yang perlu dibuat user saat mencatat.

Gunakan default value untuk hal-hal yang sering berulang:

* Default transaction type: expense.
* Default wallet: wallet terakhir atau wallet utama.
* Default date: today.
* Default time: now.
* Default category suggestion: kategori yang sering digunakan.

User tetap boleh mengubah default, tetapi tidak harus mengubahnya untuk mencatat transaksi normal.

## 5. Speed Is a Feature

Kecepatan bukan detail teknis. Kecepatan adalah fitur inti produk.

Aplikasi harus terasa cepat dalam beberapa aspek:

* Cepat dibuka.
* Cepat input nominal.
* Cepat memilih kategori.
* Cepat menyimpan transaksi.
* Cepat mengedit transaksi terakhir.
* Cepat mengulang transaksi yang mirip.

Jika user merasa perlu “menyiapkan waktu” untuk mencatat transaksi, produk gagal menjalankan prinsip ini.

## 6. Local-First Interaction

Pencatatan transaksi tidak boleh bergantung pada koneksi internet.

Saat user menekan save, transaksi harus langsung tersimpan secara lokal.

Jika nanti ada backend atau sync, prosesnya harus berjalan setelah transaksi tersimpan.

Flow yang diinginkan:

```txt
Save locally instantly
→ Sync later
```

Flow yang harus dihindari:

```txt
Tap save
→ Wait for server
→ Loading
→ Saved
```

## 7. Reports Must Not Block Recording

Report dan summary penting, tetapi bukan prioritas pertama dalam interaksi harian.

Aplikasi boleh menyediakan:

* Monthly summary.
* Spending by category.
* Wallet balance.
* Recent trend.

Namun semua itu harus berada di belakang flow pencatatan.

User yang hanya ingin mencatat transaksi tidak boleh terdistraksi oleh laporan.

## 8. Fast Correction Over Perfect Input

User boleh salah input.

Karena itu, aplikasi harus membuat koreksi menjadi cepat, bukan membuat input awal terlalu ketat.

Daripada memaksa form panjang agar transaksi sempurna sejak awal, aplikasi harus menyediakan:

* Edit transaksi terakhir.
* Undo after save.
* Quick delete.
* Quick change category.
* Quick change wallet.

Prinsipnya:

> Lebih baik transaksi cepat tercatat dan mudah dikoreksi daripada transaksi lambat karena harus sempurna dari awal.

## 9. Frequently Used Actions Should Be One Tap Away

Aksi yang sering dilakukan harus mudah dijangkau.

Contoh aksi utama:

* Add expense.
* Pick common category.
* Save transaction.
* Repeat last transaction.
* Edit last transaction.
* Change wallet.

Aksi yang jarang dilakukan boleh ditempatkan lebih dalam.

Contoh aksi sekunder:

* Manage category.
* Manage wallet.
* Export data.
* Transfer between wallets.
* Budget settings.
* Advanced report.

## 10. Avoid Feature Gravity

Aplikasi finansial mudah melebar menjadi terlalu kompleks.

Fitur seperti budgeting, investment tracking, bank integration, OCR, multi-currency, dan advanced analytics memang menarik, tetapi tidak boleh masuk terlalu awal jika mengganggu fokus utama.

Sebelum menambahkan fitur, tanyakan:

1. Apakah fitur ini membuat pencatatan lebih cepat?
2. Apakah fitur ini mengurangi friction?
3. Apakah fitur ini dibutuhkan dalam penggunaan harian?
4. Apakah fitur ini bisa ditunda tanpa merusak core value?

Jika jawabannya tidak jelas, fitur tersebut masuk later scope.

## 11. Simple Mental Model

User harus mudah memahami cara kerja aplikasi.

Mental model utama:

```txt
Wallet menyimpan uang.
Transaction mengubah saldo wallet.
Category menjelaskan uang digunakan untuk apa.
Summary membantu melihat pola.
```

Jangan membuat konsep domain terlalu rumit di awal.

Hindari kompleksitas seperti:

* Nested category terlalu dalam.
* Rule budgeting kompleks.
* Split transaction terlalu awal.
* Multi-currency otomatis.
* Accounting terminology.
* Double-entry accounting di UI.

Internal system boleh rapi dan kuat, tetapi UI harus tetap sederhana.

## 12. Trust Through Control

Karena aplikasi menyimpan data finansial, user harus merasa punya kontrol.

Aplikasi harus mendukung:

* Data bisa diedit.
* Data bisa dihapus.
* Data bisa diekspor.
* Tidak ada data sensitif dikirim tanpa alasan jelas.
* Error tracking tidak boleh membawa nominal, catatan, atau detail transaksi.
* Analytics tidak boleh merekam isi transaksi pribadi.

Prinsipnya:

> User harus merasa bahwa data finansialnya adalah miliknya, bukan milik aplikasi.

## 13. Small Habit, Not Heavy Discipline

Aplikasi ini tidak boleh membuat tracking finansial terasa seperti disiplin berat.

Tujuannya adalah membantu user membangun kebiasaan kecil:

```txt
Setelah transaksi → buka app → catat → selesai
```

Aplikasi harus mendukung pencatatan yang ringan, cepat, dan tidak menghakimi.

Tone produk sebaiknya tidak terlalu menyuruh, tidak terlalu menggurui, dan tidak membuat user merasa bersalah.

## 14. Primary Experience Must Work Without Advanced Setup

User baru harus bisa langsung memakai aplikasi tanpa konfigurasi panjang.

Setup awal harus minimal:

* Buat satu default wallet.
* Siapkan kategori dasar.
* Langsung bisa mencatat transaksi.

Hal-hal seperti custom category, banyak wallet, budget, dan report detail bisa diatur belakangan.

## 15. Every Screen Should Have a Clear Job

Setiap screen harus punya fungsi yang jelas.

Contoh:

| Screen   | Job                                |
| -------- | ---------------------------------- |
| Home     | Mencatat transaksi secepat mungkin |
| History  | Melihat dan memperbaiki transaksi  |
| Wallet   | Melihat sumber uang                |
| Summary  | Melihat gambaran bulanan           |
| Settings | Mengatur preferensi                |

Hindari screen yang mencoba melakukan terlalu banyak hal sekaligus.

## Final Principle

Aplikasi ini harus mengurangi jarak antara kejadian transaksi dan pencatatannya.

Semakin pendek jaraknya, semakin besar kemungkinan user konsisten mencatat.
