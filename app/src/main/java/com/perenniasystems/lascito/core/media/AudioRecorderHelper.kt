package com.perenniasystems.lascito.core.media

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): Boolean {
        return try {
            outputFile = File(context.cacheDir, "audio_memory_${System.currentTimeMillis()}.m4a")
            
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stopRecording(): Uri? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile?.let { Uri.fromFile(it) }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            null
        }
    }
    
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // stop() lanza si no llegó a grabarse nada; se descarta igualmente.
        } finally {
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }
}
