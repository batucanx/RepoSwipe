package com.batuhan.reposwipe

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class RepoSwipeApp :
    Application(),
    Configuration.Provider,
    ImageLoaderFactory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.tracesSampleRate = 0.2
            options.isDebug = BuildConfig.DEBUG
        }
    }

    // Coil doesn't decode animated GIFs or SVGs by default, and repo/user avatars, header images,
    // and similar-repo thumbnails throughout the app can be either; without these decoders both
    // render as blank/missing instead of the real image. Implementing ImageLoaderFactory makes
    // this the default loader for every AsyncImage/rememberAsyncImagePainter call app-wide. The
    // README itself renders through a WebView (its own native image decoding, not Coil) rather
    // than through AsyncImage — see `feature/swipe/.../component/ReadmeWebView.kt`.
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }.build()
}
