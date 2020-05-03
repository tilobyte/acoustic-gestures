package com.example.acousticgestures

import android.Manifest.permission.RECORD_AUDIO
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.TextView
import com.paramsen.noise.Noise
import java.nio.FloatBuffer
import kotlin.concurrent.thread
import kotlin.math.pow

const val SAMPLE_RATE = 44100
const val nyquist = SAMPLE_RATE / 2
const val SAMPLE_SIZE = 4096

class StartToneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_tone)

        thread(start = true) {
            var mediaPlayer: MediaPlayer? = MediaPlayer.create(this, R.raw.khz18_extended)

            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), 1337)
        }

        // init Noise (https://github.com/paramsen/noise)
        val noise = Noise.real(SAMPLE_SIZE)
        val numBins = 512
        val binSize = SAMPLE_SIZE / numBins
        var bins = FloatArray(11)

        // init recorder (modified from https://github.com/paramsen/noise/blob/master/sample/src/main/java/com/paramsen/noise/sample/source/AudioSource.kt)
        val src = MediaRecorder.AudioSource.MIC
        val cfg = AudioFormat.CHANNEL_IN_MONO
        val format = AudioFormat.ENCODING_PCM_16BIT
        val size = AudioRecord.getMinBufferSize(SAMPLE_RATE, cfg, format)

        val recorder = AudioRecord(src, SAMPLE_RATE, cfg, format, size)

        recorder.startRecording()

        // write recorder data to buffer
        val buffer = ShortArray(512)
        val out = FloatBuffer.allocate(SAMPLE_SIZE)
        var dest = FloatArray(SAMPLE_SIZE + 2)
        var read = 0

        thread(start = true) {
            while (true) {
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
                        out.clear()
                    }

                    read = 0
                }
                var fft = noise.fft(out.array(), dest)

                for (i in 413..423) {
                    var accum = .0f

                    synchronized(fft) {
                        for (j in 0 until binSize step 2)
                        // combine real and imaginary parts
                            accum += (fft[j + (i * binSize)].toDouble()
                                .pow(2.0) + fft[j + 1 + (i * binSize)].toDouble()
                                .pow(2.0)).toFloat()
                    }
                    accum /= binSize / 2
                    bins[i - 413] = accum
                }

                var bottom = bins.sliceArray(0..4).sum()
                var top = bins.sliceArray(6..10).sum()
                var frac = top / bottom
                when {
                    frac > .1 -> {
                        runOnUiThread {
                            val textView = findViewById<TextView>(R.id.textView).apply {
                                text = "push"
                            }
                        }
                    }
                    frac < 0.08 -> {
                        runOnUiThread {
                            val textView = findViewById<TextView>(R.id.textView).apply {
                                text = "pull"
                            }
                        }
                    }
                    else -> {
                        runOnUiThread {
                            val textView = findViewById<TextView>(R.id.textView).apply {
                                text = "no gesture detected"
                            }
                        }
                    }
                }
                SystemClock.sleep(500)
            }
        }
    }
}
