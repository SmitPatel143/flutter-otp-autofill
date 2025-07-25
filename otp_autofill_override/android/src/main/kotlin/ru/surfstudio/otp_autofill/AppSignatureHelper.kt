package ru.surfstudio.otp_autofill

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private const val HASH_TYPE = "SHA-256"
private const val NUM_HASHED_BYTES = 9
private const val NUM_BASE64_CHAR = 11

class AppSignatureHelper(context: Context) : ContextWrapper(context) {

    fun getAppSignatures(): List<String> {
        return try {
            val packageName = packageName
            val packageManager = packageManager

            val signatures: List<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.toList() ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                    .signatures
                    ?.toList() ?: emptyList()
            }

            signatures.mapNotNull { signature ->
                hash(packageName, signature.toCharsString())
            }

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun hash(packageName: String, signature: String): String? {
        val appInfo = "$packageName $signature"
        return try {
            val messageDigest = MessageDigest.getInstance(HASH_TYPE)
            messageDigest.update(appInfo.toByteArray(StandardCharsets.UTF_8))
            val hashSignature = messageDigest.digest().copyOfRange(0, NUM_HASHED_BYTES)
            val base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING or Base64.NO_WRAP)
            base64Hash.substring(0, NUM_BASE64_CHAR)
        } catch (e: NoSuchAlgorithmException) {
            null
        }
    }
}
