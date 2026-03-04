package com.perenniasystems.lascito.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt
import android.os.Build
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

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

            // 3. Decodificar la imagen real con la escala aplicada (ahorro de RAM en decodificación base)
            inputStream = context.contentResolver.openInputStream(uri)
            val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (decodedBitmap == null) return@withContext null

            // 4. Leer EXIF y rotar si es necesario
            var rotationDegrees = 0f
            try {
                val exifStream = context.contentResolver.openInputStream(uri)
                // Bloque finally garantiza que el stream se cierra aunque ExifInterface lance una excepción
                if (exifStream != null) {
                    try {
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        rotationDegrees = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                    } finally {
                        exifStream.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Aplicar la matriz de rotacion usando el decodedBitmap ya reducido por inSampleSize (menor costo de RAM)
            val bitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees)
                val rotated = Bitmap.createBitmap(decodedBitmap, 0, 0, decodedBitmap.width, decodedBitmap.height, matrix, true)
                decodedBitmap.recycle() // Reciclar el original no rotado
                rotated
            } else {
                decodedBitmap
            }

            // 5. Comprimir el Bitmap resultante a WEBP (80% calidad)
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
        val height = options.outHeight
        val width  = options.outWidth
        if (height <= reqHeight && width <= reqWidth) return 1

        // Cálculo logarítmico O(1): determina cuántas veces dividir por 2 para
        // que AMBAS dimensiones queden dentro del límite requerido.
        // Evita el bucle while iterativo que puede tardar decenas de iteraciones
        // con imágenes gigantes (gigapíxeles), previniendo potenciales ANRs.
        val ratioWidth  = width.toDouble()  / reqWidth
        val ratioHeight = height.toDouble() / reqHeight
        val largestRatio = max(ratioWidth, ratioHeight)
        return (2.0.pow(log2(largestRatio).toInt())).toInt().coerceAtLeast(1)
    }

    private fun Double.pow(exp: Int): Double {
        var result = 1.0
        repeat(exp) { result *= this }
        return result
    }
}
