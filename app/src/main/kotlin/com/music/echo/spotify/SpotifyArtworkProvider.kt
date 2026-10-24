/*
 * EchoMusic (2026)
 * Spotify High-Resolution Artwork Provider
 */

package echo.music.iad1tya.spotify

import android.content.Context
import echo.music.iad1tya.constants.SpotifyAccessTokenKey
import echo.music.iad1tya.utils.dataStore
import echo.music.iad1tya.utils.get
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SpotifyArtworkProvider {
  private val cache = ConcurrentHashMap<String, String>()

  suspend fun getArtworkUrl(
    context: Context,
    title: String,
    artist: String,
  ): String? = withContext(Dispatchers.IO) {
    if (title.isBlank()) return@withContext null
    val key = "${title.trim().lowercase()}_${artist.trim().lowercase()}"
    cache[key]?.let { return@withContext it }

    try {
      // Ensure access token is initialized if connected
      if (Spotify.accessToken.isNullOrBlank()) {
        val savedToken = context.dataStore[SpotifyAccessTokenKey]
        if (!savedToken.isNullOrBlank()) {
          Spotify.accessToken = savedToken
        }
      }

      val query = if (artist.isNotBlank()) "track:\"$title\" artist:\"$artist\"" else title
      val result = Spotify.search(query = query, types = listOf("track"), limit = 1).getOrNull()
      val track = result?.tracks?.items?.firstOrNull() ?: run {
        // Fallback search with broad query
        Spotify.search(query = "$title $artist", types = listOf("track"), limit = 1).getOrNull()?.tracks?.items?.firstOrNull()
      }

      val artworkUrl = track?.album?.images?.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) }?.url
        ?: track?.album?.images?.firstOrNull()?.url

      if (!artworkUrl.isNullOrBlank()) {
        cache[key] = artworkUrl
        return@withContext artworkUrl
      }
    } catch (_: Exception) {
      // Ignore network/API failures and gracefully fallback
    }
    null
  }
}
