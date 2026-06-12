package com.uniformdist.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UniformDistApp : Application(), ImageLoaderFactory {

    /**
     * Coil image loader keyed by content hash (see ItemsListScreen). The
     * backend hands out short-lived signed URLs that rotate every request, so
     * we cache by the stable `image_hash` via diskCacheKey/memoryCacheKey and
     * tell Coil to ignore the URLs' cache headers. This makes thumbnails load
     * instantly from disk on later launches and only re-download when content
     * actually changes.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
}
