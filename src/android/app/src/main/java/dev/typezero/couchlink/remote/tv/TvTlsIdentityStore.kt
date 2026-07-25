package dev.typezero.couchlink.remote.tv

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class TvTlsIdentityStore(private val context: Context) {
    internal data class Identity(
        val privateKey: PrivateKey,
        val certificate: X509Certificate,
    )

    fun loadOrCreate(): Identity = load() ?: createAndStore()

    private fun load(): Identity? {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val encryptedKey = prefs.getString(KEY_PRIVATE_KEY, null) ?: return null
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val certificateBytes = prefs.getString(KEY_CERTIFICATE, null) ?: return null

        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateWrappingKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)),
            )
            val privateKeyBytes = cipher.doFinal(Base64.decode(encryptedKey, Base64.NO_WRAP))
            val privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(Base64.decode(certificateBytes, Base64.NO_WRAP).inputStream()) as X509Certificate
            Identity(privateKey, certificate)
        }.getOrNull()
    }

    private fun createAndStore(): Identity {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048, SecureRandom())
        }.generateKeyPair()

        val now = System.currentTimeMillis()
        val subject = X500Name("CN=CouchLink TV Remote")
        val provider = BouncyCastleProvider()
        val certificateBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            Date(now - 60_000L),
            Date(now + TWENTY_YEARS_MS),
            subject,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(provider)
            .build(keyPair.private)
        val certificate = JcaX509CertificateConverter()
            .setProvider(provider)
            .getCertificate(certificateBuilder.build(signer))
        certificate.checkValidity()
        certificate.verify(keyPair.public)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val encryptedKey = cipher.doFinal(keyPair.private.encoded)

        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_PRIVATE_KEY, Base64.encodeToString(encryptedKey, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CERTIFICATE, Base64.encodeToString(certificate.encoded, Base64.NO_WRAP))
            .apply()

        return Identity(keyPair.private, certificate)
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(WRAPPING_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            WRAPPING_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(spec)
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val WRAPPING_KEY_ALIAS = "couchlink_tv_remote_wrap_v3"
        const val PREFERENCES = "couchlink_tv_remote_tls_v3"
        const val KEY_PRIVATE_KEY = "private_key"
        const val KEY_IV = "private_key_iv"
        const val KEY_CERTIFICATE = "certificate"
        const val TWENTY_YEARS_MS = 20L * 365L * 24L * 60L * 60L * 1000L
    }
}
