package com.perenniasystems.lascito.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import android.os.Build

/**
 * Utility object to compress and resize images before uploading to Firebase Storage.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1080
    private const val COMPRESS_QUALITY = 80 // 80% JPEG/WEBP quality

    /**
     * Reads the image from the provided [uri], scales it down if it exceeds [MAX_DIMENSION],
     * and compresses it to JPEG at [COMPRESS_QUALITY].
     *
     * @return ByteArray representing the compressed image.
     */
    suspend fun compressImage(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            // 1. Decodificar solo los límites para obtener dimensiones reales sin saturar memoria
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return@withContext null

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // 2. Calcular inSampleSize para submuestreo nativo (debe ser potencia de 2)
            options.inSampleSize = calculateInSampleSize(options, MAX_DIMENSION, MAX_DIMENSION)
            options.inJustDecodeBounds = false

            // 3. Decodificar la imagen real con la escala aplicada
            inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) return@withContext null

            // 4. Comprimir el Bitmap resultante a WEBP (80% calidad)
            val outputStream = ByteArrayOutputStream()
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, COMPRESS_QUALITY, outputStream)
            
            // Reciclar el bitmap explícitamente para liberar memoria en capas nativas
            bitmap.recycle()
            
            return@withContext outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            inputStream?.close()
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
