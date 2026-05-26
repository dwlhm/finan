# Design Principles: Capture-First Financial Tracker

## 1. Input Is the Main Screen

Aplikasi ini harus memperlakukan pencatatan transaksi sebagai aktivitas utama, bukan fitur tambahan.

Saat user membuka aplikasi, fokus utama layar adalah mencatat transaksi.

Dashboard, report, grafik, dan insight tidak boleh menjadi penghalang sebelum user bisa mencatat.

### Rule

Home screen harus langsung menyediakan:

- Input nominal
- Kategori cepat
- Wallet aktif
- Tombol simpan
- Akses cepat ke transaksi terakhir

### Avoid

- Membuka aplikasi langsung ke dashboard berat
- Menyembunyikan tombol add transaction
- Memaksa user memilih menu sebelum mencatat
- Menampilkan terlalu banyak insight sebelum input

---

## 2. Record First, Complete Later

User harus bisa mencatat transaksi meskipun datanya belum sempurna.

Flow utama tidak boleh memaksa user mengisi semua detail di awal.

### Required

- Amount
- Category
- Wallet, melalui default wallet
- Date, melalui default today
- Type, melalui default expense

### Optional

- Note
- Tag
- Merchant
- Attachment
- Location
- Custom time
- Description

### Principle

Lebih baik transaksi cepat tersimpan dan bisa diedit nanti daripada user gagal mencatat karena form terlalu panjang.

---

## 3. Defaults Reduce Friction

Aplikasi harus menggunakan default value untuk mengurangi jumlah keputusan user.

User tidak perlu memilih hal yang biasanya bisa ditebak oleh sistem.

### Default Values

- Transaction type: Expense
- Date: Today
- Time: Now
- Wallet: Default wallet
- Currency: Default currency
- Category order: Most used first

### Rule

Default harus terlihat, tetapi tidak mengganggu.

Contoh:

```txt
Rp25.000

From: Bank Jago ▾

[Food] [Transport] [Coffee] [Bills]

[Save]
````

User tetap bisa mengubah wallet, tanggal, atau tipe transaksi, tapi tidak wajib untuk transaksi normal.

---

## 4. Visible Context, Minimal Interaction

Walaupun beberapa data diisi otomatis, user tetap harus tahu konteks transaksi.

Sistem tidak boleh menyembunyikan informasi penting seperti wallet aktif.

### Example

Buruk:

```txt
Rp25.000
[Food]
[Save]
````

Masalah:

* User tidak tahu uangnya keluar dari wallet mana.

Lebih baik:

```txt
Rp25.000

Expense • Today
From: Bank Jago ▾

[Food] [Transport] [Coffee]

[Save]
```

### Principle

Informasi penting harus terlihat, tetapi tidak harus menjadi langkah tambahan.

---

## 5. The Fast Path Must Stay Short

Aplikasi boleh punya fitur detail, tetapi fast path harus tetap pendek.

Fast path adalah alur transaksi paling umum:

```txt
Open App
→ Input Amount
→ Choose Category
→ Save
````

Target:

| Metric            |      Target |
| ----------------- | ----------: |
| Tap count         |     2–3 tap |
| Time to save      | < 5 seconds |
| Required decision |     Minimal |
| Blocking screen   |        None |

### Rule

Setiap tambahan field di fast path harus dipertanyakan:

> Apakah field ini wajib untuk mencatat transaksi sekarang?

Jika tidak, pindahkan ke secondary flow.

---

## 6. One-Handed and Interruptible

Pencatatan transaksi sering dilakukan dalam kondisi tidak ideal:

- Setelah bayar
- Saat antre
- Saat jalan
- Saat pegang barang
- Saat buru-buru
- Saat hanya punya beberapa detik

Karena itu, UI harus bisa digunakan cepat dan idealnya dengan satu tangan.

### Design Implication

- Tombol utama mudah dijangkau
- Kategori sering dipakai berada dekat area input
- Tidak banyak scroll
- Tidak banyak modal bertingkat
- Input tidak hilang jika user keluar aplikasi
- Draft transaksi tetap disimpan sementara

---

## 7. Correction Must Be Faster Than Prevention

Jangan membuat input awal terlalu ketat hanya untuk mencegah kesalahan.

Kesalahan kecil harus mudah diperbaiki.

### Required Interactions

- Undo after save
- Edit last transaction
- Change category quickly
- Change wallet quickly
- Delete recent transaction quickly

### Principle

Aplikasi tidak perlu memaksa user sempurna di awal.

Yang penting:

1. Transaksi tercatat.
2. Kesalahan mudah diperbaiki.

---

## 8. Frequent Actions Are Direct, Rare Actions Are Hidden

UI harus memisahkan aksi harian dan aksi sesekali.

### Frequent Actions

Harus mudah diakses:

- Add expense
- Add income
- Choose common category
- Change active wallet
- Save transaction
- Edit last transaction
- See recent transactions

### Rare Actions

Boleh ditempatkan lebih dalam:

- Manage wallet
- Manage category
- Export data
- Import data
- Budget settings
- Advanced report
- Sync settings
- Security settings

### Principle

Jangan memberi bobot visual yang sama pada semua fitur.

---

## 9. Progressive Disclosure

Detail lanjutan hanya muncul ketika dibutuhkan.

Fast input screen harus sederhana, tapi tetap menyediakan akses ke detail tambahan.

### Example

Default screen:

```txt
Amount
Wallet
Category
Save
````

Advanced section:

```txt
+ Add details
  - Note
  - Date
  - Time
  - Tag
  - Merchant
```

### Principle

Sembunyikan kompleksitas, bukan kemampuan.

---

## 10. No Loading in the Critical Path

Pencatatan transaksi tidak boleh menunggu network request.

Saat user menekan save, transaksi harus langsung tersimpan secara lokal.

### Good Flow

```txt
Tap Save
→ Saved instantly
→ Sync later
````

### Bad Flow

```txt
Tap Save
→ Loading
→ Request server
→ Wait
→ Saved
```

### Principle

Network boleh ada, tapi tidak boleh menghalangi pencatatan.

---

## 11. UI Should Match Real Transaction Behavior

Aplikasi harus mengikuti cara transaksi terjadi di kehidupan nyata.

Biasanya user tahu:

- Berapa nominalnya
- Untuk apa transaksinya
- Bayarnya dari mana

Tapi user belum tentu ingin menulis:

- Catatan detail
- Merchant
- Tag
- Budget
- Deskripsi panjang

### Principle

UI harus mengikuti urutan pikiran user setelah transaksi:

```txt
Berapa?
→ Buat apa?
→ Dari mana?
→ Selesai
````

---

## 12. Reports Are Secondary Feedback

Report penting, tapi bukan pusat interaksi harian.

Report berfungsi memberi feedback setelah data terkumpul.

### Report Should Answer

- Bulan ini uang keluar berapa?
- Kategori terbesar apa?
- Wallet tersisa berapa?
- Pengeluaran hari ini berapa?
- Transaksi terakhir apa?

### Report Should Not

- Menghalangi input
- Membuat home screen berat
- Membutuhkan loading sebelum user bisa mencatat
- Mendominasi pengalaman aplikasi

---

## 13. Calm, Not Judgmental

Aplikasi finansial sering membuat user merasa bersalah.

Produk ini harus membantu user mencatat dan melihat pola, bukan menghakimi.

### Avoid Tone

- "Kamu boros"
- "Pengeluaranmu buruk"
- "Kamu gagal mencapai target"

### Prefer Tone

- "Pengeluaran makan naik minggu ini"
- "Transport lebih tinggi dari biasanya"
- "Kamu mencatat 5 transaksi hari ini"

### Principle

Data harus informatif, bukan menyalahkan.

---

## 14. Small Setup, Immediate Use

User baru harus bisa memakai aplikasi tanpa konfigurasi panjang.

Setup awal cukup:

- Pilih atau buat wallet utama
- Gunakan kategori default
- Mulai catat transaksi

### Avoid

- Setup budget wajib
- Input semua wallet di awal
- Pilih banyak preferensi
- Registrasi wajib sebelum mencoba
- Tutorial panjang

### Principle

User harus bisa mencatat transaksi pertama secepat mungkin.

---

## 15. Every UI Element Must Justify Its Presence

Karena aplikasi ini berfokus pada kecepatan, setiap elemen UI harus punya alasan jelas.

Sebelum menambahkan elemen, tanyakan:

1. Apakah ini membantu user mencatat lebih cepat?
2. Apakah ini mengurangi kesalahan?
3. Apakah ini memberi konteks penting?
4. Apakah ini lebih baik ditaruh di secondary flow?

Jika tidak, elemen tersebut tidak masuk layar utama.

---

# Versi ringkas prinsipnya

```txt
1. Input is the main screen.
2. Record first, complete later.
3. Defaults reduce friction.
4. Important context must stay visible.
5. Fast path must stay short.
6. No loading in the critical path.
7. Correction must be easy.
8. Frequent actions are direct.
9. Rare actions are hidden.
10. Reports are secondary.
```
