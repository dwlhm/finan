# Concept: Capture-First Financial Tracker

## Summary

Aplikasi ini adalah financial tracker yang berfokus pada kecepatan pencatatan transaksi harian.

Masalah utama yang ingin diselesaikan bukan analisis finansial kompleks, melainkan friction ketika user ingin mencatat pengeluaran atau pemasukan. Banyak aplikasi finansial terasa lambat, terlalu banyak menu, terlalu banyak field, dan tidak langsung membawa user ke aksi utama: mencatat transaksi.

Aplikasi ini mengambil pendekatan capture-first: buka aplikasi, input nominal, pilih kategori, simpan.

## Problem

User sering gagal konsisten mencatat transaksi karena proses pencatatan terasa berat.

Masalah yang umum terjadi:

- Aplikasi lambat saat dibuka.
- User harus melewati dashboard sebelum mencatat.
- Tombol tambah transaksi tidak langsung terlihat.
- Form terlalu panjang.
- Terlalu banyak field wajib.
- Terlalu banyak pilihan sebelum transaksi bisa disimpan.
- User harus mengingat transaksi setelah beberapa waktu, bukan mencatat langsung saat kejadian.

Akibatnya, pencatatan finansial menjadi aktivitas yang terasa seperti beban, bukan kebiasaan ringan.

## Core Problem Statement

User membutuhkan cara mencatat transaksi finansial harian dengan sangat cepat, tanpa harus melewati banyak langkah, loading panjang, atau form yang kompleks.

## Product Positioning

Aplikasi ini bukan financial management suite yang kompleks.

Aplikasi ini adalah:

> Fast, simple, and direct financial logger.

Fokus utama aplikasi adalah membantu user mencatat transaksi sebelum rasa malas muncul.

## Target User

Target utama:

- Orang yang ingin mencatat pengeluaran harian.
- Orang yang sering lupa uangnya habis untuk apa.
- Orang yang malas menggunakan aplikasi finansial karena terlalu ribet.
- Orang yang ingin tracking sederhana tanpa spreadsheet.
- Orang yang butuh pencatatan cepat saat sedang bergerak, antre, bekerja, atau buru-buru.

Target sekunder:

- User yang memiliki beberapa sumber uang seperti cash, bank, dan e-wallet.
- User yang ingin melihat ringkasan pengeluaran bulanan secara sederhana.
- User yang ingin mulai membangun kebiasaan finansial tanpa sistem budgeting kompleks.

## Core Value Proposition

Aplikasi ini membantu user mencatat transaksi dalam hitungan detik.

Nilai utama aplikasi:

- Cepat dibuka.
- Langsung masuk ke pencatatan.
- Tidak memaksa input data lengkap.
- Bisa mencatat transaksi dengan sedikit langkah.
- Tetap berguna walaupun user hanya mengisi nominal dan kategori.
- Tidak membuat user merasa sedang mengisi laporan keuangan.

## Main User Flow

Flow utama aplikasi:

```txt
Open App
→ Input Amount
→ Choose Category
→ Save
````

Contoh:

```txt
25000
→ Food
→ Saved
```

Target flow:

* Expense dapat dicatat dalam 2–3 tap.
* Input nominal langsung aktif saat aplikasi dibuka.
* Wallet, tanggal, dan tipe transaksi memiliki default value.
* Field tambahan seperti note dan tag bersifat opsional.

## Product Focus

Fokus awal aplikasi:

* Quick add expense.
* Quick add income.
* Default wallet.
* Quick category.
* Recent transactions.
* Edit/delete transaction.
* Monthly summary sederhana.

Aplikasi harus terasa seperti alat pencatat cepat, bukan sistem akuntansi.

## Non-Goals

Pada tahap awal, aplikasi ini tidak bertujuan menjadi:

* Aplikasi akuntansi bisnis.
* Aplikasi budgeting kompleks.
* Aplikasi investment tracker.
* Aplikasi trading.
* Aplikasi bank aggregator.
* Aplikasi dengan integrasi rekening bank otomatis.
* Aplikasi laporan finansial detail.
* Aplikasi multi-currency kompleks.
* Aplikasi yang mengandalkan koneksi internet untuk mencatat transaksi.

## Design Direction

Aplikasi harus memprioritaskan input transaksi di atas dashboard.

Dashboard, grafik, laporan, dan insight boleh ada, tetapi tidak boleh menghalangi aksi utama.

Prioritas layar utama:

```txt
Transaction Input > Recent Transactions > Summary > Reports
```

Home screen sebaiknya langsung menyediakan:

* Numeric input.
* Quick category.
* Default wallet indicator.
* Save action.
* Recent transactions.

## Success Metrics

Keberhasilan aplikasi tidak hanya diukur dari jumlah fitur, tetapi dari seberapa cepat user bisa mencatat transaksi.

Metric utama:

| Metric                                      |        Target |
| ------------------------------------------- | ------------: |
| Time to record expense                      |     < 5 detik |
| Tap count to save expense                   |       2–3 tap |
| App startup to input ready                  |     < 1 detik |
| Required fields                             |  Maksimal 2–3 |
| Save latency                                | Instant/local |
| User can record without opening report page |           Yes |

## Product Philosophy

Financial tracking gagal bukan karena user tidak peduli dengan uangnya.

Financial tracking sering gagal karena proses mencatat terlalu lambat dan terlalu mengganggu aktivitas harian.

Aplikasi ini harus membuat pencatatan terasa ringan, cepat, dan hampir tidak mengganggu.
