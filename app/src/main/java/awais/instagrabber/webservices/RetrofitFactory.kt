package awais.instagrabber.webservices

import awais.instagrabber.BuildConfig
import awais.instagrabber.repositories.responses.Caption
import awais.instagrabber.repositories.serializers.CaptionDeserializer
import awais.instagrabber.utils.Utils
import awais.instagrabber.webservices.interceptors.AddCookiesInterceptor
import awais.instagrabber.webservices.interceptors.IgErrorsInterceptor
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File

object RetrofitFactory {
    private const val cacheSize: Long = 10 * 1024 * 1024 // 10 MB
    private val cache = Cache(File(Utils.cacheDir), cacheSize)
    private val igErrorsInterceptor: IgErrorsInterceptor by lazy { IgErrorsInterceptor() }

    private val retrofitBuilder: Retrofit.Builder by lazy {
        val clientBuilder = OkHttpClient.Builder().apply {
            followRedirects(false)
            followSslRedirects(false)
            cache(cache)
            addInterceptor(AddCookiesInterceptor())
            addInterceptor(igErrorsInterceptor)
            if (BuildConfig.DEBUG) {
                // addInterceptor(LoggingInterceptor())
            }
        }
        val gson = GsonBuilder().apply {
            setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            registerTypeAdapter(Caption::class.java, CaptionDeserializer())
            setLenient()
        }.create()
        Retrofit.Builder().apply {
            addConverterFactory(ScalarsConverterFactory.create())
            addConverterFactory(GsonConverterFactory.create(gson))
            client(clientBuilder.build())
        }
    }

    val retrofit: Retrofit by lazy {
        retrofitBuilder
            .baseUrl("https://i.instagram.com")
            .build()
    }

    val retrofitWeb: Retrofit by lazy {
        retrofitBuilder
            .baseUrl("https://www.instagram.com")
            .build()
    }
}