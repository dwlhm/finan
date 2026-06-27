# Riset Fitur Laporan Arus Kas

Tanggal riset: 13 Juni 2026

## Ringkasan

Finan sudah memiliki sebagian besar fondasi untuk laporan arus kas: transaksi pemasukan dan
pengeluaran, saldo awal, penyesuaian saldo, transfer antardompet, rentang tanggal, filter dompet,
dan query agregasi. Layar `Ringkasan` juga sudah menampilkan pemasukan, pengeluaran, dan
`pemasukan - pengeluaran`.

Namun tampilan saat ini belum menjadi laporan arus kas yang dapat direkonsiliasi. User belum bisa
menjawab dengan jelas:

1. Berapa saldo kas pada awal periode?
2. Berapa uang yang benar-benar masuk dan keluar?
3. Mengapa saldo berubah walaupun arus kas bersih terlihat berbeda?
4. Berapa saldo kas pada akhir periode?
5. Apa pengaruh transfer dan koreksi saldo?

Rekomendasi utama:

1. Kembangkan layar `Ringkasan`, jangan menambah item navigasi utama baru.
2. Gunakan metode langsung: tampilkan pemasukan dan pengeluaran aktual dari transaksi.
3. Bedakan **arus kas bersih** dari **perubahan saldo**.
4. Tampilkan rekonsiliasi saldo awal sampai saldo akhir.
5. Jangan hitung transfer sebagai pemasukan atau pengeluaran.
6. Tampilkan efek transfer hanya ketika laporan difilter ke satu dompet.
7. Tampilkan penyesuaian saldo sebagai koreksi terpisah, bukan arus kas normal.
8. Kelompokkan laporan berdasarkan mata uang; jangan menjumlahkan mata uang berbeda.
9. Bangun klasifikasi operasi, investasi, dan pendanaan sejak MVP, tetapi terjemahkan ke bahasa
   berbasis tujuan uang pada UI.
10. Tawarkan klasifikasi ketika category dibuat, tetapi jangan menjadikannya penghalang untuk
    menyimpan transaksi.
11. Jadikan layar Kelola Kategori sebagai pusat create, edit, dan perapian klasifikasi; inbox tetap
    tersedia untuk koreksi berbasis data transaksi.

MVP memerlukan migration untuk metadata klasifikasi pada category dan snapshot klasifikasi pada
transaction. Investasi ini menghindari perombakan model laporan dan query ketika breakdown
aktivitas ditampilkan lebih lengkap.

## Definisi Produk

### Arus kas untuk Finan

Untuk pengguna personal finance, definisi yang paling mudah dipahami adalah:

```text
Arus kas bersih = pemasukan - pengeluaran
```

Nilai tersebut menjawab apakah aktivitas keuangan selama periode menghasilkan surplus atau
defisit.

### Perubahan saldo

Saldo dompet juga dapat berubah karena transfer dan penyesuaian:

```text
Perubahan saldo
= pemasukan
- pengeluaran
+ transfer masuk
- transfer keluar
+ penyesuaian naik
- penyesuaian turun
```

Karena itu, arus kas bersih tidak selalu sama dengan perubahan saldo satu dompet.

### Rekonsiliasi

Laporan harus memenuhi persamaan:

```text
Saldo akhir
= saldo awal
+ arus kas bersih
+ transfer bersih
+ koreksi saldo bersih
```

Untuk laporan seluruh dompet dengan mata uang yang sama, transfer antardompet harus saling
menghapus:

```text
total transfer masuk - total transfer keluar = 0
```

### Batas klaim

Fitur ini adalah laporan arus kas personal berbasis transaksi, bukan laporan keuangan bisnis yang
menyatakan kepatuhan terhadap PSAK atau IFRS.

IAS 7 mengklasifikasikan arus kas bisnis menjadi aktivitas operasi, investasi, dan pendanaan.
Standar tersebut juga mengizinkan metode langsung yang menampilkan kelompok utama penerimaan dan
pembayaran kas. Metode langsung sesuai dengan Finan karena data yang tersedia adalah transaksi kas
aktual.

Metode tidak langsung tidak cocok untuk Finan karena membutuhkan laba/rugi, transaksi nonkas,
akrual, dan perubahan akun neraca yang tidak dimiliki aplikasi.

### Prinsip klasifikasi

Finan menggunakan klasifikasi formal sebagai struktur data, bukan sebagai tes pengetahuan
akuntansi bagi user.

Kontrak domain:

```text
OPERATING
INVESTING
FINANCING
UNCLASSIFIED
```

Bahasa utama UI:

| Domain | Label UI | Pertanyaan yang dijawab |
| --- | --- | --- |
| `OPERATING` | Aktivitas harian | Uang dari dan untuk hidup, kerja, atau usaha sehari-hari? |
| `INVESTING` | Aset & investasi | Uang untuk membeli atau melepas sesuatu yang bernilai jangka panjang? |
| `FINANCING` | Pinjaman & modal | Uang dari atau untuk utang, pinjaman, atau modal? |
| `UNCLASSIFIED` | Belum dikelompokkan | Tujuannya belum diketahui atau user belum ingin menentukan. |

Label formal `Operasi`, `Investasi`, dan `Pendanaan` tetap dapat ditampilkan sebagai keterangan
sekunder atau pada export. UI sehari-hari tidak bergantung pada istilah tersebut.

Desain memiliki dua lapis:

```text
Lapis domain
→ stabil, formal, dapat diuji, kompatibel dengan export

Lapis pengalaman
→ berbasis pertanyaan user, bertahap, dapat dilewati, mudah dikoreksi
```

Standar akuntansi menjadi constraint kebenaran dan interoperabilitas, bukan template layout.

## Masalah Pengguna

### Pertanyaan utama

User ingin mengetahui:

- Apakah uang masuk lebih besar daripada uang keluar?
- Dari mana uang masuk?
- Ke mana uang paling banyak keluar?
- Bagaimana saldo berubah dari awal sampai akhir periode?
- Mengapa saldo akhir tidak sama dengan saldo awal ditambah pemasukan dikurangi pengeluaran?
- Bagaimana arus kas satu dompet berbeda dari keseluruhan keuangan?

### Jobs to be done

```text
Ketika meninjau keuangan bulanan,
saya ingin melihat uang masuk, uang keluar, dan perubahan saldo,
agar saya memahami apakah kondisi kas saya membaik atau memburuk.
```

```text
Ketika saldo akhir tidak sesuai perkiraan,
saya ingin melihat transfer dan koreksi saldo secara terpisah,
agar saya dapat menjelaskan selisih tanpa menganggapnya sebagai pemasukan atau pengeluaran.
```

## Kondisi Finan Saat Ini

### Data yang sudah tersedia

- `INCOME` untuk pemasukan.
- `EXPENSE` untuk pengeluaran.
- `TRANSFER_IN` dan `TRANSFER_OUT` untuk dua sisi transfer.
- `ADJUSTMENT_INCREASE` dan `ADJUSTMENT_DECREASE` untuk koreksi saldo.
- `opening_balance_minor` pada wallet.
- `occurred_at` untuk penentuan periode.
- Mata uang pada setiap wallet.
- Category, merchant, tag, note, dan wallet untuk breakdown.
- Index berdasarkan waktu, wallet, tipe, dan transfer.

### Perilaku yang sudah benar

- Summary hanya menghitung `INCOME` dan `EXPENSE` sebagai pemasukan dan pengeluaran.
- Transfer tidak menggelembungkan summary.
- Adjustment tidak masuk ke total pemasukan atau pengeluaran.
- Saldo wallet dihitung dari saldo awal dan seluruh efek transaksi.
- Transfer hanya diizinkan untuk wallet dengan mata uang yang sama.

### Kesenjangan

- Tidak ada saldo awal periode.
- Tidak ada saldo akhir yang direkonsiliasi dengan pergerakan selama periode.
- Tidak ada total transfer masuk/keluar pada laporan per dompet.
- Tidak ada total koreksi saldo.
- Total saldo seluruh wallet masih berisiko menjumlahkan mata uang berbeda.
- Breakdown hanya menampilkan kategori pengeluaran teratas.
- Default rentang layar `Ringkasan` adalah hari ini, kurang sesuai untuk review bulanan.
- Nama model `MonthlySummary` dan field `today*` tidak lagi sesuai karena layar menerima rentang
  tanggal arbitrer.
- Filter kategori dapat membingungkan jika digunakan bersamaan dengan saldo awal dan saldo akhir,
  karena saldo tidak memiliki satu kategori.
- Category dan transaction belum menyimpan klasifikasi aktivitas.
- Tidak ada cara membedakan klasifikasi bawaan category dan koreksi khusus satu transaction.

## Keputusan Produk

### Tempat fitur

Gunakan item navigasi `Ringkasan` yang sudah ada. Jangan menambah tab utama `Laporan`.

Alasan:

- Laporan tetap berada di belakang flow pencatatan.
- Bottom navigation tidak menjadi lebih padat.
- Layar saat ini sudah memiliki elemen dasar arus kas.
- User tidak perlu memilih antara dua layar yang isinya tumpang tindih.

Nama layar tetap `Ringkasan`. Di dalamnya, gunakan bagian atau pilihan tampilan:

```text
Ringkasan
├── Arus kas
├── Pengeluaran terbesar
└── Saldo dompet
```

Jika konten nantinya terlalu panjang, gunakan segmented control `Arus kas | Kategori`, bukan item
bottom navigation baru.

### Periode default

Default yang direkomendasikan adalah bulan kalender berjalan, bukan hanya hari ini.

Preset:

- Bulan ini.
- Bulan lalu.
- 30 hari terakhir.
- Rentang khusus.

Rentang khusus bersifat inklusif pada tanggal mulai dan selesai berdasarkan timezone perangkat.

### Filter

MVP:

- Semua dompet.
- Satu dompet.
- Rentang tanggal.

Filter kategori tidak digunakan untuk kartu rekonsiliasi. Kategori menjadi breakdown atau
drill-down dari pemasukan dan pengeluaran.

Jika filter kategori lama dipertahankan, saldo awal, saldo akhir, transfer, dan koreksi harus
disembunyikan serta tampilan diberi label `Analisis kategori`, bukan `Laporan arus kas`.

### Klasifikasi tanpa mengganggu capture

Transaksi yang memakai category lama tidak mendapatkan langkah tambahan. Flow transaksi normal
tetap:

```text
Nominal → kategori → simpan
```

Saat transaksi disimpan:

1. Transaction mengambil snapshot klasifikasi default category.
2. Jika category belum diklasifikasikan, transaction disimpan sebagai `UNCLASSIFIED`.
3. Transaksi tetap masuk total pemasukan, pengeluaran, arus kas bersih, dan rekonsiliasi.
4. Hanya subtotal aktivitas yang menempatkannya pada `Belum dikelompokkan`.

Dengan demikian, laporan selalu lengkap walaupun klasifikasi belum sempurna.

### Membuat category dari layar Catat

Flow `Buat kategori "..."` dan `Tambah kategori baru` membuka panel create yang sama, bukan langsung
menulis category setelah nama diketik.

Flow yang sama digunakan ketika category dibuat dari editor transaction. Tipe mengikuti transaction
yang sedang diedit dan state editor transaction tetap dipertahankan.

```text
Kategori baru

Nama
[Deposito________________]

Biasanya uang ini untuk apa?
[Aktivitas harian]
[Aset & investasi]
[Pinjaman & modal]
[Belum yakin]

[Buat & pilih]
```

Aturan:

- Nama tetap satu-satunya field wajib.
- Tipe category otomatis mengikuti transaksi aktif: `EXPENSE` atau `INCOME`.
- Klasifikasi terlihat sejak awal; `Belum yakin` dipilih secara awal agar user tetap dapat
  menyimpan tanpa keputusan palsu.
- Pilihan klasifikasi cukup satu tap dengan radio card atau chip besar.
- Keyboard tetap terbuka saat mengisi nama dan tidak menutupi tombol utama.
- Setelah `Buat & pilih`, category langsung terpilih dan user kembali ke form transaksi.
- Input nominal, wallet, tanggal, note, merchant, dan tag yang sudah diisi tidak berubah.
- Tidak ada dialog sukses tambahan yang harus ditutup; feedback cukup toast singkat.

Untuk query pencarian seperti `Deposito`, baris aksi tetap cepat:

```text
+ Buat "Deposito"
```

Menekannya membuka panel dengan nama sudah terisi dan fokus berpindah ke pilihan tujuan uang. User
dapat memilih klasifikasi lalu menyimpan, atau langsung memilih `Belum yakin`.

Jangan menebak klasifikasi dari nama category pada MVP. Jika nanti tersedia suggestion, tampilkan
sebagai rekomendasi yang belum dipilih:

```text
Saran: Aset & investasi
```

User tetap harus mengonfirmasi atau memilih `Belum yakin`.

### Inbox klasifikasi

Ringkasan menampilkan prompt non-blocking hanya bila ada data yang belum dikelompokkan:

```text
Rapikan arus kas
3 kategori belum dikelompokkan · Rp850.000
[Tinjau]
```

Tinjauan dilakukan per category, bukan per transaction:

```text
Kategori: Deposito
Biasanya uang dalam kategori ini untuk apa?

[Aktivitas harian]
[Aset & investasi]
[Pinjaman & modal]
[Belum yakin]
```

Setiap pilihan memiliki contoh singkat, bukan definisi akuntansi panjang. Pilihan `Belum yakin`
selalu tersedia dan tidak dianggap error.

Contoh microcopy:

| Pilihan | Bantuan singkat |
| --- | --- |
| Aktivitas harian | Gaji, makan, transport, tagihan, kebutuhan usaha rutin. |
| Aset & investasi | Emas, saham, deposito, properti, atau aset jangka panjang. |
| Pinjaman & modal | Menerima pinjaman, membayar pokok utang, atau menambah modal. |
| Belum yakin | Simpan dulu tanpa menghilangkan transaksi dari laporan. |

Ketika category yang sebelumnya `UNCLASSIFIED` diklasifikasikan, user memilih cakupan:

```text
Terapkan ke:
(•) Transaksi kategori ini, termasuk yang lama
( ) Transaksi berikutnya saja
```

Transaction yang pernah dikoreksi manual tidak ikut berubah pada bulk update.

Jika user mengganti category yang sudah memiliki klasifikasi, default berubah menjadi `Transaksi
berikutnya saja`. Perubahan histori harus menjadi pilihan sadar karena dapat mengubah laporan
periode lama.

### Koreksi kontekstual

User dapat menekan subtotal atau transaction lalu memilih `Ubah kelompok arus kas`. Koreksi satu
transaction menjadi override dan tidak mengubah default category.

Setelah beberapa koreksi pada category yang sama, Finan boleh menawarkan:

```text
3 transaksi kategori Properti dipindah ke Aset & investasi.
Jadikan ini default kategori?
```

Saran tidak boleh diterapkan otomatis. Ini membuat sistem belajar dari perilaku user tanpa
menambah wizard setup.

Inbox tetap diperlukan walaupun create flow sudah mendukung klasifikasi karena:

- Data hasil migration dapat belum diklasifikasikan.
- User dapat memilih `Belum yakin` saat sedang terburu-buru.
- Category lama dapat memiliki penggunaan baru yang tidak cocok dengan default.
- Override transaction dapat menunjukkan bahwa default category perlu diperbaiki.
- User mungkin baru memahami arti category setelah melihat transaksi nyata.

### Kasus ambigu

Finan tidak memaksakan satu jawaban global untuk kasus yang bergantung pada konteks:

- Laptop dapat menjadi kebutuhan harian atau aset jangka panjang.
- Uang dari keluarga dapat menjadi pemasukan biasa, pinjaman, atau modal.
- Cicilan dapat mengandung pokok dan bunga dalam satu pembayaran.
- Category bertipe `BOTH` dapat digunakan untuk arah masuk dan keluar.

MVP memakai default category dengan override per transaction. Jika satu pembayaran mengandung dua
makna dan user membutuhkan ketelitian lebih tinggi, user dapat mencatat dua transaction. Split
transaction otomatis bukan syarat MVP dan laporan tidak boleh mengklaim kepatuhan formal.

### Multi-currency

Jangan pernah menjumlahkan saldo atau arus kas dengan mata uang berbeda.

Pilihan MVP yang direkomendasikan:

- Jika semua wallet memiliki satu mata uang, tampilkan satu laporan gabungan.
- Jika terdapat beberapa mata uang, tampilkan satu bagian laporan per mata uang.
- Filter satu wallet selalu menampilkan mata uang wallet tersebut.

Konversi kurs tidak masuk MVP karena Finan tidak menyimpan kurs historis.

## Cakupan MVP

### Kartu utama

Alih-alih membuka dengan tabel akuntansi, Ringkasan menjawab pertanyaan paling sederhana dahulu:

```text
Pemasukan lebih besar
Rp1.250.000

Masuk Rp4.500.000 · Keluar Rp3.250.000
```

Jika negatif, copy berubah menjadi `Pengeluaran lebih besar`. Jika nol, gunakan `Pemasukan dan
pengeluaran seimbang`. Headline ini menjelaskan arus kas regular dan tidak mengklaim sebagai
perubahan saldo.

Gunakan warna sebagai penguat, bukan satu-satunya pembeda. Nilai negatif harus tetap memiliki
tanda minus dan label yang jelas.

Bagian berikutnya menjelaskan asal perubahan:

```text
Dari mana perubahannya?

Aktivitas harian       +Rp1.750.000
Aset & investasi         -Rp500.000
Pinjaman & modal                 Rp0
Belum dikelompokkan              Rp0
```

Setiap baris dapat dibuka untuk melihat pemasukan, pengeluaran, category, dan transaction
pembentuknya. Tampilan detail dapat memakai istilah formal sebagai keterangan sekunder.

### Rekonsiliasi saldo

```text
Saldo awal                 Rp2.000.000
Pemasukan                 +Rp4.500.000
Pengeluaran               -Rp3.250.000
Transfer bersih                     Rp0
Koreksi saldo                -Rp50.000
                           ------------
Saldo akhir                 Rp3.200.000
```

Untuk semua dompet, baris transfer dapat disembunyikan ketika nol. Untuk satu dompet, baris
transfer selalu ditampilkan jika ada mutasi transfer.

Di atas rekonsiliasi, tampilkan kesimpulan terpisah:

```text
Saldo bertambah Rp1.200.000
```

Perbedaan antara `Pemasukan lebih besar Rp1.250.000` dan `Saldo bertambah Rp1.200.000` dijelaskan
oleh baris transfer dan koreksi, tanpa membuat user harus menyimpulkan sendiri.

### Breakdown

MVP menampilkan:

- Pemasukan berdasarkan kategori.
- Pengeluaran berdasarkan kategori.
- Maksimal lima kategori pada masing-masing bagian.
- Aksi `Lihat semua` menuju riwayat dengan periode, tipe, wallet, dan kategori yang sesuai.

Merchant dan tag dapat menjadi fase lanjutan karena category sudah menjadi dimensi utama produk.

### Empty state

Jika tidak ada transaksi regular:

```text
Belum ada pemasukan atau pengeluaran pada periode ini.
```

Rekonsiliasi tetap ditampilkan bila saldo awal, transfer, atau koreksi tersedia. Ini penting karena
saldo dapat berubah walaupun tidak ada transaksi regular.

## Aturan Perhitungan

Untuk setiap wallet:

```text
saldo awal periode
= opening_balance_minor
+ seluruh efek transaksi dengan occurred_at < startInclusive
```

```text
saldo akhir periode
= opening_balance_minor
+ seluruh efek transaksi dengan occurred_at < endExclusive
```

Agregasi selama periode:

| Jenis transaksi | Pemasukan | Pengeluaran | Transfer | Koreksi | Efek saldo |
| --- | ---: | ---: | ---: | ---: | ---: |
| `INCOME` | `+amount` | 0 | 0 | 0 | `+amount` |
| `EXPENSE` | 0 | `+amount` | 0 | 0 | `-amount` |
| `TRANSFER_IN` | 0 | 0 | `+amount` | 0 | `+amount` |
| `TRANSFER_OUT` | 0 | 0 | `-amount` | 0 | `-amount` |
| `ADJUSTMENT_INCREASE` | 0 | 0 | 0 | `+amount` | `+amount` |
| `ADJUSTMENT_DECREASE` | 0 | 0 | 0 | `-amount` | `-amount` |

Turunan:

```text
arus kas bersih = income - expense
transfer bersih = transfer in - transfer out
koreksi bersih = adjustment increase - adjustment decrease
perubahan saldo = arus kas bersih + transfer bersih + koreksi bersih
```

Invariant:

```text
saldo akhir - saldo awal = perubahan saldo
jumlah net seluruh activity = arus kas bersih
```

Jika invariant gagal, laporan tidak boleh diam-diam menampilkan angka yang tidak seimbang. Log
error tanpa data finansial sensitif dan tampilkan fallback yang tidak menyesatkan.

## Contoh

Saldo awal seluruh wallet adalah Rp2.000.000. Selama Juni:

- Pemasukan Rp4.500.000.
- Pengeluaran Rp3.250.000.
- Transfer Rp500.000 dari Bank ke GoPay.
- Penyesuaian saldo turun Rp50.000.

Laporan seluruh wallet:

```text
Arus kas bersih       Rp1.250.000
Transfer bersih                Rp0
Koreksi saldo           -Rp50.000
Perubahan saldo        Rp1.200.000
Saldo akhir            Rp3.200.000
```

Laporan wallet Bank:

```text
Arus kas bersih       Rp1.250.000
Transfer bersih        -Rp500.000
Koreksi saldo           -Rp50.000
Perubahan saldo          Rp700.000
```

Transfer mengubah saldo Bank, tetapi tidak mengubah surplus atau defisit keuangan user secara
keseluruhan.

## Model Domain

Ganti model yang terlalu spesifik `MonthlySummary` dengan model laporan berbasis rentang:

```text
CashFlowReport
- startInclusive
- endExclusive
- currencyCode
- walletId nullable
- openingBalanceMinor
- incomeMinor
- expenseMinor
- transferInMinor
- transferOutMinor
- adjustmentIncreaseMinor
- adjustmentDecreaseMinor
- closingBalanceMinor
- activityTotals
- incomeCategories
- expenseCategories
```

Nilai turunan seperti net cash flow, net transfer, net adjustment, dan balance change sebaiknya
dihitung oleh model atau rule domain, bukan di Fragment.

Untuk laporan multi-currency:

```text
CashFlowReportResult
- reportsByCurrency
```

Tambahkan domain:

```text
CashFlowActivity
- OPERATING
- INVESTING
- FINANCING
- UNCLASSIFIED

CashFlowActivityTotal
- activity
- incomeMinor
- expenseMinor
- netMinor
```

## Query dan Arsitektur

### DAO

Tambahkan query khusus laporan. Jangan merakit laporan dengan membaca seluruh transaksi ke memori.

Operasi minimum:

```text
cashFlowTotalsBetween(start, end, walletId)
activityTotalsBetween(start, end, walletId)
walletBalancesBefore(timestamp, walletId)
categoryTotalsBetween(type, start, end, walletId, limit)
```

Query totals menggunakan `SUM(CASE WHEN type = ... THEN amount_minor ELSE 0 END)` agar satu scan
periode menghasilkan seluruh total.

Saldo awal dan akhir dapat dihitung per wallet lalu dikelompokkan berdasarkan `currency_code`.
Index yang sudah ada pada wallet/waktu dan tipe/waktu cukup untuk MVP. Profil query tetap perlu
dilakukan dengan data besar sebelum menambah index baru.

### Service

Tambahkan `CashFlowReportService` yang bertanggung jawab untuk:

- Normalisasi rentang tanggal.
- Mengubah tanggal lokal menjadi timestamp inklusif-eksklusif.
- Memvalidasi wallet.
- Mengelompokkan wallet berdasarkan currency.
- Mengambil totals dan breakdown.
- Memverifikasi invariant rekonsiliasi.

Fragment hanya memilih filter, memicu load pada `DbWorker`, dan merender hasil.

Tambahkan `CategoryClassificationService` agar create/edit category, bulk update histori, dan
override transaction tidak menjalankan aturan domain langsung dari UI atau DAO.

Operasi minimum:

```text
createCategory(name, typeFilter, activity)
updateCategory(category, historyScope)
overrideTransactionActivity(transactionId, activity)
findClassificationInbox(period, walletId)
```

Update category dan transaction historis harus berlangsung dalam satu database transaction.

### Database

MVP memerlukan migration:

```sql
ALTER TABLE categories
ADD COLUMN cash_flow_activity TEXT NOT NULL DEFAULT 'UNCLASSIFIED'
CHECK (cash_flow_activity IN ('OPERATING', 'INVESTING', 'FINANCING', 'UNCLASSIFIED'));

ALTER TABLE transactions
ADD COLUMN cash_flow_activity TEXT NOT NULL DEFAULT 'UNCLASSIFIED'
CHECK (cash_flow_activity IN ('OPERATING', 'INVESTING', 'FINANCING', 'UNCLASSIFIED'));

ALTER TABLE transactions
ADD COLUMN cash_flow_activity_overridden INTEGER NOT NULL DEFAULT 0
CHECK (cash_flow_activity_overridden IN (0, 1));
```

Aturan penyimpanan:

- Category menyimpan default untuk transaksi baru.
- Transaction menyimpan snapshot agar laporan historis stabil.
- `cash_flow_activity_overridden = 1` menandai koreksi khusus transaction.
- Perubahan default category tidak diam-diam mengubah histori.
- Bulk update histori hanya mengubah transaction category tersebut yang belum memiliki override.
- Transfer dan adjustment disimpan sebagai `UNCLASSIFIED`, tetapi tidak masuk subtotal aktivitas
  karena subtotal hanya membaca `INCOME` dan `EXPENSE`.
- Saat category transaction diedit dan belum memiliki override, snapshot mengikuti default category
  yang baru.

Backfill:

- Category pengeluaran bawaan `Makanan`, `Transport`, `Kopi`, `Tagihan`, dan `Belanja` menjadi
  `OPERATING`.
- Category pemasukan bawaan `Gaji` menjadi `OPERATING`.
- `Lainnya` dan category custom lama tetap `UNCLASSIFIED`.
- Transaction lama mengambil hasil klasifikasi category setelah backfill.

Jangan menebak category custom berdasarkan nama. Ketidakpastian harus terlihat sebagai
`Belum dikelompokkan`, bukan disembunyikan lewat klasifikasi otomatis yang tampak meyakinkan.

### Pengelolaan category

Layar `Kelola Kategori` tidak lagi read-only. Pekerjaan utamanya:

1. Membuat category dengan tipe dan klasifikasi yang jelas.
2. Menemukan category yang belum dikelompokkan.
3. Mengubah nama, tipe, dan default klasifikasi.
4. Memilih apakah perubahan klasifikasi berlaku ke histori.
5. Membuka transaksi category tersebut untuk memeriksa hasil.

Struktur layar:

```text
Kategori                          [+ Tambah]

[Cari kategori...]
[Semua] [Belum dikelompokkan]

Perlu dirapikan · 2
  Lainnya
  Pengeluaran & pemasukan · Belum dikelompokkan

Kategori lain
  Makanan
  Pengeluaran · Aktivitas harian

  Deposito
  Pemasukan · Aset & investasi
```

Category yang belum dikelompokkan diprioritaskan tanpa membuat layar terasa seperti daftar error.
Gunakan label `Perlu dirapikan`, bukan ikon peringatan merah.

### Membuat category dari Kelola Kategori

Karena konteks transaksi tidak tersedia, flow lengkap meminta:

```text
Kategori baru

Nama
[________________________]

Dipakai untuk
[Pengeluaran] [Pemasukan] [Keduanya]

Biasanya uang ini untuk apa?
[Aktivitas harian]
[Aset & investasi]
[Pinjaman & modal]
[Belum yakin]

[Simpan kategori]
```

Nama dan tipe wajib. Klasifikasi tetap dapat `Belum yakin`. Form menggunakan komponen serta
microcopy yang sama dengan quick-create agar tidak terasa seperti dua sistem berbeda.

### Satu editor, dua mode

Implementasi menggunakan satu komponen category editor dengan dua mode:

| Mode | Nama | Tipe | Klasifikasi | Hasil |
| --- | --- | --- | --- | --- |
| Quick create | Prefill dari pencarian | Tetap dari transaksi aktif | Terlihat, opsional | Simpan dan langsung pilih |
| Full manage | Diisi user | Dapat dipilih | Terlihat, opsional | Simpan dan kembali ke daftar/detail |

Pada quick mode, tipe tetap terlihat sebagai konteks ringan seperti `Untuk pengeluaran`, tetapi
tidak menjadi keputusan tambahan. Dengan komponen bersama, label, validasi, accessibility, dan
pilihan activity tidak berkembang berbeda antara dua flow.

### Mengedit category

Menekan row membuka detail/edit:

```text
Makanan

Nama                    Makanan
Dipakai untuk           Pengeluaran
Kelompok arus kas       Aktivitas harian

12 transaksi
[Lihat transaksi]
[Simpan perubahan]
```

Jika klasifikasi berubah, tampilkan pilihan cakupan sebelum menyimpan:

```text
Terapkan kelompok baru ke:
(•) Transaksi berikutnya saja
( ) Semua transaksi kategori ini yang belum dikoreksi manual
```

Untuk category `UNCLASSIFIED`, default cakupan boleh `Semua transaksi ...` karena perubahan tersebut
melengkapi data yang sebelumnya belum memiliki keputusan. Untuk perpindahan antar klasifikasi yang
sudah pasti, default tetap `Transaksi berikutnya saja`.

Mengubah tipe category tidak otomatis mengubah tipe transaction lama. Jika category dari
`EXPENSE` diubah menjadi `BOTH`, perubahan hanya menentukan ketersediaannya untuk transaksi
berikutnya.

Delete category tidak masuk MVP karena transaction lama masih mereferensikannya. Jika dibutuhkan
nanti, gunakan archive agar histori tetap utuh.

Tiga momen klasifikasi membentuk satu siklus:

```text
Create category
→ tentukan default sedini mungkin

Manage Category
→ rawat struktur dan ubah cakupan dengan sadar

Inbox Ringkasan
→ koreksi ketidakpastian berdasarkan transaksi nyata
```

## UX yang Direkomendasikan

Urutan konten:

1. Header dan pemilih periode.
2. Filter wallet.
3. Kartu `Pemasukan/Pengeluaran lebih besar`.
4. Breakdown berbasis tujuan uang.
5. Prompt klasifikasi bila diperlukan.
6. Rekonsiliasi saldo.
7. Breakdown pemasukan dan pengeluaran.
8. Saldo per wallet.

Prinsip interaksi:

- Laporan dimuat hanya ketika layar `Ringkasan` dibuka.
- Tidak ada query laporan pada cold start atau flow pencatatan.
- Gunakan komponen Android Views yang sudah ada.
- Grafik bukan syarat MVP.
- Jika grafik ditambahkan, gunakan batang sederhana per minggu atau bulan dan jangan menambah
  dependency chart hanya untuk satu visual.
- Baris laporan dapat ditekan untuk membuka transaksi pembentuk angka tersebut.
- Detail bertahap: angka utama terlihat dahulu, detail akuntansi tersedia saat dibuka.
- Jangan memblokir laporan dengan wizard klasifikasi.
- Jangan memakai rasa bersalah seperti `Data belum lengkap`; gunakan ajakan netral `Rapikan arus
  kas`.
- Quick-create dan Manage Category menggunakan komponen pilihan klasifikasi serta microcopy yang
  sama.
- State form transaksi tidak boleh hilang ketika user membuka, membatalkan, atau menyelesaikan
  quick-create category.

## Fase Lanjutan

### Fase 1.1: Tren

- Arus kas bersih per hari untuk rentang sampai 31 hari.
- Per minggu untuk rentang menengah.
- Per bulan untuk rentang panjang.
- Perbandingan dengan periode sebelumnya yang panjangnya sama.

### Fase 2: Bantuan klasifikasi

- Saran default berdasarkan koreksi berulang user.
- Review beberapa category dalam satu sesi.
- Indikator coverage klasifikasi tanpa skor yang menghakimi.
- Aturan personal per category dan arah transaksi jika dibutuhkan.

### Fase 3: Export laporan

- CSV untuk data terstruktur.
- PDF hanya jika ada kebutuhan kuat untuk berbagi laporan visual.
- Export transaksi mentah tetap dipisahkan dari export laporan agregat.

## Non-Goals MVP

- Kepatuhan formal PSAK/IFRS.
- Metode arus kas tidak langsung.
- Forecast arus kas.
- Transaksi berulang yang belum terjadi.
- Budget vs actual.
- Konversi multi-currency.
- Grafik kompleks.
- Valuasi atau performa investasi.
- Perhitungan utang, piutang, depresiasi, atau transaksi nonkas.

## Acceptance Criteria

1. Default laporan menampilkan bulan berjalan.
2. Tanggal selesai termasuk seluruh transaksi sampai akhir hari lokal.
3. Pemasukan hanya berasal dari `INCOME`.
4. Pengeluaran hanya berasal dari `EXPENSE`.
5. Transfer tidak mengubah arus kas bersih.
6. Transfer satu wallet terlihat pada rekonsiliasi wallet tersebut.
7. Transfer seluruh wallet dengan mata uang sama bernilai bersih nol.
8. Adjustment terlihat sebagai koreksi dan tidak masuk pemasukan/pengeluaran.
9. Saldo akhir sama dengan saldo awal ditambah seluruh efek periode.
10. Mata uang berbeda tidak pernah dijumlahkan.
11. Nilai nol, negatif, dan jumlah besar diformat dengan benar.
12. Laporan tetap benar untuk periode tanpa transaksi regular.
13. Menekan breakdown membuka riwayat dengan filter yang setara.
14. Membuka aplikasi tetap langsung siap mencatat tanpa menunggu query laporan.
15. Setiap transaction regular memiliki snapshot `CashFlowActivity`.
16. Transaction `UNCLASSIFIED` tetap masuk total dan tampil dalam subtotal khusus.
17. Quick-create category menampilkan pilihan klasifikasi dan tetap dapat disimpan sebagai
    `UNCLASSIFIED`.
18. Quick-create otomatis memakai tipe transaksi aktif dan langsung memilih category yang dibuat.
19. Membatalkan quick-create tidak menghilangkan isi form transaksi.
20. Quick-create dari editor transaction mempertahankan perubahan transaction yang belum disimpan.
21. Manage Category dapat membuat dan mengedit nama, tipe, serta klasifikasi.
22. Manage Category menolak nama kosong dan nama duplikat tanpa membedakan kapitalisasi.
23. Manage Category dapat memfilter category `UNCLASSIFIED`.
24. Perubahan klasifikasi category menyediakan cakupan future-only atau histori non-override.
25. Mengubah default category tidak mengubah transaction override.
26. User dapat mengubah satu transaction tanpa mengubah category.
27. Inbox tetap menampilkan category atau transaksi yang membutuhkan koreksi.
28. Breakdown aktivitas dapat direkonsiliasi dengan pemasukan dan pengeluaran regular.

## Strategi Pengujian

### Unit test

- Rumus arus kas bersih.
- Rumus transfer bersih.
- Rumus koreksi bersih.
- Rekonsiliasi saldo.
- Normalisasi rentang tanggal.
- Pengelompokan multi-currency.
- Resolusi klasifikasi dari category ke transaction.
- Override satu transaction.
- Bulk update yang tidak menimpa override.
- Resolusi scope `future-only` dan `include-history`.
- Rekonsiliasi subtotal aktivitas dengan arus kas bersih.
- Nilai negatif dan overflow.

### DAO test

- Batas `startInclusive` dan `endExclusive`.
- Semua jenis transaksi.
- Filter satu wallet.
- Agregasi seluruh wallet.
- Dua sisi transfer.
- Breakdown kategori.
- Breakdown per aktivitas.
- Backfill category dan transaction pada migration.
- Query `UNCLASSIFIED`.
- Count transaction per category untuk Manage Category.
- Bulk update histori dan rollback atomik ketika operasi gagal.
- Saldo awal dari opening balance dan transaksi sebelum periode.

### UI/instrumentation test

- Empty state.
- Pergantian preset periode.
- Filter wallet.
- Multi-currency.
- Inbox klasifikasi.
- Quick-create category dengan activity terpilih.
- Quick-create dengan `Belum yakin`.
- Cancel quick-create mempertahankan draft transaksi.
- Quick-create dari editor transaction mempertahankan state edit.
- Create dan edit pada Manage Category.
- Filter `Belum dikelompokkan`.
- Koreksi satu transaction.
- Pilihan cakupan perubahan category.
- Navigasi drill-down ke history.
- State tetap tersimpan setelah recreation.

## Risiko

### Angka terlihat tidak seimbang

Penyebab umum adalah adjustment atau transfer yang disembunyikan. Mitigasi: selalu sediakan
rekonsiliasi dan jangan hanya menampilkan pemasukan dikurangi pengeluaran.

### Salah menjumlahkan mata uang

Mitigasi: laporan dikelompokkan per currency dan tidak memiliki total lintas currency.

### Istilah terlalu akuntansi

Mitigasi: domain tetap menggunakan istilah formal, sedangkan UI utama memakai `Aktivitas harian`,
`Aset & investasi`, dan `Pinjaman & modal`. Definisi formal tersedia sebagai bantuan, bukan syarat
untuk memakai fitur.

### Klasifikasi salah tetapi terlihat meyakinkan

Mitigasi: category custom lama tidak ditebak, `Belum dikelompokkan` terlihat jelas, semua saran
memerlukan konfirmasi, dan perubahan dapat dibatasi pada satu transaction.

### Setup terasa seperti pekerjaan rumah

Mitigasi: tidak ada wizard wajib, review dilakukan per category, laporan tetap berguna sebelum
review selesai, klasifikasi dapat dilakukan saat category dibuat, dan inbox hanya muncul di konteks
Ringkasan.

### Quick-create memperlambat pencatatan

Mitigasi: flow tambahan hanya muncul ketika membuat category baru, nama tetap satu-satunya input
wajib, `Belum yakin` tersedia sejak awal, state transaksi dipertahankan, dan category langsung
terpilih setelah disimpan.

### Query laporan membebani aplikasi

Mitigasi: lazy load di layar Ringkasan, agregasi di SQL, pembatasan breakdown, dan tidak menambah
pekerjaan pada startup atau save transaction.

## Rekomendasi Implementasi

Urutan implementasi:

1. Tambahkan enum dan rule `CashFlowActivity`.
2. Migration category default, transaction snapshot, override flag, dan backfill.
3. Perluas model dan DAO category untuk klasifikasi, update, search, dan count penggunaan.
4. Buat `CategoryClassificationService` dengan scope histori dan transaksi database.
5. Buat komponen category editor bersama untuk quick-create dan Manage Category.
6. Upgrade quick-create pada Category Search agar meminta klasifikasi secara opsional.
7. Upgrade Manage Category menjadi create, search, filter, detail, dan edit.
8. Integrasikan snapshot klasifikasi pada create/edit transaction.
9. Tambahkan model dan rule `CashFlowReport`.
10. Tambahkan query subtotal aktivitas, total periode, dan saldo sebelum timestamp.
11. Buat `CashFlowReportService`.
12. Tambahkan unit, migration, DAO, dan UI test untuk category serta rekonsiliasi.
13. Ubah default periode Ringkasan menjadi bulan berjalan.
14. Refactor kartu utama dengan bahasa perubahan uang dan breakdown aktivitas.
15. Tambahkan inbox klasifikasi per category serta override per transaction.
16. Tambahkan rekonsiliasi, breakdown category, dan pengelompokan multi-currency.
17. Tambahkan drill-down ke history.
18. Ukur query dan waktu render dengan dataset besar.

Prioritas produk:

```text
Rekonsiliasi benar
> perlakuan transfer dan adjustment benar
> fondasi klasifikasi stabil
> klasifikasi tidak mengganggu capture
> multi-currency aman
> breakdown
> tren/grafik
> bantuan klasifikasi otomatis
```

## Sumber

- IFRS Foundation, [IAS 7 Statement of Cash
  Flows](https://www.ifrs.org/issued-standards/list-of-standards/ias-7-statement-of-cash-flows/):
  definisi kas dan setara kas, klasifikasi operasi/investasi/pendanaan, metode langsung dan tidak
  langsung, serta rekonsiliasi kas.
- `docs/product-principal.md`: report tidak boleh menghalangi flow pencatatan dan istilah akuntansi
  kompleks perlu dihindari.
- `docs/research-balance-adjustment-transfer.md`: transfer dan adjustment dikeluarkan dari total
  pemasukan/pengeluaran.
- `SummaryDao`, `SummaryService`, `SummaryFragment`, `BalanceService`, dan migration wallet
  operations: kondisi implementasi Finan saat riset dilakukan.
