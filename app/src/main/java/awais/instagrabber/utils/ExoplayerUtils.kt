package awais.instagrabber.utils

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache

object ExoplayerUtils {
    private const val MAX_CACHE_BYTES: Long = 1048576
    private val cacheEvictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
    fun getCachedMediaSourceFactory(context: Context): DefaultMediaSourceFactory {
        val exoDatabaseProvider = ExoDatabaseProvider(context)
        val simpleCache = SimpleCache(context.cacheDir, cacheEvictor, exoDatabaseProvider)
        val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(DefaultHttpDataSourceFactory())
        return DefaultMediaSourceFactory(cacheDataSourceFactory)
    }
}