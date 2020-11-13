package awais.instagrabber.repositories;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

public interface TagsRepository {

    @POST("/web/tags/follow/{tag}/")
    Call<String> follow(@Header("User-Agent") String userAgent,
                        @Header("x-csrftoken") String csrfToken,
                        @Path("tag") String tag);

    @POST("/web/tags/unfollow/{tag}/")
    Call<String> unfollow(@Header("User-Agent") String userAgent,
                          @Header("x-csrftoken") String csrfToken,
                          @Path("tag") String tag);

    @GET("/api/v1/feed/tag/{tag}/")
    Call<String> fetchPosts(@Path("tag") final String tag,
                            @QueryMap Map<String, String> queryParams);

    @GET("/graphql/query/")
    Call<String> fetchGraphQLPosts(@QueryMap(encoded = true) Map<String, String> queryParams);
}
