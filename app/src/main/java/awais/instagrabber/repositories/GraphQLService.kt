package awais.instagrabber.repositories

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface GraphQLService {
    @GET("/graphql/query/")
    suspend fun fetch(@QueryMap(encoded = true) queryParams: Map<String, String>): String

    @GET("/{username}/")
    suspend fun getUser(@Path("username") username: String): String

    @GET("/p/{shortcode}/?__a=1")
    suspend fun getPost(@Path("shortcode") shortcode: String): String

    @GET("/explore/tags/{tag}/")
    suspend fun getTag(@Path("tag") tag: String): String

    @GET("/explore/locations/{locationId}/")
    suspend fun getLocation(@Path("locationId") locationId: Long): String
}