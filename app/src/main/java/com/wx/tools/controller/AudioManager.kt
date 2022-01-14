package com.wx.tools.controller

import android.media.AudioFormat
import com.wx.tools.utils.AudioTracker
import com.wx.tools.utils.FileUtil
import com.wx.tools.utils.JLog
import com.wx.tools.utils.LameUtil
import com.zly.media.silk.SilkDecoder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioManager {
    private var audioTracker = AudioTracker()
    private var callback: AudioTracker.AudioPlayListener? = null
    private const val DEFAULT_TATE = 16000
    private const val ALIYUN_AUDIO_RATE = 16000
    private const val ALIYUN_CHANNEL_CONFIG = AudioFormat.ENCODING_DEFAULT
    private const val XES_AI_AUDIO_TATE = 16000
    private const val XES_AI_AUDIO_BITRATE = 32
    private const val XES_AI_MP3_QUALITY = 2


    fun play(from: String, callback: AudioTracker.AudioPlayListener) {
        JLog.i("status = ${audioTracker.status}")

        this.callback = callback

        when (audioTracker.status) {
            AudioTracker.Status.STATUS_NO_READY, AudioTracker.Status.STATUS_READY -> playAmr(from, callback)
            AudioTracker.Status.STATUS_START -> audioTracker.stop()
            AudioTracker.Status.STATUS_STOP -> audioTracker.start()
            else -> {
                return
            }
        }

    }

    fun pause() {
        audioTracker.pause()
    }

    private fun playAmr(from: String, callback: AudioTracker.AudioPlayListener) {
        val trans = transformStandardSLK(from)
        if (trans.isNullOrEmpty()) {
            return
        }

        val result = decodeSLK2PCM(from, trans)
        if (result.isNullOrEmpty()) {
            return
        }

//        decodePCM2MP3(result)

        //转化成功删除临时文件
        FileUtil.deleteFile(trans)

        JLog.i("result = $result")
        audioTracker.createAudioTrack(result, callback)

    }


    /**
     * 返回解码后的语音文件
     */
    fun getTransformedAudio(from: String): String? {
        val trans = transformStandardSLK(from)
        if (trans.isNullOrEmpty()) {
            return null
        }

        val result = decodeSLK2PCM(from, trans)
        if (result.isNullOrEmpty()) {
            return null
        }

//        decodePCM2MP3(result)

        FileUtil.deleteFile(trans)

        return result
    }

    fun release() {
        JLog.i("release audio status = ${audioTracker.status}")
        when (audioTracker.status) {
            AudioTracker.Status.STATUS_NO_READY -> {
            }

            //预备
            AudioTracker.Status.STATUS_READY -> {
                audioTracker.removeAudioPlayListener()
                audioTracker.release()
            }

            //播放
            AudioTracker.Status.STATUS_START -> {
                audioTracker.pause()
                audioTracker.stop()
                audioTracker.removeAudioPlayListener()
                return
            }

            //暂停中
            AudioTracker.Status.STATUS_PAUSE -> {
                audioTracker.stop()
                audioTracker.removeAudioPlayListener()
                return
            }

            //停止
            AudioTracker.Status.STATUS_STOP -> {
                audioTracker.removeAudioPlayListener()
                audioTracker.release()
            }

            else -> {
                audioTracker.removeAudioPlayListener()
            }
        }

    }


    /**
     * 将不标准的微信amr文件转化成标准的SLK文件
     */
    private fun transformStandardSLK(from: String): String? {
        try {
            JLog.i("from = $from")
            val file = File(from)
            val out = from.replace(file.name, "temp")
            val fis = FileInputStream(from)
            val bis = ByteArrayInputStream(fis.readBytes())
            val ous = FileOutputStream(out)

            //不要第一个字节
            bis.skip(1)
            val bytes = ByteArray(1024)
            var read: Int

            do {
                read = bis.read(bytes)
                if (read != -1) {
                    ous.write(bytes, 0, read)
                }
            } while (read > 0)

            ous.close()
            bis.close()
            fis.close()
            JLog.i("finish")
            return out

        } catch (ex: Exception) {
            JLog.i("ex = ${ex.message}")
            return null
        }
    }

    /**
     * 将标准的SILK音频文件转换成PCM音频文件
     */
    private fun decodeSLK2PCM(from: String, trans: String): String? {
        val file = File(from)
        val name = file.name.replace("msg_", "")
        val out = from.replace(file.name, name)
        return SilkDecoder.transcode2PCM(trans, DEFAULT_TATE, out)
    }

    private fun decodePCM2MP3(from: String): String? {
        val file = File(from)
        val name = "${System.currentTimeMillis()}.mp3"
        val out = from.replace(file.name, name)
        val inputStream = FileInputStream(from)
        val outputStream = FileOutputStream(out)

        //最多支持两声道
        LameUtil.init(ALIYUN_AUDIO_RATE, ALIYUN_CHANNEL_CONFIG, XES_AI_AUDIO_TATE, XES_AI_AUDIO_BITRATE, XES_AI_MP3_QUALITY)

        val b = ByteArray(4096)
        var read = 0
        do {
            read = inputStream.read(b)
            if (read != -1) {
                val bytes = ByteArray(read)
                System.arraycopy(b, 0, bytes, 0, read)

                val shorts = bytesToShort(bytes)
                if (shorts != null) {
                    //采样点的个数等于buffer的大小除声道数
                    val mp3Len = LameUtil.encode(shorts, shorts, shorts.size / 2, b)
                    if (mp3Len > 0) {
                        outputStream.write(b, 0, mp3Len)
                    }
                }
            }
        } while (read > 0)

        val flush = LameUtil.flush(b)
        outputStream.write(b, 0, flush)
        LameUtil.close()
        outputStream.close()
        inputStream.close()

        JLog.i("transcode finish")
        JLog.i("outPath = $out")

        val dest = File(out)
        if (dest.exists()) {
            return out
        }

        return null

    }

    private fun bytesToShort(bytes: ByteArray?): ShortArray? {
        if (bytes == null) {
            return null
        }
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[shorts]
        return shorts
    }


}