package com.sosmed.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class CloudinaryRuntimeHint implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // Registrasi Kelas UploaderStrategy Cloudinary secara aman
        try {
            hints.reflection().registerType(
                org.springframework.util.ClassUtils.resolveClassName(
                    "com.cloudinary.http5.UploaderStrategy", classLoader),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        } catch (IllegalArgumentException e) {
            // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
        }

        // Registrasi Kelas ApiStrategy Cloudinary secara aman
        try {
            hints.reflection().registerType(
                org.springframework.util.ClassUtils.resolveClassName(
                    "com.cloudinary.http5.ApiStrategy", classLoader),
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        } catch (IllegalArgumentException e) {
            // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
        }
    }
    
}
