package com.example.acousticgestures

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.nio.FloatBuffer

class AudioWriter {
    private val flowable: Flowable<FloatArray>

    init {
        flowable = Flowable.create<FloatArray>({sub ->
            // init recorder (modified from https://github.com/paramsen/noise/blob/master/sample/src/main/java/com/paramsen/noise/sample/source/AudioSource.kt)
            val src = MediaRecorder.AudioSource.MIC
            val cfg = AudioFormat.CHANNEL_IN_MONO
            val format = AudioFormat.ENCODING_PCM_16BIT
            val size = AudioRecord.getMinBufferSize(SAMPLE_RATE, cfg, format)

            val recorder = AudioRecord(src, SAMPLE_RATE, cfg, format, size)

            recorder.startRecording()
            sub.setCancellable {
                recorder.stop()
                recorder.release()
            }

            // write recorder data to buffer
            val buffer = ShortArray(512)
            val out = FloatBuffer.allocate(SAMPLE_SIZE)
            var dest = FloatArray(SAMPLE_SIZE+2)
            var read = 0

            while(!sub.isCancelled) {
                read += recorder.read(buffer, read, buffer.size - read)

                if (read == buffer.size) {
                    for (el in buffer) {
                        out.put(el.toFloat())
                    }

                    if (!out.hasRemaining()) {
                        val outCopy = FloatArray(out.array().size)
                        System.arraycopy(
                            out.array(),
                            0,
                            outCopy,
                            0,
                            out.array().size
                        )
                        sub.onNext(outCopy)
                        out.clear()
                    }

                    read = 0
                }
            }
        }, BackpressureStrategy.DROP)
            .subscribeOn(Schedulers.io())
            .share()
    }


    fun stream(): Flowable<FloatArray> {
        return flowable
    }
}
