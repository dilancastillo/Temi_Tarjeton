package com.example.temitarjeton.data.video

import android.content.Context
import android.net.Uri
import com.example.temitarjeton.R

class VideoProvider(private val context: Context) {
    fun candidateVideoUri(): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.candidato_106}")

    /** Video de atracción (loop). Reemplaza el archivo res/raw/attract_loop.mp4 por el que quieras. */
    fun attractLoopVideoUri(): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.attract_loop}")
}