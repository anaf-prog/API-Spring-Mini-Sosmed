# API Spring Mini Sosmed

Aplikasi Backend RESTful API untuk platform media sosial (Sosmed) mini yang dibangun menggunakan ekosistem modern **Java 21** dan **Spring Boot 3**. Proyek ini dirancang secara khusus dengan arsitektur kompilasi *hybrid*, memungkinkan aplikasi dijalankan di atas Java Virtual Machine (JVM) tradisional maupun dikompilasi langsung menjadi *Standalone Native Executable* menggunakan **GraalVM**.

---

## Fitur Utama & Keunggulan Sistem

* **Java 21 Virtual Threads:** Mengadopsi fitur kekinian Java 21 untuk menangani konkurensi tinggi dan *throughput* maksimal secara efisien melalui thread ringan (*lightweight threads*).
* **GraalVM Native Image Ready:** Mendukung optimasi kompilasi Ahead-of-Time (AOT). Aplikasi dapat dirakit menjadi file biner berekstensi `.exe` (untuk Windows) yang mandiri tanpa ketergantungan pada instalasi JRE/JDK di server target.
* **High-Performance Database Pooling:** Integrasi dengan **PostgreSQL** menggunakan pooling koneksi otomatis performa tinggi via **HikariCP**.
* **Eksternalisasi Konfigurasi:** Manajemen port, database, dan kredensial pihak ketiga (seperti Cloudinary dan JWT Secret) yang dinamis menggunakan folder konfigurasi terpisah (`./config/application.properties`).
* **Logging Management (Logback Externalized):** Sistem pencatatan log komprehensif yang diletakkan di luar biner aplikasi (`./config/logback-spring.xml`). Dilengkapi sistem *rolling policy* berbasis waktu harian serta ukuran maksimal berkas (maksimal 10MB per file dengan masa simpan 60 hari), serta manajemen hierarki level log (`DEBUG` untuk paket internal aplikasi dan `INFO` untuk library pihak ketiga).

---

## 🌐 API Documentation

Dokumentasi endpoint API lengkap beserta struktur *Request Body*, *Query Params*, serta contoh respons (*Example Responses* sukses maupun gagal seperti error registrasi/login) dapat diakses melalui tautan Postman berikut:

**[Postman API Documentation - Mini Sosmed API](https://documenter.getpostman.com/view/14089705/2sBXwqqVVn)**

---

## Prasyarat Lingkungan Pengembangan (Prerequisites)

Sebelum melakukan proses kompilasi, pastikan mesin lokal Anda telah memenuhi spesifikasi berikut:
1. **GraalVM JDK 21** (Sangat direkomendasikan Oracle GraalVM versi 21.0.7+8.1 atau terbaru).
2. **Variabel Lingkungan (Environment Variable):** `JAVA_HOME` diarahkan ke jalur folder instalasi GraalVM.
3. **C++ Build Tools (Khusus Windows):** **Microsoft Visual Studio 2022** (atau versi 18/terbaru) beserta paket komponen *Desktop development with C++* untuk menyediakan compiler `cl.exe`.

---

## Panduan Kompilasi & Build Aplikasi

Proyek ini menyediakan dua mode distribusi yang dapat dipilih sesuai kebutuhan deployment Anda:

### 1. Mode Standar: Thin JAR Assembly (JVM Mode)
Mode ini sangat cocok digunakan pada fase pengembangan cepat (*development*) atau deployment standar berbasis container JRE. Proses build sangat cepat karena tidak memerlukan proses optimasi native.

**Langkah Build:**
Buka terminal Anda di root proyek, lalu eksekusi perintah Gradle berikut:
```bash
./gradlew clean build -x test
```

**Output Build Thin JAR Assembly (JVM Mode):**

Jika proses build berhasil, Gradle akan menghasilkan beberapa artefak berikut:

### Folder `install/`
Folder distribusi aplikasi yang siap dijalankan pada server target.
Struktur yang dihasilkan:

```text
install/
├── Sosmed-<BUILD_VERSION>.jar
├── config/
│   ├── application.properties
│   ├── logback-spring.xml
│   └── schema.sql
└── lib/
    ├── spring-boot-*.jar
    ├── postgresql-*.jar
    ├── jjwt-*.jar
    └── dependency lainnya
```

Keterangan:
- `Sosmed-<BUILD_VERSION>.jar` merupakan file utama aplikasi.
- Folder `lib/` berisi seluruh dependency runtime yang dibutuhkan aplikasi.
- Folder `config/` berisi konfigurasi eksternal seperti:
  - `application.properties`
  - `logback-spring.xml`
  - `schema.sql`
- Versi file JAR menggunakan format:

```text
yyyyMMdd-buildNumber
```

Contoh:

```text
Sosmed-20260606-1.jar
```

Sehingga seluruh folder `install/` dapat dipindahkan ke server deployment tanpa perlu mengambil file dari folder `build/`.

### 2. Mode GraalVM Native Image (AOT Mode)
Mode ini mengubah seluruh bytecode Java aplikasi Anda menjadi aplikasi biner mandiri (`.exe` pada Windows). Menghasilkan waktu *startup* yang luar biasa cepat dan konsumsi RAM yang sangat minim.

**Langkah Build Otomatis (Khusus Windows):**
Project ini telah menyediakan script batch otomatisasi lingkungan untuk mempermudah pencarian compiler C++ Visual Studio Anda (`build-native.cmd`). Cukup jalankan perintah berikut di PowerShell atau CMD root proyek:
```bash
.\build-native.cmd
```

**Output Build:**

Jika proses build Native Image berhasil, GraalVM akan menghasilkan aplikasi biner mandiri (*standalone executable*) yang tidak memerlukan instalasi JRE maupun JDK pada server tujuan.

### Folder `dist-native/`

Script `build-native.cmd` secara otomatis akan menyusun paket distribusi siap pakai ke dalam folder `dist-native`.

Contoh struktur direktori hasil build:

```text
dist-native/
├── sosmed-native.exe
└── config/
    ├── application.properties
    ├── logback-spring.xml
    └── schema.sql
```

### Isi Folder Distribusi

#### `sosmed-native.exe`

File executable utama hasil kompilasi GraalVM Native Image.

#### Folder `config`

Folder ini dibuat secara otomatis saat proses build dan berisi konfigurasi eksternal yang diekstrak dari resource aplikasi:

```text
config/
├── application.properties
├── logback-spring.xml
└── schema.sql
```

Selama proses build, script juga akan melakukan penyesuaian konfigurasi agar aplikasi Native Image menggunakan file konfigurasi eksternal yang berada di dalam folder `config`.
Dengan pendekatan ini, perubahan konfigurasi dapat dilakukan tanpa perlu melakukan proses build ulang aplikasi.

### Menjalankan Aplikasi Native

Masuk ke folder distribusi kemudian jalankan executable:

```bash
cd dist-native
.\sosmed-native.exe
```

atau langsung:

```bash
.\dist-native\sosmed-native.exe
```