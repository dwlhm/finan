# Riset Perilaku User dan Fitur Investasi Finan

Tanggal riset: 13 Juni 2026
Status: Rekomendasi produk
Dokumen terkait:

- [`research-cash-flow-report.md`](research-cash-flow-report.md)
- [`design-manage-category.md`](design-manage-category.md)
- [`research-financial-behavior-and-product-direction.md`](research-financial-behavior-and-product-direction.md)
- [`product-principal.md`](product-principal.md)

## Ringkasan

Finan sebaiknya menangani investasi pertama-tama sebagai **alokasi dan pergerakan kas menuju aset
jangka panjang**, bukan sebagai aktivitas trading atau portofolio harga real-time.

Keputusan utama:

1. Pertahankan flow utama `Nominal -> Kategori -> Simpan`.
2. Gunakan klasifikasi `Aset & investasi` yang sudah dirancang sebagai pintu masuk, bukan membuat
   mode pencatatan baru.
3. Pada MVP, jawab berapa uang yang dialokasikan, dikembalikan, atau diterima dari investasi.
4. Sediakan detail investasi sejak pencatatan pertama melalui progressive disclosure. User dapat
   langsung mengisi akun, instrumen, jumlah lot/unit, harga, fee, dan goal, tetapi seluruh detail
   tersebut tetap opsional.
5. Investasi tidak memiliki layar pengelolaan atau flow khusus. Pencatatan dan pengeditan tetap
   memakai transaction, History, goal, dan flow umum Finan; perlakuan khusus hanya ada pada laporan.
6. Jangan menyebut setoran investasi sebagai konsumsi atau kerugian. Gunakan bahasa `Dana
   dialokasikan`.
7. Jangan menghitung setoran investasi sebagai nilai portofolio atau return.
8. Jangan membuat harga harian, rekomendasi aset, sinyal beli/jual, leaderboard, atau gamifikasi
   trading.
9. Data unit, lot, harga transaksi, dan fee boleh dicatat manual sejak awal. Valuasi pasar,
   perhitungan cost basis, dan return tetap membutuhkan fase serta aturan terpisah.
10. Nilai investasi tidak pernah dicampur dengan saldo kas dalam rekonsiliasi arus kas.
11. Goal seperti `Dana pensiun` dan `Pendidikan anak` dimiliki dan dikelola user. Finan boleh
    memberi contoh bawaan, tetapi user dapat menambah, mengedit, mengarsipkan, dan menghapusnya.
12. Sebagai improvement navigasi, layar terpisah adalah `Berita Finansial` yang read-only untuk
    kondisi nasional dan global. Layar tersebut tidak mengubah data dan hanya dapat mengarahkan
    user ke fitur Finan atau sumber berita.

Urutan nilai produk yang direkomendasikan:

```text
Pencatatan kas investasi yang benar
> memahami tujuan uang
> melihat konsistensi alokasi
> mengoreksi data dengan cepat
> nilai aktual berkala dengan sumber data
> laporan perubahan nilai berbasis data aktual
> integrasi harga atau broker
```

### Hubungan dengan arah produk umum

`research-financial-behavior-and-product-direction.md` menempatkan investment tracking dan market
price pada fase lanjut karena kompleksitasnya lebih besar daripada core capture. Dokumen ini tidak
mengubah keputusan tersebut untuk portfolio tracking.

Scope yang dinaikkan lebih awal hanya bagian yang sudah dekat dengan arus kas:

```text
Masuk sekarang
-> klasifikasi transaksi investasi
-> akun, instrumen, quantity, harga, fee, dan goal opsional
-> breakdown alokasi kas

Tetap ditunda
-> harga pasar
-> kalkulasi posisi dan cost basis otomatis
-> return otomatis
-> integrasi broker
```

Dengan batas ini, Finan dapat mengakui bahwa user berinvestasi tanpa berubah menjadi investment
tracker penuh.

## Pertanyaan Produk

Karena Finan sekarang dapat mengelompokkan transaksi sebagai `Aset & investasi`, ada dua makna
yang mudah tercampur:

1. **Aktivitas investasi dalam arus kas**
   Berapa uang keluar untuk membeli atau mendanai aset, dan berapa uang kembali?

2. **Kepemilikan dan performa investasi**
   Berapa nilai aset saat ini, untung atau rugi berapa, dan bagaimana alokasinya?

Data transaksi Finan sudah cukup untuk menjawab pertanyaan pertama. Data tersebut belum cukup
untuk menjawab pertanyaan kedua dengan benar.

Karena itu, prinsip scope-nya adalah:

> Finan tidak boleh mengubah arus kas menjadi klaim performa investasi tanpa data valuasi dan
> histori yang memadai.

## Temuan Riset

### 1. Investor Indonesia tumbuh, muda, dan sangat beragam

Statistik KSEI per akhir April 2026 mencatat sekitar 26,49 juta SID pasar modal. Investor individu
berusia 30 tahun ke bawah membentuk 54,63 persen dari investor individu. Kelompok usia ini
besar dalam jumlah, tetapi nilai aset tetap lebih terkonsentrasi pada kelompok yang lebih tua.

KSEI juga menunjukkan investor tersebar pada reksa dana, saham dan surat berharga lain, serta SBN.
Artinya, mental model produk tidak boleh hanya memakai istilah saham, lot, atau broker.

Implikasi untuk Finan:

- Gunakan istilah umum `Investasi`, `Akun investasi`, `Instrumen`, dan `Goal`.
- Sediakan jenis yang luas seperti reksa dana, saham, SBN, deposito, emas, kripto, properti, dan
  lainnya.
- Jenis instrumen hanya metadata opsional; user tetap dapat mencatat tanpa memahaminya.
- Jangan mendesain seolah semua user aktif memperjualbelikan aset.

### 2. Akses lebih tinggi daripada pemahaman

SNLIK 2025 OJK dan BPS mencatat indeks literasi keuangan nasional 66,46 persen dan inklusi 80,51
persen. Untuk sektor pasar modal, indeks literasi hanya 17,78 persen dan inklusi 1,34 persen.

Angka tersebut bukan ukuran kemampuan setiap investor Finan, tetapi menunjukkan bahwa bahasa dan
model produk tidak boleh mengasumsikan pengetahuan tentang return, volatilitas, cost basis,
diversifikasi, atau istilah pasar.

Implikasi:

- Utamakan bahasa konkret: `uang disetor`, `uang kembali`, `hasil diterima`, dan `nilai terakhir`.
- Jelaskan satu konsep pada saat dibutuhkan, bukan membuat kelas investasi sebelum user mencatat.
- Jangan memberi skor kesehatan portofolio yang terlihat ilmiah tetapi dibangun dari data tidak
  lengkap.
- `Belum ditentukan` harus menjadi state valid, sama seperti `Belum dikelompokkan` pada kategori.

### 3. User berinvestasi karena tujuan, kontrol, emosi, dan pengaruh sosial

Riset FCA terhadap self-directed investors menemukan perjalanan yang sangat personal dan
dipengaruhi tujuan, kondisi keuangan, tahap hidup, tingkat percaya diri, serta faktor sosial.
Sebagian user berorientasi pada tujuan jangka panjang. Sebagian lain tertarik karena kebaruan,
tantangan, rasa menjadi investor, hype, atau pengaruh orang yang dipercaya.

Riset FINRA Foundation yang dirilis Desember 2025 juga menunjukkan:

- 29 persen investor memakai media sosial sebagai sumber informasi.
- 26 persen memakai rekomendasi influencer dalam keputusan investasi.
- Angka penggunaan influencer mencapai 61 persen pada investor di bawah 35 tahun.
- Rata-rata responden menjawab 5,3 dari 11 pertanyaan pengetahuan investasi dengan benar.
- 34 persen merasa perlu mengambil risiko besar untuk mencapai tujuan keuangan; pada investor di
  bawah 35 tahun angkanya 62 persen.

Implikasi:

- Finan harus mengembalikan perhatian dari hype menuju `tujuan`, `jumlah yang dialokasikan`, dan
  `jangka waktu`.
- Finan tidak boleh menampilkan aset populer, tren, aktivitas user lain, atau social proof.
- Catatan investasi tidak boleh berubah menjadi rekomendasi produk.
- Microcopy harus membedakan pencatatan dari validasi: tercatat di Finan tidak berarti produk
  aman, cocok, atau berizin.

### 4. Percaya diri tidak selalu sama dengan kemampuan

FCA menemukan banyak self-directed investors mengaku percaya diri tetapi tidak memakai pendekatan
yang sistematis. Dalam sampel yang condong ke investasi berisiko tinggi, 45 persen bahkan tidak
menganggap kehilangan sebagian uang sebagai risiko investasi.

FINRA juga menemukan kesenjangan serupa. Contohnya, 75 persen responden yang menggunakan margin
menjawab pertanyaan pengetahuan tentang margin dengan salah.

Implikasi:

- Jangan menganggap banyaknya detail yang dimasukkan sebagai tanda kompetensi.
- Jangan memakai copy seperti `Portofolio aman`, `Risiko rendah`, atau `Strategi optimal`.
- Bila fitur risiko dibuat, gunakan pertanyaan dan edukasi yang dapat dijelaskan, bukan satu skor
  otomatis.
- Fitur utama harus tetap berguna bagi user yang hanya tahu nominal dan tujuan.

### 5. Aktivitas dan perhatian berlebihan dapat merugikan

Studi Barber dan Odean terhadap 66.465 rumah tangga menemukan kelompok yang paling sering
bertransaksi memperoleh return bersih tahunan jauh di bawah kelompok yang jarang bertransaksi.
Studi tersebut berasal dari data 1991-1996 dan tidak dapat dipakai untuk memprediksi return user
Finan saat ini, tetapi tetap memberi arah desain yang penting: produk pencatatan tidak perlu
mendorong frekuensi trading.

Riset mengenai myopic loss aversion juga menunjukkan evaluasi jangka sangat pendek dapat membuat
user terlalu berfokus pada kerugian sesaat. Karena itu, harga dan perubahan harian bukan default
yang netral untuk produk berorientasi tujuan jangka panjang.

Implikasi:

- Review default adalah bulanan, selaras dengan `Ringkasan`.
- Jangan memakai notifikasi perubahan harga, streak transaksi, confetti jual-beli, atau jumlah
  trade sebagai pencapaian.
- Jika valuasi tersedia, tampilkan tanggal nilai aktual dan tren periode panjang sebelum perubahan
  harian.
- Ukur keberhasilan dari kualitas pencatatan dan pemahaman, bukan frekuensi membuka portofolio.

### 6. User perlu menghubungkan investasi dengan kondisi kas

User dapat berinvestasi sambil tetap memiliki arus kas negatif, dana darurat tipis, atau kewajiban
jangka pendek. Sebaliknya, pembelian investasi yang tercatat sebagai pengeluaran biasa dapat
terlihat seperti konsumsi boros walaupun uang berpindah menjadi aset.

Ini adalah ruang yang paling sesuai dengan kekuatan Finan:

```text
Kas harian
-> dana dialokasikan
-> akun dan instrumen
-> goal personal
-> dampak pada arus kas
```

Finan dapat membantu user melihat hubungan tersebut tanpa menentukan aset mana yang harus dibeli.

## Model Mental Produk

Model mental utama Finan tetap:

```text
Wallet menyimpan kas.
Transaction mengubah saldo wallet.
Category menjelaskan tujuan uang.
Summary membantu melihat pola.
```

Investasi menambah tiga konsep sekunder yang tidak boleh dicampur:

```text
Akun investasi menjelaskan di mana aset disimpan.
Instrumen investasi menjelaskan aset apa yang dibeli.
Goal menjelaskan untuk tujuan personal apa aset tersebut dimiliki.
```

Contoh:

```text
Wallet: Bank BCA
Transaksi: -Rp1.804.500
Kategori: Investasi
Kelompok arus kas: Aset & investasi
Akun investasi: Stockbit
Instrumen: BBCA
Quantity: 2 lot / 200 lembar
Harga: Rp9.000 per lembar
Fee: Rp4.500
Goal: Dana pensiun
```

Aturan model:

- Wallet tetap menyimpan kas dan dapat direkonsiliasi.
- Akun investasi adalah tempat seperti Stockbit, Bibit, bank kustodian, atau penyimpanan emas.
- Instrumen adalah aset seperti BBCA, reksa dana tertentu, seri SBN, emas, atau BTC.
- Goal adalah tujuan personal seperti dana pensiun, pendidikan anak, rumah, atau tujuan custom.
- Satu akun dapat memiliki banyak instrumen.
- Satu instrumen dapat dikaitkan ke nol atau satu goal per transaksi pada MVP.
- Nilai investasi dapat berubah tanpa transaksi kas dan tidak masuk saldo wallet.
- Semua detail dapat ditambahkan atau dikoreksi setelah transaksi disimpan.

## Jobs to Be Done

```text
Ketika menyisihkan uang untuk investasi,
saya ingin mencatatnya secepat pengeluaran biasa,
agar arus kas saya tetap lengkap tanpa membuka spreadsheet lain.
```

```text
Ketika meninjau keuangan bulanan,
saya ingin mengetahui berapa uang yang dialokasikan ke investasi dan ke tujuan mana,
agar saya memahami prioritas uang saya.
```

```text
Ketika menerima uang dari investasi,
saya ingin membedakan uang yang kembali dari hasil yang diterima,
agar laporan tidak memberi kesan return yang keliru.
```

```text
Ketika memiliki investasi di beberapa tempat,
saya ingin melihat semuanya dalam struktur sederhana,
agar saya tidak harus mengingat setoran dari histori transaksi mentah.
```

```text
Ketika nilai investasi berubah,
saya ingin tahu kapan nilai terakhir diperbarui,
agar saya tidak menganggap angka lama sebagai harga saat ini.
```

## Keputusan Scope

### MVP: investment cash-flow tracking

MVP menjawab:

- Berapa dana dialokasikan ke investasi selama periode?
- Berapa dana kembali ke wallet?
- Berapa hasil investasi yang diterima dan ditandai secara eksplisit?
- Ke akun, instrumen, dan goal mana uang dialokasikan?
- Aset apa, berapa quantity, pada harga berapa, dan dengan fee berapa jika user memilih mengisi
  detail?
- Bagaimana alokasi investasi memengaruhi surplus atau defisit kas?
- Transaksi mana yang membentuk angka tersebut?

MVP tidak menjawab:

- Berapa nilai pasar saat ini?
- Berapa unrealized gain/loss?
- Berapa return persentase?
- Apakah portofolio terdiversifikasi dengan baik?
- Aset apa yang sebaiknya dibeli atau dijual?

### Fase berikutnya: laporan investasi berbasis data aktual

Fase berikutnya boleh menambahkan:

- Target dana dan tanggal target.
- Target setoran bulanan.
- Ringkasan konsistensi setoran.
- Nilai aktual per akun dan instrumen dengan sumber serta waktu observasi.
- Perubahan nilai nominal yang dihitung dari nilai aktual, bukan disimpulkan dari setoran.
- Laporan bulanan kenaikan atau penurunan nilai aset.
- Layar `Berita Finansial` read-only untuk konteks nasional dan global.

### Later scope: portfolio accounting

Hal berikut tidak sesuai untuk fase awal Finan:

- Sinkronisasi harga real-time.
- Kalkulasi average price dan tax lot otomatis.
- Corporate action.
- Order book atau jurnal trading.
- Perhitungan pajak investasi.
- Money-weighted dan time-weighted return tanpa data yang lengkap.
- Rekomendasi alokasi atau produk.
- Integrasi broker yang membuat capture bergantung pada network.

## Pergerakan Kas dan Aktivitas Aset

Kategori `Aset & investasi` menjelaskan kelompok arus kas. Pergerakan kas dan aktivitas aset perlu
dipisahkan agar pembelian internal setelah transfer ke RDN tidak dihitung dua kali.

Jenis pergerakan kas:

```text
CONTRIBUTION
WITHDRAWAL
YIELD
FEE
UNSPECIFIED
```

| Domain | Label UI | Arah kas umum | Makna |
| --- | --- | --- | --- |
| `CONTRIBUTION` | Setor dana | Keluar | Kas dipindahkan menuju akun investasi |
| `WITHDRAWAL` | Tarik dana | Masuk | Kas kembali dari akun investasi; belum tentu keuntungan |
| `YIELD` | Hasil diterima | Masuk | Dividen, kupon, bunga, bagi hasil, atau hasil lain |
| `FEE` | Biaya investasi | Keluar | Biaya, pajak, atau beban terkait |
| `UNSPECIFIED` | Belum ditentukan | Keduanya | Makna belum diketahui |

Jenis aktivitas aset:

```text
BUY
SELL
INCOME_REINVESTED
POSITION_ADJUSTMENT
UNSPECIFIED
```

`BUY` dan `SELL` mengubah posisi instrumen, tetapi tidak selalu mengubah wallet. Aktivitas dapat
ditautkan ke transaksi kas bila keduanya terjadi sebagai satu kejadian.

Default pergerakan kas:

```text
INVESTING + EXPENSE -> CONTRIBUTION
INVESTING + INCOME  -> WITHDRAWAL
```

User hanya memilih `YIELD` atau `FEE` ketika memang ingin membedakannya. Aktivitas `BUY` atau `SELL`
muncul ketika user membuka detail aset. Default tidak bergantung pada tebakan dari nama category.

### Batas klaim return

`WITHDRAWAL` tidak boleh otomatis disebut keuntungan.

Contoh:

```text
Setor Rp1.000.000
Tarik Rp400.000
```

Data itu hanya membuktikan alokasi bersih Rp600.000. Data tersebut tidak membuktikan bahwa user
untung atau rugi karena Finan belum mengetahui nilai aset yang tersisa.

Jika user menerima Rp50.000 dan menandainya sebagai `YIELD`, Ringkasan boleh menyatakan:

```text
Hasil investasi diterima Rp50.000
```

Finan tetap tidak boleh menyatakan persentase return tanpa basis nilai dan periode yang benar.

## Flow Pencatatan

Transaksi normal tidak berubah:

```text
Nominal -> Kategori -> Simpan
```

Jika category memiliki klasifikasi `INVESTING`, UI dapat menampilkan field opsional:

```text
Untuk investasi
[Pilih akun investasi]  Opsional
[Pilih goal]             Opsional
[+ Tambah detail aset]
```

Aturan:

- Tidak ada field investasi tambahan yang wajib untuk menyimpan.
- Aksi `Tambah detail aset` tersedia sebelum save, bukan hanya dari editor setelah transaksi
  dibuat.
- Field yang sudah dibuka tidak harus diisi lengkap; user dapat menyimpan data parsial.
- Default dapat memakai akun, instrumen, atau goal terakhir untuk konteks yang sama bila mudah
  dikoreksi.
- User dapat memilih `Belum ditentukan` untuk akun, instrumen, dan goal.
- Draft transaksi tidak hilang ketika membuat akun, instrumen, atau goal baru.
- Event kind memakai default dari arah transaksi dan tidak menambah langkah pada penggunaan biasa.
- Seluruh detail tersedia untuk diedit dari detail transaksi dan layar `Investasi`.
- Perubahan quantity, harga, fee, atau pajak menghitung ulang preview nominal, tetapi tidak
  menimpa nominal wallet tanpa konfirmasi user.

Contoh fast path:

```text
500000
-> kategori Investasi
-> simpan
```

Contoh pencatatan lengkap sejak awal:

```text
1.804.500
-> kategori Investasi
-> aksi Beli
-> akun Stockbit
-> instrumen BBCA
-> 2 lot / 200 lembar
-> Rp9.000 per lembar
-> fee Rp4.500
-> goal Dana pensiun
-> simpan
```

Pada contoh tersebut, satu aksi save membuat:

- Transaction `CONTRIBUTION` sebesar efek kas aktual pada wallet.
- InvestmentActivity `BUY` untuk BBCA sebanyak 200 lembar.
- Relasi antara keduanya agar laporan kas dan posisi dapat ditelusuri tanpa pencatatan ganda.

### Detail berdasarkan jenis instrumen

Finan menentukan field yang didukung untuk setiap jenis instrumen. User mengelola nilai datanya,
tetapi tidak perlu membuat schema field sendiri.

| Jenis | Detail yang didukung |
| --- | --- |
| Saham | Nama/kode emiten, lot, lembar per lot, harga per lembar |
| Reksa dana | Nama produk, unit penyertaan, NAB per unit |
| Obligasi/SBN | Seri, nilai nominal, harga beli, tanggal jatuh tempo |
| Deposito | Bank/produk, pokok, tenor, tanggal jatuh tempo, bunga opsional |
| Emas | Nama/penyedia, berat, satuan, harga per satuan |
| Kripto | Nama/simbol, quantity, harga per unit |
| Properti | Nama aset, porsi kepemilikan, nilai transaksi |
| Bisnis/lainnya | Nama aset, quantity dan satuan custom opsional |

Semua detail di atas opsional. Field spesifik baru muncul setelah jenis instrumen dipilih.

Kontrak aktivitas investasi:

```text
InvestmentActivity
- id
- cashTransactionId optional
- activityKind
- investmentAccountId optional
- investmentInstrumentId optional
- financialGoalId optional
- quantity optional
- quantityUnit optional
- unitsPerLot optional
- unitPriceMinor optional
- grossValueMinor optional
- feeMinor optional
- taxMinor optional
- maturityDate optional
- occurredAt
- createdAt
- updatedAt
```

`cashTransactionId` nullable karena pembelian atau penjualan dapat terjadi dari saldo yang sudah
berada di akun investasi. Nominal Transaction tetap merepresentasikan perubahan aktual pada wallet.
Jika activity ditautkan ke transaction, detail harus dapat direkonsiliasi:

```text
Beli:
cash out = gross value + fee + tax

Jual:
cash in = gross value - fee - tax
```

Jika perhitungan detail berbeda dari nominal wallet, Finan menampilkan selisih dan menawarkan:

```text
[Gunakan hasil perhitungan] [Pertahankan nominal]
```

Fee yang melekat pada pembelian atau penjualan disimpan pada detail transaksi yang sama.
Cash event `FEE` dipakai untuk biaya yang berdiri sendiri, misalnya biaya kustodian atau
biaya administrasi periodik.

### Pengeditan

User dapat mengedit:

- Cash event kind dan activity kind.
- Akun investasi.
- Instrumen dan jenisnya.
- Goal.
- Lot, unit, quantity, harga, fee, pajak, dan tanggal jatuh tempo.
- Nominal, wallet, tanggal, category, dan catatan transaction sesuai aturan editor transaction.

Edit harus memperbarui laporan dan agregasi posisi. Jika perubahan detail menghasilkan nominal kas
yang berbeda, Finan meminta konfirmasi sebelum mengubah saldo wallet. Menghapus transaction yang
memiliki activity terkait harus menawarkan apakah activity ikut dihapus atau dipertahankan sebagai
aktivitas tanpa relasi kas. Operasi yang dipilih berlangsung atomik.

### Transfer ke rekening investasi

MVP menggunakan aturan:

> Efek kas pada wallet dicatat satu kali; aktivitas aset boleh dicatat terpisah tanpa mengubah
> wallet lagi.

Jika user mentransfer Rp500.000 dari bank ke RDN lalu membeli BBCA dari saldo RDN:

1. Catat contribution/transfer Rp500.000 ketika uang meninggalkan wallet bank.
2. Catat activity `BUY BBCA` ketika pembelian terjadi.
3. Activity pembelian tidak kembali mengurangi wallet bank.

Dengan model ini user tetap dapat mengisi saham dan lot secara lengkap tanpa menggandakan arus kas.
Saldo kas internal RDN belum direkonsiliasi pada MVP. Dukungan ledger kas akun investasi menjadi
improvement terpisah jika kebutuhan tersebut tervalidasi.

### Rekomendasi improvement: fee transfer

Transfer antardompet biasa maupun transfer menuju akun investasi dapat memiliki biaya. Desain
transfer sebaiknya dikembangkan agar menerima `feeMinor` opsional.

Contoh:

```text
Transfer Bank A -> RDN       Rp500.000
Biaya transfer                Rp2.500

Efek wallet sumber          -Rp502.500
Efek wallet tujuan          +Rp500.000
Transfer bersih                    Rp0
Pengeluaran biaya             Rp2.500
```

Fee tidak boleh disembunyikan sebagai bagian nominal transfer karena total seluruh wallet memang
berkurang sebesar fee. Rekomendasi model:

- Transfer tetap aggregate dengan dua sisi bernilai sama.
- Fee menjadi expense terkait yang ikut dibuat, diedit, dan dihapus secara atomik bersama transfer.
- Category fee dipilih user atau memakai default user seperti `Biaya admin`; jangan membuat
  category sistem tersembunyi.
- Laporan menampilkan transfer bersih nol dan fee sebagai pengeluaran.
- Spesifikasi ini perlu ditambahkan ke `research-balance-adjustment-transfer.md` sebelum
  implementasi.

## Akun, Instrumen, dan Goal

Ketiga konsep dikelola terpisah karena menjawab pertanyaan berbeda:

| Konsep | Pertanyaan | Contoh |
| --- | --- | --- |
| Akun investasi | Disimpan atau ditransaksikan di mana? | Stockbit, Bibit, Bank Mandiri, brankas |
| Instrumen | Aset apa yang dimiliki? | BBCA, SBR013, emas Antam, BTC |
| Goal | Untuk tujuan personal apa? | Dana pensiun, pendidikan anak, rumah |

### Akun investasi

```text
InvestmentAccount
- id
- name
- providerName optional
- accountType optional
- currencyCode
- archived
- createdAt
- updatedAt
```

User dapat membuat, mengedit, dan mengarsipkan akun. Hapus permanen hanya tersedia jika akun belum
direferensikan transaksi atau nilai aktual; selain itu gunakan archive agar histori tetap dapat
dijelaskan.

### Instrumen investasi

```text
InvestmentInstrument
- id
- name
- symbol optional
- kind
- currencyCode
- defaultUnitsPerLot optional
- archived
- createdAt
- updatedAt
```

Jenis instrumen yang didukung Finan:

```text
MUTUAL_FUND
STOCK
BOND
DEPOSIT
GOLD
CRYPTO
PROPERTY
BUSINESS
OTHER
UNSPECIFIED
```

Finan menentukan struktur field yang tersedia per jenis. User dapat menambah, mengedit, dan
mengarsipkan instrumen miliknya. Perubahan nama atau simbol tidak mengubah label historis pada
transaksi lama tanpa pilihan eksplisit.

### Goal

Goal adalah area milik user, bukan taxonomy investasi yang ditentukan Finan.

```text
FinancialGoal
- id
- name
- targetAmountMinor optional
- targetDate optional
- monthlyContributionTargetMinor optional
- priority optional
- archived
- createdAt
- updatedAt
```

Finan boleh menyediakan contoh awal:

- Dana pensiun.
- Pendidikan anak.
- Rumah.
- Dana darurat.
- Tanpa goal.

Semua contoh tersebut dapat diubah atau dihapus. User selalu dapat menambah goal custom. Hapus goal
tidak menghapus transaksi atau aset; referensi historis menjadi `Tanpa goal` atau dipindahkan
setelah konfirmasi user.

Satu goal dapat menerima alokasi dari banyak akun dan instrumen. Satu instrumen yang sama juga
dapat digunakan untuk goal berbeda pada transaksi yang berbeda.

## Ringkasan Investasi

Pada MVP, tampilkan bagian ringkas di bawah breakdown `Aset & investasi` pada layar `Ringkasan`.

Contoh:

```text
Aset & investasi

Dana dialokasikan             Rp1.500.000
Dana kembali                    Rp300.000
Hasil diterima                    Rp50.000
Biaya dan pajak                   Rp10.000
Alokasi kas bersih             Rp1.160.000

Goal terbesar
Dana pensiun                     Rp800.000
Pendidikan anak                  Rp300.000
Tanpa goal                       Rp100.000

Instrumen terbesar
BBCA                             Rp700.000
SBR013                           Rp400.000
Emas                             Rp100.000

[Lihat transaksi investasi]
```

Rumus:

```text
Alokasi kas bersih
= contribution
+ fee dan pajak
- withdrawal
- yield
```

Untuk transaksi yang memiliki rincian aset, `contribution` dan `withdrawal` memakai nilai bruto
aset sebelum fee dan pajak. Fee dan pajak ditampilkan terpisah agar tidak dihitung dua kali. Jika
user hanya mencatat nominal kas tanpa rincian, seluruh nominal masuk sebagai contribution atau
withdrawal dan fee dianggap nol.

Nilai positif berarti lebih banyak kas dialokasikan ke investasi daripada kembali ke wallet pada
periode tersebut. Label ini tidak berarti kenaikan nilai aset.

Jika cash event atau activity kind belum lengkap:

```text
2 aktivitas investasi belum ditentukan
[Rapikan]
```

Tone tetap netral dan tidak menghalangi laporan.

### Rekomendasi improvement: layar Berita Finansial

Layar terpisah tidak menjadi layar investasi atau pusat pengelolaan data. Fungsinya adalah memberi
konteks read-only tentang kondisi dan perubahan finansial nasional serta global.

Struktur:

```text
Berita Finansial
├── Kondisi finansial nasional
├── Kondisi finansial global
├── Kebijakan dan indikator ekonomi
├── Perubahan pasar yang relevan
└── Sumber dan waktu pembaruan
```

Aturan:

- Tidak ada create, edit, delete, input nilai, atau perubahan data user pada layar ini.
- Setiap berita menampilkan sumber, waktu publikasi, dan waktu kejadian bila berbeda.
- Konten bersifat faktual dan netral; tidak ada rekomendasi beli/jual, trending asset, ranking,
  prediksi personal, atau social proof.
- Berita tidak otomatis mengubah nilai investasi, category, transaction, goal, atau laporan user.
- Action paling jauh adalah redirect seperti `Buka Ringkasan`, `Lihat laporan`, `Lihat History`,
  atau `Baca sumber`.
- Pengeditan tetap dilakukan pada fitur tujuan, transaction, atau History yang dituju oleh redirect,
  bukan di layar berita.
- Layar berita tidak menjadi syarat startup, pencatatan, maupun pembukaan laporan lokal.

### Konsistensi setoran

Setelah tersedia beberapa bulan data, Finan dapat menampilkan:

```text
Ada setoran pada 4 dari 6 bulan terakhir
Rata-rata setoran Rp650.000 per bulan aktif
```

Hindari:

- Streak yang hilang.
- Badge sempurna.
- Copy `Gagal investasi bulan ini`.
- Perbandingan dengan user lain.

Tujuannya membantu refleksi, bukan menciptakan tekanan atau mendorong transaksi demi menjaga
status.

## Goal Investasi

Target adalah atribut opsional pada `FinancialGoal`, bukan jenis akun atau instrumen.

Sebelum valuasi tersedia, progress harus diberi label:

```text
Dana yang pernah dialokasikan
```

bukan:

```text
Nilai tujuan saat ini
```

Karena setoran historis tidak sama dengan nilai aset saat ini.

Untuk target setoran bulanan, tampilkan kondisi faktual:

```text
Dialokasikan bulan ini Rp500.000 dari rencana Rp750.000
```

Jangan memakai bahasa menyalahkan atau otomatis menyarankan menambah risiko untuk mengejar target.

## Nilai Aktual pada Fase Lanjut

Jika kebutuhan valuasi tervalidasi, mulai dengan catatan nilai aktual yang sederhana. Nilai ini
boleh diinput user dari aplikasi broker/provider, diimport dari file, atau diambil dari integrasi
read-only. Prinsipnya: angka nilai tidak boleh dibuat dari inferensi setoran dan penarikan saja.

```text
Nilai investasi saat ini
[Rp12.500.000]

Per tanggal
[13 Juni 2026]

Sumber
[Diinput dari aplikasi broker]
```

Kontrak data:

```text
InvestmentActualValue
- id
- investmentAccountId optional
- investmentInstrumentId optional
- valueMinor
- observedAt
- sourceType
- sourceLabel optional
- note optional
- createdAt
- updatedAt
```

Aturan:

- Nilai aktual dapat dibuat per akun, per instrumen, atau per posisi akun-instrumen.
- Tampilkan `Diperbarui 13 Juni 2026`, bukan memberi kesan live.
- Tampilkan sumber nilai seperti `Diinput manual`, `Import file`, atau nama provider bila ada.
- Nilai lama tidak dihapus ketika nilai baru dimasukkan.
- Nilai dapat diedit atau dihapus dengan konfirmasi dan histori perubahan yang dapat dijelaskan.
- Mata uang berbeda tidak dijumlahkan tanpa conversion source yang eksplisit.
- Nilai investasi masuk overview kekayaan, bukan saldo kas atau rekonsiliasi wallet.
- Jika tidak ada nilai aktual, Finan tidak menampilkan angka nilai atau perubahan nilai.
- Jangan meminta update harian.
- Default ajakan update maksimal bulanan dan dapat diabaikan.

Perubahan nilai hanya dapat dihitung jika nilai aktual awal, nilai aktual akhir, dan arus kas
periode tersedia.

```text
Perubahan nilai bersih
= nilai aktual akhir
+ dana ditarik
+ hasil diterima
- nilai aktual awal
- dana ditambahkan
- fee dan pajak
```

Label harus menyebut bahwa angka dihitung dari data aktual yang tercatat. Persentase return dan
annualized return ditunda sampai dukungan multiple cash flow, periode, fee, dan data completeness
dirancang dengan benar.

### Rekomendasi improvement: laporan perubahan nilai bulanan

Kenaikan dan penurunan nilai aset lebih sesuai menjadi laporan bulanan daripada angka harian pada
home. Laporan ini adalah bagian dari pelaporan Finan, bukan layar pengelolaan investasi.

Contoh:

```text
Perubahan nilai investasi · Juni

Nilai aktual awal              Rp12.000.000
Dana ditambahkan               Rp1.000.000
Dana ditarik                     Rp300.000
Hasil diterima                    Rp50.000
Fee dan pajak                     Rp10.000
Nilai aktual akhir             Rp13.100.000
Perubahan nilai bersih            Rp440.000

Sumber nilai awal              Diinput manual · 1 Juni 2026
Sumber nilai akhir             Diinput manual · 30 Juni 2026
```

Rumus hasil ekonomis dari perspektif user:

```text
Perubahan nilai bersih
= nilai aktual akhir
+ dana ditarik
+ hasil diterima
- nilai aktual awal
- dana ditambahkan
- fee dan pajak
```

Aturan:

- Hanya hitung jika tersedia nilai aktual pembuka dan penutup serta data arus kas yang cukup.
- Tampilkan sumber, tanggal observasi, dan coverage data setiap nilai aktual.
- Jika data aktual tidak lengkap, tampilkan state `Belum cukup data aktual untuk menghitung
  perubahan nilai`.
- Drill-down memisahkan perubahan karena setoran/penarikan dari perubahan nilai aset.
- Default periode bulanan; tidak ada P&L harian di home.
- Laporan dapat dilihat per seluruh investasi, akun, instrumen, atau goal.
- Nilai positif/negatif tidak memakai copy yang mendorong user membeli atau menjual.

## Fitur yang Harus Dihindari

| Fitur | Alasan |
| --- | --- |
| Harga dan P&L harian di home | Menggeser capture-first dan mendorong perhatian jangka pendek |
| Trending asset | Memperkuat hype dan social proof |
| Rekomendasi beli/jual | Mengubah Finan menjadi pemberi saran investasi |
| Leaderboard return | Mendorong perbandingan sosial dan risiko |
| Streak transaksi | Menganggap lebih banyak transaksi selalu lebih baik |
| Confetti saat membeli aset | Menguatkan tindakan tanpa konteks kemampuan dan tujuan |
| Skor portofolio tunggal | Terlihat pasti walaupun data dan preferensi tidak lengkap |
| Return dari setoran dan penarikan saja | Secara matematis tidak valid tanpa nilai aset tersisa |
| Menjadikan investasi sebagai wallet biasa | Nilainya dapat berubah tanpa transaksi kas |
| Wajib memilih tujuan saat capture | Melanggar minimum required input |
| Menebak instrumen dari nama kategori | Dapat terlihat meyakinkan tetapi salah |
| Broker sync pada fase awal | Menambah network, keamanan, maintenance, dan kompleksitas domain |

## Hubungan dengan Kelola Kategori

`design-manage-category.md` tetap berlaku:

- Tipe transaksi menjelaskan arah uang.
- `Aset & investasi` menjelaskan kelompok arus kas.
- Cash event kind menjelaskan pergerakan kas investasinya.
- Activity kind menjelaskan perubahan posisi asetnya.
- Akun investasi menjelaskan tempat penyimpanan atau transaksi.
- Instrumen menjelaskan aset yang dibeli.
- Goal menjelaskan tujuan personal user.

Strukturnya:

```text
Category
├── type filter
└── cash flow activity

Transaction
├── amount dan direction
├── category snapshot
├── cash flow activity snapshot
└── investment cash event kind optional

InvestmentActivity
├── cash transaction link optional
├── activity kind
├── account optional
├── instrument optional
├── goal optional
├── quantity dan unit optional
├── unit price optional
└── fee dan tax optional
```

Editor kategori MVP laporan arus kas tidak perlu langsung ditambah field investasi. Ketika detail
investasi dibangun, pilihan default cash event hanya muncul untuk category `INVESTING` dan tetap
dapat dilewati. Detail akun, instrumen, goal, quantity, harga, dan fee berada pada
`InvestmentActivity` atau editor transaksi investasi, bukan category.

## Prioritas Fitur

### P0: fondasi yang sudah berjalan

1. Klasifikasi category dan transaction sebagai `INVESTING`.
2. Breakdown `Aset & investasi` pada laporan arus kas.
3. Drill-down ke transaction pembentuk angka.
4. Correction flow yang tidak mengubah histori diam-diam.

### P1: MVP investasi

1. `InvestmentAccount`, `InvestmentInstrument`, dan `FinancialGoal` sebagai entity terpisah
   dengan create, edit, archive, dan delete sesuai aturan referensi.
2. `InvestmentActivity` opsional dengan quantity, unit, harga, fee, dan pajak.
3. Cash event kind dengan default berdasarkan arah kas serta activity kind untuk `BUY`/`SELL`.
4. Breakdown dana dialokasikan, kembali, hasil, biaya, dan alokasi bersih.
5. Breakdown berdasarkan akun, instrumen, dan goal.
6. Inbox untuk transaksi `UNSPECIFIED`.
7. Create dan edit detail investasi sejak capture maupun setelah save.
8. Export seluruh field investasi.

### P1.1: kebiasaan dan tujuan

1. Target setoran bulanan opsional.
2. Jumlah bulan dengan setoran.
3. Perbandingan alokasi dengan periode sebelumnya.
4. Reminder lokal yang sepenuhnya opt-in.

### P2: nilai investasi

1. Catatan nilai aktual dengan sumber, tanggal observasi, dan waktu pencatatan.
2. Nilai terakhir hanya tampil jika ada data aktual.
3. Riwayat nilai aktual.
4. Laporan bulanan kenaikan/penurunan nilai berdasarkan nilai aktual awal dan akhir.
5. Overview kekayaan yang memisahkan kas dan investasi.

### Rekomendasi improvement lintas fitur

1. Fee transfer yang atomik bersama aggregate transfer dan masuk sebagai pengeluaran.
2. Layar `Berita Finansial` read-only untuk konteks nasional dan global.

### P3: hanya setelah kebutuhan kuat

1. Import file dari broker.
2. Integrasi read-only dengan provider.
3. Kalkulasi cost basis dan tax lot otomatis.
4. Return yang lebih lengkap.

Harga real-time, order, rekomendasi, dan social investing tetap di luar arah produk.

## Acceptance Criteria MVP Investasi

1. Pencatatan transaksi biasa tetap dapat selesai tanpa field investasi.
2. Category `INVESTING` memberi default cash event kind tanpa menambah tap wajib.
3. User dapat membuka dan mengisi detail lengkap sebelum save.
4. User dapat menyimpan transaksi investasi tanpa memilih akun, instrumen, atau goal.
5. Contribution, withdrawal, yield, dan fee memiliki arah kas yang valid.
6. Withdrawal tidak pernah otomatis disebut keuntungan.
7. Alokasi kas bersih dapat direkonsiliasi dengan transaction investasi periode tersebut.
8. Nilai investasi tidak mengubah saldo wallet.
9. Transfer atau pembelian internal broker tidak dihitung dua kali menurut aturan MVP.
10. Mata uang berbeda tidak dijumlahkan.
11. Breakdown dapat dibuka ke History dengan filter setara.
12. Transaction tanpa akun, instrumen, goal, atau event pasti tetap masuk total.
13. Ordinary capture time dan startup tidak menunggu query investasi.
14. Analytics tidak merekam nominal, nama goal, provider, atau instrumen user.
15. UI tidak menampilkan rekomendasi, social proof, atau klaim keamanan produk investasi.
16. User dapat mengedit seluruh detail investasi setelah save.
17. User dapat menambah, mengedit, mengarsipkan, dan menghapus goal sesuai aturan referensi.
18. Input quantity, harga, fee, dan pajak menampilkan rekonsiliasi terhadap efek kas wallet.

## Acceptance Criteria Rekomendasi Improvement

### Laporan nilai investasi

1. Laporan bulanan hanya menghitung perubahan nilai jika ada nilai aktual awal dan akhir.
2. Setiap nilai aktual menampilkan sumber, tanggal observasi, dan waktu pencatatan.
3. Setoran, penarikan, hasil, fee, dan pajak dipakai sebagai data arus kas pendukung, bukan sebagai
   pengganti nilai aktual.
4. Jika data aktual belum cukup, laporan menampilkan state `Belum cukup data aktual`.
5. Setiap angka dapat dibuka ke transaction, activity, atau catatan nilai aktual pembentuknya.

### Layar Berita Finansial

1. Layar `Berita Finansial` bersifat read-only dan tidak mengubah data user.
2. Tidak ada create, edit, delete, atau input nilai pada layar berita.
3. Action hanya berupa redirect ke fitur Finan yang relevan atau ke sumber berita.
4. Setiap item berita menampilkan sumber dan waktu publikasi.
5. Konten berita tidak berisi rekomendasi aset, ranking, prediksi personal, atau social proof.

### Fee transfer

1. Transfer Rp500.000 dengan fee Rp2.500 mengurangi sumber Rp502.500 dan menambah tujuan Rp500.000.
2. Transfer bersih tetap nol, sedangkan Rp2.500 masuk pengeluaran.
3. Create, edit, dan delete fee berlangsung atomik bersama aggregate transfer.
4. User dapat mengubah nominal fee dan category fee.
5. Fee transfer tidak dibuat sebagai category sistem tersembunyi.

## Metrik Evaluasi

Metrik utama tetap metrik capture Finan:

- Waktu mencatat transaksi biasa tidak meningkat.
- Save tetap lokal dan instan.
- Tidak ada field investasi wajib pada transaksi biasa.

Metrik fitur investasi:

| Metrik | Tujuan |
| --- | --- |
| Task completion pencatatan investasi | Menilai apakah flow tetap sederhana |
| Pemahaman `dana dialokasikan` vs `nilai saat ini` | Mencegah klaim dan model mental yang salah |
| Pemahaman laporan nilai berbasis data aktual | Memastikan user tahu angka nilai berasal dari data aktual |
| Correction success | Memastikan default mudah diperbaiki |
| Drill-down success | Memastikan angka laporan dapat dijelaskan |
| Proporsi transaction/activity dengan akun, instrumen, atau goal | Mengukur manfaat detail opsional |
| Salah tafsir withdrawal sebagai profit | Harus mendekati nol pada usability test |

Jangan memakai jumlah transaksi investasi atau frekuensi membuka Ringkasan sebagai proxy bahwa user
menjadi investor yang lebih baik.

## Riset Pengguna Lanjutan

Sebelum P1 diimplementasikan penuh, lakukan 8-12 wawancara dan usability test dengan variasi:

- Baru mulai dan sudah rutin.
- Reksa dana, saham, SBN, deposito, emas, kripto, atau properti.
- Satu dan beberapa provider.
- Setoran rutin dan transaksi tidak rutin.
- User yang hanya ingin melacak kas dan user yang juga ingin melihat nilai aset.

Pertanyaan riset:

1. Pada momen apa user merasa investasi perlu dicatat di Finan?
2. Apakah user mencatat saat transfer ke provider atau saat aset dibeli?
3. Bagaimana user membedakan penarikan modal, hasil investasi, dan penjualan?
4. Apakah struktur utama mereka berdasarkan tujuan, provider, atau instrumen?
5. Seberapa sering mereka benar-benar membutuhkan nilai terbaru?
6. Apakah target setoran membantu atau terasa seperti tekanan?
7. Copy mana yang paling jelas antara `Dana dialokasikan`, `Setoran`, dan `Pembelian investasi`?

Prototype harus menguji setidaknya:

1. Mencatat setoran investasi dari home.
2. Mencatat pembelian BBCA lengkap dengan lot, harga, fee, akun, dan goal sebelum save.
3. Menyimpan tanpa akun, instrumen, atau goal.
4. Mengedit quantity, harga, fee, instrumen, dan goal setelah save.
5. Mengaitkan transaksi lama ke akun, instrumen, dan goal.
6. Mencatat uang kembali tanpa menganggapnya profit.
7. Membaca Ringkasan investasi bulanan.
8. Membaca laporan perubahan nilai hanya ketika tersedia nilai aktual awal dan akhir.
9. Menjelaskan perbedaan alokasi kas, aktivitas aset, dan nilai investasi.
10. Memahami bahwa transfer ke RDN dan pembelian dari RDN tidak mengurangi wallet dua kali.
11. Membuka layar `Berita Finansial` dan memahami bahwa aksinya hanya redirect, bukan edit data.

## Risiko dan Mitigasi

### User menganggap investasi sebagai pengeluaran konsumtif

Mitigasi: tampilkan pada kelompok `Aset & investasi` dengan label `Dana dialokasikan`, terpisah
dari `Aktivitas harian`.

### User menganggap penarikan sebagai keuntungan

Mitigasi: default label `Dana kembali`, bukan `Untung`, dan hanya tampilkan `Hasil diterima` jika
ditandai eksplisit.

### User mencatat transfer dan pembelian dua kali

Mitigasi: satu aturan MVP yang terlihat pada bantuan kontekstual dan deteksi transaksi mirip sebagai
peringatan non-blocking pada fase lanjut.

### Detail investasi memperlambat capture

Mitigasi: seluruh detail opsional dan berada di balik progressive disclosure. User dapat mengisi
lengkap sejak awal, memakai default sebelumnya, atau merapikannya setelah save tanpa kehilangan
draft.

### Data valuasi terlihat live padahal sudah lama

Mitigasi: selalu tampilkan sumber, tanggal observasi, waktu pencatatan, dan state `Perlu
diperbarui` yang netral.

### Finan dianggap memberi saran investasi

Mitigasi: tidak ada rekomendasi aset, prediksi return, ranking produk, atau bahasa yang menyatakan
kecocokan dan keamanan.

### Scope berkembang menjadi aplikasi trading kompleks

Mitigasi: setiap fase harus tetap memenuhi pertanyaan apakah fitur membantu memahami uang dan tidak
mengganggu pencatatan. Harga real-time dan trading berada di luar positioning.

## Kesimpulan

Peran investasi yang paling sesuai untuk Finan adalah:

> Membantu user memahami berapa kas yang dialokasikan ke masa depan, untuk tujuan apa, dan
> bagaimana kebiasaan itu hidup berdampingan dengan kebutuhan keuangan hari ini.

Finan tidak perlu menang melawan aplikasi broker dalam harga, chart, atau eksekusi. Keunggulannya
adalah konteks yang tidak dimiliki broker: investasi terlihat bersama pemasukan, pengeluaran,
saldo kas, transfer, koreksi, dan tujuan personal.

Karena itu, MVP investasi adalah **investment cash-flow tracker dengan detail posisi opsional**.
Rekomendasi improvement menambahkan laporan perubahan nilai berbasis data aktual dan layar `Berita
Finansial` read-only. Pengelolaan data tetap terjadi melalui flow umum Finan, bukan lewat layar
investasi khusus.

## Sumber

- OJK dan BPS, [Hasil Survei Nasional Literasi dan Inklusi Keuangan
  2025](https://ojk.go.id/id/berita-dan-kegiatan/siaran-pers/Pages/OJK-dan-BPS-Umumkan-Hasil-Survei-Nasional-Literasi-Dan-Inklusi-Keuangan-SNLIK-Tahun-2025.aspx):
  literasi dan inklusi nasional, demografi, serta indeks sektor pasar modal.
- KSEI, [Statistik Pasar Modal Indonesia April
  2026](https://web.ksei.co.id/files/Statistik_Publik_April_2026.pdf): jumlah SID, komposisi
  investor, usia, penghasilan, instrumen, dan sebaran investor domestik.
- Financial Conduct Authority, [Understanding self-directed
  investors](https://www.fca.org.uk/publication/research/understanding-self-directed-investors.pdf):
  motivasi, archetype, pengaruh aplikasi dan media sosial, overconfidence, serta pemahaman risiko.
- FINRA Foundation, [Investors in the United States: Results from the National Financial
  Capability
  Study](https://www.finrafoundation.org/sites/finrafoundation/files/2025-11/NFCS_Investor_Survey_Report_White_Paper.pdf):
  perilaku investor 2024, sikap risiko, finfluencer, fraud, dan pengetahuan investasi.
- FINRA, [New FINRA Foundation Research Examines Shifting Investor Behaviors, Preferences and
  Attitudes](https://www.finra.org/media-center/newsreleases/2025/new-finra-foundation-research-examines-shifting-investor-behaviors):
  ringkasan temuan laporan investor yang dirilis 4 Desember 2025.
- Brad M. Barber dan Terrance Odean, [Trading Is Hazardous to Your
  Wealth](https://faculty.haas.berkeley.edu/odean/papers/returns/individual_investor_performance_final.pdf):
  hubungan frekuensi trading, biaya, overconfidence, dan performa investor individual.
- Ryan Wesslen et al., [Effect of uncertainty visualizations on myopic loss aversion and equity
  premium puzzle in retirement investment decisions](https://arxiv.org/abs/2107.02334): dampak
  horizon evaluasi dan visualisasi ketidakpastian terhadap keputusan investasi jangka panjang.
- IOSCO, [Investor Behaviour and Investor Education in Times of
  Turmoil](https://www.iosco.org/library/pubdocs/pdf/IOSCOPD724.pdf): perilaku investor ritel,
  edukasi, dan respons regulator pada kondisi pasar bergejolak.

## Batasan Riset

- Statistik KSEI menggambarkan akun dan aset yang tercatat, bukan seluruh perilaku harian atau
  investor aktif.
- SNLIK mengukur populasi Indonesia secara luas dan bukan usability produk investasi.
- Riset FCA berfokus pada Inggris dan sampelnya condong ke investasi berisiko tinggi.
- Riset FINRA berfokus pada investor Amerika Serikat dengan akun non-pensiun.
- Studi Barber dan Odean memakai data historis lama; angka return tidak boleh digeneralisasi ke
  kondisi Indonesia saat ini.
- Rekomendasi fitur di dokumen ini adalah inferensi gabungan dari sumber eksternal dan prinsip
  Finan. Rekomendasi tersebut perlu divalidasi melalui wawancara serta usability test lokal.
