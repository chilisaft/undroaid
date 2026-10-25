package com.chilisaft.undroid.utils

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts/decrypts short strings. Abstracted so [SecureStore] is testable on the JVM without the Android Keystore. */
interface StringCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}

/**
 * AES-256-GCM with a key generated inside the Android Keystore under [keyAlias]: the key material
 * never leaves secure hardware (where available), can't be exported, and isn't part of backups -
 * which is also why the encrypted file is excluded from backup (see res/xml/backup_rules.xml).
 * Output is Base64 of `IV || ciphertext+tag`, with a fresh random 12-byte IV per encryption.
 *
 * Replaces androidx.security:security-crypto's EncryptedSharedPreferences (deprecated).
 */
class KeystoreStringCipher(private val keyAlias: String) : StringCipher {

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
        val sealed = Base64.decode(ciphertext, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        }
        return String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/**
 * String values encrypted with [cipher] and kept in [preferences]. A value that can't be decrypted
 * (its Keystore key is gone - e.g. a file restored from another device) reads as absent instead of
 * crashing; the user just logs in again.
 */
class SecureStore(private val preferences: SharedPreferences, private val cipher: StringCipher) {

    fun getString(key: String): String? =
        preferences.getString(key, null)?.let { runCatching { cipher.decrypt(it) }.getOrNull() }

    fun putString(key: String, value: String?) {
        preferences.edit {
            if (value == null) remove(key) else putString(key, cipher.encrypt(value))
        }
    }
}
