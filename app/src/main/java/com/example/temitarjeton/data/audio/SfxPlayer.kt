package com.example.temitarjeton.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.temitarjeton.R

class SfxPlayer(context: Context) {

    private val soundPool: SoundPool
    private val winId: Int
    private val loseId: Int

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()

        winId = soundPool.load(context, R.raw.win, 1)
        loseId = soundPool.load(context, R.raw.lose, 1)
    }

    fun playWin() = soundPool.play(winId, 1f, 1f, 1, 0, 1f)
    fun playLose() = soundPool.play(loseId, 1f, 1f, 1, 0, 1f)

    fun release() = soundPool.release()
}