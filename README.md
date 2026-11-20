# Bayiku – aplikasi catatan bayi (Android, offline)

Aplikasi Android untuk mencatat:
- menyusu kiri/kanan (pakai timer, bisa dijeda dan ganti sisi),
- botol ASIP/sufor,
- tidur,
- popok (dengan stok yang berkurang otomatis),
- tummy time,
- suhu badan dan catatan/obat,
- berat, panjang, dan lingkar kepala (dengan grafik berat),
- jadwal imunisasi Kemenkes (Nasional atau Yogyakarta), lengkap dengan tanggalnya dihitung dari tanggal lahir,
- kurva pertumbuhan WHO 0–24 bulan (berat, panjang, lingkar kepala) dan z-score,
- milestone dengan foto,
- obat & vitamin dengan jadwal jam,
- notifikasi timer berjalan, serta pengingat menyusu, imunisasi, dan obat,
- mode gelap otomatis malam hari,
- widget layar utama (tombol cepat Kiri, Kanan, Popok),
- tampilan earth tone dengan mode terang/gelap,
- vitamin/obat manual (ml), tren berat & panjang badan,
- Buku Bayi Kecil (Kemenkes): usia koreksi, data lahir & pulang RS, penapisan, PMK, perah ASI, rekap pemantauan harian BBLR, tanda bahaya, tahapan perkembangan, ringkasan pelayanan/kontrol,
- ekspor riwayat ke Excel dan PDF.

Semua data tersimpan di HP saja dan aplikasinya bisa dipakai tanpa internet. Ada fitur **Simpan cadangan** dan **Pulihkan** di menu Atur.

## Cara mendapatkan file APK

File APK dirakit otomatis oleh GitHub Actions setiap kali ada perubahan di cabang `main`.

1. Buka tab **Actions** di repo ini, lalu tunggu proses "Rakit APK Bayiku" selesai (sekitar 3–5 menit).
2. Buka **Releases**, lalu unduh **Bayiku.apk** dari HP Android.
3. Buka file itu. Kalau muncul peringatan, izinkan "Instal dari sumber tidak dikenal" untuk browser atau file manager yang kamu pakai.

## Catatan penting

- **Jadikan repo ini Private.** File `app/bayiku.jks` adalah kunci tanda tangan aplikasi. Kunci yang sama membuat pembaruan bisa dipasang menimpa versi lama **tanpa menghapus data**.
- Kalau mau merilis versi baru, naikkan `versionCode` di `app/build.gradle`.
- Tampilan dan logika aplikasi ada di `app/src/main/assets/index.html`.
