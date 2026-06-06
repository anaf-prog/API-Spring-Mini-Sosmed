package com.sosmed.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class JwtRuntimeHint implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
            // Kelas Utama Algoritma Enkripsi JJWT
            try {
                    hints.reflection().registerType(
                        org.springframework.util.ClassUtils.resolveClassName(
                                "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms", classLoader),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.DECLARED_FIELDS);
            } catch (IllegalArgumentException e) {
                    // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
            }

            // Kelas Operasi Key Kriptografi
            try {
                    hints.reflection().registerType(
                        org.springframework.util.ClassUtils.resolveClassName(
                                "io.jsonwebtoken.impl.security.StandardKeyOperations", classLoader),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS,
                        MemberCategory.DECLARED_FIELDS);
            } catch (IllegalArgumentException e) {
                    // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
            }

            // Struktur Utama Pembuatan Token (DefaultJwtBuilder)
            try {
                    hints.reflection().registerType(
                        org.springframework.util.ClassUtils.resolveClassName(
                                "io.jsonwebtoken.impl.DefaultJwtBuilder", classLoader),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);
            } catch (IllegalArgumentException e) {
                    // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
            }

            // Struktur Utama Parsing Token (DefaultJwtParserBuilder)
            try {
                    hints.reflection().registerType(
                        org.springframework.util.ClassUtils.resolveClassName(
                                "io.jsonwebtoken.impl.DefaultJwtParserBuilder", classLoader),
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);
            } catch (IllegalArgumentException e) {
                    // Abaikan jika kelas tidak ditemukan di classpath agar proses build thin jar tidak gagal
            }
    }
    
}
