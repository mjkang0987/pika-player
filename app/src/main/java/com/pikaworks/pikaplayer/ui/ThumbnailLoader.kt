package com.pikaworks.pikaplayer.ui

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

/**
 * 동영상 파일에서 첫 프레임을 뽑아 썸네일로 쓴다.
 * Coil 기본 ImageLoader 는 이미지 파일만 다루므로 VideoFrameDecoder 를 붙여야 한다.
 *
 * TODO: PikaApp 에서 ImageLoaderFactory 로 등록해 전역 기본값으로 만들 것.
 */
fun buildImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(true)
        .build()
