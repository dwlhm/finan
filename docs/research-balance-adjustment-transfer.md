# Riset Fitur Penyesuaian Saldo dan Transfer

## Ringkasan

Finan sudah menyimpan `cached_balance_minor` pada wallet dan mengubahnya ketika transaksi
dibuat. Namun transaksi belum sepenuhnya menjadi source of truth karena saldo awal wallet
ditulis langsung ke cache. Saat saldo dihitung ulang, sistem hanya menjumlahkan transaksi,
sehingga saldo awal dapat hilang.

Sebelum menambahkan transfer, fondasi saldo perlu diperbaiki. Rekomendasi utama:

1. Simpan saldo awal sebagai `opening_balance_minor` pada wallet.
2. Catat penyesuaian saldo sebagai transaksi sistem bertipe `ADJUSTMENT`.
3. Catat transfer sebagai satu entitas transfer dengan dua sisi transaksi yang terhubung.
4. Jalankan pembuatan, perubahan, dan penghapusan seluruh sisi transfer dalam satu transaksi
   database.
5. Keluarkan penyesuaian dan transfer dari total pemasukan, pengeluaran, dan kategori.

## Kondisi Saat Ini

### Model saldo

- Wallet menyimpan `cached_balance_minor`.
- Transaksi hanya memiliki tipe `EXPENSE` dan `INCOME`.
- Saldo dihitung sebagai pemasukan dikurangi pengeluaran.
- Saldo awal hanya ditulis ke `cached_balance_minor` ketika wallet dibuat.
- Proses `recalculate(walletId)` mengabaikan saldo awal dan menghitung dari nol berdasarkan
  transaksi.
- Penyimpanan transaksi dan pembaruan cache saldo belum berada dalam satu transaksi database.

Konsekuensi: wallet dengan saldo awal Rp1.000.000 yang kemudian memiliki pengeluaran
Rp100.000 akan terlihat Rp900.000 setelah transaksi dibuat, tetapi dapat berubah menjadi
-Rp100.000 setelah transaksi diedit atau dihapus dan saldo dihitung ulang.

### Dampak model transaksi saat ini

- `category_id` wajib untuk semua transaksi.
- History hanya memfilter pemasukan atau pengeluaran.
- Adapter UI menganggap semua tipe selain `INCOME` adalah pengeluaran.
- Summary menghitung seluruh `EXPENSE` dan `INCOME` sebagai arus uang riil.
- CSV belum memiliki identitas transfer atau jenis transaksi selain pemasukan/pengeluaran.

Karena itu transfer tidak aman jika hanya dibuat sebagai satu pengeluaran dan satu pemasukan.
Cara tersebut memang mengubah saldo kedua wallet dengan benar, tetapi menggelembungkan laporan
pemasukan dan pengeluaran serta membutuhkan mekanisme untuk menjaga kedua transaksi tetap
sinkron.

## Tujuan Produk

### Penyesuaian saldo

User dapat menyamakan saldo wallet di Finan dengan saldo aktual tanpa harus menghitung selisih
sendiri.

Input minimum:

- Wallet.
- Saldo aktual.
- Tanggal dan waktu, default sekarang.
- Catatan opsional.

Sistem menampilkan:

- Saldo saat ini.
- Saldo aktual yang dimasukkan.
- Selisih yang akan dicatat.

Jika saldo saat ini Rp900.000 dan saldo aktual Rp875.000, sistem membuat adjustment
-Rp25.000. Jika tidak ada selisih, tidak ada perubahan yang disimpan.

### Transfer

User dapat memindahkan uang antar-wallet tanpa transfer tersebut dianggap sebagai pendapatan
atau pengeluaran.

Input minimum:

- Wallet sumber.
- Wallet tujuan.
- Jumlah.
- Tanggal dan waktu, default sekarang.
- Catatan opsional.

Validasi:

- Wallet sumber dan tujuan harus berbeda.
- Jumlah harus lebih dari nol.
- Kedua wallet harus ada.
- Untuk versi pertama, currency kedua wallet harus sama.
- Saldo negatif dapat tetap diizinkan agar konsisten dengan perilaku wallet saat ini.

Biaya transfer belum perlu masuk versi pertama. Nantinya biaya dapat dicatat sebagai transaksi
pengeluaran terpisah yang terhubung dengan transfer.

## Model Data yang Direkomendasikan

### Opening balance

Tambahkan pada tabel `wallets`:

```sql
opening_balance_minor INTEGER NOT NULL DEFAULT 0
```

Rumus saldo:

```text
balance = opening balance + seluruh delta transaksi wallet
```

Saat migrasi, nilai `opening_balance_minor` tidak boleh langsung diisi dari cached balance,
karena cache tersebut sudah mencakup transaksi. Nilainya perlu dihitung:

```text
opening balance lama = cached balance lama - jumlah delta transaksi lama
```

Setelah migrasi, hitung ulang cache seluruh wallet dan verifikasi hasilnya sama dengan cache
sebelum migrasi.

### Transaction kind

Pisahkan arah saldo dari makna bisnis. Pilihan sederhana untuk struktur saat ini:

```text
TransactionType: EXPENSE, INCOME, ADJUSTMENT, TRANSFER_OUT, TRANSFER_IN
```

Tambahkan nilai delta bertanda untuk adjustment, atau tambahkan `direction` terpisah. Agar
perubahan tetap kecil, adjustment dapat menyimpan jumlah absolut dan arah melalui field baru:

```sql
balance_delta_minor INTEGER
```

Namun desain yang lebih konsisten adalah menjadikan `amount_minor` sebagai delta bertanda untuk
jenis sistem. Ini berisiko karena kode saat ini mengasumsikan amount selalu positif.

Rekomendasi praktis: pertahankan `amount_minor` positif dan tambahkan tipe
`ADJUSTMENT_INCREASE` serta `ADJUSTMENT_DECREASE`, atau gunakan field `balance_effect` yang
eksplisit. Hindari menebak arah adjustment dari note atau category.

### Transfer identity

Tambahkan tabel:

```sql
CREATE TABLE transfers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_wallet_id INTEGER NOT NULL,
    destination_wallet_id INTEGER NOT NULL,
    amount_minor INTEGER NOT NULL,
    occurred_at INTEGER NOT NULL,
    note TEXT,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (source_wallet_id) REFERENCES wallets(id),
    FOREIGN KEY (destination_wallet_id) REFERENCES wallets(id),
    CHECK (source_wallet_id <> destination_wallet_id),
    CHECK (amount_minor > 0)
);
```

Tambahkan `transfer_id` nullable pada transactions. Satu transfer menghasilkan:

- `TRANSFER_OUT` pada wallet sumber.
- `TRANSFER_IN` pada wallet tujuan.

Kedua sisi harus memiliki `transfer_id`, jumlah, dan waktu yang sama. Transfer menjadi aggregate
root: edit dan delete dilakukan melalui `TransferService`, bukan melalui editor transaksi biasa.

Alternatif berupa satu baris transaction dengan dua wallet terlihat lebih ringkas, tetapi akan
memaksa hampir seluruh query saldo dan history memahami dua sisi sekaligus. Dua entry yang
terhubung lebih cocok dengan struktur query per-wallet saat ini.

### Category

Adjustment dan transfer bukan transaksi berkategori. `category_id` perlu dibuat nullable untuk
jenis sistem, sementara `EXPENSE` dan `INCOME` tetap mewajibkan category melalui validasi domain.

Jangan membuat category tersembunyi seperti "Transfer" atau "Penyesuaian". Itu mencampur jenis
operasi dengan tujuan pengeluaran dan berisiko muncul dalam laporan kategori.

## Aturan Saldo

Efek tiap jenis:

| Jenis | Efek saldo | Masuk laporan income/expense |
| --- | ---: | --- |
| `EXPENSE` | `-amount` | Expense |
| `INCOME` | `+amount` | Income |
| Adjustment increase | `+amount` | Tidak |
| Adjustment decrease | `-amount` | Tidak |
| `TRANSFER_OUT` | `-amount` | Tidak |
| `TRANSFER_IN` | `+amount` | Tidak |

Invariants:

- Cached balance selalu dapat dibangun ulang.
- Total efek transfer dengan currency sama adalah nol.
- Transfer tidak pernah memiliki hanya satu sisi.
- Edit atau delete transfer memperbarui kedua wallet secara atomik.
- Adjustment menyimpan selisih saat dibuat, bukan target saldo. Target saldo hanya input UI.
- Summary income dan expense tidak mencakup adjustment atau transfer.
- Top category tidak mencakup adjustment atau transfer.

## Alur UI

### Penyesuaian saldo

Lokasi yang disarankan: aksi pada setiap kartu wallet.

```text
Wallet > Penyesuaian saldo
→ tampilkan saldo saat ini
→ input saldo aktual
→ tampilkan selisih
→ konfirmasi
→ simpan adjustment
```

Label konfirmasi sebaiknya konkret, misalnya:

```text
Saldo Bank Jago akan berkurang Rp25.000 menjadi Rp875.000.
```

### Transfer

Lokasi yang disarankan: aksi sekunder pada layar wallet.

```text
Wallet > Transfer
→ pilih sumber
→ pilih tujuan
→ input jumlah
→ input biaya transfer (opsional)
→ simpan
```

#### Rekomendasi improvement: biaya transfer

Transfer antar-wallet dapat memiliki biaya admin. Field biaya bersifat opsional dan tidak menambah
langkah wajib pada transfer tanpa biaya.

Contoh:

```text
Transfer                         Rp500.000
Biaya transfer                    Rp2.500

Wallet sumber                   -Rp502.500
Wallet tujuan                   +Rp500.000
Arus kas transfer bersih                 0
Pengeluaran biaya transfer        Rp2.500
```

Kontrak perilaku:

- Nilai transfer tetap netral pada laporan arus kas.
- Biaya dicatat sebagai transaction expense biasa dan dapat memakai category default `Biaya admin`.
- User dapat memilih, mengganti, atau membuat category fee sendiri.
- Transaction fee terhubung dengan pasangan transfer agar create, edit, dan delete berjalan atomik.
- Mengubah biaya tidak mengubah nominal yang diterima wallet tujuan.
- Fee tidak disembunyikan di dalam nominal transfer karena akan mengurangi keterjelasan laporan.

History menampilkan transfer sebagai satu kejadian yang mudah dipahami:

```text
Transfer ke GoPay       -Rp100.000
Transfer dari Bank      +Rp100.000
```

Membuka salah satu sisi menampilkan detail transfer yang sama. Edit dan delete bekerja pada
aggregate transfer, bukan pada satu sisi saja.

## Dampak ke Komponen

### Domain dan service

- Perlu aturan balance yang mendukung semua jenis efek saldo.
- Perlu `AdjustmentService`.
- Perlu `TransferService`.
- Perlu unit of work atau akses transaksi database untuk operasi atomik.
- `TransactionService` harus menolak edit/delete langsung terhadap transaction yang terhubung
  ke transfer.

### Database

- Migration untuk opening balance, jenis transaksi baru, nullable category, transfer identity,
  dan tabel transfer.
- Index pada `transactions.transfer_id`.
- Pengujian migration harus memastikan saldo sebelum dan sesudah migration identik.

### Summary dan history

- Query saldo memasukkan semua jenis berdasarkan efek saldo.
- Query income/expense tetap hanya menghitung `INCOME` dan `EXPENSE`.
- Filter history perlu opsi "Transfer" dan "Penyesuaian", atau kategori tipe yang lebih umum.
- Count history boleh menghitung setiap sisi sebagai baris bila dilihat per-wallet, tetapi layar
  tanpa filter wallet sebaiknya mempertimbangkan penggabungan transfer agar tidak terlihat
  ganda.

### Export

- Naikkan versi format CSV.
- Tambahkan operation kind dan transfer identity.
- Ekspor opening balance wallet secara terpisah; ekspor transaksi saja tidak cukup untuk
  memulihkan saldo.
- Tentukan sejak awal apakah import akan mengembalikan transfer sebagai aggregate atau dua
  transaction terkait.

## Urutan Implementasi

### Fase 1: integritas saldo

1. Tambahkan opening balance sebagai source of truth.
2. Migrasikan data lama tanpa mengubah saldo.
3. Ubah semua kalkulasi saldo menjadi opening balance plus transaction deltas.
4. Tambahkan operasi database atomik untuk transaksi dan cache saldo.

### Fase 2: penyesuaian saldo

1. Tambahkan jenis adjustment dan validasinya.
2. Tambahkan service untuk menghitung dan menyimpan selisih.
3. Tambahkan aksi pada wallet.
4. Keluarkan adjustment dari report income/expense dan category.

### Fase 3: transfer

1. Tambahkan tabel transfer dan relasi transaction.
2. Implementasikan create, edit, dan delete atomik.
3. Tambahkan UI transfer dari layar wallet.
4. Tambahkan representasi history dan filter.
5. Perbarui export.

## Acceptance Criteria Inti

### Penyesuaian

- Mengubah target saldo dari Rp900.000 ke Rp875.000 menghasilkan delta -Rp25.000.
- Recalculate setelah adjustment tetap menghasilkan Rp875.000.
- Adjustment tidak mengubah total expense atau income.
- Adjustment dapat diedit atau dihapus tanpa merusak cache saldo.
- Target yang sama dengan saldo saat ini tidak membuat transaksi nol.

### Transfer

- Transfer Rp100.000 dari A ke B mengurangi A dan menambah B dengan jumlah yang sama.
- Total seluruh wallet dengan currency sama tidak berubah.
- Transfer tidak mengubah total expense atau income.
- Gagal menyimpan salah satu sisi membatalkan seluruh transfer.
- Edit jumlah dan wallet memperbarui semua wallet terdampak.
- Delete transfer menghapus kedua sisi dan memulihkan saldo.
- Sumber dan tujuan yang sama ditolak.
- Transfer beda currency ditolak pada versi pertama.

### Rekomendasi improvement: biaya transfer

- Transfer Rp500.000 dengan fee Rp2.500 menurunkan wallet sumber Rp502.500.
- Wallet tujuan tetap bertambah Rp500.000.
- Transfer tetap netral pada laporan, sedangkan fee masuk sebagai expense Rp2.500.
- User dapat mengedit nominal fee dan category fee.
- Menghapus transfer juga menghapus transaction fee yang terhubung setelah konfirmasi.

## Risiko dan Keputusan Lanjutan

- Multi-currency membutuhkan kurs dan dapat membuat total transfer tidak nol secara nominal.
- Biaya admin membutuhkan linked expense transaction seperti pada rekomendasi improvement fee
  transfer.
- Penggabungan dua sisi transfer dalam global history menambah kompleksitas paging.
- Undo transaksi terakhir harus memahami aggregate transfer agar tidak menghapus satu sisi.
- Category dan merchant usage tidak boleh bertambah untuk adjustment atau transfer.
- Perubahan enum tanpa pembaruan adapter akan membuat tipe baru tampil sebagai expense.

## Kesimpulan

Penyesuaian saldo layak dibuat lebih dulu karena memperbaiki kebutuhan rekonsiliasi sekaligus
memaksa fondasi saldo menjadi benar. Transfer sebaiknya menyusul setelah operasi transaction dan
cache sudah atomik.

Desain yang paling sesuai dengan struktur Finan saat ini adalah:

- Opening balance tersimpan sebagai data sumber.
- Adjustment tercatat sebagai transaksi sistem.
- Transfer adalah aggregate dengan dua transaction entries terhubung.
- Report hanya menganggap transaksi ekonomi nyata sebagai income atau expense.
