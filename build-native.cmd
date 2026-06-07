@echo off
setlocal

echo =======================================================================
echo [PRE-BUILD] Memeriksa dan membersihkan sisa proses sosmed-native...
echo =======================================================================

rem Stop proses berdasarkan nama file exe jika masih berjalan di background
taskkill /f /im sosmed-native.exe 2>nul

rem Memastikan folder tujuan bersih dari exe lama agar tidak terjadi konflik copy
if exist "dist-native\sosmed-native.exe" (
    del /f /q "dist-native\sosmed-native.exe" 2>nul
)

set "VCVARS="

if exist "C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\18\Community\VC\Auxiliary\Build\vcvars64.bat"
) else if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
) else if exist "C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
)

if not defined VCVARS (
    echo Could not find vcvars64.bat for Visual Studio.
    exit /b 1
)

call "%VCVARS%"
where cl

if "%~1"=="" (
    call gradlew.bat clean nativeCompile -x test --console=plain
) else (
    call gradlew.bat %*
)

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Proses kompilasi Gradle atau Native Image gagal!
    exit /b %ERRORLEVEL%
)

echo.
echo =======================================================================
echo [SUCCESS] Build Native Image Berhasil Selesai!
echo =======================================================================
echo Menyusun folder distribusi mandiri (dist-native)...

if not exist "dist-native" (
    mkdir dist-native
)

if exist "build\native\nativeCompile\sosmed-native.exe" (
    copy /Y "build\native\nativeCompile\sosmed-native.exe" "dist-native\"
    echo [+] File sosmed-native.exe berhasil disalin ke dist-native/
) else (
    echo [WARNING] File sosmed-native.exe tidak ditemukan di folder build default.
)

echo Membuat folder konfigurasi eksternal...
if not exist "dist-native\config" (
    mkdir "dist-native\config"
)

if exist "src\main\resources\application.properties" (
    copy /Y "src\main\resources\application.properties" "dist-native\config\"
    echo [+] application.properties berhasil diekstrak ke dist-native/config/
)

if exist "src\main\resources\logback-spring.xml" (
    copy /Y "src\main\resources\logback-spring.xml" "dist-native\config\"
    echo [+] logback-spring.xml berhasil diekstrak ke dist-native/config/
)

if exist "src\main\resources\schema.sql" (
    copy /Y "src\main\resources\schema.sql" "dist-native\config\"
    echo [+] schema.sql berhasil diekstrak ke dist-native/config/
)

echo Menyesuaikan konfigurasi internal untuk Mode Native
if exist "dist-native\config\application.properties" (
    powershell -Command "$p = 'dist-native\config\application.properties'; (Get-Content $p) -replace 'logging.config=classpath:logback-spring.xml', 'logging.config=file:./config/logback-spring.xml' -replace 'spring.sql.init.mode=always', ('spring.sql.init.mode=always' + [Environment]::NewLine + 'spring.sql.init.schema-locations=file:./config/schema.sql') | Set-Content $p"
    echo [+] Berhasil melakukan replasment konfigurasi path eksternal pada dist-native/config/application.properties
)

echo =======================================================================
echo Distribusi siap digunakan! Cek folder: .\dist-native
echo =======================================================================
