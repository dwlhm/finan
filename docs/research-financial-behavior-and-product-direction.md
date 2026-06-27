# Riset Perilaku Finansial dan Arah Produk Finan

Tanggal riset: 13 Juni 2026

Sumber internal:

- [`concept.md`](concept.md)
- [`product-principal.md`](product-principal.md)
- [`design-principal.md`](design-principal.md)
- [`research-cash-flow-report.md`](research-cash-flow-report.md)
- [`design-manage-category.md`](design-manage-category.md)

## Ringkasan

Finan sebaiknya tidak berkembang menjadi suite finansial yang meminta user menyusun sistem
keuangan lengkap sebelum mendapat manfaat. Arah yang paling sesuai adalah menjadi **alat kontrol
keuangan ringan berbasis transaksi aktual**:

```text
Catat
→ cocokkan dengan saldo
→ pahami yang sudah terjadi
→ antisipasi yang akan terjadi
→ ambil satu tindakan kecil
```

Urutan tersebut memperluas manfaat Finan tanpa mengubah identitas capture-first. Home tetap
menjadi tempat pencatatan. Laporan, rencana, tujuan, dan insight berada di flow sekunder dan
memakai data yang sudah dikumpulkan user.

Rekomendasi utama:

1. Selesaikan integritas saldo, transfer, penyesuaian, laporan arus kas, dan klasifikasi category
   sebagai fondasi kepercayaan.
2. Tambahkan transaksi berulang sebagai **template dan pengingat**, bukan auto-post yang diam-diam
   mengubah saldo.
3. Tambahkan tampilan `Uang mendatang` untuk melihat pemasukan dan kewajiban terjadwal serta
   proyeksi saldo.
4. Tampilkan `Sisa setelah rencana`, bukan hanya saldo saat ini, dengan formula yang dapat dibuka
   dan dijelaskan.
5. Tambahkan `Ulangi transaksi` dan mode pencatatan susulan agar kegagalan mencatat beberapa hari
   tidak membuat user meninggalkan aplikasi.
6. Buat review bulanan singkat, deskriptif, dan dapat ditindaklanjuti tanpa skor atau bahasa
   menghakimi.
7. Tambahkan tujuan/dana cadangan sederhana setelah proyeksi arus kas stabil. Gunakan target
   nominal dan transfer antardompet, bukan budgeting kompleks.
8. Perlakukan backup, export, app lock, dan minimisasi data sensitif sebagai fitur produk inti,
   bukan pekerjaan teknis tambahan.
9. Tunda bank sync, OCR, investasi, utang lengkap, shared household, dan budgeting lanjutan sampai
   core loop terbukti dipakai.

## Sasaran Produk

Definisi financial well-being dari CFPB memiliki empat unsur:

1. Memiliki kontrol atas keuangan harian dan bulanan.
2. Mampu menyerap guncangan finansial.
3. Berada di jalur menuju tujuan finansial.
4. Memiliki kebebasan memilih cara menikmati hidup.

Finan tidak dapat menjamin hasil tersebut karena pendapatan, biaya hidup, tanggungan, dan kondisi
ekonomi berada di luar kendali aplikasi. Finan dapat membantu user menjalankan perilaku yang
mendukungnya:

| Sasaran | Peran Finan |
| --- | --- |
| Kontrol harian | Pencatatan cepat, saldo akurat, kewajiban terlihat |
| Menyerap guncangan | Dana cadangan dan pengeluaran tak rutin dapat direncanakan |
| Menuju tujuan | Target sederhana dengan progres yang dapat dijelaskan |
| Kebebasan memilih | Menunjukkan ruang yang tersedia tanpa menghakimi pilihan user |

North star Finan tetap:

> Mengurangi jarak antara kejadian transaksi dan pencatatannya.

Perluasan produk tidak mengganti north star tersebut. Ia membuat data hasil pencatatan lebih
berguna dalam keputusan sehari-hari.

## Konteks Pengguna

### Akses tidak sama dengan kemampuan mengelola

SNLIK 2024 OJK dan BPS mencatat indeks inklusi keuangan Indonesia sebesar 75,02%, lebih tinggi
daripada indeks literasi keuangan 65,43%. Survei mencakup pengetahuan, keterampilan, keyakinan,
sikap, dan perilaku, bukan sekadar kepemilikan produk.

Implikasi untuk Finan:

- Jangan menganggap user yang memakai bank atau e-wallet memahami istilah finansial formal.
- Bahasa, default, dan contoh lebih penting daripada menambah materi edukasi panjang.
- Produk harus membantu user melakukan tindakan yang benar pada saat dibutuhkan.
- UI tidak boleh bergantung pada kemampuan membaca tabel akuntansi.

### Kondisi finansial sering tidak stabil

Survei Federal Reserve terhadap rumah tangga AS pada 2024 memberi sinyal perilaku yang relevan
secara umum:

- 29% orang dewasa mengalami pendapatan yang berubah setidaknya sesekali dari bulan ke bulan.
- 11% mengalami kesulitan membayar tagihan karena variasi pendapatan.
- 19% membelanjakan lebih banyak daripada pendapatan pada bulan sebelumnya.
- 17% tidak membayar seluruh tagihan secara penuh pada bulan sebelumnya.
- 37% tidak dapat menutup pengeluaran darurat USD400 sepenuhnya dengan kas atau ekuivalennya.

Angka tersebut bukan estimasi untuk user Indonesia. Nilainya dipakai untuk menunjukkan bahwa
masalah finansial bukan hanya "terlalu banyak belanja". Timing pemasukan, kewajiban, dan kejadian
tak terduga sama pentingnya dengan total bulanan.

Implikasi:

- Saldo saat ini belum cukup untuk menjawab apakah uang benar-benar tersedia.
- Laporan masa lalu perlu dilengkapi pandangan ke depan.
- Produk harus mendukung pendapatan tidak tetap dan pengeluaran besar yang tidak muncul tiap
  bulan.
- Tone harus netral karena defisit dapat berasal dari kondisi, bukan kegagalan disiplin.

## Pola Perilaku dan Implikasi Desain

### 1. Niat tidak otomatis menjadi tindakan

Pengetahuan finansial saja tidak konsisten mengubah perilaku. Riset CFPB menekankan routine money
management, planning, goal-setting, dan kemampuan menindaklanjuti keputusan. Eksperimen Drexler,
Fischer, dan Schoar juga menemukan bahwa aturan praktis sederhana dapat lebih efektif daripada
pelatihan akuntansi formal bagi peserta dengan kemampuan awal lebih rendah.

Implikasi:

- Beri satu tindakan kecil, bukan kuliah finansial.
- Gunakan label seperti `Sisa setelah tagihan`, bukan rasio teknis tanpa konteks.
- Insight harus memiliki drill-down atau aksi konkret.
- Hindari rekomendasi panjang yang tidak dapat dijalankan di Finan.

### 2. Perhatian terhadap masa depan terbatas

Orang dapat mengingat kebutuhan hari ini tetapi melewatkan pengeluaran masa depan yang besar atau
jarang. Eksperimen Karlan, McConnell, Mullainathan, dan Zinman menunjukkan bahwa pengingat dapat
meningkatkan perhatian terhadap tujuan menabung, khususnya ketika pengingat menghubungkan tindakan
dengan kebutuhan tertentu.

Implikasi:

- Pengingat harus spesifik: `Tagihan internet diperkirakan besok`, bukan `Jangan lupa atur uang`.
- User memilih waktu dan jenis pengingat.
- Pengingat membuka aksi langsung: catat, tandai selesai, tunda, atau lewati.
- Jangan mengirim nominal atau detail sensitif pada lock screen secara default.

### 3. User memakai kantong mental

Orang cenderung memisahkan uang berdasarkan sumber atau tujuan: uang harian, tagihan, liburan,
darurat, dan lain-lain. Wallet dan category Finan sudah mendukung sebagian model mental ini.

Implikasi:

- Pertahankan wallet sebagai tempat uang dan category sebagai penjelas transaksi.
- Tujuan atau dana cadangan sebaiknya memakai target yang terhubung ke wallet/transfer.
- Jangan memaksa semua uang masuk ke sistem envelope yang kompleks.
- Total keseluruhan tetap tersedia agar kantong mental tidak menyembunyikan kondisi nyata.

### 4. Pencatatan manual mudah terputus

User mencatat setelah bertransaksi, sering dalam kondisi terburu-buru. Satu hari terlewat dapat
berubah menjadi banyak transaksi yang harus diingat. Jika proses mengejar ketertinggalan terlalu
berat, user cenderung menghindari aplikasi.

Implikasi:

- Pertahankan fast path normal.
- Tambahkan `Ulangi transaksi`, draft, dan edit terakhir.
- Sediakan pencatatan susulan yang cepat dengan tanggal tetap terlihat.
- Jangan memakai streak yang mengubah hari terlewat menjadi kegagalan.
- Ringkasan perlu menjelaskan kapan transaksi terakhir dicatat agar tidak memberi kesan data pasti
  ketika pencatatan mungkin belum lengkap.

### 5. User membutuhkan penjelasan, bukan hanya angka

Perbedaan antara saldo, arus kas, transfer, dan koreksi mudah disalahartikan. Perubahan klasifikasi
historis juga dapat merusak kepercayaan bila terjadi tanpa persetujuan.

Implikasi:

- Setiap angka penting harus dapat ditelusuri ke transaksi pembentuknya.
- Proyeksi harus memisahkan data aktual dan data terjadwal.
- Auto-category dan perubahan histori tidak boleh diterapkan diam-diam.
- Ketidakpastian harus terlihat, misalnya `berdasarkan 3 transaksi terjadwal`.
- Invariant rekonsiliasi harus diuji pada domain, bukan hanya dipercayakan pada UI.

### 6. Rasa aman lebih berguna daripada grafik yang ramai

Financial well-being tidak sama dengan pendapatan atau kekayaan. Rasa memiliki kontrol,
kemampuan menghadapi kejutan, dan kebebasan memilih juga penting.

Implikasi:

- Prioritaskan saldo yang dapat dipercaya, kewajiban mendatang, dan buffer.
- Gunakan grafik hanya jika membantu pertanyaan spesifik.
- Bandingkan user dengan histori dirinya sendiri, bukan benchmark orang lain.
- Jangan memberi skor kesehatan finansial tunggal yang menyederhanakan kondisi user.

## Core Loop Finan

```text
CAPTURE
Nominal → kategori → wallet → simpan

RECONCILE
Saldo aktual → transfer → penyesuaian → saldo dapat dipercaya

UNDERSTAND
Arus kas → kategori → perubahan dari periode sebelumnya

ANTICIPATE
Pemasukan terjadwal → tagihan → pengeluaran tak rutin → proyeksi saldo

ACT
Catat → transfer ke dana tujuan → ubah rencana → buat pengingat
```

Aturan arsitektur informasi:

- `Home`: capture.
- `Riwayat`: menemukan dan memperbaiki transaksi.
- `Wallet`: saldo, transfer, dan penyesuaian.
- `Ringkasan`: memahami masa lalu dan mengantisipasi masa dekat.
- `Pengaturan`: preferensi, kategori, export, backup, privasi.
- Jangan menambah bottom navigation baru untuk setiap tahap.

## Rekomendasi Fitur

### Prioritas 0: fondasi kepercayaan

#### 1. Saldo dan laporan yang dapat direkonsiliasi

Lanjutkan keputusan pada riset arus kas:

- Opening balance sebagai source of truth.
- Transfer tidak dihitung sebagai pemasukan/pengeluaran.
- Adjustment dipisahkan dari transaksi regular.
- Laporan dikelompokkan per mata uang.
- Saldo awal sampai saldo akhir dapat dijelaskan.
- Klasifikasi activity menjadi metadata laporan, bukan langkah wajib capture.

Tanpa fondasi ini, fitur prediksi, tujuan, dan budget akan memperbesar ketidakakuratan.

#### 2. Koreksi cepat

Wajib tersedia:

- Undo setelah save.
- Edit transaksi terakhir.
- Ubah category dan wallet dengan cepat.
- Hapus transaksi recent.
- Edit/delete transfer melalui aggregate, bukan salah satu sisinya.

#### 3. Export, backup, dan pemulihan

Minimum:

- Export yang mencakup wallet, opening balance, transaction kind, transfer identity, category, dan
  activity snapshot.
- Backup lokal lengkap dengan versi format.
- Restore yang memvalidasi checksum, versi, referensi, dan saldo.
- User dapat menghapus seluruh data dari perangkat.

#### 4. Privasi perangkat

Minimum:

- App lock opsional menggunakan kemampuan keamanan perangkat.
- Isi notifikasi sensitif disembunyikan secara default.
- Analytics tidak memuat nominal, note, merchant, category custom, atau saldo.
- Error log memakai identifier teknis yang tidak dapat merekonstruksi transaksi.

### Prioritas 1: membantu masa dekat

#### 5. Transaksi berulang sebagai template

Use case:

- Gaji.
- Sewa/kos.
- Listrik, internet, telepon.
- Cicilan.
- Langganan.
- Uang sekolah atau dukungan keluarga.

Model awal:

```text
RecurringTemplate
- transactionType
- amountMinor nullable
- walletId
- categoryId
- frequency
- nextOccurrence
- reminderOffset
- noteTemplate nullable
```

Perilaku:

- Nominal boleh tetap atau diisi saat konfirmasi.
- Tanggal berikutnya dapat diedit.
- Saat jatuh tempo, Finan menawarkan `Catat sekarang`.
- Transaksi baru hanya tercipta setelah user mengonfirmasi.
- `Lewati kali ini` tidak menghapus template.

Auto-post dapat dipertimbangkan nanti hanya sebagai opsi eksplisit untuk transaksi yang benar-benar
tetap. Default tetap konfirmasi agar saldo merepresentasikan kejadian aktual.

#### 6. Uang mendatang

Tambahkan bagian pada `Ringkasan`, bukan bottom navigation baru:

```text
Uang mendatang · 30 hari

Saldo sekarang                 Rp3.200.000
Pemasukan terjadwal           +Rp4.500.000
Pengeluaran terjadwal         -Rp3.100.000
                              ------------
Perkiraan saldo                Rp4.600.000
```

Aturan:

- Aktual dan terjadwal selalu dibedakan.
- User dapat mengubah horizon 7, 30, atau sampai akhir bulan.
- Proyeksi per mata uang dan per wallet.
- Jangan memasukkan transaksi perkiraan ke saldo aktual.
- Tekan baris untuk melihat komponen pembentuknya.

#### 7. Sisa setelah rencana

Saldo besar dapat memberi rasa aman palsu bila tagihan belum dibayar. Finan dapat menampilkan:

```text
Sisa setelah rencana
Rp1.500.000

Saldo sekarang Rp3.200.000
dikurangi Rp1.700.000 pengeluaran terjadwal sampai 30 Juni
```

Gunakan `Sisa setelah rencana`, bukan `Aman dibelanjakan`, karena:

- Finan mungkin belum mengetahui semua kewajiban.
- Pencatatan manual dapat belum lengkap.
- Produk tidak boleh memberi jaminan yang lebih kuat daripada datanya.

#### 8. Ulangi dan pencatatan susulan

Tambahkan:

- `Ulangi` pada transaksi recent.
- Suggestion berdasarkan kombinasi wallet, category, merchant, dan nominal yang sering dipakai.
- Multi-add ringan untuk transaksi yang terlewat tanpa membuka form panjang berulang kali.
- Tanggal transaksi tetap jelas dan mudah diganti.
- Tidak ada auto-save dari suggestion.

#### 9. Review bulanan singkat

Review menjawab maksimal empat pertanyaan:

1. Pemasukan lebih besar atau pengeluaran lebih besar?
2. Mengapa saldo berubah?
3. Kategori mana yang paling berubah dibanding bulan lalu?
4. Apa yang perlu diperhatikan pada bulan berikutnya?

Contoh:

```text
Juni sejauh ini

Pemasukan lebih besar Rp850.000
Saldo bertambah Rp800.000 setelah koreksi saldo.

Transport Rp220.000 lebih tinggi dari Mei.
Tagihan Rp1.200.000 terjadwal dalam 7 hari.
```

Semua insight harus:

- Deskriptif, bukan diagnosis.
- Dibandingkan dengan histori user sendiri.
- Memiliki drill-down.
- Tidak tampil bila data pembanding tidak cukup.
- Tidak memakai label `boros`, `buruk`, `gagal`, atau skor moral.

### Prioritas 2: ketahanan dan tujuan

#### 10. Dana cadangan dan tujuan sederhana

Mulai dari target nominal:

```text
Dana darurat
Rp3.500.000 dari target Rp6.000.000
```

Aturan:

- User menentukan nama dan target.
- Progres berasal dari saldo wallet khusus atau alokasi yang eksplisit.
- Menambah dana memakai transfer, bukan expense.
- Finan tidak memaksa target tiga atau enam bulan.
- Setelah data cukup, Finan boleh menampilkan konteks opsional seperti `setara 1,4 bulan
  pengeluaran rata-rata`, dengan formula yang dapat dibuka.

#### 11. Pengeluaran tak rutin

Gunakan tujuan berjangka untuk pajak kendaraan, sekolah, mudik, servis, atau premi tahunan:

```text
Pajak kendaraan
Target Rp2.400.000 · Desember
Saran rencana Rp400.000 per bulan
```

Nilai saran tidak memindahkan uang otomatis. Aksi utama:

- Transfer sekarang.
- Ubah target.
- Ingatkan nanti.

#### 12. Rencana pengeluaran opsional

Budget category hanya masuk setelah laporan category stabil.

Desain awal:

- User memilih beberapa category yang ingin dipantau.
- Default berdasarkan nominal user, bukan persentase universal.
- Carry-over opsional, bukan default.
- Melewati rencana tidak memblokir pencatatan.
- Status memakai bahasa `tersisa` atau `melewati rencana`, bukan alarm moral.

Ini adalah guardrail, bukan zero-based budgeting penuh.

### Prioritas 3: perlu validasi lebih lanjut

- Pencatatan utang dan piutang sebagai liability/receivable, bukan negative wallet.
- Net worth yang memisahkan kas, aset, dan kewajiban.
- Import CSV dengan inbox rekonsiliasi.
- OCR struk sebagai prefill yang wajib dikonfirmasi.
- Bank sync opsional dengan consent dan model ancaman yang jelas.
- Shared household dengan ownership, conflict resolution, dan privasi antaranggota.
- Investment tracking dan market price.
- Rule automation untuk category dan merchant.

Fitur tersebut berguna, tetapi membawa kompleksitas domain, privasi, sinkronisasi, atau sumber data
yang lebih besar daripada manfaat awalnya bagi capture-first tracker.

## Fitur yang Tidak Direkomendasikan Sekarang

| Fitur | Alasan |
| --- | --- |
| Dashboard berat sebagai home | Menghalangi capture |
| Setup budget wajib | Menunda transaksi pertama |
| Streak harian | Mengubah hari terlewat menjadi rasa gagal |
| Skor kesehatan finansial tunggal | Menyederhanakan situasi dan sulit dijelaskan |
| Benchmark terhadap user lain | Tidak relevan dengan konteks pribadi |
| Auto-post seluruh transaksi berulang | Saldo dapat berubah sebelum kejadian aktual |
| Auto-category tanpa konfirmasi/koreksi | Mengurangi kepercayaan laporan |
| Insight generatif yang memberi nasihat investasi | Risiko klaim, privasi, dan ketidakakuratan |
| Bank sync sebagai syarat | Bertentangan dengan local-first dan immediate use |
| Nested category dalam | Memperpanjang capture dan perawatan |
| Split transaction wajib | Menambah beban pada transaksi normal |
| Notifikasi agresif | Mendorong avoidance, bukan kebiasaan kecil |

## Urutan Implementasi yang Direkomendasikan

### Fase A: data dapat dipercaya

1. Integritas opening balance, transfer, adjustment, dan operasi atomik.
2. Laporan arus kas yang dapat direkonsiliasi.
3. Manage category dan activity classification.
4. Export/backup versi baru.
5. Audit privacy dan log.

### Fase B: kebiasaan tetap ringan

1. Undo dan edit transaksi terakhir.
2. Ulangi transaksi.
3. Draft yang selamat dari interruption.
4. Pencatatan susulan cepat.
5. Indikator transaksi terakhir tanpa streak.

### Fase C: melihat ke depan

1. Recurring template.
2. Reminder opsional.
3. `Uang mendatang`.
4. `Sisa setelah rencana`.
5. Review bulanan.

### Fase D: ketahanan

1. Tujuan nominal.
2. Dana cadangan.
3. Pengeluaran tak rutin.
4. Rencana category opsional.

### Fase E: perluasan

Validasi utang, import, sync, shared household, dan investasi satu per satu. Jangan menjalankannya
sebagai satu paket "financial management".

## Acceptance Criteria Produk

### Capture tidak memburuk

- Expense normal tetap dapat disimpan tanpa membuka report atau plan.
- Tidak ada field recurring, goal, atau activity pada fast path.
- Median tap dan waktu capture tidak meningkat dari baseline.
- Semua suggestion dapat diabaikan.

### Angka dapat dipercaya

- Saldo aktual tidak memasukkan transaksi terjadwal.
- Proyeksi selalu menunjukkan horizon dan jumlah item pembentuknya.
- Report memenuhi invariant rekonsiliasi.
- Multi-currency tidak pernah dijumlahkan tanpa kurs historis.
- Transaction override tidak diubah oleh bulk update category.

### User tetap memegang kontrol

- Reminder opt-in dan dapat dihentikan per template.
- Transaksi recurring default-nya dikonfirmasi.
- User dapat edit, delete, export, backup, restore, dan menghapus data.
- Tidak ada perubahan histori diam-diam.

### Tone tetap tenang

- Tidak ada copy yang memberi rasa bersalah.
- Defisit dilaporkan sebagai kondisi, bukan karakter user.
- Empty state tidak memaksa setup lanjutan.
- Keterlambatan mencatat tidak menghapus progres atau menampilkan kegagalan.

## Pengukuran

### Metric utama

- Median waktu dari app ready sampai transaksi tersimpan.
- Median jumlah tap untuk expense normal.
- Save latency lokal.
- Persentase transaksi yang dikoreksi dalam 24 jam.
- Retensi user yang mencatat pada beberapa minggu berbeda.

### Metric manfaat sekunder

- Persentase user Ringkasan yang membuka drill-down.
- Persentase recurring reminder yang berakhir pada catat, lewati, atau ubah.
- Persentase proyeksi yang dibuka sebelum tanggal kewajiban.
- Persentase user yang melakukan transfer ke tujuan setelah melihat review.
- Persentase report yang lulus invariant rekonsiliasi: target 100%.

### Guardrail

- Waktu capture tidak meningkat.
- Crash-free save dan restore.
- Jumlah notifikasi per user tetap sesuai pilihan user.
- Tidak ada nominal atau teks transaksi pada telemetry.
- Tidak ada kenaikan transaksi salah akibat suggestion atau recurring.

Metric tidak boleh menganggap:

- Banyak transaksi selalu lebih baik.
- Semua user harus membuka report.
- Semua category harus diklasifikasikan.
- Semua user harus membuat budget atau tujuan.

## Rencana Validasi Pengguna

Riset ini menggunakan sumber lintas negara dan prinsip behavioral finance. Ia belum menggantikan
observasi terhadap user Finan.

Validasi berikutnya:

1. Rekrut 8-12 orang yang saat ini mencatat manual, pernah berhenti mencatat, atau memiliki
   beberapa bank/e-wallet.
2. Minta mereka memakai build capture-first selama 14 hari.
3. Wawancarai momen transaksi terlewat, koreksi saldo, tagihan, pendapatan tidak tetap, dan
   pengeluaran tak rutin.
4. Uji prototype `Uang mendatang` dan `Sisa setelah rencana`.
5. Periksa apakah user memahami perbedaan aktual, terjadwal, dan proyeksi tanpa penjelasan lisan.
6. Ukur apakah recurring confirmation mengurangi input berulang tanpa membuat saldo palsu.
7. Uji copy netral pada kondisi surplus, defisit, data belum lengkap, dan target terlewat.

Pertanyaan riset terpenting:

- Kapan user merasa saldo tidak cukup untuk mengambil keputusan?
- Kewajiban apa yang paling sering terlupakan?
- Apakah user lebih memahami waktu melalui kalender, daftar, atau angka ringkas?
- Bagaimana user memisahkan uang secara mental: wallet, category, amplop, atau tujuan?
- Apa yang membuat user kembali setelah beberapa hari tidak mencatat?
- Data apa yang dianggap terlalu sensitif untuk notifikasi, backup, atau analytics?

## Sumber Eksternal

1. OJK dan BPS, [Hasil Survei Nasional Literasi dan Inklusi Keuangan Tahun
   2024](https://ojk.go.id/id/berita-dan-kegiatan/siaran-pers/Pages/OJK-dan-BPS-Umumkan-Hasil-Survei-Nasional-Literasi-dan-Inklusi-Keuangan-Tahun-2024.aspx),
   2 Agustus 2024.
2. CFPB, [Financial well-being: The goal of financial
   education](https://files.consumerfinance.gov/f/201501_cfpb_report_financial-well-being.pdf),
   Januari 2015.
3. CFPB, [Measuring financial well-being: A guide to using the CFPB Financial Well-Being
   Scale](https://www.consumerfinance.gov/data-research/research-reports/financial-well-being-scale/).
4. Federal Reserve, [Report on the Economic Well-Being of U.S. Households in 2024: Income and
   Expenses](https://www.federalreserve.gov/publications/2025-economic-well-being-of-us-households-in-2024-income-and-expenses.htm),
   Mei 2025.
5. Federal Reserve, [Report on the Economic Well-Being of U.S. Households in 2024: Savings and
   Investments](https://www.federalreserve.gov/publications/2025-economic-well-being-of-us-households-in-2024-savings-and-investments.htm),
   Mei 2025.
6. OECD, [OECD/INFE 2023 International Survey of Adult Financial
   Literacy](https://www.oecd.org/en/publications/oecd-infe-2023-international-survey-of-adult-financial-literacy_56003a32-en.html),
   14 Desember 2023.
7. Drexler, Fischer, dan Schoar, [Keeping It Simple: Financial Literacy and Rules of
   Thumb](https://www.aeaweb.org/articles?id=10.1257/app.6.2.1), American Economic Journal:
   Applied Economics, 2014.
8. Karlan, McConnell, Mullainathan, dan Zinman, [Getting to the Top of Mind: How Reminders
   Increase Saving](https://www.nber.org/papers/w16205), NBER Working Paper 16205, 2010;
   diterbitkan di Management Science pada 2016.

## Kesimpulan

Finan tidak perlu memenangkan kompetisi jumlah fitur. Ia perlu menjadi alat yang paling ringan
untuk menjaga gambaran uang tetap dapat dipercaya.

Arah produk yang direkomendasikan:

```text
Capture-first tetap menjadi inti.
Saldo dan laporan membangun kepercayaan.
Rencana mendatang membangun kontrol.
Tujuan sederhana membangun ketahanan.
User tetap memegang keputusan.
```

Fitur terbaik bagi Finan bukan fitur yang paling canggih, tetapi fitur yang mengubah data transaksi
menjadi satu keputusan kecil yang jelas tanpa membuat transaksi berikutnya lebih sulit dicatat.
